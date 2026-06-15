package com.grahambartley.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.grahambartley.client.config.ClientSettings;
import com.grahambartley.client.config.ClientSettingsManager;
import com.grahambartley.text.LootLockLang;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.lwjgl.glfw.GLFW;

class SettingsTabViewTest {

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
    com.grahambartley.text.LootLockTestLanguage.install();
  }

  @Test
  void keyLabelReturnsUnboundForUnknownKey() {
    KeyBinding binding = newBinding("test_unbound", GLFW.GLFW_KEY_UNKNOWN);

    Text label = SettingsTabView.keyLabel(binding);

    assertEquals(
        LootLockLang.SETTINGS_CONTROLS_UNBOUND,
        ((TranslatableTextContent) label.getContent()).getKey());
  }

  @Test
  void keyLabelReturnsLocalizedNameForBoundKey() {
    KeyBinding binding = newBinding("test_bound", GLFW.GLFW_KEY_P);

    Text label = SettingsTabView.keyLabel(binding);

    if (label.getContent() instanceof TranslatableTextContent translatable) {
      assertNotEquals(LootLockLang.SETTINGS_CONTROLS_UNBOUND, translatable.getKey());
    }
    assertFalse(label.getString().isEmpty());
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

  @Test
  void sectionLabelsIncludeServerPolicyOnlyWhenFlagIsTrue() {
    assertEquals(
        List.of(
            LootLockLang.SETTINGS_SECTION_NOTIFICATIONS,
            LootLockLang.SETTINGS_SECTION_SAFETY,
            LootLockLang.SETTINGS_SECTION_SERVER_POLICY,
            LootLockLang.SETTINGS_SECTION_CONTROLS,
            LootLockLang.SETTINGS_SECTION_ABOUT),
        SettingsTabView.sectionLabels(true));
    assertEquals(
        List.of(
            LootLockLang.SETTINGS_SECTION_NOTIFICATIONS,
            LootLockLang.SETTINGS_SECTION_SAFETY,
            LootLockLang.SETTINGS_SECTION_CONTROLS,
            LootLockLang.SETTINGS_SECTION_ABOUT),
        SettingsTabView.sectionLabels(false));
  }

  @ParameterizedTest(name = "aboutBody(showServerPolicy={0}) returns mode-specific copy")
  @ValueSource(booleans = {true, false})
  void aboutBodyVariesByMode(boolean showServerPolicy) {
    String body = SettingsTabView.aboutBody(showServerPolicy);
    String expected =
        showServerPolicy
            ? SettingsTabView.IN_WORLD_ABOUT_BODY
            : SettingsTabView.CLIENT_PREFS_ABOUT_BODY;
    assertEquals(expected, body);
  }

  @Test
  void inWorldAndClientPrefsAboutBodiesDiffer() {
    assertNotEquals(SettingsTabView.IN_WORLD_ABOUT_BODY, SettingsTabView.CLIENT_PREFS_ABOUT_BODY);
  }

  @Test
  void attachConstructsPolicySwitchByDefault() {
    SettingsTabView view = new SettingsTabView();
    Consumer<ClickableWidget> noopAdd = w -> {};

    view.attach(null, noopAdd);

    assertNotNull(view.policySwitchForTest());
  }

  @Test
  void attachSkipsPolicySwitchWhenServerPolicyHidden() {
    SettingsTabView view = new SettingsTabView();
    view.setShowServerPolicy(false);
    Consumer<ClickableWidget> noopAdd = w -> {};

    view.attach(null, noopAdd);

    assertNull(view.policySwitchForTest());
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
