package com.grahambartley.lootlock.config;

import com.grahambartley.lootlock.data.LootLockPlayerData;
import java.util.ArrayList;
import java.util.List;

public final class ConfigMigration {

  public static ConfigValidationResult run(LootLockPlayerData data) {
    List<String> warnings = new ArrayList<>();
    int schemaVersion = data.getSchemaVersion();

    if (schemaVersion > LootLockPlayerData.CURRENT_SCHEMA_VERSION) {
      warnings.add(
          "Unknown schema version "
              + schemaVersion
              + ". Expected "
              + LootLockPlayerData.CURRENT_SCHEMA_VERSION
              + " or earlier.");
      return ConfigValidationResult.failure(warnings);
    }

    if (schemaVersion < LootLockPlayerData.CURRENT_SCHEMA_VERSION) {
      migrateFromOlder(data, schemaVersion, warnings);
    }

    data.setSchemaVersion(LootLockPlayerData.CURRENT_SCHEMA_VERSION);

    return warnings.isEmpty()
        ? ConfigValidationResult.success()
        : new ConfigValidationResult(true, warnings);
  }

  private static void migrateFromOlder(
      LootLockPlayerData data, int fromVersion, List<String> warnings) {
    int current = fromVersion;

    if (current < 1) {
      migrateV0ToV1(data);
      current = 1;
      warnings.add("Migrated from schema version " + fromVersion + " to " + current);
    }
  }

  private static void migrateV0ToV1(LootLockPlayerData data) {
    if (data.getActiveProfileId() == null && !data.getProfiles().isEmpty()) {
      data.setActiveProfileId(data.getProfiles().get(0).getId());
    }
  }
}
