# Instrucciones para agentes — Sazón

Este repositorio contiene **Sazón**, una app Android que convierte los ingredientes que el usuario ya
tiene en casa en recetas completas generadas con IA.

## Leer primero

- `CONTEXT.md` — vocabulario del dominio. Si un término no está aquí, no lo inventes.
- `docs/build-brief.md` — problema, alcance del MVP y no-objetivos.
- `docs/domain-model.md` — entidades, relaciones y estados.
- `docs/risks-and-open-questions.md` — riesgos e incógnitas sin resolver.

Leer solo cuando sea relevante para la tarea:

- `docs/technical-discovery.md` — stack, almacenamiento, integraciones y verificación.
- `docs/domain/taxonomy.md` — modelo de la Taxonomía y reglas del Validador.
- `docs/design/stitch-prompt.md` — pantallas y microcopy de referencia.
- `DESIGN.md` — dirección visual y tokens.
- `docs/adr/` — decisiones aceptadas. Consúltalas si algo parece contradecirlas.

## Flujo de arranque

Antes de escribir código:

1. Confirma el directorio de trabajo.
2. Lee `PROGRESS.md` para conocer el estado verificado y el siguiente paso.
3. Lee `feature_list.json` y toma la primera feature lista sin terminar, en orden de lista.
4. Ejecuta el camino de arranque (ver "Notas de entorno" para el comando exacto en Windows).
5. Si la verificación base falla, arréglala antes de empezar trabajo nuevo.

## Reglas de trabajo

- Una feature a la vez.
- No marques una feature como hecha solo porque hayas añadido código.
- Mantén los cambios dentro del alcance de la feature elegida.
- No cambies las reglas de verificación durante la implementación.
- Actualiza los artefactos del repositorio en lugar de depender de resúmenes de chat.

## Artefactos obligatorios

- `feature_list.json` — fuente de verdad del estado de las features.
- `PROGRESS.md` — estado verificado y registro ligero de sesiones.
- `init.sh` — camino estándar de arranque y verificación.

## Definición de hecho

Una feature está hecha solo si se cumplen todas:

- el comportamiento objetivo está implementado,
- la verificación exigida se ejecutó de verdad,
- la evidencia queda registrada en `feature_list.json` o `PROGRESS.md`,
- el repositorio sigue siendo reiniciable desde el camino estándar,
- la documentación relevante está actualizada si cambió el comportamiento o las reglas.

## Fin de sesión

1. Actualiza `PROGRESS.md`.
2. Actualiza `feature_list.json`.
3. Registra riesgos o bloqueos sin resolver.
4. Deja el repositorio listo para que la siguiente sesión pueda verificar de inmediato.

## Notas de entorno

- Plataforma: **Windows**. `init.sh` es un script bash, no un ejecutable de PowerShell. Se invoca con
  `bash init.sh` si `C:\Program Files\Git\bin` está en el PATH; si no, con la ruta completa:

  ```powershell
  & "C:\Program Files\Git\bin\bash.exe" init.sh
  ```

  En este equipo `Git\bin` se añadió al PATH de sistema, pero las sesiones abiertas antes del cambio
  no lo ven.
- El proyecto Android **está bootstrapeado**: `init.sh` ejecuta la verificación real
  (`./gradlew test` y `./gradlew lint`) y **no** arranca procesos largos.
- `java` en el PATH es **JDK 11**, insuficiente para Android Gradle Plugin. El wrapper arranca con
  `JAVA_HOME` (el JBR de Android Studio, **JDK 17**) y el daemon corre con un **JDK 21** que Gradle
  aprovisiona solo, según `gradle/gradle-daemon-jvm.properties`. No asumas que el `java` del PATH
  sirve para compilar.
- El SDK de Android está en `%LOCALAPPDATA%\Android\Sdk` y se localiza vía `local.properties`
  (`sdk.dir`), que está fuera de control de versiones. `ANDROID_HOME` no está definido.
- Al hacer el primer commit, marca el script como ejecutable para no perder el bit en otros entornos:
  `git add --chmod=+x init.sh`.
