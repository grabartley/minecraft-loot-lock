package com.grahambartley.client;

import com.grahambartley.LootLock;
import com.grahambartley.client.state.ClientLootLockState;
import com.grahambartley.network.ClientToServerPackets;
import com.grahambartley.network.PacketIds;
import com.grahambartley.network.ServerToClientPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.loader.api.FabricLoader;

public class LootLockClient implements ClientModInitializer {
  private static final ClientLootLockState STATE = new ClientLootLockState();

  public static ClientLootLockState getState() {
    return STATE;
  }

  @Override
  public void onInitializeClient() {
    ClientPlayConnectionEvents.JOIN.register(
        (handler, sender, client) -> {
          STATE.onLogin();
          if (STATE.shouldRequestSync(ClientPlayNetworking.canSend(PacketIds.REQUEST_SYNC_C2S))) {
            ClientPlayNetworking.send(PacketIds.REQUEST_SYNC_C2S, PacketByteBufs.create());
          }
        });

    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> STATE.clear());

    ClientPlayNetworking.registerGlobalReceiver(
        PacketIds.SERVER_CAPABILITIES_S2C,
        (client, handler, buf, responseSender) -> {
          boolean serverSupportsLootLock = buf.readBoolean();
          int schemaVersion = buf.readVarInt();
          STATE.onServerCapabilities(serverSupportsLootLock);
          String modVersion =
              FabricLoader.getInstance()
                  .getModContainer("loot-lock")
                  .map(container -> container.getMetadata().getVersion().getFriendlyString())
                  .orElse("unknown");
          ClientPlayNetworking.send(
              PacketIds.HELLO_C2S,
              ClientToServerPackets.writeHelloPayload(modVersion, schemaVersion));
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
  }
}
