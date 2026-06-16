package com.grahambartley.client.screen.inventory;

import com.grahambartley.client.config.ClientSettings;
import com.grahambartley.client.config.ClientSettingsManager;
import com.grahambartley.text.LootLockLang;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class InventoryOnboardingController {
  private InventoryOnboardingController() {}

  public static void maybeShow(MinecraftClient client, ClientSettingsManager manager) {
    if (client == null || manager == null) {
      return;
    }
    ClientSettings settings = manager.getSettingsCopy();
    if (!shouldShowOnboarding(settings)) {
      return;
    }
    LootLockToast.show(
        client,
        Text.translatable(LootLockLang.ONBOARDING_TITLE),
        Text.translatable(LootLockLang.ONBOARDING_BODY));
    settings.setHasSeenOnboarding(true);
    manager.replaceAndSave(settings);
  }

  public static boolean shouldShowOnboarding(ClientSettings settings) {
    return settings != null && !settings.hasSeenOnboarding();
  }
}
