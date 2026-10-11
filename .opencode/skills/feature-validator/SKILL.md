---
name: feature-validator
description: Independently validate one implemented feature against a feature spec file under docs/specs, feature_list.json, PROGRESS.md, and the current diff. Use when the user asks to validate, review, QA, evaluate, or accept/reject a feature after implementation. The skill checks behavior, verification evidence, scope discipline, architecture, security, durable documentation, and handoff readiness, then returns accept/revise/block.
---

# Feature Validator

Use this skill after an implementation session. The validator is independent from the implementer: it does not trust self-reported completion and judges the work against repo artifacts and runtime evidence.

## Role Boundary

You are the validator/evaluator, not the planner and not the implementer.

- Do inspect the spec, diff, harness state, and verification evidence.
- Do rerun checks when feasible and useful.
- Do review architecture, security, maintainability, documentation, and scope.
- Do produce a clear verdict: `accept`, `revise`, or `block`.
- Do give concrete, executable repair instructions for every finding that prevents acceptance.
- Do not implement broad fixes during validation unless the user explicitly asks for fixes.
- Do not accept based only on the implementer's confidence or summary.
- Treat `passing` in `feature_list.json` as "ready for validation", not as already accepted. Treat `accepted` as already independently accepted unless the user explicitly asks for revalidation.

## Inputs To Read First

1. `../../../AGENTS.md`
2. `../../../PROGRESS.md`
3. `feature_list.json`
4. Selected feature spec: `../../../docs/specs/<feature-id>.md`
5. Current git status and diff
6. Durable docs if present (this repo: `docs/technical-discovery.md`, `docs/domain-model.md`, `DESIGN.md`; there is no `ARCHITECTURE.md`/`CONSTRAINTS.md`):
   - `../../../docs/technical-discovery.md`
   - `../../../docs/domain-model.md`
   - `../../../DESIGN.md` when the spec or implementation has UI or visual behavior
   - related docs under `docs/`
7. Files changed by the implementation.

If no feature id is provided, choose the feature currently `in_progress`. If none exists, choose the most recently evidenced `passing` feature. Do not choose features whose status is `accepted` unless the user explicitly asks to revalidate them. If ambiguous, ask for the feature id.

## Workflow

### 1. Establish Review Target

Identify:

- selected feature id,
- spec path,
- implementation diff,
- claimed status in `feature_list.json`,
- evidence recorded in `feature_list.json` and `../../../PROGRESS.md`.

Status convention:

- `in_progress`: implementation may still be underway; validate only if the user asks for interim review.
- `passing`: implementer self-verification passed; this is the normal state to validate.
- `accepted`: independent validator acceptance has already been persisted by the main orchestrator.
- validator `accept`: independent acceptance verdict; the main orchestrator should persist it as status `accepted` after the validator reports.

If there is no spec, stop: validation needs a contract.

### 2. Validate Against The Spec

Check:

- goal satisfied,
- non-goals respected,
- acceptance scenarios pass or have convincing evidence,
- expected file changes are present or deviations are justified,
- implementation tasks were completed or explicitly deferred,
- verification plan was followed,
- for UI features, the spec named capture file(s) under `../../../docs/design/concepts/<feature-id>/`
  that existed at implementation time, and the implementation follows them (with `../../../DESIGN.md` as
  the overriding source of truth). Missing captures or an unnamed visual baseline is a `revise` (or
  `block`) finding.

### 3. Rerun Or Inspect Verification

Run the repo standard gate and focused checks when practical. If checks are expensive, unavailable, or require external services, inspect recorded evidence and state what was not rerun.

If `init.sh` exists after a runnable baseline has been created, treat it as the standard non-blocking startup/verification gate. It should execute the relevant checks and fail on errors. If it only prints commands while the repo is already bootstrapped, raise a `revise` finding with a concrete repair brief. It must not start long-running processes such as a dev server.

#### Gradle daemon hygiene

When you need to run Gradle **directly** for verification (e.g. `--rerun-tasks`), use the wrapper `bash scripts/gradle.sh <tasks>`: it sends the build output to a file, so no surviving Gradle/Kotlin daemon inherits the shell's stdout (which can leave the shell tool hanging forever, even after `BUILD SUCCESSFUL`). Do not call `./gradlew --stop`: it is global and kills the builds of other worktrees running in parallel.

```bash
timeout "${SAZON_BUILD_TIMEOUT:-1800}" bash scripts/gradle.sh testDebugUnitTest --rerun-tasks
timeout "${SAZON_BUILD_TIMEOUT:-1800}" bash scripts/gradle.sh lint --rerun-tasks
```

Use this hierarchy:

1. static/syntax checks,
2. tests and runtime/startup checks,
3. persistent E2E checks when the repo has them (this repo does not; UI is verified by JVM logic/state plus the captures),
4. user-flow or manual smoke checks when persistent E2E is not yet available or cannot cover the case.

If the repo has a persistent E2E command and the feature changes user-visible behavior, authentication, authorization, routing, or API flows, verify that focused E2E coverage was added/updated or that the spec/implementation gives a credible reason it was not needed. Missing relevant E2E coverage is normally a `revise` finding once the E2E harness exists.

### 4. Review Quality And Risk

Use `references/validation-rubric.md` to evaluate:

- correctness,
- verification evidence,
- scope discipline,
- architecture compliance,
- security/privacy/access-control risk,
- maintainability,
- durable documentation,
- handoff readiness.

Also read and apply `references/security-checklist.md` for a portable feature-scoped security review. Security review should be scoped to the feature, current diff, and directly supporting files, not a full repository audit unless requested.

### 5. Check Durable Documentation

Validate the spec's `Durable Documentation Impact` section:

- required docs were created/updated,
- unnecessary docs were not created,
- `../../../AGENTS.md` stayed focused and non-duplicative (it is allowed to carry repo-wide policy),
- `../../../docs/technical-discovery.md` captures durable boundaries (stack, storage, integrations) without becoming a file inventory,
- durable MUST/MUST NOT rules live in `../../../AGENTS.md` or `docs/technical-discovery.md` rather than vague preferences,
- docs do not contradict the implemented behavior.
- UI or visual changes follow `../../../DESIGN.md` when the feature involves product screens, public verification pages, or generated design assets.
- For UI features, the mandatory captures under `../../../docs/design/concepts/<feature-id>/` named by the spec exist and were treated as required input.

### 6. Produce Verdict

Return one of:

- `accept`: feature satisfies the spec, verification evidence is adequate, and harness/docs are consistent.
- `revise`: feature is close but needs specific fixes; list required changes.
- `block`: validation cannot continue or the implementation is fundamentally unsafe/wrong; list blocker and required next action.

For each finding that leads to `revise` or `block`, include:

- severity,
- evidence,
- why it matters,
- required change,
- suggested implementation steps,
- verification after fix.

If the verdict is `revise`, also include an `Implementation Repair Brief` that can be handed directly to the implementer. It should be ordered, scoped, and executable without this chat.

Do not mark final acceptance in files unless the user explicitly asks. In normal feature-flow orchestration, the main orchestrator persists validator `accept` by setting the feature status to `accepted` in `feature_list.json` and recording concise evidence. If you are run directly as validator, return the verdict and recommended state update instead of editing files.

## Output Summary

Report:

- verdict,
- feature id and spec path,
- checks rerun and results,
- findings by severity,
- implementation repair brief when the verdict is `revise` or `block`,
- security assessment summary, including whether the security checklist found no relevant issues, notes, or blocking findings,
- documentation/harness state assessment,
- required follow-up if any.
