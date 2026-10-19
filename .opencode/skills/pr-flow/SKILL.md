---
name: pr-flow
description: Orchestrate the review, repair, conflict-resolution, and merge of open pull requests against the base branch. Use when the user asks to review PRs, have PRs reviewed and fixed, resolve PR conflicts, or merge approved PRs automatically. It launches the reviewer subagent, routes fix briefs to the implementer subagent, resolves conflicts as the single integrator, and merges each PR with a merge commit once the reviewer accepts and there are no conflicts. This is the only skill allowed to merge.
---

# PR Flow

Coordinate the lifecycle of **open pull requests** against a base branch: review, repair, conflict resolution, and merge. This skill owns the post-publish stage on top of `../github-flow/SKILL.md`. It delegates the review to the `reviewer` subagent and repairs to the `implementer` subagent, and it is the **single integrator**: only this skill commits, pushes, resolves conflicts, and merges.

The main agent is the orchestrator. It selects a PR, launches the right subagent, reviews the result, keeps the same PR moving, and merges only when the gate is satisfied.

## Hard Rules

- **This skill is the only merge authority in the repository.** `AGENTS.md` scopes the "never merge" rule to every other flow: the `pr-flow` orchestrator merges, and nothing else does. Never enable auto-merge; merge explicitly, once per PR, at the end of the loop.
- **Merge method is a merge commit**: `gh pr merge <n> --merge`. Never `--squash`, never `--rebase`, never `--auto`. This preserves the acceptance commit and matches the repo's no-rebase, no-force-push culture.
- **Never force-push and never rebase.** Integrate the base with a merge, exactly like `github-flow`.
- **Work on one PR at a time.** Do not review or merge two PRs in the same step.
- **The reviewer never edits and never merges.** It reports `accept` / `revise` / `block` plus the conflict status. You (the orchestrator) do the writes.
- **The implementer never commits or pushes.** It leaves changes in the PR worktree; you commit and push them to the PR head branch.
- **Conflicts are resolved by the orchestrator** (single integrator). Mechanical conflicts (shared state/docs: `feature_list.json`, `PROGRESS.md`, `docs/technical-discovery.md`, ...) are resolved by keeping **both** contributions. Conflicts that need code judgement are routed to `implementer` with a repair brief.
- **Merge is gated**: reviewer `accept` + conflict status `none` + no failing/pending required status checks. Without the gate there is no merge.
- **Do not ask for a per-run push/merge grant.** Standing permission applies to this flow, bounded by the merge gate.
- **Idempotent and reuse-only**: reuse the existing PR branch, worktree, and PR. Never delete, close, rewrite, or duplicate them (except the `prune` cleanup of a merged PR, delegated to `github-flow`).
- Never merge a draft PR, a PR whose head is not a `feature/<id>` branch of this repository, or a PR that is not against the base branch.

## Inputs

1. `repository`: GitHub slug (`owner/repo`) or local checkout path. Resolve from `git remote get-url origin` and confirm.
2. `base_branch`: branch PRs target (here `main`).
3. `scope` (optional): a PR number, a feature id, or `all` open PRs. Default: `all`.
4. `mode` (optional): `review` (review + repair loop only, never merge) | `merge` (full loop through merge). Default: `merge`.

## Preconditions

Checked before touching any PR.

1. `gh auth status` shows an account that can push branches and merge PRs in the repository.
2. The base checkout is current: `git fetch origin <base_branch>` then `git merge --ff-only origin/<base_branch>` (fast-forward only; if it is not a fast-forward, stop and report instead of clobbering).
3. `bash init.sh` passes in the base checkout (the standard gate). If it fails, fix or report before starting a review loop.

If a PR needs a worktree that does not exist, ensure it with `scripts/pr-worktree.sh <pr-number> <base_branch>` (see `## Working Copies`). If the required input is missing and only a person can supply it, report the PR as `blocked` with the concrete missing input; do not merge anything else on its behalf.

## PR Selection

1. List open PRs: `gh pr list --state open --base <base_branch> --json number,title,headRefName,isDraft,mergeable,mergeStateStatus,url`.
2. If `scope` is a PR number or a feature id, keep only that PR. If `scope` is `all` (default), keep all open PRs against the base branch; process them one at a time in ascending PR-number order.
3. Skip and report: draft PRs (`isDraft`), PRs whose head is not `feature/<id>` for a feature present in `feature_list.json`, and PRs not targeting `<base_branch>`. Never silently drop a PR: report the skip reason.

## Working Copies

Conflict resolution, repair, and the local gate all need the PR's head branch checked out. Reuse the feature worktree when it exists at `<worktrees-root>/<feature-id>` (the same one `github-flow` created). If it does not exist, create it from the PR head with the helper:

```bash
bash scripts/pr-worktree.sh <pr-number> <base_branch>   # prints the worktree path
```

Rules:

- Never switch the base checkout's branch and never `git checkout` the PR branch in the base checkout; work only inside the PR worktree.
- If the PR head lives in another repository (`isCrossRepository=true`), stop that PR and report it as unsupported by this flow. The owner of the head repository is irrelevant: in an organization repository it never matches the authenticated user.
- Keep WIP=1 per PR; do not run two features in one worktree.

## Orchestration Loop (per PR)

Run these steps for one PR, in order, before moving to the next PR.

1. **Establish target.** Resolve PR number, head branch `feature/<id>`, feature id, spec path (`../../../docs/specs/<id>.md`), and the PR worktree path. Read `feature_list.json` and `PROGRESS.md` for the feature's claimed status and acceptance evidence.

