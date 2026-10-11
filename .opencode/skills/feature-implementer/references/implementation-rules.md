# Feature Implementer Rules

## Durable Documentation Rules

Code changes and durable knowledge changes travel together.

Update docs only when the change creates or changes knowledge future agents need:

- `../../../../AGENTS.md`: update only if the agent workflow, startup path, verification path, or repo-wide operating rules change. Keep it focused and non-duplicative.
- `../../../../docs/technical-discovery.md`: update when the stack, storage, integrations, verification path or durable boundaries change.
- `../../../../docs/domain-model.md`: update when entities, relationships or states change.
- `../../../../docs/specs/<feature-id>.md`: update if implementation discovers that the spec was wrong, incomplete, or materially changed by necessary findings.
- `../../../../PROGRESS.md`: update every implementation session.
- `feature_list.json`: update selected feature status and evidence every implementation session.

Do not create architecture or constraints docs just to look complete. Create them when they reduce future rediscovery or prevent future mistakes.

## Self-Verification Rules

The implementer must verify its own work, but self-verification is not final acceptance.

Feature status convention:

- `in_progress`: implementation is underway or incomplete.
- `passing`: implementation is complete enough that required self-verification passed and evidence is recorded. This means ready for independent validation, not accepted.
- `accepted`: independent validation returned `accept` and the main orchestrator persisted that result. Implementers must not set this status.
- `blocked`: implementation cannot continue with the current spec/context.

Do not use `passing` to mean final approval. Final approval is a validator verdict persisted by the main orchestrator as `accepted` in `feature_list.json`, with concise evidence in `feature_list.json` and/or `../../../../PROGRESS.md`.

## `init.sh` Rule

Once a repository has a runnable baseline, `init.sh` should be an executable non-blocking gate, not a help screen.

Default behavior:

- execute the standard checks that can run without external long-lived services,
- fail fast on errors via `set -euo pipefail`,
- avoid starting blocking commands (dev servers, watchers, emulators),
- optionally print manual follow-up commands after checks pass.

For this Android/JVM repo, the gate is:

```bash
bash init.sh                     # ./gradlew test + ./gradlew lint via scripts/gradle.sh
bash scripts/gradle.sh <tasks>   # focused checks
```

Only allow an informational-only `init.sh` during an explicitly pre-bootstrap phase.

Use the strongest applicable available checks:

1. static checks: formatting, lint, typecheck, build;
2. focused tests: unit/integration/regression;
3. runtime checks: app starts, endpoint responds, migration runs;
4. user-flow checks: browser/API/manual smoke or E2E when relevant.

Record exact commands and results. If a check is impossible in the current repo state, record the reason and the next action needed to make it possible.

## Scope Rules

- One feature per implementation session.
- No opportunistic adjacent features.
- No false `passing` status.
- If the feature grows beyond the spec, stop and update the spec or ask for a split.
