# CI version pins

Some versions used by CI are pinned manually and must be kept current. This
file is the checklist of what needs periodic review and where each pin lives.

| What | Version | Defined in | Notes |
|------|---------|------------|-------|
| Java (JDK) | `21` | `.github/actions/setup-java/action.yml` | Single source of truth for the CI JDK. Must match `maven.compiler.source`/`maven.compiler.target` in `pom.xml`. Review when the project adopts a new LTS. |
| Elasticsearch | `8.11.2` | `.github/workflows/_integration-test.yml` and `services/save-and-restore/docker-compose.yml` | Service container for the save-and-restore integration tests. Update both files together. |

## Automatically maintained

GitHub Action versions (the `uses:` refs in `.github/workflows/*` and
`.github/actions/**`) are bumped by Dependabot — see `.github/dependabot.yml`.
These do **not** need manual tracking here.
