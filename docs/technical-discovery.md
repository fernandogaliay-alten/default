# Technical Discovery

## Product Surface

App **Android nativa**, orientación retrato, un único usuario local. Sin web, sin escritorio, sin
backend propio. El diseño de referencia está en `DESIGN.md`.

## Candidate Stack

- Kotlin + Jetpack Compose + Material 3.
- Android: **`minSdk` 33** (Android 13), **`targetSdk` 36** y **`compileSdk` 36**. Decisión de curso:
  no se soportan versiones anteriores a Android 13.
- Toolchain del bootstrap: Gradle 9.5.0 (wrapper), AGP 9.3.3, Kotlin 2.2.10, Compose BOM 2026.02.01.
- `applicationId` / paquete base: **`com.sazon.app`**. Barato de cambiar ahora; caro tras publicar.
- Room para persistencia local; DataStore para flags de aplicación (p. ej. "onboarding completado").
- Coroutines + Flow para asincronía.
- Cliente HTTP (Retrofit o Ktor) + `kotlinx.serialization` para el proveedor de IA.
- Inyección de dependencias ligera. En un proyecto de curso, manual o Koin es suficiente; Hilt añade
  ceremonia.
- Tests: JUnit en JVM. El dominio no debe depender de Android para poder testarse sin emulador.

## Data and Storage

| Qué | Dónde | Por qué |
|---|---|---|
| Taxonomía | `app/src/main/assets/taxonomy.json`, generado por la tarea `syncTaxonomy` desde `docs/domain/taxonomy.json`, y parseado a objetos tipados al arrancar | Fuente única editable y artefacto empaquetado, sin base de datos |
| Recetas semilla | **Pendiente**: JSON en assets o código (ver pregunta 11 de riesgos) | Contenido empaquetado, siempre disponible offline |
| Perfil de usuario (identidad) | Room (1 fila, compartida con preferencias) | Nombre y color de avatar |
| Perfil de preferencias | Room (1 fila) + DataStore para el flag de onboarding | Estado mutable del usuario; fuente de la firma |
| Resumen de actividad | Contador `recipesGeneratedCount` en Room + COUNT de Favoritos | El contador se persiste; el conteo de favoritos se deriva |
| Favoritos | Room | Disponibilidad offline garantizada |
| Lote de sugerencias y recetas `ai` | Room | Evita llamadas repetidas a la IA y da soporte offline |
| Iconos e imágenes de receta | Assets locales (`assets/icons/**`, `assets/img/recipes/**`) | Determinista, offline, sin coste |
| Clave de IA | `BuildConfig`, desde `local.properties` | Decisión de demo; ver riesgos |

La Taxonomía **no** se persiste en base de datos: se empaqueta con la app y es idéntica entre
versiones. Los `id` nunca se renombran.

`docs/domain/taxonomy.json` es la única fuente editable. La tarea de Gradle `syncTaxonomy` la copia a
`app/src/main/assets/`, que está ignorado en git. El parseo vive en `com.sazon.app.domain.taxonomy`
(`TaxonomyJson.parse`, sin dependencias de Android) y la lectura del asset en `com.sazon.app.data`
(`AssetTaxonomySource`), de modo que los tests corren en JVM sin emulador. Ver
`docs/specs/taxonomy-in-code.md`.

Decisiones de arquitectura que afectan a este documento: `docs/adr/0001-taxonomy-in-code.md` y
`docs/adr/0002-local-first-no-backend.md`.

## Integrations

- **API de IA**: única integración externa. El proveedor **está por decidir**.
- El dominio depende de la interfaz `RecipeGenerator` (tres operaciones: sugerir por preferencias,
  buscar por texto y generar desde ingredientes), **no** de un SDK concreto. La implementación real
  es lo último que se conecta.
- `FakeRecipeGenerator` (datos fijos, sin red) es la implementación por defecto en desarrollo y
  tests, y permite construir y validar toda la app antes de elegir proveedor.
- **Búsqueda con IA**: no se persiste un caché consulta→resultado; se memoiza en memoria por sesión
  para evitar llamadas repetidas al volver atrás. Las recetas devueltas se guardan como recetas `ai`
  en la caché normal.
- No hay analítica, ni telemetría remota, ni backend.

## Authentication and Authorization

No aplica: no hay cuentas, roles, ni recursos compartidos. Todos los datos pertenecen al usuario del
dispositivo y no salen de él, salvo la petición de generación a la API de IA.

## Deployment and Operations

- Distribución como APK de depuración para el curso. **No** Play Store en el MVP.
- La clave de IA se inyecta en tiempo de build desde `local.properties` (que no se versiona).
- Sin CI obligatoria. El harness usa `init.sh` localmente.
- Operación: la asume el propio estudiante; no hay entorno de producción.

## Testing and Verification

- **Tests unitarios de dominio en JVM** (obligatorios): integridad de la Taxonomía, `PromptBuilder`,
  parsing y validación de la respuesta, descarte por Restricciones duras, firma de preferencias y
  política de caché, escalado de raciones.
- `FakeRecipeGenerator` disponible para el flujo completo sin red.
- **Prueba manual end-to-end** en emulador (ver `docs/build-brief.md` → Validation Plan).
- `init.sh` debe ejecutar los tests de forma reproducible y ser el punto de entrada de verificación
  del harness.

## Observability

- Sin telemetría. El diagnóstico se hace con **log local de eventos de IA**: proveedor, código de
  respuesta, duración y **motivo de descarte del validador**.
- El motivo del descarte es la señal más valiosa del producto: indica si el problema es el prompt o
  el modelo.

## Constraints

- **Sin backend**: la clave de IA viaja en el APK → solo válido para demo.
- **Coste y cuota** del proveedor desconocidos hasta decidirlo → la política cache-first y el modo
  semilla son también una protección de coste.
- **Red intermitente**: solo la Búsqueda con IA necesita red; el Lote de sugerencias, "Tus recetas" y
  Favoritos deben funcionar sin conexión.
- **Tamaño del APK**: las imágenes locales de receta suman; conviene formato WebP y resolución
  moderada.
- **Tiempo de curso**: el MVP debe priorizar el corte vertical sobre la amplitud de features.
- **Complejidad de build**: Kotlin/Compose conlleva Gradle, SDK y emulador; es el mayor coste de
  arranque del proyecto.
