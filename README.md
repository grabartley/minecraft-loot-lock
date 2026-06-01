<p align="center">
<img src="docs/banner.png" alt="Loot Lock Banner" width="800">
</p>

[![Build](https://github.com/grabartley/minecraft-loot-lock/actions/workflows/cicd.yml/badge.svg)](https://github.com/grabartley/minecraft-loot-lock/actions/workflows/cicd.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-yellow.svg)](LICENSE)
[![Ko-fi](https://img.shields.io/badge/Ko--fi-Support_Loot_Lock-009078?logo=ko-fi&logoColor=white)](https://ko-fi.com/grahambartley)

Your loot, your rules. LootLock is an open-source Fabric mod for per-player ground-item filtering. Server-authoritative, multiplayer-safe, zero junk.

Stop letting your inventory turn into a graveyard of rotten flesh and wheat seeds. LootLock lets every player on the server decide exactly which ground items they want to pick up, and ignores everything else. No more inventory tetris after every cave run. No more shift-clicking junk into hoppers for ten minutes after a raid.

<p align="center">
<img src="https://cdn.modrinth.com/data/8AB6MX34/images/4d0fb71bea06889427ebfe9d73f5033dfab65d17.gif" alt="Example gif of egg pickup being locked based on active denylist">
</p>

## Built For

**Survival players** who are tired of picking up eggs when breeding their chickens or wheat seeds when clearing grass.

**Modpack authors** who need a clean, sane way to give players inventory control without bolting on five separate utility mods.

**SMP servers** where one player wants to hoard every glow berry and the other wants nothing but diamonds. Both can have their way, on the same world, at the same time.

**Anyone who has ever yelled** at their screen because they ran over a pile of seeds while trying to grab netherite.

![Profile configuration item search interface](https://cdn.modrinth.com/data/8AB6MX34/images/8269a6959aa4832c43e78465cb3ed4a9e284cdf1.png)

![Profile view interface](https://cdn.modrinth.com/data/8AB6MX34/images/64421f353f65dad6e1a8b34fed5215188887d368.png)

## What You Get

- **Per-player profiles** with independent rules, so your filter does not affect anyone else's gameplay.
- **Allowlist or denylist mode** per profile. Pick exactly what to grab, or pick exactly what to skip.
- **Multiple profiles per player** for fast switching. Mining loadout, building loadout, farming loadout. Swap in one click.
- **Leave on ground or delete** rejected items. Delete mode includes safety confirmations and an operator opt-out.
- **In-game UI plus full command coverage** so you can manage rules however you prefer.
- **Server-authoritative** state with proper edit synchronisation. No desync. No cheaty client overrides.
- **Works for vanilla clients too.** Players without the mod installed can still benefit if a server operator manages their rules through commands.

## Quick Start

1. Install LootLock and Fabric API into your `mods` folder.
2. Launch the game, join your world, and open the LootLock UI (set the keybind under `Controls > LootLock` first).
3. Create a profile, pick a mode, add the items you care about. Test it by walking over a few drops.

That is the whole setup. No config file editing required.

## Controls

LootLock ships with two keybinds, both unbound by default. Set them in `Controls > LootLock`:

- **Open UI** opens the main LootLock screen.
- **Cycle Profile** swaps to your next saved profile in one keypress.

## Commands

All commands are rooted at `/lootlock`. The in-game UI covers most of these, but commands are the only path if you do not have the client mod installed.

| Command | What it does | Notes |
| --- | --- | --- |
| `/lootlock` | Prints quick command help. | Works without player context. |
| `/lootlock status` | Shows active profile, enabled state, mode, action, and rule count. | Player only. |
| `/lootlock enable` | Enables LootLock for your active profile. | Player only. |
| `/lootlock disable` | Disables LootLock for your active profile. | Player only. |
| `/lootlock mode denylist` | Sets active profile filter mode to denylist. | Player only. |
| `/lootlock mode allowlist` | Sets active profile filter mode to allowlist. | Player only. |
| `/lootlock action leave` | Sets rejected-item behaviour to leave drops on the ground. | Player only. |
| `/lootlock action delete` | Shows safety warning and confirmation instructions for delete mode. | Player only. |
| `/lootlock action delete confirm` | Sets rejected-item behaviour to permanently delete rejected drops. | Player only, blocked if server policy disallows delete mode. |
| `/lootlock profile list` | Lists your profiles and marks the active one. | Player only. |
| `/lootlock profile create <name>` | Creates a new profile with default settings. | Names are trimmed and must be 1 to 32 chars. |
| `/lootlock profile delete <name>` | Deletes a profile by name. | Cannot delete your last profile. |
| `/lootlock profile activate <name>` | Activates a profile by name, then shows status. | Player only. |
| `/lootlock rule add <namespace:item>` | Adds an item rule to the active profile. | Item id must exist. |
| `/lootlock rule remove <namespace:item>` | Removes an item rule from the active profile. | Player only. |
| `/lootlock rule list` | Lists rules in the active profile. | Player only. |
| `/lootlock rule clear` | Shows confirmation hint before clearing rules. | Player only. |
| `/lootlock rule clear confirm` | Removes all rules from the active profile. | Player only. |
| `/lootlock policy` | Shows current server policy values. | Requires operator permission level 2. |
| `/lootlock policy allowDeleteRejectedItems true` | Allows players to use delete mode for rejected items. | Requires operator permission level 2. |
| `/lootlock policy allowDeleteRejectedItems false` | Blocks delete mode and forces leave mode behaviour. | Requires operator permission level 2. |

## Compatibility

- **Minecraft:** `1.20.1`
- **Loader:** Fabric `0.19.2+`
- **Fabric API:** `0.92.9+1.20.1` minimum
- **Java:** `21`
- **Environments:** dedicated server and integrated server, with or without client mod installed

## Dependencies

| Dependency | Version | Required | Purpose |
| --- | --- | --- | --- |
| Fabric Loader | `>=0.19.2` | Yes | Mod loader |
| Fabric API | `>=0.92.9+1.20.1` | Yes | Fabric hooks and APIs |
| Mod Menu | `>=7.2.2` | No | Settings access from the mod list |

## Permissions

Operator permission level `2` is required for the `policy` command path and for managing other players' profiles. Players without operator permissions can still fully manage their own profile through the UI or commands.

## Open Source

LootLock is open source under the MIT license.

If LootLock saved your sanity on a long survival run, a star on GitHub or a coffee on Ko-fi goes a long way.
