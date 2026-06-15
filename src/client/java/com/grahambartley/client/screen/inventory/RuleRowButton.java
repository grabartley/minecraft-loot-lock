package com.grahambartley.client.screen.inventory;

import com.grahambartley.data.RuleEntry;
import com.grahambartley.text.LootLockLang;
import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * One row inside the Rules tab's content well. Renders a 16x16 item icon, the item display name in
 * white, and the namespaced id in dim grey beneath. Selected rows pick up a blue accent + outline
 * matching the prototype's {@code .row.selected} state. An optional "in list" pill tag is painted
 * on the right side when the item is already part of the active profile.
 *
 * <p>Modifier-aware clicks (Shift / Ctrl / Cmd / double) are handled by the host {@link
 * RulesTabView}; this widget just delegates {@link #onPress()} with no extra context.
 */
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

  @Override
  protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
    boolean selected = selectedSupplier != null && selectedSupplier.getAsBoolean();
    boolean hovered = isHovered();

    int rowBg;
    if (selected) {
      rowBg = 0xFF3A4A6B;
    } else if (hovered) {
      rowBg = Palette.WELL_ROW;
    } else {
      rowBg = 0;
    }
    if (rowBg != 0) {
      context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), rowBg);
    }
    if (selected) {
      // 1px blue outline matching .row.selected box-shadow inset 0 0 0 2px.
      int outline = 0xFF6F8FCE;
      context.fill(getX(), getY(), getX() + getWidth(), getY() + 1, outline);
      context.fill(
          getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), outline);
      context.fill(getX(), getY(), getX() + 1, getY() + getHeight(), outline);
      context.fill(
          getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), outline);
    }

    MinecraftClient client = MinecraftClient.getInstance();
    int iconX = getX() + ICON_INSET;
    int iconY = getY() + (getHeight() - ICON_SIZE) / 2;
    if (icon != null) {
      context.drawItem(new ItemStack(icon), iconX, iconY);
    } else {
      Chrome.slot(context, iconX, iconY, ICON_SIZE, ICON_SIZE);
      if (itemId != null && itemId.startsWith(RuleEntry.TAG_PREFIX)) {
        String glyph = RuleEntry.TAG_PREFIX;
        int gx = iconX + (ICON_SIZE - client.textRenderer.getWidth(glyph)) / 2;
        int gy = iconY + (ICON_SIZE - 8) / 2;
        context.drawText(client.textRenderer, Text.literal(glyph), gx, gy, Palette.GOLD, false);
      }
    }

    int textX = iconX + ICON_SIZE + 4;
    int nameY = getY() + 3;
    int idY = nameY + 10;

    int pillWidth = 0;
    if (inList) {
      Text pillText = Text.translatable(LootLockLang.RULES_ROW_IN_LIST);
      pillWidth = client.textRenderer.getWidth(pillText) + 6;
      int pillX = getX() + getWidth() - pillWidth - 3;
      int pillY = getY() + (getHeight() - 10) / 2;
      context.fill(pillX, pillY, pillX + pillWidth, pillY + 10, 0xFF3A3A42);
      context.drawText(
          client.textRenderer,
          pillText.copy().formatted(Formatting.GRAY),
          pillX + 3,
          pillY + 1,
          0xFF9A9AA4,
          false);
    }

    int textMaxWidth = getWidth() - (textX - getX()) - (pillWidth > 0 ? pillWidth + 6 : 4);
    Text nameText = ellipsize(client, Text.literal(displayName), textMaxWidth);
    Text idText = ellipsize(client, Text.literal(itemId), textMaxWidth);
    context.drawText(client.textRenderer, nameText, textX, nameY, 0xFFECECF0, false);
    context.drawText(client.textRenderer, idText, textX, idY, 0xFF9A9AA4, false);
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
  protected void appendClickableNarrations(
      net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
    appendDefaultNarrations(builder);
  }
}
