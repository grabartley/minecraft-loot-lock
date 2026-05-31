package com.grahambartley.network;

import com.grahambartley.LootLock;
import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.data.RuleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;

public final class ClientToServerPackets {
  private static final int MAX_CLIENT_VERSION_LENGTH = 64;
  private static final int MAX_PROFILE_NAME_LENGTH = 64;
  private static final int MAX_RULE_ID_LENGTH = 256;
  private static final int MAX_PROFILES = 64;
  private static final int MAX_RULES_PER_PROFILE = 1024;

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

    ServerPlayNetworking.registerGlobalReceiver(
        PacketIds.UPDATE_PROFILE_C2S,
        (server, player, handler, buf, responseSender) -> {
          UpdateProfilePayload payload = readUpdateProfilePayload(buf);
          server.execute(() -> handleUpdateProfile(player, payload));
        });

    ServerPlayNetworking.registerGlobalReceiver(
        PacketIds.ACTIVATE_PROFILE_C2S,
        (server, player, handler, buf, responseSender) -> {
          ActivateProfilePayload payload = readActivateProfilePayload(buf);
          server.execute(() -> handleActivateProfile(player, payload));
        });

    ServerPlayNetworking.registerGlobalReceiver(
        PacketIds.CREATE_PROFILE_C2S,
        (server, player, handler, buf, responseSender) -> {
          CreateProfilePayload payload = readCreateProfilePayload(buf);
          server.execute(() -> handleCreateProfile(player, payload));
        });

    ServerPlayNetworking.registerGlobalReceiver(
        PacketIds.DELETE_PROFILE_C2S,
        (server, player, handler, buf, responseSender) -> {
          DeleteProfilePayload payload = readDeleteProfilePayload(buf);
          server.execute(() -> handleDeleteProfile(player, payload));
        });
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

  static PacketByteBuf writeUpdateProfilePayload(long baseRevision, LootLockProfile profile) {
    PacketByteBuf buf = PacketByteBufs.create();
    buf.writeVarLong(baseRevision);
    writeProfile(buf, profile);
    return buf;
  }

  static UpdateProfilePayload readUpdateProfilePayload(PacketByteBuf buf) {
    return new UpdateProfilePayload(buf.readVarLong(), readProfile(buf));
  }

  static PacketByteBuf writeActivateProfilePayload(long baseRevision, UUID profileId) {
    PacketByteBuf buf = PacketByteBufs.create();
    buf.writeVarLong(baseRevision);
    buf.writeUuid(profileId);
    return buf;
  }

  static ActivateProfilePayload readActivateProfilePayload(PacketByteBuf buf) {
    return new ActivateProfilePayload(buf.readVarLong(), buf.readUuid());
  }

  static PacketByteBuf writeCreateProfilePayload(
      long baseRevision, String name, LootLockProfile copyFromProfile) {
    PacketByteBuf buf = PacketByteBufs.create();
    buf.writeVarLong(baseRevision);
    buf.writeString(name == null ? "" : name, MAX_PROFILE_NAME_LENGTH);
    buf.writeBoolean(copyFromProfile != null);
    if (copyFromProfile != null) {
      writeProfile(buf, copyFromProfile);
    }
    return buf;
  }

  static CreateProfilePayload readCreateProfilePayload(PacketByteBuf buf) {
    long baseRevision = buf.readVarLong();
    String name = buf.readString(MAX_PROFILE_NAME_LENGTH);
    LootLockProfile copyFrom = buf.readBoolean() ? readProfile(buf) : null;
    return new CreateProfilePayload(baseRevision, name, copyFrom);
  }

  static PacketByteBuf writeDeleteProfilePayload(long baseRevision, UUID profileId) {
    PacketByteBuf buf = PacketByteBufs.create();
    buf.writeVarLong(baseRevision);
    buf.writeUuid(profileId);
    return buf;
  }

  static DeleteProfilePayload readDeleteProfilePayload(PacketByteBuf buf) {
    return new DeleteProfilePayload(buf.readVarLong(), buf.readUuid());
  }

  static MutationResult applyUpdateProfile(LootLockPlayerData data, UpdateProfilePayload payload) {
    if (!isEditable(data) || isStale(data, payload.baseRevision())) {
      return MutationResult.rejected();
    }

    if (payload.profile() == null || payload.profile().getId() == null) {
      return MutationResult.rejected();
    }

    List<LootLockProfile> profiles = new ArrayList<>(data.getProfiles());
    int profileIndex = indexOfProfile(profiles, payload.profile().getId());
    if (profileIndex < 0) {
      return MutationResult.rejected();
    }

    LootLockProfile sanitized = sanitizeProfile(payload.profile(), payload.profile().getId());
    if (sanitized == null) {
      return MutationResult.rejected();
    }

    profiles.set(profileIndex, sanitized);
    data.setProfiles(profiles);
    if (data.getActiveProfileId() == null) {
      data.setActiveProfileId(sanitized.getId());
    }
    return MutationResult.applied();
  }

  static MutationResult applyActivateProfile(
      LootLockPlayerData data, ActivateProfilePayload payload) {
    if (!isEditable(data) || isStale(data, payload.baseRevision())) {
      return MutationResult.rejected();
    }

    if (indexOfProfile(data.getProfiles(), payload.profileId()) < 0) {
      return MutationResult.rejected();
    }

    data.setActiveProfileId(payload.profileId());
    return MutationResult.applied();
  }

