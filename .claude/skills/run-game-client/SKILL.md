---
description: Runs the Minecraft game client locally with the mod for manual testing. Use when asked to launch, run, or start the game client for testing.
---

# Run Game Client

Runs the Minecraft client with the Loot Lock mod for manual testing.

## Quick Start

Ensure Java 21 is active (`jenv local 21` or `sdk use java 21-amzn`), then:

```bash
./gradlew runClient
```

## Testing Commands

- `/gamemode creative` - give yourself editing freedom while testing
- `/give @s minecraft:diamond 1` - quickly validate pickup and inventory behavior
- `/lootlock` - check command registration and available subcommands

## Post-Test Protocol

After running the game client for manual testing, always ask for human input before continuing with next steps.

## Hot Reload

Press **F3+T** to reload textures/models without restarting.

## Related Skills

- `automated-qa`, use instead of this skill when the client should drive itself and capture screenshot evidence for a PR
