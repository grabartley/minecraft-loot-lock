package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
  void swapClockAndPanel() {
    originalClock = LootLockInventoryPanel.clockMillis;
    now = new AtomicLong(1000L);
    LootLockInventoryPanel.clockMillis = now::get;
    panel = new LootLockInventoryPanel();
  }

  @AfterEach
  void restoreClock() {
    LootLockInventoryPanel.clockMillis = originalClock;
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
        collected.stream().anyMatch(w -> w instanceof NavArrowButton),
        "NavArrowButton should not be created in client-prefs mode");
    assertFalse(
        collected.stream().anyMatch(w -> w instanceof SegmentedButton),
        "SegmentedButton (mode/action) should not be created in client-prefs mode");
    assertFalse(
        collected.stream().anyMatch(w -> w instanceof VanillaTab),
        "VanillaTab (tab row) should not be created in client-prefs mode");
    for (ClickableWidget widget : collected) {
      assertTrue(
          widget instanceof VanillaSwitch,
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
}
