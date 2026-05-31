# Networking Handshake (Issue #28)

Status: Implemented

This document describes the implemented initial join sync behavior.

## Flow

1. Server join hook initializes player data and sends `loot-lock:server_capabilities_s2c` when the client can receive LootLock channels.
2. Client receives capabilities and responds with `loot-lock:hello_c2s` including client mod version and schema version.
3. Server handles hello and sends `loot-lock:sync_player_data_s2c` with the authoritative snapshot.
4. Client can request another authoritative snapshot at any time using `loot-lock:request_sync_c2s`.

## Compatibility

- If the client does not have LootLock installed, `ServerPlayNetworking.canSend` is false for the LootLock channels.
- In that case no handshake packets are sent, and server-side command and pickup behavior continue normally.

## Snapshot payload

`sync_player_data_s2c` contains:

- schema version
- player UUID
- revision
- active profile ID
- full profile list and rules
- client edit capability flag
