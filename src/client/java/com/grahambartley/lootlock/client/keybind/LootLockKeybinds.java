package com.grahambartley.lootlock.client.keybind;

import com.grahambartley.lootlock.client.LootLockClient;
import com.grahambartley.lootlock.client.config.ClientSettings;
import com.grahambartley.lootlock.client.network.ClientMutationSync;
import com.grahambartley.lootlock.client.screen.inventory.GlobalEnableController;
import com.grahambartley.lootlock.client.screen.inventory.LootLockToast;
import com.grahambartley.lootlock.client.screen.inventory.ProfileColors;
import com.grahambartley.lootlock.client.state.ClientLootLockState;
import com.grahambartley.lootlock.data.LootLockPlayerData;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.text.LootLockLang;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.lwjgl.glfw.GLFW;

public final class LootLockKeybinds {
  private static final String CATEGORY = LootLockLang.KEY_CATEGORY;
  private static KeyBinding TOGGLE_ENABLED;
  private static KeyBinding CYCLE_PROFILE;
  private static KeyBinding ADD_HOVERED;

  public static boolean matchesCycleProfile(int keyCode, int scanCode) {
    return CYCLE_PROFILE != null && CYCLE_PROFILE.matchesKey(keyCode, scanCode);
  }

  public static boolean matchesToggleEnabled(int keyCode, int scanCode) {
    return TOGGLE_ENABLED != null && TOGGLE_ENABLED.matchesKey(keyCode, scanCode);
  }

  public static boolean matchesAddHovered(int keyCode, int scanCode) {
    return ADD_HOVERED != null && ADD_HOVERED.matchesKey(keyCode, scanCode);
  }

  public static KeyBinding getToggleEnabled() {
    return TOGGLE_ENABLED;
  }

  public static KeyBinding getCycleProfile() {
    return CYCLE_PROFILE;
  }

  public static KeyBinding getAddHovered() {
    return ADD_HOVERED;
  }

  public static void cycleProfileNow(MinecraftClient client) {
    cycleProfile(client);
  }

  public static void toggleEnabledNow(MinecraftClient client) {
    GlobalEnableController.toggle(client);
  }

  private LootLockKeybinds() {}

  public static void register() {
    TOGGLE_ENABLED =
        KeyBindingHelper.registerKeyBinding(
            new KeyBinding(LootLockLang.KEY_TOGGLE_ENABLED, GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
    CYCLE_PROFILE =
        KeyBindingHelper.registerKeyBinding(
            new KeyBinding(LootLockLang.KEY_CYCLE_PROFILE, GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
    ADD_HOVERED =
        KeyBindingHelper.registerKeyBinding(
            new KeyBinding(LootLockLang.KEY_ADD_HOVERED, GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
    ClientTickEvents.END_CLIENT_TICK.register(LootLockKeybinds::onEndClientTick);
  }

  private static void onEndClientTick(MinecraftClient client) {
    while (TOGGLE_ENABLED.wasPressed()) {
      if (client.player == null || client.world == null) {
        continue;
      }
      toggleEnabledNow(client);
    }

    while (CYCLE_PROFILE.wasPressed()) {
      cycleProfile(client);
    }

    discardPresses(ADD_HOVERED);
  }

  private static void discardPresses(KeyBinding binding) {
    while (binding.wasPressed()) {}
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
    int rgb = ProfileColors.colorForProfile(nextProfile) & 0xFFFFFF;
    LootLockToast.show(
        client,
        Text.translatable(LootLockLang.TOAST_PROFILE_SWITCHED),
        Text.literal(nextProfile.getName())
            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
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
