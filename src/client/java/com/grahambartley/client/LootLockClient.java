package com.grahambartley.client;

import com.grahambartley.LootLock;
import com.grahambartley.client.config.ClientSettingsManager;
import com.grahambartley.client.hud.BlockedNoticePresenter;
import com.grahambartley.client.keybind.LootLockKeybinds;
import com.grahambartley.client.screen.inventory.LootLockInventoryPanel;
import com.grahambartley.client.screen.inventory.LootLockPanelHolder;
import com.grahambartley.client.screen.inventory.RulesTagCatalog;
import com.grahambartley.client.state.ClientLootLockState;
import com.grahambartley.network.ClientToServerPackets;
import com.grahambartley.network.ServerToClientPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
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

    // Hook the survival inventory's mouse scroll so wheeling over the Rules results list paginates
    // through items. Vanilla InventoryScreen does not declare mouseScrolled, so a direct mixin into
    // the method cannot find a target. Using the Fabric screen event sidesteps that and only
    // suppresses the wheel event when the panel actually consumed it.
    ScreenEvents.AFTER_INIT.register(
        (client, screen, scaledWidth, scaledHeight) -> {
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
          boolean serverSupportsLootLock = payload.supported();
          int schemaVersion = payload.schemaVersion();
          String modVersion =
              FabricLoader.getInstance()
                  .getModContainer("loot-lock")
                  .map(container -> container.getMetadata().getVersion().getFriendlyString())
                  .orElse("unknown");
          context
              .client()
              .execute(
                  () -> {
                    STATE.onServerCapabilities(serverSupportsLootLock);
                    ClientPlayNetworking.send(
                        new ClientToServerPackets.HelloPayload(modVersion, schemaVersion));
                  });
        });

    ClientPlayNetworking.registerGlobalReceiver(
        ServerToClientPackets.SyncPayload.ID,
        (payload, context) ->
            context
                .client()
                .execute(
                    () -> {
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
                    }));

    ClientPlayNetworking.registerGlobalReceiver(
        ServerToClientPackets.BlockedNoticePayload.ID,
        (payload, context) ->
            context
                .client()
                .execute(
                    () ->
                        BlockedNoticePresenter.show(
                            context.client(),
                            clientSettingsManager.getSettingsCopy(),
                            payload.itemId(),
                            payload.deleted())));
  }
}
