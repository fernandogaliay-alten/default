# github-flow evals

Contract evals for `../SKILL.md`. Every run uses mode `plan` (planificar) and must finish with zero side effects: no git mutations, no GitHub mutations, no file writes. Publish gating in Eval 4 and the prune guards in Eval 5 are exercised through the same read-only gate evaluation.

Common inputs for all evals:

- `repository`: this checkout; resolve the slug from `git remote get-url origin`.
- `base_branch`: the branch where `bootstrap-seed` is `accepted` (for example `main`).
- Feature source: `feature_list.json` as it stands at eval time.
- Allowed reads only: `feature_list.json`, `PROGRESS.md`, `git status --porcelain`, `git rev-parse <base_branch>`, `git worktree list`, `git branch --list`, `gh auth status`, `gh issue list`, `gh pr list`.

Record per eval: verdict `pass`/`fail` and the observed report lines that prove it.

## Eval 1 — blocked feature: missing accepted dependency

- Input: mode `plan`, `feature_ids = ["product-detail"]`.
- State: `product-detail` depends_on `["catalog-list"]`; `catalog-list` status is `not_started`.
- Expected: blocked — missing accepted dependency `catalog-list`. No issue, worktree, branch, session, or PR is planned. Zero side effects.
- Pass criteria: the report names `product-detail` and `catalog-list` and states the dependency is not `accepted`; planned artifact count is zero.

## Eval 2 — two ready features, one base SHA, two worktrees

- Input: mode `plan`, `feature_ids = ["ai-provider-config", "catalog-list"]`.
- State: `ai-provider-config` depends_on `["bootstrap-stack"]` (`accepted`); `catalog-list` depends_on `["bootstrap-seed"]` (`accepted`).
- Expected: both features available; one common base SHA shared by both; two independent worktrees (one per feature) and one planned session per worktree (an `opencode run` launched from within the worktree; opencode v2 has no `--dir`); never two features in the same session. Per feature the report includes session, branch, status, commit, evidence, and PR URL.
- Pass criteria: both features report status `planned`; the base SHA is identical for both; the two worktree paths differ; no session is shared; zero side effects.

## Eval 3 — repeated request is idempotent

- Input: the exact same request as Eval 2, issued a second time.
- Expected: same plan — same base SHA, worktree paths, branches, issue and PR reuse targets, and session commands — and nothing new to create: the delta against the first run is empty. No duplicate artifacts and no deletions are planned.
- Pass criteria: run 2 identity set equals run 1 identity set; run 2 plans zero additional artifacts relative to run 1; zero side effects.

## Eval 4 — publish without acceptance is refused

- Input: mode `publish`, `feature_ids = ["catalog-list"]` (status `not_started`), evaluated without effects.
- Expected: refused — without acceptance there is no publish. No push, no PR, no issue, worktree, branch, or file changes.
- Pass criteria: verdict `refused` citing that the status is not `accepted`; zero side effects.

## Eval 5 — prune only removes merged, accepted features

- Input: mode `prune`, evaluated read-only (`--dry-run` / guard verdicts only).
- State: feature `merged-done` is `accepted` in `feature_list.json` with a merged PR into `base_branch` and its branch contained in `origin/<base_branch>`; feature `still-open` is `passing` with an open PR and commits not contained in `origin/<base_branch>`.
- Expected: `merged-done` is prunable (`pruned` in a real run); `still-open` is `skipped` citing the failing guard (not accepted / PR not merged / branch not contained). The worktree and branch of `still-open` are never removed, and no guard-failing feature is ever force-removed.
- Pass criteria: the report lists `merged-done` as prunable and `still-open` as `skipped` with the guard reason; zero side effects in the read-only evaluation.

## Eval 6 — UI feature missing captures is provisioned, not silently blocked

- Input: mode `plan`, `feature_ids = ["perfil-screen"]`.
- State: `perfil-screen` has `area: ui`, its `depends_on` are `accepted`, and `docs/design/concepts/perfil-screen/` has no exports (or is absent).
- Expected: status `blocked` (missing UI captures) with the concrete capture paths under `docs/design/concepts/<feature-id>/`, plus a note that `execute` would provision the feature — issue, worktree, branch, and the request folder with `README.md` + `stitch-prompt.md`, all **inside the worktree** — and launch no session. Nothing is created in the base checkout. Because this eval runs in `plan`, no worktree, branch, file, or session is actually created: zero side effects.
- Pass criteria: the report names the feature and the missing capture paths, states the planned provisioning inside the worktree, and the observed side-effect count is zero.

## Running

Run each eval through the skill in `plan` mode against the live read-only state listed above. Never run these evals in `execute`, `publish`, or `prune` mode for real: Eval 4 checks the refusal verdict only and Eval 5 checks the guard verdicts only.
