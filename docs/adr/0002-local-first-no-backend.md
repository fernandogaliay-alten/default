# ADR 0002: Arquitectura local-first sin backend

## Estado

Aceptado.

## Contexto

El producto tiene un **Usuario local único**: no hay cuentas, ni roles, ni compartición. La app debe
funcionar en un móvil Android, y su única integración externa es la API de IA que genera las recetas.
El proyecto es de curso, con tiempo acotado y sin entorno de producción.

La pregunta de fondo: ¿dónde vive el estado del usuario y quién custodia la clave de la API de IA?

## Decisión

- **Todo el estado del usuario vive en el dispositivo**: perfil, preferencias, favoritos, lote de
  sugerencias y caché de recetas (`Room`), más los assets empaquetados.
- **No hay servidor propio.** La app habla directamente con el proveedor de IA.
- La **clave de IA se inyecta en tiempo de build** desde `local.properties` a `BuildConfig`. El APK
  resultante es **solo para demo** y no se distribuye.
- La app debe **degradar con elegancia**: si falta la clave o el proveedor falla, entra en modo
  semilla y sigue siendo utilizable.

## Alternativas consideradas

1. **Proxy propio que custodia la clave.** Es lo correcto en producción: la clave nunca sale del
   servidor. Añade backend, autenticación, despliegue, coste y superficie de ataque. Desproporcionado
   para el MVP de un curso.
2. **El usuario introduce su propia clave en Ajustes**, cifrada en el dispositivo. Elimina el backend
   y no expone la clave de nadie más, pero añade pantalla de ajustes, validación de la clave, manejo
   de errores por clave inválida y carga de soporte al usuario. Se descartó por coste de UX, no por
   seguridad: es la evolución natural si el producto se distribuyera.
3. **Cuentas con login y sincronización en la nube.** Permite multi-dispositivo y recuperación de
   datos. Es un **no-objetivo permanente** del MVP y multiplicaría el alcance.

## Consecuencias

- Home (salvo su buscador con IA), "Tus recetas" y Favoritos funcionan **sin conexión**; solo la
  Búsqueda con IA necesita red.
- **No hay multi-dispositivo ni recuperación de datos**: si se desinstala la app, se pierde todo. Es
  aceptable porque no hay cuentas, pero debe ser una decisión consciente, no un olvido.
- **La clave de IA es extraíble del APK.** Es el riesgo de seguridad principal del proyecto y está
  asumido y registrado en `docs/risks-and-open-questions.md`. Nunca se publica el APK.
- El **modo semilla** no es un caso de error, es un modo degradado esperado y diseñado.
- Añadir backend más adelante significa introducir autenticación y una capa de sincronización, con un
  coste real. Esta ADR es la que hace que ese coste exista.
