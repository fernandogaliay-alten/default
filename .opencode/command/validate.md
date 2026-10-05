---
description: Valida la feature en curso (o una concreta) con el subagente validator
agent: validator
subtask: true
---
Carga tu skill feature-validator y sigue su workflow.

Feature id solicitado (puede venir vacío): "$ARGUMENTS"

Si el id está vacío, selecciona la feature a validar según la skill (prefiere `in_progress`; si no, la última `passing` con evidencia). Si viene un id, valida exactamente esa feature. Devuelve accept/revise/block y no modifiques ficheros.
