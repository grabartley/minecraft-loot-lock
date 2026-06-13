package com.grahambartley.client.screen.inventory;

import net.minecraft.client.gui.DrawContext;

/**
 * Drawing helpers for the vanilla Minecraft GUI design language ported from the prototype CSS. Each
 * method paints into the supplied {@link DrawContext} using {@code fill()} primitives so the
 * widgets stay pixel-faithful at any GUI scale.
 *
 * <p>The naming mirrors the CSS class names in {@code ux_redesign_2/lootlock.css}:
 *
 * <ul>
 *   <li>{@code guiWindow}: raised panel with thick edge + inset highlight + inset shadow.
 *   <li>{@code guiButton}: smaller raised button face suitable for compact toggles.
 *   <li>{@code pressedButton}: same area drawn inverted to read as pressed.
 *   <li>{@code well}: recessed dark content well.
 *   <li>{@code slot}: recessed slot inset, used for nav arrows and the profile pill base.
 *   <li>{@code coloredSegment}: tinted on-state for segmented controls (allow / deny / leave /
 *       delete).
 * </ul>
 */
public final class Chrome {
  private Chrome() {}

  /**
   * Raised panel: 1px dark edge then 1px white highlight (top/left) and 1px shadow (bottom/right).
   */
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

  /** Raised button face (1px edge). */
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

  /** Same area drawn inverted to read as pressed. */
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

  /** Dark recessed content well. */
  public static void well(DrawContext context, int x, int y, int width, int height) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x, y, x2, y2, Palette.WELL);
    context.fill(x, y, x2, y + 1, Palette.WELL_LO);
    context.fill(x, y, x + 1, y2, Palette.WELL_LO);
    context.fill(x, y2 - 1, x2, y2, Palette.WELL_HI);
    context.fill(x2 - 1, y, x2, y2, Palette.WELL_HI);
  }

  /** Recessed slot inset (lighter than well). */
  public static void slot(DrawContext context, int x, int y, int width, int height) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x, y, x2, y2, Palette.SLOT);
    context.fill(x, y, x2, y + 1, Palette.SLOT_LO);
    context.fill(x, y, x + 1, y2, Palette.SLOT_LO);
    context.fill(x, y2 - 1, x2, y2, Palette.SLOT_HI);
    context.fill(x2 - 1, y, x2, y2, Palette.SLOT_HI);
  }

  /** Tinted segment on-state: face color set explicitly + 1px shadow + 1px highlight. */
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

  /** Top-accented active tab: face + top yellow bar + dark well peeking through bottom. */
  public static void activeTab(DrawContext context, int x, int y, int width, int height) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x, y, x2, y2, Palette.EDGE);
    context.fill(x + 1, y + 1, x2 - 1, y2, Palette.WELL);
    context.fill(x + 1, y + 1, x2 - 1, y + 4, Palette.GOLD);
  }

  /** Raised tab (inactive). */
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

  /** Solid color square (e.g., profile color dot). */
  public static void colorChip(
      DrawContext context, int x, int y, int width, int height, int color) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x - 1, y - 1, x2 + 1, y2 + 1, 0xFF000000);
    context.fill(x, y, x2, y2, color);
  }

  /** Toggle switch background (raised) for the OFF state. */
  public static void switchOff(DrawContext context, int x, int y, int width, int height) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x, y, x2, y2, Palette.EDGE);
    context.fill(x + 1, y + 1, x2 - 1, y2 - 1, 0xFF9A9A9A);
    context.fill(x + 1, y + 1, x2 - 1, y + 2, Palette.SLOT_LO);
  }

  /** Toggle switch background (raised) for the ON state. */
  public static void switchOn(DrawContext context, int x, int y, int width, int height) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x, y, x2, y2, Palette.EDGE);
    context.fill(x + 1, y + 1, x2 - 1, y2 - 1, Palette.ALLOW);
    context.fill(x + 1, y + 1, x2 - 1, y + 2, 0xFF2F6A28);
  }

  /** Toggle switch background tinted red (used for Server: OFF unsupported state). */
  public static void switchBad(DrawContext context, int x, int y, int width, int height) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x, y, x2, y2, Palette.EDGE);
    context.fill(x + 1, y + 1, x2 - 1, y2 - 1, 0xFF7A3A34);
    context.fill(x + 1, y + 1, x2 - 1, y + 2, 0xFF5A2722);
  }

  /** Sliding knob inside a switch. */
  public static void switchKnob(DrawContext context, int x, int y, int width, int height) {
    int x2 = x + width;
    int y2 = y + height;
    context.fill(x, y, x2, y2, Palette.FACE);
    context.fill(x, y, x2, y + 1, Palette.FACE_HI);
    context.fill(x, y, x + 1, y2, Palette.FACE_HI);
    context.fill(x, y2 - 1, x2, y2, Palette.FACE_LO);
    context.fill(x2 - 1, y, x2, y2, Palette.FACE_LO);
  }

  /** Left-accent summary block: dark well + colored left border. */
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
