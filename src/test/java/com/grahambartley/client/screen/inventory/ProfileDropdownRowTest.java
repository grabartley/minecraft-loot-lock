package com.grahambartley.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Covers the chip-versus-row click routing on {@link ProfileDropdownRow}: the chip hit box only
 * consumes clicks inside the chip rectangle, and clicks outside the chip still select the profile.
 */
class ProfileDropdownRowTest {
  private static final int X = 10;
  private static final int Y = 20;
  private static final int WIDTH = 200;

  @Test
  void isMouseOverChipHitsInsideChipRectangle() {
    ProfileDropdownRow row = newRow(() -> {}, () -> {});
    // Chip is inset 6px from the row's left edge, vertically centred, sized CHIP_SIZE px.
    int chipLeft = X + ProfileDropdownRow.CHIP_INSET_X;
    int chipTop = Y + (ProfileDropdownRow.ROW_HEIGHT - ProfileDropdownRow.CHIP_SIZE) / 2;
    assertTrue(row.isMouseOverChip(chipLeft, chipTop));
    assertTrue(row.isMouseOverChip(chipLeft + 5, chipTop + 5));
    assertTrue(
        row.isMouseOverChip(
            chipLeft + ProfileDropdownRow.CHIP_SIZE - 1,
            chipTop + ProfileDropdownRow.CHIP_SIZE - 1));
  }

  @Test
  void isMouseOverChipMissesOutsideChipRectangle() {
    ProfileDropdownRow row = newRow(() -> {}, () -> {});
    int chipLeft = X + ProfileDropdownRow.CHIP_INSET_X;
    int chipTop = Y + (ProfileDropdownRow.ROW_HEIGHT - ProfileDropdownRow.CHIP_SIZE) / 2;
    // Left of chip, above chip, right of chip, below chip.
    assertFalse(row.isMouseOverChip(chipLeft - 1, chipTop + 4));
    assertFalse(row.isMouseOverChip(chipLeft + 4, chipTop - 1));
    assertFalse(row.isMouseOverChip(chipLeft + ProfileDropdownRow.CHIP_SIZE, chipTop + 4));
    assertFalse(row.isMouseOverChip(chipLeft + 4, chipTop + ProfileDropdownRow.CHIP_SIZE));
    // Well to the right, in the name region.
    assertFalse(row.isMouseOverChip(X + WIDTH - 10, Y + ProfileDropdownRow.ROW_HEIGHT / 2));
  }

  @Test
  void clickInsideChipRoutesToChipAction() {
    AtomicInteger chipPresses = new AtomicInteger();
    AtomicInteger rowPresses = new AtomicInteger();
    ProfileDropdownRow row = newRow(rowPresses::incrementAndGet, chipPresses::incrementAndGet);

    int chipLeft = X + ProfileDropdownRow.CHIP_INSET_X;
    int chipTop = Y + (ProfileDropdownRow.ROW_HEIGHT - ProfileDropdownRow.CHIP_SIZE) / 2;
    row.onClick(chipLeft + 4, chipTop + 4);

    assertEquals(1, chipPresses.get());
    assertEquals(0, rowPresses.get());
  }

  @Test
  void clickOutsideChipKeepsRowSelectionBehaviour() {
    AtomicInteger chipPresses = new AtomicInteger();
    AtomicInteger rowPresses = new AtomicInteger();
    ProfileDropdownRow row = newRow(rowPresses::incrementAndGet, chipPresses::incrementAndGet);

    // Well past the chip, in the name region.
    row.onClick(X + WIDTH - 20, Y + ProfileDropdownRow.ROW_HEIGHT / 2);

    assertEquals(0, chipPresses.get());
    assertEquals(1, rowPresses.get());
  }

  @Test
  void successiveClicksDoNotLeakChipPressedState() {
    AtomicInteger chipPresses = new AtomicInteger();
    AtomicInteger rowPresses = new AtomicInteger();
    ProfileDropdownRow row = newRow(rowPresses::incrementAndGet, chipPresses::incrementAndGet);
    int chipLeft = X + ProfileDropdownRow.CHIP_INSET_X;
    int chipTop = Y + (ProfileDropdownRow.ROW_HEIGHT - ProfileDropdownRow.CHIP_SIZE) / 2;

    row.onClick(chipLeft + 4, chipTop + 4);
    row.onClick(X + WIDTH - 20, Y + ProfileDropdownRow.ROW_HEIGHT / 2);

    assertEquals(1, chipPresses.get(), "chip should fire once on the first click");
    assertEquals(1, rowPresses.get(), "row should fire once on the second click");
  }

  private static ProfileDropdownRow newRow(Runnable onPressAction, Runnable onChipPressAction) {
    return new ProfileDropdownRow(
        X,
        Y,
        WIDTH,
        UUID.randomUUID(),
        Palette.PROFILE_COLORS[0],
        "Test",
        "deny . 0 items",
        false,
        onPressAction,
        onChipPressAction);
  }
}
