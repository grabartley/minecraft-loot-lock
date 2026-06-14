package com.grahambartley.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.grahambartley.client.config.ClientSettings;
import com.grahambartley.client.config.ClientSettingsManager;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.lwjgl.glfw.GLFW;

class SettingsTabViewTest {

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @Test
  void keyLabelReturnsUnboundForUnknownKey() {
    KeyBinding binding = newBinding("test_unbound", GLFW.GLFW_KEY_UNKNOWN);

    assertEquals("Unbound", SettingsTabView.keyLabel(binding));
  }

  @Test
  void keyLabelReturnsLocalizedNameForBoundKey() {
    KeyBinding binding = newBinding("test_bound", GLFW.GLFW_KEY_P);

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

  static Stream<Arguments> toggleCases() {
    Consumer<ClientSettingsManager> blockedHud = SettingsTabView::toggleBlockedHud;
    Consumer<ClientSettingsManager> profileCycleToast = SettingsTabView::toggleProfileCycleToast;
    Consumer<ClientSettingsManager> toggleToast = SettingsTabView::toggleToggleToast;
    Consumer<ClientSettingsManager> confirmBeforeDelete =
        SettingsTabView::toggleConfirmBeforeDelete;
    Predicate<ClientSettings> isBlockedHud = ClientSettings::isShowBlockedHudNotification;
    Predicate<ClientSettings> isProfileCycleToast = ClientSettings::isEnableProfileCycleToast;
    Predicate<ClientSettings> isToggleToast = ClientSettings::isEnableToggleToast;
    Predicate<ClientSettings> isConfirmBeforeDelete = ClientSettings::isConfirmBeforeEnablingDelete;
    return Stream.of(
        Arguments.of("blockedHud", blockedHud, isBlockedHud),
        Arguments.of("profileCycleToast", profileCycleToast, isProfileCycleToast),
        Arguments.of("toggleToast", toggleToast, isToggleToast),
        Arguments.of("confirmBeforeDelete", confirmBeforeDelete, isConfirmBeforeDelete));
  }

  @ParameterizedTest(name = "toggle({0}) flips persisted setting")
  @MethodSource("toggleCases")
  void toggleFlipsAndPersistsSetting(
      String label,
      Consumer<ClientSettingsManager> toggle,
      Predicate<ClientSettings> reader,
      @TempDir Path tempDir) {
    ClientSettingsManager manager = freshManager(tempDir);
    boolean initial = reader.test(manager.getSettingsCopy());

    toggle.accept(manager);

    assertEquals(!initial, reader.test(reload(tempDir)));
  }

  @Test
  void notificationTogglesAreNoOpsWhenManagerIsNull() {
    SettingsTabView.toggleBlockedHud(null);
    SettingsTabView.toggleProfileCycleToast(null);
    SettingsTabView.toggleToggleToast(null);
    SettingsTabView.toggleConfirmBeforeDelete(null);
  }

  private static KeyBinding newBinding(String name, int key) {
    return new KeyBinding(
        "key.loot-lock." + name, InputUtil.Type.KEYSYM, key, "key.categories.misc");
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
