#!/usr/bin/env bash
#
# Descarga y empaqueta las 31 fotos de plato de `recipe-photos` como assets offline.
#
# Fuente: los `src` de las laminas de Stitch versionadas en
# `docs/design/concepts/recipe-photos/{fotos-semillas,fotos-categorias}/code.html`. Cada `src` se
# descarga con el sufijo `=s0` (sin el, el host devuelve un thumbnail de 512 px) y se guarda con la
# extension real (magic bytes) en `app/src/main/assets/img/recipes/`:
#
#   seed/<seedRecipeId>.<ext>       12 fotos propias (mapeadas POR POSICION; el `alt` no es la clave)
#   category/cat-<categoryId>.<ext> 18 fotos de categoria (mapeadas por el `alt`, que ES la clave)
#   default.<ext>                    1 foto generica/neutra (clave `default`)
#
# Herramienta de UNA SOLA VEZ: necesita red, asi que NO entra en `init.sh` (el gate debe seguir siendo
# offline). Es idempotente: si el asset ya existe y no se pasa `--force`, no lo vuelve a descargar.
#
# Si hay `cwebp` o `magick`/`convert`, convierte a WebP q80 y redimensiona a 960 px de ancho maximo
# (Constraint "Tamaño del APK" de docs/technical-discovery.md); si no hay ninguna herramienta, conserva
# el original y lo registra en la salida.
#
# Uso:
#   bash scripts/fetch-recipe-photos.sh [--force]
#
# Codigo de salida: 0 si las 31 fotos quedan empaquetadas; != 0 si falta un `src`, el conteo no es
# 12/19/claves esperadas o una descarga falla (nunca un empaquetado parcial silencioso).
set -euo pipefail

FORCE=0
for arg in "$@"; do
    case "$arg" in
        --force) FORCE=1 ;;
        -h|--help)
            sed -n '2,30p' "${BASH_SOURCE[0]}"
            exit 0
            ;;
        *)
            echo "Argumento no reconocido: $arg (usa --force o --help)" >&2
            exit 2
            ;;
    esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
CONCEPTS_DIR="$REPO_ROOT/docs/design/concepts/recipe-photos"
DEST_DIR="$REPO_ROOT/app/src/main/assets/img/recipes"
WORK_DIR="$(mktemp -d -t sazon-recipe-photos-XXXXXX)"
trap 'rm -rf "$WORK_DIR"' EXIT

# Orden EXACTO de `docs/design/concepts/recipe-photos/README.md` (y del lote A del stitch-prompt).
EXPECTED_SEED_KEYS=(
    seed-tortilla-patatas
    seed-gazpacho-andaluz
    seed-pasta-tomate
    seed-pollo-al-ajillo
    seed-lentejas-estofadas
    seed-salmon-a-la-plancha
    seed-tacos-de-pollo
    seed-ensalada-de-quinoa
    seed-tofu-salteado
    seed-crema-de-calabaza
    seed-tortitas-de-avena
    seed-tarta-de-manzana
)

# Claves EXACTAS del lote B (`alt` de cada `<img>`): 18 categorias + `default`.
EXPECTED_CATEGORY_KEYS=(
    cat-pasta
    cat-rice
    cat-soup_cream
    cat-salad
    cat-meat
    cat-poultry
    cat-fish_seafood
    cat-vegetarian
    cat-vegan
    cat-legume_stew
    cat-pizza_bread
    cat-egg_breakfast
    cat-sandwich_wrap
    cat-tapas_appetizer
    cat-dessert
    cat-smoothie_drink
    cat-casserole_oven
    cat-other
    default
)

# Extension real por magic bytes: JPEG (ff d8 ff), PNG (89 50 4e 47) o WebP (RIFF....WEBP).
magic_extension() {
    local file="$1"
    local head
    head="$(od -An -tx1 -N12 "$file" | tr -d ' \n')"
    case "$head" in
        ffd8ff*) echo "jpg" ;;
        89504e47*) echo "png" ;;
        52494646*57454250*) echo "webp" ;;
        *)
            echo "desconocida (cabecera: $head)" >&2
            return 1
            ;;
    esac
}

