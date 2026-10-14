#!/usr/bin/env bash
set -euo pipefail

# Lanza UNA sesion de feature-flow para una feature, en su worktree, sin esperar a que termine.
#
# Arranca la sesion en el servidor de opencode y devuelve el control en segundos: el bucle del agente
# corre en el servidor. Un cliente que espera el ciclo completo (30-90 min) es abandonado por el harness
# y deja al orquestador a ciegas, de ahi que no se use `opencode run` aqui.
#
# Uso:
#   scripts/launch-feature-session.sh <feature-id> [prompt-extra] [--dry-run]
#
#   <feature-id>    id de la feature: rama feature/<id>, worktree <root>/<id>
#   prefijo/id      rama <prefijo>/<id>, worktree <root>/<prefijo>-<id>
#   [prompt-extra]  texto que se anade al prompt estandar (opcional)
#   --dry-run       imprime lo que enviaria y no crea nada
#
# Variables de entorno opcionales:
#   WORKTREES_ROOT   directorio de worktrees (por defecto ../<carpeta-checkout>-worktrees)
#   SAZON_MODEL      modelo en formato provider/model[#variant]; vacio = el de la configuracion
#   SAZON_OPENCODE   binario de opencode (por defecto: opencode)
#
# Deja el id de la sesion en <worktrees-root>/.sazon-sessions/<feature-id>.

dry=0
args=()
for a in "$@"; do
  case "$a" in
    --dry-run) dry=1 ;;
    *) args+=("$a") ;;
  esac
done
target="${args[0]:-}"
if [ -z "$target" ]; then
  echo "uso: $0 <feature-id | prefijo/id> [prompt-extra] [--dry-run]" >&2
  exit 2
fi

repo_root="$(cd "$(dirname "$(git rev-parse --path-format=absolute --git-common-dir)")" && pwd)"
base_dirname="$(basename "$repo_root")"
worktrees_root="${WORKTREES_ROOT:-$(dirname "$repo_root")/${base_dirname}-worktrees}"
oc="${SAZON_OPENCODE:-opencode}"

case "$target" in
  */*) prefix="${target%%/*}"; id="${target#*/}" ;;
  *)   prefix="feature"; id="$target" ;;
esac
if [ "$prefix" = "feature" ]; then dir_name="$id"; else dir_name="${prefix}-${id}"; fi
worktree_path="${worktrees_root}/${dir_name}"
registry_dir="${worktrees_root}/.sazon-sessions"
registry="${registry_dir}/${dir_name}"

if [ ! -d "$worktree_path" ]; then
  echo "no existe el worktree: $worktree_path" >&2
  echo "crea el worktree primero con: bash scripts/new-worktree.sh $target" >&2
  exit 1
fi

prompt="Use feature-flow for ${id} only, until accepted. Do not push and do not open a PR; the orchestrator publishes."
if [ -n "${args[1]:-}" ]; then
  prompt="$prompt ${args[1]}"
fi

# El worktree lleva los agentes del repo (.opencode/agent): la sesion se crea CON esa ubicacion o los
# subagentes planner/implementer/validator no existirian para ella.
# Permisos: SOLO external_directory (Gradle lee fuera del worktree, p. ej. ~/.gradle). Nada de comodines
# `allow`: un `{action:"*",resource:"*",effect:"allow"}` anula las guardas deny de los agentes.
payload="$(python3 - "$id" "$worktree_path" "${SAZON_MODEL:-}" "$prompt" <<'PY'
import json, sys
id_, worktree, model, prompt = sys.argv[1:5]
body = {
    "title": f"[{id_}] feature-flow",
    "location": {"directory": worktree},
    "permissions": [{"action": "external_directory", "resource": "*", "effect": "allow"}],
}
if model:
    prov, resto = model.split("/", 1) if "/" in model else ("", model)
    mid, _, variant = resto.partition("#")
    body["model"] = {"providerID": prov, "id": mid}
    if variant:
        body["model"]["variant"] = variant
print(json.dumps({"session": body, "prompt": {"text": prompt}}, ensure_ascii=False))
PY
)"

if [ "$dry" = "1" ]; then
  echo "DRY-RUN · worktree: $worktree_path"
  echo "DRY-RUN · registro: $registry"
  echo "$payload" | python3 -c "import json,sys; d=json.load(sys.stdin); print('DRY-RUN · POST /api/session'); print(json.dumps(d['session'], ensure_ascii=False, indent=2)); print('DRY-RUN · POST /api/session/<id>/prompt'); print(json.dumps(d['prompt'], ensure_ascii=False, indent=2))"
  exit 0
fi

if ! command -v "$oc" >/dev/null 2>&1; then
  echo "no encuentro el binario '$oc' en el PATH (usa SAZON_OPENCODE)" >&2
  exit 1
fi

session_json="$(printf '%s' "$payload" | python3 -c "import json,sys; print(json.dumps(json.load(sys.stdin)['session'], ensure_ascii=False))")"
prompt_json="$(printf '%s' "$payload" | python3 -c "import json,sys; print(json.dumps(json.load(sys.stdin)['prompt'], ensure_ascii=False))")"

sid="$(timeout 60 "$oc" api post /api/session --data "$session_json" 2>/dev/null \
  | python3 -c "import json,sys
try:
    d=json.load(sys.stdin)
except Exception:
    print(''); raise SystemExit
print((d.get('data') or d).get('id',''))")"
case "$sid" in
  ses*) ;;
  *) echo "no he podido crear la sesion en el servidor de opencode (respuesta vacia o sin id)" >&2; exit 1 ;;
esac

timeout 60 "$oc" api post "/api/session/${sid}/prompt" --data "$prompt_json" >/dev/null 2>&1 \
  || { echo "la sesion $sid se creo pero no he podido encolar el prompt" >&2; exit 1; }

mkdir -p "$registry_dir"
printf '%s\n' "$sid" > "$registry"

echo "sesion: $sid  (arrancada en $worktree_path; el lanzador no espera)"
