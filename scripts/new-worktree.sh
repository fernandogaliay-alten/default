#!/usr/bin/env bash
set -euo pipefail

# Crea (o reutiliza) un worktree git para una feature, fuera del checkout base,
# y lo siembra con local.properties para que Gradle encuentre el SDK de Android.
#
# Uso: scripts/new-worktree.sh <feature-id> [base-sha]
#   <feature-id>  id de la feature (worktree y rama pasan a feature/<feature-id>)
#   [base-sha]    commit desde el que ramificar; por defecto origin/<base-branch>
#
# Variables de entorno opcionales:
#   BASE_BRANCH    rama base (por defecto: main)
#   WORKTREES_ROOT directorio de worktrees (por defecto: ../<carpeta-checkout>-worktrees)

BASE_BRANCH="${BASE_BRANCH:-main}"

feature_id="${1:-}"
if [ -z "$feature_id" ]; then
  echo "uso: $0 <feature-id> [base-sha]" >&2
  exit 2
fi

repo_root="$(git rev-parse --show-toplevel)"
base_dirname="$(basename "$repo_root")"
worktrees_root="${WORKTREES_ROOT:-$(dirname "$repo_root")/${base_dirname}-worktrees}"
worktree_path="${worktrees_root}/${feature_id}"
branch="feature/${feature_id}"

if [ "$#" -ge 2 ] && [ -n "${2:-}" ]; then
  # SHA explicito: el llamante ya es responsable de haber fetcheado.
  base_sha="$2"
else
  # Sin SHA explicito: fetchea origin para no ramificar de un origin/<base> obsoleto.
  if git remote get-url origin >/dev/null 2>&1; then
    echo "fetch origin ${BASE_BRANCH}..."
    git fetch --quiet origin "${BASE_BRANCH}" || echo "AVISO: fetch fallo; se usa origin/${BASE_BRANCH} local" >&2
  fi
  if git rev-parse --verify --quiet "origin/${BASE_BRANCH}" >/dev/null; then
    base_sha="$(git rev-parse "origin/${BASE_BRANCH}")"
  else
    base_sha="$(git rev-parse "${BASE_BRANCH}")"
  fi
fi

mkdir -p "$worktrees_root"

if [ -d "$worktree_path" ]; then
  echo "worktree ya existe: $worktree_path"
elif git show-ref --verify --quiet "refs/heads/${branch}"; then
  git worktree add "$worktree_path" "$branch"
else
  git worktree add "$worktree_path" -b "$branch" "$base_sha"
fi

# local.properties esta ignorado en git y no viaja al worktree.
if [ -f "${repo_root}/local.properties" ]; then
  cp "${repo_root}/local.properties" "${worktree_path}/local.properties"
  echo "local.properties copiado a $worktree_path"
elif [ -n "${ANDROID_HOME:-}" ]; then
  echo "sin local.properties; Gradle usara ANDROID_HOME=${ANDROID_HOME}"
else
  echo "AVISO: no hay local.properties ni ANDROID_HOME; el build puede fallar" >&2
fi

echo "worktree: $worktree_path"
echo "branch:   ${branch} @ $(git -C "$worktree_path" rev-parse --short HEAD)"

# Aviso de desincronizacion respecto a la base.
if git rev-parse --verify --quiet "origin/${BASE_BRANCH}" >/dev/null; then
  behind="$(git -C "$worktree_path" rev-list --count "HEAD..origin/${BASE_BRANCH}" 2>/dev/null || echo 0)"
  if [ "${behind:-0}" != "0" ]; then
    echo "AVISO: la rama va ${behind} commit(s) por detras de origin/${BASE_BRANCH}; revisa antes de mergear" >&2
  fi
fi
