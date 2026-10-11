#!/usr/bin/env bash
# Entorno de WSL para Sazón. Este fichero se *sourcea*, no se ejecuta.
#
# Por qué existe: dentro de WSL el `java` del PATH pertenece a Windows (interop) y no sirve para
# compilar Android, y el SDK de Windows solo trae binarios .exe (aapt2.exe, adb.exe, zipalign.exe)
# que Gradle en Linux no puede ejecutar. Aquí se publica el JDK y el SDK de Linux que instala
# scripts/wsl-bootstrap.sh, sin depender de que la shell sea interactiva: los agentes lanzan shells
# no interactivas que no leen ~/.bashrc ni ~/.profile.
#
# Fuera de WSL no hace nada, así que init.sh y el resto de scripts pueden sourcearlo siempre.
#
# Uso:
#   . scripts/wsl-env.sh
#
# Variables respetadas:
#   SAZON_WSL_JAVA_HOME     JDK de Linux (por defecto: ~/.local/opt/jdk-21)
#   SAZON_WSL_ANDROID_SDK   SDK de Android de Linux (por defecto: ~/Android/Sdk)
#   SAZON_WSL_GRADLE_HOME   GRADLE_USER_HOME en ext4 (por defecto: ~/.gradle)
#   SAZON_WSL_LOCAL_PROPS   0 para no normalizar local.properties (por defecto: se normaliza)
#   SAZON_NO_COLOR          no usado hoy; reservado

sazon_is_wsl() {
  [ -n "${WSL_DISTRO_NAME:-}" ] && return 0
  [ -r /proc/version ] && grep -qi microsoft /proc/version
}

# Añade un directorio al PATH solo si no está ya (evita PATHs que crecen sin fin al sourcear).
sazon_path_add() {
  local dir="$1"
  [ -d "$dir" ] || return 0
  case ":$PATH:" in
    *":$dir:"*) ;;
    *) PATH="$dir:$PATH" ;;
  esac
}

# local.properties lo regenera Android Studio en Windows con la ruta del SDK de Windows. Si el
# fichero trae una ruta de Windows, se reescribe a la del SDK de Linux para que Gradle en WSL no
# dependa del orden de precedencia entre `sdk.dir` y ANDROID_HOME. Solo se corrige ese caso: si el
# fichero ya apunta a Linux (o no existe) se deja intacto.
sazon_wsl_sync_local_properties() {
  local android_sdk="$1" props="$SAZON_REPO_ROOT/local.properties"

  [ "${SAZON_WSL_LOCAL_PROPS:-1}" = "0" ] && return 0
  [ -f "$props" ] || return 0
  # Android Studio escribe `sdk.dir=C\:\\Users\\...` (dos puntos y barras escapados: formato de
  # propiedades de Java), así que la barra invertida delante de `:` es opcional en la detección.
  grep -qE '^sdk\.dir=[A-Za-z]\\?:' "$props" || return 0

  local tmp
  tmp="$(mktemp "$props.XXXXXX")" || return 0
  if sed -E "s|^sdk\.dir=.*$|sdk.dir=$android_sdk|" "$props" >"$tmp"; then
    cat "$tmp" >"$props"
    echo "local.properties: sdk.dir apuntado al SDK de Linux ($android_sdk)"
  fi
  rm -f "$tmp"
}

sazon_wsl_env() {
  sazon_is_wsl || return 0

  local java_home="${SAZON_WSL_JAVA_HOME:-$HOME/.local/opt/jdk-21}"
  local android_sdk="${SAZON_WSL_ANDROID_SDK:-$HOME/Android/Sdk}"

  # JDK: también es el JVM del daemon que exige gradle/gradle-daemon-jvm.properties (21), así que
  # Gradle no necesita aprovisionar otro.
  if [ -x "$java_home/bin/java" ]; then
    export JAVA_HOME="$java_home"
    sazon_path_add "$JAVA_HOME/bin"
  else
    echo "AVISO: no encuentro el JDK de Linux en $java_home." >&2
    echo "       Ejecuta 'bash scripts/wsl-bootstrap.sh' para instalarlo." >&2
  fi

  if [ -d "$android_sdk" ]; then
    export ANDROID_HOME="$android_sdk"
    export ANDROID_SDK_ROOT="$android_sdk"
    sazon_path_add "$android_sdk/platform-tools"
  else
    echo "AVISO: no encuentro el SDK de Android de Linux en $android_sdk." >&2
    echo "       Ejecuta 'bash scripts/wsl-bootstrap.sh' para instalarlo." >&2
  fi

  # Herramientas nativas del harness (gh, y lo que se instale en el futuro).
  sazon_path_add "$HOME/.local/bin"

  # La caché de Gradle (dependencias, distribuciones, JDKs aprovisionados) tiene que estar en ext4:
  # bajo /mnt/c (DrvFs/9p) cada acceso cuesta órdenes de magnitud más.
  case "${GRADLE_USER_HOME:-}" in
    "" | /mnt/*) export GRADLE_USER_HOME="${SAZON_WSL_GRADLE_HOME:-$HOME/.gradle}" ;;
  esac

  # El repositorio vive en /mnt/c: el file-watching nativo de Gradle no es fiable en DrvFs.
  case "${GRADLE_OPTS:-}" in
    *vfs.watch*) ;;
    *) export GRADLE_OPTS="${GRADLE_OPTS:+$GRADLE_OPTS }-Dorg.gradle.vfs.watch=false" ;;
  esac

  # Sin TTY, algunas herramientas intentan leer de stdin; los scripts redirigen stdin explícitamente.

  sazon_wsl_sync_local_properties "$android_sdk"
}

SAZON_REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
sazon_wsl_env