# Extrae pares "clave url" de la lamina de semillas: la clave vive en el comentario
# `<!-- NN. <clave> -->` inmediatamente ANTERIOR a cada `<img>` (el `alt` describe el plato, no es la
# clave; el mapeo es por posicion).
extract_seed_pairs() {
    awk '
        /<!--[[:space:]]*[0-9][0-9]\./ {
            key = $0
            sub(/.*\. /, "", key)
            sub(/ *-->.*/, "", key)
            next
        }
        /<img[^>]*src="/ {
            url = $0
            sub(/.*src="/, "", url)
            sub(/".*/, "", url)
            if (key != "") { print key " " url; key = "" }
        }
    ' "$1"
}

# Extrae pares "alt url" de la lamina de categorias (el `alt` ES la clave).
extract_category_pairs() {
    sed -n 's/.*alt="\([^"]*\)".*src="\([^"]*\)".*/\1 \2/p' "$1"
}

validate_keys() {
    local label="$1"
    shift
    local -a expected=("$@")
    local -a actual=()
    local line
    while IFS= read -r line; do
        actual+=("${line%% *}")
    done
    if [ "${#actual[@]}" -ne "${#expected[@]}" ]; then
        echo "ERROR: $label: se esperaban ${#expected[@]} claves y hay ${#actual[@]}" >&2
        return 1
    fi
    local i
    for i in "${!expected[@]}"; do
        if [ "${actual[$i]}" != "${expected[$i]}" ]; then
            echo "ERROR: $label: clave ${i} es '${actual[$i]}' y se esperaba '${expected[$i]}'" >&2
            return 1
        fi
    done
    return 0
}

# Empaqueta un par "clave url" en <grupo>/<clave>.<ext> (o default.<ext>).
fetch_one() {
    local group="$1" key="$2" url="$3"
    local out_base downloaded="${WORK_DIR}/download.bin" ext source_bytes convert_tool=""
    if [ -z "$url" ]; then
        echo "ERROR: falta el src de '$key' en la lamina" >&2
        return 1
    fi

    if [ "$group" = "category" ] && [ "$key" = "default" ]; then
        out_base="$DEST_DIR/default"
    else
        out_base="$DEST_DIR/$group/$key"
    fi

    if [ "$FORCE" -eq 0 ] && ls "$out_base".* >/dev/null 2>&1; then
        echo "  = $key (ya empaquetado, se omite)"
        return 0
    fi

    # El `src` de la lamina viene sin sufijo; `=s0` fuerza la resolucion nativa (1376x768).
    echo "  + $key <- ${url}=s0"
    if ! curl -fsSL --max-time 120 -o "$downloaded" "${url}=s0"; then
        echo "ERROR: fallo la descarga de '$key'" >&2
        return 1
    fi

    ext="$(magic_extension "$downloaded")" || {
        echo "ERROR: '$key' no es jpg/png/webp" >&2
        return 1
    }

    mkdir -p "$(dirname "$out_base")"
    source_bytes="$(stat -c %s "$downloaded")"
    if [ "$ext" != "webp" ] && command -v cwebp >/dev/null 2>&1; then
        convert_tool="cwebp"
    elif [ "$ext" != "webp" ] && command -v magick >/dev/null 2>&1; then
        convert_tool="magick"
    elif [ "$ext" != "webp" ] && command -v convert >/dev/null 2>&1; then
        convert_tool="convert"
    fi

    rm -f "$out_base".*
    if [ -n "$convert_tool" ]; then
        case "$convert_tool" in
            cwebp) cwebp -quiet -q 80 -resize 960 0 "$downloaded" -o "$out_base.webp" ;;
            magick) magick "$downloaded" -resize 960x -quality 80 "$out_base.webp" ;;
            convert) convert "$downloaded" -resize 960x -quality 80 "$out_base.webp" ;;
        esac
        echo "    -> $(basename "$out_base").webp (WebP q80 con $convert_tool; original $source_bytes B)"
    else
        cp "$downloaded" "$out_base.$ext"
        echo "    -> $(basename "$out_base").$ext (sin cwebp/magick/convert: se conserva el original)"
    fi

    if [ ! -s "$out_base.webp" ] && [ ! -s "$out_base.$ext" ]; then
        echo "ERROR: '$key' quedo sin fichero empaquetado" >&2
        return 1
    fi
    return 0
}

