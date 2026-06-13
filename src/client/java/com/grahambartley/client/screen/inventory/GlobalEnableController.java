package com.grahambartley.client.screen.inventory;

import com.grahambartley.client.LootLockClient;
import com.grahambartley.client.config.ClientSettings;
import com.grahambartley.client.state.ClientLootLockState;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.network.ClientMutationSync;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Single entry point used by both the inventory-panel Client toggle and the toggle-enabled keybind.
 * Computes the new whole-mod enabled state from the current snapshot, sends the authoritative
 * server packet, and surfaces a confirmation toast when the user has opted in.
 */
public final class GlobalEnableController {
  private GlobalEnableController() {}

  public static boolean toggle(MinecraftClient client) {
    ClientLootLockState state = LootLockClient.getState();
    Optional<LootLockPlayerData> snapshotOptional = state.getSnapshot();
    if (snapshotOptional.isEmpty()) {
      return false;
    }

    LootLockPlayerData snapshot = snapshotOptional.get();
    boolean nextEnabled = !snapshot.isGloballyEnabled();
    if (!ClientMutationSync.sendUpdateGlobalEnableRequest(snapshot.getRevision(), nextEnabled)) {
      return false;
    }

    maybeShowToast(client, nextEnabled);
    return true;
  }

  static void maybeShowToast(MinecraftClient client, boolean nextEnabled) {
    if (client == null) {
      return;
    }
    ClientSettings settings =
        LootLockClient.getClientSettingsManager() == null
            ? ClientSettings.defaults()
            : LootLockClient.getClientSettingsManager().getSettingsCopy();
    if (!shouldShowToast(settings)) {
      return;
    }
    LootLockToast.show(
        client,
        Text.literal("Loot Lock"),
        Text.literal(nextEnabled ? "Enabled" : "Disabled")
            .formatted(nextEnabled ? Formatting.GREEN : Formatting.RED));
  }

  /** Pure decision used by the controller and verifiable in unit tests. */
  public static boolean shouldShowToast(ClientSettings settings) {
    return settings != null && settings.isEnableToggleToast();
  }
}
