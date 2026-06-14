package com.grahambartley.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ProfileDropdownRowTest {

  private static final int X = 10;
  private static final int Y = 20;
  private static final int WIDTH = 200;
  private static final int CHIP_LEFT = X + ProfileDropdownRow.CHIP_INSET_X;
  private static final int CHIP_TOP =
      Y + (ProfileDropdownRow.ROW_HEIGHT - ProfileDropdownRow.CHIP_SIZE) / 2;
  private static final int CHIP_END = ProfileDropdownRow.CHIP_SIZE - 1;

  static Stream<Arguments> chipInsidePoints() {
    return Stream.of(
        Arguments.of("top-left corner", CHIP_LEFT, CHIP_TOP),
        Arguments.of("middle", CHIP_LEFT + 5, CHIP_TOP + 5),
        Arguments.of("bottom-right corner", CHIP_LEFT + CHIP_END, CHIP_TOP + CHIP_END));
  }

  static Stream<Arguments> chipOutsidePoints() {
    return Stream.of(
        Arguments.of("left of chip", CHIP_LEFT - 1, CHIP_TOP + 4),
        Arguments.of("above chip", CHIP_LEFT + 4, CHIP_TOP - 1),
        Arguments.of("right of chip", CHIP_LEFT + ProfileDropdownRow.CHIP_SIZE, CHIP_TOP + 4),
        Arguments.of("below chip", CHIP_LEFT + 4, CHIP_TOP + ProfileDropdownRow.CHIP_SIZE),
        Arguments.of("name region", X + WIDTH - 10, Y + ProfileDropdownRow.ROW_HEIGHT / 2));
  }

  @ParameterizedTest(name = "{0} is inside chip")
  @MethodSource("chipInsidePoints")
  void isMouseOverChipHitsInsideChipRectangle(String label, int mouseX, int mouseY) {
    assertTrue(newRow(() -> {}, () -> {}).isMouseOverChip(mouseX, mouseY));
  }

  @ParameterizedTest(name = "{0} is outside chip")
  @MethodSource("chipOutsidePoints")
  void isMouseOverChipMissesOutsideChipRectangle(String label, int mouseX, int mouseY) {
    assertFalse(newRow(() -> {}, () -> {}).isMouseOverChip(mouseX, mouseY));
  }

  @Test
  void clickInsideChipRoutesToChipAction() {
    AtomicInteger chipPresses = new AtomicInteger();
    AtomicInteger rowPresses = new AtomicInteger();
    ProfileDropdownRow row = newRow(rowPresses::incrementAndGet, chipPresses::incrementAndGet);

    row.onClick(CHIP_LEFT + 4, CHIP_TOP + 4);

    assertEquals(1, chipPresses.get());
    assertEquals(0, rowPresses.get());
  }

  @Test
  void clickOutsideChipKeepsRowSelectionBehaviour() {
    AtomicInteger chipPresses = new AtomicInteger();
    AtomicInteger rowPresses = new AtomicInteger();
    ProfileDropdownRow row = newRow(rowPresses::incrementAndGet, chipPresses::incrementAndGet);

    row.onClick(X + WIDTH - 20, Y + ProfileDropdownRow.ROW_HEIGHT / 2);

    assertEquals(0, chipPresses.get());
    assertEquals(1, rowPresses.get());
  }

  @Test
  void successiveClicksDoNotLeakChipPressedState() {
    AtomicInteger chipPresses = new AtomicInteger();
    AtomicInteger rowPresses = new AtomicInteger();
    ProfileDropdownRow row = newRow(rowPresses::incrementAndGet, chipPresses::incrementAndGet);

    row.onClick(CHIP_LEFT + 4, CHIP_TOP + 4);
    row.onClick(X + WIDTH - 20, Y + ProfileDropdownRow.ROW_HEIGHT / 2);

    assertEquals(1, chipPresses.get());
    assertEquals(1, rowPresses.get());
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
