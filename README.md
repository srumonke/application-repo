# application-repo — `service-a`

Spring Boot demo service for a **Harness CI/CD + GitOps** pipeline. This repo holds the
**application source**; deployment manifests live in the separate
[`release-repo`](https://github.com/srumonke/release-repo), which Harness GitOps (ArgoCD) syncs
to the cluster.

The demo showcases an **isolated CVE hotfix promoted dev → prod by image retag** — building an
image once, then promoting the *same bytes* to prod without a rebuild.

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
```

## Versioning & branching

Releases follow a `4.1.x` line. Production runs a tagged baseline; hotfixes branch **off the
production tag**, never off `main`, so a fix ships without pulling in unreleased work. The
**candidate** version lives in `pom.xml` `<version>` (e.g. `4.1.2-RC1`); the **release** tag is that
value with `-RC*` stripped (`4.1.2`).

```
main ──●────────────●              (ongoing development)
        \
 v4.1.0-service-a (prod baseline)
          \
           ●── hotfix/CVE-XXXX ──► 4.1.2-RC1 (candidate) ──► 4.1.2 (released, by retag)
```

## CI/CD — one Harness pipeline: `service-a-hotfix`

A single pipeline (Harness Cloud build farm + Harness GitOps) with five stages. **Nothing is
hardcoded** — the image tag is read from `pom.xml` at runtime.

```
1. Build Candidate  (CI)
     ├─ Resolve Version  — read mvn project.version → POM_VERSION; strip -RC → RELEASE_TAG
     ├─ AquaTrivy scan   — shift-left STO gate, fail_on_severity: critical
     └─ Build & Push     — Jib builds ONCE → ghcr.io/srumonke/service-a:$POM_VERSION
2. Deploy to Dev    — GitOps sync of the dev environment
3. Approval         — governance gate (audited)
4. Promote Release  (CI)
     └─ Retag           — crane tag $POM_VERSION → $RELEASE_TAG in GHCR (same digest, no rebuild)
5. Deploy to Prod   — Update Release Repo (opens PR → prod image = $RELEASE_TAG) → Merge PR → GitOps sync
```

**Promote by reference, not rebuild:** step 4 retags the validated candidate to the release
version. Identical digest, no layer re-upload — the immutable-artifact promotion pattern.

## Event-driven triggers

The pipeline is wired to GitHub webhooks on this repo:

| Trigger | Fires on | 
|---------|----------|
| `pr_build_scan` | a **pull request** opened/updated against `main` (Build Type: Git Pull Request) |
| `merge_deploy`  | a **push to `main`** where `pom.xml` changed (i.e. a merged hotfix PR) |

**Developer flow:** branch off `main` → bump `pom.xml` version + patch the dependency → push →
open a PR (fires the pipeline) → review & merge (fires the pipeline).

## Testing strategy

- `@SpringBootTest` (`DemoApplicationTests`) — context-load smoke test.
- `@WebMvcTest` (`HealthControllerTest`) — controller tests with mocked services (fast).

When adding an endpoint: add the controller method, a response record in `model/`, and a
`@WebMvcTest` covering it, then `mvn clean test`.
