---
description: Revisa, repara y mergea PRs abiertos con el orquestador pr-flow
agent: build
---
Carga la skill pr-flow y orquesta la revisión de pull requests.

Alcance solicitado (puede venir vacío): "$ARGUMENTS"

Si viene un número de PR o un feature id, trabaja solo ese PR. Si está vacío, procesa todos los PRs
abiertos contra la rama base, uno a uno en orden de número. Lanza el subagente `reviewer` para
revisar, delega las reparaciones (`revise`) en `implementer`, resuelve los conflictos como único
integrador y mergea con **merge commit** (`gh pr merge <n> --merge`) solo cuando el reviewer devuelva
`accept`, no haya conflictos y no haya checks requeridos en rojo. Nunca squash, nunca rebase, nunca
auto-merge, nunca force-push.
