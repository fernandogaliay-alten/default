#!/usr/bin/env bash
set -euo pipefail

# En WSL publica el JDK y el SDK de Linux (ver scripts/wsl-env.sh). Fuera de WSL no hace nada.
# shellcheck source=scripts/wsl-env.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/scripts/wsl-env.sh"

# Limite por build; configurable con SAZON_BUILD_TIMEOUT (segundos).
BUILD_TIMEOUT="${SAZON_BUILD_TIMEOUT:-1800}"

# La salida de Gradle va a un fichero (ver scripts/gradle.sh): los daemons heredan ese
# fichero y no el stdout de esta shell, que es lo que deja colgada a la herramienta de
# shell de opencode cuando un daemon sobrevive al build. Por eso ya no se llama a
# `./gradlew --stop` al salir: es global y mataria los builds de otros worktrees que
# corren en paralelo.
run_gradle() {
  local status=0
  timeout "$BUILD_TIMEOUT" bash scripts/gradle.sh "$@" || status=$?
  if [ "$status" -ne 0 ]; then
    echo
    echo "Gradle termino con codigo $status."
    exit "$status"
  fi
  echo
}

echo "Repositorio: $(pwd)"
echo

echo "== Tests de dominio (JVM) =="
run_gradle test

echo "== Lint =="
run_gradle lint

echo "Verificacion completada correctamente."
echo
echo "Comandos manuales (no se ejecutan solos):"
echo "  bash scripts/gradle.sh assembleDebug     # genera el APK de depuracion"
echo "  bash scripts/gradle.sh installDebug      # instala en un emulador o dispositivo conectado"
