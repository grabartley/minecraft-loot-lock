package com.grahambartley.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.client.config.ClientSettings;
import com.grahambartley.client.config.ClientSettingsManager;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class InventoryOnboardingControllerTest {
  @TempDir Path tempDir;

  @AfterEach
  void restoreDefaultDispatcher() {
    InventoryOnboardingController.dispatcher = InventoryOnboardingController.DEFAULT_DISPATCHER;
  }

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
  void maybeShowPersistsFlagThenDispatchesToast() {
    ClientSettingsManager manager = newLoadedManager();
    List<Boolean> flagAtDispatchTime = new ArrayList<>();
    InventoryOnboardingController.dispatcher =
        (title, body) -> flagAtDispatchTime.add(manager.getSettingsCopy().hasSeenOnboarding());

    InventoryOnboardingController.maybeShow(manager);

    assertTrue(manager.getSettingsCopy().hasSeenOnboarding(), "flag persists on disk path");
    assertEquals(
        List.of(true), flagAtDispatchTime, "flag is true by the time the toast dispatches");
  }

  @Test
  void maybeShowDispatchesOnceEvenIfCalledAgain() {
    ClientSettingsManager manager = newLoadedManager();
    List<Text> dispatched = new ArrayList<>();
    InventoryOnboardingController.dispatcher = (title, body) -> dispatched.add(body);

    InventoryOnboardingController.maybeShow(manager);
    InventoryOnboardingController.maybeShow(manager);

    assertEquals(1, dispatched.size());
  }

  @Test
  void maybeShowDoesNotPersistFlagWhenAlreadySeen() {
    ClientSettingsManager manager = newLoadedManager();
    ClientSettings seen = manager.getSettingsCopy();
    seen.setHasSeenOnboarding(true);
    manager.replaceAndSave(seen);
    InventoryOnboardingController.dispatcher =
        (title, body) -> {
          throw new AssertionError("dispatcher must not fire when already seen");
        };

    InventoryOnboardingController.maybeShow(manager);

    assertTrue(manager.getSettingsCopy().hasSeenOnboarding());
  }

  @Test
  void maybeShowEarlyReturnsForNullManagerWithoutThrowing() {
    InventoryOnboardingController.dispatcher =
        (title, body) -> {
          throw new AssertionError("dispatcher must not fire on null manager");
        };
    InventoryOnboardingController.maybeShow(null);
  }

  @Test
  void maybeShowSurvivesDispatcherThrowingButStillPersists() {
    ClientSettingsManager manager = newLoadedManager();
    InventoryOnboardingController.dispatcher =
        (title, body) -> {
          throw new RuntimeException("simulated toast failure");
        };

    try {
      InventoryOnboardingController.maybeShow(manager);
    } catch (RuntimeException ignored) {
    }

    assertTrue(
        manager.getSettingsCopy().hasSeenOnboarding(),
        "flag persisted before dispatcher ran, so a thrown toast does not re-prompt next open");
  }

  private ClientSettingsManager newLoadedManager() {
    ClientSettingsManager manager =
        new ClientSettingsManager(tempDir.resolve("loot-lock-client.json"));
    manager.load();
    return manager;
  }

  @Test
  void shouldShowOnboardingFalseAfterFlagSet() {
    ClientSettings settings = ClientSettings.defaults();
    assertTrue(InventoryOnboardingController.shouldShowOnboarding(settings));
    settings.setHasSeenOnboarding(true);
    assertFalse(InventoryOnboardingController.shouldShowOnboarding(settings));
  }
}
