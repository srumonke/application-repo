# application-repo

Spring Boot demo application for a **Harness CI/CD + GitOps** demo.

This repo holds the **application source code**. CI (Harness) builds/tests it, containerizes it with Jib, pushes the image, and then updates image tags in the separate [`release-repo`](https://github.com/srumonke/release-repo), which ArgoCD (Harness GitOps) syncs to the cluster.

## Tech stack

- Java 21, Spring Boot 3.3.2, Maven
- [Jib](https://github.com/GoogleContainerTools/jib) for daemonless container builds

## Endpoints

| Method | Path      | Description                       |
|--------|-----------|-----------------------------------|
| GET    | `/health` | Liveness/readiness — `{"status":"UP"}` |
| GET    | `/greet`  | Greeting message                  |

## Local development

Java 21 is required:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

mvn clean test          # build + test
mvn package             # build JAR
java -jar target/demo-app-1.0.0.jar   # run on :8080
```

## Container build

```bash
# Build straight to a registry (no Docker daemon needed)
mvn compile jib:build \
  -Ddocker.image.name=<registry>/demo-app \
  -Ddocker.image.tag=v1.2.3

# Build to local Docker daemon for testing
mvn compile jib:dockerBuild -Ddocker.image.name=demo-app -Ddocker.image.tag=test
```

## CI/CD flow

1. Push to `main` → Harness CI runs `mvn clean test`.
2. Jib builds and pushes the image tagged with the build ID.
3. Pipeline updates the image tag in `release-repo` for the target environment.
4. Harness GitOps / ArgoCD detects the manifest change and syncs it to the cluster.

See `CLAUDE.md` for architecture details.
