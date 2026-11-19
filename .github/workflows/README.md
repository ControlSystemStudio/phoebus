# GitHub Actions workflows

Workflows are split into **entry points** (triggered by GitHub events) and
**reusable building blocks** (file names starting with `_`, triggered only via
`workflow_call`). Entry points contain no build logic of their own; they just
compose the building blocks.

## Entry points

| Workflow | Trigger | Jobs |
|----------|---------|------|
| [`pr.yml`](pr.yml) (*PR*) | `pull_request`, manual `workflow_dispatch` | lint, build; then unit, UI and integration tests (after build succeeds) |
| [`branch.yml`](branch.yml) (*Branch*) | push to any branch except `master` | lint, build |
| [`merge.yml`](merge.yml) (*Merge to master*) | push to `master` | lint; build on Linux (x86_64 and aarch64), Windows and macOS (product archives uploaded as artifacts; a failing aarch64 build does not fail the workflow); unit, UI and integration tests; Docker images for the services |
| [`release.yml`](release.yml) (*Release*) | push of any tag | Docker images for the services |
| [`report.yml`](report.yml) (*Test report*) | `workflow_run` after *PR* or *Merge to master* completes | publishes JUnit results as check runs |

`pr.yml` and `branch.yml` use a `concurrency` group, so a new push cancels the
still-running workflow for the same PR/branch.

## Reusable workflows

| Workflow | Purpose | Inputs |
|----------|---------|--------|
| [`_lint.yml`](_lint.yml) | Runs [pre-commit](../../.pre-commit-config.yaml) on the files changed between two refs | `from-ref`, `to-ref` |
| [`_build.yml`](_build.yml) | `mvn install -DskipTests` | `os` (default `ubuntu-latest`), `upload-artifacts` (upload `phoebus-product/target/*.{tar.gz,zip}`), `allow-failure` (set `continue-on-error` on the job) |
| [`_test.yml`](_test.yml) | Generic "run a test command, upload reports" job | `name`, `run`, `report-path` |
| [`_unit-test.yml`](_unit-test.yml) | `_test.yml` with `mvn verify` (surefire) | – |
| [`_ui-test.yml`](_ui-test.yml) | `_test.yml` with `xvfb-run -a mvn verify -P ui-tests` (TestFX, headless) | – |
| [`_integration-test.yml`](_integration-test.yml) | `-P it-tests,docker-tests` for `services/save-and-restore` against an Elasticsearch service container | – |
| [`_docker-image.yml`](_docker-image.yml) | Builds a service with Maven, then builds and pushes a multi-arch (amd64/arm64) image to `ghcr.io/<owner>/<repo>/<image-suffix>` | `image-suffix`, `context`, `maven-args` |

All jobs that need Java use the composite action
[`../actions/setup-java`](../actions/setup-java/action.yml)
(Temurin JDK with Maven dependency caching).

### Lint ranges

Only changed files are linted, because `master` is not yet clean for the
`end-of-file-fixer` / `trailing-whitespace` hooks:

- PR: `pull_request.base.sha` … `pull_request.head.sha`
- Branch push: `origin/master` … pushed commit (`github.event.before` would be
  the all-zero SHA on the first push of a new branch)
- Merge to master: `github.event.before` … pushed commit

To reproduce locally: `pre-commit run --from-ref origin/master --to-ref HEAD`.

### Docker images

`merge.yml` and `release.yml` build images for these services (matrix):
`alarm-server`, `alarm-logger`, `save-and-restore`, `scan-server`.
Each is published as `service-<name>`, built from `services/<name>`.
`scan-server` additionally needs the `-Pexecutable-jar` Maven profile.
Image tags are derived by `docker/metadata-action` from the branch or tag name.

### Test reports

Test jobs upload their surefire/failsafe XML as artifacts named
`<name>-reports` / `integration-test-reports`. Pull requests from forks only
get a read-only token, so they cannot publish check runs themselves.
`report.yml` therefore runs separately in the context of the base repository
(with `checks: write`), downloads all `*test-reports` artifacts of the
completed run and publishes them. It never checks out or runs PR code — keep
it that way (this is why the `dangerous-triggers` zizmor finding is
suppressed there).

## Conventions

- Default token permissions are `contents: read`; jobs that need more
  (`packages: write` for Docker, `checks`/`pull-requests: write` for reports)
  declare them explicitly.
- Third-party actions are pinned to a full commit SHA and
  `persist-credentials: false` is set on checkout. The pre-commit hook
  [zizmor](https://github.com/zizmorcore/zizmor) checks workflows for these
  and other security issues.
- Untrusted or input-derived values are passed to `run:` steps through `env:`
  variables rather than being interpolated with `${{ }}` directly.
- Action versions are bumped by Dependabot ([`../dependabot.yml`](../dependabot.yml)).
  Manually pinned versions (JDK, Elasticsearch) are listed in
  [`../CI_VERSIONS.md`](../CI_VERSIONS.md).
