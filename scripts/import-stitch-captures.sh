#!/usr/bin/env bash
set -euo pipefail

# Importa las exportaciones de Stitch de una feature desde una ruta EXTERNA al worktree de la
# feature, en docs/design/concepts/<feature-id>/<pantalla>/. Asi la persona no tiene que copiar
# los ficheros a mano a la ruta del proyecto: pasa la ruta y el orquestador los lee de ahi.
#
# Uso:
#   bash scripts/import-stitch-captures.sh <feature-id> <ruta-origen> [--screen <pantalla>]
#
#   <feature-id>   id de la feature (worktree en <root>/<id>)
#   <ruta-origen>  carpeta con los exports: o bien subcarpetas por pantalla
#                  (<pantalla>/ con .png + .html + .md), o los ficheros sueltos de UNA pantalla
#   --screen <n>   subcarpeta de pantalla a usar cuando el origen son ficheros sueltos
#
# Acepta rutas de WSL (/mnt/c/...) y de Windows (C:\... o C:/...); convierte estas ultimas.
#
# Salida:
#   0  importado y validado (todas las pantallas esperadas tienen sus tres ficheros)
#   1  importado pero faltan ficheros en alguna pantalla esperada
#   2  uso incorrecto, origen inexistente o worktree ausente

usage() {
  echo "uso: $0 <feature-id> <ruta-origen> [--screen <pantalla>]" >&2
  exit 2
}

feature_id="${1:-}"
source_path="${2:-}"
[ -n "$feature_id" ] && [ -n "$source_path" ] || usage
shift 2 || usage

screen=""
while [ "$#" -gt 0 ]; do
  case "$1" in
    --screen) screen="${2:-}"; shift 2 ;;
    *) echo "argumento desconocido: $1" >&2; usage ;;
  esac
done

# Ruta de Windows -> WSL.
case "$source_path" in
  [A-Za-z]:\\*|[A-Za-z]:/*)
    drive="$(printf '%s' "$source_path" | cut -c1 | tr 'A-Z' 'a-z')"
    rest="$(printf '%s' "$source_path" | cut -c3- | tr '\\' '/')"
    source_path="/mnt/${drive}${rest}"
    ;;
esac

[ -d "$source_path" ] || { echo "no existe la ruta de origen: $source_path" >&2; exit 2; }

# Raiz del checkout base, robusta desde un worktree (git common dir).
repo_root="$(cd "$(dirname "$(git rev-parse --path-format=absolute --git-common-dir)")" && pwd)"
base_dirname="$(basename "$repo_root")"
worktrees_root="${WORKTREES_ROOT:-$(dirname "$repo_root")/${base_dirname}-worktrees}"
worktree_path="${worktrees_root}/${feature_id}"
[ -d "$worktree_path" ] || {
  echo "no existe el worktree: $worktree_path" >&2
  echo "crea el worktree primero con: bash scripts/new-worktree.sh $feature_id" >&2
  exit 2
}

target="${worktree_path}/docs/design/concepts/${feature_id}"
mkdir -p "$target"

# Una carpeta es candidata a pantalla si trae algun fichero de export (png/html/md); la validacion
# posterior informa de los que falten.
is_screen_dir() {
  local d="$1" f
  for f in "$d"/*; do
    [ -f "$f" ] || continue
    case "$f" in *.png|*.PNG|*.html|*.HTML|*.md|*.MD) return 0 ;; esac
  done
  return 1
}

# Pantallas esperadas segun el README de peticion (bullets "- `<pantalla>/` — ...").
expected=()
if [ -f "$target/README.md" ]; then
  while IFS= read -r s; do [ -n "$s" ] && expected+=("$s"); done \
    < <(sed -nE 's/^- `([^`]+)\/`.*/\1/p' "$target/README.md")
fi

# 1) subcarpetas que parecen export de pantalla.
imported=0
for d in "$source_path"/*/; do
  [ -d "$d" ] || continue
  if is_screen_dir "$d"; then
    name="$(basename "$d")"
    mkdir -p "$target/$name"
    cp -f "$d"/* "$target/$name"/
    echo "importado: ${name}/  <- ${d}"
    imported=1
  fi
done

# 2) ficheros sueltos -> una sola pantalla.
if [ "$imported" -eq 0 ]; then
  if [ -z "$screen" ]; then
    if [ "${#expected[@]}" -eq 1 ]; then
      screen="${expected[0]}"
    elif [ "${#expected[@]}" -eq 0 ]; then
      echo "el origen no tiene subcarpetas de pantalla y no hay README de peticion; indica --screen <pantalla>" >&2
      exit 2
    else
      echo "el origen no tiene subcarpetas de pantalla y hay varias pantallas esperadas (${expected[*]}); indica --screen <pantalla>" >&2
      exit 2
    fi
  fi
  mkdir -p "$target/$screen"
  cp -f "$source_path"/* "$target/$screen"/
  echo "importado: ${screen}/  <- ${source_path} (ficheros sueltos)"
fi

# Validacion.
status=0
has_ext() {
  local d="$1" ext="$2" f
  for f in "$d"/*."$ext" "$d"/*."$(printf '%s' "$ext" | tr 'a-z' 'A-Z')"; do
    [ -e "$f" ] && return 0
  done
  return 1
}

check_screen() {
  local s="$1" d="$target/$1" miss=()
  [ -d "$d" ] || { echo "FALTA  ${s}/ (no existe)"; return 1; }
  for ext in png html md; do
    has_ext "$d" "$ext" || miss+=("$ext")
  done
  if [ "${#miss[@]}" -eq 0 ]; then
    echo "OK     ${s}/ (png + html + md)"
  else
    echo "FALTA  ${s}/ -> falta: ${miss[*]}"
    return 1
  fi
}

if [ "${#expected[@]}" -gt 0 ]; then
  for s in "${expected[@]}"; do check_screen "$s" || status=1; done
else
  for d in "$target"/*/; do
    [ -d "$d" ] || continue
    check_screen "$(basename "$d")" || status=1
  done
fi

if [ "$status" -eq 0 ]; then
  echo "capturas listas en ${target}"
else
  echo "faltan ficheros; revisa el origen o vuelve a importar" >&2
fi
exit "$status"
