---
name: pr-review
description: Independently review one open pull request against its feature spec, feature_list.json, PROGRESS.md, the PR diff, and the repository rules. Use when the user asks to review, QA, or evaluate a pull request, to check a PR for errors or conflicts before merge, or to accept/reject a PR. The skill reports conflict/mergeability state and returns accept, revise, or block with an actionable repair brief. It does not edit, commit, push, or merge.
---

# PR Review

Use this skill to review one open pull request **before** it is merged. The reviewer is independent from the author/implementer: it does not trust self-reported completion and judges the PR against repository artifacts, the diff, and runtime evidence. It reports findings and a verdict; it never changes the branch.

## Role Boundary

You are the reviewer, not the planner, not the implementer, and not the merger.

- Do inspect the PR metadata and diff, the spec, harness state, and verification evidence.
- Do report the PR's conflict/mergeability state explicitly (the orchestrator resolves conflicts; you only detect and escalate them).
- Do rerun checks when feasible and useful.
- Do review architecture, security, maintainability, documentation, and scope.
- Do produce a clear verdict: `accept`, `revise`, or `block`, each paired with a conflict status.
- Do give concrete, executable repair instructions for every finding that prevents acceptance.
- Do not edit any file, commit, push, merge, or open/close/edit the PR.
- Do not resolve conflicts yourself. If a conflict needs code judgement, say so in the repair brief so the orchestrator routes it to the implementer.
- Do not accept based only on the PR description, the author's summary, or green-looking checks.
- Treat the PR body's acceptance/validation claims as claims to verify against the diff and evidence, not as proof.

## Inputs To Read First

1. `../../../AGENTS.md`
2. `../../../PROGRESS.md`
3. `feature_list.json`
4. Selected feature spec if it exists: `../../../docs/specs/<feature-id>.md`
5. The PR itself:
   - `gh pr view <n> --json number,title,headRefName,baseRefName,mergeable,mergeStateStatus,state,isDraft,url,body,files,statusCheckRollup`
   - `gh pr diff <n>`
6. Current git status of the PR worktree (when available) and the branch commits (`git log --oneline origin/<base_branch>..<head>`, `git diff origin/<base_branch>...<head>`).
7. Durable docs if present (this repo: `docs/technical-discovery.md`, `docs/domain-model.md`, `DESIGN.md`; there is no `ARCHITECTURE.md`/`CONSTRAINTS.md`).
8. Files changed by the PR.

If no PR number or feature id is provided, pick the oldest open PR against the base branch. If the PR is a draft or its head is not a `feature/<id>` branch, report it and stop unless the user explicitly asked to review it.

## Workflow

### 1. Establish Review Target

Identify:

- PR number, URL, head branch (`feature/<feature-id>`), base branch, and draft/state,
- feature id and spec path (if the PR references a feature),
- the PR diff and its commits,
- claimed status in `feature_list.json` and the acceptance evidence in `PROGRESS.md`.

Status convention:

- A published PR normally corresponds to a feature already `accepted` by `feature-validator`; this PR review is a second, PR-level review, not a replacement for the feature acceptance.
- `accept` here means the PR is safe and correct to merge; it does not by itself change `feature_list.json`, which the orchestrator owns.

If there is no spec for a non-trivial PR, note the missing contract: review against `AGENTS.md` and repo patterns and record the gap as a finding.

### 2. Check Conflicts And Mergeability

Read the PR mergeable state and report it explicitly:

- `mergeable` / `mergeStateStatus` from `gh pr view` (`MERGEABLE`, `CONFLICTING`, `BEHIND`, `BLOCKED`, `UNSTABLE`, `DIRTY`, `UNKNOWN`, ...).
- If `UNKNOWN`, gh may still be computing it: re-query once after a short wait, then report the result.
- If `CONFLICTING`/`DIRTY`, identify the conflicting files: `git fetch origin <base_branch>`, then in the PR worktree `git merge-tree`/`git merge --no-commit --no-ff origin/<base_branch>` **without committing** is not allowed for you (no writes); instead list likely conflicts from `gh pr diff` and the branch state, and report `CONFLICTING` with the files you can determine.
- If `BEHIND`, the branch is not up to date with the base: report it; the orchestrator decides whether to sync before merge.

