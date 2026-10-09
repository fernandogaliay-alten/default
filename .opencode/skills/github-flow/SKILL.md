---
name: github-flow
description: "Orchestrate multi-feature GitHub delivery on top of feature-flow using git worktrees and gh issues/PRs. Use when the user asks to plan, execute, publish, or prune feature ids in a GitHub repository, to run features in parallel isolated worktrees with independent opencode sessions, to reuse or create issues, branches, worktrees, or PRs for features, to publish accepted features as PRs, or to remove worktrees and branches of features whose PR is already merged. Modes: plan (planificar, read-only), execute (ejecutar), publish (publicar), prune (limpiar). Never merges."
---

# GitHub Flow

Coordinate one or more `feature_list.json` features across a GitHub repository. This skill owns repository topology: issues, branches, worktrees, sessions, pushes, and PRs. It delegates each feature's planner/implementer/validator loop to `../feature-flow/SKILL.md` inside an isolated worktree session and never implements product code itself.

## Inputs

All four inputs are required. If any is missing, ask before doing anything.

1. `feature_ids`: one or more feature ids from `feature_list.json`.
2. `repository`: GitHub slug (`owner/repo`) or a local checkout path. Resolve the slug from `git remote get-url origin` and confirm it.
3. `base_branch`: branch that all work is based on and that PRs target.
4. `mode`: `plan` | `execute` | `publish` | `prune` (aliases: planificar | ejecutar | publicar | limpiar).

## Hard Rules

- Plan mode is read-only. It must not mutate git, GitHub, or files: no fetch, push, commit, checkout, stash, or worktree add; no issue/PR create, comment, or edit; no file writes.
- Without acceptance there is no publish. `publish` requires each feature's status to be `accepted` in `feature_list.json` (independent validator `accept`). `passing` is not accepted.
- Push and PR are automatic after acceptance. The orchestrator has standing push permission: once a feature is `accepted`, it pushes `feature/<feature-id>` and opens (or reuses) the PR without asking for a separate per-run grant. There is no push gate beyond acceptance.
- Merge is always out of scope. Never merge a PR and never enable auto-merge.
- If a request repeats, reuse the existing issue, worktree, and PR. Never duplicate them. Never delete or rewrite existing work outside `prune`: no worktree removal, branch deletion, force push, or issue/PR closing. The single exception is `prune`, which removes the worktree and local branch only of a feature whose PR is already merged into `base_branch`, behind the guards in `## Mode: prune`. `plan` never removes anything.
- One feature per session. Never two features in the same session. One independent session per worktree via `opencode run --dir <worktree>`.
- All worktrees branch from one common base SHA fixed at the start of `execute`.
- The orchestrator is the single integrator of shared state and docs (`feature_list.json`, `PROGRESS.md`, `docs/technical-discovery.md`, ...). Before publishing, it brings the latest `origin/<base_branch>` into the feature branch with a **merge** (never rebase, never force-push), resolving conflicts by keeping the feature's additions alongside the base's. After any other feature's PR merges, it re-syncs the remaining open feature branches the same way.
- A blocked feature is reported with its blocking dependency ids or missing required inputs (e.g. UI captures under `docs/design/concepts/<feature-id>/`). Never silently skip it and never substitute a later feature.
- `feature-flow` owns the role loop and acceptance inside a worktree session; its commit and validation rules still apply there.

## Preconditions

Checked before `execute` is allowed to mutate anything. `plan` runs the same checks read-only. Every feature to execute must pass all five:

1. Dependencies accepted: every id in `depends_on` has status `accepted` in `feature_list.json`.
2. Clean base: the base checkout has no staged, modified, or deleted tracked files (`git status --porcelain` has no tracked entries; untracked files are allowed and reported), no merge/rebase in progress, and `base_branch` resolves.
3. Permissions: `gh auth status` shows an account that can manage issues and PRs in the repository. Push/PR is automatic after acceptance (standing permission); there is no per-run push grant.
4. Independent environments: one worktree per feature outside the base checkout, one session per worktree, no shared mutable state between features (each worktree runs its own `./init.sh` and local database).
5. Required external inputs present: a feature with UI (`area: ui` or any screen/visual behavior) must have its mandatory captures under `docs/design/concepts/<feature-id>/`, named by the spec, already present. If any is missing, the feature is `blocked` (reason: missing UI captures) and no worktree, branch, or session is created for it.

If any precondition fails, report the failing features with the concrete missing inputs and stop before mutating anything. A precondition that depends on a person (missing captures, missing credentials) is a `blocked` feature, not a failure: see `## Blocked → Resume`.

## Deterministic Identities (reuse keys)

