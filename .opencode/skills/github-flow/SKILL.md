---
name: github-flow
description: "Orchestrate multi-feature GitHub delivery on top of feature-flow using git worktrees and gh issues/PRs. Use when the user asks to plan, execute, publish, or prune feature ids in a GitHub repository, to run features in parallel isolated worktrees with independent opencode sessions, to reuse or create issues, branches, worktrees, or PRs for features, to publish accepted features as PRs, or to remove worktrees and branches of features whose PR is already merged. Modes: plan (planificar, read-only), execute (ejecutar), publish (publicar), prune (limpiar). Never merges."
---

# GitHub Flow

Coordinate one or more `feature_list.json` features across a GitHub repository. This skill owns repository topology: issues, branches, worktrees, sessions, pushes, and PRs. It delegates each feature's planner/implementer/validator loop to `../feature-flow/SKILL.md` inside an isolated worktree session and never implements product code itself.

## Inputs

The first four inputs are required. If any is missing, ask before doing anything.

1. `feature_ids`: one or more feature ids from `feature_list.json`.
2. `repository`: GitHub slug (`owner/repo`) or a local checkout path. Resolve the slug from `git remote get-url origin` and confirm it.
3. `base_branch`: branch that all work is based on and that PRs target.
4. `mode`: `plan` | `execute` | `publish` | `prune` (aliases: planificar | ejecutar | publicar | limpiar).

## Hard Rules

