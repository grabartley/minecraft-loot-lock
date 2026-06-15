package com.grahambartley.client.screen.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;

public final class MiniActionButton extends PressableWidget {
  public static final int SIZE = 18;

  private final Runnable onPressAction;
  private final boolean danger;
  private final Text glyph;

  public MiniActionButton(int x, int y, Text glyph, boolean danger, Runnable onPressAction) {
    super(x, y, SIZE, SIZE, glyph);
    this.glyph = glyph;
    this.danger = danger;
    this.onPressAction = onPressAction;
  }

  @Override
  public void onPress() {
    if (onPressAction != null) {
      onPressAction.run();
    }
  }

  @Override
  protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
    int face;
    if (!active) {
      face = 0xFF2C2C32;
    } else if (isHovered()) {
      face = danger ? Palette.DENY : 0xFF4A4A54;
    } else {
      face = 0xFF3A3A42;
    }
    int x2 = getX() + getWidth();
    int y2 = getY() + getHeight();
    context.fill(getX(), getY(), x2, y2, face);
    context.fill(getX(), getY(), x2, getY() + 1, 0xFF54545E);
    context.fill(getX(), getY(), getX() + 1, y2, 0xFF54545E);
    context.fill(getX(), y2 - 1, x2, y2, 0xFF1C1C20);
    context.fill(x2 - 1, getY(), x2, y2, 0xFF1C1C20);

    int textColor = active ? 0xFFDCDCE2 : 0xFF5A5A62;
    MinecraftClient client = MinecraftClient.getInstance();
    int textX = getX() + (getWidth() - client.textRenderer.getWidth(glyph)) / 2;
    int textY = getY() + (getHeight() - 8) / 2;
    context.drawText(client.textRenderer, glyph, textX, textY, textColor, false);
  }

  @Override
  protected void appendClickableNarrations(
      net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
    appendDefaultNarrations(builder);
  }
}
