package com.grahambartley.client.hud;

import com.grahambartley.client.config.ClientSettings;
import java.util.Optional;
import java.util.function.Function;
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
      MinecraftClient client, ClientSettings settings, Identifier itemId, boolean deleted) {
    Text message = formatMessage(resolveItemLabel(itemId), deleted);

    if (settings.isShowBlockedHudNotification()) {
      SystemToast.show(
          client.getToastManager(),
          SystemToast.Type.PERIODIC_NOTIFICATION,
          Text.literal("LootLock"),
          message);
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
    return resolveItemLabel(itemId, Registries.ITEM::getOrEmpty);
  }

  static String resolveItemLabel(
      Identifier itemId, Function<Identifier, Optional<Item>> itemLookup) {
    if (itemId == null) {
      return "unknown item";
    }
    Optional<Item> itemOptional = itemLookup.apply(itemId);
    if (itemOptional.isEmpty()) {
      return itemId.toString();
    }
    Item item = itemOptional.get();
    String displayName = item.getName().getString();
    if (displayName == null || displayName.isBlank()) {
      return itemId.toString();
    }
    return displayName;
  }
}
