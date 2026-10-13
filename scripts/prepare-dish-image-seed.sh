#!/usr/bin/env bash
#
# Prepara y empaqueta el **set semilla** de imagenes de plato (fase 7 de
# `docs/ai-dish-images-proposal.md` §5): los platos frecuentes del demo se generan **una sola vez** y se
# versionan como assets en `app/src/main/assets/img/dishes/seed/`, con el nombre = clave canonica del
# plato (`DishImageCacheKey.of(titulo)`, p. ej. `dish-image-v2-arroz-con-pollo-criollo`).
#
# En runtime `DishImageResolver` resuelve esos bytes por la clave canonica **entre el disco y el
# generador**, asi que esos platos muestran su foto propia sin red y sin gastar cuota (ni la primera
# vez). Ver `docs/adr/0005-ai-dish-images.md`.
#
# Herramienta de UNA SOLA VEZ: necesita red y clave, asi que NO entra en `init.sh` (el gate sigue siendo
# offline) y **ningun** codigo de runtime la invoca. Es idempotente: si el asset ya existe y no se pasa
# `--force`, no lo vuelve a generar ni a importar.
#
# Modos:
#   (por defecto) proveedor: lee `devexpert.apiKey` de `local.properties` (nunca la imprime) y, por
#     plato, pide la imagen con **una** llamada `POST /images/edits` (multipart, `model=image-edit`,
#     la foto neutra `img/recipes/default.<ext>` como referencia de estilo: mismo contrato que la fase 6
#     en `DevExpertDishImageGenerator`). Solo ante un **rechazo definitivo** (`400/404/405/413/415/422`)
#     o una carga `2xx` inutilizable respalda con **una** llamada a `POST /images/generations`
#     (`model=image`); transporte/timeout y `401/403/429/5xx` fallan el plato **sin** respaldo.
#   --source <dir>: importa imagenes ya obtenidas (una por plato, `<cacheKey>.<ext>`) y ejecuta la
#     **misma** validacion y empaquetado sin llamar al proveedor. Via documentada para obtenerlas sin
#     gastar cuota: ejecutar una vez los platos de la demo con la app y copiar `cacheDir/dish-images/`
#     (cuyos nombres **ya son** las claves canonicas).
#
# El prompt se compone **espejando** `domain.image.DishImagePrompt` (plantilla v2):
#   `<PROMPT_PREFIX> <plato>[, <etiqueta de cocina>], <COMPOSITION>, <STYLE_SUFFIX>, <NEGATIVES>`
# con la etiqueta de cocina leida de `docs/domain/taxonomy.json` (la misma fuente que la app) y las
# coletillas identicas; el wire espeja `data.ai.image.DishImagePolicy`.
#
# Deriva conocida y acotada: este espejo **solo** compone titulo + cocina (el script no tiene receta ni
# descriptor), asi que **no** puede reproducir las clausulas de `imagePrompt` ni de ingredientes clave
# que la plantilla v2 anade cuando existen. Ademas, la coletilla se acota a titulo+cocina: los bytes del
# set se preparan **una vez** y su contenido ya es la foto correcta del plato, de modo que la deriva es
# cosmetica. Queda ademas acotada por la regla durable de `DishImageCacheKey.VERSION`: cambiar la
# plantilla obliga a subir la version y, con ella, los **nombres** de los assets (el renombrado o la
# regeneracion son decision de la persona).
#
# Requisitos: `curl`, `jq` y `base64` (y `devexpert.apiKey` en `local.properties`) para el modo
# proveedor. Si hay `cwebp` o `magick`/`convert`, convierte a WebP q80 y redimensiona a 960 px de ancho
# maximo (presupuesto del APK); si no hay ninguna herramienta, conserva el original (jpg/png) y lo
# registra. `--source` no necesita `jq`.
#
# Uso:
#   bash scripts/prepare-dish-image-seed.sh [--force]
#   bash scripts/prepare-dish-image-seed.sh --source <directorio-con-las-imagenes> [--force]
#
# Codigo de salida: 0 si el set queda empaquetado y validado (una clave por plato, extension
# `webp`/`jpg`/`png`, ningun fichero vacio, total <= 2 MiB); != 0 ante cualquier desvio (clave faltante,
# asset vacio, extension desconocida, proveedor que rechaza o total por encima del presupuesto).
# **Nunca** un empaquetado parcial silencioso.
set -euo pipefail

