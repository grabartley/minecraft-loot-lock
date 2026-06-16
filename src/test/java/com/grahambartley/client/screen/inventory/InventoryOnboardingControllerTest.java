package com.grahambartley.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.client.config.ClientSettings;
import com.grahambartley.client.config.ClientSettingsManager;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class InventoryOnboardingControllerTest {
  @TempDir Path tempDir;

  static Stream<Arguments> shouldShowOnboardingCases() {
    Supplier<ClientSettings> alreadySeen =
        () -> {
          ClientSettings settings = ClientSettings.defaults();
          settings.setHasSeenOnboarding(true);
          return settings;
        };
    return Stream.of(
        Arguments.of("defaults", (Supplier<ClientSettings>) ClientSettings::defaults, true),
        Arguments.of("already-seen", alreadySeen, false),
        Arguments.of("null", (Supplier<ClientSettings>) () -> null, false));
  }

  @ParameterizedTest(name = "{0} -> {2}")
  @MethodSource("shouldShowOnboardingCases")
  void shouldShowOnboardingMatchesFlag(
      String label, Supplier<ClientSettings> settings, boolean expected) {
    assertEquals(expected, InventoryOnboardingController.shouldShowOnboarding(settings.get()));
  }

  @Test
  void resetClearsHasSeenOnboardingAndPersists() {
    ClientSettingsManager manager = newLoadedManager();
    ClientSettings seen = manager.getSettingsCopy();
    seen.setHasSeenOnboarding(true);
    manager.replaceAndSave(seen);

    InventoryOnboardingController.reset(manager);

    assertFalse(manager.getSettingsCopy().hasSeenOnboarding());

    ClientSettingsManager reloaded =
        new ClientSettingsManager(tempDir.resolve("loot-lock-client.json"));
    reloaded.load();
    assertFalse(reloaded.getSettingsCopy().hasSeenOnboarding());
  }

  @Test
  void resetIsNoOpWhenManagerNull() {
    InventoryOnboardingController.reset(null);
  }

  @Test
  void maybeShowDoesNotPersistFlagWhenAlreadySeen() {
    ClientSettingsManager manager = newLoadedManager();
    ClientSettings seen = manager.getSettingsCopy();
    seen.setHasSeenOnboarding(true);
    manager.replaceAndSave(seen);

    InventoryOnboardingController.maybeShow(null, manager);

    assertTrue(manager.getSettingsCopy().hasSeenOnboarding());
  }

  @Test
  void maybeShowEarlyReturnsForNullArgsWithoutThrowing() {
    InventoryOnboardingController.maybeShow(null, null);
    ClientSettingsManager manager = newLoadedManager();
    InventoryOnboardingController.maybeShow(null, manager);
    assertFalse(manager.getSettingsCopy().hasSeenOnboarding());
  }

  private ClientSettingsManager newLoadedManager() {
    ClientSettingsManager manager =
        new ClientSettingsManager(tempDir.resolve("loot-lock-client.json"));
    manager.load();
    return manager;
  }
}
