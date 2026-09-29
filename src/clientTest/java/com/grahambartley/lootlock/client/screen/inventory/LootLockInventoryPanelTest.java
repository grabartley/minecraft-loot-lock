package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.lootlock.client.LootLockClient;
import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.stream.Stream;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class LootLockInventoryPanelTest {

  private LongSupplier originalClock;
  private AtomicLong now;
  private LootLockInventoryPanel panel;

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @BeforeEach
  void swapStaticsAndPanel() {
    originalClock = LootLockInventoryPanel.clockMillis;
    now = new AtomicLong(1000L);
    LootLockInventoryPanel.clockMillis = now::get;
    LootLockClient.getState().clear();
    panel = new LootLockInventoryPanel();
  }

  @AfterEach
  void restoreStatics() {
    LootLockInventoryPanel.clockMillis = originalClock;
    LootLockClient.getState().clear();
  }

  @Test
  void dropArmedDefaultsToFalse() {
    assertFalse(panel.isDropArmed());
  }

  @ParameterizedTest(name = "setDropArmed({0}) reflects in isDropArmed")
  @ValueSource(booleans = {true, false})
  void setDropArmedPropagatesValue(boolean state) {
    panel.setDropArmed(state);

    assertEquals(state, panel.isDropArmed());
  }

  @Test
  void setDropArmedClearsAfterPreviouslyArmed() {
    panel.setDropArmed(true);
    panel.setDropArmed(false);

    assertFalse(panel.isDropArmed());
  }

  @Test
  void flashIsInactiveBeforeAnyDrop() {
    assertFalse(panel.isFlashActive());
    assertEquals(1f, panel.flashProgress());
  }

  @Test
  void flashDropSuccessActivatesFlash() {
    panel.flashDropSuccess();

    assertTrue(panel.isFlashActive());
    assertEquals(0f, panel.flashProgress());
  }

  @Test
  void flashProgressAdvancesLinearlyOverDuration() {
    panel.flashDropSuccess();
    now.addAndGet(LootLockInventoryPanel.FLASH_DURATION_MILLIS / 2);

    assertEquals(0.5f, panel.flashProgress(), 0.001f);
  }

  @Test
  void flashClearsExactlyAtDurationEnd() {
    panel.flashDropSuccess();
    now.addAndGet(LootLockInventoryPanel.FLASH_DURATION_MILLIS);

    assertFalse(panel.isFlashActive());
    assertEquals(1f, panel.flashProgress());
  }

  @Test
  void flashClearsAfterDurationOverrun() {
    panel.flashDropSuccess();
    now.addAndGet(LootLockInventoryPanel.FLASH_DURATION_MILLIS + 250L);

    assertFalse(panel.isFlashActive());
  }

  @Test
  void backToBackFlashesResetTheTimer() {
    panel.flashDropSuccess();
    now.addAndGet(LootLockInventoryPanel.FLASH_DURATION_MILLIS - 50L);
    panel.flashDropSuccess();
    now.addAndGet(50L);

    assertTrue(panel.isFlashActive());
    assertEquals(50f / LootLockInventoryPanel.FLASH_DURATION_MILLIS, panel.flashProgress(), 0.001f);
  }

  @Test
  void closedButtonDropSequenceLeavesPanelOpenOnRulesWithFlashActive() {
    panel.setOpen(false);

    panel.setOpen(true);
    panel.setTab(PanelTab.RULES);
    panel.clearRulesSearch();
    panel.flashDropSuccess();

    assertTrue(panel.isOpen());
    assertEquals(PanelTab.RULES, panel.getActiveTab());
    assertTrue(panel.isFlashActive());
  }

  @Test
  void clientPrefsModeAttachOmitsPerWorldWidgets() {
    panel.setClientPrefsMode(true);
    List<ClickableWidget> collected = new ArrayList<>();

    panel.attach(null, 0, 0, collected::add);

    assertFalse(
        collected.stream().anyMatch(w -> w instanceof ProfilePill),
        "ProfilePill should not be created in client-prefs mode");
    assertFalse(
        collected.stream().anyMatch(w -> w instanceof SegmentedButton),
        "SegmentedButton (mode/action) should not be created in client-prefs mode");
    assertFalse(
        collected.stream().anyMatch(w -> w instanceof PanelTabButton),
        "PanelTabButton (tab row) should not be created in client-prefs mode");
    for (ClickableWidget widget : collected) {
      assertTrue(
          widget instanceof OnOffButton,
          "Only notification + safety switches should remain, got "
              + widget.getClass().getSimpleName());
    }
    assertEquals(
        4,
        collected.size(),
        "Expected 3 notification switches + 1 safety switch in client-prefs mode");
  }

  @Test
  void setClientPrefsModeAfterAttachThrows() {
    panel.setClientPrefsMode(true);
    panel.attach(null, 0, 0, w -> {});

    assertThrows(IllegalStateException.class, () -> panel.setClientPrefsMode(false));
  }

  @Test
  void inlineRenameInactiveByDefault() {
    assertFalse(panel.isInlineRenameActive());
  }

  @ParameterizedTest(
      name = "handleInlineRenameKey(keyCode={0}) returns false when no rename is active")
  @ValueSource(ints = {69, 257, 256, 263, 259})
  void handleInlineRenameKeyReturnsFalseWhenInactive(int keyCode) {
    assertFalse(panel.handleInlineRenameKey(keyCode, 0, 0));
  }

  @Test
  void handleInlineRenameCharReturnsFalseWhenInactive() {
    assertFalse(panel.handleInlineRenameChar('e', 0));
  }

  static Stream<Arguments> summaryAccentCases() {
    return Stream.of(
        Arguments.of("disabled overrides profile", false, FilterMode.ALLOWLIST, Palette.LEAVE),
        Arguments.of("no active profile", true, null, Palette.SLOT_LO),
        Arguments.of("allowlist", true, FilterMode.ALLOWLIST, Palette.ALLOW),
        Arguments.of("denylist", true, FilterMode.DENYLIST, Palette.DENY));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("summaryAccentCases")
  void summaryAccentFollowsEnabledStateAndMode(
      String label, boolean enabled, FilterMode mode, int expected) {
    LootLockProfile profile = mode == null ? null : newProfile(mode);

    assertEquals(expected, LootLockInventoryPanel.summaryAccent(enabled, profile));
  }

  static Stream<Arguments> blendCases() {
    return Stream.of(
        Arguments.of(0f, 0xFF000000, 0xFFFFFFFF, 0xFF000000),
        Arguments.of(1f, 0xFF000000, 0xFFFFFFFF, 0xFFFFFFFF),
        Arguments.of(0.5f, 0xFF000000, 0xFFFFFFFF, 0xFF7F7F7F),
        Arguments.of(0.5f, 0x00204060, 0xFF204060, 0x7F204060));
  }

  @ParameterizedTest(name = "blend(0x{1}, 0x{2}, {0})")
  @MethodSource("blendCases")
  void blendArgbInterpolatesEachChannel(float t, int from, int to, int expected) {
    assertEquals(expected, LootLockInventoryPanel.blendArgb(from, to, t));
  }

  private static LootLockProfile newProfile(FilterMode mode) {
    return new LootLockProfile(
        UUID.randomUUID(), "Profile", mode, RejectedItemAction.LEAVE_ON_GROUND, true, 0, List.of());
  }
}
