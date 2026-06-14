package com.grahambartley.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.data.LootLockProfile;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProfileUiControllerTest {
  @Test
  void canDeleteRequiresMoreThanOneProfile() {
    assertFalse(ProfileUiController.canDelete(List.of(LootLockProfile.createDefault())));
    assertTrue(
        ProfileUiController.canDelete(
            List.of(LootLockProfile.createDefault(), LootLockProfile.createDefault())));
  }

  @Test
  void nextDuplicateNameAppendsNumberWhenNeeded() {
    LootLockProfile alpha = LootLockProfile.createDefault();
    alpha.setName("Alpha");
    LootLockProfile alphaTwo = LootLockProfile.createDefault();
    alphaTwo.setName("Alpha (2)");

    String candidate = ProfileUiController.nextDuplicateName(List.of(alpha, alphaTwo), "Alpha");
    assertEquals("Alpha (3)", candidate);
  }
}
