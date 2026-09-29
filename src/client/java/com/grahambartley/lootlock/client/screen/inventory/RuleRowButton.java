package com.grahambartley.lootlock.client.screen.inventory;

import com.grahambartley.lootlock.data.RuleEntry;
import com.grahambartley.lootlock.text.LootLockLang;
import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class RuleRowButton extends PressableWidget {
  public static final int ROW_HEIGHT = 22;
  private static final int ICON_SIZE = 16;
  private static final int ICON_INSET = 3;

  private Item icon;
  private String displayName;
  private String itemId;
  private final BooleanSupplier selectedSupplier;
  private boolean inList;
  private final Runnable onPressAction;

  public void update(Item icon, String displayName, String itemId, boolean inList) {
    this.icon = icon;
    this.displayName = displayName == null ? "" : displayName;
    this.itemId = itemId == null ? "" : itemId;
    this.inList = inList;
  }

  public RuleRowButton(
      int x,
      int y,
      int width,
      Item icon,
      String displayName,
      String itemId,
      boolean inList,
      BooleanSupplier selectedSupplier,
      Runnable onPressAction) {
    super(x, y, width, ROW_HEIGHT, Text.literal(displayName));
    this.icon = icon;
    this.displayName = displayName;
    this.itemId = itemId;
    this.inList = inList;
    this.selectedSupplier = selectedSupplier;
    this.onPressAction = onPressAction;
  }

  @Override
  public void onPress() {
    if (onPressAction != null) {
      onPressAction.run();
    }
  }

  static int rowWash(boolean selected, boolean hovered) {
    if (selected) {
      return Palette.SELECTED_WASH;
    }
    return hovered ? Palette.HOVER_WASH : 0;
  }

  @Override
  protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
    boolean selected = selectedSupplier != null && selectedSupplier.getAsBoolean();
    boolean hovered = isHovered();
    int x2 = getX() + getWidth();
    int y2 = getY() + getHeight();

    int wash = rowWash(selected, hovered);
    if (wash != 0) {
      context.fill(getX(), getY(), x2, y2, wash);
    }
    if (selected) {
      context.drawBorder(getX(), getY(), getWidth(), getHeight(), Palette.SLOT_HI);
    }

    MinecraftClient client = MinecraftClient.getInstance();
    int iconX = getX() + ICON_INSET;
    int iconY = getY() + (getHeight() - ICON_SIZE) / 2;
    Chrome.slot(context, iconX - 1, iconY - 1);
    if (icon != null) {
      context.drawItem(new ItemStack(icon), iconX, iconY);
    } else if (itemId != null && itemId.startsWith(RuleEntry.TAG_PREFIX)) {
      String glyph = RuleEntry.TAG_PREFIX;
      int gx = iconX + (ICON_SIZE - client.textRenderer.getWidth(glyph)) / 2;
      int gy = iconY + (ICON_SIZE - 8) / 2;
      context.drawText(client.textRenderer, Text.literal(glyph), gx, gy, Palette.SLOT_HI, true);
    }
    if (hovered) {
      context.fill(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, Palette.SELECTED_WASH);
    }

    int textX = iconX + ICON_SIZE + 4;
    int nameY = getY() + 3;
    int idY = nameY + 10;

    int inListWidth = 0;
    if (inList) {
      Text inListText = Text.translatable(LootLockLang.RULES_ROW_IN_LIST);
      inListWidth = client.textRenderer.getWidth(inListText);
      int inListX = x2 - inListWidth - 4;
      int inListY = getY() + (getHeight() - 8) / 2;
      context.drawText(client.textRenderer, inListText, inListX, inListY, Palette.INK_DIM, false);
    }

    int textMaxWidth = getWidth() - (textX - getX()) - (inListWidth > 0 ? inListWidth + 8 : 4);
    Text nameText = ellipsize(client, Text.literal(displayName), textMaxWidth);
    Text idText = ellipsize(client, Text.literal(itemId), textMaxWidth);
    context.drawText(client.textRenderer, nameText, textX, nameY, Palette.INK, false);
    context.drawText(client.textRenderer, idText, textX, idY, Palette.INK_DIM, false);
  }

  private static Text ellipsize(MinecraftClient client, Text full, int maxWidth) {
    String str = full.getString();
    if (client.textRenderer.getWidth(full) <= maxWidth) {
      return full;
    }
    int len = str.length();
    while (len > 1 && client.textRenderer.getWidth(str.substring(0, len) + "..") > maxWidth) {
      len--;
    }
    return Text.literal(str.substring(0, len) + "..");
  }

  @Override
  protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    appendDefaultNarrations(builder);
  }
}
