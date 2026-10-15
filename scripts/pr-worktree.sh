#!/usr/bin/env bash
set -euo pipefail

# En WSL publica el JDK y el SDK de Linux (ver scripts/wsl-env.sh). Fuera de WSL no hace nada.
# shellcheck source=scripts/wsl-env.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/scripts/wsl-env.sh"

# Asegura un worktree con la rama head de un pull request abierto, fuera del
# checkout base, para que pr-flow pueda revisar, resolver conflictos y reparar
# sin tocar el checkout base. Reutiliza el worktree existente si ya esta.
#
# Uso: scripts/pr-worktree.sh <pr-number | feature-id> [base-branch]
#   <pr-number>    numero del PR (se consulta su headRefName con gh)
#   <feature-id>   id de la feature (rama feature/<id>)
#   [base-branch]  rama base del PR (por defecto: main)
#
# Variables de entorno opcionales:
#   BASE_BRANCH    rama base (por defecto: main)
#   WORKTREES_ROOT directorio de worktrees (por defecto: ../<carpeta-checkout>-worktrees)
#
# Salida (stdout, una linea al final): la ruta del worktree.
#   0  worktree listo (existente o creado)
#   1  no se puede: PR no encontrado, rama fork, o rama sin ref remoto
#   2  uso incorrecto

BASE_BRANCH="${2:-${BASE_BRANCH:-main}}"

target="${1:-}"
if [ -z "$target" ]; then
  echo "uso: $0 <pr-number | feature-id> [base-branch]" >&2
  exit 2
fi

# Raiz del checkout base, robusta aunque se ejecute desde un worktree (git common dir).
repo_root="$(cd "$(dirname "$(git rev-parse --path-format=absolute --git-common-dir)")" && pwd)"
base_dirname="$(basename "$repo_root")"
worktrees_root="${WORKTREES_ROOT:-$(dirname "$repo_root")/${base_dirname}-worktrees}"

branch=""
if [[ "$target" =~ ^[0-9]+$ ]]; then
  # Numero de PR: resolver headRefName, owner, state y draft con gh.
  if ! command -v gh >/dev/null 2>&1; then
    echo "gh no esta disponible y se paso un numero de PR" >&2
    exit 1
  fi
  info="$(gh pr view "$target" \
    --json headRefName,headRepositoryOwner,state,isDraft \
    --jq '[.headRefName, .headRepositoryOwner.login, .state, (.isDraft|tostring)] | @tsv' 2>/dev/null || true)"
  if [ -z "$info" ]; then
    echo "no se encontro el PR #${target}" >&2
    exit 1
  fi
  IFS=$'\t' read -r branch owner state draft <<< "$info"
  if [ -z "$branch" ]; then
    echo "no se pudo leer headRefName del PR #${target}" >&2
    exit 1
  fi
  me="$(gh api user --jq .login 2>/dev/null || echo '')"
  if [ -n "$owner" ] && [ -n "$me" ] && [ "$owner" != "$me" ]; then
    echo "PR #${target} viene de un fork (${owner}); pr-flow no lo soporta todavia" >&2
    exit 1
  fi
  echo "PR #${target}: head=${branch} state=${state:-?} draft=${draft:-false}" >&2
else
  case "$target" in
    */*) branch="$target" ;;
    *)   branch="feature/${target}" ;;
  esac
fi

case "$branch" in
  */*) prefix="${branch%%/*}"; id="${branch#*/}" ;;
  *)   echo "rama head sin prefijo: ${branch}" >&2; exit 1 ;;
esac
if [ "$prefix" = "feature" ]; then dir_name="$id"; else dir_name="${prefix}-${id}"; fi
worktree_path="${worktrees_root}/${dir_name}"

if [ -d "$worktree_path" ]; then
  echo "worktree ya existe: $worktree_path" >&2
  echo "$worktree_path"
  exit 0
fi

mkdir -p "$worktrees_root"

if git remote get-url origin >/dev/null 2>&1; then
  echo "fetch origin ${branch}..." >&2
  git fetch --quiet origin "${branch}" || echo "AVISO: fetch de ${branch} fallo" >&2
fi

if git show-ref --verify --quiet "refs/heads/${branch}"; then
  git worktree add "$worktree_path" "$branch" >&2
elif git rev-parse --verify --quiet "refs/remotes/origin/${branch}" >/dev/null; then
  git worktree add "$worktree_path" -b "$branch" "origin/${branch}" >&2
else
  echo "no existe ref local ni remoto para ${branch}" >&2
  exit 1
fi

# local.properties esta ignorado en git y no viaja al worktree.
if [ -f "${repo_root}/local.properties" ]; then
  cp "${repo_root}/local.properties" "${worktree_path}/local.properties"
  echo "local.properties copiado a $worktree_path" >&2
fi

echo "$worktree_path"
