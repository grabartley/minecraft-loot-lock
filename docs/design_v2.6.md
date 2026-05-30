# LootLock Developer Design Document v2.6

**Status:** Architecture Locked – Pending Technical Validation
**Version:** 2.6 (merge of v2 implementation detail + v2.5 architecture structure)
**Minecraft Version:** Java Edition 1.20.1
**Loader / Framework:** Fabric Loader + Fabric API
**Mappings:** Yarn 1.20.1
**Primary Language:** Java
**Document Owner:** LootLock Project
**Document Purpose:** Implementation blueprint and authoritative project specification.

> This document supersedes v2.5 and v2. It is the authoritative project specification until the open research gaps (Section 31) are resolved and a final v3 architecture document is produced.

---

## 1. Executive Summary

LootLock is a server-authoritative utility mod for Minecraft that allows players to control which dropped items may enter their inventory. It solves inventory pollution through configurable, per-player pickup filtering rules.

Examples of intended use:

- Ignore wheat seeds while clearing grass.
- Ignore eggs around chicken farms.
- Ignore rotten flesh from mob farms.
- Ignore cobblestone while mining.
- Ignore netherrack during Nether excavation.
- Collect only diamonds.
- Collect only gunpowder.

LootLock supports singleplayer, LAN worlds, and dedicated Fabric servers, with both vanilla and modded items. The system is designed around personal player preferences — each player owns their own filtering configuration.

---

## 2. Product Goals

### Primary Goal
Prevent unwanted items from entering player inventories.

### Secondary Goals
- Fast configuration.
- Minimal performance impact.
- Multiplayer compatibility.
- Safe defaults.
- Intuitive user experience.
- Strong mod compatibility.

### Success Criteria
A player should be able to:

1. Install LootLock.
2. Create a profile.
3. Add a rule.
4. Observe filtering behavior.

**Target setup time:** Less than 60 seconds.

### v1 Feature Scope
LootLock v1 should include as much functionality as is safe and realistic:

- Denylist mode.
- Allowlist mode.
- Multiple profiles.
- Active profile switching.
- Searchable item rule UI.
- Keybind to open UI.
- Keybind to cycle active profile.
- Optional rejected-item deletion.
- Modded item support via registry IDs.
- Per-player multiplayer behavior.
- Server-authoritative pickup prevention.
- Client UI for comfortable editing when client + server both have the mod.
- Server commands as fallback when no client UI is available.
- Import/export JSON from client-side UI.
- Versioned config schema for future migrations.

---

## 3. Non-Goals

LootLock is **not**:

- An inventory sorter.
- A storage network.
- A logistics framework.
- A hopper filter.
- An automation system.
- A chest management mod.
- A data-component aware inventory management tool (v1).

---

## 4. Engineering Principles

### 4.1 Server authority first
Minecraft multiplayer inventory state is server-authoritative. LootLock must not rely on client prediction to prevent inventory mutation. The actual pickup decision must happen on the logical server.

### 4.2 Client UI is convenience, not authority
The client may present menus, keybinds, HUD messages, and local draft editing. The server owns the rules that are applied to real pickup logic.

### 4.3 Common data model
Rule models, profile models, evaluators, serializers, and packet DTOs must live in common code so both physical client and dedicated server can compile safely.

### 4.4 No accidental deletion by default
Rejected items remain on the ground unless the user explicitly enables deletion for the active profile.

### 4.5 O(1) rule checks
Rules must evaluate using hash-based lookup. A player with thousands of filtered items should not cause pickup lag.

### 4.6 Registry ID based compatibility
Rules store item registry identifiers such as `minecraft:wheat_seeds`, not translated names and not live `Item` object references.

---

## 5. Target Runtime Sides

LootLock is a hybrid mod.

### 5.1 Required on server for reliable multiplayer
For dedicated servers, LootLock must be installed server-side to actually block item pickup.

### 5.2 Recommended on client
The client install provides UI, keybinds, profile editing, import/export, and nicer UX.

### 5.3 Supported deployment modes

| Mode | Supported | Behavior |
|---|---|---|
| Singleplayer (integrated server) + client installed | Yes | Full feature set. |
| Dedicated Fabric server + client installed | Yes | Full feature set with packet sync. |
| Dedicated Fabric server only | Yes | Command-only management. No GUI. |
| Client only joining vanilla/non-LootLock server | Limited / No | Cannot reliably block server-side pickups. UI shows unsupported. |
| LAN with both sides installed | Yes | Same as integrated/dedicated hybrid. |

---

## 6. Architecture Overview

### Client Responsibilities
- Screens.
- Keybindings.
- HUD notifications.
- ModMenu integration.
- Draft editing.
- Import/export UX.

Client code does **not** decide pickup outcomes.

### Shared Responsibilities
- Data models.
- Rule models.
- Packet DTOs.
- Serialization logic.
- Validation logic.

### Server Responsibilities
- Pickup decisions.
- Profile persistence.
- Rule evaluation.
- Networking authority.
- Security validation.
- Commands.

**Server state is authoritative.**

---

## 7. Architecture Decision Records

### ADR-001 — Server-authoritative pickup filtering
**Decision:** Server authoritative pickup filtering.
**Reasoning:** Minecraft inventory state is server authoritative. Client-only cancellation cannot reliably prevent the server from inserting items into the authoritative inventory.
**Status:** Accepted.

### ADR-002 — Registry ID storage
**Decision:** Rules are stored using registry IDs (e.g. `minecraft:wheat_seeds`, `minecraft:egg`, `create:crushed_raw_zinc`).
**Reasoning:** Stable persistence and mod compatibility.
**Status:** Accepted.

### ADR-003 — Profiles in v1
**Decision:** Profiles included in v1 (examples: Mining, Farming, Nether, Building, Mob Farm).
**Reasoning:** Profiles are central to intended gameplay.
**Status:** Accepted.

