package com.grahambartley.client.screen.inventory;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class LootLockIconButton extends ButtonWidget {
  public static final Identifier ICON_TEXTURE =
      new Identifier("loot-lock", "textures/gui/icon.png");

  private final BooleanSupplier highlightedSupplier;

  public LootLockIconButton(
      int x,
      int y,
      int width,
      int height,
      BooleanSupplier highlightedSupplier,
      PressAction onPress) {
    super(x, y, width, height, Text.empty(), onPress, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
    this.highlightedSupplier = highlightedSupplier;
    setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Loot Lock")));
  }

  @Override
  public boolean isFocused() {
    return highlightedSupplier != null && highlightedSupplier.getAsBoolean();
  }

  @Override
  protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
    super.renderButton(context, mouseX, mouseY, delta);
    int inset = 3;
    int size = Math.min(getWidth(), getHeight()) - inset * 2;
    int iconX = getX() + (getWidth() - size) / 2;
    int iconY = getY() + (getHeight() - size) / 2;
    context.drawTexture(ICON_TEXTURE, iconX, iconY, 0f, 0f, size, size, size, size);
  }
}
