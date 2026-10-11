#!/usr/bin/env bash
set -euo pipefail

# Provisiona, sin sudo y todo bajo $HOME, lo que el proyecto necesita dentro de WSL:
#   - JDK 21 (Temurin)               -> ~/.local/opt/jdk-21
#   - Android SDK de Linux           -> ~/Android/Sdk (cmdline-tools + platform + build-tools)
#   - GitHub CLI (`gh`)              -> ~/.local/opt/gh (+ enlace en ~/.local/bin)
#   - caché de Gradle en ext4        -> ~/.gradle (reutiliza la distribución ya descargada en Windows)
#   - credenciales de `gh`           -> ~/.config/gh (si la persona ya había autenticado en Windows)
#
# Por qué no vale lo de Windows: el SDK de Windows solo trae binarios .exe (aapt2.exe, adb.exe,
# zipalign.exe) y Gradle en Linux no puede ejecutarlos; el `java` del PATH es el de Windows.
#
# Es idempotente: lo que ya existe se reutiliza y solo se descarga lo que falta. Sin `sudo`.
#
# Uso:
#   bash scripts/wsl-bootstrap.sh
#
# Variables de entorno opcionales:
#   SAZON_WSL_JAVA_HOME     JDK a instalar (por defecto: ~/.local/opt/jdk-21)
#   SAZON_WSL_ANDROID_SDK   SDK a instalar (por defecto: ~/Android/Sdk)
#   SAZON_WSL_GRADLE_HOME   caché de Gradle (por defecto: ~/.gradle)
#   SAZON_SDK_PACKAGES      paquetes de sdkmanager (por defecto: platform-tools,
#                           platforms;android-36.1 y build-tools;36.0.0)
#   SAZON_CMDLINE_TOOLS     revisión del zip de command-line tools (por defecto: 13114758)
#   SAZON_GH_VERSION        versión de `gh` (por defecto: la de gh --version en Windows, o 2.102.0)
#   SAZON_WSL_NO_REUSE      =1 para no reutilizar la caché de Gradle de Windows

JDK_DIR="${SAZON_WSL_JAVA_HOME:-$HOME/.local/opt/jdk-21}"
ANDROID_SDK="${SAZON_WSL_ANDROID_SDK:-$HOME/Android/Sdk}"
GRADLE_HOME="${SAZON_WSL_GRADLE_HOME:-$HOME/.gradle}"
GH_DIR="$HOME/.local/opt/gh"
WORK="$HOME/.cache/sazon-wsl"
CMDLINE_TOOLS_REV="${SAZON_CMDLINE_TOOLS:-13114758}"
GH_VERSION="${SAZON_GH_VERSION:-2.102.0}"
# 36 es el compileSdk del proyecto (con minorApiLevel 1 -> plataforma android-36.1).
read -r -a SDK_PACKAGES <<<"${SAZON_SDK_PACKAGES:-platform-tools platforms;android-36.1 build-tools;36.0.0}"

log() { printf '\n== %s ==\n' "$*"; }
warn() { printf 'AVISO: %s\n' "$*" >&2; }

is_wsl() {
  [ -n "${WSL_DISTRO_NAME:-}" ] && return 0
  [ -r /proc/version ] && grep -qi microsoft /proc/version
}

# Ruta de un directorio de Windows (p. ej. %APPDATA%) traducida al sistema de ficheros de WSL.
windows_dir() {
  local cmd win_path
  # `cmd.exe` se busca por nombre (interop con el PATH de Windows) y, si no, por ruta absoluta.
  cmd="$(command -v cmd.exe 2>/dev/null || echo /mnt/c/Windows/System32/cmd.exe)"
  [ -x "$cmd" ] || return 1
  win_path="$("$cmd" /c "echo %$1%" 2>/dev/null | tr -d '\r\n')" || return 1
  [ -n "$win_path" ] || return 1
  wslpath -u "$win_path" 2>/dev/null
}

download() {
  local url="$1" dest="$2"
  if [ -s "$dest" ]; then
    log "ya descargado: $(basename "$dest")"
    return 0
  fi
  log "descargando $(basename "$dest")"
  curl -fSL --retry 3 --retry-delay 2 --progress-bar -o "$dest.part" "$url"
  mv "$dest.part" "$dest"
}

