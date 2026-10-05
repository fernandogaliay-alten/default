---
description: Flujo completo spec -> implement -> validate con feature-flow
agent: build
---
Carga la skill feature-flow y orquesta el flujo para una sola feature.

Feature id solicitado (puede venir vacío): "$ARGUMENTS"

Si el id está vacío, selecciona la siguiente feature según feature-flow y mantén ese mismo id en las tres fases. Si viene un id, usa exactamente ese. Persiste la aceptación y crea el commit al recibir `accept`, como indica la skill.