FORCE=0
SOURCE_DIR=""

while [ $# -gt 0 ]; do
    case "$1" in
        --force) FORCE=1; shift ;;
        --source)
            SOURCE_DIR="${2:-}"
            if [ -z "$SOURCE_DIR" ]; then
                echo "ERROR: --source necesita un directorio" >&2
                exit 2
            fi
            shift 2
            ;;
        --source=*)
            SOURCE_DIR="${1#--source=}"
            shift
            ;;
        -h|--help)
            sed -n '2,53p' "${BASH_SOURCE[0]}"
            exit 0
            ;;
        *)
            echo "Argumento no reconocido: $1 (usa --force, --source <dir> o --help)" >&2
            exit 2
            ;;
    esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ASSETS_DIR="$REPO_ROOT/app/src/main/assets"
DEST_DIR="$ASSETS_DIR/img/dishes/seed"
REFERENCE_BASE="$ASSETS_DIR/img/recipes/default"
TAXONOMY_FILE="$REPO_ROOT/docs/domain/taxonomy.json"
LOCAL_PROPERTIES="$REPO_ROOT/local.properties"
WORK_DIR="$(mktemp -d -t sazon-dish-seed-XXXXXX)"
trap 'rm -rf "$WORK_DIR"' EXIT

# --- Lista autoritativa: clave canonica <- titulo del plato -> cocina (decision 12 de la spec) -------
# `EXPECTED_SEED_KEYS` va **ordenada** y cada clave se valida contra el alfabeto canonico `[a-z0-9-]`;
# el test JVM deriva cada clave con `DishImageCacheKey.of(titulo)`, de modo que un desajuste entre el
# nombre del asset y la clave real hace **fallar** el test.
# Las 12 recetas semilla quedan **excluidas** por regla (ya resuelven su foto propia por `id`).
EXPECTED_SEED_KEYS=(
    dish-image-v2-arroz-con-pollo-criollo
    dish-image-v2-paella-valenciana
    dish-image-v2-pizza-margarita
    dish-image-v2-hamburguesa-casera
    dish-image-v2-tarta-de-chocolate
    dish-image-v2-ensalada-cesar
)
DISH_TITLES=(
    "Arroz con pollo criollo"
    "Paella valenciana"
    "Pizza margarita"
    "Hamburguesa casera"
    "Tarta de chocolate"
    "Ensalada césar"
)
DISH_CUISINES=(
    spanish
    spanish
    italian
    american
    italian
    american
)

# --- Contrato espejado de `DishImagePrompt`/`DishImagePolicy` (un solo sitio en el repo: el codigo) ---
PROMPT_PREFIX="Fotografía gastronómica de"
COMPOSITION="una ración completa del plato en primer plano, sobre fondo neutro"
STYLE_SUFFIX="cerámica artesanal, luz natural suave, estilo editorial gastronómico"
NEGATIVES="sin texto, sin manos, sin personas, sin cubiertos ni marcas de agua, sin collage ni varios platos, sin fondo recargado"
BASE_URL="https://inference.devexpert.io/v1"
IMAGES_EDITS_URL="$BASE_URL/images/edits"
IMAGES_GENERATIONS_URL="$BASE_URL/images/generations"
EDIT_MODEL_ALIAS="image-edit"
MODEL_ALIAS="image"
IMAGE_SIZE="1024x1024"
TARGET_WIDTH_PX=960
WEBP_QUALITY=80
EDIT_FALLBACK_STATUSES=(400 404 405 413 415 422)
MAX_TOTAL_BYTES=$((2 * 1024 * 1024))
EXTENSIONS=(webp jpg png)

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

