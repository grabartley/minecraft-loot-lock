package com.grahambartley.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.data.LootLockProfile;
import java.util.List;
import java.util.UUID;
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

  @Test
  void nextProfileIdFallsBackToFirstWhenActiveMissing() {
    LootLockProfile first = LootLockProfile.createDefault();
    LootLockProfile second = LootLockProfile.createDefault();

    UUID next =
        ProfileUiController.nextProfileId(List.of(first, second), UUID.randomUUID()).orElseThrow();

    assertEquals(first.getId(), next);
  }

  @Test
  void nextProfileIdCyclesWhenActiveFound() {
    LootLockProfile first = LootLockProfile.createDefault();
    LootLockProfile second = LootLockProfile.createDefault();

    UUID next =
        ProfileUiController.nextProfileId(List.of(first, second), first.getId()).orElseThrow();

    assertEquals(second.getId(), next);
    assertTrue(ProfileUiController.nextProfileId(List.of(), first.getId()).isEmpty());
  }
}
