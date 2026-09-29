package com.grahambartley.lootlock.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.lootlock.data.LootLockProfile;
import java.util.ArrayList;
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

  static Stream<Arguments> canCreateProfileCases() {
    Supplier<List<LootLockProfile>> nullList = () -> null;
    Supplier<List<LootLockProfile>> empty = List::of;
    Supplier<List<LootLockProfile>> single = () -> List.of(LootLockProfile.createDefault());
    Supplier<List<LootLockProfile>> eight = () -> profiles(8);
    Supplier<List<LootLockProfile>> nine = () -> profiles(9);
    return Stream.of(
        Arguments.of("null", nullList, false),
        Arguments.of("empty", empty, true),
        Arguments.of("single profile", single, true),
        Arguments.of("8 profiles", eight, true),
        Arguments.of("9 profiles (at cap)", nine, false));
  }

  @ParameterizedTest(name = "{0} -> {2}")
  @MethodSource("canCreateProfileCases")
  void canCreateProfileRespectsMaxCap(
      String label, Supplier<List<LootLockProfile>> profiles, boolean expected) {
    assertEquals(expected, ProfileUiController.canCreateProfile(profiles.get()));
  }

  private static List<LootLockProfile> profiles(int count) {
    List<LootLockProfile> list = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      list.add(LootLockProfile.createDefault());
    }
    return list;
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
