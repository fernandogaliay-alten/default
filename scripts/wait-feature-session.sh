#!/usr/bin/env bash
set -euo pipefail

# Espera (con tope) a que termine una sesion de feature-flow lanzada en background con
# `SAZON_DETACH=1` y reporta su codigo de salida.
#
# La senal de fin es la sentinela <worktree>/.feature-session.exit que escribe el lanzador al
# terminar. Si esta herramienta se cancela, no se pierde nada: la sesion esta desacoplada
# (setsid) y la sentinela se escribe igual; el orquestador puede reconciliar despues por
# artefactos (estado en feature_list.json + commit local).
#
# Uso:
#   bash scripts/wait-feature-session.sh <feature-id> [timeout-segundos]   (por defecto 3600)
#
# Salida:
#   0  la sesion termino (imprime SESION_TERMINADA y el exit real: 0 = ok)
#   1  se agoto el timeout (imprime TIMEOUT; reconcilia por artefactos)
#   2  uso incorrecto
#
# Variables de entorno: WORKTREES_ROOT (por defecto ../<carpeta-checkout>-worktrees)

target="${1:-}"
timeout_s="${2:-3600}"
if [ -z "$target" ]; then
  echo "uso: $0 <feature-id | prefijo/id> [timeout-segundos]" >&2
  exit 2
fi

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
  exit 1
fi

exit_file="$worktree_path/.feature-session.exit"
log_file="$worktree_path/.feature-session.log"

start=$SECONDS
while [ ! -f "$exit_file" ]; do
  if [ $((SECONDS - start)) -ge "$timeout_s" ]; then
    echo "TIMEOUT esperando la sesion de ${id} (${timeout_s}s); reconcilia por artefactos" >&2
    echo "log: $log_file" >&2
    exit 1
  fi
  sleep 15
done

rc="$(cat "$exit_file" 2>/dev/null || echo "?")"
echo "SESION_TERMINADA ${id} exit=${rc}"
echo "log: $log_file"
if [ "$rc" != "0" ]; then
  exit 1
fi