- The base checkout must be **current** before any check that reads shared state (`feature_list.json`, `PROGRESS.md`, statuses, dependencies). `execute` and `publish` fast-forward the local `base_branch` to `origin/<base_branch>` as one of their first steps, before the precondition/acceptance gate; a stale checkout misjudges dependencies and statuses (a feature whose dependency already merged would look blocked). `plan` never fetches and only reports whether the local checkout is behind.
- Plan mode is read-only. It must not mutate git, GitHub, or files: no fetch, push, commit, checkout, stash, or worktree add; no issue/PR create, comment, or edit; no file writes.
- Without acceptance there is no publish. `publish` requires each feature's status to be `accepted` in `feature_list.json` (independent validator `accept`). `passing` is not accepted.
- Push and PR are automatic after acceptance. The orchestrator has standing push permission: once a feature is `accepted`, it pushes `feature/<feature-id>` and opens (or reuses) the PR without asking for a separate per-run grant. There is no push gate beyond acceptance.
- Merge is always out of scope **for this skill**. Never merge a PR and never enable auto-merge. Merging a published PR is owned exclusively by the `pr-flow` orchestrator, behind its review + conflict + checks gate; `github-flow` publishes only.
- If a request repeats, reuse the existing issue, worktree, and PR. Never duplicate them. Never delete or rewrite existing work outside `prune`: no worktree removal, branch deletion, force push, or issue/PR closing. The single exception is `prune`, which removes the worktree and local branch only of a feature whose PR is already merged into `base_branch`, behind the guards in `## Mode: prune`. `plan` never removes anything.
- One feature per session. Never two features in the same session. One independent session per worktree, launched with `opencode run` **from the worktree directory** (opencode v2 has no `--dir`; the session's working directory is the process cwd).
- All worktrees branch from one common base SHA fixed at the start of `execute`.
- The orchestrator is the single integrator of shared state and docs (`feature_list.json`, `PROGRESS.md`, `docs/technical-discovery.md`, ...). Before publishing, it brings the latest `origin/<base_branch>` into the feature branch with a **merge** (never rebase, never force-push), resolving conflicts by keeping the feature's additions alongside the base's. After any other feature's PR merges, it re-syncs the remaining open feature branches the same way.
- A blocked feature is reported with its blocking dependency ids or missing required inputs (e.g. missing credentials). Never silently skip it and never substitute a later feature.
- `feature-flow` owns the role loop and acceptance inside a worktree session; its commit and validation rules still apply there.
- Publishing stops at the open PR. Reviewing, repairing, resolving conflicts, and merging that PR are owned by `pr-flow` (the only merge authority); `github-flow` never performs those steps.

## Preconditions

Checked by `execute` before it launches any session, and always **after** `execute` has fast-forwarded the base checkout to `origin/<base_branch>` (see `## Mode: execute`, step 0). `plan` runs the same checks read-only against the local refs and must flag if the base checkout is behind. Every feature to execute must pass all four.

1. Dependencies accepted: every id in `depends_on` has status `accepted` in `feature_list.json`.
2. Clean base: the base checkout has no staged, modified, or deleted tracked files (`git status --porcelain` has no tracked entries; untracked files are allowed and reported), no merge/rebase in progress, and `base_branch` resolves.
3. Permissions: `gh auth status` shows an account that can manage issues and PRs in the repository. Push/PR is automatic after acceptance (standing permission); there is no per-run push grant.
4. Independent environments: one worktree per feature outside the base checkout, one session per worktree, no shared mutable state between features (each worktree runs its own `./init.sh` and local database).

If a precondition fails, report the failing features with the concrete missing inputs. A missing dependency or a missing credential stops before mutating anything. A precondition that depends on a person (missing credentials, or a decision only the person can make) is a `blocked` feature, not a failure: see `## Blocked → Resume`.

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

1. Resolve inputs; read `feature_list.json`, `PROGRESS.md`, git state (`git status --porcelain`, `git rev-parse <base_branch>`, `git rev-parse origin/<base_branch>`, `git worktree list`, `git branch --list`) and gh state (`gh auth status`, issue and PR lookups). Never `git fetch`. If `<base_branch>` is behind the local `origin/<base_branch>` ref, say so and warn that the plan may be based on a stale checkout (a fetch during `execute` may reveal more); the plan does not sync anything itself.
2. Evaluate the four preconditions per feature. For blocked features, report the concrete missing inputs: `accepted` dependency ids and/or missing credentials.
3. Planned base SHA = current `git rev-parse origin/<base_branch>` when it resolves, otherwise `git rev-parse <base_branch>`; flag it if the local `<base_branch>` is behind it (the planned worktree should branch from the remote SHA, which `execute` fast-forwards to first).
4. For each ready feature, resolve reuse targets (issue, worktree, branch, PR) and the planned session command `opencode run ...` launched with the worktree as the working directory (opencode v2 has no `--dir`).
5. Emit the output report with status `planned` and `pr: none`. Nothing is created.

## Mode: execute (ejecutar)

Do these two things first, in order, before evaluating any precondition:

0. **Sync the base checkout** so every later check reads fresh state. Fetch and fast-forward the local base branch to the remote:

   ```bash
   git fetch origin <base_branch>
   git merge --ff-only origin/<base_branch>
   ```

   Run it in the base checkout. The precondition gate (step 2) and every status/dependency read take `feature_list.json`/`PROGRESS.md` from this checkout, so a stale local `base_branch` misjudges them (a dependency whose PR already merged would still look `not_started`, and the feature would be wrongly reported `blocked`). This is a **fast-forward only**: never rebase, never force-push, never stash or discard local work. If it is not a fast-forward (diverged local branch or local commits), stop and report instead of clobbering.

   Then run the `prune` cleanup (`## Mode: prune`) for any requested feature whose PR is already merged into `base_branch`; never prune a feature that is not merged. This is what keeps orphan worktrees from accumulating between runs.

1. Fix the common base SHA: `git fetch origin <base_branch>` then `base_sha=$(git rev-parse origin/<base_branch>)`. Every worktree branches from this exact SHA.
2. Run the precondition gate. Split the request into **ready** and **blocked** (missing dependency or credential) features and report both lists; continue with the ready features.
3. Push/PR is standing and automatic after acceptance. Do not ask for a per-run push grant.
4. For each ready feature, independently:
   1. Reuse or create the issue (lookup first).
   2. Reuse or create the worktree and branch from `base_sha`: `git worktree add <worktrees-root>/<feature-id> -b feature/<feature-id> <base_sha>`.
   3. Launch exactly one session with the documented command, **with the worktree as the working directory** (opencode v2 has no `--dir`; the session's directory is the process cwd). Prefer the repo helper, which centralizes the launch policy (cwd, model, DNS, title):
      ```bash
      bash scripts/launch-feature-session.sh <feature-id>
      ```
      If the helper is absent, launch `opencode run` from inside the worktree (`cd "<worktrees-root>/<feature-id>" && opencode run "..."`); add `-m <provider/model>` only if the launcher must override the configured default model. Do NOT improvise a launch: keep the launch policy (cwd, model, DNS, title, detach, exit sentinel) in one place — the repo helper.

      **Never block a tool call for the whole cycle.** The flow lasts 30-60+ minutes; a single foreground tool call that long is fragile: any cancellation aborts the call and the orchestrator loses the return even though the work keeps running server-side. Launch the session **detached** and take the **artifacts** as the completion signal, not the tool call's return:
      ```bash
      SAZON_DETACH=1 bash scripts/launch-feature-session.sh <feature-id>   # decoupled (setsid); returns immediately
      bash scripts/wait-feature-session.sh <feature-id>                    # bounded wait on the exit sentinel
      ```
      `SAZON_DETACH=1` runs the cycle decoupled from the calling process, writing its output to `<worktree>/.feature-session.log` and its exit code to `<worktree>/.feature-session.exit` when it ends; the waiting process is not required for completion, so an interrupted `wait` loses nothing. If the completion signal is missed, **reconcile from the artifacts**: the feature status in `feature_list.json`, the local acceptance commit on `feature/<feature-id>`, and (when present) `docs/validations/<feature-id>.md`. When the session has finished, publish if the feature is `accepted`; if it returned `accept` but left the work uncommitted (for example it crashed before committing), complete the single acceptance commit first. Never two features in one session. Different features use different worktrees and different sessions; they may run concurrently because the environments are independent. Only the top-level `github-flow` orchestrator publishes; the delegated `feature-flow` session never pushes or opens PRs.
   4. Collect the `feature-flow` outcome (validator `accept` / `revise` / `block`). On `accept`, confirm the feature branch has the acceptance commit; if the session returned `accept` but left the work uncommitted (for example it crashed before committing), complete the single acceptance commit on `feature/<feature-id>` before publishing.
   5. On `accept`, publish automatically: push `feature/<feature-id>` and reuse or create the PR into `base_branch` with acceptance evidence in the body and a `Closes #<issue-number>` line. Never merge.
   6. Keep the issue body updated with status and evidence.
5. Emit the output report.

## Blocked → Resume

A feature is `blocked` when it fails a precondition for a reason outside the agents' control (missing credentials, or a decision only the person can make). Blocking is not failure: it is the correct stop, and it is resumable.

- Report the block with the concrete missing inputs (paths) and the exact resume command: `github-flow execute <feature-id>`.
- When the missing input exists, resume by re-running `github-flow execute <feature-id>`. The run is idempotent: it reuses the existing issue, worktree, and branch (never duplicates) and relaunches exactly one `feature-flow` session, which now passes the gate and continues from the persisted spec/implementation. Do not ask for a new push grant; standing permission still applies.
- Never resume a `github-flow`-owned feature with a standalone `feature-flow` run. Issue, worktree, branch, and PR ownership live in `github-flow`; a bare `feature-flow` run leaves the acceptance commit local and skips publishing. When a `feature-flow` session discovers the block, it hands the feature back to `github-flow`.

## Mode: publish (publicar)

0. Prune first: run the `prune` cleanup (`## Mode: prune`) for any requested feature whose PR is already merged into `base_branch` before publishing; never prune a feature that is not merged. Then sync the base checkout so the acceptance gate reads fresh state: `git fetch origin <base_branch>` then `git merge --ff-only origin/<base_branch>` in the base checkout (fast-forward only; if it is not a fast-forward, stop and report).
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
- `session`: the `opencode run ...` command launched from the worktree directory (opencode v2 has no `--dir`), or `none` in plan mode.
- `branch`: `feature/<id> @ <base_sha>`.
- `status`: gate verdict or feature status: `blocked`, `planned`, `in_progress`, `passing`, `accepted`, `published`, `refused`, `pruned`, `skipped`, `already clean`.
- `commit`: hash and subject, or `none`.
- `evidence`: verification or acceptance summary.
- `pr`: PR URL when a PR exists or was published, otherwise `none`.
