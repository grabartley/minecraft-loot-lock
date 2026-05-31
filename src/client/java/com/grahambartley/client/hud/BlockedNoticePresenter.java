package com.grahambartley.client.hud;

import com.grahambartley.client.config.ClientSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class BlockedNoticePresenter {
  private BlockedNoticePresenter() {}

  public static void show(
      MinecraftClient client,
      ClientSettings settings,
      Identifier itemId,
      int count,
      boolean deleted) {
    String prefix = deleted ? "Deleted" : "Blocked";
    Text message =
        Text.literal(prefix + " " + Math.max(1, count) + "x ")
            .append(Text.literal(itemId.toString()).formatted(Formatting.YELLOW));

    if (settings.isShowBlockedHudNotification()) {
      SystemToast.show(
          client.getToastManager(),
          SystemToast.Type.NARRATOR_TOGGLE,
          Text.literal("LootLock"),
          message);
    }

    if (settings.isShowActionbarFallback() && client.player != null) {
      client.player.sendMessage(Text.literal("[LootLock] ").append(message), true);
    }
  }
}
