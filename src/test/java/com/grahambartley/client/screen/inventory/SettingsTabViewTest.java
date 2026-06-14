package com.grahambartley.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.client.config.ClientSettings;
import com.grahambartley.client.config.ClientSettingsManager;
import java.nio.file.Path;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lwjgl.glfw.GLFW;

class SettingsTabViewTest {
  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @Test
  void keyLabelReturnsUnboundForUnknownKey() {
    KeyBinding binding =
        new KeyBinding(
            "key.loot-lock.test_unbound",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.misc");

    assertEquals("Unbound", SettingsTabView.keyLabel(binding));
  }

  @Test
  void keyLabelReturnsLocalizedNameForBoundKey() {
    KeyBinding binding =
        new KeyBinding(
            "key.loot-lock.test_bound",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "key.categories.misc");

    String label = SettingsTabView.keyLabel(binding);

    assertFalse(label.isEmpty());
    assertNotEquals("Unbound", label);
  }

  @Test
  void operatorPermissionLevelIsTwo() {
    assertEquals(2, SettingsTabView.OPERATOR_PERMISSION_LEVEL);
  }

  @Test
  void notificationTogglesRoundTripThroughClientSettingsManager(@TempDir Path tempDir) {
    Path configPath = tempDir.resolve("loot-lock-client.json");
    ClientSettingsManager manager = new ClientSettingsManager(configPath);
    manager.load();

    ClientSettings draft = manager.getSettingsCopy();
    draft.setShowBlockedHudNotification(true);
    draft.setEnableProfileCycleToast(true);
    draft.setEnableToggleToast(true);
    manager.replaceAndSave(draft);

    ClientSettingsManager reloaded = new ClientSettingsManager(configPath);
    reloaded.load();
    ClientSettings persisted = reloaded.getSettingsCopy();

    assertTrue(persisted.isShowBlockedHudNotification());
    assertTrue(persisted.isEnableProfileCycleToast());
    assertTrue(persisted.isEnableToggleToast());
  }

  @Test
  void confirmBeforeEnablingDeleteRoundTripsThroughClientSettingsManager(@TempDir Path tempDir) {
    Path configPath = tempDir.resolve("loot-lock-client.json");
    ClientSettingsManager manager = new ClientSettingsManager(configPath);
    manager.load();

    ClientSettings draft = manager.getSettingsCopy();
    draft.setConfirmBeforeEnablingDelete(false);
    manager.replaceAndSave(draft);

    ClientSettingsManager reloaded = new ClientSettingsManager(configPath);
    reloaded.load();
    assertFalse(reloaded.getSettingsCopy().isConfirmBeforeEnablingDelete());
  }
}