if ! is_wsl; then
  echo "Este script es solo para WSL (no se ha detectado /proc/version de Microsoft)." >&2
  exit 2
fi

for tool in curl tar unzip; do
  command -v "$tool" >/dev/null || {
    echo "Falta '$tool'. Instálalo con: sudo apt-get update && sudo apt-get install -y $tool" >&2
    exit 2
  }
done

mkdir -p "$WORK"

log "JDK 21 (Temurin) en $JDK_DIR"
if [ -x "$JDK_DIR/bin/java" ]; then
  echo "ya instalado: $("$JDK_DIR/bin/java" -version 2>&1 | head -1)"
else
  download "https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse" \
    "$WORK/jdk21.tar.gz"
  rm -rf "$JDK_DIR.tmp"
  mkdir -p "$JDK_DIR.tmp"
  tar -xzf "$WORK/jdk21.tar.gz" -C "$JDK_DIR.tmp" --strip-components=1
  mkdir -p "$(dirname "$JDK_DIR")"
  rm -rf "$JDK_DIR"
  mv "$JDK_DIR.tmp" "$JDK_DIR"
  echo "instalado: $("$JDK_DIR/bin/java" -version 2>&1 | head -1)"
fi

log "Android SDK de Linux en $ANDROID_SDK"
SDKMANAGER="$ANDROID_SDK/cmdline-tools/latest/bin/sdkmanager"
cmdline_version() {
  sed -nE 's/^Pkg.Revision=(.*)$/\1/p' \
    "$ANDROID_SDK/cmdline-tools/latest/source.properties" 2>/dev/null | tr -d '\r'
}
if [ -x "$SDKMANAGER" ]; then
  echo "ya instalado (cmdline-tools $(cmdline_version))"
else
  download "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_REV}_latest.zip" \
    "$WORK/cmdline-tools.zip"
  rm -rf "$ANDROID_SDK/cmdline-tools/.tmp" "$ANDROID_SDK/cmdline-tools/latest"
  mkdir -p "$ANDROID_SDK/cmdline-tools"
  unzip -q "$WORK/cmdline-tools.zip" -d "$ANDROID_SDK/cmdline-tools/.tmp"
  mv "$ANDROID_SDK/cmdline-tools/.tmp/cmdline-tools" "$ANDROID_SDK/cmdline-tools/latest"
  rmdir "$ANDROID_SDK/cmdline-tools/.tmp"
  echo "instalado: cmdline-tools $(cmdline_version)"
fi

log "Licencias del SDK"
# `yes` responde a todas las preguntas; si ya estaban aceptadas es un no-op.
yes | JAVA_HOME="$JDK_DIR" "$SDKMANAGER" --sdk_root="$ANDROID_SDK" --licenses >/dev/null || true

log "Paquetes del SDK: ${SDK_PACKAGES[*]}"
JAVA_HOME="$JDK_DIR" "$SDKMANAGER" --sdk_root="$ANDROID_SDK" "${SDK_PACKAGES[@]}" </dev/null

log "GitHub CLI en $GH_DIR"
if [ -x "$GH_DIR/bin/gh" ]; then
  echo "ya instalado: $("$GH_DIR/bin/gh" --version | head -1)"
else
  download "https://github.com/cli/cli/releases/download/v${GH_VERSION}/gh_${GH_VERSION}_linux_amd64.tar.gz" \
    "$WORK/gh.tar.gz"
  rm -rf "$GH_DIR"
  mkdir -p "$GH_DIR"
  tar -xzf "$WORK/gh.tar.gz" -C "$GH_DIR" --strip-components=1
  mkdir -p "$HOME/.local/bin"
  ln -sf "$GH_DIR/bin/gh" "$HOME/.local/bin/gh"
  echo "instalado: $("$GH_DIR/bin/gh" --version | head -1)"
fi
mkdir -p "$HOME/.local/bin"
ln -sf "$GH_DIR/bin/gh" "$HOME/.local/bin/gh"

