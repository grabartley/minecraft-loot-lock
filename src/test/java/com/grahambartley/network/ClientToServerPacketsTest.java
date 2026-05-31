package com.grahambartley.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.data.RuleEntry;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.PacketByteBuf;
import org.junit.jupiter.api.Test;

class ClientToServerPacketsTest {
  @Test
  void helloPayloadRoundTripsVersionAndSchema() {
    PacketByteBuf buf = ClientToServerPackets.writeHelloPayload("1.2.3", 7);

    ClientToServerPackets.HelloPayload payload = ClientToServerPackets.readHelloPayload(buf);

    assertEquals("1.2.3", payload.clientVersion());
    assertEquals(7, payload.schemaVersion());
  }

  @Test
  void updateProfilePayloadRoundTrips() {
    LootLockProfile profile =
        new LootLockProfile(
            UUID.randomUUID(),
            "Mining",
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            List.of(new RuleEntry("minecraft:cobblestone")));

    PacketByteBuf buf = ClientToServerPackets.writeUpdateProfilePayload(8L, profile);
    ClientToServerPackets.UpdateProfilePayload payload =
        ClientToServerPackets.readUpdateProfilePayload(buf);

    assertEquals(8L, payload.baseRevision());
    assertEquals(profile.getId(), payload.profile().getId());
    assertEquals("Mining", payload.profile().getName());
    assertEquals(1, payload.profile().getRules().size());
  }

  @Test
  void applyUpdateProfileRejectsStaleRevision() {
    LootLockPlayerData data = createDataWithOneProfile();
    LootLockProfile replacement =
        new LootLockProfile(
            data.getProfiles().get(0).getId(),
            "Updated",
            FilterMode.ALLOWLIST,
            RejectedItemAction.DELETE,
            false,
            List.of(new RuleEntry("minecraft:diamond")));

    ClientToServerPackets.MutationResult result =
        ClientToServerPackets.applyUpdateProfile(
            data, new ClientToServerPackets.UpdateProfilePayload(999L, replacement));

    assertFalse(result.success());
    assertNotEquals("Updated", data.getProfiles().get(0).getName());
  }

  @Test
  void applyCreateProfileCreatesAndActivatesProfile() {
    LootLockPlayerData data = createDataWithOneProfile();
    data.setRevision(4L);

    ClientToServerPackets.MutationResult result =
        ClientToServerPackets.applyCreateProfile(
            data, new ClientToServerPackets.CreateProfilePayload(4L, " Farming ", null));

    assertTrue(result.success());
    assertEquals(2, data.getProfiles().size());
    assertEquals("Farming", data.getProfiles().get(1).getName());
    assertEquals(data.getProfiles().get(1).getId(), data.getActiveProfileId());
  }

  @Test
  void applyActivateAndDeleteProfileMutatesState() {
    LootLockPlayerData data = createDataWithOneProfile();
    data.setRevision(3L);
    LootLockProfile second =
        new LootLockProfile(
            UUID.randomUUID(),
            "Second",
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            List.of());
    data.setProfiles(List.of(data.getProfiles().get(0), second));

    ClientToServerPackets.MutationResult activateResult =
        ClientToServerPackets.applyActivateProfile(
            data, new ClientToServerPackets.ActivateProfilePayload(3L, second.getId()));

    assertTrue(activateResult.success());
    assertEquals(second.getId(), data.getActiveProfileId());

    ClientToServerPackets.MutationResult deleteResult =
        ClientToServerPackets.applyDeleteProfile(
            data, new ClientToServerPackets.DeleteProfilePayload(3L, second.getId()));

    assertTrue(deleteResult.success());
    assertEquals(1, data.getProfiles().size());
    assertEquals(data.getProfiles().get(0).getId(), data.getActiveProfileId());
  }

  private static LootLockPlayerData createDataWithOneProfile() {
    UUID playerId = UUID.randomUUID();
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerId);
    LootLockProfile profile =
        new LootLockProfile(
            UUID.randomUUID(),
            "Default",
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            List.of(new RuleEntry("minecraft:stone")));
    data.setProfiles(List.of(profile));
    data.setActiveProfileId(profile.getId());
    return data;
  }
}
