package com.grahambartley.lootlock.client.screen.inventory;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.util.Identifier;

public final class GuiSprites {
  private GuiSprites() {}

  public static final Identifier PANEL = Identifier.ofVanilla("recipe_book/overlay_recipe");
  public static final Identifier SLOT = Identifier.ofVanilla("container/slot");
  static final Identifier TOAST = Identifier.ofVanilla("toast/system");
  private static final int SLOT_SIZE = 18;

  public static final ButtonTextures BUTTON =
      new ButtonTextures(
          Identifier.ofVanilla("widget/button"),
          Identifier.ofVanilla("widget/button_disabled"),
          Identifier.ofVanilla("widget/button_highlighted"));

  public static final ButtonTextures TAB =
      new ButtonTextures(
          Identifier.ofVanilla("widget/tab_selected"),
          Identifier.ofVanilla("widget/tab"),
          Identifier.ofVanilla("widget/tab_selected_highlighted"),
          Identifier.ofVanilla("widget/tab_highlighted"));

  public static void panel(DrawContext context, int x, int y, int width, int height) {
    context.drawGuiTexture(PANEL, x, y, width, height);
  }

  public static void inset(DrawContext context, int x, int y, int width, int height) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x, y, x2, y2, Palette.SLOT);
    context.fill(x, y, x2, y + 1, Palette.SLOT_LO);
    context.fill(x, y, x + 1, y2, Palette.SLOT_LO);
    context.fill(x, y2 - 1, x2, y2, Palette.SLOT_HI);
    context.fill(x2 - 1, y, x2, y2, Palette.SLOT_HI);
  }

  public static void slot(DrawContext context, int x, int y) {
    context.drawGuiTexture(SLOT, x, y, SLOT_SIZE, SLOT_SIZE);
  }

  public static void button(
      DrawContext context, int x, int y, int width, int height, boolean active, boolean hovered) {
    context.drawGuiTexture(BUTTON.get(active, hovered), x, y, width, height);
  }

  public static void colorChip(
      DrawContext context, int x, int y, int width, int height, int color) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x - 2, y - 2, x2 + 2, y2 + 2, 0xFF000000);
    context.fill(x - 1, y - 1, x2 + 1, y2 + 1, Palette.FACE);
    context.fill(x, y, x2, y2, color);
  }
}
