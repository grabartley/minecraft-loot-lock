package com.grahambartley.client.screen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SupportStateViewModelTest {
  @Test
  void unsupportedServerIsNotEditable() {
    SupportStateViewModel model = SupportStateViewModel.fromState(false, false, false);
    assertFalse(model.editable());
  }

  @Test
  void readOnlyServerDisablesEditing() {
    SupportStateViewModel model = SupportStateViewModel.fromState(true, true, false);
    assertFalse(model.editable());
  }

  @Test
  void syncedSupportedEditableStateEnablesEditing() {
    SupportStateViewModel model = SupportStateViewModel.fromState(true, true, true);
    assertTrue(model.editable());
  }
}
