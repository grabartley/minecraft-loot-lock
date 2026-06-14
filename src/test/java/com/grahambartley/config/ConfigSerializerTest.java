package com.grahambartley.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.grahambartley.config.ConfigSerializer.ConfigDeserializationException;
import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.data.RuleEntry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConfigSerializerTest {

  @Test
  void defaultProfileRoundtrips() throws ConfigDeserializationException {
    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData original = LootLockPlayerData.createDefault(playerUuid);

    String json = ConfigSerializer.serialize(original);
    LootLockPlayerData restored = ConfigSerializer.deserialize(json, playerUuid);

    assertEquals(original.getSchemaVersion(), restored.getSchemaVersion());
    assertEquals(original.getPlayerUuid(), restored.getPlayerUuid());
    assertEquals(original.getActiveProfileId(), restored.getActiveProfileId());
    assertEquals(original.getRevision(), restored.getRevision());
    assertEquals(original.getProfiles().size(), restored.getProfiles().size());

    LootLockProfile originalProfile = original.getActiveProfile().orElseThrow();
    LootLockProfile restoredProfile = restored.getActiveProfile().orElseThrow();
    assertEquals(originalProfile.getId(), restoredProfile.getId());
    assertEquals(originalProfile.getName(), restoredProfile.getName());
    assertEquals(originalProfile.getMode(), restoredProfile.getMode());
    assertEquals(originalProfile.getRejectedItemAction(), restoredProfile.getRejectedItemAction());
    assertEquals(originalProfile.isEnabled(), restoredProfile.isEnabled());
    assertEquals(originalProfile.getRules().size(), restoredProfile.getRules().size());
  }

  @Test
  void multipleProfilesRoundtrip() throws ConfigDeserializationException {
    UUID playerUuid = UUID.randomUUID();
    UUID miningId = UUID.randomUUID();
    UUID farmingId = UUID.randomUUID();
    UUID mobFarmId = UUID.randomUUID();

    LootLockPlayerData original = LootLockPlayerData.createDefault(playerUuid);
    original.setProfiles(
        List.of(
            new LootLockProfile(
                miningId,
                "Mining",
                FilterMode.DENYLIST,
                RejectedItemAction.DELETE,
                true,
                List.of(
                    new RuleEntry("minecraft:cobblestone"),
                    new RuleEntry("minecraft:granite"),
                    new RuleEntry("minecraft:diorite"),
                    new RuleEntry("minecraft:andesite"))),
            new LootLockProfile(
                farmingId,
                "Farming",
                FilterMode.DENYLIST,
                RejectedItemAction.LEAVE_ON_GROUND,
                true,
                List.of(new RuleEntry("minecraft:wheat_seeds"), new RuleEntry("minecraft:egg"))),
            new LootLockProfile(
                mobFarmId,
                "Mob Farm",
                FilterMode.ALLOWLIST,
                RejectedItemAction.LEAVE_ON_GROUND,
                true,
                List.of(new RuleEntry("minecraft:gunpowder")))));
    original.setActiveProfileId(farmingId);

    String json = ConfigSerializer.serialize(original);
    LootLockPlayerData restored = ConfigSerializer.deserialize(json, playerUuid);

    assertEquals(3, restored.getProfiles().size());
    assertEquals(farmingId, restored.getActiveProfileId());

    LootLockProfile restoredMining = profileById(restored, miningId);
    assertEquals("Mining", restoredMining.getName());
    assertEquals(FilterMode.DENYLIST, restoredMining.getMode());
    assertEquals(RejectedItemAction.DELETE, restoredMining.getRejectedItemAction());
    assertEquals(4, restoredMining.getRules().size());

    LootLockProfile restoredFarming = profileById(restored, farmingId);
    assertEquals("Farming", restoredFarming.getName());
    assertEquals(2, restoredFarming.getRules().size());

    LootLockProfile restoredMobFarm = profileById(restored, mobFarmId);
    assertEquals("Mob Farm", restoredMobFarm.getName());
    assertEquals(FilterMode.ALLOWLIST, restoredMobFarm.getMode());
    assertEquals(1, restoredMobFarm.getRules().size());
  }

  @Test
  void unknownItemIdIsRetained() throws ConfigDeserializationException {
    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData original = LootLockPlayerData.createDefault(playerUuid);
    original.setProfiles(
        List.of(
            new LootLockProfile(
                UUID.randomUUID(),
                "Test",
                FilterMode.DENYLIST,
                RejectedItemAction.LEAVE_ON_GROUND,
                true,
                List.of(
                    new RuleEntry("minecraft:dirt"),
                    new RuleEntry("oldmod:removed_item"),
                    new RuleEntry("anothermod:unknown_item")))));

    String json = ConfigSerializer.serialize(original);
    LootLockPlayerData restored = ConfigSerializer.deserialize(json, playerUuid);

    List<String> itemIds =
        restored.getProfiles().get(0).getRules().stream().map(RuleEntry::itemId).toList();
    assertEquals(
        List.of("minecraft:dirt", "oldmod:removed_item", "anothermod:unknown_item"), itemIds);
  }

  @ParameterizedTest(name = "schemaVersion={0} round-trips")
  @ValueSource(ints = {0, 1, 42})
  void schemaVersionIsPreserved(int schemaVersion) throws ConfigDeserializationException {
    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData original = LootLockPlayerData.createDefault(playerUuid);
    original.setSchemaVersion(schemaVersion);

    String json = ConfigSerializer.serialize(original);
    LootLockPlayerData restored = ConfigSerializer.deserialize(json, playerUuid);

    assertEquals(schemaVersion, restored.getSchemaVersion());
  }

  @ParameterizedTest(name = "revision={0} round-trips")
  @ValueSource(longs = {0L, 1L, 42L, 1_000_000L})
  void revisionIsPreserved(long revision) throws ConfigDeserializationException {
    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData original = LootLockPlayerData.createDefault(playerUuid);
    original.setRevision(revision);

    String json = ConfigSerializer.serialize(original);
    LootLockPlayerData restored = ConfigSerializer.deserialize(json, playerUuid);

    assertEquals(revision, restored.getRevision());
  }

  @Test
  void deserializeWithWrongUuidFails() {
    UUID playerUuid = UUID.randomUUID();
    UUID wrongUuid = UUID.randomUUID();
    String json = ConfigSerializer.serialize(LootLockPlayerData.createDefault(playerUuid));

    assertThrows(
        ConfigDeserializationException.class, () -> ConfigSerializer.deserialize(json, wrongUuid));
  }

  @ParameterizedTest(name = "deserialize rejects \"{0}\"")
  @ValueSource(strings = {"", "{not valid json}", "null"})
  void deserializeRejectsInvalidJson(String invalidJson) {
    assertThrows(
        ConfigDeserializationException.class,
        () -> ConfigSerializer.deserialize(invalidJson, UUID.randomUUID()));
  }

  @Test
  void profileColorRoundtripsThroughJson() throws ConfigDeserializationException {
    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData original = LootLockPlayerData.createDefault(playerUuid);
    int color = 0xFF4F9D43;
    original.setProfiles(
        List.of(
            new LootLockProfile(
                UUID.randomUUID(),
                "Tinted",
                FilterMode.DENYLIST,
                RejectedItemAction.LEAVE_ON_GROUND,
                true,
                color,
                List.of())));

    String json = ConfigSerializer.serialize(original);
    LootLockPlayerData restored = ConfigSerializer.deserialize(json, playerUuid);

    assertEquals(color, restored.getProfiles().get(0).getColor());
  }

  @Test
  void legacyProfileWithoutColorDeserializesAsZero() throws ConfigDeserializationException {
    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData original = LootLockPlayerData.createDefault(playerUuid);
    original.setProfiles(
        List.of(
            new LootLockProfile(
                UUID.randomUUID(),
                "Legacy",
                FilterMode.DENYLIST,
                RejectedItemAction.LEAVE_ON_GROUND,
                true,
                List.of())));

    String json =
        ConfigSerializer.serialize(original).replaceAll("\"color\": [\\-]?\\d+,?\\n?\\s*", "");
    LootLockPlayerData restored = ConfigSerializer.deserialize(json, playerUuid);

    assertEquals(0, restored.getProfiles().get(0).getColor());
  }

  private static LootLockProfile profileById(LootLockPlayerData data, UUID id) {
    return data.getProfiles().stream().filter(p -> p.getId().equals(id)).findFirst().orElseThrow();
  }
}
