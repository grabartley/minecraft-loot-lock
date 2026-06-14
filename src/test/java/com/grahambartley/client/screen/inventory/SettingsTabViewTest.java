package com.grahambartley.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

  @ParameterizedTest(name = "integrated={0}, operator={1} -> readOnly={2}")
  @CsvSource({
    "true,  false, false",
    "true,  true,  false",
    "false, true,  false",
    "false, false, true",
  })
  void policySwitchReadOnlyMatrix(boolean integrated, boolean operator, boolean expected) {
    assertEquals(expected, SettingsTabView.isPolicySwitchReadOnly(integrated, operator));
  }

  @Test
  void toggleBlockedHudFlipsAndPersists(@TempDir Path tempDir) {
    ClientSettingsManager manager = freshManager(tempDir);
    boolean initial = manager.getSettingsCopy().isShowBlockedHudNotification();

    SettingsTabView.toggleBlockedHud(manager);

    assertEquals(!initial, reload(tempDir).isShowBlockedHudNotification());
  }

  @Test
  void toggleProfileCycleToastFlipsAndPersists(@TempDir Path tempDir) {
    ClientSettingsManager manager = freshManager(tempDir);
    boolean initial = manager.getSettingsCopy().isEnableProfileCycleToast();

    SettingsTabView.toggleProfileCycleToast(manager);

    assertEquals(!initial, reload(tempDir).isEnableProfileCycleToast());
  }

  @Test
  void toggleToggleToastFlipsAndPersists(@TempDir Path tempDir) {
    ClientSettingsManager manager = freshManager(tempDir);
    boolean initial = manager.getSettingsCopy().isEnableToggleToast();

    SettingsTabView.toggleToggleToast(manager);

    assertEquals(!initial, reload(tempDir).isEnableToggleToast());
  }

  @Test
  void toggleConfirmBeforeDeleteFlipsAndPersists(@TempDir Path tempDir) {
    ClientSettingsManager manager = freshManager(tempDir);
    boolean initial = manager.getSettingsCopy().isConfirmBeforeEnablingDelete();

    SettingsTabView.toggleConfirmBeforeDelete(manager);

    assertEquals(!initial, reload(tempDir).isConfirmBeforeEnablingDelete());
  }

  @Test
  void notificationTogglesAreNoOpsWhenManagerIsNull() {
    SettingsTabView.toggleBlockedHud(null);
    SettingsTabView.toggleProfileCycleToast(null);
    SettingsTabView.toggleToggleToast(null);
    SettingsTabView.toggleConfirmBeforeDelete(null);
  }

  private static ClientSettingsManager freshManager(Path tempDir) {
    ClientSettingsManager manager =
        new ClientSettingsManager(tempDir.resolve("loot-lock-client.json"));
    manager.load();
    return manager;
  }

  private static ClientSettings reload(Path tempDir) {
    ClientSettingsManager reloaded =
        new ClientSettingsManager(tempDir.resolve("loot-lock-client.json"));
    reloaded.load();
    return reloaded.getSettingsCopy();
  }
}
