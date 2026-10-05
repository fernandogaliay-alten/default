---
description: Implementa la siguiente feature (o una concreta) con el subagente implementer
agent: implementer
subtask: true
---
Carga tu skill feature-implementer y sigue su workflow.

Feature id solicitado (puede venir vacío): "$ARGUMENTS"

Si el id está vacío, selecciona la feature según la skill (prefiere `in_progress`; si no, la primera no `passing`/`accepted`, con dependencias satisfechas y con spec en `docs/specs/`). Si viene un id, implementa exactamente esa feature y deja el estado en `passing`, nunca `accepted`.
