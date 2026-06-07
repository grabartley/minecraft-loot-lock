package com.grahambartley.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
  }

  @Test
  void uiScaleIsClampedToExpectedBounds() {
    ClientSettings settings = ClientSettings.defaults();
    settings.setUiScalePercent(20);
    assertEquals(80, settings.getUiScalePercent());

    settings.setUiScalePercent(999);
    assertEquals(140, settings.getUiScalePercent());
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
