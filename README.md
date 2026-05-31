# LootLock

LootLock is a server-authoritative Fabric mod that filters item pickups per player, so everyone can keep the loot they want and ignore the rest without micromanaging inventory every minute.

## Why LootLock Exists

In busy survival worlds, inventory chaos is real. LootLock gives each player customizable pickup rules, then enforces those rules on the server for predictable, multiplayer-safe behavior.

## Capabilities

- Per-player profiles with independent pickup behavior.
- Two filter modes: allowlist and denylist.
- Two rejected-item actions: leave item on ground or delete item entity.
- Rule management by command and in-game UI.
- Server-authoritative state with client sync and stale-update protection.
- Safety rails for destructive behavior, including explicit delete-mode confirmations.
- Dedicated-server safe architecture, client-only code isolated to `src/client`.
- Works for players with and without the client mod installed.

## How It Works

1. The server evaluates item pickup events against the active player profile.
2. If a rule allows pickup, behavior stays vanilla.
3. If a rule rejects pickup, LootLock applies the configured reject action.
4. Client installs receive synced state and editing tools, server-only players can still fully manage via commands.

## Commands at a Glance

LootLock ships command coverage for day-to-day admin and player use, including:

- Status and emergency toggles.
- Profile creation, activation, rename, duplicate, and delete.
- Rule add, remove, list, and clear operations.
- Delete-mode confirmation and safety-oriented flows.

Use `/lootlock` in-game to explore the full command tree.

## Compatibility

- Minecraft: `1.20.1`
- Java: `17+`
- Environments: dedicated server and client-supported multiplayer
- Side model: server-authoritative, optional client UX enhancements

## Required and Optional Dependencies

### Runtime Required

- Fabric Loader: `0.19.2` or newer
- Fabric API: `0.92.9+1.20.1`

### Runtime Optional

- Mod Menu: `7.2.2` (for launcher-side discoverability and settings entrypoint)

### Development Toolchain

- Yarn mappings: `1.20.1+build.10`
- Fabric Loom: `1.16-SNAPSHOT`
- JUnit Jupiter: `5.10.2`

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

1. Server sends capabilities packet if the client supports LootLock channels.
2. Client responds with hello packet including version/schema context.
3. Server sends authoritative player snapshot.
4. Client may request additional snapshots after conflicts or reconnect flows.

If a player does not have the client mod, server-side pickup enforcement and command management continue to work normally.

## Side-Safety and Validation Guardrails

`src/main` is kept server-safe by policy and build checks.

- Side safety only: `./gradlew verifyMainSourceSideSafety`
- Full local verification: `./gradlew check`
- Full test suite: `./gradlew test`
- Dedicated server smoke boot: `timeout 120s ./gradlew runServer --no-daemon --stacktrace --args="nogui" || test $? -eq 124`

## First Release Version

The first public release line for this project is `0.21.0`.

## License

This project is licensed under `CC0-1.0`. See `LICENSE` for details.