### ADR-004 — Exact item rules only in v1
**Decision:** Exact item rules only. Tag support deferred.
**Reasoning:** Reduce implementation complexity.
**Status:** Accepted.

### ADR-005 — Hybrid client/server architecture
**Decision:** Hybrid client/server architecture.
**Reasoning:** Server controls gameplay; client improves UX.
**Status:** Accepted.

### ADR-006 — Primary hook is `ItemEntity#onPlayerCollision`
**Decision:** Use `ItemEntity#onPlayerCollision(PlayerEntity)` via Mixin as the primary interception point.
**Reasoning:** It is semantically closest to "dropped item pickup", matching the product promise. `PlayerInventory.insertStack` would be broader and catch non-pickup paths.
**Status:** Accepted, PoC-001 singleplayer hook lifecycle validated, PoC-002 delete-mode safety still pending.

### ADR-007 — PlayerInventory guard is not active by default
**Decision:** Secondary `PlayerInventory#insertStack` guard is implemented behind an internal flag and disabled by default in v1.
**Reasoning:** May unintentionally affect container, command, crafting, and cross-mod insertion paths.
**Status:** Accepted.

### ADR-008 — Profiles are per-player
**Decision:** Profiles are stored and evaluated per-player.
**Reasoning:** LootLock is personal utility behavior, not a global loot rule.
**Status:** Accepted.

### ADR-009 — Delete rejected items is per-profile and off by default
**Decision:** Rejected-item deletion is a per-profile setting, defaulting to `LEAVE_ON_GROUND`, and may be disabled globally by server policy.
**Reasoning:** Safety. Prevents accidental destruction of death drops and important items.
**Status:** Accepted.

---

## 8. Fabric / Minecraft Technical Findings

### 8.1 Networking
For Fabric API around 1.20, `ServerPlayNetworking` is server-side play-stage networking and includes packet receive/send capabilities. It is logical-server only. `ClientPlayNetworking` is physical/logical-client only.

For 1.20.1, use the Fabric networking API appropriate to the selected Fabric API version. In API docs for the 0.81 / 0.82 era, both classic `Identifier` + `PacketByteBuf` style and packet object-based APIs are present. The implementation should choose one style and keep it consistent across all packets.

**Recommendation for 1.20.1:** use Fabric's packet object API if available in the selected dependency; otherwise use classic channels with `Identifier` and `PacketByteBuf`.

### 8.2 Inventory insertion
Yarn 1.20.1 exposes `PlayerInventory.insertStack(ItemStack)` and `PlayerInventory.insertStack(int, ItemStack)`. These are defensive guard candidates, but they are broad hooks and may catch insertions from more than ground item pickup.

### 8.3 Dropped item pickup
The primary desired hook is dropped-item collision with player, likely `ItemEntity#onPlayerCollision(PlayerEntity)`. This is semantically closer to "ground item pickup" than inventory insertion. It should be the primary interception point.

PoC-001 singleplayer validation confirms the hook is invoked on the logical server and can fire repeatedly for the same `ItemEntity` across consecutive ticks before final pickup resolution.

### 8.4 Existing mod landscape
Existing pickup filter mods confirm the feature demand:

- **FiltPick** exposes blacklist/whitelist and destruction modes.
- **Pickup Filter** for Minecraft 1.20–1.20.1 is described as fully server-side, which supports the architecture choice that reliable pickup filtering belongs on the server.

---

## 9. Source Set Layout

Fabric template source sets:

- `src/main/java` — common + server-safe code. **Must not** reference `net.minecraft.client.*` classes.
- `src/client/java` — client-only code. Screens, keybindings, HUD, ModMenu integration.
- `src/main/resources` — `fabric.mod.json`, mixin config, lang, icons.
- `src/client/resources` — client-only assets if separated by build setup.

### `fabric.mod.json`

```json
{
  "schemaVersion": 1,
  "id": "lootlock",
  "version": "${version}",
  "name": "LootLock",
  "description": "Configurable item pickup filtering for Minecraft.",
  "environment": "*",
  "entrypoints": {
    "main": ["com.lootlock.LootLock"],
    "client": ["com.lootlock.client.LootLockClient"]
  },
  "mixins": [
    "lootlock.mixins.json"
  ],
  "depends": {
    "fabricloader": ">=0.14.21",
    "minecraft": "1.20.1",
    "fabric-api": "*"
  },
  "suggests": {
    "modmenu": "*"
  }
}
```

---

## 10. Package Structure

This structure is **architecture-locked**.

```
com.lootlock
├── LootLock.java
├── LootLockConstants.java
│
├── api
│   ├── LootLockApi.java
│   └── PickupDecision.java
│
├── command
│   └── LootLockCommand.java
│
├── config
│   ├── LootLockConfig.java
│   ├── ConfigManager.java
│   ├── ConfigPaths.java
│   ├── ConfigSerializer.java
│   ├── ConfigMigration.java
│   └── ConfigValidationResult.java
│
├── data
│   ├── FilterMode.java
│   ├── RejectedItemAction.java
│   ├── LootLockProfile.java
│   ├── LootLockPlayerData.java
│   ├── RuleSet.java
│   └── RuleEntry.java
│
├── rules
│   ├── RuleEngine.java
│   ├── RuleContext.java
│   ├── RuleMatchResult.java
│   └── ItemRuleMatcher.java
│
├── server
│   ├── ServerPlayerDataManager.java
│   ├── ServerLifecycleHooks.java
│   ├── ServerConfigStore.java
│   └── PickupGuard.java
│
├── network
│   ├── LootLockNetworking.java
│   ├── PacketIds.java
│   ├── ClientToServerPackets.java
│   ├── ServerToClientPackets.java
│   └── (codec / buf helpers, payload classes)
│
├── mixin
│   ├── ItemEntityMixin.java
│   └── PlayerInventoryMixin.java
│
└── util
    ├── IdentifierUtil.java
    ├── RegistryUtil.java
    ├── TextUtil.java
    └── HashUtil.java

com.lootlock.client
├── LootLockClient.java
├── keybind
│   └── LootLockKeybinds.java
├── screen
│   ├── LootLockMainScreen.java
│   ├── ProfileListScreen.java
│   ├── ProfileEditScreen.java
│   ├── RuleListScreen.java
│   ├── ItemSearchScreen.java
│   ├── SettingsScreen.java
│   └── ConfirmActionScreen.java
├── state
│   ├── ClientLootLockState.java
│   └── ClientDraftProfile.java
├── hud
│   └── LootLockHud.java
└── modmenu
    └── LootLockModMenuIntegration.java
```

