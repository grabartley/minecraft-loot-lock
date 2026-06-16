package com.grahambartley.client;

import com.grahambartley.LootLock;
import com.grahambartley.client.command.LootLockClientCommands;
import com.grahambartley.client.config.ClientSettingsManager;
import com.grahambartley.client.hud.BlockedNoticePresenter;
import com.grahambartley.client.keybind.LootLockKeybinds;
import com.grahambartley.client.screen.inventory.LootLockInventoryPanel;
import com.grahambartley.client.screen.inventory.LootLockPanelHolder;
import com.grahambartley.client.screen.inventory.RulesTagCatalog;
import com.grahambartley.client.state.ClientLootLockState;
import com.grahambartley.network.ClientToServerPackets;
import com.grahambartley.network.PacketIds;
import com.grahambartley.network.ServerToClientPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
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
          if (ClientPlayNetworking.canSend(PacketIds.REQUEST_SYNC_C2S)) {
            ClientPlayNetworking.send(PacketIds.REQUEST_SYNC_C2S, PacketByteBufs.create());
          }
        });

    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> STATE.clear());
    LootLockKeybinds.register();
    LootLockClientCommands.register();

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
        PacketIds.SERVER_CAPABILITIES_S2C,
        (client, handler, buf, responseSender) -> {
          boolean serverSupportsLootLock = buf.readBoolean();
          int schemaVersion = buf.readVarInt();
          String modVersion =
              FabricLoader.getInstance()
                  .getModContainer("loot-lock")
                  .map(container -> container.getMetadata().getVersion().getFriendlyString())
                  .orElse("unknown");
          client.execute(
              () -> {
                STATE.onServerCapabilities(serverSupportsLootLock);
                ClientPlayNetworking.send(
                    PacketIds.HELLO_C2S,
                    ClientToServerPackets.writeHelloPayload(modVersion, schemaVersion));
              });
        });

    ClientPlayNetworking.registerGlobalReceiver(
        PacketIds.SYNC_PLAYER_DATA_S2C,
        (client, handler, buf, responseSender) -> {
          ServerToClientPackets.SyncPayload payload = ServerToClientPackets.readSyncPayload(buf);
          client.execute(
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
              });
        });

    ClientPlayNetworking.registerGlobalReceiver(
        PacketIds.BLOCKED_NOTICE_S2C,
        (client, handler, buf, responseSender) -> {
          ServerToClientPackets.BlockedNoticePayload payload =
              ServerToClientPackets.readBlockedNoticePayload(buf);
          client.execute(
              () ->
                  BlockedNoticePresenter.show(
                      client,
                      clientSettingsManager.getSettingsCopy(),
                      payload.itemId(),
                      payload.deleted()));
        });
  }
}
