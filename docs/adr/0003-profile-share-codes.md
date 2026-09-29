# ADR 0003: Profiles travel as versioned, compressed share codes

- **Status:** Accepted
- **Date:** 2026-09-29

## Context

Players want to reuse a profile on another world or server, and to share one with other players. Profiles are stored per world (see [ADR 0002](0002-per-player-json-store.md)), so they need a way to leave the world.

The format has to survive being pasted into chat, Discord, Reddit, or a modpack readme, and small profiles should fit in a chat command, which vanilla caps at 256 characters. And the decoder receives untrusted text from strangers, so it has to be safe against anything a hostile code can contain.

A file export would need the player to reach the server's disk. Raw JSON is long, full of characters chat and markdown mangle, and has no marker saying what it is.

## Decision

A profile is exported as a share code: the prefix `ll1.` followed by URL-safe Base64, without padding, of raw DEFLATE-compressed JSON. The JSON holds a format version `v` of `1`, the profile name, the filter mode, the rejected-item action, and the rule list in order. Rules are item ids, or tag ids prefixed with `#`.

Only what describes the filter travels. The profile id, colour, and enabled state stay behind. Import mints a fresh id and creates the profile enabled.

`ProfileShareCodec` lives in the shared source set, so the `/lootlock profile export` and `import` commands and the panel's export button and import prompt use the same code. Decoding checks, in order:

- the code is at most 4096 characters and starts with `ll1.`;
- the Base64 and DEFLATE layers are valid, and the decompressed payload stays under 256 KiB;
- the JSON parses and `v` reads as the version `1`;
- the name is 1 to 32 characters, and the mode and action are known values;
- there are at most 1024 rules, and every rule is a parseable id of at most 256 characters.

A failure returns a reason, which maps to a translated error message, and nothing is created from a code that fails any check. The version check is looser than intended: Gson's `getAsInt` accepts `"1"` and `1.5`, and throws instead of returning a reason for a non-numeric `v`. That is tracked in #185, and this record describes the intended contract once it is fixed.

The panel's import path then goes through the normal create packet, so the server applies its own limits and revision checks on top (see [ADR 0001](0001-pickup-filtering-runs-on-the-server.md)).

## Consequences

Codes paste cleanly anywhere, because the URL-safe Base64 alphabet has no characters that chat or markdown treat specially. Compression keeps small profiles short enough for `/lootlock profile import`. A profile with many rules can produce a code longer than chat accepts, and the panel's import prompt takes codes up to the full 4096 characters for that case.

A hostile code cannot exhaust memory or smuggle bad data in. The length cap bounds the input, the decompression cap stops a small code expanding into a huge payload, and every field is validated before a profile exists.

The prefix makes a code recognisable at a glance and lets the decoder fail fast on text that is not a code.

Codes work on vanilla clients through the commands, and between worlds and servers, because they carry nothing tied to a particular world.

Changing the format is deliberate. The mode and action are stored by enum constant name, so renaming `FilterMode` or `RejectedItemAction` constants breaks every existing code. A new format needs a new version number and prefix, and a decoder that keeps accepting `ll1.`.

Codes are public by nature. Anyone holding one can read the rules inside it, which the README tells players.
