package com.grahambartley.client.screen.inventory;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Vanilla-style button that paints the Loot Lock icon centered in the button area instead of a text
 * label. Used for the inventory-screen entry point next to the recipe-book button and any other
 * in-game surface that wants the brand icon.
 */
public final class LootLockIconButton extends ButtonWidget {
  public static final Identifier ICON_TEXTURE =
      new Identifier("loot-lock", "textures/gui/icon.png");

  public LootLockIconButton(int x, int y, int width, int height, PressAction onPress) {
    // Empty label so the inherited drawMessage call paints nothing; the icon replaces it.
    super(x, y, width, height, Text.empty(), onPress, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
    setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Loot Lock")));
  }

  @Override
  protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
    super.renderButton(context, mouseX, mouseY, delta);
    int inset = 2;
    int size = Math.min(getWidth(), getHeight()) - inset * 2;
    int iconX = getX() + (getWidth() - size) / 2;
    int iconY = getY() + (getHeight() - size) / 2;
    context.drawTexture(ICON_TEXTURE, iconX, iconY, 0f, 0f, size, size, size, size);
  }
}
