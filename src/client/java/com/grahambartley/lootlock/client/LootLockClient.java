package com.grahambartley.lootlock.client;

import com.grahambartley.lootlock.LootLock;
import com.grahambartley.lootlock.client.compat.RecipeViewerBridge;
import com.grahambartley.lootlock.client.config.ClientSettingsManager;
import com.grahambartley.lootlock.client.hud.BlockedNoticePresenter;
import com.grahambartley.lootlock.client.keybind.LootLockKeybinds;
import com.grahambartley.lootlock.client.screen.inventory.LootLockInventoryPanel;
import com.grahambartley.lootlock.client.screen.inventory.LootLockPanelHolder;
import com.grahambartley.lootlock.client.screen.inventory.RulesTagCatalog;
import com.grahambartley.lootlock.client.state.ClientLootLockState;
import com.grahambartley.lootlock.network.ClientToServerPackets;
import com.grahambartley.lootlock.network.ServerToClientPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

public class LootLockClient implements ClientModInitializer {
  private static final ClientLootLockState STATE = new ClientLootLockState();
  private static ClientSettingsManager clientSettingsManager;

  public static ClientLootLockState getState() {
    return STATE;
  }

  public static ClientSettingsManager getClientSettingsManager() {
    return clientSettingsManager;
  }

  @Override
  public void onInitializeClient() {
    clientSettingsManager =
        new ClientSettingsManager(
            FabricLoader.getInstance().getConfigDir().resolve("loot-lock-client.json"));
    clientSettingsManager.load();

    ClientPlayConnectionEvents.JOIN.register(
        (handler, sender, client) -> {
          STATE.onLogin();
          if (ClientPlayNetworking.canSend(ClientToServerPackets.RequestSyncPayload.ID)) {
            ClientPlayNetworking.send(ClientToServerPackets.RequestSyncPayload.INSTANCE);
          }
        });

    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> STATE.clear());
    LootLockKeybinds.register();

    CommonLifecycleEvents.TAGS_LOADED.register(
        (registries, client) -> RulesTagCatalog.invalidate());

    ScreenEvents.AFTER_INIT.register(
        (client, screen, scaledWidth, scaledHeight) -> {
          if (RecipeViewerBridge.isRecipeViewerLoaded()) {
            ScreenKeyboardEvents.allowKeyPress(screen)
                .register(
                    (s, key, scancode, modifiers) ->
                        !(LootLockKeybinds.matchesAddHovered(key, scancode)
                            && RecipeViewerBridge.handleAddHoveredKey(client, s)));
          }
          if (!(screen instanceof InventoryScreen)) {
            return;
          }
          if (!(screen instanceof LootLockPanelHolder holder)) {
            return;
          }
          ScreenMouseEvents.allowMouseScroll(screen)
              .register(
                  (s, mouseX, mouseY, horizontalAmount, verticalAmount) -> {
                    LootLockInventoryPanel panel = holder.lootlock$getPanel();
                    if (panel == null) {
                      return true;
                    }
                    return !panel.handleMouseScroll(mouseX, mouseY, verticalAmount);
                  });
        });

    ClientPlayNetworking.registerGlobalReceiver(
        ServerToClientPackets.ServerCapabilitiesPayload.ID,
        (payload, context) -> {
          String modVersion =
              FabricLoader.getInstance()
                  .getModContainer("loot-lock")
                  .map(container -> container.getMetadata().getVersion().getFriendlyString())
                  .orElse("unknown");
          STATE.onServerCapabilities(payload.supported());
          ClientPlayNetworking.send(
              new ClientToServerPackets.HelloPayload(modVersion, payload.schemaVersion()));
        });

    ClientPlayNetworking.registerGlobalReceiver(
        ServerToClientPackets.SyncPayload.ID,
        (payload, context) -> {
          STATE.onAuthoritativeSync(payload);
          LootLock.LOGGER.debug(
              "Client state synced: supported={}, synced={}, revision={}",
              STATE.isServerSupportsLootLock(),
              STATE.isSynced(),
              payload.revision());
          LootLock.LOGGER.debug(
              "Received authoritative sync: player={}, revision={}, profiles={}",
              payload.playerUuid(),
              payload.revision(),
              payload.profiles().size());
        });

    ClientPlayNetworking.registerGlobalReceiver(
        ServerToClientPackets.BlockedNoticePayload.ID,
        (payload, context) ->
            BlockedNoticePresenter.show(
                context.client(),
                clientSettingsManager.getSettingsCopy(),
                payload.itemId(),
                payload.deleted()));
  }
}
