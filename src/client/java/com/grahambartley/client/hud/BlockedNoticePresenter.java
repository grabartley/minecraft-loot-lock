package com.grahambartley.client.hud;

import com.grahambartley.client.config.ClientSettings;
import com.grahambartley.client.screen.inventory.LootLockToast;
import com.grahambartley.text.LootLockLang;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.MinecraftClient;
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
      LootLockToast.show(client, Text.translatable(LootLockLang.BRAND), message);
    }
  }

  static Text formatMessage(String itemLabel, boolean deleted) {
    Text prefix =
        Text.translatable(deleted ? LootLockLang.BLOCKED_DELETED : LootLockLang.BLOCKED_BLOCKED);
    return prefix
        .copy()
        .append(Text.literal(" "))
        .append(Text.literal(itemLabel).formatted(Formatting.YELLOW));
  }

  static String resolveItemLabel(Identifier itemId) {
    if (itemId == null) {
      return Text.translatable(LootLockLang.BLOCKED_UNKNOWN_ITEM).getString();
    }
    return resolveItemLabel(itemId, Registries.ITEM::getOrEmpty);
  }

  static String resolveItemLabel(
      Identifier itemId, Function<Identifier, Optional<Item>> itemLookup) {
    if (itemId == null) {
      return Text.translatable(LootLockLang.BLOCKED_UNKNOWN_ITEM).getString();
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
