# application-repo — `service-a`

Spring Boot demo service for a **Harness CI/CD + GitOps** pipeline. This repo holds the
**application source**; deployment manifests live in the separate
[`release-repo`](https://github.com/srumonke/release-repo), which Harness GitOps (ArgoCD) syncs
to the cluster.

The demo showcases an **isolated CVE hotfix promoted dev → prod by image retag** — building an
image once, validating it in dev, then promoting the *same bytes* to prod without a rebuild.

## Tech stack

- Java 21, Spring Boot 3.3.2, Maven
- [Jib](https://github.com/GoogleContainerTools/jib) for daemonless container builds
- Container registry: `ghcr.io/srumonke/service-a`

## Endpoints

| Method | Path      | Description                             |
|--------|-----------|-----------------------------------------|
| GET    | `/health` | Liveness/readiness — `{"status":"UP"}`  |
| GET    | `/greet`  | Greeting message (`?name=` optional)    |

## Local development

Java 21 is required:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

mvn clean test                        # build + test
mvn package                           # build JAR
java -jar target/demo-app-4.1.0.jar   # run on :8080
```

## Container build (Jib)

```bash
# Build straight to a registry (no Docker daemon needed)
mvn compile jib:build \
  -Ddocker.image.name=ghcr.io/srumonke/service-a \
  -Ddocker.image.tag=<tag> \
  -Djib.to.auth.username=<user> -Djib.to.auth.password=<token>

# Build to a local tarball (useful behind a TLS proxy), then push with crane
mvn compile jib:buildTar -Ddocker.image.name=ghcr.io/srumonke/service-a -Ddocker.image.tag=<tag>
crane push target/jib-image.tar ghcr.io/srumonke/service-a:<tag>
```

## Versioning & branching

Releases follow a `4.1.x` line. Production runs a tagged baseline; hotfixes branch **off the
production tag**, never off `main`, so a fix can ship without pulling in unreleased work:

```
main ──●────────────●              (ongoing development)
        \
 v4.1.0-service-a (prod baseline)
          \
           ●── hotfix/CVE-XXXX ──► 4.1.1-RC1 (candidate) ──► v4.1.1 (released, by retag)
```

## CI/CD flow

**CI — `service-a-hotfix-ci`** (Harness Cloud build farm):

1. Checkout the hotfix branch (cut from the prod tag).
2. **Shift-left scan** — Trivy fails the build on any `CRITICAL` CVE in the dependencies
   (see [`.trivyignore`](.trivyignore) for documented, risk-accepted exceptions).
3. **Build once** with Jib → push `ghcr.io/srumonke/service-a:4.1.1-RC1`.

**Promote — retag, no rebuild:** once the candidate is validated in dev, the *same image* is
retagged to the release version. Identical digest, no layer re-upload — the immutable-artifact
promotion pattern:

```bash
crane tag ghcr.io/srumonke/service-a:4.1.1-RC1 v4.1.1   # same sha256, ~seconds
```

**CD — `service-a-hotfix-cd`** (Harness GitOps):

```
Deploy to Dev (GitOps Sync)
      │
Approval  ── governance gate (audited)
      │
Deploy to Prod
      ├─ Update Release Repo ── opens a PR bumping prod/deployment.yaml → v4.1.1
      ├─ Merge PR            ── merges it
      └─ GitOps Sync         ── ArgoCD rolls prod (zero-downtime RollingUpdate)
```

Git stays the single source of truth: prod changes only through a merged PR in `release-repo`.

## Testing strategy

- `@SpringBootTest` (`DemoApplicationTests`) — context-load smoke test.
- `@WebMvcTest` (`HealthControllerTest`) — controller tests with mocked services (fast).

When adding an endpoint: add the controller method, a response record in `model/`, and a
`@WebMvcTest` covering it, then `mvn clean test`.



