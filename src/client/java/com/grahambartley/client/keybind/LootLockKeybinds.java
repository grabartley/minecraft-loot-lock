package com.grahambartley.client.keybind;

import com.grahambartley.client.LootLockClient;
import com.grahambartley.client.config.ClientSettings;
import com.grahambartley.client.screen.inventory.GlobalEnableController;
import com.grahambartley.client.state.ClientLootLockState;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.network.ClientMutationSync;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public final class LootLockKeybinds {
  private static final String CATEGORY = "key.categories.loot-lock";
  private static final KeyBinding TOGGLE_ENABLED =
      KeyBindingHelper.registerKeyBinding(
          new KeyBinding("key.loot-lock.toggle_enabled", GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
  private static final KeyBinding CYCLE_PROFILE =
      KeyBindingHelper.registerKeyBinding(
          new KeyBinding("key.loot-lock.cycle_profile", GLFW.GLFW_KEY_UNKNOWN, CATEGORY));

  private LootLockKeybinds() {}

  public static void register() {
    ClientTickEvents.END_CLIENT_TICK.register(LootLockKeybinds::onEndClientTick);
  }

  private static void onEndClientTick(MinecraftClient client) {
    while (TOGGLE_ENABLED.wasPressed()) {
      if (client.player == null || client.world == null) {
        // Drain queued presses while world is unavailable to prevent delayed toggles.
        continue;
      }
      GlobalEnableController.toggle(client);
    }

    while (CYCLE_PROFILE.wasPressed()) {
      cycleProfile(client);
    }
  }

  private static void cycleProfile(MinecraftClient client) {
    ClientLootLockState state = LootLockClient.getState();
    Optional<LootLockPlayerData> snapshotOptional = state.getSnapshot();
    if (snapshotOptional.isEmpty()) {
      return;
    }
    LootLockPlayerData snapshot = snapshotOptional.get();
    if (snapshot.getProfiles().isEmpty()) {
      return;
    }

    int activeIndex = indexOfProfile(snapshot, snapshot.getActiveProfileId());
    if (activeIndex < 0) {
      return;
    }
    LootLockProfile nextProfile =
        snapshot.getProfiles().get((activeIndex + 1) % snapshot.getProfiles().size());
    if (!ClientMutationSync.sendActivateRequest(snapshot.getRevision(), nextProfile.getId())) {
      return;
    }

    ClientSettings settings = LootLockClient.getClientSettingsManager().getSettingsCopy();
    if (!settings.isEnableProfileCycleToast()) {
      return;
    }
    SystemToast.show(
        client.getToastManager(),
        SystemToast.Type.PERIODIC_NOTIFICATION,
        Text.literal("LootLock profile switched"),
        Text.literal(nextProfile.getName()).formatted(Formatting.YELLOW));
  }

  private static int indexOfProfile(LootLockPlayerData data, UUID profileId) {
    if (profileId == null) {
      return -1;
    }
    for (int i = 0; i < data.getProfiles().size(); i++) {
      LootLockProfile profile = data.getProfiles().get(i);
      if (profileId.equals(profile.getId())) {
        return i;
      }
    }
    return -1;
  }
}