if ! "$HOME/.local/bin/gh" auth status >/dev/null 2>&1; then
  log "Credenciales de gh"
  # `gh` en Windows guarda el token en el Administrador de credenciales (keyring), no en hosts.yml,
  # así que copiar el fichero no basta: se le pide el token al gh de Windows y se reusa aquí.
  win_token=""
  if command -v gh.exe >/dev/null 2>&1 || [ -x "/mnt/c/Program Files/GitHub CLI/gh.exe" ]; then
    gh_win="$(command -v gh.exe 2>/dev/null || echo "/mnt/c/Program Files/GitHub CLI/gh.exe")"
    win_token="$("$gh_win" auth token 2>/dev/null | tr -d '\r\n' || true)"
  fi
  if [ -n "$win_token" ]; then
    printf '%s' "$win_token" | "$HOME/.local/bin/gh" auth login --with-token >/dev/null 2>&1 ||
      warn "gh auth login --with-token falló"
    chmod 600 "$HOME/.config/gh/hosts.yml" 2>/dev/null || true
    unset win_token
  elif [ -n "$(windows_dir APPDATA || true)" ] &&
    [ -f "$(windows_dir APPDATA || true)/GitHub CLI/hosts.yml" ]; then
    mkdir -p "$HOME/.config/gh"
    cp "$(windows_dir APPDATA || true)/GitHub CLI/hosts.yml" "$HOME/.config/gh/hosts.yml"
    chmod 600 "$HOME/.config/gh/hosts.yml" 2>/dev/null || true
    echo "copiada la configuración de gh de Windows"
  fi
  if "$HOME/.local/bin/gh" auth status >/dev/null 2>&1; then
    echo "sesión de gh reutilizada: $("$HOME/.local/bin/gh" auth status 2>&1 | grep -o 'account [^ ]*' | head -1)"
  else
    warn "gh no está autenticado; ejecuta 'gh auth login' antes de publicar (push/PR)"
  fi
fi

# La caché de Gradle (dependencias + distribuciones) es enorme y el sistema de ficheros de Windows
# es mucho más lento: se queda en ext4 y, si existe ya en Windows, se reutiliza la distribución.
log "Caché de Gradle en $GRADLE_HOME"
mkdir -p "$GRADLE_HOME"
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# El fichero lo genera Gradle/Android Studio y puede traer CRLF: se quitan antes de parsear.
dist_name="$(tr -d '\r' <"$repo_root/gradle/wrapper/gradle-wrapper.properties" |
  sed -nE 's|^distributionUrl=.*/([^/]+)\.zip$|\1|p')"
if [ "${SAZON_WSL_NO_REUSE:-0}" != "1" ] && [ -n "$dist_name" ] &&
  [ ! -d "$GRADLE_HOME/wrapper/dists/$dist_name" ]; then
  win_home="$(windows_dir USERPROFILE || true)"
  if [ -n "$win_home" ] && [ -d "$win_home/.gradle/wrapper/dists/$dist_name" ]; then
    mkdir -p "$GRADLE_HOME/wrapper/dists"
    echo "reutilizando la distribución $dist_name ya descargada en Windows..."
    cp -r "$win_home/.gradle/wrapper/dists/$dist_name" "$GRADLE_HOME/wrapper/dists/" ||
      warn "no he podido copiar la distribución; Gradle la descargará sola"
  fi
fi

log "Listo"
cat <<EOF

Entorno instalado sin sudo:
  JAVA_HOME      $JDK_DIR
  ANDROID_HOME   $ANDROID_SDK
  GRADLE_USER_HOME $GRADLE_HOME
  gh             $HOME/.local/bin/gh

Los scripts del repo (init.sh, scripts/gradle.sh, scripts/new-worktree.sh) ya cargan
scripts/wsl-env.sh solos, así que 'bash init.sh' funciona tal cual.
Para tener java/gh/adb también en tus shells interactivas, añade a ~/.profile:

  # >>> sazón (WSL) >>>
  export JAVA_HOME="$JDK_DIR"
  export ANDROID_HOME="$ANDROID_SDK"
  export PATH="\$HOME/.local/bin:\$JAVA_HOME/bin:\$ANDROID_HOME/platform-tools:\$PATH"
  # <<< sazón (WSL) <<<
EOF
