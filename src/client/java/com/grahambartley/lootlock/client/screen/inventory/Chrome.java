package com.grahambartley.lootlock.client.screen.inventory;

import net.minecraft.client.gui.DrawContext;

public final class Chrome {
  private Chrome() {}

  public static void guiWindow(DrawContext context, int x, int y, int width, int height) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x, y, x2, y2, Palette.EDGE);
    context.fill(x + 1, y + 1, x2 - 1, y2 - 1, Palette.FACE);
    context.fill(x + 1, y + 1, x2 - 1, y + 2, Palette.FACE_HI);
    context.fill(x + 1, y + 1, x + 2, y2 - 1, Palette.FACE_HI);
    context.fill(x + 1, y2 - 2, x2 - 1, y2 - 1, Palette.FACE_LO);
    context.fill(x2 - 2, y + 1, x2 - 1, y2 - 1, Palette.FACE_LO);
  }

  public static void guiButton(DrawContext context, int x, int y, int width, int height) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x, y, x2, y2, Palette.EDGE);
    context.fill(x + 1, y + 1, x2 - 1, y2 - 1, Palette.FACE);
    context.fill(x + 1, y + 1, x2 - 1, y + 2, Palette.FACE_HI);
    context.fill(x + 1, y + 1, x + 2, y2 - 1, Palette.FACE_HI);
    context.fill(x + 1, y2 - 2, x2 - 1, y2 - 1, Palette.FACE_LO);
    context.fill(x2 - 2, y + 1, x2 - 1, y2 - 1, Palette.FACE_LO);
  }

  public static void pressedButton(DrawContext context, int x, int y, int width, int height) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x, y, x2, y2, Palette.EDGE);
    context.fill(x + 1, y + 1, x2 - 1, y2 - 1, Palette.FACE);
    context.fill(x + 1, y + 1, x2 - 1, y + 2, Palette.FACE_LO);
    context.fill(x + 1, y + 1, x + 2, y2 - 1, Palette.FACE_LO);
    context.fill(x + 1, y2 - 2, x2 - 1, y2 - 1, Palette.FACE_HI);
    context.fill(x2 - 2, y + 1, x2 - 1, y2 - 1, Palette.FACE_HI);
  }

  public static void well(DrawContext context, int x, int y, int width, int height) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x, y, x2, y2, Palette.WELL);
    context.fill(x, y, x2, y + 1, Palette.WELL_LO);
    context.fill(x, y, x + 1, y2, Palette.WELL_LO);
    context.fill(x, y2 - 1, x2, y2, Palette.WELL_HI);
    context.fill(x2 - 1, y, x2, y2, Palette.WELL_HI);
  }

  public static void slot(DrawContext context, int x, int y, int width, int height) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x, y, x2, y2, Palette.SLOT);
    context.fill(x, y, x2, y + 1, Palette.SLOT_LO);
    context.fill(x, y, x + 1, y2, Palette.SLOT_LO);
    context.fill(x, y2 - 1, x2, y2, Palette.SLOT_HI);
    context.fill(x2 - 1, y, x2, y2, Palette.SLOT_HI);
  }

  public static void coloredSegment(
      DrawContext context, int x, int y, int width, int height, int color) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x, y, x2, y2, Palette.EDGE);
    context.fill(x + 1, y + 1, x2 - 1, y2 - 1, color);
    context.fill(x + 1, y + 1, x2 - 1, y + 2, blend(color, Palette.FACE_HI, 0.3f));
    context.fill(x + 1, y + 1, x + 2, y2 - 1, blend(color, Palette.FACE_HI, 0.3f));
    context.fill(x + 1, y2 - 2, x2 - 1, y2 - 1, blend(color, 0xFF000000, 0.3f));
    context.fill(x2 - 2, y + 1, x2 - 1, y2 - 1, blend(color, 0xFF000000, 0.3f));
  }

  public static void activeTab(DrawContext context, int x, int y, int width, int height) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x, y, x2, y2, Palette.EDGE);
    context.fill(x + 1, y + 1, x2 - 1, y2, Palette.WELL);
    context.fill(x + 1, y + 1, x2 - 1, y + 4, Palette.GOLD);
  }

  public static void inactiveTab(DrawContext context, int x, int y, int width, int height) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x, y, x2, y2, Palette.EDGE);
    context.fill(x + 1, y + 1, x2 - 1, y2 - 1, 0xFFB4B4B4);
    context.fill(x + 1, y + 1, x2 - 1, y + 2, 0xFFE2E2E2);
    context.fill(x + 1, y + 1, x + 2, y2 - 1, 0xFFE2E2E2);
    context.fill(x + 1, y2 - 2, x2 - 1, y2 - 1, 0xFF8F8F8F);
    context.fill(x2 - 2, y + 1, x2 - 1, y2 - 1, 0xFF8F8F8F);
  }

  public static void colorChip(
      DrawContext context, int x, int y, int width, int height, int color) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x - 1, y - 1, x2 + 1, y2 + 1, 0xFF000000);
    context.fill(x, y, x2, y2, color);
  }

  public static void switchOff(DrawContext context, int x, int y, int width, int height) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x, y, x2, y2, Palette.EDGE);
    context.fill(x + 1, y + 1, x2 - 1, y2 - 1, 0xFF9A9A9A);
    context.fill(x + 1, y + 1, x2 - 1, y + 2, Palette.SLOT_LO);
  }

  public static void switchOn(DrawContext context, int x, int y, int width, int height) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x, y, x2, y2, Palette.EDGE);
    context.fill(x + 1, y + 1, x2 - 1, y2 - 1, Palette.ALLOW);
    context.fill(x + 1, y + 1, x2 - 1, y + 2, 0xFF2F6A28);
  }

  public static void switchBad(DrawContext context, int x, int y, int width, int height) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x, y, x2, y2, Palette.EDGE);
    context.fill(x + 1, y + 1, x2 - 1, y2 - 1, 0xFF7A3A34);
    context.fill(x + 1, y + 1, x2 - 1, y + 2, 0xFF5A2722);
  }

  public static void switchKnob(DrawContext context, int x, int y, int width, int height) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x, y, x2, y2, Palette.FACE);
    context.fill(x, y, x2, y + 1, Palette.FACE_HI);
    context.fill(x, y, x + 1, y2, Palette.FACE_HI);
    context.fill(x, y2 - 1, x2, y2, Palette.FACE_LO);
    context.fill(x2 - 1, y, x2, y2, Palette.FACE_LO);
  }

  public static void summaryBlock(
      DrawContext context, int x, int y, int width, int height, int accent) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x, y, x2, y2, 0xFF2B2B31);
    context.fill(x, y, x + 4, y2, accent);
    context.fill(x, y, x2, y + 1, Palette.WELL_HI);
    context.fill(x, y2 - 1, x2, y2, Palette.WELL_LO);
    context.fill(x2 - 1, y, x2, y2, Palette.WELL_LO);
  }

  private static int blend(int a, int b, float t) {
    int ar = (a >> 16) & 0xFF;
    int ag = (a >> 8) & 0xFF;
    int ab = a & 0xFF;
    int br = (b >> 16) & 0xFF;
    int bg = (b >> 8) & 0xFF;
    int bb = b & 0xFF;
    int r = (int) (ar + (br - ar) * t);
    int g = (int) (ag + (bg - ag) * t);
    int bl = (int) (ab + (bb - ab) * t);
    return 0xFF000000 | (r << 16) | (g << 8) | bl;
  }
}
