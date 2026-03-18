# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> See also: `../../CLAUDE.md` (repo root) for full domain architecture, entity model, and project rules.

## Backend: gersangtrade

Spring Boot 4.0.3, Java 21. Base package: `org.example.gersangtrade`.

## Commands

```bash
# Build
./gradlew build

# Run application
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "org.example.gersangtrade.SomeTestClass"

# Clean build
./gradlew clean build
```

## Configuration

`src/main/resources/application.properties` only sets `spring.application.name=gersangtrade`. Required additions:
- `spring.datasource.*` — MySQL connection
- `spring.security.oauth2.client.*` — OAuth2 login
- `spring.batch.*` — batch job configuration
- `spring.flyway.*` — migration configuration (planned)

In production, use `spring.jpa.hibernate.ddl-auto=validate` and let Flyway manage schema.
