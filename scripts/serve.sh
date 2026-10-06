#!/usr/bin/env bash
set -euo pipefail

# Arranca un backend opencode COMPARTIDO para poder monitorear en vivo (TUI/web)
# las sesiones que lanza el orquestador de github-flow.
#
# Uso: scripts/serve.sh [puerto]        (por defecto 4096)
#
# En otra terminal, para ver las sesiones en vivo:
#   opencode attach http://localhost:<puerto>
#
# El orquestador lanza cada feature con:
#   opencode run --attach http://localhost:<puerto> --dir <worktree> ...

PORT="${1:-${OPENCODE_SERVER_PORT:-4096}}"
echo "backend opencode en http://localhost:${PORT} (Ctrl+C para parar)"
exec opencode serve --port "$PORT"
