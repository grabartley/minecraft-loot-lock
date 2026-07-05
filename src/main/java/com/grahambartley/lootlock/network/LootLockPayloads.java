package com.grahambartley.lootlock.network;

import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.data.RuleEntry;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;

public final class LootLockPayloads {
  public static final PacketCodec<PacketByteBuf, LootLockProfile> PROFILE_CODEC =
      PacketCodec.of(LootLockPayloads::writeProfile, LootLockPayloads::readProfile);

  private static boolean typesRegistered = false;

  private LootLockPayloads() {}

  public static void registerTypes() {
    if (typesRegistered) {
      return;
    }
    typesRegistered = true;

    PayloadTypeRegistry.playC2S()
        .register(ClientToServerPackets.HelloPayload.ID, ClientToServerPackets.HelloPayload.CODEC);
    PayloadTypeRegistry.playC2S()
        .register(
            ClientToServerPackets.RequestSyncPayload.ID,
            ClientToServerPackets.RequestSyncPayload.CODEC);
    PayloadTypeRegistry.playC2S()
        .register(
            ClientToServerPackets.UpdateProfilePayload.ID,
            ClientToServerPackets.UpdateProfilePayload.CODEC);
    PayloadTypeRegistry.playC2S()
        .register(
            ClientToServerPackets.ActivateProfilePayload.ID,
            ClientToServerPackets.ActivateProfilePayload.CODEC);
    PayloadTypeRegistry.playC2S()
        .register(
            ClientToServerPackets.CreateProfilePayload.ID,
            ClientToServerPackets.CreateProfilePayload.CODEC);
    PayloadTypeRegistry.playC2S()
        .register(
            ClientToServerPackets.DeleteProfilePayload.ID,
            ClientToServerPackets.DeleteProfilePayload.CODEC);
    PayloadTypeRegistry.playC2S()
        .register(
            ClientToServerPackets.UpdateServerPolicyPayload.ID,
            ClientToServerPackets.UpdateServerPolicyPayload.CODEC);
    PayloadTypeRegistry.playC2S()
        .register(
            ClientToServerPackets.UpdateGlobalEnablePayload.ID,
            ClientToServerPackets.UpdateGlobalEnablePayload.CODEC);

    PayloadTypeRegistry.playS2C()
        .register(ServerToClientPackets.SyncPayload.ID, ServerToClientPackets.SyncPayload.CODEC);
    PayloadTypeRegistry.playS2C()
        .register(
            ServerToClientPackets.ServerCapabilitiesPayload.ID,
            ServerToClientPackets.ServerCapabilitiesPayload.CODEC);
    PayloadTypeRegistry.playS2C()
        .register(
            ServerToClientPackets.BlockedNoticePayload.ID,
            ServerToClientPackets.BlockedNoticePayload.CODEC);
  }

  static void writeProfile(LootLockProfile profile, PacketByteBuf buf) {
    buf.writeUuid(profile.getId());
    buf.writeString(profile.getName(), PacketLimits.MAX_PROFILE_NAME_LENGTH);
    buf.writeEnumConstant(profile.getMode());
    buf.writeEnumConstant(profile.getRejectedItemAction());
    buf.writeBoolean(profile.isEnabled());
    buf.writeInt(profile.getColor());
    buf.writeVarInt(profile.getRules().size());
    for (RuleEntry rule : profile.getRules()) {
      buf.writeString(rule.itemId(), PacketLimits.MAX_RULE_ID_LENGTH);
    }
  }

  static LootLockProfile readProfile(PacketByteBuf buf) {
    UUID profileId = buf.readUuid();
    String profileName = buf.readString(PacketLimits.MAX_PROFILE_NAME_LENGTH);
    FilterMode mode = buf.readEnumConstant(FilterMode.class);
    RejectedItemAction action = buf.readEnumConstant(RejectedItemAction.class);
    boolean enabled = buf.readBoolean();
    int color = buf.readInt();
    int ruleCount = readBoundedCount(buf, PacketLimits.MAX_RULES_PER_PROFILE, "rule");
    List<RuleEntry> rules = new ArrayList<>(ruleCount);
    for (int i = 0; i < ruleCount; i++) {
      rules.add(new RuleEntry(buf.readString(PacketLimits.MAX_RULE_ID_LENGTH)));
    }
    return new LootLockProfile(profileId, profileName, mode, action, enabled, color, rules);
  }

  // The count arrives before its elements and sizes an allocation, so it must be bounded before
  // it is trusted; a hostile peer can claim any varint regardless of actual payload size.
  static int readBoundedCount(PacketByteBuf buf, int max, String kind) {
    int count = buf.readVarInt();
    if (count < 0 || count > max) {
      throw new DecoderException(kind + " count " + count + " is outside [0, " + max + "]");
    }
    return count;
  }
}
