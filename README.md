![LootLock banner](.//src/main/resources/assets/loot-lock/banner.png)

[![Build](https://github.com/grabartley/minecraft-loot-lock/actions/workflows/build.yml/badge.svg)](https://github.com/grabartley/minecraft-loot-lock/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-yellow.svg)](LICENSE)

LootLock is a server-authoritative Fabric mod that filters item pickups per player, so everyone can keep the loot they want and ignore the rest without micromanaging inventory every minute.

## Why LootLock Exists

In busy survival worlds, inventory chaos is real. LootLock gives each player customizable pickup rules, then enforces those rules on the server for predictable, multiplayer-safe behavior.

Common use cases:

- Late-game survival cleanup: stop auto-picking low-value drops like `minecraft:egg` and `minecraft:wheat_seeds` while still grabbing the loot you care about.
- Cave and exploration runs: filter decorative clutter from newer biomes, like lush cave plants and other decorative blocks, so inventory space stays focused on resources.
- Multiplayer role builds: keep separate profiles for mining, building, and farming so you can switch behavior fast instead of rewriting rules every session.

## Capabilities

- Allowlist and denylist filtering so players can tune pickup behavior fast.
- Leave on ground and delete rejected-item actions, with safety confirmations for delete flows.
- Per-player profiles with independent pickup behavior.
- Rule management by command and in-game UI.
- Server-authoritative state with client sync and stale-update protection.
- Dedicated-server safe architecture, client-only code isolated to `src/client`.
- Works for players with and without the client mod installed.


## How It Works

1. The server evaluates item pickup events against the active player profile.
2. If a rule allows pickup, behavior stays vanilla.
3. If a rule rejects pickup, LootLock applies the configured reject action.
4. Client installs receive synced state and editing tools, server-only players can still use management commands when they have operator permissions.

## Controls

LootLock ships with two keybinds that default to unbound:

- Open UI
- Cycle Profile

Set these in `Controls > LootLock` before first use.

## Commands

All commands are rooted at `/lootlock`.

| Command | What it does | Notes |
| --- | --- | --- |
| `/lootlock` | Prints quick command help. | Player context not required for the help output. |
| `/lootlock status` | Shows active profile, enabled state, mode, action, and rule count. | Player only. |
| `/lootlock enable` | Enables LootLock for your active profile. | Player only. |
| `/lootlock disable` | Disables LootLock for your active profile. | Player only. |
| `/lootlock mode denylist` | Sets active profile filter mode to denylist. | Player only. |
| `/lootlock mode allowlist` | Sets active profile filter mode to allowlist. | Player only. |
| `/lootlock action leave` | Sets rejected-item behavior to leave drops on the ground. | Player only. |
| `/lootlock action delete` | Shows safety warning and confirmation instructions for delete mode. | Player only. |
| `/lootlock action delete confirm` | Sets rejected-item behavior to permanently delete rejected drops. | Player only, blocked if server policy disallows delete mode. |
| `/lootlock profile list` | Lists your profiles and marks the active one. | Player only. |
| `/lootlock profile create <name>` | Creates a new profile with default settings. | Player only, names are trimmed and must be 1 to 32 chars. |
| `/lootlock profile delete <name>` | Deletes a profile by name. | Player only, cannot delete your last profile. |
| `/lootlock profile activate <name>` | Activates a profile by name, then shows status. | Player only. |
| `/lootlock rule add <namespace:item>` | Adds an item rule to the active profile. | Player only, item id must exist. |
| `/lootlock rule remove <namespace:item>` | Removes an item rule from the active profile. | Player only. |
| `/lootlock rule list` | Lists rules in the active profile. | Player only. |
| `/lootlock rule clear` | Shows confirmation hint before clearing rules. | Player only. |
| `/lootlock rule clear confirm` | Removes all rules from the active profile. | Player only. |
| `/lootlock policy` | Shows current server policy values. | Requires operator permission level 2. |
| `/lootlock policy allowDeleteRejectedItems true` | Allows players to use delete mode for rejected items. | Requires operator permission level 2. |
| `/lootlock policy allowDeleteRejectedItems false` | Blocks delete mode and forces leave mode behavior. | Requires operator permission level 2. |

## Permissions

- Operator permission level `2` is required for policy and profile mutation command paths.
- Players without the client mod can still interact through commands when operator permissions are granted.

## Compatibility

- Minecraft: `1.20.1`
- Java: `17+`
- Fabric API: `0.92.9+1.20.1` minimum
- Environments: dedicated server and client-supported multiplayer
- Side model: server-authoritative, optional client UX enhancements

## Dependencies

| Dependency | Version | Requirement | Purpose |
| --- | --- | --- | --- |
| Fabric Loader | `>=0.19.2` | Required runtime | Mod loader |
| Fabric API | `>=0.92.9+1.20.1` | Required runtime | Fabric hooks and APIs |
| Mod Menu | `>=7.2.2` | Optional runtime | Client mod discoverability and settings entrypoint |
| Yarn mappings | `1.20.1+build.10` | Dev toolchain | Named mappings for development |
| Fabric Loom | `1.16-SNAPSHOT` | Dev toolchain | Build and remap pipeline |
| JUnit Jupiter | `5.10.2` | Dev toolchain | Unit testing |

## Installation

1. Install Minecraft `1.20.1`.
2. Install Fabric Loader `0.19.2` or newer.
3. Drop LootLock and Fabric API into your `mods` folder.
4. Optionally add Mod Menu for better client UX.
5. Start the game or server, then run `/lootlock status` to verify setup.

## Quick Start

1. Join your world or server.
2. Open the LootLock UI (if client is installed) or use `/lootlock` commands.
3. Create or select a profile.
4. Choose allowlist or denylist mode.
5. Add rules for item IDs you care about.
6. Test by picking up a few filtered and unfiltered items.

## Networking and Sync Behavior

On join, LootLock performs a lightweight capability handshake:

1. Server sends `loot-lock:server_capabilities_s2c` when `ServerPlayNetworking.canSend` confirms LootLock channels are supported.
2. Client responds with `loot-lock:hello_c2s`, including mod and schema context.
3. Server sends `loot-lock:sync_player_data_s2c` with the authoritative player snapshot.
4. Client can request another snapshot at any time using `loot-lock:request_sync_c2s`.

If a player does not have the client mod, `ServerPlayNetworking.canSend` is false for LootLock channels, no handshake packets are sent, and server-side pickup enforcement plus operator command management still work normally.

## Side-Safety and Validation Guardrails

`src/main` is kept server-safe by policy and build checks.

- Side safety only: `./gradlew verifyMainSourceSideSafety`
- Full local verification: `./gradlew check`
- Full test suite: `./gradlew test`
- Dedicated server smoke boot: `timeout 120s ./gradlew runServer --no-daemon --stacktrace --args="nogui" || test $? -eq 124`

## Version History

GitHub Releases is the source of truth for version history and changelogs:

[https://github.com/grabartley/minecraft-loot-lock/releases](https://github.com/grabartley/minecraft-loot-lock/releases)

## License

This project is licensed under `MIT`. See `LICENSE` for details.
