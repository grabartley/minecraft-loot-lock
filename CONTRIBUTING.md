# Contributing to Loot Lock

Thanks for helping improve Loot Lock.

## Development Environment Setup

1. Install Java `21` (`.java-version` pins it for jenv and similar tools).
2. Clone the repository.
3. Run `./gradlew build` once to download dependencies and verify local setup.
4. Use the included Gradle tasks for all validation.

## Required Validation

Run this before opening or updating a PR:

- `./gradlew check`

This includes formatting checks, unit and client tests, coverage, and side-safety verification.

## Side-Safety Rules

Loot Lock supports dedicated server operation. `src/main` must stay server-safe.

- `src/main/java` must not reference `net.minecraft.client.*`
- `src/main/java` must not reference `com.grahambartley.lootlock.client.*`
- Client UI, keybinds, networking senders, and rendering code must stay in `src/client/java`, under `com.grahambartley.lootlock.client`
- Client-side tests live in `src/clientTest/java`

## Validation Commands

- Side-safety only: `./gradlew verifyMainSourceSideSafety`
- Full verification: `./gradlew check`
- Unit tests only: `./gradlew test`
- Client tests only: `./gradlew clientTest`
- Coverage report: `./gradlew test` writes `build/reports/jacoco/test/html/index.html`
- Dedicated server smoke boot: `mkdir -p run && echo "eula=true" > run/eula.txt`, then `./gradlew runServer --args="nogui"` and wait for `Done (` before typing `stop`