media_type_for() {
    case "$1" in
        webp) echo "image/webp" ;;
        png) echo "image/png" ;;
        *) echo "image/jpeg" ;;
    esac
}

# Clave -> fichero ya empaquetado (cualquier extension valida), si existe.
packaged_asset() {
    local key="$1" ext
    for ext in "${EXTENSIONS[@]}"; do
        if [ -f "$DEST_DIR/$key.$ext" ]; then
            echo "$DEST_DIR/$key.$ext"
            return 0
        fi
    done
    return 1
}

# Etiqueta de cocina de la Taxonomia (misma fuente que la app); cadena vacia si el id no existe.
cuisine_label() {
    jq -r --arg id "$1" '.cuisines.items[] | select(.id == $id) | .label' "$TAXONOMY_FILE" 2>/dev/null | head -1
}

# Prompt del plato $1 (indice en la lista): espejo (acotado) de `DishImagePrompt.build` v2.
seed_prompt() {
    local index="$1"
    local label
    label="$(cuisine_label "${DISH_CUISINES[$index]}")"
    if [ -n "$label" ]; then
        echo "$PROMPT_PREFIX ${DISH_TITLES[$index]}, $label, $COMPOSITION, $STYLE_SUFFIX, $NEGATIVES"
    else
        echo "$PROMPT_PREFIX ${DISH_TITLES[$index]}, $COMPOSITION, $STYLE_SUFFIX, $NEGATIVES"
    fi
}

status_in_edit_fallback_set() {
    local status="$1" candidate
    for candidate in "${EDIT_FALLBACK_STATUSES[@]}"; do
        [ "$candidate" = "$status" ] && return 0
    done
    return 1
}

# Empaqueta `<clave>.<ext>` en DEST_DIR a partir de un fichero ya descargado/importado.
package_asset() {
    local key="$1" source_file="$2"
    local ext convert_tool="" source_bytes

    ext="$(magic_extension "$source_file")" || {
        echo "ERROR: '$key' no es jpg/png/webp" >&2
        return 1
    }
    source_bytes="$(stat -c %s "$source_file")"

    if [ "$ext" != "webp" ] && command -v cwebp >/dev/null 2>&1; then
        convert_tool="cwebp"
    elif [ "$ext" != "webp" ] && command -v magick >/dev/null 2>&1; then
        convert_tool="magick"
    elif [ "$ext" != "webp" ] && command -v convert >/dev/null 2>&1; then
        convert_tool="convert"
    fi

    mkdir -p "$DEST_DIR"
    find "$DEST_DIR" -maxdepth 1 -type f -name "$key.*" -delete
    if [ -n "$convert_tool" ]; then
        case "$convert_tool" in
            cwebp) cwebp -quiet -q "$WEBP_QUALITY" -resize "$TARGET_WIDTH_PX" 0 "$source_file" -o "$DEST_DIR/$key.webp" ;;
            magick) magick "$source_file" -resize "${TARGET_WIDTH_PX}x" -quality "$WEBP_QUALITY" "$DEST_DIR/$key.webp" ;;
            convert) convert "$source_file" -resize "${TARGET_WIDTH_PX}x" -quality "$WEBP_QUALITY" "$DEST_DIR/$key.webp" ;;
        esac
        echo "    -> $key.webp (WebP q$WEBP_QUALITY con $convert_tool; original $source_bytes B)"
    else
        cp "$source_file" "$DEST_DIR/$key.$ext"
        echo "    -> $key.$ext (sin cwebp/magick/convert: se conserva el original)"
    fi

    if [ ! -s "$DEST_DIR/$key.webp" ] && [ ! -s "$DEST_DIR/$key.$ext" ]; then
        echo "ERROR: '$key' quedo sin fichero empaquetado" >&2
        return 1
    fi
    return 0
}

