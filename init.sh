#!/usr/bin/env bash
set -euo pipefail

# Limite por build; configurable con SAZON_BUILD_TIMEOUT (segundos).
BUILD_TIMEOUT="${SAZON_BUILD_TIMEOUT:-1800}"

# No dejar daemons de Gradle vivos al salir. La herramienta de shell de opencode
# espera a que termine el arbol de procesos del comando; un daemon sobreviviente
# (spawned por el build, detached por diseno) puede dejarla colgada para siempre.
# Se ejecuta en cualquier salida: exito, fallo o timeout.
trap './gradlew --stop >/dev/null 2>&1 || true' EXIT

echo "Repositorio: $(pwd)"
echo

echo "== Tests de dominio (JVM) =="
timeout "$BUILD_TIMEOUT" ./gradlew test

echo
echo "== Lint =="
timeout "$BUILD_TIMEOUT" ./gradlew lint

echo
echo "Verificacion completada correctamente."
echo
echo "Comandos manuales (no se ejecutan solos):"
echo "  ./gradlew assembleDebug           # genera el APK de depuracion"
echo "  ./gradlew installDebug            # instala en un emulador o dispositivo conectado"