---

## 11. Runtime Lifecycle

### Server Startup
1. Load policy configuration.
2. Load player state store.
3. Register commands.
4. Register networking.
5. Register lifecycle hooks.

### Player Join
1. Load player data.
2. Create defaults if absent.
3. Compile rule sets.
4. Synchronize state to client (if channel supported).

### Player Disconnect
1. Save dirty state.
2. Remove caches.

### Server Shutdown
1. Flush all dirty data.
2. Persist state.

### Client Initialization
1. Register client packet receivers.
2. Register keybinds.
3. Register HUD.

---

## 12. Data Model

### 12.1 FilterMode

```java
public enum FilterMode {
    DENYLIST,
    ALLOWLIST
}
```

Behavior:
- `DENYLIST`: rule match means reject item.
- `ALLOWLIST`: rule match means accept item; non-match means reject item.

### 12.2 RejectedItemAction

```java
public enum RejectedItemAction {
    LEAVE_ON_GROUND,
    DELETE
}
```

**Default:** `LEAVE_ON_GROUND`.

### 12.3 RuleEntry

v1 only supports exact item ID rules.

```java
public record RuleEntry(
    String itemId
) {}
```

Validation:
- Must parse as `Identifier`.
- Must exist in item registry when possible.
- Unknown IDs may be retained but marked unresolved in UI. This protects configs when mods are temporarily missing.

### 12.4 RuleSet

```java
public final class RuleSet {
    private final Set<Identifier> itemIds;

    public boolean contains(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        return itemIds.contains(id);
    }
}
```

Rules must be compiled after loading profile data. **Do not parse strings during every pickup event.**

### 12.5 LootLockProfile

```java
public final class LootLockProfile {
    private UUID id;
    private String name;
    private FilterMode mode;
    private RejectedItemAction rejectedItemAction;
    private boolean enabled;
    private List<RuleEntry> rules;

    // runtime compiled:
    private transient RuleSet compiledRuleSet;
}
```

Profile defaults:
- `name = "Default"`
- `mode = DENYLIST`
- `rejectedItemAction = LEAVE_ON_GROUND`
- `enabled = true`
- `rules = []`

Empty rule behavior:

| Mode | Empty Rules Result |
|---|---|
| Denylist | Accept everything. |
| Allowlist | Reject everything. |

Because empty allowlist can lock pickup entirely, UI must display a warning.

### 12.6 LootLockPlayerData

```java
public final class LootLockPlayerData {
    private int schemaVersion;
    private UUID playerUuid;
    private UUID activeProfileId;
    private List<LootLockProfile> profiles;
    private boolean clientCanEdit;
    private long revision;
}
```

`revision` increments whenever server-side data changes. Client packets must include the revision they were based on to detect stale edits.

---

## 13. Config and Storage Design

### 13.1 Server-side storage is authoritative
In multiplayer, player LootLock data must be stored on the server.

**Recommended path:**
```
<world>/lootlock/players/<player-uuid>.json
```

**Alternative:**
```
<world>/data/lootlock/players/<player-uuid>.json
```

Avoid global `.minecraft/config` as the only authoritative store because multiplayer rules are world/server-specific.

### 13.2 Client-side storage
Client stores:
```
.minecraft/config/lootlock/client.json
.minecraft/config/lootlock/exports/*.json
```

Client config contains UI preferences only:

```json
{
  "schemaVersion": 1,
  "openScreenKey": "key.keyboard.o",
  "cycleProfileKey": "key.keyboard.p",
  "showBlockedToast": true,
  "showHudIndicator": true,
  "lastOpenedProfileId": "..."
}
```

### 13.3 Player data JSON schema

```json
{
  "schemaVersion": 1,
  "playerUuid": "00000000-0000-0000-0000-000000000000",
  "activeProfileId": "11111111-1111-1111-1111-111111111111",
  "revision": 12,
  "profiles": [
    {
      "id": "11111111-1111-1111-1111-111111111111",
      "name": "Mining",
      "mode": "DENYLIST",
      "rejectedItemAction": "LEAVE_ON_GROUND",
      "enabled": true,
      "rules": [
        {"itemId": "minecraft:cobblestone"},
        {"itemId": "minecraft:granite"},
        {"itemId": "minecraft:diorite"},
        {"itemId": "minecraft:andesite"}
      ]
    }
  ]
}
```

### 13.4 Migration policy
Every persisted file must include `schemaVersion`.

Migration rules:
- **Unknown newer schema:** fail gracefully, back up file, create default profile.
- **Older schema:** migrate in memory, save new version.
- **Corrupt JSON:** rename to `.broken.<timestamp>.json`, create default profile, notify user / server log.

### 13.5 Save timing
Save player data:
- On player disconnect.
- On profile / rule update.
- On server stopping.
- Debounced after bursts of UI edits.

