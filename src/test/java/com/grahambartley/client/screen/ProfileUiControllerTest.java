package com.grahambartley.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.data.LootLockProfile;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ProfileUiControllerTest {

  static Stream<Arguments> canDeleteCases() {
    Supplier<List<LootLockProfile>> single = () -> List.of(LootLockProfile.createDefault());
    Supplier<List<LootLockProfile>> two =
        () -> List.of(LootLockProfile.createDefault(), LootLockProfile.createDefault());
    Supplier<List<LootLockProfile>> empty = List::of;
    return Stream.of(
        Arguments.of("empty", empty, false),
        Arguments.of("single profile", single, false),
        Arguments.of("two profiles", two, true));
  }

  @ParameterizedTest(name = "{0} -> {2}")
  @MethodSource("canDeleteCases")
  void canDeleteRequiresMoreThanOneProfile(
      String label, Supplier<List<LootLockProfile>> profiles, boolean expected) {
    assertEquals(expected, ProfileUiController.canDelete(profiles.get()));
  }

  @Test
  void nextDuplicateNameAppendsNextAvailableNumber() {
    LootLockProfile alpha = LootLockProfile.createDefault();
    alpha.setName("Alpha");
    LootLockProfile alphaTwo = LootLockProfile.createDefault();
    alphaTwo.setName("Alpha (2)");

    assertEquals(
        "Alpha (3)", ProfileUiController.nextDuplicateName(List.of(alpha, alphaTwo), "Alpha"));
  }
}
