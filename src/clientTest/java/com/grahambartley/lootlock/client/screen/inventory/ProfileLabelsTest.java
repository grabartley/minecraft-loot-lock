package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.data.RuleEntry;
import com.grahambartley.lootlock.text.LootLockTestLanguage;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ProfileLabelsTest {

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
    LootLockTestLanguage.install();
  }

  static Stream<Arguments> ruleCountCases() {
    return Stream.of(
        Arguments.of(FilterMode.DENYLIST, 1, "deny . 1 item"),
        Arguments.of(FilterMode.DENYLIST, 3, "deny . 3 items"),
        Arguments.of(FilterMode.ALLOWLIST, 1, "allow . 1 item"),
        Arguments.of(FilterMode.ALLOWLIST, 0, "allow . 0 items"));
  }

  @ParameterizedTest(name = "{0} with {1} rules -> {2}")
  @MethodSource("ruleCountCases")
  void ruleCountLabelPluralisesPerMode(FilterMode mode, int rules, String expected) {
    assertEquals(expected, ProfileLabels.ruleCountLabel(profile(mode, rules)));
  }

  private static LootLockProfile profile(FilterMode mode, int rules) {
    List<RuleEntry> entries =
        IntStream.range(0, rules).mapToObj(i -> new RuleEntry("minecraft:item_" + i)).toList();
    return new LootLockProfile(
        UUID.randomUUID(), "P", mode, RejectedItemAction.LEAVE_ON_GROUND, true, 0, entries);
  }
}
