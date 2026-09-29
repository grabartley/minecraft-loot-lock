package com.grahambartley.lootlock.client.screen.inventory;

import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;

public final class ProfileDropdownRow extends PressableWidget {
  public static final int ROW_HEIGHT = 22;
  public static final int ACTIONS_WIDTH = 80;
  static final int CHIP_SIZE = 12;
  static final int CHIP_INSET_X = 6;

  private final UUID profileId;
  private final int profileColor;
  private final String profileName;
  private final String metaText;
  private final boolean active;
  private final Runnable onPressAction;
  private final Runnable onChipPressAction;
  private boolean suppressNameRender;
  private boolean chipPressed;

  public ProfileDropdownRow(
      int x,
      int y,
      int width,
      UUID profileId,
      int profileColor,
      String profileName,
      String metaText,
      boolean active,
      Runnable onPressAction,
      Runnable onChipPressAction) {
    super(x, y, width, ROW_HEIGHT, Text.literal(profileName));
    this.profileId = profileId;
    this.profileColor = profileColor;
    this.profileName = profileName == null ? "" : profileName;
    this.metaText = metaText == null ? "" : metaText;
    this.active = active;
    this.onPressAction = onPressAction;
    this.onChipPressAction = onChipPressAction;
  }

  public UUID getProfileId() {
    return profileId;
  }

  public void setSuppressNameRender(boolean suppressNameRender) {
    this.suppressNameRender = suppressNameRender;
  }

  public boolean isMouseOverChip(double mouseX, double mouseY) {
    int chipX = chipX();
    int chipY = chipY();
    return mouseX >= chipX
        && mouseX < chipX + CHIP_SIZE
        && mouseY >= chipY
        && mouseY < chipY + CHIP_SIZE;
  }

  private int chipX() {
    return getX() + CHIP_INSET_X;
  }

  private int chipY() {
    return getY() + (getHeight() - CHIP_SIZE) / 2;
  }

  @Override
  public void onClick(double mouseX, double mouseY) {
    chipPressed = isMouseOverChip(mouseX, mouseY);
    super.onClick(mouseX, mouseY);
  }

  @Override
  public void onPress() {
    if (chipPressed) {
      chipPressed = false;
      if (onChipPressAction != null) {
        onChipPressAction.run();
      }
      return;
    }
    if (onPressAction != null) {
      onPressAction.run();
    }
  }

  @Override
  protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
    int bg = active ? 0xFF34301F : (isHovered() ? Palette.WELL_ROW : 0);
    if (bg != 0) {
      context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
    }
    if (active) {
      context.fill(getX(), getY(), getX() + 3, getY() + getHeight(), Palette.GOLD);
    }

    int chipX = chipX();
    int chipY = chipY();
    Chrome.colorChip(context, chipX, chipY, CHIP_SIZE, CHIP_SIZE, profileColor);

    MinecraftClient client = MinecraftClient.getInstance();
    int textX = chipX + CHIP_SIZE + 5;
    int nameY = getY() + 3;
    int metaY = nameY + 10;

    int nameColor = active ? Palette.GOLD : Palette.ON_WELL;
    if (!suppressNameRender) {
      context.drawText(
          client.textRenderer, Text.literal(profileName), textX, nameY, nameColor, false);
    }
    context.drawText(
        client.textRenderer, Text.literal(metaText), textX, metaY, Palette.ON_WELL_DIM, false);
  }

  public int nameRenderX() {
    return chipX() + CHIP_SIZE + 5;
  }

  public int nameRenderY() {
    return getY() + 3;
  }

  @Override
  protected void appendClickableNarrations(
      net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
    appendDefaultNarrations(builder);
  }
}
