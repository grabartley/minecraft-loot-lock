package com.grahambartley.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

class ClientSettingsTest {
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
    copy.setShowActionbarFallback(false);

    assertNotSame(settings, copy);
    assertEquals(true, settings.isShowActionbarFallback());
  }
}
