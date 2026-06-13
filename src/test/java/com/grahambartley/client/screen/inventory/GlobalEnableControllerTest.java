package com.grahambartley.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.client.config.ClientSettings;
import org.junit.jupiter.api.Test;

class GlobalEnableControllerTest {
  @Test
  void shouldShowToastReturnsTrueWhenSettingEnabled() {
    ClientSettings settings = ClientSettings.defaults();
    settings.setEnableToggleToast(true);

    assertTrue(GlobalEnableController.shouldShowToast(settings));
  }

  @Test
  void shouldShowToastReturnsFalseWhenSettingDisabled() {
    ClientSettings settings = ClientSettings.defaults();

    assertFalse(GlobalEnableController.shouldShowToast(settings));
  }

  @Test
  void shouldShowToastReturnsFalseWhenSettingsIsNull() {
    assertFalse(GlobalEnableController.shouldShowToast(null));
  }
}