**Debounce recommendation:** mark dirty immediately; flush to disk after 2 seconds or on disconnect.

### 13.6 Persistence Strategy (v2.5 update)
**Preferred Architecture:** `PersistentState`.
**Purpose:** Authoritative server storage.
**Potential Alternative:** World-scoped JSON (as detailed above).
**Current Status:** Provisional Architecture Decision — `PersistentState` proof-of-concept has not yet been completed (see RG-003 / PoC-003).

---

## 14. Rule Engine

### 14.1 Public contract

```java
public interface RuleEngine {
    PickupDecision evaluate(ServerPlayerEntity player, ItemStack stack, RuleContext context);
}

public enum PickupDecision {
    ALLOW,
    REJECT_LEAVE,
    REJECT_DELETE
}
```

### 14.2 RuleContext

```java
public record RuleContext(
    World world,
    @Nullable ItemEntity sourceEntity,
    PickupSource source
) {}

public enum PickupSource {
    ITEM_ENTITY_COLLISION,
    INVENTORY_INSERT_GUARD,
    COMMAND_TEST,
    FUTURE_UNKNOWN
}
```

### 14.3 Evaluation algorithm

```
LootLockPlayerData data = playerDataManager.get(player);
LootLockProfile profile = data.getActiveProfile();

if profile == null:
    return ALLOW
if !profile.enabled:
    return ALLOW

Identifier itemId = Registries.ITEM.getId(stack.getItem());
boolean matched = profile.compiledRuleSet().contains(itemId);

boolean reject = switch(profile.mode()) {
    case DENYLIST  -> matched;
    case ALLOWLIST -> !matched;
};

if !reject:
    return ALLOW

return profile.rejectedItemAction() == DELETE
    ? REJECT_DELETE
    : REJECT_LEAVE;
```

### 14.4 Performance
Per pickup event:
- One player UUID lookup.
- One active profile lookup.
- One registry ID fetch.
- One `HashSet.contains` call.

**No JSON parsing, no registry iteration, no translated name matching, no NBT scan.**

**Complexity requirement:** O(1). All rule sets must be compiled into `HashSet` structures.

### 14.5 Future extensibility
v1 exact item ID rules should be designed so v2 can add:

- Tag rules: `#minecraft:seeds`.
- NBT / component-sensitive rules.
- Stack count conditions.
- Source conditions.
- Dimension conditions.
- Temporary session rules.

Suggested future model:

```java
sealed interface RuleEntry permits ItemIdRule, TagRule, NbtRule
```

Do not implement this in v1 unless time allows. Do design package boundaries around it.

---

## 15. Profile System

Each player owns profiles.

### Example profiles

**Mining** (Denylist):
- `minecraft:cobblestone`
- `minecraft:granite`
- `minecraft:andesite`

**Farming** (Denylist):
- `minecraft:wheat_seeds`
- `minecraft:egg`

**Mob Farm** (Allowlist):
- `minecraft:gunpowder`

**Nether** (Denylist):
- `minecraft:netherrack`

### Default Profile
- **Name:** Default
- **Mode:** DENYLIST
- **Enabled:** True
- **Rules:** Empty

---

## 16. Pickup Interception Strategy

### 16.1 Primary Mixin: ItemEntityMixin
**Goal:** block dropped item pickup before inventory mutation.

**Target:** `net.minecraft.entity.ItemEntity#onPlayerCollision(PlayerEntity player)`

**Implementation:** Mixin injection, server-side only.

```java
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(
        method = "onPlayerCollision",
        at = @At("HEAD"),
        cancellable = true
    )
    private void lootlock$beforePickup(PlayerEntity player, CallbackInfo ci) {
        if (player.getWorld().isClient()) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        ItemEntity itemEntity = (ItemEntity)(Object)this;
        ItemStack stack = itemEntity.getStack();

        PickupDecision decision = LootLockServer.PICKUP_GUARD.evaluate(
            serverPlayer,
            stack,
            itemEntity
        );

        if (decision == PickupDecision.ALLOW) return;

        if (decision == PickupDecision.REJECT_DELETE) {
            itemEntity.discard();
            LootLockServer.PICKUP_GUARD.notifyBlocked(serverPlayer, stack, true);
        } else {
            // Leave entity in world; cancel vanilla pickup.
            LootLockServer.PICKUP_GUARD.notifyBlocked(serverPlayer, stack, false);
        }

        ci.cancel();
    }
}
```

**Important notes:**
- Must only run on logical server.
- Must not call client-only classes.
- Must not mutate stack when leaving on ground.
- Delete mode should remove the item entity authoritatively.
- For partial pickup behavior, v1 treats the whole `ItemEntity` stack as blocked or allowed. **No partial stack splitting in v1.**

### 16.2 Secondary Mixin: PlayerInventoryMixin
**Goal:** optional defensive guard to prevent accidental insertion paths.

**Target candidates:**
- `PlayerInventory#insertStack(ItemStack)`
- `PlayerInventory#insertStack(int, ItemStack)`

**Risk:** This hook may affect inventory insertions from containers, commands, crafting, item returns, or other mods. Because LootLock's v1 promise is ground item pickup filtering, this guard should be disabled by default or scoped.

**Recommendation:**
- Implement behind internal config flag: `enableInventoryInsertGuard = false` for v1 release.
- Use only during testing to understand extra insertion paths.
- Do not ship active unless proven safe.

**Safer v1 release:** Primary hook only — `ItemEntity#onPlayerCollision`. Commands and docs state LootLock filters dropped item pickup, not all inventory insertions.

### 16.3 Why not client-only?
Client-only cancellation can visually reduce pickup attempts but cannot reliably prevent a server from inserting items into the authoritative inventory. For multiplayer, client-only prevention would desync or fail. It can be a future best-effort mode, but not the main implementation.

