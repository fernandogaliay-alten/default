#!/usr/bin/env bash
set -uo pipefail

# En WSL publica el JDK y el SDK de Linux (ver scripts/wsl-env.sh). Fuera de WSL no hace nada.
# shellcheck source=scripts/wsl-env.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/scripts/wsl-env.sh"

# Ejecuta Gradle sin dejar que un daemon sobreviviente cuelgue la shell que lo lanza.
#
# Por que existe: la herramienta de shell de los agentes (opencode) espera al arbol de procesos del
# comando y lee su stdout. El daemon de Gradle y, sobre todo, el daemon de Kotlin que arranca
# `compileDebugKotlin`, heredan el stdout del proceso que lanza el build; si alguno sobrevive al
# comando, el pipe nunca llega a EOF y la herramienta se queda colgada para siempre, aunque el build
# ya haya impreso `BUILD SUCCESSFUL` (incidencia observada en la sesion de `activity-summary`).
#
# Que hace: manda la salida del build a un fichero (los daemons heredan ese fichero, no el pipe del
# llamante), vuelca la salida al terminar y devuelve el codigo de salida de Gradle. No para daemons:
# `./gradlew --stop` es global y mataria tambien los builds de otros worktrees que corren en paralelo
# (los daemons de Gradle se apagan solos tras su periodo de inactividad).
#
# Uso (mismos argumentos que `./gradlew`):
#   bash scripts/gradle.sh test
#   bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.domain.activity.*"
#   bash scripts/gradle.sh lint
#
# La ruta del log se puede fijar con `SAZON_GRADLE_LOG`; por defecto es un temporal nuevo.

LOG="${SAZON_GRADLE_LOG:-$(mktemp -t sazon-gradle-XXXXXX.log)}"

echo "\$ ./gradlew $*   # salida completa en $LOG"

./gradlew "$@" >"$LOG" 2>&1 </dev/null
status=$?

cat "$LOG"

if [ "$status" -ne 0 ]; then
  echo
  echo "Gradle termino con codigo $status (salida completa en $LOG)."
fi

exit "$status"
