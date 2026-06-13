package com.grahambartley.client.screen.inventory;

/** Color palette ported from the design's CSS variables. All values are 0xAARRGGBB. */
public final class Palette {
  private Palette() {}

  // Vanilla widget greys.
  public static final int FACE = 0xFFC6C6C6;
  public static final int FACE_HI = 0xFFFEFEFE;
  public static final int FACE_LO = 0xFF545454;
  public static final int SLOT = 0xFF8B8B8B;
  public static final int SLOT_HI = 0xFFFFFFFF;
  public static final int SLOT_LO = 0xFF373737;
  public static final int INK = 0xFF3B3B3B;
  public static final int INK_DIM = 0xFF6E6E6E;
  public static final int EDGE = 0xFF1B1B1B;

  // Dark "content well" backgrounds for lists and recessed panels.
  public static final int WELL = 0xFF26262B;
  public static final int WELL_HI = 0xFF46464E;
  public static final int WELL_LO = 0xFF161619;
  public static final int WELL_ROW = 0xFF34343B;
  public static final int WELL_ROW_HI = 0xFF43434C;
  public static final int ON_WELL = 0xFFECECF0;
  public static final int ON_WELL_DIM = 0xFF9A9AA4;

  // Loot-lock accent system.
  public static final int ALLOW = 0xFF4F9D43;
  public static final int ALLOW_BR = 0xFF6EC85D;
  public static final int DENY = 0xFFC0453A;
  public static final int DENY_BR = 0xFFE0675B;
  public static final int GOLD = 0xFFE6B33E;
  public static final int GOLD_DEEP = 0xFFB07D1D;
  public static final int INFO = 0xFF5A8FD6;
  public static final int PURPLE = 0xFF7A52C9;

  public static final int SHADOW = 0x8C000000;
}
