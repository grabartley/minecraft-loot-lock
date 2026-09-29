package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.grahambartley.lootlock.client.LootLockClient;
import com.grahambartley.lootlock.client.config.ClientSettings;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GlobalEnableControllerTest {
  @BeforeEach
  void resetSharedState() {
    LootLockClient.getState().clear();
  }

  static Stream<Arguments> shouldShowToastCases() {
    Supplier<ClientSettings> toastEnabled =
        () -> {
          ClientSettings settings = ClientSettings.defaults();
          settings.setEnableToggleToast(true);
          return settings;
        };
    return Stream.of(
        Arguments.of("toast-enabled", toastEnabled, true),
        Arguments.of("defaults", (Supplier<ClientSettings>) ClientSettings::defaults, false),
        Arguments.of("null", (Supplier<ClientSettings>) () -> null, false));
  }

  @ParameterizedTest(name = "{0} -> {2}")
  @MethodSource("shouldShowToastCases")
  void shouldShowToastMatchesSetting(
      String label, Supplier<ClientSettings> settings, boolean expected) {
    assertEquals(expected, GlobalEnableController.shouldShowToast(settings.get()));
  }

  @Test
  void toggleReturnsFalseWhenSnapshotIsEmpty() {
    assertFalse(GlobalEnableController.toggle(null));
  }
}
