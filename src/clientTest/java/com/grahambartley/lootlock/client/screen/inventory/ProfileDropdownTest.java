package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.lootlock.client.LootLockClient;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProfileDropdownTest {

  private final AtomicBoolean panelOpen = new AtomicBoolean(true);
  private final AtomicInteger openChanges = new AtomicInteger();
  private ProfileDropdown dropdown;

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
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

  @ParameterizedTest(name = "active={0} -> tinted={0}")
  @ValueSource(booleans = {true, false})
  void tintWhenActiveOnlyColoursLiveButtons(boolean active) {
    ButtonWidget button = ButtonWidget.builder(Text.literal("X"), b -> {}).build();
    button.active = active;

    ProfileDropdown.tintWhenActive(button, Formatting.RED);

    assertEquals(
        active ? TextColor.fromFormatting(Formatting.RED) : null,
        button.getMessage().getStyle().getColor());
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
}
