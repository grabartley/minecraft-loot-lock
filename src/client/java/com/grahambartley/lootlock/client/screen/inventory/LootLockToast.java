package com.grahambartley.lootlock.client.screen.inventory;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public final class LootLockToast implements Toast {
  static final int BACKGROUND = 0xFF082C4C;
  static final int TITLE_COLOR = 0xFFFFFF00;
  static final int BODY_COLOR = 0xFFFFFFFF;
  static final double MIN_TEXT_CONTRAST = 4.5;

  private static final long DEFAULT_DURATION_MS = 5000L;
  private static final int MIN_WIDTH = 160;
  private static final int MAX_WIDTH = 240;
  private static final int BASE_HEIGHT = 32;
  private static final int LINE_HEIGHT = 10;
  private static final int ICON_SIZE = 20;
  private static final int ICON_INSET = 6;
  private static final int TEXT_LEFT_PAD = ICON_INSET + ICON_SIZE + 6;
  private static final int TEXT_RIGHT_PAD = 8;
  private static final int SPRITE_WIDTH = 160;
  private static final int SPRITE_HEIGHT = 32;
  private static final int BORDER = 4;
  private static final int CLEAN_LEFT = 20;

  private final Text title;
  private final List<OrderedText> subtitleLines;
  private final long durationMs;
  private final int width;
  private final int height;

  public LootLockToast(Text title, Text subtitle) {
    this.title = title;
    this.subtitleLines = wrapSubtitle(subtitle).stream().map(LootLockToast::readable).toList();
    this.durationMs = DEFAULT_DURATION_MS;
    this.width = computeWidth(title, subtitleLines);
    this.height = BASE_HEIGHT + Math.max(0, subtitleLines.size() - 1) * LINE_HEIGHT;
  }

  public static void show(MinecraftClient client, Text title, Text subtitle) {
    if (client == null || subtitle == null) {
      return;
    }
    client.getToastManager().add(new LootLockToast(title, subtitle));
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public int getHeight() {
    return height;
  }

  @Override
  public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
    drawBackground(context);

    TextRenderer tr = manager.getClient().textRenderer;
    int textX = TEXT_LEFT_PAD;
    int textY = 7;
    if (title != null) {
      context.drawText(tr, readable(title.asOrderedText()), textX, textY, TITLE_COLOR, false);
    }
    int bodyY = textY + 11;
    for (int i = 0; i < subtitleLines.size(); i++) {
      context.drawText(tr, subtitleLines.get(i), textX, bodyY + i * LINE_HEIGHT, BODY_COLOR, false);
    }

    context.drawTexture(
        LootLockIconButton.ICON_TEXTURE,
        ICON_INSET,
        (BASE_HEIGHT - ICON_SIZE) / 2,
        0f,
        0f,
        ICON_SIZE,
        ICON_SIZE,
        ICON_SIZE,
        ICON_SIZE);

    return startTime >= durationMs ? Visibility.HIDE : Visibility.SHOW;
  }

  private void drawBackground(DrawContext context) {
    int right = SPRITE_WIDTH - BORDER;
    int bottom = SPRITE_HEIGHT - BORDER;
    int innerW = width - BORDER * 2;
    int innerH = height - BORDER * 2;
    int edgeW = right - CLEAN_LEFT;
    int edgeH = bottom - BORDER;
    blit(context, 0, 0, 0, 0, BORDER, BORDER);
    blit(context, right, 0, width - BORDER, 0, BORDER, BORDER);
    blit(context, 0, bottom, 0, height - BORDER, BORDER, BORDER);
    blit(context, right, bottom, width - BORDER, height - BORDER, BORDER, BORDER);
    for (int x = 0; x < innerW; x += edgeW) {
      int w = Math.min(edgeW, innerW - x);
      blit(context, CLEAN_LEFT, 0, BORDER + x, 0, w, BORDER);
      blit(context, CLEAN_LEFT, bottom, BORDER + x, height - BORDER, w, BORDER);
    }
    for (int y = 0; y < innerH; y += edgeH) {
      int h = Math.min(edgeH, innerH - y);
      blit(context, 0, BORDER, 0, BORDER + y, BORDER, h);
      blit(context, right, BORDER, width - BORDER, BORDER + y, BORDER, h);
    }
    for (int y = 0; y < innerH; y += edgeH) {
      for (int x = 0; x < innerW; x += edgeW) {
        blit(
            context,
            CLEAN_LEFT,
            BORDER,
            BORDER + x,
            BORDER + y,
            Math.min(edgeW, innerW - x),
            Math.min(edgeH, innerH - y));
      }
    }
  }

  private static void blit(DrawContext context, int u, int v, int x, int y, int w, int h) {
    context.drawGuiTexture(GuiSprites.TOAST, SPRITE_WIDTH, SPRITE_HEIGHT, u, v, x, y, w, h);
  }

  static OrderedText readable(OrderedText text) {
    return visitor ->
        text.accept((index, style, codePoint) -> visitor.accept(index, readable(style), codePoint));
  }

  static Style readable(Style style) {
    TextColor color = style.getColor();
    if (color == null) {
      return style;
    }
    int rgb = color.getRgb() & 0xFFFFFF;
    int adjusted = readableColor(rgb);
    return adjusted == rgb ? style : style.withColor(TextColor.fromRgb(adjusted));
  }

  static int readableColor(int rgb) {
    int current = rgb & 0xFFFFFF;
    for (int step = 0; step < 20 && contrast(current, BACKGROUND) < MIN_TEXT_CONTRAST; step++) {
      current = towardWhite(current, 0.1f);
    }
    return current;
  }

  private static int towardWhite(int rgb, float t) {
    int r = (rgb >> 16) & 0xFF;
    int g = (rgb >> 8) & 0xFF;
    int b = rgb & 0xFF;
    r = Math.round(r + (255 - r) * t);
    g = Math.round(g + (255 - g) * t);
    b = Math.round(b + (255 - b) * t);
    return (r << 16) | (g << 8) | b;
  }

  static double contrast(int rgbA, int rgbB) {
    double la = luminance(rgbA);
    double lb = luminance(rgbB);
    return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
  }

  private static double luminance(int rgb) {
    return 0.2126 * channel((rgb >> 16) & 0xFF)
        + 0.7152 * channel((rgb >> 8) & 0xFF)
        + 0.0722 * channel(rgb & 0xFF);
  }

  private static double channel(int value) {
    double c = value / 255.0;
    return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
  }

  private static List<OrderedText> wrapSubtitle(Text subtitle) {
    if (subtitle == null) {
      return List.of();
    }
    MinecraftClient client = MinecraftClient.getInstance();
    if (client == null || client.textRenderer == null) {
      return List.of(subtitle.asOrderedText());
    }
    int maxBodyWidth = MAX_WIDTH - TEXT_LEFT_PAD - TEXT_RIGHT_PAD;
    List<OrderedText> lines = client.textRenderer.wrapLines(subtitle, maxBodyWidth);
    return lines.isEmpty() ? List.of(subtitle.asOrderedText()) : lines;
  }

  private static int computeWidth(Text title, List<OrderedText> subtitleLines) {
    MinecraftClient client = MinecraftClient.getInstance();
    if (client == null || client.textRenderer == null) {
      return MIN_WIDTH;
    }
    TextRenderer tr = client.textRenderer;
    int titleW = title == null ? 0 : tr.getWidth(title);
    int subtitleW = 0;
    for (OrderedText line : subtitleLines) {
      subtitleW = Math.max(subtitleW, tr.getWidth(line));
    }
    int total = TEXT_LEFT_PAD + Math.max(titleW, subtitleW) + TEXT_RIGHT_PAD;
    return Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, total));
  }
}
