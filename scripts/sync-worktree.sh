#!/usr/bin/env bash
set -euo pipefail

# Integra origin/<base-branch> en la rama de un worktree de feature mediante
# un merge (nunca rebase ni force-push). El orquestador es el unico integrador
# de los artefactos compartidos (feature_list.json, PROGRESS.md, docs, ...).
#
# Uso: scripts/sync-worktree.sh <feature-id> [base-branch]
#   <feature-id>   id de la feature (worktree en ../<checkout>-worktrees/<id>)
#   [base-branch]  rama base (por defecto: main)
#
# Variables de entorno opcionales:
#   BASE_BRANCH    rama base (por defecto: main)
#   WORKTREES_ROOT directorio de worktrees (por defecto: ../<carpeta-checkout>-worktrees)
#
# Salida:
#   0  sincronizado (sin conflictos o merge ya al dia)
#   1  hay conflictos: quedan sin resolver en el worktree para que los resuelvas
#      (resuelve, `git add`, `git commit`; o `git merge --abort` para deshacer)
#   3  el worktree no esta en la rama de la feature (detached u otra): no se toca nada

BASE_BRANCH="${2:-${BASE_BRANCH:-main}}"

feature_id="${1:-}"
if [ -z "$feature_id" ]; then
  echo "uso: $0 <feature-id> [base-branch]" >&2
  exit 2
fi

repo_root="$(git rev-parse --show-toplevel)"
base_dirname="$(basename "$repo_root")"
worktrees_root="${WORKTREES_ROOT:-$(dirname "$repo_root")/${base_dirname}-worktrees}"
worktree_path="${worktrees_root}/${feature_id}"

if [ ! -d "$worktree_path" ]; then
  echo "no existe el worktree: $worktree_path" >&2
  exit 1
fi

# Guarda: el worktree debe estar en la rama de la feature. Evita mergear
# origin/<base> sobre un HEAD detached o de otra rama (nunca se toca nada).
branch="feature/${feature_id}"
current_branch="$(git -C "$worktree_path" symbolic-ref --quiet --short HEAD 2>/dev/null || echo '')"
if [ "$current_branch" != "$branch" ]; then
  echo "SALTADO: ${worktree_path} no esta en ${branch} (esta en '${current_branch:-detached}'); no se toca nada" >&2
  exit 3
fi

echo "fetch origin ${BASE_BRANCH}..."
git -C "$repo_root" fetch --quiet origin "${BASE_BRANCH}"

if git -C "$worktree_path" merge --no-edit "origin/${BASE_BRANCH}"; then
  echo "sincronizado: ${feature_id} al dia con origin/${BASE_BRANCH}"
  exit 0
else
  echo "CONFLICTOS al integrar origin/${BASE_BRANCH} en ${feature_id}:" >&2
  git -C "$worktree_path" diff --name-only --diff-filter=U >&2 || true
  echo "resuelve los ficheros, luego: git -C \"$worktree_path\" add <ficheros> && git -C \"$worktree_path\" commit" >&2
  echo "para deshacer: git -C \"$worktree_path\" merge --abort" >&2
  exit 1
fi
