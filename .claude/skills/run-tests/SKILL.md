---
description: Run and write tests for the Loot Lock mod (unit tests, game tests, manual testing). Use when asked to test, run tests, write tests, or verify changes.
---

# Run Tests

Run unit tests, game tests, and manual tests for the Loot Lock mod.

## Quick Commands

```bash
# Apply code formatting
./gradlew spotlessApply

# Run unit tests
./gradlew test

# Run game tests (headless)
./gradlew runGametestServer

# Full build with all checks
./gradlew clean build

# Manual testing
./gradlew runClient
```

## Test Types

| Test Type | Location | Run Command |
|-----------|----------|-------------|
| Unit Tests (JUnit 5) | `src/test/java/` | `./gradlew test` |
| Game Tests | `src/main/java/.../gametest/` | `./gradlew runGametestServer` |
| Manual Tests | In-game client | `./gradlew runClient` |

## Java 21 Required

Ensure Java 21 is active:
- **jenv**: `jenv local 21`
- **SDKMAN**: `sdk use java 21-amzn`

## Writing Tests

**Unit Test Example:**
```java
@Test
@DisplayName("LootLock mod id should be stable")
void testModId() {
	assertEquals("loot-lock", LootLock.MOD_ID);
}
```

**Game Test Example:**
```java
@GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
public void entitySpawns(final TestContext context) {
	// Spawn and test entity behavior
	context.complete();
}
```

## Test Workflow

Before pushing changes:
1. `./gradlew spotlessApply` - Format code
2. `./gradlew test` - Run unit tests
3. `./gradlew runGametestServer` - Run game tests
4. `./gradlew runClient` - Manual test if functionality changed
