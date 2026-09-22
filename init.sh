#!/usr/bin/env bash
set -euo pipefail

echo "Repositorio: $(pwd)"
echo

echo "== Tests de dominio (JVM) =="
./gradlew test

echo
echo "== Lint =="
./gradlew lint

echo
echo "Verificacion completada correctamente."
echo
echo "Comandos manuales (no se ejecutan solos):"
echo "  ./gradlew assembleDebug           # genera el APK de depuracion"
echo "  ./gradlew installDebug            # instala en un emulador o dispositivo conectado"
