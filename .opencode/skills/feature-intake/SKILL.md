---
name: feature-intake
description: Da de alta una feature o una correctiva nueva en feature_list.json antes de lanzar el flujo. Úsala cuando la persona diga "añade una feature", "da de alta esta correctiva", "registra esta tarea en el plan", "apunta esta idea para después" o pida preparar una entrada del feature list sin implementarla. No escribe la spec ni implementa código.
---

# Feature Intake

Da de alta **una** entrada nueva en `feature_list.json` con el formato que ya usa el repositorio, lista
para que `github-flow plan|execute` la vea. Es el paso previo al flujo, no parte de él: aquí no se
planifica, no se implementa y no se valida.

## Reglas duras

- Una sola entrada por ejecución. Si la persona pide varias, procesa la primera y pregunta si sigue.
- No escribas la spec (`docs/specs/<id>.md`): eso es de `feature-spec` / `planner`.
- No implementes ni toques `app/**`, `scripts/**`, `init.sh`, `build.gradle.kts` ni ningún otro fichero.
  El único fichero que se modifica es `feature_list.json`.
- No cambies el estado ni el contenido de ninguna entrada existente.
- No inventes `depends_on`: cada id debe existir ya en `feature_list.json`.
- No hagas push por tu cuenta. El commit local sí; publicar en `origin/main` es una decisión de la
  persona (ver "Cierre").
- El JSON debe seguir siendo válido y conservar el formato: 2 espacios de sangría, UTF-8, mismas claves
  y mismo orden de claves que las entradas existentes.

## Entradas que debes leer

1. `../../../AGENTS.md` (reglas del repositorio).
2. `feature_list.json` (ids existentes, áreas, orden y formato).
3. `PROGRESS.md` (contexto de la sesión y del siguiente paso).
4. Solo si hace falta para redactar: `CONTEXT.md`, `docs/build-brief.md`,
   `docs/risks-and-open-questions.md` y `docs/technical-discovery.md`.

## Datos que necesitas

Pregunta **solo** lo que no puedas deducir de la conversación y de los documentos. Como mínimo:

1. **Tipo**: `feature` (comportamiento nuevo) o `correctiva` (arreglo o mejora acotada de algo ya
   aceptado).
2. **Título** corto, en español, sin prefijo de tipo.
3. **Comportamiento visible para el usuario**: qué nota o deja de notar. Si no cambia la app (script,
   documentación, herramienta), dilo explícitamente ("no cambia la app") y describe el efecto real.
4. **Área**: una de las ya usadas en `feature_list.json` (`data`, `domain`, `integration`, `platform`,
   `ui`). No inventes áreas nuevas.
5. **Dependencias**: ids de features `accepted` de las que dependa funcionalmente. Si la persona no lo
   tiene claro, propone tú una lista corta y explícala antes de escribirla.
6. **Motivo y alcance** para `notes`: de dónde sale la petición (fecha y contexto), qué entra y qué
   **no** entra, y cualquier decisión de producto ya tomada.
7. **Id**: deriveselo del título en kebab-case (`[a-z0-9-]`), corto y específico. Si ya existe, propone
   otro y no sobrescribas nada.

## Comprobaciones antes de escribir

- El `id` no existe ya en `feature_list.json`.
- Todas las dependencias existen y su `status` es `accepted`. Si alguna no lo es, no la metas en
  `depends_on`: explica el bloqueo y ofrece (a) esperar, (b) registrar la entrada con las dependencias
  aceptadas y una nota del bloqueo, o (c) no darla de alta todavía.
- La entrada no duplica otra existente por título o comportamiento. Si se parece a una ya registrada,
  dilo antes de crear una nueva.
- Si el alcance es tan grande que no cabe en una spec de 100-250 líneas, propone dividirlo en dos o
  tres entradas en lugar de una vaga.

## Formato de la entrada

Añádela al final del array y usa exactamente estas claves, en este orden:

```json
{
  "id": "<kebab-case>",
  "area": "<data|domain|integration|platform|ui>",
  "title": "<título>",
  "user_visible_behavior": "<qué nota el usuario>",
  "depends_on": ["<id-accepted>"],
  "status": "not_started",
  "verification": [
    "<verificación observable 1>",
    "<verificación observable 2>"
  ],
  "notes": "<motivo, fecha, alcance y no-objetivos>"
}
```

Buenas `verification`: observables, reproducibles y sin depender de la implementación. Para una feature
de UI, separa lo verificable en JVM de la prueba manual de la persona. Para una correctiva, incluye la
reproducción del defecto y una comprobación de que el resto no cambia. No inventes umbrales numéricos ni
resultados de tests.

`notes` debe dejar claro qué **no** entra. Si la entrada cambia una decisión ya documentada, dilo y
señala el documento a enmendar (ADR, `docs/technical-discovery.md`, `AGENTS.md`).

## Salida

1. Muestra la entrada propuesta y explica en dos o tres líneas por qué esas dependencias y esa
   verificación.
2. Muestra el resultado de validar el JSON (por ejemplo con `python3 -m json.tool feature_list.json` o
   una comprobación equivalente) y el `git diff --stat`.
3. Pide confirmación explícita antes de commitear.

## Cierre

Con la confirmación de la persona:

```bash
git add feature_list.json
git commit -m "chore(plan): alta de <feature|la correctiva> <id>"
```

Sigue el precedente del repositorio (`chore(plan): alta de la correctiva pr-worktree-fork-detection`).

No hagas push. Recuerda que `github-flow execute` hace un fast-forward a `origin/main`: hasta que la
entrada esté publicada en la rama base, el flujo no la verá. Pregunta si quiere publicarla y, solo con
su autorización, hala la rama base y haz push.

Termina indicando el siguiente paso: `github-flow plan <id>` y después `github-flow execute <id>`.