# --- Modo proveedor ---------------------------------------------------------------------------------

require_provider_tools() {
    local missing=0 tool
    for tool in curl jq base64; do
        if ! command -v "$tool" >/dev/null 2>&1; then
            echo "ERROR: falta '$tool' (el modo proveedor necesita curl, jq y base64)" >&2
            missing=1
        fi
    done
    if [ ! -f "$TAXONOMY_FILE" ]; then
        echo "ERROR: falta la Taxonomia '$TAXONOMY_FILE' (la etiqueta de cocina del prompt sale de ahi)" >&2
        missing=1
    fi
    [ "$missing" -eq 0 ]
}

read_api_key() {
    [ -f "$LOCAL_PROPERTIES" ] || {
        echo "ERROR: falta local.properties (define devexpert.apiKey)" >&2
        return 1
    }
    local key
    key="$(sed -n 's/^devexpert\.apiKey=//p' "$LOCAL_PROPERTIES" | head -1)"
    [ -n "$key" ] || {
        echo "ERROR: falta devexpert.apiKey en local.properties" >&2
        return 1
    }
    printf '%s' "$key"
}

reference_path() {
    local ext
    for ext in "${EXTENSIONS[@]}"; do
        if [ -f "$REFERENCE_BASE.$ext" ]; then
            echo "$REFERENCE_BASE.$ext"
            return 0
        fi
    done
    return 1
}

# Codifica b64_json ($1) a fichero ($2); falla si base64 no decodifica o queda vacio.
decode_b64() {
    local b64="$1" out="$2"
    if ! printf '%s' "$b64" | tr -d '\n' | base64 -d >"$out" 2>/dev/null; then
        echo "ERROR: b64_json no decodifica" >&2
        return 1
    fi
    [ -s "$out" ] || {
        echo "ERROR: la imagen decodificada esta vacia" >&2
        return 1
    }
    return 0
}

fetch_from_provider() {
    local index="$1"
    local key="${EXPECTED_SEED_KEYS[$index]}"
    local prompt body_file http_status b64 decoded needs_fallback=0

    prompt="$(seed_prompt "$index")"
    body_file="$WORK_DIR/response-$key.json"
    decoded="$WORK_DIR/$key.bin"

    echo "  + $key <- /images/edits (model=$EDIT_MODEL_ALIAS)"
    if ! http_status="$(curl -sS -o "$body_file" -w '%{http_code}' --max-time 120 \
        -X POST "$IMAGES_EDITS_URL" \
        -H "Authorization: Bearer $API_KEY" \
        -F "model=$EDIT_MODEL_ALIAS" \
        -F "prompt=$prompt" \
        -F "size=$IMAGE_SIZE" \
        -F "image=@$REFERENCE_PATH;type=$(media_type_for "${REFERENCE_PATH##*.}")")"; then
        echo "ERROR: '$key': fallo de transporte/timeout en /images/edits (sin respaldo)" >&2
        return 1
    fi

    if [ "${http_status:0:1}" = "2" ]; then
        b64="$(jq -r '.data[0].b64_json // empty' "$body_file" 2>/dev/null || true)"
        if [ -z "$b64" ]; then
            echo "    ! $key: /images/edits respondio $http_status sin b64_json utilizable; se respalda por texto"
            needs_fallback=1
        fi
    elif status_in_edit_fallback_set "$http_status"; then
        echo "    ! $key: /images/edits rechazo definitivo ($http_status); se respalda por texto"
        needs_fallback=1
    else
        echo "ERROR: '$key': /images/edits respondio $http_status (sin respaldo)" >&2
        return 1
    fi

    if [ "$needs_fallback" -eq 1 ]; then
        echo "    + $key <- /images/generations (model=$MODEL_ALIAS)"
        if ! http_status="$(curl -sS -o "$body_file" -w '%{http_code}' --max-time 120 \
            -X POST "$IMAGES_GENERATIONS_URL" \
            -H "Authorization: Bearer $API_KEY" \
            -H "Content-Type: application/json" \
            -d "$(jq -n --arg model "$MODEL_ALIAS" --arg prompt "$prompt" --arg size "$IMAGE_SIZE" \
                '{model: $model, prompt: $prompt, size: $size}')")"; then
            echo "ERROR: '$key': fallo de transporte/timeout en /images/generations" >&2
            return 1
        fi
        if [ "${http_status:0:1}" != "2" ]; then
            echo "ERROR: '$key': /images/generations respondio $http_status" >&2
            return 1
        fi
        b64="$(jq -r '.data[0].b64_json // empty' "$body_file" 2>/dev/null || true)"
        if [ -z "$b64" ]; then
            echo "ERROR: '$key': /images/generations no devolvio b64_json utilizable" >&2
            return 1
        fi
    fi

    decode_b64 "$b64" "$decoded" || {
        echo "ERROR: '$key': respuesta del proveedor no utilizable" >&2
        return 1
    }
    package_asset "$key" "$decoded"
}

