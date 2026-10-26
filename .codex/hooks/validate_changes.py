"""Возвращает агенту результат проверок исходников, изменённых вызовом инструмента."""

import contextlib
import fcntl
import hashlib
import json
import os
from pathlib import Path
import shutil
import signal
import subprocess
import sys
import time
import uuid


def snapshot(root):
    result = {}
    for module in ("rfr-auth", "rfr-api", "rfr-e2e", "rfr-gql-client"):
        digest = hashlib.sha256()
        source = root / module if module == "rfr-gql-client" else root / module / "src"
        for directory, directories, files in os.walk(source):
            directories[:] = sorted(name for name in directories if name not in {
                "node_modules", "build", "dist", ".git", ".worktrees",
            } and not name.startswith("."))
            for name in sorted(files):
                path = Path(directory) / name
                extensions = {".ts", ".tsx", ".js", ".jsx", ".mjs", ".cjs", ".css", ".scss", ".html", ".json"}
                if path.is_symlink() or path.suffix not in (extensions if module == "rfr-gql-client" else {".java"}):
                    continue
                digest.update(str(path.relative_to(root)).encode())
                digest.update(b"\0")
                digest.update(hashlib.sha256(path.read_bytes()).digest())
        result[module] = digest.hexdigest()
    return result


@contextlib.contextmanager
def lock(path, timeout=20):
    with path.open("a") as stream:
        deadline = time.monotonic() + timeout
        while True:
            try:
                fcntl.flock(stream, fcntl.LOCK_EX | fcntl.LOCK_NB)
                break
            except BlockingIOError:
                if time.monotonic() >= deadline:
                    raise TimeoutError("Не удалось дождаться блокировки проверок")
                time.sleep(0.05)
        try:
            yield
        finally:
            fcntl.flock(stream, fcntl.LOCK_UN)


def commands(modules):
    result = []
    java = sorted(module for module in modules if module != "rfr-gql-client")
    if java:
        result.append((["./gradlew"] + [":" + module + ":compileTestJava" for module in java] + ["--console=plain"], "."))
    if "rfr-gql-client" in modules:
        result.extend([(["npm", "run", "lint"], "rfr-gql-client"), (["npm", "run", "build"], "rfr-gql-client")])
    return result


def environment(needs_java):
    result = dict(os.environ)
    if needs_java and sys.platform == "darwin":
        java = subprocess.run(["/usr/libexec/java_home", "-v", "25"], capture_output=True, text=True, timeout=5)
        if java.returncode:
            raise RuntimeError("Для компиляции необходим установленный JDK 25")
        result["JAVA_HOME"] = java.stdout.strip()
    if not shutil.which("npm", path=result.get("PATH")):
        versions = list((Path.home() / ".nvm/versions/node").glob("v24.*/bin"))
        if versions:
            latest = max(versions, key=lambda path: tuple(int(part) for part in path.parent.name[1:].split(".")))
            result["PATH"] = str(latest) + os.pathsep + result.get("PATH", "")
    return result


def terminate(process):
    if process.poll() is None:
        os.killpg(process.pid, signal.SIGTERM)
        try:
            process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            os.killpg(process.pid, signal.SIGKILL)
            process.wait(timeout=5)


def execute(command, directory, log, environ, timeout=150):
    with log.open("w") as output:
        process = subprocess.Popen(command, cwd=directory, env=environ, stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
        try:
            code = process.wait(timeout=timeout)
        except subprocess.TimeoutExpired:
            terminate(process)
            output.write("\nПроверка остановлена: превышен тайм-аут\n")
            code = 124
        except BaseException:
            terminate(process)
            raise
    return code


def validate(root, modules):
    reports = []
    passed = True
    environ = environment(any(module != "rfr-gql-client" for module in modules))
    directory = root / ".codex/hook-state/logs"
    directory.mkdir(parents=True, exist_ok=True)
    for command, relative in commands(modules):
        log = directory / (uuid.uuid4().hex + ".log")
        try:
            code = execute(command, root / relative, log, environ)
        except OSError as error:
            log.write_text(str(error))
            code = 127
        passed = passed and code == 0
        output = log.read_text(errors="replace")
        preview = output if len(output) <= 10000 else output[:2000] + "\n[Часть вывода сохранена в журнале]\n" + output[-8000:]
        reports.append("Команда: " + " ".join(command) + " (" + relative + ")\nКод выхода: " + str(code) + "\nПолный журнал: " + str(log) + "\n" + preview)
    return passed, "\n\n".join(reports)


def feedback(event, message):
    return {"hookSpecificOutput": {"hookEventName": event, "additionalContext": message}}


def state(path):
    return json.loads(path.read_text()) if path.exists() else {"pending": {}, "checked": {}}


def process_event(payload, root, checker=validate):
    event = payload["hook_event_name"]
    session = hashlib.sha256(payload["session_id"].encode()).hexdigest()[:24]
    directory = root / ".codex/hook-state"
    directory.mkdir(parents=True, exist_ok=True)
    path = directory / (session + ".json")
    guard = directory / (session + ".lock")
    if event == "PreToolUse":
        with lock(guard):
            current = state(path)
            current["pending"][payload["tool_use_id"]] = snapshot(root)
            # Ограничиваем состояние при прерванных вызовах, не оставляющих PostToolUse.
            current["pending"] = dict(list(current["pending"].items())[-64:])
            path.write_text(json.dumps(current))
        return None
    if event != "PostToolUse":
        return None
    with lock(directory / "validation.lock"):
        with lock(guard):
            current = state(path)
            before = current["pending"].pop(payload["tool_use_id"], None)
            path.write_text(json.dumps(current))
        if before is None:
            return feedback(event, "Автопроверка не выполнена: отсутствует снимок PreToolUse для этого вызова; при изменении исходников запустите проверки вручную")
        after = snapshot(root)
        modules = [module for module in after if before.get(module) != after[module] and current["checked"].get(module) != after[module]]
        if not modules:
            return None
        passed, output = checker(root, modules)
        stable = snapshot(root) == after
        if stable:
            with lock(guard):
                current = state(path)
                current["checked"].update({module: after[module] for module in modules})
                path.write_text(json.dumps(current))
        status = "Проверки прошли" if passed else "Проверки завершились с ошибками; исправьте причину перед завершением задачи"
        if not stable:
            status += "; исходники изменились во время проверки, результат промежуточный — проверьте актуальную версию"
        return feedback(event, status + ". E2E-тесты не запускались\n\n" + output)


def interrupt(signum, frame):
    raise InterruptedError("Проверка прервана сигналом " + str(signum))


def main():
    signal.signal(signal.SIGTERM, interrupt)
    event = "PostToolUse"
    try:
        root = Path(__file__).resolve().parents[2]
        payload = json.load(sys.stdin)
        event = payload.get("hook_event_name", event)
        result = process_event(payload, root)
    except Exception as error:
        result = feedback(event, "Автопроверка не выполнена: " + str(error) + "; выполните профильные проверки вручную")
    if result:
        print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