2. **Ensure a mergeable base.** In the PR worktree, integrate the base first so the reviewer sees the real merge result:
   - If `mergeStateStatus` is `BEHIND`, or before merging, run `scripts/sync-worktree.sh <feature-id> <base_branch>`.
   - If it reports conflicts (exit 1), resolve them **in the worktree**: keep both contributions for shared state/docs (`feature_list.json`, `PROGRESS.md`, `docs/...`), and for code conflicts route a conflict repair brief to `implementer` (Step 5) when judgement is needed. Then `git add` the resolved files and `git commit --no-edit` to finish the merge.
   - Push the synced head: `git push origin feature/<id>`.
   - Re-check `gh pr view <n> --json mergeable,mergeStateStatus` until it is no longer `CONFLICTING`/`DIRTY` (or report `blocked` if it cannot be made mergeable).

3. **Review.** Launch the `reviewer` subagent for this PR. Send only the dynamic handoff context: repository path, PR number, head branch, feature id, spec path, worktree path, and expected output (verdict + conflict status + repair brief).

4. **Apply the verdict.**
   - `accept` with conflict status `none` → go to **Step 6 (Merge Gate)**. If mode is `review`, stop here for this PR and report `reviewed`.
   - `accept` but still `conflicting` → fix the conflict (Step 2) and re-run the reviewer.
   - `revise` → route the repair brief to `implementer` (Step 5), then re-run the reviewer.
   - `block` → stop this PR, leave it untouched, report the blocker, move to the next PR. Never merge a `block`.

5. **Repair via implementer.** Launch the `implementer` subagent in the PR worktree with the reviewer's repair brief (feature id, spec path, the ordered findings, and the expected result). The implementer edits files and self-verifies but does **not** commit or push. When it returns:
   - Inspect the diff (`git -C <worktree> status --short`, `git -C <worktree> diff`).
   - Stage and commit the repair on `feature/<id>` with a Conventional Commit naming the feature (e.g. `fix: address PR review for <feature-id>`); the orchestrator, not the subagent, commits.
   - Push: `git push origin feature/<id>`.
   - Re-run the reviewer (back to Step 3). Bound the loop: at most **two** repair rounds per PR; after that, report the PR `blocked` with the outstanding findings.

6. **Merge Gate.** Merge only if ALL hold; otherwise report `refused` with the failing condition:
   - reviewer verdict was `accept` for the current head,
   - `gh pr view <n> --json mergeable,mergeStateStatus` reports `MERGEABLE` (not `CONFLICTING`/`DIRTY`/`BLOCKED`),
   - required status checks (`statusCheckRollup`) show no failing or pending required checks,
   - the head is fully pushed (`git -C <worktree> status --porcelain` clean and `git rev-parse HEAD` equals the remote head),
   - the PR body still contains a `Closes #<issue-number>` line (add it if missing, before merging).

   Then merge with a merge commit:

   ```bash
   gh pr merge <n> --merge        # never --squash, never --rebase, never --auto
   ```

   If `gh pr merge` fails (branch protection requiring a human review, missing merge permission, or a
   race), do **not** retry with a different method and do **not** force anything: report the PR
   `refused`/`blocked` with the exact `gh` error and move to the next PR.

7. **Post-merge.** After the merge:
   - Confirm the merge landed on the base (`git fetch origin <base_branch>`; `git log origin/<base_branch>` contains the merge).
   - Report that the feature's issue closed automatically via `Closes #<n>`.
   - Delegate cleanup of the merged worktree/branch to `github-flow` `prune` (`scripts/cleanup-worktree.sh <feature-id> <base_branch>`), which re-checks the merged/contained guards. Never force-remove.

8. **Next PR.** Move to the next open PR in the list. Re-sync the base checkout (`git fetch origin <base_branch>`; `git merge --ff-only origin/<base_branch>`) after each merge so later PRs are reviewed against the fresh base.

## Failure Recovery

A subagent step can end without a result because of a transient infrastructure failure (stalled provider stream, aborted request, opencode crash, killed process). Treat these as recoverable.

- If a subagent aborts or returns without the expected verdict/artifact, re-launch the SAME role for the SAME PR. Do not skip the role and do not merge on a missing verdict.
- The roles are idempotent: the reviewer re-reads the PR and re-runs its checks; the implementer re-reads the worktree before changing anything. Re-launching is safe.
- Bound the retries: at most two re-launches per role per PR. After that, report the PR `blocked`.
- Before re-launching, confirm the expected verdict/artifact is still absent, so a step that already finished is not repeated.

## Output Report

Header: mode (`review`/`merge`), repository, base branch, base SHA, merge authority (`pr-flow`, standing), scope.

Per PR, one block with exactly these fields:

- `pr`: number and URL.
- `feature`: feature id and head branch `feature/<id>`.
- `conflict`: `none` | `behind` | `conflicting` (and resolved files when applicable).
- `verdict`: reviewer verdict for the current head (`accept` / `revise` / `block`), or `none` if not reviewed.
- `repair rounds`: number of implementer repair rounds used.
- `commits`: repair/merge-commit hashes and subjects on the branch, or `none`.
- `checks`: gate and required-status-check summary.
- `status`: `merged` | `reviewed` | `refused` | `blocked` | `skipped` | `already merged`.
- `merge`: merge commit hash, or `none`.
- `next`: next action (e.g. next PR number, or the concrete blocker).

## Relationship To The Other Flows

- `feature-flow` plans, implements, and validates one feature; it ends with a local acceptance commit and never pushes.
- `github-flow` publishes that commit as a PR (push + `Closes #<n>`), and never merges.
- `pr-flow` (this skill) reviews the published PR, repairs it if needed, resolves conflicts, and merges it. It is the only flow that merges. A feature already `accepted` by `feature-validator` still has to pass this PR review before its PR is merged.
