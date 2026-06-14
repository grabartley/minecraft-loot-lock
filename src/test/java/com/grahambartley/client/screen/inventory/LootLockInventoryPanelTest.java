package com.grahambartley.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LootLockInventoryPanelTest {
  private LongSupplier originalClock;
  private AtomicLong now;

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @BeforeEach
  void swapClock() {
    originalClock = LootLockInventoryPanel.clockMillis;
    now = new AtomicLong(1000L);
    LootLockInventoryPanel.clockMillis = now::get;
  }

  @AfterEach
  void restoreClock() {
    LootLockInventoryPanel.clockMillis = originalClock;
  }

  @Test
  void dropArmedDefaultsToFalse() {
    LootLockInventoryPanel panel = new LootLockInventoryPanel();
    assertFalse(panel.isDropArmed());
  }

  @Test
  void setDropArmedTrueArmsTheWell() {
    LootLockInventoryPanel panel = new LootLockInventoryPanel();
    panel.setDropArmed(true);
    assertTrue(panel.isDropArmed());
  }

  @Test
  void setDropArmedFalseClearsArmedState() {
    LootLockInventoryPanel panel = new LootLockInventoryPanel();
    panel.setDropArmed(true);
    panel.setDropArmed(false);
    assertFalse(panel.isDropArmed());
  }

  @Test
  void flashIsInactiveBeforeAnyDrop() {
    LootLockInventoryPanel panel = new LootLockInventoryPanel();
    assertFalse(panel.isFlashActive());
    assertEquals(1f, panel.flashProgress());
  }

  @Test
  void flashDropSuccessActivatesFlash() {
    LootLockInventoryPanel panel = new LootLockInventoryPanel();
    panel.flashDropSuccess();
    assertTrue(panel.isFlashActive());
    assertEquals(0f, panel.flashProgress());
  }

  @Test
  void flashProgressAdvancesLinearlyOverDuration() {
    LootLockInventoryPanel panel = new LootLockInventoryPanel();
    panel.flashDropSuccess();
    now.addAndGet(LootLockInventoryPanel.FLASH_DURATION_MILLIS / 2);
    assertEquals(0.5f, panel.flashProgress(), 0.001f);
  }

  @Test
  void flashClearsExactlyAtDurationEnd() {
    LootLockInventoryPanel panel = new LootLockInventoryPanel();
    panel.flashDropSuccess();
    now.addAndGet(LootLockInventoryPanel.FLASH_DURATION_MILLIS);
    assertFalse(panel.isFlashActive());
    assertEquals(1f, panel.flashProgress());
  }

  @Test
  void flashClearsAfterDurationOverrun() {
    LootLockInventoryPanel panel = new LootLockInventoryPanel();
    panel.flashDropSuccess();
    now.addAndGet(LootLockInventoryPanel.FLASH_DURATION_MILLIS + 250L);
    assertFalse(panel.isFlashActive());
  }

  @Test
  void backToBackFlashesResetTheTimer() {
    LootLockInventoryPanel panel = new LootLockInventoryPanel();
    panel.flashDropSuccess();
    now.addAndGet(LootLockInventoryPanel.FLASH_DURATION_MILLIS - 50L);
    // Late in the first flash, fire a second drop. The window restarts from now.
    panel.flashDropSuccess();
    now.addAndGet(50L);
    assertTrue(panel.isFlashActive(), "second flash should still be running 50ms in");
    assertEquals(50f / LootLockInventoryPanel.FLASH_DURATION_MILLIS, panel.flashProgress(), 0.001f);
  }

  @Test
  void closedButtonDropSequenceLeavesPanelOpenOnRulesWithFlashActive() {
    LootLockInventoryPanel panel = new LootLockInventoryPanel();
    panel.setOpen(false);
    assertFalse(panel.isOpen());

    // Mirror the body of InventoryScreenMixin.lootlock$catchDragReleaseOverEntryButton: after
    // DragToAddRouter.route succeeds, the mixin opens, switches to Rules, clears the search, and
    // flashes.
    panel.setOpen(true);
    panel.setTab(PanelTab.RULES);
    panel.clearRulesSearch();
    panel.flashDropSuccess();

    assertTrue(panel.isOpen());
    assertEquals(PanelTab.RULES, panel.getActiveTab());
    assertTrue(panel.isFlashActive());
  }
}
