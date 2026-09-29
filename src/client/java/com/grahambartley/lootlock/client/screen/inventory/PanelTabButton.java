package com.grahambartley.lootlock.client.screen.inventory;

import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class PanelTabButton extends PressableWidget {
  private final BooleanSupplier activeSupplier;
  private final Runnable onPressAction;

  public PanelTabButton(
      int x,
      int y,
      int width,
      int height,
      Text label,
      BooleanSupplier activeSupplier,
      Runnable onPressAction) {
    super(x, y, width, height, label);
    this.activeSupplier = activeSupplier;
    this.onPressAction = onPressAction;
  }

  @Override
  public void onPress() {
    if (onPressAction != null) {
      onPressAction.run();
    }
  }

  static Identifier sprite(boolean selected, boolean active, boolean hovered) {
    return Chrome.TAB.get(selected, active && hovered);
  }

  static int labelColor(boolean selected, boolean active, boolean hovered) {
    if (selected) {
      return Palette.TITLE;
    }
    return active && hovered ? Palette.BUTTON_TEXT : Palette.BUTTON_TEXT_DISABLED;
  }

  @Override
  protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
    boolean selected = activeSupplier.getAsBoolean();
    boolean hovered = isSelected();
    context.drawGuiTexture(
        sprite(selected, active, hovered), getX(), getY(), getWidth(), getHeight());

    MinecraftClient client = MinecraftClient.getInstance();
    int textX = getX() + (getWidth() - client.textRenderer.getWidth(getMessage())) / 2;
    int textY = getY() + (getHeight() - 8) / 2 + 1;
    context.drawText(
        client.textRenderer,
        getMessage(),
        textX,
        textY,
        labelColor(selected, active, hovered),
        false);
  }

  @Override
  protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    appendDefaultNarrations(builder);
  }
}
