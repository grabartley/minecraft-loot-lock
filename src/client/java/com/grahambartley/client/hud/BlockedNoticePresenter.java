package com.grahambartley.client.hud;

import com.grahambartley.client.config.ClientSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
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
    Text message = formatMessage(resolveItemLabel(itemId), deleted);

    if (settings.isShowBlockedHudNotification()) {
      SystemToast.show(
          client.getToastManager(),
          SystemToast.Type.PERIODIC_NOTIFICATION,
          Text.literal("LootLock"),
          message);
    }

    if (settings.isShowActionbarFallback() && client.player != null) {
      client.player.sendMessage(Text.literal("[LootLock] ").append(message), true);
    }
  }

  static Text formatMessage(String itemLabel, boolean deleted) {
    String prefix = deleted ? "Deleted" : "Blocked";
    return Text.literal(prefix + " ").append(Text.literal(itemLabel).formatted(Formatting.YELLOW));
  }

  static String resolveItemLabel(Identifier itemId) {
    if (itemId == null) {
      return "unknown item";
    }
    Item item = Registries.ITEM.get(itemId);
    if (item == null) {
      return itemId.toString();
    }
    String displayName = item.getName().getString();
    if (displayName == null || displayName.isBlank()) {
      return itemId.toString();
    }
    return displayName;
  }
}
