#!/bin/bash
# PostToolUse: Write/Edit file paths and apply_patch headers.
root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd) || exit 2
files=$(jq -r '
  .tool_input as $input |
  [($input.file_path? // .tool_response.filePath? // empty),
   (($input | if type == "string" then . else .patch // .input // .command // "" end)
    | split("\n")[]
    | select(test("^\\*\\*\\* (Add File|Update File|Delete File|Move to): "))
    | sub("^\\*\\*\\* (Add File|Update File|Delete File|Move to): "; ""))]
  | unique[]
') || exit 2

if ! command -v node >/dev/null 2>&1; then
  for node_bin in "$HOME"/.nvm/versions/node/v24.*/bin; do
    [[ -x "$node_bin/node" ]] && export PATH="$node_bin:$PATH"
  done
fi

checked=" "
report=""
while IFS= read -r file; do
  rel="${file#"$root"/}"
  rel="${rel#./}"
  dir="$root"
  case "$rel" in
    rfr-auth/src/*.java|rfr-api/src/*.java|rfr-e2e/src/*.java)
      label="${rel%%/*}"
      cmd=(./gradlew -q ":$label:compileTestJava")
      ;;
    rfr-gql-client/src/*.ts|rfr-gql-client/src/*.tsx)
      label=rfr-gql-client
      dir="$root/$label"
      frontend_file="${rel#rfr-gql-client/}"
      cmd=(./node_modules/.bin/tsc --noEmit)
      ;;
    *) continue ;;
  esac
  if [[ "$label" != rfr-gql-client && -x /usr/libexec/java_home ]]; then
    JAVA_HOME=$(/usr/libexec/java_home -v 25) || exit 2
    export JAVA_HOME
  fi
  if output=$(
    {
      cd "$dir" || exit 1

      if [[ "$label" == rfr-gql-client && -f "$frontend_file" ]]; then
        ./node_modules/.bin/eslint "$frontend_file" \
          --report-unused-disable-directives \
          --max-warnings 0 || exit $?
      fi

      if [[ "$checked" != *" $label "* ]]; then
        "${cmd[@]}"
      fi
    } 2>&1
  ); then
    checked+="$label "
    report+="Compilation of $label passed"$'\n'
  else
    printf 'Compilation of %s failed after editing %s:\n' "$label" "$rel" >&2
    printf '%s\n' "$output" | tail -60 >&2
    exit 2
  fi
done <<< "$files"

[[ -z "$report" ]] && exit 0
jq -n --arg msg "$report" \
  '{hookSpecificOutput: {hookEventName: "PostToolUse", additionalContext: $msg}}'
