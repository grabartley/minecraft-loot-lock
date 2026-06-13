package com.grahambartley.client.screen.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;

/**
 * One styled row inside the profile dropdown manager: color chip + profile name + meta line + three
 * mini action buttons (rename / duplicate / delete). Visually aligned with the prototype's {@code
 * .pf-opt} CSS row.
 *
 * <p>Click areas are split: the left "main" area (chip + name + meta) selects the profile, while
 * the right-side mini buttons are siblings registered separately. This widget only renders the
 * background, chip, and text — the per-row action buttons live on the panel itself.
 */
public final class ProfileDropdownRow extends PressableWidget {
  public static final int ROW_HEIGHT = 22;
  public static final int ACTIONS_WIDTH = 60;
  private static final int CHIP_SIZE = 12;

  private final int profileColor;
  private final String profileName;
  private final String metaText;
  private final boolean active;
  private final Runnable onPressAction;

  public ProfileDropdownRow(
      int x,
      int y,
      int width,
      int profileColor,
      String profileName,
      String metaText,
      boolean active,
      Runnable onPressAction) {
    super(x, y, width, ROW_HEIGHT, Text.literal(profileName));
    this.profileColor = profileColor;
    this.profileName = profileName == null ? "" : profileName;
    this.metaText = metaText == null ? "" : metaText;
    this.active = active;
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
    int bg = active ? 0xFF34301F : (isHovered() ? Palette.WELL_ROW : 0);
    if (bg != 0) {
      context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
    }
    if (active) {
      // Gold left accent matching .pf-opt.active box-shadow inset 3px 0 0 gold.
      context.fill(getX(), getY(), getX() + 3, getY() + getHeight(), Palette.GOLD);
    }

    int chipX = getX() + 6;
    int chipY = getY() + (getHeight() - CHIP_SIZE) / 2;
    Chrome.colorChip(context, chipX, chipY, CHIP_SIZE, CHIP_SIZE, profileColor);

    MinecraftClient client = MinecraftClient.getInstance();
    int textX = chipX + CHIP_SIZE + 5;
    int nameY = getY() + 3;
    int metaY = nameY + 10;

    int nameColor = active ? Palette.GOLD : Palette.ON_WELL;
    context.drawText(
        client.textRenderer, Text.literal(profileName), textX, nameY, nameColor, false);
    context.drawText(
        client.textRenderer, Text.literal(metaText), textX, metaY, Palette.ON_WELL_DIM, false);
  }

  @Override
  protected void appendClickableNarrations(
      net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
    appendDefaultNarrations(builder);
  }
}
