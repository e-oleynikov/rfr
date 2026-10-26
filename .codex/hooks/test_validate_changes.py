"""Проверяет выбор компиляции, обратную связь и отсутствие повторных запусков hook."""

from concurrent.futures import ThreadPoolExecutor
import importlib.util
import os
from pathlib import Path
import sys
import tempfile
import unittest

spec = importlib.util.spec_from_file_location("validate_changes", Path(__file__).with_name("validate_changes.py"))
hook = importlib.util.module_from_spec(spec)
spec.loader.exec_module(hook)


def workspace():
    root = Path(tempfile.mkdtemp(prefix="rfr-hook-check-"))
    for module in ("rfr-auth", "rfr-api", "rfr-e2e"):
        directory = root / module / "src/test/java"
        directory.mkdir(parents=True)
        (directory / "Example.java").write_text("class Example {}")
    (root / "rfr-gql-client/src").mkdir(parents=True)
    (root / "rfr-gql-client/src/example.ts").write_text("export const value = 17")
    return root


def event(name, tool="edit-39"):
    return {"hook_event_name": name, "session_id": "session-73", "tool_use_id": tool}


class ValidationTest(unittest.TestCase):
    """Проверяет наблюдаемое поведение hook на временных исходниках."""

    def test_java_edit_checks_only_the_changed_module(self):
        root = workspace()
        calls = []
        hook.process_event(event("PreToolUse"), root)
        (root / "rfr-api/src/test/java/Example.java").write_text("class Example { int value = 23; }")
        hook.process_event(event("PostToolUse"), root, lambda root, modules: (calls.append(modules) is None, "compiled"))
        self.assertEqual(calls, [["rfr-api"]])

    def test_frontend_edit_runs_lint_and_build(self):
        root = workspace()
        calls = []
        hook.process_event(event("PreToolUse"), root)
        (root / "rfr-gql-client/src/example.ts").write_text("export const value = 41")
        hook.process_event(event("PostToolUse"), root, lambda root, modules: (calls.extend(hook.commands(modules)) is None, "built"))
        self.assertEqual(calls, [(["npm", "run", "lint"], "rfr-gql-client"), (["npm", "run", "build"], "rfr-gql-client")])

    def test_unchanged_sources_do_not_run_checks(self):
        root = workspace()
        calls = []
        hook.process_event(event("PreToolUse"), root)
        hook.process_event(event("PostToolUse"), root, lambda root, modules: (calls.append(modules) is None, "unexpected"))
        self.assertEqual(calls, [])

    def test_generated_java_does_not_run_checks(self):
        root = workspace()
        calls = []
        hook.process_event(event("PreToolUse"), root)
        directory = root / "rfr-api/build/generated"
        directory.mkdir(parents=True)
        (directory / "Generated.java").write_text("class Generated {}")
        hook.process_event(event("PostToolUse"), root, lambda root, modules: (calls.append(modules) is None, "unexpected"))
        self.assertEqual(calls, [])

    def test_frontend_dependencies_do_not_trigger_checks(self):
        root = workspace()
        calls = []
        hook.process_event(event("PreToolUse"), root)
        directory = root / "rfr-gql-client/node_modules/sample"
        directory.mkdir(parents=True)
        (directory / "index.js").write_text("exports.value = 91")
        hook.process_event(event("PostToolUse"), root, lambda root, modules: (calls.append(modules) is None, "unexpected"))
        self.assertEqual(calls, [])

    def test_moving_a_java_source_to_backup_triggers_compilation(self):
        root = workspace()
        calls = []
        hook.process_event(event("PreToolUse"), root)
        (root / "rfr-auth/src/test/java/Example.java").rename(root / "rfr-auth/src/test/java/.Example.java.73.bak")
        hook.process_event(event("PostToolUse"), root, lambda root, modules: (calls.append(modules) is None, "compiled"))
        self.assertEqual(calls, [["rfr-auth"]])

    def test_failed_compilation_returns_output_to_the_agent(self):
        root = workspace()
        hook.process_event(event("PreToolUse"), root)
        (root / "rfr-e2e/src/test/java/Example.java").write_text("invalid java")
        output = hook.process_event(event("PostToolUse"), root, lambda root, modules: (False, "compiler: missing symbol Widget73"))
        self.assertIn("missing symbol Widget73", output["hookSpecificOutput"]["additionalContext"])

    def test_concurrent_tools_do_not_compile_the_same_sources_twice(self):
        root = workspace()
        calls = []
        for tool in ("edit-17", "edit-91"):
            hook.process_event(event("PreToolUse", tool), root)
        (root / "rfr-api/src/test/java/Example.java").write_text("class Updated {}")
        with ThreadPoolExecutor(max_workers=2) as executor:
            futures = [executor.submit(hook.process_event, event("PostToolUse", tool), root, lambda root, modules: (calls.append(modules) is None, "compiled")) for tool in ("edit-17", "edit-91")]
            for future in futures:
                future.result(timeout=5)
        self.assertEqual(calls, [["rfr-api"]])

    def test_a_new_edit_after_failure_is_checked_again(self):
        root = workspace()
        calls = []
        for value in ("broken", "class Repaired {}"):
            hook.process_event(event("PreToolUse"), root)
            (root / "rfr-api/src/test/java/Example.java").write_text(value)
            hook.process_event(event("PostToolUse"), root, lambda root, modules: (False, str(calls.append(modules))))
        self.assertEqual(len(calls), 2)

    def test_e2e_checks_compile_without_running_tests(self):
        plan = hook.commands(["rfr-e2e"])
        self.assertEqual(plan, [(["./gradlew", ":rfr-e2e:compileTestJava", "--console=plain"], ".")])

    def test_source_changes_during_compilation_are_reported_as_intermediate(self):
        root = workspace()
        hook.process_event(event("PreToolUse"), root)
        source = root / "rfr-api/src/test/java/Example.java"
        source.write_text("class Updated {}")
        output = hook.process_event(event("PostToolUse"), root, lambda root, modules: (source.write_text("class ChangedAgain {}") > 0, "compiled"))
        self.assertIn("результат промежуточный", output["hookSpecificOutput"]["additionalContext"])

    def test_a_process_failure_is_preserved_in_the_full_log(self):
        root = workspace()
        log = root / "check.log"
        code = hook.execute([sys.executable, "-c", "import sys; print('diagnostic-47'); sys.exit(7)"], root, log, dict(os.environ), timeout=5)
        self.assertEqual((code, log.read_text().strip()), (7, "diagnostic-47"))

    def test_a_slow_check_is_stopped_with_a_timeout(self):
        root = workspace()
        code = hook.execute([sys.executable, "-c", "import time; time.sleep(30)"], root, root / "timeout.log", dict(os.environ), timeout=0.1)
        self.assertEqual(code, 124)


if __name__ == "__main__":
    unittest.main()
