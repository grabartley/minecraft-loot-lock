package com.grahambartley.network;

import com.grahambartley.LootLock;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;

public final class ClientToServerPackets {
  private static final int MAX_CLIENT_VERSION_LENGTH = 64;

  private ClientToServerPackets() {}

  public static void register() {
    ServerPlayNetworking.registerGlobalReceiver(
        PacketIds.HELLO_C2S,
        (server, player, handler, buf, responseSender) -> {
          HelloPayload payload = readHelloPayload(buf);
          server.execute(
              () -> {
                LootLock.LOGGER.debug(
                    "Received hello from {}: version={}, schema={}.",
                    player.getUuid(),
                    payload.clientVersion(),
                    payload.schemaVersion());
                ServerToClientPackets.sendAuthoritativeSync(player);
              });
        });

    ServerPlayNetworking.registerGlobalReceiver(
        PacketIds.REQUEST_SYNC_C2S,
        (server, player, handler, buf, responseSender) ->
            server.execute(() -> ServerToClientPackets.sendAuthoritativeSync(player)));
  }

  public static PacketByteBuf writeHelloPayload(String clientVersion, int schemaVersion) {
    PacketByteBuf buf = PacketByteBufs.create();
    buf.writeString(clientVersion == null ? "unknown" : clientVersion, MAX_CLIENT_VERSION_LENGTH);
    buf.writeVarInt(schemaVersion);
    return buf;
  }

  static HelloPayload readHelloPayload(PacketByteBuf buf) {
    String clientVersion = buf.readString(MAX_CLIENT_VERSION_LENGTH);
    int schemaVersion = buf.readVarInt();
    return new HelloPayload(clientVersion, schemaVersion);
  }

  record HelloPayload(String clientVersion, int schemaVersion) {}
}
