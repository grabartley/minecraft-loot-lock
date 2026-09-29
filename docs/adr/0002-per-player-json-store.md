# ADR 0002: Player data is one JSON file per player, per world, saved on a debounce

- **Status:** Accepted
- **Date:** 2026-09-29

## Context

Each player owns up to nine profiles, each with up to 1024 rules, plus an active profile and an on and off switch. That data is read on every pickup and written whenever the player edits a rule, sometimes many times a second while dragging items into the panel.

Operators need to edit players who are offline, including players who have never joined, identified only by UUID. Operators also need to be able to find, back up, and repair the data by hand.

A corrupt or unreadable file must never stop a player joining or a server starting, and must never be silently destroyed.

## Decision

`ServerPlayerDataManager` owns an in-memory cache of player data, backed by one JSON file per player at `<world>/lootlock/players/<uuid>.json`. The files live in the world save directory, so data is per world.

- **Load lazily.** A player's file is read the first time anything needs it: joining, a pickup, or an operator command naming their UUID. A missing file creates default data, marked dirty so it gets written.
- **Save on a debounce.** Every change increments the data's revision and marks the entry dirty. The server tick saves an entry once 40 ticks have passed since its most recent change, so a burst of edits is one write.
- **Save at the edges.** Disconnect saves a dirty entry and evicts it from the cache. Server stop flushes every dirty entry.
- **Write atomically.** A save writes a `.tmp` sibling and moves it over the real file with an atomic move.
- **Never destroy a bad file.** A file that fails to read or parse is moved aside as `<uuid>.broken.<timestamp>.json` when the move succeeds, and the player starts from defaults. If the move fails it is only logged, and the next save overwrites the original.
- **Version the schema.** Each file carries a `schemaVersion`. Older versions are migrated forward on load by `ConfigMigration`. A version newer than the mod knows is treated like a corrupt file.
- **One thread.** The cache is only touched on the server thread, which is where Fabric's lifecycle events, tick events, commands, and play packet receivers all run, so it needs no locking.

The store for a world is created when that world's session opens, from the `LevelStorage` session, before the server is constructed.

## Consequences

Rule edits are cheap. Dragging twenty items into the panel costs twenty in-memory changes and one file write.

Operators can pre-stage a profile for a player who has never joined, because the store is keyed by UUID and loads on demand. The same path serves online and offline targets.

The data is plain JSON in the world folder. It is included in world backups, readable in any editor, and can be repaired by hand with the server stopped.

A crash loses a player's unsaved edits: those from the last two seconds, or longer if they kept editing without a two second pause. A crash mid-write cannot leave a half-written file, because the move is atomic.

Data is per world, not global. A player on two worlds, or two servers, has separate profiles. Share codes exist to carry a profile between them. See [ADR 0003](0003-profile-share-codes.md).

Downgrading the mod after a schema change moves newer files aside rather than reading them, and players start from defaults. The original file is kept beside the new one, so upgrading again and renaming it back recovers it.

Accepted drawback: anything that touches the store from another thread, such as async chat handling or a future off-thread packet receiver, must hop back to the server thread first.