  static MutationResult applyCreateProfile(LootLockPlayerData data, CreateProfilePayload payload) {
    if (!isEditable(data) || isStale(data, payload.baseRevision())) {
      return MutationResult.rejected();
    }

    List<LootLockProfile> profiles = new ArrayList<>(data.getProfiles());
    if (profiles.size() >= MAX_PROFILES) {
      return MutationResult.rejected();
    }

    String sanitizedName = sanitizeName(payload.name());
    if (sanitizedName.isBlank()) {
      return MutationResult.rejected();
    }

    LootLockProfile created =
        payload.copyFromProfile() == null
            ? createDefaultProfile(sanitizedName)
            : cloneProfile(payload.copyFromProfile(), UUID.randomUUID(), sanitizedName);
    if (created == null) {
      return MutationResult.rejected();
    }

    profiles.add(created);
    data.setProfiles(profiles);
    data.setActiveProfileId(created.getId());
    return MutationResult.applied();
  }

  static MutationResult applyDeleteProfile(LootLockPlayerData data, DeleteProfilePayload payload) {
    if (!isEditable(data) || isStale(data, payload.baseRevision())) {
      return MutationResult.rejected();
    }

    List<LootLockProfile> profiles = new ArrayList<>(data.getProfiles());
    if (profiles.size() <= 1) {
      return MutationResult.rejected();
    }

    boolean removed =
        profiles.removeIf(
            profile -> profile != null && payload.profileId().equals(profile.getId()));
    if (!removed) {
      return MutationResult.rejected();
    }

    data.setProfiles(profiles);
    if (payload.profileId().equals(data.getActiveProfileId())) {
      data.setActiveProfileId(profiles.get(0).getId());
    }
    return MutationResult.applied();
  }

  private static void handleUpdateProfile(
      net.minecraft.server.network.ServerPlayerEntity player, UpdateProfilePayload payload) {
    applyAndSync(player, applyUpdateProfile(LootLock.PLAYER_DATA_MANAGER.get(player), payload));
  }

  private static void handleActivateProfile(
      net.minecraft.server.network.ServerPlayerEntity player, ActivateProfilePayload payload) {
    applyAndSync(player, applyActivateProfile(LootLock.PLAYER_DATA_MANAGER.get(player), payload));
  }

  private static void handleCreateProfile(
      net.minecraft.server.network.ServerPlayerEntity player, CreateProfilePayload payload) {
    applyAndSync(player, applyCreateProfile(LootLock.PLAYER_DATA_MANAGER.get(player), payload));
  }

  private static void handleDeleteProfile(
      net.minecraft.server.network.ServerPlayerEntity player, DeleteProfilePayload payload) {
    applyAndSync(player, applyDeleteProfile(LootLock.PLAYER_DATA_MANAGER.get(player), payload));
  }

  private static void applyAndSync(
      net.minecraft.server.network.ServerPlayerEntity player, MutationResult result) {
    if (result.success()) {
      LootLock.PLAYER_DATA_MANAGER.markDirty(player);
    }
    ServerToClientPackets.sendAuthoritativeSync(player);
  }

  private static boolean isEditable(LootLockPlayerData data) {
    return data != null && data.isClientCanEdit();
  }

  private static boolean isStale(LootLockPlayerData data, long baseRevision) {
    return data.getRevision() != baseRevision;
  }

  private static int indexOfProfile(List<LootLockProfile> profiles, UUID profileId) {
    for (int i = 0; i < profiles.size(); i++) {
      LootLockProfile profile = profiles.get(i);
      if (profile != null && profileId.equals(profile.getId())) {
        return i;
      }
    }
    return -1;
  }

  private static LootLockProfile sanitizeProfile(LootLockProfile profile, UUID profileId) {
    return cloneProfile(profile, profileId, sanitizeName(profile.getName()));
  }

  private static LootLockProfile cloneProfile(LootLockProfile source, UUID profileId, String name) {
    if (source == null || profileId == null || name.isBlank()) {
      return null;
    }

    List<RuleEntry> sanitizedRules = new ArrayList<>();
    List<RuleEntry> rules = source.getRules() == null ? List.of() : source.getRules();
    if (rules.size() > MAX_RULES_PER_PROFILE) {
      return null;
    }

    for (RuleEntry rule : rules) {
      if (rule == null || rule.itemId() == null || rule.itemId().isBlank()) {
        return null;
      }
      String itemId = rule.itemId().trim();
      if (itemId.length() > MAX_RULE_ID_LENGTH) {
        return null;
      }
      sanitizedRules.add(new RuleEntry(itemId));
    }

    return new LootLockProfile(
        profileId,
        name,
        Optional.ofNullable(source.getMode()).orElse(FilterMode.DENYLIST),
        Optional.ofNullable(source.getRejectedItemAction())
            .orElse(RejectedItemAction.LEAVE_ON_GROUND),
        source.isEnabled(),
        sanitizedRules);
  }

  private static LootLockProfile createDefaultProfile(String profileName) {
    return new LootLockProfile(
        UUID.randomUUID(),
        profileName,
        FilterMode.DENYLIST,
        RejectedItemAction.LEAVE_ON_GROUND,
        true,
        List.of());
  }

  private static String sanitizeName(String name) {
    if (name == null) {
      return "";
    }
    String sanitized = name.trim();
    if (sanitized.length() > MAX_PROFILE_NAME_LENGTH) {
      return "";
    }
    return sanitized;
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

  record HelloPayload(String clientVersion, int schemaVersion) {}

  record UpdateProfilePayload(long baseRevision, LootLockProfile profile) {}

  record ActivateProfilePayload(long baseRevision, UUID profileId) {}

  record CreateProfilePayload(long baseRevision, String name, LootLockProfile copyFromProfile) {}

  record DeleteProfilePayload(long baseRevision, UUID profileId) {}

  record MutationResult(boolean success) {
    static MutationResult applied() {
      return new MutationResult(true);
    }

    static MutationResult rejected() {
      return new MutationResult(false);
    }
  }
}
