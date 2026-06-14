package com.grahambartley.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DeleteConfirmStripTest {
  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @Test
  void newStripStartsInactive() {
    DeleteConfirmStrip strip = new DeleteConfirmStrip();
    assertFalse(strip.isActive());
  }

  @Test
  void attachMountsButtons() {
    DeleteConfirmStrip strip = new DeleteConfirmStrip();
    List<ClickableWidget> mounted = new ArrayList<>();
    strip.attach(mounted::add, () -> {}, () -> {});

    assertTrue(mounted.size() >= 2, "expected at least two button widgets attached");
    for (ClickableWidget widget : mounted) {
      assertNotNull(widget);
    }
  }

  @Test
  void setActiveTrueMarksStripActiveAndShowsButtons() {
    DeleteConfirmStrip strip = new DeleteConfirmStrip();
    List<ClickableWidget> mounted = new ArrayList<>();
    strip.attach(mounted::add, () -> {}, () -> {});

    strip.setActive(true);

    assertTrue(strip.isActive());
    for (ClickableWidget widget : mounted) {
      assertTrue(widget.visible);
      assertTrue(widget.active);
    }
  }

  @Test
  void setActiveFalseHidesAndDeactivatesButtons() {
    DeleteConfirmStrip strip = new DeleteConfirmStrip();
    List<ClickableWidget> mounted = new ArrayList<>();
    strip.attach(mounted::add, () -> {}, () -> {});
    strip.setActive(true);

    strip.setActive(false);

    assertFalse(strip.isActive());
    for (ClickableWidget widget : mounted) {
      assertFalse(widget.visible);
      assertFalse(widget.active);
    }
  }

  @Test
  void confirmButtonFiresConfirmCallback() {
    AtomicBoolean confirmed = new AtomicBoolean();
    AtomicBoolean cancelled = new AtomicBoolean();
    DeleteConfirmStrip strip = new DeleteConfirmStrip();
    List<ClickableWidget> mounted = new ArrayList<>();
    strip.attach(mounted::add, () -> confirmed.set(true), () -> cancelled.set(true));
    strip.setActive(true);

    fireFirstButtonByMessage(mounted, "Enable delete");

    assertTrue(confirmed.get());
    assertFalse(cancelled.get());
  }

  @Test
  void cancelButtonFiresCancelCallback() {
    AtomicBoolean confirmed = new AtomicBoolean();
    AtomicBoolean cancelled = new AtomicBoolean();
    DeleteConfirmStrip strip = new DeleteConfirmStrip();
    List<ClickableWidget> mounted = new ArrayList<>();
    strip.attach(mounted::add, () -> confirmed.set(true), () -> cancelled.set(true));
    strip.setActive(true);

    fireFirstButtonByMessage(mounted, "Cancel");

    assertTrue(cancelled.get());
    assertFalse(confirmed.get());
  }

  private static void fireFirstButtonByMessage(List<ClickableWidget> widgets, String message) {
    for (ClickableWidget widget : widgets) {
      if (message.equals(widget.getMessage().getString())) {
        widget.onClick(widget.getX() + 1, widget.getY() + 1);
        return;
      }
    }
    throw new AssertionError("No widget found with message: " + message);
  }
}
