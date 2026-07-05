package com.grahambartley.lootlock.client.screen.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;

/** Compact triangular nav arrow used for previous / next profile navigation. */
public final class NavArrowButton extends PressableWidget {
  private final Runnable onPressAction;
  private final boolean rightFacing;

  public NavArrowButton(
      int x, int y, int width, int height, boolean rightFacing, Runnable onPressAction) {
    super(x, y, width, height, Text.empty());
    this.rightFacing = rightFacing;
    this.onPressAction = onPressAction;
  }

  @Override
  public void onPress() {
    if (onPressAction != null) {
      onPressAction.run();
    }
  }

  @Override
  protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
    Chrome.guiButton(context, getX(), getY(), getWidth(), getHeight());
    MinecraftClient client = MinecraftClient.getInstance();
    String glyph = rightFacing ? ">" : "<";
    int textX = getX() + (getWidth() - client.textRenderer.getWidth(glyph)) / 2;
    int textY = getY() + (getHeight() - 8) / 2;
    int color = active ? Palette.INK : 0xFF9D9D9D;
    context.drawText(client.textRenderer, Text.literal(glyph), textX, textY, color, false);
  }

  @Override
  protected void appendClickableNarrations(
      net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
    appendDefaultNarrations(builder);
  }
}
