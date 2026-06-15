package com.grahambartley.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.data.RuleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
        newProfile(
            "Mining",
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            "minecraft:cobblestone");

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
        newProfileWithId(
            data.getProfiles().get(0).getId(),
            "Updated",
            FilterMode.ALLOWLIST,
            RejectedItemAction.DELETE,
            false,
            "minecraft:diamond");

    ClientToServerPackets.MutationResult result =
        ClientToServerPackets.applyUpdateProfile(
            data, new ClientToServerPackets.UpdateProfilePayload(999L, replacement));

    assertFalse(result.success());
    assertEquals(ClientToServerPackets.MutationRejectionReason.STALE, result.reason());
    assertNotEquals("Updated", data.getProfiles().get(0).getName());
  }

  @Test
  void applyUpdateProfileNormalizesDeleteWhenPolicyDisablesIt() {
    LootLockPlayerData data = createDataWithOneProfile();
    data.setRevision(6L);
    LootLockProfile replacement =
        newProfileWithId(
            data.getProfiles().get(0).getId(),
            "Updated",
            FilterMode.DENYLIST,
            RejectedItemAction.DELETE,
            true,
            "minecraft:stone");

    ClientToServerPackets.MutationResult result =
        ClientToServerPackets.applyUpdateProfile(
            data, new ClientToServerPackets.UpdateProfilePayload(6L, replacement), false);

    assertTrue(result.success());
    assertEquals(
        RejectedItemAction.LEAVE_ON_GROUND, data.getProfiles().get(0).getRejectedItemAction());
  }

  @Test
  void applyCreateProfileCreatesWithoutChangingActiveProfile() {
    LootLockPlayerData data = createDataWithOneProfile();
    UUID originalActiveProfileId = data.getActiveProfileId();
    data.setRevision(4L);

    ClientToServerPackets.MutationResult result =
        ClientToServerPackets.applyCreateProfile(
            data, new ClientToServerPackets.CreateProfilePayload(4L, " Farming ", null));

    assertTrue(result.success());
    assertEquals(2, data.getProfiles().size());
    assertEquals("Farming", data.getProfiles().get(1).getName());
    assertEquals(originalActiveProfileId, data.getActiveProfileId());
  }

  @ParameterizedTest(name = "existing={0} -> success={1}")
  @CsvSource({
    "8, true",
    "9, false",
  })
  void applyCreateProfileEnforcesMaxProfilesCap(int existing, boolean expectedSuccess) {
    LootLockPlayerData data = createDataWithNProfiles(existing);

    ClientToServerPackets.MutationResult result =
        ClientToServerPackets.applyCreateProfile(
            data,
            new ClientToServerPackets.CreateProfilePayload(data.getRevision(), "Fresh", null));

    assertEquals(expectedSuccess, result.success());
    if (expectedSuccess) {
      assertEquals(existing + 1, data.getProfiles().size());
    } else {
      assertEquals(ClientToServerPackets.MutationRejectionReason.TOO_MANY, result.reason());
      assertEquals(existing, data.getProfiles().size());
    }
  }

  @Test
  void applyCreateProfileRejectsDuplicateNamesIgnoringCase() {
    LootLockPlayerData data = createDataWithOneProfile();
    data.setRevision(2L);

    ClientToServerPackets.MutationResult result =
        ClientToServerPackets.applyCreateProfile(
            data, new ClientToServerPackets.CreateProfilePayload(2L, "default", null));

    assertFalse(result.success());
    assertEquals(ClientToServerPackets.MutationRejectionReason.DUPLICATE_NAME, result.reason());
    assertEquals(1, data.getProfiles().size());
  }

  @Test
  void applyActivateAndDeleteProfileMutatesState() {
    LootLockPlayerData data = createDataWithOneProfile();
    data.setRevision(3L);
    LootLockProfile second =
        newProfile("Second", FilterMode.DENYLIST, RejectedItemAction.LEAVE_ON_GROUND, true);
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

  @Test
  void updateServerPolicyPayloadRoundTrips() {
    PacketByteBuf buf = ClientToServerPackets.writeUpdateServerPolicyPayload(false);
    ClientToServerPackets.UpdateServerPolicyPayload payload =
        ClientToServerPackets.readUpdateServerPolicyPayload(buf);

    assertFalse(payload.allowDeleteRejectedItems());
  }

  @Test
  void updateGlobalEnablePayloadRoundTrips() {
    PacketByteBuf buf = ClientToServerPackets.writeUpdateGlobalEnablePayload(12L, false);
    ClientToServerPackets.UpdateGlobalEnablePayload payload =
        ClientToServerPackets.readUpdateGlobalEnablePayload(buf);

    assertEquals(12L, payload.baseRevision());
    assertFalse(payload.enabled());
  }

  @Test
  void applyUpdateGlobalEnableFlipsEveryProfile() {
    LootLockPlayerData data = createDataWithMixedEnabledProfiles();
    data.setRevision(5L);

    ClientToServerPackets.MutationResult result =
        ClientToServerPackets.applyUpdateGlobalEnable(
            data, new ClientToServerPackets.UpdateGlobalEnablePayload(5L, false));

    assertTrue(result.success());
    for (LootLockProfile profile : data.getProfiles()) {
      assertFalse(profile.isEnabled());
    }
  }

  @Test
  void applyUpdateGlobalEnableRejectsStaleRevision() {
    LootLockPlayerData data = createDataWithMixedEnabledProfiles();
    data.setRevision(5L);

    ClientToServerPackets.MutationResult result =
        ClientToServerPackets.applyUpdateGlobalEnable(
            data, new ClientToServerPackets.UpdateGlobalEnablePayload(4L, false));

    assertFalse(result.success());
    assertEquals(ClientToServerPackets.MutationRejectionReason.STALE, result.reason());
    assertTrue(data.getProfiles().get(0).isEnabled());
  }

  @ParameterizedTest(name = "permissionLevel2={0} -> isOperator={1}")
  @CsvSource({"true, true", "false, false"})
  void isOperatorReflectsPermissionLevel(boolean hasPermissionLevel2, boolean expected) {
    ServerPlayerEntity player = mock(ServerPlayerEntity.class);
    lenient().when(player.hasPermissionLevel(2)).thenReturn(hasPermissionLevel2);

    assertEquals(expected, ClientToServerPackets.isOperator(player));
  }

  @Test
  void isOperatorReturnsFalseForNullPlayer() {
    assertFalse(ClientToServerPackets.isOperator(null));
  }

  @Test
  void applyUpdateProfileSucceedsForNonOpSelfEditAtApplyLayer() {
    LootLockPlayerData data = createDataWithOneProfile();
    data.setRevision(2L);
    LootLockProfile replacement =
        newProfileWithId(
            data.getProfiles().get(0).getId(),
            "Self Edit",
            FilterMode.ALLOWLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            "minecraft:wheat");

    ClientToServerPackets.MutationResult result =
        ClientToServerPackets.applyUpdateProfile(
            data, new ClientToServerPackets.UpdateProfilePayload(2L, replacement));

    assertTrue(result.success());
    assertEquals("Self Edit", data.getProfiles().get(0).getName());
  }

  @Test
  void applyActivateProfileSucceedsForNonOpSelfEditAtApplyLayer() {
    LootLockPlayerData data = createDataWithOneProfile();
    data.setRevision(2L);

    ClientToServerPackets.MutationResult result =
        ClientToServerPackets.applyActivateProfile(
            data,
            new ClientToServerPackets.ActivateProfilePayload(
                2L, data.getProfiles().get(0).getId()));

    assertTrue(result.success());
  }

  @Test
  void applyCreateProfileSucceedsForNonOpSelfEditAtApplyLayer() {
    LootLockPlayerData data = createDataWithOneProfile();
    data.setRevision(2L);

    ClientToServerPackets.MutationResult result =
        ClientToServerPackets.applyCreateProfile(
            data, new ClientToServerPackets.CreateProfilePayload(2L, "Self Created", null));

    assertTrue(result.success());
    assertEquals(2, data.getProfiles().size());
    assertEquals("Self Created", data.getProfiles().get(1).getName());
  }

  @Test
  void applyDeleteProfileSucceedsForNonOpSelfEditAtApplyLayer() {
    LootLockPlayerData data = createDataWithOneProfile();
    data.setRevision(3L);
    LootLockProfile second =
        newProfile("Second", FilterMode.DENYLIST, RejectedItemAction.LEAVE_ON_GROUND, true);
    data.setProfiles(List.of(data.getProfiles().get(0), second));

    ClientToServerPackets.MutationResult result =
        ClientToServerPackets.applyDeleteProfile(
            data, new ClientToServerPackets.DeleteProfilePayload(3L, second.getId()));

    assertTrue(result.success());
    assertEquals(1, data.getProfiles().size());
  }

  @Test
  void applyUpdateGlobalEnableSucceedsForNonOpSelfEditAtApplyLayer() {
    LootLockPlayerData data = createDataWithMixedEnabledProfiles();
    data.setRevision(5L);

    ClientToServerPackets.MutationResult result =
        ClientToServerPackets.applyUpdateGlobalEnable(
            data, new ClientToServerPackets.UpdateGlobalEnablePayload(5L, false));

    assertTrue(result.success());
    for (LootLockProfile profile : data.getProfiles()) {
      assertFalse(profile.isEnabled());
    }
  }

  private static LootLockProfile newProfile(
      String name,
      FilterMode mode,
      RejectedItemAction action,
      boolean enabled,
      String... ruleItemIds) {
    return newProfileWithId(UUID.randomUUID(), name, mode, action, enabled, ruleItemIds);
  }

  private static LootLockProfile newProfileWithId(
      UUID id,
      String name,
      FilterMode mode,
      RejectedItemAction action,
      boolean enabled,
      String... ruleItemIds) {
    RuleEntry[] rules = new RuleEntry[ruleItemIds.length];
    for (int i = 0; i < ruleItemIds.length; i++) {
      rules[i] = new RuleEntry(ruleItemIds[i]);
    }
    return new LootLockProfile(id, name, mode, action, enabled, List.of(rules));
  }

  private static LootLockPlayerData createDataWithMixedEnabledProfiles() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());
    data.setProfiles(
        List.of(
            newProfile("Farming", FilterMode.DENYLIST, RejectedItemAction.LEAVE_ON_GROUND, true),
            newProfile("Mining", FilterMode.ALLOWLIST, RejectedItemAction.LEAVE_ON_GROUND, false)));
    return data;
  }

  private static LootLockPlayerData createDataWithNProfiles(int count) {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());
    List<LootLockProfile> profiles = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      profiles.add(
          newProfile(
              "Profile " + i, FilterMode.DENYLIST, RejectedItemAction.LEAVE_ON_GROUND, true));
    }
    data.setProfiles(profiles);
    if (!profiles.isEmpty()) {
      data.setActiveProfileId(profiles.get(0).getId());
    }
    return data;
  }

  private static LootLockPlayerData createDataWithOneProfile() {
    UUID playerId = UUID.randomUUID();
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerId);
    LootLockProfile profile =
        newProfile(
            "Default",
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            "minecraft:stone");
    data.setProfiles(List.of(profile));
    data.setActiveProfileId(profile.getId());
    return data;
  }
}
