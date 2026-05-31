package com.grahambartley.network;

import com.grahambartley.LootLock;
import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.data.RuleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ServerToClientPackets {
  private static final int MAX_PROFILE_NAME_LENGTH = 64;
  private static final int MAX_RULE_ID_LENGTH = 256;

  private ServerToClientPackets() {}

  public static void sendServerCapabilities(ServerPlayerEntity player) {
    if (!ServerPlayNetworking.canSend(player, PacketIds.SERVER_CAPABILITIES_S2C)) {
      return;
    }

    PacketByteBuf buf = PacketByteBufs.create();
    buf.writeBoolean(true);
    buf.writeVarInt(LootLockPlayerData.CURRENT_SCHEMA_VERSION);
    ServerPlayNetworking.send(player, PacketIds.SERVER_CAPABILITIES_S2C, buf);
  }

  public static void sendAuthoritativeSync(ServerPlayerEntity player) {
    if (player == null
        || player.isRemoved()
        || player.getServer() == null
        || !player.getServerWorld().getPlayers().contains(player)) {
      return;
    }

    if (LootLock.PLAYER_DATA_MANAGER == null
        || !ServerPlayNetworking.canSend(player, PacketIds.SYNC_PLAYER_DATA_S2C)) {
      return;
    }

    LootLockPlayerData data = LootLock.PLAYER_DATA_MANAGER.get(player);
    ServerPlayNetworking.send(player, PacketIds.SYNC_PLAYER_DATA_S2C, writeSyncPayload(data));
  }

  public static PacketByteBuf writeSyncPayload(LootLockPlayerData data) {
    PacketByteBuf buf = PacketByteBufs.create();
    buf.writeVarInt(data.getSchemaVersion());
    buf.writeUuid(data.getPlayerUuid());
    buf.writeVarLong(data.getRevision());
    buf.writeBoolean(data.getActiveProfileId() != null);
    if (data.getActiveProfileId() != null) {
      buf.writeUuid(data.getActiveProfileId());
    }
    buf.writeVarInt(data.getProfiles().size());
    for (LootLockProfile profile : data.getProfiles()) {
      writeProfile(buf, profile);
    }
    buf.writeBoolean(data.isClientCanEdit());
    return buf;
  }

  public static SyncPayload readSyncPayload(PacketByteBuf buf) {
    int schemaVersion = buf.readVarInt();
    UUID playerUuid = buf.readUuid();
    long revision = buf.readVarLong();
    UUID activeProfileId = buf.readBoolean() ? buf.readUuid() : null;
    int profileCount = buf.readVarInt();
    List<LootLockProfile> profiles = new ArrayList<>(profileCount);
    for (int i = 0; i < profileCount; i++) {
      profiles.add(readProfile(buf));
    }
    boolean clientCanEdit = buf.readBoolean();
    return new SyncPayload(
        schemaVersion, playerUuid, revision, activeProfileId, profiles, clientCanEdit);
  }

  private static void writeProfile(PacketByteBuf buf, LootLockProfile profile) {
    buf.writeUuid(profile.getId());
    buf.writeString(profile.getName(), MAX_PROFILE_NAME_LENGTH);
    buf.writeEnumConstant(profile.getMode());
    buf.writeEnumConstant(profile.getRejectedItemAction());
    buf.writeBoolean(profile.isEnabled());
    buf.writeVarInt(profile.getRules().size());
    for (RuleEntry rule : profile.getRules()) {
      buf.writeString(rule.itemId(), MAX_RULE_ID_LENGTH);
    }
  }

  private static LootLockProfile readProfile(PacketByteBuf buf) {
    UUID profileId = buf.readUuid();
    String profileName = buf.readString(MAX_PROFILE_NAME_LENGTH);
    FilterMode mode = buf.readEnumConstant(FilterMode.class);
    RejectedItemAction action = buf.readEnumConstant(RejectedItemAction.class);
    boolean enabled = buf.readBoolean();
    int ruleCount = buf.readVarInt();
    List<RuleEntry> rules = new ArrayList<>(ruleCount);
    for (int i = 0; i < ruleCount; i++) {
      rules.add(new RuleEntry(buf.readString(MAX_RULE_ID_LENGTH)));
    }
    return new LootLockProfile(profileId, profileName, mode, action, enabled, rules);
  }

  public record SyncPayload(
      int schemaVersion,
      UUID playerUuid,
      long revision,
      UUID activeProfileId,
      List<LootLockProfile> profiles,
      boolean clientCanEdit) {}
}
