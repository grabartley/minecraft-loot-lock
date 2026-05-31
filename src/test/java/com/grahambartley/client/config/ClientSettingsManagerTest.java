package com.grahambartley.client.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClientSettingsManagerTest {
  @TempDir Path tempDir;

  @Test
  void savesAndLoadsSettingsFromDisk() {
    Path configPath = tempDir.resolve("loot-lock-client.json");
    ClientSettingsManager manager = new ClientSettingsManager(configPath);
    manager.load();

    ClientSettings updated = manager.getSettingsCopy();
    updated.setShowBlockedHudNotification(false);
    manager.replaceAndSave(updated);

    ClientSettingsManager reloaded = new ClientSettingsManager(configPath);
    reloaded.load();
    assertFalse(reloaded.getSettingsCopy().isShowBlockedHudNotification());
  }

  @Test
  void createsDefaultFileWhenMissing() {
    Path configPath = tempDir.resolve("loot-lock-client.json");
    ClientSettingsManager manager = new ClientSettingsManager(configPath);
    manager.load();

    assertTrue(configPath.toFile().exists());
  }
}
