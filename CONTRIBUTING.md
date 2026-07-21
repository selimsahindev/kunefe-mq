# Contributing to Kunefe MQ

Thank you for your interest in contributing! This document explains how to get started.

---

## Getting Started

### Prerequisites

- Java 21
- Gradle 9.x
- Docker (for integration tests and local broker)

### Build from Source

```bash
git clone https://github.com/selimsahindev/kunefe-mq.git
cd kunefe-mq
./gradlew build
```

### Run the Broker Locally

```bash
./gradlew :kunefe-broker:bootRun
```

### Run Tests

```bash
# Unit tests
./gradlew test -x :kunefe-test:test

# Integration tests (broker must not be running separately)
./gradlew :kunefe-test:test
```

---

## How to Contribute

### Reporting Bugs

Please use the [bug report template](.github/ISSUE_TEMPLATE/bug_report.md) when opening an issue.

Include:
- Steps to reproduce
- Expected vs actual behavior
- Java version and OS

### Suggesting Features

Open an issue using the [feature request template](.github/ISSUE_TEMPLATE/feature_request.md) before starting work on a large feature. This avoids duplicate effort and ensures alignment.

### Submitting a Pull Request

1. Fork the repository
2. Create a branch from `main`:
```bash
   git checkout -b feat/your-feature
```
3. Make your changes
4. Write or update tests
5. Ensure all tests pass:
```bash
   ./gradlew test
```
6. Follow the commit message convention (see below)
7. Open a pull request against `main`

---

## Commit Message Convention

We use [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(broker): add partition support
fix(client): handle connection timeout gracefully
docs: update README with new configuration options
chore(build): upgrade Spring Boot to 3.5.0
test(broker): add TopicLog segment rolling tests
refactor(consumer): extract offset management to separate class
```

| Prefix | When to use |
|---|---|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `chore` | Build, dependencies, tooling |
| `test` | Tests only |
| `refactor` | Code change without behavior change |
| `perf` | Performance improvement |

---

## Code Style

- Follow standard Java conventions
- Always use `{}` braces for control structures — never inline
- Constructor injection over field injection (`@Autowired`)
- Prefer interfaces over concrete classes for dependencies (dependency inversion)
- JavaDoc for public APIs

---

## Module Structure

```
kunefe-proto/               # Protobuf contracts
kunefe-broker/              # Broker application
kunefe-client/              # Core Java client
kunefe-spring-boot-starter/ # Spring Boot auto-configuration
kunefe-test/                # Integration tests
```

When adding a new feature, consider which module it belongs to before starting.

---

## Security

Please do not open public issues for security vulnerabilities. See [SECURITY.md](SECURITY.md) for responsible disclosure.

---

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).
