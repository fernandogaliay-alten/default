# Feature Implementation Spec Template

Use this structure for `../../../../docs/specs/<feature-id>.md`.

```md
# Feature Implementation Spec: <feature title>

## Source Feature

- `id`: <feature-id>
- `area`: <area>
- `depends_on`: <feature ids this feature depends on>
- `status`: <status at planning time>
- `source`: `feature_list.json`

## Goal

<One or two paragraphs describing the outcome this feature must enable.>

## Non-Goals

- <Behavior or technical work explicitly outside this slice.>
- <Adjacent feature that must not be pulled in.>

## Job Story

When <situation/context>,
I want to <motivation/action>,
so I can <expected outcome>.

## Users And Permissions

- <Actor/role>: <allowed behavior and constraints>

## Acceptance Scenarios

### Scenario 1: <happy path name>

Given <initial state>
When <user/system action>
Then <observable result>

### Scenario 2: <edge/error path name>

Given <initial state>
When <user/system action>
Then <observable result>

## Repository Research

### Files Inspected

- `<path>` — <why it matters>

### Existing Patterns To Follow

- <Pattern, convention, command, or architectural choice found in the repo.>

### Current Gaps

- <Missing app infrastructure, missing command, missing table, unknown integration, etc.>

## Technical Approach

<Describe the implementation strategy at a high enough level that another agent can execute it without re-planning.>

## Expected File Changes

- `<path>` — create/modify; <reason>
- `<path>` — create/modify; <reason>

If paths are provisional because the app is not bootstrapped yet, say so explicitly.

## Visual Design Impact

- UI involved: yes/no
- Design source: `../../../../DESIGN.md` / existing design asset / not applicable
- Screens or states affected: <list>
- New design artifact required: yes/no — <reason>

For UI features, name the concrete capture file(s) under
`../../../../docs/design/concepts/<feature-id>/` that must exist before implementation, e.g.:

- `UI captures (required)`: `../../../../docs/design/concepts/<feature-id>/<screen>.<ext>` (light and dark if both apply)

If a required capture is missing, create `../../../../docs/design/concepts/<feature-id>/README.md`
listing the exact expected file names, then stop and request them from the user instead of emitting this
spec (the feature is blocked pending design input).

If UI is involved, follow `../../../../DESIGN.md` and identify any feature-specific visual states the implementer must handle. If `../../../../DESIGN.md` is missing but needed, mark it as a planning gap rather than letting the implementer invent a visual style.

## Durable Documentation Impact

- `../../../../docs/technical-discovery.md`: create/update/not needed — <reason>
- `../../../../docs/domain-model.md`: create/update/not needed — <reason>
- `../../../../AGENTS.md`: update/not needed — <reason>
- Other docs: <path or none> — <reason>

Use these rules:

- Update `../../../../AGENTS.md` only when the agent workflow, startup path, or repo-wide operating rules change.
- Create or update `../../../../docs/technical-discovery.md` when this feature changes the stack, storage, integrations, verification path or other durable boundaries.
- Create or update `../../../../docs/domain-model.md` when this feature changes entities, relationships or states; a durable MUST/MUST NOT rule goes to `../../../../AGENTS.md` or `docs/technical-discovery.md`.
- Avoid duplicating product behavior already captured in discovery docs unless the implemented behavior changes the source of truth.

## Implementation Plan

1. <First implementation step.>
2. <Second implementation step.>
3. <Final integration/update step.>

## Implementation Tasks

- [ ] <Small executable task.>
- [ ] <Small executable task.>
- [ ] <Small executable task.>

## Verification Plan

- <Command or manual scenario to run.>
- <Expected result.>
- If the repo has a persistent E2E command and this feature changes user-visible behavior, authentication, authorization, routing, or API flows, add or update focused E2E coverage and include the E2E command here. If E2E is not appropriate (this repo: UI is verified by JVM logic/state and the mandatory captures), state why.

For `init.sh`, be explicit:

- `bash init.sh` should execute the non-blocking standard gate for the current repo state (`./gradlew test` + `./gradlew lint`).
- `bash init.sh` must not start long-running processes (dev servers, watchers, emulators).
- It may print manual follow-up commands after the non-blocking checks pass.

## Evidence To Capture

- <Command output, screenshot, test name, log line, or manual result to record in `feature_list.json` or `../../../../PROGRESS.md`.>

## Validator Checklist

- [ ] Implementation stays within this feature's scope.
- [ ] Acceptance scenarios pass.
- [ ] Verification evidence is present.
- [ ] Persistent E2E coverage was added/updated when the feature has an observable user/API flow and an E2E harness exists, or the spec explains why it is not needed.
- [ ] `feature_list.json` and `../../../../PROGRESS.md` were updated correctly.
- [ ] For UI features, the spec's `Visual Design Impact` names captures under `../../../../docs/design/concepts/<feature-id>/` that exist, and the implementation follows them.
- [ ] No unrelated product behavior or extra feature work was added.
```
