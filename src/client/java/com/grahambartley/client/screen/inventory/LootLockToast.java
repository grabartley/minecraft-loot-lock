package com.grahambartley.client.screen.inventory;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

public final class LootLockToast implements Toast {
  private static final long DEFAULT_DURATION_MS = 5000L;
  private static final int MIN_WIDTH = 160;
  private static final int MAX_WIDTH = 240;
  private static final int BASE_HEIGHT = 32;
  private static final int LINE_HEIGHT = 10;
  private static final int ICON_SIZE = 20;
  private static final int ICON_INSET = 6;
  private static final int TEXT_LEFT_PAD = ICON_INSET + ICON_SIZE + 6;
  private static final int TEXT_RIGHT_PAD = 8;

  private final Text title;
  private final List<OrderedText> subtitleLines;
  private final long durationMs;
  private final int width;
  private final int height;

  public LootLockToast(Text title, Text subtitle) {
    this.title = title;
    this.subtitleLines = wrapSubtitle(subtitle);
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
    context.fill(0, 0, width, height, 0xFF1B1B1B);
    context.fill(1, 1, width - 1, height - 1, 0xFFC6C6C6);
    context.fill(2, 2, width - 2, 3, 0xFFFEFEFE);
    context.fill(2, 2, 3, height - 2, 0xFFFEFEFE);
    context.fill(2, height - 3, width - 2, height - 2, 0xFF545454);
    context.fill(width - 3, 2, width - 2, height - 2, 0xFF545454);

    TextRenderer tr = manager.getClient().textRenderer;
    int textX = TEXT_LEFT_PAD;
    int textY = 7;
    context.drawText(tr, title, textX, textY, 0xFF3B3B3B, false);
    int bodyY = textY + 11;
    for (int i = 0; i < subtitleLines.size(); i++) {
      context.drawText(tr, subtitleLines.get(i), textX, bodyY + i * LINE_HEIGHT, 0xFF3B3B3B, false);
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
