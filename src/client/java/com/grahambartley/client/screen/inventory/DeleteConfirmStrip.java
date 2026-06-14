package com.grahambartley.client.screen.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/**
 * Inline safety strip rendered inside the docked Loot Lock panel when the user picks Delete under
 * Action and {@code ClientSettings.confirmBeforeEnablingDelete} is true. Replaces the legacy {@code
 * ConfirmScreen} redirect so the user stays in the inventory while approving the action.
 *
 * <p>Layout matches {@code ux_redesign_2/shots/05-after.png}:
 *
 * <ul>
 *   <li>4px red left accent ({@link Palette#DENY}).
 *   <li>Dark red fill ({@code #3A2422}).
 *   <li>Body sentence with the red-tinted "Delete mode" lead.
 *   <li>"Enable delete" red button and a "Cancel" plain button.
 *   <li>Dim "Server operators can disable delete mode for everyone." note below the buttons.
 * </ul>
 */
public final class DeleteConfirmStrip {
  /** Background color matching the prototype {@code .confirm-strip} fill. */
  static final int FILL = 0xFF3A2422;

  /** Top body text color matching the prototype {@code #f0d8d4}. */
  static final int BODY_INK = 0xFFF0D8D4;

  /** Secondary note color matching the prototype {@code #caa}. */
  static final int NOTE_INK = 0xFFCCAAAA;

  /** Strip-internal padding in GUI pixels. */
  static final int PAD = 6;

  /** Width of the colored left-accent bar. */
  static final int ACCENT_WIDTH = 3;

  /** Fixed strip height when active. */
  public static final int HEIGHT = 60;

  /** Inner button height. */
  static final int BUTTON_HEIGHT = 14;

  private final List<ClickableWidget> widgets = new ArrayList<>();
  private ButtonWidget enableButton;
  private ButtonWidget cancelButton;

  private int x;
  private int y;
  private int width;
  private boolean active;

  public void attach(
      Consumer<ClickableWidget> addDrawableChild, Runnable onConfirm, Runnable onCancel) {
    widgets.clear();
    enableButton =
        ButtonWidget.builder(Text.literal("Enable delete"), button -> onConfirm.run())
            .dimensions(0, 0, 10, BUTTON_HEIGHT)
            .build();
    cancelButton =
        ButtonWidget.builder(Text.literal("Cancel"), button -> onCancel.run())
            .dimensions(0, 0, 10, BUTTON_HEIGHT)
            .build();
    addDrawableChild.accept(enableButton);
    addDrawableChild.accept(cancelButton);
    widgets.add(enableButton);
    widgets.add(cancelButton);
    applyVisibility();
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
    applyVisibility();
  }

  public void setPosition(int x, int y, int width) {
    this.x = x;
    this.y = y;
    this.width = width;
    if (enableButton == null || cancelButton == null) {
      return;
    }
    int buttonsY = y + HEIGHT - PAD - BUTTON_HEIGHT - 12;
    int contentLeft = x + ACCENT_WIDTH + PAD;
    int enableWidth = 64;
    int cancelWidth = 44;
    enableButton.setPosition(contentLeft, buttonsY);
    enableButton.setWidth(enableWidth);
    cancelButton.setPosition(contentLeft + enableWidth + 4, buttonsY);
    cancelButton.setWidth(cancelWidth);
  }

  public void paint(DrawContext context, int mouseX, int mouseY, float delta) {
    if (!active) {
      return;
    }
    context.fill(x, y, x + width, y + HEIGHT, FILL);
    context.fill(x, y, x + ACCENT_WIDTH, y + HEIGHT, Palette.DENY);
    context.fill(x, y, x + width, y + 1, Palette.WELL_HI);
    context.fill(x, y + HEIGHT - 1, x + width, y + HEIGHT, Palette.WELL_LO);
    context.fill(x + width - 1, y, x + width, y + HEIGHT, Palette.WELL_LO);

    MinecraftClient client = MinecraftClient.getInstance();
    int textX = x + ACCENT_WIDTH + PAD;
    int textTopY = y + PAD;
    int textWidth = width - ACCENT_WIDTH - PAD * 2;

    // The red-tinted "Delete mode" lead and the rest of the body's first line render side by side
    // because drawTextWrapped doesn't support inline color spans; the remaining sentence wraps on
    // the next line.
    context.drawText(
        client.textRenderer, Text.literal("Delete mode"), textX, textTopY, Palette.DENY_BR, false);
    int afterLeadX = textX + client.textRenderer.getWidth("Delete mode ");
    context.drawText(
        client.textRenderer,
        Text.literal("permanently destroys every"),
        afterLeadX,
        textTopY,
        BODY_INK,
        false);
    context.drawTextWrapped(
        client.textRenderer,
        Text.literal("rejected item, it will not drop on the ground. This can't be undone."),
        textX,
        textTopY + 10,
        textWidth,
        BODY_INK);

    int noteY = y + HEIGHT - PAD - 8;
    context.drawText(
        client.textRenderer,
        Text.literal("Server operators can disable delete mode for everyone."),
        textX,
        noteY,
        NOTE_INK,
        false);
  }

  private void applyVisibility() {
    for (ClickableWidget widget : widgets) {
      widget.visible = active;
      widget.active = active;
    }
  }
}
