---
description: Especifica la siguiente feature (o una concreta) con el subagente planner
agent: planner
subtask: true
---
Carga tu skill feature-spec y sigue su workflow.

Feature id solicitado (puede venir vacío): "$ARGUMENTS"

Si el id está vacío, selecciona la siguiente feature según las reglas de la skill (prefiere `in_progress`; si no, la primera en `feature_list.json` que no sea `passing` ni `accepted` y con dependencias satisfechas). Si viene un id, especifica exactamente esa feature.
