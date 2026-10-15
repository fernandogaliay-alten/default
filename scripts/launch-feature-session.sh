#!/usr/bin/env bash
set -euo pipefail

# Lanza UNA sesion de feature-flow para una feature, dentro de su worktree.
#
# Existe para que la politica de arranque viva en un solo sitio. Antes estaba duplicada en
# AGENTS.md, .opencode/skills/github-flow/SKILL.md y references/evals.md, y se desincronizo
# con opencode v2 (que ya no tiene `--dir`): el directorio de la sesion es el cwd del proceso.
#
# Dos modos:
#   - Primer plano (por defecto): corre `opencode run ...` y NO retorna hasta que el ciclo termina.
#     Comodo para una persona, pero para un agente es fragil: una tool call tan larga (30-60+ min)
#     puede cancelarse, el orquestador pierde el retorno y se queda ciego (aunque el trabajo siga
#     vivo en el servidor de opencode).
#   - Background (`SAZON_DETACH=1`): lanza el ciclo DESACOPLADO del proceso llamante (setsid) y
#     RETORNA al instante. La salida va a <worktree>/.feature-session.log y el codigo de salida a
#     <worktree>/.feature-session.exit cuando termina. La senal de fin real es esa sentinela (y los
#     artefactos: estado en feature_list.json + commit local), no el retorno de la tool call. Ver
#     scripts/wait-feature-session.sh.
#
# Uso:
#   bash scripts/launch-feature-session.sh <feature-id> [prompt-extra]
#
#   <feature-id>    id de la feature (worktree en <root>/<feature-id> o <root>/<prefijo>-<id>)
#   [prompt-extra]  texto que se anade al prompt estandar (opcional)
#
# Variables de entorno opcionales:
#   WORKTREES_ROOT   directorio de worktrees (por defecto ../<carpeta-checkout>-worktrees)
#   SAZON_MODEL      modelo a pasar con -m; vacio = usar el configurado (por defecto: vacio)
#   SAZON_OPENCODE   binario de opencode (por defecto: opencode)
#   SAZON_NO_AUTO    1 para no pasar --auto (por defecto: se pasa)
#   SAZON_DETACH     1 para lanzar en background y escribir la sentinela de salida (por defecto: 0)
#   SAZON_TARGET      (interno) id de la feature que reusa el modo __run
#   SAZON_PROMPT_EXTRA (interno) prompt-extra que reusa el modo __run

script_path="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/$(basename "${BASH_SOURCE[0]}")"

# --- modo interno (re-invocado por el lanzamiento en background): corre el ciclo y sale ---
if [ "${1:-}" = "__run" ]; then
  target="${SAZON_TARGET:?falta SAZON_TARGET}"
  extra_prompt="${SAZON_PROMPT_EXTRA:-}"
else
  target="${1:-}"
  extra_prompt="${2:-}"
  if [ -z "$target" ]; then
    echo "uso: $0 <feature-id | prefijo/id> [prompt-extra]" >&2
    exit 2
  fi
fi

# Raiz del checkout base, robusta aunque se ejecute desde un worktree (git common dir).
repo_root="$(cd "$(dirname "$(git rev-parse --path-format=absolute --git-common-dir)")" && pwd)"
base_dirname="$(basename "$repo_root")"
worktrees_root="${WORKTREES_ROOT:-$(dirname "$repo_root")/${base_dirname}-worktrees}"

case "$target" in
  */*) prefix="${target%%/*}"; id="${target#*/}" ;;
  *)   prefix="feature"; id="$target" ;;
esac
if [ "$prefix" = "feature" ]; then dir_name="$id"; else dir_name="${prefix}-${id}"; fi
worktree_path="${worktrees_root}/${dir_name}"

if [ ! -d "$worktree_path" ]; then
  echo "no existe el worktree: $worktree_path" >&2
  echo "crea el worktree primero con: bash scripts/new-worktree.sh $target" >&2
  exit 1
fi

# --- lanzamiento en background (desacoplado): log + sentinela de salida, y retorna ya ---
# El chequeo `$1 != __run` evita recursion: el hijo se re-invoca con `__run` y no vuelve a detach.
if [ "${1:-}" != "__run" ] && [ "${SAZON_DETACH:-0}" = "1" ]; then
  log_file="$worktree_path/.feature-session.log"
  exit_file="$worktree_path/.feature-session.exit"
  pid_file="$worktree_path/.feature-session.pid"
  rm -f "$exit_file"
  : > "$log_file"
  SAZON_TARGET="$target" SAZON_PROMPT_EXTRA="$extra_prompt" \
    setsid bash -c 'rc=0; "$0" __run >"$1" 2>&1 || rc=$?; echo "$rc" > "$2"' \
      "$script_path" "$log_file" "$exit_file" </dev/null >/dev/null 2>&1 &
  child_pid=$!
  echo "$child_pid" > "$pid_file"
  echo "feature-session lanzada en background (feature ${id})"
  echo "  worktree: $worktree_path"
  echo "  pid:      $child_pid"
  echo "  log:      $log_file"
  echo "  salida:   $exit_file (se escribe al terminar; ver scripts/wait-feature-session.sh)"
  exit 0
fi

# --- primer plano ---
prompt="Use feature-flow for ${id} only, until accepted. Do not push and do not open a PR; the orchestrator publishes."
if [ -n "$extra_prompt" ]; then
  prompt="$prompt $extra_prompt"
fi

extra=()
if [ -n "${SAZON_MODEL:-}" ]; then
  extra+=(-m "$SAZON_MODEL")
fi
if [ "${SAZON_NO_AUTO:-0}" != "1" ]; then
  extra+=(--auto)
fi
extra+=(--title "[${id}] feature-flow")

# opencode v2 no tiene --dir: el directorio de la sesion es el cwd del proceso.
# DNS: algunos hosts resuelven IPv6-first y el lookup se cuelga (`getaddrinfo ETIMEOUT`); preferir IPv4.
cd "$worktree_path"
echo "opencode run en $worktree_path (feature ${id})"
if [ "${1:-}" = "__run" ]; then
  # Re-invocado por el modo background: corre sin `exec` para que el hijo registre el exit code.
  env NODE_OPTIONS="${NODE_OPTIONS:+$NODE_OPTIONS }--dns-result-order=ipv4first" \
    "${SAZON_OPENCODE:-opencode}" run "${extra[@]}" "$prompt"
  exit $?
else
  exec env NODE_OPTIONS="${NODE_OPTIONS:+$NODE_OPTIONS }--dns-result-order=ipv4first" \
    "${SAZON_OPENCODE:-opencode}" run "${extra[@]}" "$prompt"
fi
