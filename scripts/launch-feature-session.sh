#!/usr/bin/env bash
set -euo pipefail

# Lanza UNA sesion de feature-flow para una feature, dentro de su worktree, en primer plano.
#
# Existe para que la politica de arranque viva en un solo sitio. Antes estaba duplicada en
# AGENTS.md, .opencode/skills/github-flow/SKILL.md y references/evals.md, y se desincronizo
# con opencode v2 (que ya no tiene `--dir`): el directorio de la sesion es el cwd del proceso.
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
#
# El llamante debe ejecutarlo en PRIMER PLANO y con el timeout de su herramienta desactivado
# (la sesion dura mucho mas que un timeout por defecto). No usar background/detach.

target="${1:-}"
if [ -z "$target" ]; then
  echo "uso: $0 <feature-id | prefijo/id> [prompt-extra]" >&2
  exit 2
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

prompt="Use feature-flow for ${id} only, until accepted. Do not push and do not open a PR; the orchestrator publishes."
if [ -n "${2:-}" ]; then
  prompt="$prompt $2"
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
exec env NODE_OPTIONS="${NODE_OPTIONS:+$NODE_OPTIONS }--dns-result-order=ipv4first" \
  "${SAZON_OPENCODE:-opencode}" run "${extra[@]}" "$prompt"
