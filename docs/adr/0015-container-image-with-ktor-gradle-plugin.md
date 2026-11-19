# 15. Build the container image with the Ktor Gradle plugin

- **Status:** Accepted
- **Date:** 2025-05-28

## Context

The service should run as a container next to other home services. An early version had its own `Dockerfile`.

## Decision

Remove the `Dockerfile` and use the Ktor Gradle plugin (`io.ktor.plugin`): `buildFatJar`, `buildImage`,
`publishImageToLocalRegistry`. The image uses the JRE version from `java_version`, the name `luxtronik-restful-api`
and the tag from `app_version`, and maps port `8080`.

## Consequences

- No separate Dockerfile to maintain. Versions come from `gradle.properties`.
- Less control over the image (base image, user, layers) than a hand-written Dockerfile.
- Every release needs a version bump in `gradle.properties` (the `chore: bump version` commits).
