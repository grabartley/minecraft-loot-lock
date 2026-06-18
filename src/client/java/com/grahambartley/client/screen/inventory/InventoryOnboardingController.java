package com.grahambartley.client.screen.inventory;

import com.grahambartley.client.config.ClientSettings;
import com.grahambartley.client.config.ClientSettingsManager;
import com.grahambartley.text.LootLockLang;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class InventoryOnboardingController {
  private InventoryOnboardingController() {}

  @FunctionalInterface
  public interface ToastDispatcher {
    void show(Text title, Text body);
  }

  static final ToastDispatcher DEFAULT_DISPATCHER =
      (title, body) -> LootLockToast.show(MinecraftClient.getInstance(), title, body);

  static ToastDispatcher dispatcher = DEFAULT_DISPATCHER;

  public static void maybeShow(ClientSettingsManager manager) {
    if (manager == null) {
      return;
    }
    ClientSettings settings = manager.getSettingsCopy();
    if (!shouldShowOnboarding(settings)) {
      return;
    }
    settings.setHasSeenOnboarding(true);
    manager.replaceAndSave(settings);
    dispatcher.show(
        Text.translatable(LootLockLang.ONBOARDING_TITLE),
        Text.translatable(LootLockLang.ONBOARDING_BODY));
  }

  public static boolean shouldShowOnboarding(ClientSettings settings) {
    return settings != null && !settings.hasSeenOnboarding();
  }
}
