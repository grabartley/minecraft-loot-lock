package com.grahambartley.lootlock.client.screen.inventory;

import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class SegmentedButton extends PressableWidget {
  static final int ACCENT_HEIGHT = 2;

  private final BooleanSupplier onSupplier;
  private final Runnable onPressAction;
  private final int accentColor;
  private final int selectedLabelColor;

  public SegmentedButton(
      int x,
      int y,
      int width,
      int height,
      Text label,
      int accentColor,
      int selectedLabelColor,
      BooleanSupplier onSupplier,
      Runnable onPressAction) {
    super(x, y, width, height, label);
    this.onSupplier = onSupplier;
    this.onPressAction = onPressAction;
    this.accentColor = accentColor;
    this.selectedLabelColor = selectedLabelColor;
  }

  @Override
  public void onPress() {
    if (onPressAction != null) {
      onPressAction.run();
    }
  }

  static Identifier sprite(boolean selected, boolean active, boolean hovered) {
    if (selected) {
      return Chrome.BUTTON.disabled();
    }
    return Chrome.BUTTON.get(active, hovered);
  }

  static int labelColor(boolean selected, boolean active, int selectedLabelColor) {
    if (selected) {
      return selectedLabelColor;
    }
    return active ? Palette.BUTTON_TEXT : Palette.BUTTON_TEXT_DISABLED;
  }

  @Override
  protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
    boolean selected = onSupplier.getAsBoolean();
    context.drawGuiTexture(
        sprite(selected, active, isSelected()), getX(), getY(), getWidth(), getHeight());
    if (selected) {
      int accentBottom = getY() + getHeight() - 1;
      context.fill(
          getX() + 1,
          accentBottom - ACCENT_HEIGHT,
          getX() + getWidth() - 1,
          accentBottom,
          accentColor);
    }

    MinecraftClient client = MinecraftClient.getInstance();
    int textX = getX() + (getWidth() - client.textRenderer.getWidth(getMessage())) / 2;
    int textY = getY() + (getHeight() - 8) / 2 - (selected ? 1 : 0);
    context.drawText(
        client.textRenderer,
        getMessage(),
        textX,
        textY,
        labelColor(selected, active, selectedLabelColor),
        true);
  }

  @Override
  protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    appendDefaultNarrations(builder);
  }
}