# --- Modo --source ----------------------------------------------------------------------------------

# Fichero de origen del plato (extension probada en orden), si existe en SOURCE_DIR.
source_file_for() {
    local key="$1" ext
    for ext in "${EXTENSIONS[@]}"; do
        if [ -f "$SOURCE_DIR/$key.$ext" ]; then
            printf '%s' "$SOURCE_DIR/$key.$ext"
            return 0
        fi
    done
    return 1
}

import_from_source() {
    local key="$1" file
    file="$(source_file_for "$key")" || {
        echo "ERROR: '$key': falta $SOURCE_DIR/$key.{webp,jpg,png}" >&2
        return 1
    }

    echo "  + $key <- $file"
    package_asset "$key" "$file"
}

# --- Inventario y validacion final ------------------------------------------------------------------

validate_inventory() {
    local problems=0
    local expected_count="${#EXPECTED_SEED_KEYS[@]}"
    local total=0
    local key ext file size

    if [ ! -d "$DEST_DIR" ]; then
        echo "ERROR: no existe el directorio del set '$DEST_DIR'" >&2
        return 1
    fi

    # 1) Exactamente un fichero por plato listado, con extension conocida y no vacio.
    for key in "${EXPECTED_SEED_KEYS[@]}"; do
        if [[ ! "$key" =~ ^[a-z0-9-]+$ ]]; then
            echo "ERROR: la clave '$key' no cumple el alfabeto canonico [a-z0-9-]" >&2
            problems=1
        fi

        local -a matches=()
        while IFS= read -r file; do
            matches+=("$file")
        done < <(find "$DEST_DIR" -maxdepth 1 -type f -name "$key.*" -printf '%f\n' | sort)

        if [ "${#matches[@]}" -ne 1 ]; then
            echo "ERROR: '$key' deberia tener exactamente un asset y tiene ${#matches[@]}" >&2
            problems=1
            continue
        fi

        file="${matches[0]}"
        ext="${file##*.}"
        case "$ext" in
            webp|jpg|png) ;;
            *)
                echo "ERROR: '$file' tiene una extension desconocida ('$ext')" >&2
                problems=1
                continue
                ;;
        esac

        size="$(stat -c %s "$DEST_DIR/$file")"
        if [ "$size" -le 0 ]; then
            echo "ERROR: '$file' esta vacio" >&2
            problems=1
            continue
        fi
        total=$((total + size))
    done

    # 2) Ningun asset ajeno a la lista (nunca un set con claves de mas).
    while IFS= read -r file; do
        local base="${file%.*}"
        local known=0 expected
        for expected in "${EXPECTED_SEED_KEYS[@]}"; do
            if [ "$expected" = "$base" ]; then
                known=1
                break
            fi
        done
        if [ "$known" -eq 0 ]; then
            echo "ERROR: '$file' no corresponde a ningun plato de la lista" >&2
            problems=1
        fi
    done < <(find "$DEST_DIR" -maxdepth 1 -type f -printf '%f\n' | sort)

    # 3) Presupuesto de tamano del APK.
    if [ "$total" -gt "$MAX_TOTAL_BYTES" ]; then
        echo "ERROR: el set ocupa $total B y supera el presupuesto de $MAX_TOTAL_BYTES B (2 MiB)" >&2
        problems=1
    fi

    echo
    echo "Inventario final en ${DEST_DIR#"$REPO_ROOT"/}:"
    while IFS= read -r file; do
        echo "  $file ($(stat -c %s "$DEST_DIR/$file") B)"
    done < <(find "$DEST_DIR" -maxdepth 1 -type f -printf '%f\n' | sort)
    echo "  TOTAL: $expected_count ficheros esperados, $total B ($((total / 1024)) KiB) de $((MAX_TOTAL_BYTES / 1024)) KiB"

    return "$problems"
}