### 16.4 Current Status
Architecture accepted. **Technical validation still required** — see Research Gaps RG-001 / RG-002 and PoC Gates PoC-001 / PoC-002.

---

## 17. Rejected Item Behavior

### 17.1 Leave on ground (default)
Behavior:
- Cancel pickup.
- Item entity remains.
- Player may continue colliding with it.
- To avoid message spam, apply notification cooldown.

**Possible issue:** the item may repeatedly attempt pickup every tick while player stands on it. Rule evaluation is cheap, but notifications must be throttled.

### 17.2 Delete (advanced)
Behavior:
- Cancel pickup.
- Remove entire item entity from world.
- Send optional blocked / deleted notification.

**Safety requirements:**
- UI warning when enabling.
- Per-profile setting.
- Disabled by default.
- Command requires confirmation or explicit argument.

**Command example:**
```
/lootlock profile set rejectedAction delete confirm
```

### 17.3 Notification throttling
Server should track:
```java
Map<UUID, BlockedNotificationAccumulator>
```

Accumulator groups blocked items for a short window:
```
Blocked 16x Wheat Seeds
Deleted 4x Egg
```

**Cooldown:** 1 message per player per 2 seconds. Client HUD can display prettier toasts when available.

---

## 18. Networking Design

### 18.1 Channel naming
Use namespace `lootlock`.

```
lootlock:hello_c2s
lootlock:hello_s2c
lootlock:sync_player_data_s2c
lootlock:update_profile_c2s
lootlock:activate_profile_c2s
lootlock:delete_profile_c2s
lootlock:create_profile_c2s
lootlock:request_sync_c2s
lootlock:blocked_notice_s2c
lootlock:server_capabilities_s2c
```

### 18.2 Compatibility handshake
On join:

1. Server detects client channel support if API supports it.
2. Server sends capabilities.
3. Client sends hello with mod version and supported schema.
4. Server sends authoritative player data snapshot.
5. Client stores snapshot in `ClientLootLockState`.

If client lacks mod:
- Server still applies existing player data.
- Player uses commands only.

### 18.3 Packet style for 1.20.1
Implementation should support the selected Fabric API version. Recommended:
- Prefer packet object-based API if available.
- Otherwise classic `Identifier` + `PacketByteBuf`.

For stability in 1.20.1, define explicit read/write helpers:

```java
public interface LootLockPacket {
    Identifier id();
    void write(PacketByteBuf buf);
}
```

### 18.4 Packet specifications

#### SyncPlayerDataS2CPacket
**Purpose:** Server sends full authoritative state to client.

**Fields:**
- `int schemaVersion`
- `UUID playerUuid`
- `long revision`
- `UUID activeProfileId`
- `List<ProfileDto> profiles`
- `boolean clientCanEdit`
- `ServerPolicyDto policy`

**When sent:**
- On login.
- After successful update.
- After admin command affecting player.
- On client request sync.

#### UpdateProfileC2SPacket
**Purpose:** Client sends edited profile.

**Fields:**
- `long baseRevision`
- `ProfileDto profile`

**Server behavior:**
1. Verify sender is allowed to edit.
2. Verify `baseRevision` is current or mergeable.
3. Validate profile.
4. Save.
5. Increment revision.
6. Send `SyncPlayerDataS2C`.

**Conflict behavior:**
- If stale revision: reject and re-sync current server data.
- UI shows "Profile changed on server; your edit was not applied."

#### ActivateProfileC2SPacket
**Fields:**
- `long baseRevision`
- `UUID profileId`

**Server behavior:**
- Verify profile exists.
- Set active profile.
- Increment revision.
- Save.
- Sync.

#### CreateProfileC2SPacket
**Fields:**
- `long baseRevision`
- `String name`
- `Optional<ProfileDto> copyFrom`

**Server behavior:**
- Sanitize name.
- Assign new UUID server-side.
- Add profile.
- Optionally activate it.
- Save and sync.

#### DeleteProfileC2SPacket
**Fields:**
- `long baseRevision`
- `UUID profileId`

**Rules:**
- Cannot delete last profile.
- If active profile deleted, activate first remaining profile.
- Save and sync.

#### BlockedNoticeS2CPacket
**Fields:**
- `Identifier itemId`
- `int count`
- `boolean deleted`

Used for client HUD / toasts. If client not installed, server can use chat / actionbar fallback.

### 18.5 Revision-based synchronization
Revision-based synchronization is required. Stale updates are rejected.

---

## 19. Commands

Commands are required for server-only installs and debugging.

**Root:** `/lootlock`

### Player commands
```
/lootlock status
/lootlock enable
/lootlock disable
/lootlock profile list
/lootlock profile create <name>
/lootlock profile delete <name>
/lootlock profile activate <name>
/lootlock mode denylist
/lootlock mode allowlist
/lootlock action leave
/lootlock action delete confirm
/lootlock rule add <item>
/lootlock rule remove <item>
/lootlock rule list
/lootlock rule clear confirm
/lootlock export
/lootlock import <json?>
```

### Admin commands
```
/lootlock admin <player> status
/lootlock admin <player> profile list
/lootlock admin <player> lock true|false
/lootlock admin reload
/lootlock admin save
```

v1 can skip import via command if too awkward, but `export` / `status` / `list` / `add` / `remove` should exist.

### Brigadier argument types
- Use item identifier argument where possible.
- Accept `minecraft:wheat_seeds`.
- Resolve registry entry server-side.

---

## 20. Client UI Design

### 20.1 Entry
**Default keybind:** `O` = Open LootLock
**Optional:** `P` = Cycle next profile

Keybinds are client-only. Register in client initializer.

### 20.2 Server support states