Record a `conflict` field: `none` | `behind` | `conflicting`. Any `conflicting` PR cannot be accepted for merge until the orchestrator resolves it; you may still review the code and produce a partial verdict, but the merge-blocking reason is the conflict.

### 3. Review Against The Spec And Repo Rules

Check:

- goal satisfied, non-goals respected,
- acceptance scenarios pass or have convincing evidence,
- scope discipline: only the selected feature's changes, no adjacent features or unrelated refactors,
- PR metadata rules from `../../../AGENTS.md`: title format `[<feature-id>] <title>` and a body line `Closes #<issue-number>` (English keyword). A missing `Closes #` line is a finding (the issue would not close on merge).
- for UI features, the diff follows `../../../DESIGN.md` (the overriding visual source of truth); a contradiction without justification is a `revise`/`block` finding.

### 4. Rerun Or Inspect Verification

Rerun the repo standard gate and focused checks in the PR worktree when practical. If checks are expensive, unavailable, or require external services, inspect recorded evidence and state what was not rerun.

If `init.sh` exists after a runnable baseline, treat it as the standard non-blocking gate: it should execute the relevant checks and fail on errors. It must not start long-running processes such as a dev server.

#### Gradle daemon hygiene

When you need to run Gradle **directly** for verification (e.g. `--rerun-tasks`), use the wrapper `bash scripts/gradle.sh <tasks>`: it sends the build output to a file, so no surviving Gradle/Kotlin daemon inherits the shell's stdout (which can leave the shell tool hanging forever, even after `BUILD SUCCESSFUL`). Do not call `./gradlew --stop`: it is global and kills the builds of other worktrees running in parallel.

```bash
timeout "${SAZON_BUILD_TIMEOUT:-1800}" bash scripts/gradle.sh testDebugUnitTest --rerun-tasks
timeout "${SAZON_BUILD_TIMEOUT:-1800}" bash scripts/gradle.sh lint --rerun-tasks
```

Use this hierarchy:

1. static/syntax checks,
2. tests and runtime/startup checks,
3. persistent E2E checks when the repo has them (this repo does not; UI is verified by JVM logic/state plus `../../../DESIGN.md`),
4. user-flow or manual smoke checks when persistent E2E is not available or cannot cover the case.

If GitHub reports required status checks (`statusCheckRollup`) that failed or are pending, report them; they block merge.

### 5. Review Quality And Risk

Use `../feature-validator/references/validation-rubric.md` to evaluate correctness, verification evidence, scope discipline, architecture compliance, security/privacy/access-control risk, maintainability, durable documentation, and handoff readiness.

Also apply `../feature-validator/references/security-checklist.md` for a portable, PR-scoped security review (diff-focused, not a full repository audit).

### 6. Produce Verdict

Return one of:

- `accept`: PR satisfies the spec and repo rules, evidence is adequate, and the PR is `MERGEABLE` (or only `BEHIND` with the orchestrator able to fast-forward/merge cleanly). No critical/high findings.
- `revise`: PR is close but needs specific fixes, or it is `CONFLICTING` and the conflict is mechanical (state/docs) enough to describe a repair brief. List required changes.
- `block`: cannot review, missing spec for a non-trivial change, unsafe state, or the PR is fundamentally wrong. List the blocker and the required next action.

For each finding that leads to `revise` or `block`, include:

- severity (Critical / High / Medium / Low),
- evidence (file, line, command output),
- why it matters,
- required change,
- suggested implementation steps,
- verification after fix.

If the verdict is `revise`, also include an `Implementation Repair Brief` that can be handed directly to the implementer (or to the orchestrator, for a mechanical conflict), ordered, scoped, and executable without this chat.

Do not edit files, do not commit, do not push, do not merge, and do not change `feature_list.json`. Return the verdict, the conflict status, and the recommended next action; the `pr-flow` orchestrator persists state and merges.

## Output Summary

Report:

- verdict (`accept` / `revise` / `block`) and conflict status (`none` / `behind` / `conflicting`),
- PR number, URL, head branch, feature id, and spec path,
- checks rerun and results (and any required status checks reported by GitHub),
- findings by severity,
- implementation repair brief when the verdict is `revise` or `block`, including whether conflict resolution needs the implementer,
- security assessment summary,
- documentation/harness state assessment,
- required follow-up.
