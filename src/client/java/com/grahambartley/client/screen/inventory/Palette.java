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

  /** Neutral grey used as the on-state background for the "Leave" action segment. */
  public static final int LEAVE = 0xFF6A6F78;

  /** Fill behind the keyboard-key pill chip used in settings and bulk-bar hints. */
  public static final int KBD_FILL = 0xFF3A3A42;

  /** 1px divider between adjacent rows inside the dark content well. */
  public static final int ROW_DIVIDER = 0xFF1E1E22;

  public static final int SHADOW = 0x8C000000;

  /**
   * Profile colour palette cycled by clicking the chip on a profile dropdown row. Order is the
   * cycle order: index 0 is the default for unset profiles, each click advances by one, the last
   * entry wraps back to index 0. ARGB with full alpha so the values feed directly into {@link
   * Chrome#colorChip}.
   */
  public static final int[] PROFILE_COLORS = {
    0xFF4D4D54, 0xFF3B7530, 0xFF2C5FA5, 0xFF9B3127, 0xFF7E5A14, 0xFF7A52C9, 0xFF9C5414, 0xFF1F6E69,
  };
}
