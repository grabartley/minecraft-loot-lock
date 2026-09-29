# ADR 0001: Pickup filtering runs on the server, and clients only send intent

- **Status:** Accepted
- **Date:** 2026-09-29

## Context

Loot Lock decides, per player, whether a dropped item is picked up, left on the ground, or deleted. That decision has to hold on a dedicated server with many players, some running the mod and some on vanilla clients.

The client could filter. It knows the player's rules and it sees the item entities. But vanilla pickup is decided by the server when a player collides with an item entity, so a client-side filter could only avoid walking into drops, never refuse one. It would also let a modified client lie about its rules, and it would do nothing for players without the mod.

The rules themselves also have to be edited somewhere. If the client edits its own copy and reports the result, the server has to trust whatever arrives: a spoofed profile id, a thousand-character name, delete mode on a server whose operator turned it off.

## Decision

The server makes every pickup decision. `ItemEntityMixin` injects at the head of `ItemEntity.onPlayerCollision`, only for server players, and asks `PickupGuard` for a `PickupDecision`: allow, reject and leave, or reject and delete. Anything but allow cancels the vanilla pickup, and delete also discards the item entity.

`PickupGuard` reads the player's active profile from the server's own store. No profile, or a disabled one, always allows. If the server policy forbids deleting rejected items, a delete decision is downgraded to leave at decision time, whatever the profile says.

Clients send intent, and the server resolves it. Every profile and enable edit from the panel is a packet carrying the revision the client based its edit on. The server policy packet carries no revision and is gated on operator permission instead. The server:

- rejects a profile or enable edit if its revision is stale;
- keeps the profile id from its own record rather than the payload;
- enforces the limits in `PacketLimits` on names, rule counts, and rule ids, and drops rule ids that do not parse;
- downgrades delete to leave when the policy forbids it;
- requires permission level 2 for the server policy packet;
- and answers every edit, accepted or rejected, with an authoritative sync of the player's data.

Blocked pickups are reported back to the player. Notices are accumulated per player and item and sent at most once every 40 ticks, as a packet the client mod renders as a toast, or as an action bar message for a client without the mod.

## Consequences

A modified client cannot pick up what its owner's rules reject, and cannot pick up anything differently from what the server stored. Two players over the same drops each get their own outcome, which the gametests check.

**Vanilla clients are filtered too.** Nothing about filtering needs the client mod, so an operator can manage a vanilla player's profiles with `/lootlock player <target>` and the rules apply on the next pickup.

The operator's delete policy cannot be bypassed. Even a profile that was stored with delete mode before the policy changed, or that arrived through a path that does not check the policy, only ever leaves items on the ground while delete is forbidden.

The panel stays correct under concurrent edits. A command and the panel, or two panels, editing the same player cannot silently overwrite each other: the second edit is stale, is rejected, and the client resyncs.

Accepted drawbacks: the mod must be installed on the server for anything to happen, and the panel shows a change only after the server's sync arrives. A client-side filter would have worked on servers without the mod, but it could never have refused a pickup, so it would not have been this mod.