| State | UI behavior |
|---|---|
| Integrated server | Full editor. |
| Dedicated server with LootLock | Full editor after sync. |
| Dedicated server without LootLock | Read-only error screen. |
| Not in world | Local export / import manager only (optional). |

### 20.3 Main screen
**Fields:**
- Title: LootLock
- Server: Supported / Unsupported
- Active profile: dropdown
- Mode: Denylist / Allowlist
- Action: Leave / Delete
- Rules: count
- Enabled: toggle

**Buttons:**
- Edit Rules
- Profiles
- Settings
- Import / Export
- Done

### 20.4 Rule list screen
**Elements:**
- Search field.
- Current rule list.
- Remove selected rule.
- Add item button.
- Clear all button with confirmation.
- Mode warning when allowlist has zero rules.

**Search behavior:**
- Search current registry items.
- Match registry ID.
- Match translated display name client-side.
- Display unresolved stored IDs separately.

### 20.5 Item search screen
**Data source:** `Registries.ITEM`

For each item:
- Icon.
- Display name.
- Registry ID.
- Mod namespace.

**Actions:**
- Add item.
- Remove item if already present.
- Prevent duplicates.

### 20.6 Profiles screen
**Actions:**
- Create profile.
- Rename profile.
- Duplicate profile.
- Delete profile.
- Activate profile.

**Rules:**
- Cannot delete final profile.
- Profile names max 32 characters.
- Duplicate names allowed internally but discouraged; UI can append number.

### 20.7 Settings screen
**Settings:**
- Show blocked HUD notification.
- Show actionbar fallback.
- Confirm before enabling delete.
- Enable profile-cycle toast.
- Client UI scale / layout preference.

### 20.8 UI save model
Use **draft editing**:

1. Client receives authoritative state.
2. UI creates mutable draft.
3. User edits draft.
4. Press Save.
5. Client sends packet.
6. Server validates.
7. Server sends authoritative state.
8. UI refreshes.

**Avoid sending a packet for every keystroke.**

---

## 21. Client State Model

```java
public final class ClientLootLockState {
    private boolean serverSupportsLootLock;
    private boolean synced;
    private LootLockPlayerData snapshot;
    private ServerPolicyDto policy;
}
```

**On disconnect:** `clear()`

**On login:**
- `serverSupportsLootLock = false`
- `synced = false`
- Request sync if channel available.

---

## 22. Server Policy

Server administrators control:

```json
{
  "schemaVersion": 1,
  "allowClientEditing": true,
  "allowDeleteRejectedItems": true,
  "maxProfilesPerPlayer": 16,
  "maxRulesPerProfile": 4096,
  "enableCommands": true,
  "sendBlockedNotices": true,
  "saveDebounceTicks": 40
}
```

**Policy effects:**
- If `allowClientEditing = false`, UI becomes read-only.
- If `allowDeleteRejectedItems = false`, delete option is hidden / disabled.
- Limits prevent griefy or accidental huge packets.

**Policy is authoritative. Clients cannot override server policy.**

---

## 23. Security & Abuse Prevention

### 23.1 Threats
- Packet spam.
- Oversized profiles.
- Invalid identifiers.
- Unauthorized edits.
- Malicious clients.

### 23.2 Server validation
- Maximum profiles.
- Maximum rules.
- Maximum name lengths.
- Revision validation.
- Identifier validation.

### 23.3 Server never trusts
- Client UUID.
- Client permissions.
- Client policy flags.
- Client ownership claims.

### 23.4 Cooldowns
- Profile switching: **250 ms**
- Enable / Disable: **250 ms**
- Save debounce: **2 seconds**

### 23.5 Threat model — potential abuse cases
- Client sends 1000 profiles.
- Client sends 100,000 rules.
- Client sends invalid item identifiers.
- Client sends stale revisions.
- Client attempts profile ownership spoofing.

**Mitigations:**
- Hard limits.
- Validation.
- Ownership checks.
- Revision checks.
- Packet throttling.

---

## 24. Emergency Recovery

**Required v1 feature.**

### Commands
```
/lootlock enable
/lootlock disable
```

### Client Keybind
Toggle LootLock.

### Purpose
- Recover death drops.
- Recover from allowlist mistakes.
- Recover from bad profiles.

---

## 25. Multiplayer Behavior

Profiles are player-specific.

**Example:**
- Player A: ignores seeds.
- Player B: collects seeds.

Both behaviors operate independently. **All evaluations occur server-side.**

---

## 26. Performance Requirements

| Metric | Target |
|---|---|
| Startup overhead | < 1 second |
| Memory | < 10 MB |
| Rule evaluation | O(1) |
| Disk writes | Debounced |
| Network traffic | Minimal |
| TPS impact | No measurable impact under normal usage |

---

## 27. Compatibility Requirements

### Must support
- Fabric API
- ModMenu
- Vanilla items
- Modded items

### Must not break
- REI
- EMI
- Inventory sorting mods
- Dedicated servers

---

## 28. Error Handling

### 28.1 Invalid item ID
On load:
- Keep raw rule.
- Mark unresolved.
- Do not crash.
- Do not match anything while unresolved.

UI:
```
Unknown item: oldmod:removed_item
```
Buttons: **Remove** / **Keep**.

### 28.2 Corrupt profile
If a single profile is corrupt:
- Skip profile.
- Log warning.
- If no profiles remain, create default.

### 28.3 Packet validation failure
Server rejects packet and sends:
- `SyncPlayerDataS2C` current state.
- Optional error toast / actionbar.

**Never trust client-provided:**
- Player UUID.
- Revision as authoritative.
- Profile count above server max.
- Rule count above server max.
- Delete action when server policy forbids it.

### 28.4 Item deletion safety
Delete action should **never** delete:
- Items not rejected by active profile.
- Items picked up by other players unless their own rule rejects them.
- Non-item entities.

