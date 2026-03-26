---
title: "Build from Source"
description: "Install and run GROBID without Docker"
sidebar_position: 3
---

# Build from Source

Use this path if you are contributing to GROBID, debugging the codebase directly, or working on local development tasks that are awkward inside Docker.

If your goal is simply to run GROBID reliably, use [Quick Start (Docker)](./quickstart-docker) instead. Docker remains the safest default for most users.

## When this path makes sense

Build from source if you need to:

- modify the codebase
- run the service directly during development
- debug build, runtime, or model-loading issues locally
- work on training or advanced integration scenarios

Do not choose this path just because it sounds more flexible. Native and local setups expose more moving parts than the Docker path.

## Before you start

You need:

- JDK 21 or newer
- a working Git checkout of the repository
- a path without parent directories containing spaces

That last point matters. Paths with spaces have caused repeated problems in native and JNI-related setups.

## 1. Verify Java

Check both Java and the JDK compiler:

```bash
java -version
javac -version
```

You should see Java 21 or newer.

## 2. Get the source

Clone the repository:

```bash
git clone https://github.com/grobidOrg/grobid.git
```

Then work from the repository root.

## 3. Build GROBID

The standard build command is:

```bash
./gradlew clean install
```

If you explicitly want to run tests too:

```bash
./gradlew clean install test
```

For most development setups, start with the standard build first and only add more variables if you need them.

## 4. Start the service locally

From the repository root:

```bash
./gradlew run
```

The service starts on port `8070` by default.

Important:

- the Gradle process may appear to hang at a high percentage while the service is running
- that is normal for this workflow

## 5. Verify that it works

Check these URLs:

- `http://localhost:8070`
- `http://localhost:8070/api/version`
- `http://localhost:8070/api/isalive`
- `http://localhost:8070/api/health`

Then continue with [REST API Usage](../guides/api/rest-api-usage).

## Common local pitfalls

### Paths with spaces

Avoid them if possible, especially in parent directories. This has caused repeated local setup failures.

### Java version mismatch

If the build behaves strangely, confirm again that both `java` and `javac` are pointing to JDK 21+.

### Proxy-restricted environments

If you are building behind a proxy, you may need to set Gradle proxy settings in `gradle.properties`.

### Windows-native execution

Native Windows execution is not the preferred path. If you are on Windows and do not specifically need a local/native workflow, go back to Docker.

## If something fails

Go to:

- [Troubleshooting](../guides/troubleshooting)
- [Configuration Guide](../guides/configuration)

If you are unsure whether the issue is your local environment or GROBID itself, compare the same workflow against the Docker path. That is often the fastest way to separate local-environment problems from actual product problems.
