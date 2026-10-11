#!/usr/bin/env bash
set -euo pipefail

# Elimina el worktree y la rama local de una feature cuyo PR ya esta mergeado
# en la rama base. Es la primitiva del modo `prune` de github-flow: solo toca
# features mergeadas, nunca features en curso.
#
# Uso: scripts/cleanup-worktree.sh <feature-id> [base-branch] [--dry-run]
#   <feature-id>   id de la feature (worktree en ../<checkout>-worktrees/<id>)
#   [base-branch]  rama base (por defecto: main)
#   --dry-run      informa lo que haria sin eliminar nada
#
# Variables de entorno opcionales:
#   BASE_BRANCH    rama base (por defecto: main)
#   WORKTREES_ROOT directorio de worktrees (por defecto: ../<carpeta-checkout>-worktrees)
#
# Salida:
#   0  limpio (eliminado, ya no existia, o dry-run)
#   1  saltado por guardas (no mergeada, cambios sin commitear, ...)
#   2  uso incorrecto

dry_run=0
args=()
for a in "$@"; do
  case "$a" in
    --dry-run) dry_run=1 ;;
    *) args+=("$a") ;;
  esac
done

target="${args[0]:-}"
if [ -z "$target" ]; then
  echo "uso: $0 <feature-id | prefijo/id> [base-branch] [--dry-run]" >&2
  exit 2
fi
base_branch="${args[1]:-${BASE_BRANCH:-main}}"

# Raiz del checkout base, robusta aunque se ejecute desde un worktree (git common dir).
repo_root="$(cd "$(dirname "$(git rev-parse --path-format=absolute --git-common-dir)")" && pwd)"
base_dirname="$(basename "$repo_root")"
worktrees_root="${WORKTREES_ROOT:-$(dirname "$repo_root")/${base_dirname}-worktrees}"

case "$target" in
  */*) prefix="${target%%/*}"; id="${target#*/}" ;;
  *)   prefix="feature"; id="$target" ;;
esac
branch="${prefix}/${id}"
if [ "$prefix" = "feature" ]; then dir_name="$id"; else dir_name="${prefix}-${id}"; fi
worktree_path="${worktrees_root}/${dir_name}"

# La rama base debe estar al dia para comprobar la integracion.
if git remote get-url origin >/dev/null 2>&1; then
  git -C "$repo_root" fetch --quiet origin "$base_branch" || echo "AVISO: fetch fallo; se usa origin/$base_branch local" >&2
fi
if git -C "$repo_root" rev-parse --verify --quiet "origin/${base_branch}" >/dev/null; then
  base_ref="origin/${base_branch}"
else
  base_ref="${base_branch}"
fi

# Idempotente: sin worktree ni rama no hay nada que hacer.
if [ ! -d "$worktree_path" ] && ! git -C "$repo_root" show-ref --verify --quiet "refs/heads/${branch}"; then
  echo "nada que limpiar: ${branch} no tiene worktree ni rama"
  exit 0
fi

# Guarda 1: si el worktree existe, debe estar en la rama esperada. Evita
# borrar un worktree que apunta a otra rama (trabajo no mergeado).
if [ -d "$worktree_path" ]; then
  current_branch="$(git -C "$worktree_path" symbolic-ref --quiet --short HEAD 2>/dev/null || echo '')"
  if [ "$current_branch" != "$branch" ]; then
    echo "SALTADO: ${worktree_path} no esta en ${branch} (esta en '${current_branch:-detached}')" >&2
    exit 1
  fi
fi

# Guarda 2: la rama debe estar totalmente contenida en la base (mergeada).
if ! git -C "$repo_root" merge-base --is-ancestor "$branch" "$base_ref" 2>/dev/null; then
  echo "SALTADO: ${branch} no esta mergeada en ${base_ref}" >&2
  exit 1
fi

# Guarda 3: sin cambios sin commitear en el worktree.
if [ -d "$worktree_path" ] && [ -n "$(git -C "$worktree_path" status --porcelain)" ]; then
  echo "SALTADO: ${worktree_path} tiene cambios sin commitear" >&2
  exit 1
fi

if [ "$dry_run" -eq 1 ]; then
  echo "dry-run: eliminaria worktree ${worktree_path} y rama ${branch}"
  exit 0
fi

if [ -d "$worktree_path" ]; then
  git -c core.longpaths=true -C "$repo_root" worktree remove "$worktree_path"
  echo "worktree eliminado: $worktree_path"
fi

if git -C "$repo_root" show-ref --verify --quiet "refs/heads/${branch}"; then
  # La contención en ${base_ref} ya se verifico arriba, asi que -D es seguro
  # aunque main local vaya por detras de origin.
  git -C "$repo_root" branch -D "$branch" >/dev/null
  echo "rama eliminada: ${branch}"
fi

echo "limpio: ${branch}"
