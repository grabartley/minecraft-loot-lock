package com.grahambartley.lootlock.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.lootlock.data.LootLockPlayerData;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConfigMigrationTest {

  @Test
  void currentSchemaPassesWithoutWarnings() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());

    ConfigValidationResult result = ConfigMigration.run(data);

    assertSame(ConfigValidationResult.success(), result);
    assertEquals(LootLockPlayerData.CURRENT_SCHEMA_VERSION, data.getSchemaVersion());
  }

  @ParameterizedTest(name = "schema {0} is rejected and left untouched")
  @ValueSource(ints = {LootLockPlayerData.CURRENT_SCHEMA_VERSION + 1, Integer.MAX_VALUE})
  void futureSchemaFailsWithoutMigrating(int futureVersion) {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());
    data.setSchemaVersion(futureVersion);

    ConfigValidationResult result = ConfigMigration.run(data);

    assertFalse(result.valid());
    assertEquals(
        List.of(
            "Unknown schema version "
                + futureVersion
                + ". Expected "
                + LootLockPlayerData.CURRENT_SCHEMA_VERSION
                + " or earlier."),
        result.errors());
    assertEquals(futureVersion, data.getSchemaVersion());
  }

  @ParameterizedTest(name = "schema {0} migrates to current with a warning")
  @ValueSource(ints = {0, -1})
  void legacySchemaMigratesAndWarns(int legacyVersion) {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());
    data.setSchemaVersion(legacyVersion);

    ConfigValidationResult result = ConfigMigration.run(data);

    assertTrue(result.valid());
    assertEquals(
        List.of("Migrated from schema version " + legacyVersion + " to 1"), result.errors());
    assertEquals(LootLockPlayerData.CURRENT_SCHEMA_VERSION, data.getSchemaVersion());
  }

  @Test
  void legacySchemaBindsFirstProfileWhenActiveProfileMissing() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());
    data.setSchemaVersion(0);
    data.setActiveProfileId(null);

    ConfigMigration.run(data);

    assertEquals(data.getProfiles().get(0).getId(), data.getActiveProfileId());
  }

  @Test
  void legacySchemaKeepsExistingActiveProfile() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());
    UUID chosen = UUID.randomUUID();
    data.setSchemaVersion(0);
    data.setActiveProfileId(chosen);

    ConfigMigration.run(data);

    assertEquals(chosen, data.getActiveProfileId());
  }
}
