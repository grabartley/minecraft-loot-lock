package com.grahambartley.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.data.RuleEntry;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

class ServerToClientPacketsTest {
  @Test
  void syncPayloadRoundTripsPlayerSnapshot() {
    UUID playerId = UUID.randomUUID();
    UUID profileId = UUID.randomUUID();

    LootLockProfile profile =
        new LootLockProfile(
            profileId,
            "Mining",
            FilterMode.DENYLIST,
            RejectedItemAction.DELETE,
            true,
            List.of(new RuleEntry("minecraft:cobblestone"), new RuleEntry("minecraft:gravel")));

    LootLockPlayerData data = LootLockPlayerData.createDefault(playerId);
    data.setActiveProfileId(profileId);
    data.setProfiles(List.of(profile));
    data.setClientCanEdit(false);
    data.setRevision(42L);

    PacketByteBuf encoded = ServerToClientPackets.writeSyncPayload(data);
    ServerToClientPackets.SyncPayload decoded = ServerToClientPackets.readSyncPayload(encoded);

    assertEquals(data.getSchemaVersion(), decoded.schemaVersion());
    assertEquals(playerId, decoded.playerUuid());
    assertEquals(42L, decoded.revision());
    assertEquals(profileId, decoded.activeProfileId());
    assertEquals(1, decoded.profiles().size());
    assertEquals("Mining", decoded.profiles().get(0).getName());
    assertEquals(FilterMode.DENYLIST, decoded.profiles().get(0).getMode());
    assertEquals(RejectedItemAction.DELETE, decoded.profiles().get(0).getRejectedItemAction());
    assertEquals(2, decoded.profiles().get(0).getRules().size());
    assertEquals("minecraft:cobblestone", decoded.profiles().get(0).getRules().get(0).itemId());
    assertFalse(decoded.clientCanEdit());
    assertTrue(decoded.allowDeleteRejectedItems());
  }

  @Test
  void syncPayloadRoundTripsExplicitPolicyValue() {
    UUID playerId = UUID.randomUUID();
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerId);

    ServerToClientPackets.SyncPayload decoded =
        ServerToClientPackets.readSyncPayload(
            ServerToClientPackets.writeSyncPayload(data, true, false));

    assertFalse(decoded.allowDeleteRejectedItems());
  }

  @Test
  void syncPayloadRoundTripsEmptyRuleList() {
    UUID playerId = UUID.randomUUID();
    UUID profileId = UUID.randomUUID();

    LootLockProfile profile =
        new LootLockProfile(
            profileId,
            "EmptyRules",
            FilterMode.ALLOWLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            List.of());

    LootLockPlayerData data = LootLockPlayerData.createDefault(playerId);
    data.setActiveProfileId(profileId);
    data.setProfiles(List.of(profile));
    data.setRevision(7L);

    ServerToClientPackets.SyncPayload decoded =
        ServerToClientPackets.readSyncPayload(ServerToClientPackets.writeSyncPayload(data));

    assertEquals(1, decoded.profiles().size());
    assertEquals("EmptyRules", decoded.profiles().get(0).getName());
    assertEquals(0, decoded.profiles().get(0).getRules().size());
  }

  @Test
  void syncPayloadRoundTripsNullActiveProfileId() {
    UUID playerId = UUID.randomUUID();
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerId);
    data.setActiveProfileId(null);
    data.setRevision(3L);

    ServerToClientPackets.SyncPayload decoded =
        ServerToClientPackets.readSyncPayload(ServerToClientPackets.writeSyncPayload(data));

    assertNull(decoded.activeProfileId());
    assertEquals(1, decoded.profiles().size());
  }

  @Test
  void blockedNoticePayloadRoundTrips() {
    Identifier itemId = Identifier.of("minecraft", "wheat_seeds");
    PacketByteBuf encoded = ServerToClientPackets.writeBlockedNoticePayload(itemId, 16, false);
    ServerToClientPackets.BlockedNoticePayload decoded =
        ServerToClientPackets.readBlockedNoticePayload(encoded);

    assertEquals(itemId, decoded.itemId());
    assertEquals(16, decoded.count());
    assertFalse(decoded.deleted());
  }
}
