package com.grahambartley.lootlock.client.screen.inventory;

public final class Palette {
  private Palette() {}

  public static final int FACE = 0xFFC6C6C6;
  public static final int FACE_HI = 0xFFFEFEFE;
  public static final int FACE_LO = 0xFF545454;
  public static final int SLOT = 0xFF8B8B8B;
  public static final int SLOT_HI = 0xFFFFFFFF;
  public static final int SLOT_LO = 0xFF373737;
  public static final int EDGE = 0xFF1B1B1B;

  public static final int TITLE = 0xFF404040;
  public static final int INK = 0xFF1B1B1B;
  public static final int INK_DIM = 0xFF383838;
  public static final int BUTTON_TEXT = 0xFFFFFFFF;
  public static final int BUTTON_TEXT_DIM = 0xFFD0D0D0;
  public static final int BUTTON_TEXT_DISABLED = 0xFFA0A0A0;
  public static final int CURRENT_PROFILE_NAME = 0xFFFFFF55;

  public static final int ALLOW = 0xFF4F9D43;
  public static final int DENY = 0xFFC0453A;
  public static final int LEAVE = 0xFF6A6F78;
  public static final int ALLOW_ON_PRESSED = 0xFF6EC85D;
  public static final int DENY_ON_PRESSED = 0xFFE0675B;
  public static final int LEAVE_ON_PRESSED = 0xFFB4B8C0;
  public static final int ALLOW_ON_SLOT = 0xFF004A00;
  public static final int DENY_ON_SLOT = 0xFF800000;
  public static final int OFF_ON_SLOT = 0xFF383838;

  public static final int HOVER_WASH = 0x40FFFFFF;
  public static final int SELECTED_WASH = 0x80FFFFFF;

  public static final int[] PROFILE_COLORS = {
    0xFF4D4D54, 0xFF3B7530, 0xFF2C5FA5, 0xFF9B3127, 0xFF7E5A14, 0xFF7A52C9, 0xFF9C5414, 0xFF1F6E69,
  };
}