---

## 29. Edge Cases

### 29.1 Full inventory
If inventory is full, vanilla already prevents insertion. LootLock should not alter this.

### 29.2 Multiple players colliding with same item
Server processes collision events. Each player's profile is evaluated independently. If player A rejects (leave-on-ground) and player B accepts, B may pick it up.

### 29.3 Delete mode with multiple players
If player A has delete enabled and rejects item, item is removed before B can pick it up. This is expected but should be documented because it can matter on shared servers. Servers may disable delete via policy.

### 29.4 Creative mode
**Default:** apply LootLock to creative players too, because creative inventory pollution can still happen. Provide future setting if needed.

### 29.5 Death drops
LootLock applies to dropped item entities. If allowlist mode is active and player tries to recover death drops, non-allowlisted items will not be picked up. UI should include "panic disable" key / command.

```
/lootlock disable
```

Optional keybind: Toggle LootLock Enabled.

### 29.6 Empty allowlist
Dangerous but valid. It means "pick up nothing". UI must warn.

### 29.7 Modded items
Supported by registry ID. If a mod is removed, rules become unresolved but stay in file.

### 29.8 Item stacks with NBT
v1 matches only base item ID. A named sword and an unnamed sword both match `minecraft:diamond_sword`.

### 29.9 Hoppers and mobs
v1 applies to players only. Hoppers, foxes, allays, and other item interactions are out of scope unless they use player pickup logic, which they generally do not.

---

## 30. Research Gaps

### RG-001 — Pickup lifecycle
Verify complete item pickup lifecycle.
**Status:** Complete, PoC-001 validated in integrated singleplayer with server-thread lifecycle logs and explicit reject-path cancellation (2026-05-30).

### RG-002 — Delete-mode injection point
Verify safest delete-mode injection point. Must respect pickup delay and ownership.
**Status:** Open.

### RG-003 — PersistentState implementation
Validate `PersistentState` implementation. PoC has not yet been completed.
**Status:** Open.

### RG-004 — Networking implementation style
Validate networking implementation style (packet object API vs. classic `Identifier` + `PacketByteBuf`) for the selected Fabric API version on 1.20.1.
**Status:** Open.

### RG-005 — Dedicated server compatibility
Dedicated server compatibility verification. No accidental client-class loading.
**Status:** Open.

---

## 31. Proof of Concept Gates

### PoC-002 — Delete Mode Validation
**Goal:** Respect pickup delay and ownership.
**Success:** No unintended deletions.

### PoC-003 — Persistence Validation
**Goal:** `PersistentState` survives restart.
**Success:** Data restored correctly.

### PoC-004 — Networking Validation
**Goal:** Client / server sync functions.
**Success:** Profile updates synchronize correctly.

### PoC-005 — Dedicated Server Validation
**Goal:** No client-class loading on dedicated server.
**Success:** Dedicated server stable.

---

## 32. Implementation Phases

### Phase 0 — Project setup
**Deliverables:**
- Fabric 1.20.1 project.
- Mod ID `lootlock`.
- Main / client entrypoints.
- Empty mixin config.
- Logger.
- Basic run configs.

**Exit criteria:**
- Client launches.
- Dedicated server launches.
- No client classes loaded on server.

### Phase 1 — Data model and serialization
**Deliverables:**
- Enums.
- Profile classes.
- Player data classes.
- JSON serializer.
- Config migration shell.
- Unit tests for load / save.

**Exit criteria:**
- Can create default profile.
- Can save and reload profile.
- Invalid item IDs do not crash.

### Phase 2 — Rule engine
**Deliverables:**
- Compiled `RuleSet`.
- `RuleEngine`.
- Tests for denylist / allowlist.

**Exit criteria:**
- O(1) exact item ID matching.
- Empty denylist accepts all.
- Empty allowlist rejects all.

### Phase 3 — Server pickup hook
**Deliverables:**
- `ItemEntityMixin`.
- `PickupGuard`.
- Server player data manager.
- Leave-on-ground behavior.

**Exit criteria:**
- Seeds can be blocked.
- Allowed items still pick up.
- Dedicated server test passes.

### Phase 4 — Commands
**Deliverables:**
- `/lootlock status`.
- `/lootlock mode`.
- `/lootlock rule add/remove/list`.
- `/lootlock profile`.
- `/lootlock enable/disable`.

**Exit criteria:**
- Server-only usage is possible.

### Phase 5 — Networking
**Deliverables:**
- Register server / client packets.
- Sync player data on join.
- Update profile from client.
- Activate profile from client.
- Blocked notice packet.

**Exit criteria:**
- Client receives server profile.
- Client edits are validated server-side.
- Stale revisions are rejected safely.

### Phase 6 — Client UI
**Deliverables:**
- Main screen.
- Profile screen.
- Rule screen.
- Item search.
- Save / cancel draft flow.
- Unsupported server screen.

**Exit criteria:**
- Player can configure without commands.
- No packet spam during typing.
- UI handles server rejection.

### Phase 7 — Profiles and keybinds
**Deliverables:**
- Cycle active profile keybind.
- Open UI keybind.
- Profile activation packet.
- Toast / actionbar feedback.

**Exit criteria:**
- Profile switches immediately server-side.
- Active profile persists.

### Phase 8 — Delete rejected items
**Deliverables:**
- Profile rejected action.
- Server policy.
- Confirmation UI.
- Command confirmation.

**Exit criteria:**
- Delete is disabled by default.
- Leave-on-ground remains default.
- Server can forbid delete.

### Phase 9 — Polish and release
**Deliverables:**
- Lang entries.
- Icon.
- README.
- Modrinth / CurseForge metadata.
- Tests.
- Known limitations doc.

