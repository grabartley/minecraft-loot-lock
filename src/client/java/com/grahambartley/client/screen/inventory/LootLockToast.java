package com.grahambartley.client.screen.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;

/**
 * Custom vanilla-style toast that paints the Loot Lock icon on the left and a title + subtitle on
 * the right. Replaces the generic SystemToast surface for Loot Lock notifications so the brand icon
 * shows up wherever the mod talks to the player.
 */
public final class LootLockToast implements Toast {
  private static final long DEFAULT_DURATION_MS = 5000L;
  private static final int WIDTH = 160;
  private static final int HEIGHT = 32;
  private static final int ICON_SIZE = 20;
  private static final int ICON_INSET = 6;

  private final Text title;
  private final Text subtitle;
  private final long durationMs;

  public LootLockToast(Text title, Text subtitle) {
    this(title, subtitle, DEFAULT_DURATION_MS);
  }

  public LootLockToast(Text title, Text subtitle, long durationMs) {
    this.title = title;
    this.subtitle = subtitle;
    this.durationMs = durationMs;
  }

  public static void show(MinecraftClient client, Text title, Text subtitle) {
    if (client == null) {
      return;
    }
    client.getToastManager().add(new LootLockToast(title, subtitle));
  }

  @Override
  public int getWidth() {
    return WIDTH;
  }

  @Override
  public int getHeight() {
    return HEIGHT;
  }

  @Override
  public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
    // Two-tone beveled background matching the vanilla GUI panel style. We avoid the system-toast
    // texture sprite directly to keep this toast compatible across small texture-layout changes.
    context.fill(0, 0, WIDTH, HEIGHT, 0xFF1B1B1B);
    context.fill(1, 1, WIDTH - 1, HEIGHT - 1, 0xFFC6C6C6);
    context.fill(2, 2, WIDTH - 2, 3, 0xFFFEFEFE);
    context.fill(2, 2, 3, HEIGHT - 2, 0xFFFEFEFE);
    context.fill(2, HEIGHT - 3, WIDTH - 2, HEIGHT - 2, 0xFF545454);
    context.fill(WIDTH - 3, 2, WIDTH - 2, HEIGHT - 2, 0xFF545454);

    int textX = ICON_INSET + ICON_SIZE + 6;
    int textY = subtitle == null ? (HEIGHT - 8) / 2 : 7;
    context.drawText(manager.getClient().textRenderer, title, textX, textY, 0xFF3B3B3B, false);
    if (subtitle != null) {
      context.drawText(
          manager.getClient().textRenderer, subtitle, textX, textY + 11, 0xFF3B3B3B, false);
    }

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
}
