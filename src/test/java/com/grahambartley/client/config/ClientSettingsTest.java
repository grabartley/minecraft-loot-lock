package com.grahambartley.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ClientSettingsTest {
  @Test
  void defaultsMatchUserFacingExpectations() {
    ClientSettings settings = ClientSettings.defaults();

    assertFalse(settings.isShowBlockedHudNotification());
    assertFalse(settings.isEnableProfileCycleToast());
    assertFalse(settings.isEnableToggleToast());
    assertFalse(settings.hasSeenOnboarding());
    assertTrue(settings.isConfirmBeforeEnablingDelete());
  }

  static Stream<Arguments> booleanRoundTripCases() {
    return Stream.of(
        Arguments.of(
            "showBlockedHudNotification",
            (BiConsumer<ClientSettings, Boolean>) ClientSettings::setShowBlockedHudNotification,
            (Predicate<ClientSettings>) ClientSettings::isShowBlockedHudNotification,
            true),
        Arguments.of(
            "confirmBeforeEnablingDelete",
            (BiConsumer<ClientSettings, Boolean>) ClientSettings::setConfirmBeforeEnablingDelete,
            (Predicate<ClientSettings>) ClientSettings::isConfirmBeforeEnablingDelete,
            false),
        Arguments.of(
            "enableProfileCycleToast",
            (BiConsumer<ClientSettings, Boolean>) ClientSettings::setEnableProfileCycleToast,
            (Predicate<ClientSettings>) ClientSettings::isEnableProfileCycleToast,
            true),
        Arguments.of(
            "enableToggleToast",
            (BiConsumer<ClientSettings, Boolean>) ClientSettings::setEnableToggleToast,
            (Predicate<ClientSettings>) ClientSettings::isEnableToggleToast,
            true),
        Arguments.of(
            "hasSeenOnboarding",
            (BiConsumer<ClientSettings, Boolean>) ClientSettings::setHasSeenOnboarding,
            (Predicate<ClientSettings>) ClientSettings::hasSeenOnboarding,
            true));
  }

  @ParameterizedTest(name = "{0} round-trips through copy()")
  @MethodSource("booleanRoundTripCases")
  void booleanFieldRoundTripsIndependentlyThroughCopy(
      String label,
      BiConsumer<ClientSettings, Boolean> setter,
      Predicate<ClientSettings> getter,
      boolean writtenValue) {
    ClientSettings settings = ClientSettings.defaults();
    setter.accept(settings, writtenValue);

    ClientSettings copy = settings.copy();
    assertEquals(writtenValue, getter.test(copy), label);

    setter.accept(copy, !writtenValue);
    assertEquals(writtenValue, getter.test(settings), label + " original untouched");
    assertEquals(!writtenValue, getter.test(copy), label + " copy mutated independently");
  }

  @Test
  void copyReturnsIndependentInstance() {
    ClientSettings settings = ClientSettings.defaults();
    ClientSettings copy = settings.copy();
    copy.setShowBlockedHudNotification(true);

    assertNotSame(settings, copy);
    assertFalse(settings.isShowBlockedHudNotification());
    assertTrue(copy.isShowBlockedHudNotification());
  }
}