**Exit criteria:**
- No crashes on dedicated server.
- No item duplication.
- No client-only class loading on server.
- 1000+ rule profile performs normally.

---

## 33. Test Plan

### 33.1 Unit tests

**Rule engine:**
- Denylist rejects listed item.
- Denylist accepts unlisted item.
- Allowlist accepts listed item.
- Allowlist rejects unlisted item.
- Disabled profile accepts all.
- Missing active profile accepts all.

**Serialization:**
- Default profile roundtrip.
- Multiple profiles roundtrip.
- Unknown item ID retained.
- Corrupt JSON backup behavior.

### 33.2 Manual singleplayer tests
- Block wheat seeds.
- Block eggs.
- Allow only diamonds.
- Switch profiles.
- Restart world and verify persistence.
- Enable delete and verify rejected entity removed.
- Disable mod behavior and verify pickup normal.

### 33.3 Dedicated server tests
- Server-only commands work.
- Client UI sync works with modded client.
- Client without mod can still play.
- Client cannot bypass server rule by editing local files.
- Two players with different profiles behave differently.

### 33.4 Compatibility tests
Test with:
- Fabric API only.
- ModMenu.
- REI or EMI installed.
- Inventory sorting mod.
- A content mod adding items.

### 33.5 Regression tests
- Empty allowlist warning.
- Delete action confirmation.
- Stale revision rejection.
- Removed mod item ID retained.
- Dedicated server does not load `net.minecraft.client.*`.

### 33.6 Performance tests
- 1000+ rules.
- Continuous collisions.
- Multiple players.

---

## 34. Release Criteria

**Must pass:**
- No crashes.
- No duplication bugs.
- No inventory corruption.
- Dedicated server support.
- Modded item support.
- Profile persistence.
- No measurable TPS degradation.

**User experience:**
- First rule configured within one minute.

---

## 35. Release Notes / User Manual

### 35.1 Install

**Singleplayer:**
1. Install Fabric Loader for Minecraft 1.20.1.
2. Install Fabric API.
3. Put LootLock in `mods`.
4. Launch game.

**Multiplayer:**
1. Install LootLock on the Fabric server.
2. Install LootLock on clients for UI.
3. Players without client install can use commands if server allows.

### 35.2 Basic usage with UI
1. Press `O` to open LootLock.
2. Create or select a profile.
3. Choose Denylist or Allowlist.
4. Open Rules.
5. Search for an item.
6. Add it.
7. Save.
8. Walk over dropped items normally.

### 35.3 Denylist example — Farming
```
Profile:
  Name: Farming
  Mode: Denylist
  Rules:
    - minecraft:wheat_seeds
    - minecraft:egg
  Action: Leave on ground
```
**Result:** Seeds and eggs do not enter your inventory.

### 35.4 Allowlist example — Rare Mining
```
Profile:
  Name: Rare Mining
  Mode: Allowlist
  Rules:
    - minecraft:diamond
    - minecraft:emerald
    - minecraft:ancient_debris
  Action: Leave on ground
```
**Result:** Only listed items enter inventory.

### 35.5 Delete warning
Delete mode permanently removes rejected dropped item entities. Use it only for junk items. Server admins can disable it.

### 35.6 Panic disable
Use:
```
/lootlock disable
```
Use this before recovering death drops if your allowlist would block important items.

---

## 36. Known Limitations in v1
- Exact item ID matching only.
- No item tag rules.
- No NBT-specific filtering.
- No container insertion filtering.
- No hopper filtering.
- No cloud / shared profiles.
- No automatic JEI / REI / EMI integration required.
- Client-only mode is not a supported reliable feature.

---

## 37. Future Roadmap

### v1.1
- Tag-based filtering (`#minecraft:seeds`).
- Import / export profile files from UI.
- Better HUD blocked item summaries.
- Favorite rules.
- Better notifications.

### v1.2
- Temporary session filters.
- Preset profiles.
- Rule comments.
- Advanced server admin policy.
- Favorites.

### v2
- Conditional rules.
- NBT / component-aware matching.
- Optional container filtering.
- Cross-version migration to newer Minecraft data component system.

---

## 38. Implementation Decision Log

**Decision 1:** Server-side pickup authority — chosen because the server owns actual inventory mutation and dropped item entity state.

**Decision 2:** Primary hook is `ItemEntity` collision — chosen because it matches the product promise: filtering dropped item pickup.

**Decision 3:** `PlayerInventory` guard is not active by default — chosen because it may unintentionally affect non-pickup inventory flows.

**Decision 4:** Rules use registry IDs — chosen for mod compatibility and stable persistence.

**Decision 5:** Profiles are per-player — chosen because LootLock is personal utility behavior, not a global loot rule.

**Decision 6:** Delete rejected items is per-profile and off by default — chosen for safety.

---

## 39. Research References

- **Fabric networking docs / API:** `ServerPlayNetworking` and `ClientPlayNetworking` are side-specific play-stage networking APIs.
- **Yarn 1.20.1 PlayerInventory docs** expose inventory fields and `insertStack` methods.
- **Yarn / Minecraft item model:** `ItemStack` holds stack-specific data; `Item` represents the item type.
- **FiltPick:** existing mod with blacklist / whitelist and destruction modes.
- **Pickup Filter:** existing server-side Fabric mod for 1.20–1.20.1 with whitelist / blacklist behavior.

---

## 40. Current Project Status

| Aspect | Status |
|---|---|
| Architecture | **Locked** |
| Implementation | **Not Started** |
| Research | **Partially Complete** — RG-001 complete, RG-002 through RG-005 open |
| Next Milestone | **PoC-002 — Delete Mode Validation** |

---

*This document is the authoritative project specification until the research gaps in Section 30 are resolved and a final v3 architecture document is produced.*
