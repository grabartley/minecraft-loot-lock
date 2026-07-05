package com.grahambartley.lootlock.client.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
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

  @Test
  void loadsOldConfigWithRemovedFields() throws IOException {
    Path configPath = tempDir.resolve("old-loot-lock-client.json");
    String oldJson =
        "{"
            + "\"showBlockedHudNotification\": false,"
            + "\"showActionbarFallback\": true,"
            + "\"confirmBeforeEnablingDelete\": true,"
            + "\"enableProfileCycleToast\": true,"
            + "\"uiScalePercent\": 100"
            + "}";
    Files.writeString(configPath, oldJson);

    ClientSettingsManager manager =
        new ClientSettingsManager(configPath, new GsonBuilder().setPrettyPrinting().create());
    manager.load();

    ClientSettings settings = manager.getSettingsCopy();
    assertFalse(settings.isShowBlockedHudNotification());
    assertTrue(settings.isEnableProfileCycleToast());
  }

  @Test
  void loadsPreOnboardingConfigDefaultsHasSeenOnboardingToFalse() throws IOException {
    Path configPath = tempDir.resolve("v1-loot-lock-client.json");
    String preOnboardingJson =
        "{"
            + "\"showBlockedHudNotification\": false,"
            + "\"confirmBeforeEnablingDelete\": true,"
            + "\"enableProfileCycleToast\": false,"
            + "\"enableToggleToast\": false"
            + "}";
    Files.writeString(configPath, preOnboardingJson);

    ClientSettingsManager manager =
        new ClientSettingsManager(configPath, new GsonBuilder().setPrettyPrinting().create());
    manager.load();

    assertFalse(manager.getSettingsCopy().hasSeenOnboarding());
  }

  @Test
  void roundTripsHasSeenOnboardingThroughDisk() {
    Path configPath = tempDir.resolve("loot-lock-client.json");
    ClientSettingsManager manager = new ClientSettingsManager(configPath);
    manager.load();

    ClientSettings updated = manager.getSettingsCopy();
    updated.setHasSeenOnboarding(true);
    manager.replaceAndSave(updated);

    ClientSettingsManager reloaded = new ClientSettingsManager(configPath);
    reloaded.load();
    assertTrue(reloaded.getSettingsCopy().hasSeenOnboarding());
  }
}
