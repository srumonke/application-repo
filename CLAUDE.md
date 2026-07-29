# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Minimal Spring Boot 3.3.2 application (Java 21) designed for CI/CD demonstrations on Harness. Two REST endpoints (`/health`, `/greet`) backed by a thin service layer. No database, no complex business logic—intentionally lightweight for pipeline testing.

## Architecture

- **Package structure**: `com.harness.demo` with standard Spring Boot layout
  - `controller/` - REST endpoints (@RestController)
  - `service/` - Business logic (@Service)
  - `model/` - Response records (Java records)
- **Dependency injection**: Constructor injection (Spring default)
- **Response format**: JSON via Spring Boot's automatic serialization of records

## Build & Test Commands

**Prerequisites**: Java 21 must be active. On macOS with Homebrew:
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
```

**Common commands**:
```bash
# Full build with tests
mvn clean test

# Package executable JAR
mvn package

# Run locally (app starts on port 8080)
java -jar target/demo-app-4.1.0.jar

# Run single test class
mvn test -Dtest=HealthControllerTest

# Run single test method
mvn test -Dtest=HealthControllerTest#testHealthEndpoint
```

## Container Builds

Uses **Jib Maven Plugin** (NOT Dockerfile) for containerization. No Docker daemon required for building.

**Image name/tag parameterization** via Maven properties:
```bash
# Build to remote registry (requires authentication)
mvn compile jib:build \
  -Ddocker.image.name=myregistry.io/demo-app \
  -Ddocker.image.tag=v1.2.3

# Build to local Docker daemon
mvn compile jib:dockerBuild \
  -Ddocker.image.name=demo-app \
  -Ddocker.image.tag=test

# Run containerized app
docker run -p 8080:8080 demo-app:test
```

**Base image**: `eclipse-temurin:21-jre` (official Eclipse Adoptium JRE, chosen for demo debugging convenience over distroless).

## Testing Strategy

- **@SpringBootTest** (`DemoApplicationTests`) - context load smoke test
- **@WebMvcTest** (`HealthControllerTest`) - controller layer tests with mocked service
  - Covers `/health` endpoint
  - Covers `/greet` with and without `name` parameter (tests default value behavior)

When adding endpoints: use `@WebMvcTest(ControllerName.class)` with `@MockBean` for services. This is faster than full `@SpringBootTest` for controller testing.

## API Endpoints

- `GET /health` → `{"status":"UP"}`
- `GET /greet?name=Alice` → `{"message":"Hello, Alice!"}`
- `GET /greet` (no param) → `{"message":"Hello, World!"}`
