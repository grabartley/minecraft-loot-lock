package com.grahambartley.client;

import com.grahambartley.LootLock;
import com.grahambartley.network.ClientToServerPackets;
import com.grahambartley.network.PacketIds;
import com.grahambartley.network.ServerToClientPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

public class LootLockClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    ClientPlayNetworking.registerGlobalReceiver(
        PacketIds.SERVER_CAPABILITIES_S2C,
        (client, handler, buf, responseSender) -> {
          buf.readBoolean();
          int schemaVersion = buf.readVarInt();
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
              () ->
                  LootLock.LOGGER.debug(
                      "Received authoritative sync: player={}, revision={}, profiles={}",
                      payload.playerUuid(),
                      payload.revision(),
                      payload.profiles().size()));
        });
  }
}
