package com.grahambartley.client.screen.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;

public final class LootLockToast implements Toast {
  private static final long DEFAULT_DURATION_MS = 5000L;
  private static final int MIN_WIDTH = 160;
  private static final int MAX_WIDTH = 280;
  private static final int HEIGHT = 32;
  private static final int ICON_SIZE = 20;
  private static final int ICON_INSET = 6;
  private static final int TEXT_LEFT_PAD = ICON_INSET + ICON_SIZE + 6;
  private static final int TEXT_RIGHT_PAD = 8;

  private final Text title;
  private final Text subtitle;
  private final long durationMs;
  private final int width;

  public LootLockToast(Text title, Text subtitle) {
    this.title = title;
    this.subtitle = subtitle;
    this.durationMs = DEFAULT_DURATION_MS;
    this.width = computeWidth(title, subtitle);
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
    return HEIGHT;
  }

  @Override
  public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
    context.fill(0, 0, width, HEIGHT, 0xFF1B1B1B);
    context.fill(1, 1, width - 1, HEIGHT - 1, 0xFFC6C6C6);
    context.fill(2, 2, width - 2, 3, 0xFFFEFEFE);
    context.fill(2, 2, 3, HEIGHT - 2, 0xFFFEFEFE);
    context.fill(2, HEIGHT - 3, width - 2, HEIGHT - 2, 0xFF545454);
    context.fill(width - 3, 2, width - 2, HEIGHT - 2, 0xFF545454);

    int textX = TEXT_LEFT_PAD;
    int textY = 7;
    context.drawText(manager.getClient().textRenderer, title, textX, textY, 0xFF3B3B3B, false);
    context.drawText(
        manager.getClient().textRenderer, subtitle, textX, textY + 11, 0xFF3B3B3B, false);

    context.drawTexture(
        LootLockIconButton.ICON_TEXTURE,
        ICON_INSET,
        (HEIGHT - ICON_SIZE) / 2,
        0f,
        0f,
        ICON_SIZE,
        ICON_SIZE,
        ICON_SIZE,
        ICON_SIZE);

    return startTime >= durationMs ? Visibility.HIDE : Visibility.SHOW;
  }

  private static int computeWidth(Text title, Text subtitle) {
    MinecraftClient client = MinecraftClient.getInstance();
    if (client == null) {
      return MIN_WIDTH;
    }
    TextRenderer tr = client.textRenderer;
    if (tr == null) {
      return MIN_WIDTH;
    }
    int titleW = title == null ? 0 : tr.getWidth(title);
    int subtitleW = subtitle == null ? 0 : tr.getWidth(subtitle);
    int total = TEXT_LEFT_PAD + Math.max(titleW, subtitleW) + TEXT_RIGHT_PAD;
    return Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, total));
  }
}
