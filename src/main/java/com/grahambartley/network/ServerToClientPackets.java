package com.grahambartley.network;

import com.grahambartley.LootLock;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class ServerToClientPackets {
  private ServerToClientPackets() {}

  public static void sendServerCapabilities(ServerPlayerEntity player) {
    if (!ServerPlayNetworking.canSend(player, ServerCapabilitiesPayload.ID)) {
      return;
    }

    ServerPlayNetworking.send(
        player, new ServerCapabilitiesPayload(true, LootLockPlayerData.CURRENT_SCHEMA_VERSION));
  }

  public static void sendAuthoritativeSync(ServerPlayerEntity player) {
    if (player == null
        || player.isRemoved()
        || player.getServer() == null
        || !player.getServerWorld().getPlayers().contains(player)) {
      return;
    }

    if (LootLock.PLAYER_DATA_MANAGER == null
        || !ServerPlayNetworking.canSend(player, SyncPayload.ID)) {
      return;
    }

    LootLockPlayerData data = LootLock.PLAYER_DATA_MANAGER.get(player);
    // clientCanEdit reflects data ownership: self-data sync always grants edit rights.
    ServerPlayNetworking.send(
        player, syncPayloadOf(data, true, LootLock.SERVER_CONFIG.allowDeleteRejectedItems()));
  }

  public static SyncPayload syncPayloadOf(LootLockPlayerData data) {
    return syncPayloadOf(
        data, data.isClientCanEdit(), LootLock.SERVER_CONFIG.allowDeleteRejectedItems());
  }

  public static SyncPayload syncPayloadOf(
      LootLockPlayerData data, boolean clientCanEdit, boolean allowDeleteRejectedItems) {
    return new SyncPayload(
        data.getSchemaVersion(),
        data.getPlayerUuid(),
        data.getRevision(),
        data.getActiveProfileId(),
        // Defensive copy: a later mutation of the manager's backing list must not poison an
        // in-flight payload before it is encoded.
        List.copyOf(data.getProfiles()),
        clientCanEdit,
        allowDeleteRejectedItems);
  }

  public static boolean sendBlockedNotice(
      ServerPlayerEntity player, Identifier itemId, int count, boolean deleted) {
    if (player == null || !ServerPlayNetworking.canSend(player, BlockedNoticePayload.ID)) {
      return false;
    }

    ServerPlayNetworking.send(player, new BlockedNoticePayload(itemId, count, deleted));
    return true;
  }

  public record SyncPayload(
      int schemaVersion,
      UUID playerUuid,
      long revision,
      UUID activeProfileId,
      List<LootLockProfile> profiles,
      boolean clientCanEdit,
      boolean allowDeleteRejectedItems)
      implements CustomPayload {
    public static final CustomPayload.Id<SyncPayload> ID =
        new CustomPayload.Id<>(PacketIds.SYNC_PLAYER_DATA_S2C);
    public static final PacketCodec<PacketByteBuf, SyncPayload> CODEC =
        PacketCodec.of(SyncPayload::write, SyncPayload::read);

    private static void write(SyncPayload payload, PacketByteBuf buf) {
      buf.writeVarInt(payload.schemaVersion());
      buf.writeUuid(payload.playerUuid());
      buf.writeVarLong(payload.revision());
      buf.writeBoolean(payload.activeProfileId() != null);
      if (payload.activeProfileId() != null) {
        buf.writeUuid(payload.activeProfileId());
      }
      buf.writeVarInt(payload.profiles().size());
      for (LootLockProfile profile : payload.profiles()) {
        LootLockPayloads.writeProfile(profile, buf);
      }
      buf.writeBoolean(payload.clientCanEdit());
      buf.writeBoolean(payload.allowDeleteRejectedItems());
    }

    private static SyncPayload read(PacketByteBuf buf) {
      int schemaVersion = buf.readVarInt();
      UUID playerUuid = buf.readUuid();
      long revision = buf.readVarLong();
      UUID activeProfileId = buf.readBoolean() ? buf.readUuid() : null;
      int profileCount =
          LootLockPayloads.readBoundedCount(buf, PacketLimits.MAX_PROFILES, "profile");
      List<LootLockProfile> profiles = new ArrayList<>(profileCount);
      for (int i = 0; i < profileCount; i++) {
        profiles.add(LootLockPayloads.readProfile(buf));
      }
      boolean clientCanEdit = buf.readBoolean();
      boolean allowDeleteRejectedItems = buf.readBoolean();
      return new SyncPayload(
          schemaVersion,
          playerUuid,
          revision,
          activeProfileId,
          profiles,
          clientCanEdit,
          allowDeleteRejectedItems);
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record ServerCapabilitiesPayload(boolean supported, int schemaVersion)
      implements CustomPayload {
    public static final CustomPayload.Id<ServerCapabilitiesPayload> ID =
        new CustomPayload.Id<>(PacketIds.SERVER_CAPABILITIES_S2C);
    public static final PacketCodec<PacketByteBuf, ServerCapabilitiesPayload> CODEC =
        PacketCodec.of(
            (payload, buf) -> {
              buf.writeBoolean(payload.supported());
              buf.writeVarInt(payload.schemaVersion());
            },
            buf -> new ServerCapabilitiesPayload(buf.readBoolean(), buf.readVarInt()));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record BlockedNoticePayload(Identifier itemId, int count, boolean deleted)
      implements CustomPayload {
    public BlockedNoticePayload {
      count = Math.max(1, count);
    }

    public static final CustomPayload.Id<BlockedNoticePayload> ID =
        new CustomPayload.Id<>(PacketIds.BLOCKED_NOTICE_S2C);
    public static final PacketCodec<PacketByteBuf, BlockedNoticePayload> CODEC =
        PacketCodec.of(
            (payload, buf) -> {
              buf.writeIdentifier(payload.itemId());
              buf.writeVarInt(payload.count());
              buf.writeBoolean(payload.deleted());
            },
            buf ->
                new BlockedNoticePayload(
                    buf.readIdentifier(), buf.readVarInt(), buf.readBoolean()));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
      return ID;
    }
  }
}