# --- Ejecucion --------------------------------------------------------------------------------------

mkdir -p "$DEST_DIR"

if [ -n "$SOURCE_DIR" ]; then
    if [ ! -d "$SOURCE_DIR" ]; then
        echo "ERROR: el directorio de origen '$SOURCE_DIR' no existe" >&2
        exit 1
    fi
    echo "Importando el set semilla desde '$SOURCE_DIR' (sin llamar al proveedor)"
    # Primera pasada: comprobar que el origen tiene **todos** los platos pendientes antes de escribir
    # nada (asi un origen incompleto no deja un empaquetado parcial).
    MISSING=0
    for i in "${!EXPECTED_SEED_KEYS[@]}"; do
        key="${EXPECTED_SEED_KEYS[$i]}"
        if [ "$FORCE" -eq 0 ] && packaged_asset "$key" >/dev/null; then
            continue
        fi
        if ! source_file_for "$key" >/dev/null; then
            echo "ERROR: '$key': falta $SOURCE_DIR/$key.{webp,jpg,png}" >&2
            MISSING=1
        fi
    done
    [ "$MISSING" -eq 0 ] || exit 1

    # Segunda pasada: empaquetar (idempotente; omite lo ya existente sin `--force`).
    for i in "${!EXPECTED_SEED_KEYS[@]}"; do
        key="${EXPECTED_SEED_KEYS[$i]}"
        if [ "$FORCE" -eq 0 ] && packaged_asset "$key" >/dev/null; then
            echo "  = $key (ya empaquetado, se omite)"
            continue
        fi
        import_from_source "$key" || exit 1
    done
else
    echo "Preparando el set semilla con el proveedor (/images/edits con respaldo a /images/generations)"
    PENDING=()
    for i in "${!EXPECTED_SEED_KEYS[@]}"; do
        key="${EXPECTED_SEED_KEYS[$i]}"
        if [ "$FORCE" -eq 0 ] && packaged_asset "$key" >/dev/null; then
            echo "  = $key (ya empaquetado, se omite)"
            continue
        fi
        PENDING+=("$i")
    done

    if [ "${#PENDING[@]}" -gt 0 ]; then
        require_provider_tools || exit 1
        API_KEY="$(read_api_key)" || exit 1
        REFERENCE_PATH="$(reference_path)" || {
            echo "ERROR: falta la referencia de estilo '$REFERENCE_BASE.{webp,jpg,png}'" >&2
            exit 1
        }
        for i in "${PENDING[@]}"; do
            fetch_from_provider "$i" || exit 1
        done
    fi
fi

validate_inventory || exit 1
echo "Empaquetado completo."
