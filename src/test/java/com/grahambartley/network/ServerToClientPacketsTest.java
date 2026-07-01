package com.grahambartley.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.data.RuleEntry;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

class ServerToClientPacketsTest {

  private static <T extends CustomPayload> T roundTrip(
      PacketCodec<PacketByteBuf, T> codec, T payload) {
    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
    codec.encode(buf, payload);
    return codec.decode(buf);
  }

  @Test
  void syncPayloadRoundTripsFullPlayerSnapshot() {
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

    ServerToClientPackets.SyncPayload decoded =
        roundTrip(
            ServerToClientPackets.SyncPayload.CODEC, ServerToClientPackets.syncPayloadOf(data));

    assertEquals(data.getSchemaVersion(), decoded.schemaVersion());
    assertEquals(playerId, decoded.playerUuid());
    assertEquals(42L, decoded.revision());
    assertEquals(profileId, decoded.activeProfileId());
    assertEquals(1, decoded.profiles().size());

    LootLockProfile decodedProfile = decoded.profiles().get(0);
    assertEquals("Mining", decodedProfile.getName());
    assertEquals(FilterMode.DENYLIST, decodedProfile.getMode());
    assertEquals(RejectedItemAction.DELETE, decodedProfile.getRejectedItemAction());
    assertEquals(2, decodedProfile.getRules().size());
    assertEquals("minecraft:cobblestone", decodedProfile.getRules().get(0).itemId());

    assertFalse(decoded.clientCanEdit());
    assertTrue(decoded.allowDeleteRejectedItems());
  }

  @Test
  void syncPayloadRoundTripsExplicitPolicyValue() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());

    ServerToClientPackets.SyncPayload decoded =
        roundTrip(
            ServerToClientPackets.SyncPayload.CODEC,
            ServerToClientPackets.syncPayloadOf(data, true, false));

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
        roundTrip(
            ServerToClientPackets.SyncPayload.CODEC, ServerToClientPackets.syncPayloadOf(data));

    assertEquals(1, decoded.profiles().size());
    assertEquals("EmptyRules", decoded.profiles().get(0).getName());
    assertEquals(0, decoded.profiles().get(0).getRules().size());
  }

  @Test
  void syncPayloadRoundTripsNullActiveProfileId() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());
    data.setActiveProfileId(null);
    data.setRevision(3L);

    ServerToClientPackets.SyncPayload decoded =
        roundTrip(
            ServerToClientPackets.SyncPayload.CODEC, ServerToClientPackets.syncPayloadOf(data));

    assertNull(decoded.activeProfileId());
    assertEquals(1, decoded.profiles().size());
  }

  @Test
  void syncPayloadRejectsOutOfBoundsProfileCount() {
    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
    buf.writeVarInt(LootLockPlayerData.CURRENT_SCHEMA_VERSION);
    buf.writeUuid(UUID.randomUUID());
    buf.writeVarLong(1L);
    buf.writeBoolean(false);
    buf.writeVarInt(PacketLimits.MAX_PROFILES + 1);

    assertThrows(DecoderException.class, () -> ServerToClientPackets.SyncPayload.CODEC.decode(buf));
  }

  @Test
  void blockedNoticePayloadRoundTrips() {
    Identifier itemId = Identifier.of("minecraft", "wheat_seeds");

    ServerToClientPackets.BlockedNoticePayload decoded =
        roundTrip(
            ServerToClientPackets.BlockedNoticePayload.CODEC,
            new ServerToClientPackets.BlockedNoticePayload(itemId, 16, false));

    assertEquals(itemId, decoded.itemId());
    assertEquals(16, decoded.count());
    assertFalse(decoded.deleted());
  }
}
