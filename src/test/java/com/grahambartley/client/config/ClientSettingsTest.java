package com.grahambartley.client.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClientSettingsTest {
  @Test
  void defaultsDisableBlockedToastAndProfileCycleToast() {
    ClientSettings settings = ClientSettings.defaults();

    assertFalse(settings.isShowBlockedHudNotification());
    assertFalse(settings.isEnableProfileCycleToast());
    assertFalse(settings.isEnableToggleToast());
  }

  @Test
  void enableToggleToastRoundTripsThroughCopy() {
    ClientSettings settings = ClientSettings.defaults();
    settings.setEnableToggleToast(true);

    ClientSettings copy = settings.copy();

    assertTrue(copy.isEnableToggleToast());
    copy.setEnableToggleToast(false);
    assertTrue(settings.isEnableToggleToast());
    assertFalse(copy.isEnableToggleToast());
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