echo "Lamina A (12 recetas semilla) desde $CONCEPTS_DIR/fotos-semillas/code.html"
SEED_PAIRS_FILE="$WORK_DIR/seed-pairs.txt"
extract_seed_pairs "$CONCEPTS_DIR/fotos-semillas/code.html" >"$SEED_PAIRS_FILE"
validate_keys "fotos-semillas" "${EXPECTED_SEED_KEYS[@]}" <"$SEED_PAIRS_FILE" || exit 1

echo "Lamina B (18 categorias + default) desde $CONCEPTS_DIR/fotos-categorias/code.html"
CATEGORY_PAIRS_FILE="$WORK_DIR/category-pairs.txt"
extract_category_pairs "$CONCEPTS_DIR/fotos-categorias/code.html" >"$CATEGORY_PAIRS_FILE"
validate_keys "fotos-categorias" "${EXPECTED_CATEGORY_KEYS[@]}" <"$CATEGORY_PAIRS_FILE" || exit 1

mkdir -p "$DEST_DIR/seed" "$DEST_DIR/category"

# Descargar con el sufijo =s0 (sin el, el host devuelve un thumbnail de 512 px). Los `src` de los
# `code.html` ya vienen SIN sufijo, y `fetch_one` lo anade.
while read -r key url; do
    fetch_one seed "$key" "$url"
done <"$SEED_PAIRS_FILE"

while read -r key url; do
    fetch_one category "$key" "$url"
done <"$CATEGORY_PAIRS_FILE"

echo
echo "Inventario final en ${DEST_DIR#"$REPO_ROOT"/}:"
TOTAL_FILES=0
TOTAL_BYTES=0
for group in seed category; do
    count="$(find "$DEST_DIR/$group" -maxdepth 1 -type f | wc -l | tr -d ' ')"
    bytes="$(find "$DEST_DIR/$group" -maxdepth 1 -type f -printf '%s\n' | awk '{s+=$1} END {print s+0}')"
    ext="$(find "$DEST_DIR/$group" -maxdepth 1 -type f -printf '%f\n' | sed 's/.*\.//' | sort -u | paste -sd, -)"
    echo "  $group/: $count ficheros, $bytes bytes, extension(es): ${ext:-ninguna}"
    TOTAL_FILES=$((TOTAL_FILES + count))
    TOTAL_BYTES=$((TOTAL_BYTES + bytes))
done
default_files="$(find "$DEST_DIR" -maxdepth 1 -type f -name 'default.*' | wc -l | tr -d ' ')"
default_bytes="$(find "$DEST_DIR" -maxdepth 1 -type f -name 'default.*' -printf '%s\n' | awk '{s+=$1} END {print s+0}')"
default_ext="$(find "$DEST_DIR" -maxdepth 1 -type f -name 'default.*' -printf '%f\n' | sed 's/.*\.//')"
echo "  default: $default_files fichero(s), $default_bytes bytes, extension: ${default_ext:-ninguna}"
TOTAL_FILES=$((TOTAL_FILES + default_files))
TOTAL_BYTES=$((TOTAL_BYTES + default_bytes))
echo "  TOTAL: $TOTAL_FILES ficheros, $TOTAL_BYTES bytes ($((TOTAL_BYTES / 1024)) KiB)"

if [ "$TOTAL_FILES" -ne 31 ]; then
    echo "ERROR: se esperaban 31 ficheros empaquetados y hay $TOTAL_FILES" >&2
    exit 1
fi
if [ "$TOTAL_BYTES" -gt $((8 * 1024 * 1024)) ]; then
    echo "ERROR: el total supera el presupuesto de 8 MB" >&2
    exit 1
fi

echo "Empaquetado completo."