- Branch: `feature/<feature-id>`.
- Worktree: `<worktrees-root>/<feature-id>` where `<worktrees-root>` is `../<base-checkout-dirname>-worktrees`, outside the base checkout.
- Issue title: `[<feature-id>] <feature title from feature_list.json>`.
- PR: head `feature/<feature-id>` into `base_branch`, title `[<feature-id>] <feature title>`. The body must contain a line `Closes #<issue-number>` (with `#<issue-number>` the feature's issue) so that merging the PR closes the issue automatically; a bare `Issue: #<n>` reference does not close it.

Reuse lookup before any creation (read-only):

- Issue: `gh issue list --state all --search "<feature-id>"`; reuse the issue whose title starts with `[<feature-id>]`; create only if none exists.
- Worktree and branch: `git worktree list` and `git branch --list "feature/<feature-id>"`; reuse when present; `git worktree add` only if absent.
- PR: `gh pr list --state all --head "feature/<feature-id>" --base <base_branch>`; reuse when present; create only in `publish` and only if absent.

## Mode: plan (planificar)

Read-only. Produce the plan report only.

1. Resolve inputs; read `feature_list.json`, `PROGRESS.md`, git state (`git status --porcelain`, `git rev-parse <base_branch>`, `git worktree list`, `git branch --list`) and gh state (`gh auth status`, issue and PR lookups). Never `git fetch`.
2. Evaluate the five preconditions per feature. For blocked features, report the concrete missing inputs: `accepted` dependency ids and/or missing UI capture paths under `docs/design/concepts/<feature-id>/`.
3. Planned base SHA = current `git rev-parse <base_branch>`.
4. For each ready feature, resolve reuse targets (issue, worktree, branch, PR) and the planned session command `opencode run --dir <worktree> ...`.
5. Emit the output report with status `planned` and `pr: none`. Nothing is created.

## Mode: execute (ejecutar)

Before the steps below, run the `prune` cleanup (`## Mode: prune`) for any requested feature whose PR is already merged into `base_branch`; never prune a feature that is not merged. This is what keeps orphan worktrees from accumulating between runs.

1. Run the precondition gate. Split the request into ready and blocked features and report both lists; continue with ready features only. For a feature blocked by missing UI captures, report the missing paths and the resume command `github-flow execute <feature-id>`, and create the request folder plus `README.md` under `docs/design/concepts/<feature-id>/` (idempotent; skip if present) so the person knows where to drop the exports — but create no worktree, branch, or session for it.
2. Fix the common base SHA: `git fetch origin <base_branch>` then `base_sha=$(git rev-parse origin/<base_branch>)`. Every worktree branches from this exact SHA.
3. Push/PR is standing and automatic after acceptance. Do not ask for a per-run push grant.
4. For each ready feature, independently:
   1. Reuse or create the issue (lookup first).
   2. Reuse or create the worktree and branch from `base_sha`: `git worktree add <worktrees-root>/<feature-id> -b feature/<feature-id> <base_sha>`.
   3. Launch exactly one session with the documented command, quoting the worktree path and the prompt because both contain spaces:
      ```bash
      opencode run --dir "<worktrees-root>/<feature-id>" "Use feature-flow for <feature-id> only, until accepted. Do not push and do not open a PR; the orchestrator publishes."
      ```
      Run it as-is, from a shell that handles quoting (on Windows, Git Bash), and run it **in the foreground**. The run outlasts a shell's default timeout, so **disable the command timeout** (e.g. `timeout: 0` / unbounded) instead of backgrounding it: background/detach hides the run from the terminal, which is exactly what this rule forbids. Do NOT invent a custom launcher: no `Start-Process`, no detached/background process, no redirecting output to log files you create, and never redirect into a directory that may not exist. Those improvisations have repeatedly broken the launch (argument splitting on spaces, missing directories, orphan processes). Never two features in one session. Different features use different worktrees and different sessions; they may run concurrently because the environments are independent. Only the top-level `github-flow` orchestrator publishes; the delegated `feature-flow` session never pushes or opens PRs.
   4. Collect the `feature-flow` outcome (validator `accept` / `revise` / `block`). On `accept`, confirm the feature branch has the acceptance commit; if the session returned `accept` but left the work uncommitted (for example it crashed before committing), complete the single acceptance commit on `feature/<feature-id>` before publishing.
   5. On `accept`, publish automatically: push `feature/<feature-id>` and reuse or create the PR into `base_branch` with acceptance evidence in the body and a `Closes #<issue-number>` line. Never merge.
   6. Keep the issue body updated with status and evidence.
5. Emit the output report.

## Blocked → Resume

A feature is `blocked` when it fails a precondition for a reason outside the agents' control (missing UI captures under `docs/design/concepts/<feature-id>/`, missing credentials, or a decision only the person can make). Blocking is not failure: it is the correct stop, and it is resumable.

- Report the block with the concrete missing inputs (paths) and the exact resume command: `github-flow execute <feature-id>`.
- When the missing input exists, resume by re-running `github-flow execute <feature-id>`. The run is idempotent: it reuses the existing issue, worktree, and branch (never duplicates) and relaunches exactly one `feature-flow` session, which now passes the gate and continues from the persisted spec/implementation. Do not ask for a new push grant; standing permission still applies.
- For a UI block, create the request folder plus `README.md` under `docs/design/concepts/<feature-id>/` (idempotent; skip if present) so the person knows where to drop the exports. Create nothing else for a blocked feature: no worktree, branch, or session.
- When resuming a UI block, the person's exports sit untracked in the base checkout and are therefore absent from the freshly created worktree. Before launching the session, bring the `docs/design/concepts/<feature-id>/` folder into the feature worktree (copy or move it) so the captures land in the feature branch and its PR; if the exports were already committed to the base branch, the worktree has them and nothing is needed.
- Never resume a `github-flow`-owned feature with a standalone `feature-flow` run. Issue, worktree, branch, and PR ownership live in `github-flow`; a bare `feature-flow` run leaves the acceptance commit local and skips publishing. When a `feature-flow` session discovers the block, it hands the feature back to `github-flow`.

## Mode: publish (publicar)

0. Prune first: run the `prune` cleanup (`## Mode: prune`) for any requested feature whose PR is already merged into `base_branch` before publishing; never prune a feature that is not merged.
1. Acceptance gate: every feature must be `accepted` in `feature_list.json`. If any is not, refuse that feature: without acceptance there is no publish. `passing` does not qualify.
2. Push is standing (automatic after acceptance); there is no per-run push gate. Refuse only if the feature is not `accepted`.
3. Commit gate: the feature branch must contain the acceptance commit. If `feature_list.json` says `accepted` but the branch has no commit (for example the `feature-flow` session crashed before committing), create the single Conventional Commit naming the feature before pushing; never push an uncommitted branch.
4. Sync with base (single-integrator rule): in the feature worktree, `git fetch origin <base_branch>` then `git merge --no-edit origin/<base_branch>`. Resolve any conflict in shared state/docs by keeping the feature's additions alongside the base's, then commit. Never rebase and never force-push. Prefer `scripts/sync-worktree.sh <feature-id>` when present.
5. Push `feature/<feature-id>` and reuse or create the PR into `base_branch` with acceptance evidence in the body and a `Closes #<issue-number>` line.
6. Never merge. Report the PR URL per published feature.

## Mode: prune (limpiar)

Remove the worktree and local branch of features whose work is finished. It never pushes, never opens or merges PRs, and never touches features that are not merged.

A feature is prunable only when ALL of these hold; otherwise skip it and report the reason:

1. Its status is `accepted` in `feature_list.json` (completed, not just `passing`).
2. Its PR is merged into `base_branch`: `gh pr list --state merged --head "feature/<feature-id>" --base <base_branch>` returns at least one merged PR.
3. Its branch is fully contained in `origin/<base_branch>`: `git merge-base --is-ancestor feature/<feature-id> origin/<base_branch>`.
4. No `feature-flow`/`opencode` session is still running in that worktree.

Deletion steps, per feature:

1. Fetch the base: `git fetch origin <base_branch>`.
2. Run `scripts/cleanup-worktree.sh <feature-id> <base_branch>` when present; it re-checks the git-level guards (worktree on the expected branch, branch merged, clean worktree) and removes the worktree plus the branch. Never pass `--force`, never delete with an unmerged branch.
3. If the script is absent, perform the same guarded steps manually: `git worktree remove <worktree>` then delete `feature/<feature-id>` only after the containment check passes.

Rules:

- Idempotent: a feature with neither worktree nor branch reports `already clean` and nothing changes.
- If any guard fails, leave the artifacts untouched and report `skipped` with the failing guard. A `skipped` feature is never force-removed.
- `plan` only reports prunable/skipped features read-only; it removes nothing.

## Output Report

Header: mode, repository, base branch, base SHA, push permission (standing/automatic), reused vs created artifacts.

Per feature, one row/block with exactly these fields:

- `feature`: feature id.
- `session`: the `opencode run --dir ...` command, or `none` in plan mode.
- `branch`: `feature/<id> @ <base_sha>`.
- `status`: gate verdict or feature status: `blocked`, `planned`, `in_progress`, `passing`, `accepted`, `published`, `refused`, `pruned`, `skipped`, `already clean`.
- `commit`: hash and subject, or `none`.
- `evidence`: verification or acceptance summary.
- `pr`: PR URL when a PR exists or was published, otherwise `none`.
