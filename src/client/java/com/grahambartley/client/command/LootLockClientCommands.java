package com.grahambartley.client.command;

import com.grahambartley.client.LootLockClient;
import com.grahambartley.client.config.ClientSettingsManager;
import com.grahambartley.client.screen.inventory.InventoryOnboardingController;
import com.grahambartley.text.LootLockLang;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;

public final class LootLockClientCommands {
  private LootLockClientCommands() {}

  public static void register() {
    ClientCommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess) ->
            dispatcher.register(
                ClientCommandManager.literal("lootlock")
                    .then(
                        ClientCommandManager.literal("client")
                            .then(
                                ClientCommandManager.literal("reset-onboarding")
                                    .executes(
                                        ctx -> {
                                          ClientSettingsManager manager =
                                              LootLockClient.getClientSettingsManager();
                                          if (!resetOnboarding(manager)) {
                                            ctx.getSource()
                                                .sendError(
                                                    Text.translatable(
                                                        LootLockLang
                                                            .COMMAND_CLIENT_RESET_ONBOARDING_UNAVAILABLE));
                                            return 0;
                                          }
                                          ctx.getSource()
                                              .sendFeedback(
                                                  Text.translatable(
                                                      LootLockLang
                                                          .COMMAND_CLIENT_RESET_ONBOARDING_OK));
                                          return 1;
                                        })))));
  }

  public static boolean resetOnboarding(ClientSettingsManager manager) {
    if (manager == null) {
      return false;
    }
    InventoryOnboardingController.reset(manager);
    return true;
  }
}
