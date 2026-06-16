package com.grahambartley.client.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.client.config.ClientSettings;
import com.grahambartley.client.config.ClientSettingsManager;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LootLockClientCommandsTest {
  @TempDir Path tempDir;

  @Test
  void resetOnboardingReturnsFalseWhenManagerNull() {
    assertFalse(LootLockClientCommands.resetOnboarding(null));
  }

  @Test
  void resetOnboardingClearsFlagAndReturnsTrue() {
    ClientSettingsManager manager =
        new ClientSettingsManager(tempDir.resolve("loot-lock-client.json"));
    manager.load();
    ClientSettings seen = manager.getSettingsCopy();
    seen.setHasSeenOnboarding(true);
    manager.replaceAndSave(seen);

    assertTrue(LootLockClientCommands.resetOnboarding(manager));
    assertFalse(manager.getSettingsCopy().hasSeenOnboarding());
  }
}
