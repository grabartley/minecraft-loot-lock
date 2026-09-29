package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.lootlock.client.LootLockClient;
import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.data.RuleEntry;
import com.grahambartley.lootlock.text.LootLockTestLanguage;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ProfileDropdownTest {

  private final AtomicBoolean panelOpen = new AtomicBoolean(true);
  private final AtomicInteger openChanges = new AtomicInteger();
  private ProfileDropdown dropdown;

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
    LootLockTestLanguage.install();
  }

  @BeforeEach
  void newDropdown() {
    LootLockClient.getState().clear();
    dropdown = new ProfileDropdown(panelOpen::get, openChanges::incrementAndGet);
    dropdown.setAnchor(10, 40, 240);
  }

  @AfterEach
  void clearState() {
    LootLockClient.getState().clear();
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
    assertEquals(expected, ProfileDropdown.ruleCountLabel(profile(mode, rules)));
  }

  @Test
  void startsClosed() {
    assertFalse(dropdown.isOpen());
    assertFalse(dropdown.isInlineRenameActive());
  }

  @Test
  void toggleOpensThenClosesAndNotifiesEachTime() {
    dropdown.toggle();
    assertTrue(dropdown.isOpen());

    dropdown.toggle();
    assertFalse(dropdown.isOpen());

    assertEquals(2, openChanges.get());
  }

  @Test
  void emptyDropdownFrameEndsBelowItsTwoFooterButtons() {
    dropdown.open();

    assertEquals(97, dropdown.frameBottom());
  }

  @Test
  void clicksAreIgnoredWhileClosed() {
    assertFalse(dropdown.handleMouseClick(20, 60, 0));
  }

  @Test
  void clicksAreIgnoredWhilePanelIsClosed() {
    dropdown.open();
    panelOpen.set(false);

    assertFalse(dropdown.handleMouseClick(20, 60, 0));
  }

  @Test
  void clickOutsideFrameClosesDropdownAndPassesThrough() {
    dropdown.open();

    assertFalse(dropdown.handleMouseClick(500, 500, 0));
    assertFalse(dropdown.isOpen());
  }

  @Test
  void clickInsideFrameIsConsumed() {
    dropdown.open();

    assertTrue(dropdown.handleMouseClick(12, 42, 0));
    assertTrue(dropdown.isOpen());
  }

  @Test
  void renameKeysAreIgnoredWithoutActiveRename() {
    assertFalse(dropdown.handleInlineRenameKey(257, 0, 0));
    assertFalse(dropdown.handleInlineRenameChar('a', 0));
  }

  private static LootLockProfile profile(FilterMode mode, int rules) {
    List<RuleEntry> entries =
        IntStream.range(0, rules).mapToObj(i -> new RuleEntry("minecraft:item_" + i)).toList();
    return new LootLockProfile(
        UUID.randomUUID(), "P", mode, RejectedItemAction.LEAVE_ON_GROUND, true, 0, entries);
  }
}
