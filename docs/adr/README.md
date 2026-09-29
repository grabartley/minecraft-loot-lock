# Architecture Decision Records

These records capture **why** Loot Lock is built the way it is. They are not specifications: what the mod does lives in the [README](../../README.md), and the issues remain the working specification on acceptance criteria.

A record is historical. When a decision changes, add a new record superseding the old one rather than editing it, so the reasoning behind the original choice is not lost.

Shared engineering standards across all the mods in this family live in [`../standards.md`](../standards.md).

| Record | Decision |
|---|---|
| [0001](0001-pickup-filtering-runs-on-the-server.md) | Pickup filtering runs on the server, and clients only send intent |
| [0002](0002-per-player-json-store.md) | Player data is one JSON file per player, per world, saved on a debounce |
| [0003](0003-profile-share-codes.md) | Profiles travel as versioned, compressed share codes |
