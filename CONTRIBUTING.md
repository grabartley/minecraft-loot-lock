# Contributing to Loot Lock

Thanks for helping improve Loot Lock.

## Development Environment Setup

1. Install Java `17`.
2. Clone the repository.
3. Run `./gradlew build` once to download dependencies and verify local setup.
4. Use the included Gradle tasks for all validation.

## Required Validation

Run this before opening or updating a PR:

- `./gradlew check`

This includes formatting checks, tests, and side-safety verification.

## Side-Safety Rules

Loot Lock supports dedicated server operation. `src/main` must stay server-safe.

- `src/main/java` must not reference `net.minecraft.client.*`
- `src/main/java` must not reference `com.grahambartley.client.*`
- Client UI, keybinds, and rendering code must stay in `src/client/java`

## Validation Commands

- Side-safety only: `./gradlew verifyMainSourceSideSafety`
- Full verification: `./gradlew check`
- Unit tests only: `./gradlew test`
- Dedicated server smoke boot: `timeout 120s ./gradlew runServer --no-daemon --stacktrace --args="nogui" || test $? -eq 124`
