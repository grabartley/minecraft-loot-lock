package com.grahambartley.network;

import com.grahambartley.LootLock;
import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.data.RuleEntry;
import com.grahambartley.server.ServerPolicyService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

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

    ServerPlayNetworking.registerGlobalReceiver(
        PacketIds.UPDATE_SERVER_POLICY_C2S,
        (server, player, handler, buf, responseSender) -> {
          UpdateServerPolicyPayload payload = readUpdateServerPolicyPayload(buf);
          server.execute(() -> handleUpdateServerPolicy(player, payload));
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

  public static PacketByteBuf writeUpdateProfilePayload(
      long baseRevision, LootLockProfile profile) {
    PacketByteBuf buf = PacketByteBufs.create();
    buf.writeVarLong(baseRevision);
    writeProfile(buf, profile);
    return buf;
  }

  static UpdateProfilePayload readUpdateProfilePayload(PacketByteBuf buf) {
    return new UpdateProfilePayload(buf.readVarLong(), readProfile(buf));
  }

  public static PacketByteBuf writeActivateProfilePayload(long baseRevision, UUID profileId) {
    PacketByteBuf buf = PacketByteBufs.create();
    buf.writeVarLong(baseRevision);
    buf.writeUuid(profileId);
    return buf;
  }

  static ActivateProfilePayload readActivateProfilePayload(PacketByteBuf buf) {
    return new ActivateProfilePayload(buf.readVarLong(), buf.readUuid());
  }

  public static PacketByteBuf writeCreateProfilePayload(
      long baseRevision, String name, LootLockProfile copyFromProfile) {
    PacketByteBuf buf = PacketByteBufs.create();
    buf.writeVarLong(baseRevision);
    buf.writeString(name == null ? "" : name, PacketLimits.MAX_PROFILE_NAME_LENGTH);
    buf.writeBoolean(copyFromProfile != null);
    if (copyFromProfile != null) {
      writeProfile(buf, copyFromProfile);
    }
    return buf;
  }

  static CreateProfilePayload readCreateProfilePayload(PacketByteBuf buf) {
    long baseRevision = buf.readVarLong();
    String name = buf.readString(PacketLimits.MAX_PROFILE_NAME_LENGTH);
    LootLockProfile copyFrom = buf.readBoolean() ? readProfile(buf) : null;
    return new CreateProfilePayload(baseRevision, name, copyFrom);
  }

  public static PacketByteBuf writeDeleteProfilePayload(long baseRevision, UUID profileId) {
    PacketByteBuf buf = PacketByteBufs.create();
    buf.writeVarLong(baseRevision);
    buf.writeUuid(profileId);
    return buf;
  }

  static DeleteProfilePayload readDeleteProfilePayload(PacketByteBuf buf) {
    return new DeleteProfilePayload(buf.readVarLong(), buf.readUuid());
  }

  public static PacketByteBuf writeUpdateServerPolicyPayload(boolean allowDeleteRejectedItems) {
    PacketByteBuf buf = PacketByteBufs.create();
    buf.writeBoolean(allowDeleteRejectedItems);
    return buf;
  }

  static UpdateServerPolicyPayload readUpdateServerPolicyPayload(PacketByteBuf buf) {
    return new UpdateServerPolicyPayload(buf.readBoolean());
  }

  static MutationResult applyUpdateProfile(LootLockPlayerData data, UpdateProfilePayload payload) {
    return applyUpdateProfile(data, payload, true);
  }

  static MutationResult applyUpdateProfile(
      LootLockPlayerData data, UpdateProfilePayload payload, boolean allowDeleteRejectedItems) {
    if (!isEditable(data)) {
      return MutationResult.rejected(MutationRejectionReason.NOT_EDITABLE);
    }
    if (isStale(data, payload.baseRevision())) {
      return MutationResult.rejected(MutationRejectionReason.STALE);
    }

    if (payload.profile() == null || payload.profile().getId() == null) {
      return MutationResult.rejected(MutationRejectionReason.INVALID);
    }

    List<LootLockProfile> profiles = new ArrayList<>(data.getProfiles());
    int profileIndex = indexOfProfile(profiles, payload.profile().getId());
    if (profileIndex < 0) {
      return MutationResult.rejected(MutationRejectionReason.NOT_FOUND);
    }

    LootLockProfile sanitized =
        sanitizeProfile(payload.profile(), payload.profile().getId(), allowDeleteRejectedItems);
    if (sanitized == null) {
      return MutationResult.rejected(MutationRejectionReason.INVALID);
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
    if (!isEditable(data)) {
      return MutationResult.rejected(MutationRejectionReason.NOT_EDITABLE);
    }
    if (isStale(data, payload.baseRevision())) {
      return MutationResult.rejected(MutationRejectionReason.STALE);
    }

    if (indexOfProfile(data.getProfiles(), payload.profileId()) < 0) {
      return MutationResult.rejected(MutationRejectionReason.NOT_FOUND);
    }

    data.setActiveProfileId(payload.profileId());
    return MutationResult.applied();
  }

  static MutationResult applyCreateProfile(LootLockPlayerData data, CreateProfilePayload payload) {
    if (!isEditable(data)) {
      return MutationResult.rejected(MutationRejectionReason.NOT_EDITABLE);
    }
    if (isStale(data, payload.baseRevision())) {
      return MutationResult.rejected(MutationRejectionReason.STALE);
    }

    List<LootLockProfile> profiles = new ArrayList<>(data.getProfiles());
    if (profiles.size() >= PacketLimits.MAX_PROFILES) {
      return MutationResult.rejected(MutationRejectionReason.TOO_MANY);
    }

    String sanitizedName = sanitizeName(payload.name());
    if (sanitizedName.isBlank()) {
      return MutationResult.rejected(MutationRejectionReason.INVALID);
    }

    if (hasDuplicateProfileName(profiles, sanitizedName)) {
      return MutationResult.rejected(MutationRejectionReason.DUPLICATE_NAME);
    }

    LootLockProfile created =
        payload.copyFromProfile() == null
            ? createDefaultProfile(sanitizedName)
            : cloneProfile(payload.copyFromProfile(), UUID.randomUUID(), sanitizedName, true);
    if (created == null) {
      return MutationResult.rejected(MutationRejectionReason.INVALID);
    }

    profiles.add(created);
    data.setProfiles(profiles);
    return MutationResult.applied();
  }

  static MutationResult applyDeleteProfile(LootLockPlayerData data, DeleteProfilePayload payload) {
    if (!isEditable(data)) {
      return MutationResult.rejected(MutationRejectionReason.NOT_EDITABLE);
    }
    if (isStale(data, payload.baseRevision())) {
      return MutationResult.rejected(MutationRejectionReason.STALE);
    }

    List<LootLockProfile> profiles = new ArrayList<>(data.getProfiles());
    if (profiles.size() <= 1) {
      return MutationResult.rejected(MutationRejectionReason.INVALID);
    }

    boolean removed =
        profiles.removeIf(
            profile -> profile != null && payload.profileId().equals(profile.getId()));
    if (!removed) {
      return MutationResult.rejected(MutationRejectionReason.NOT_FOUND);
    }

    data.setProfiles(profiles);
    if (payload.profileId().equals(data.getActiveProfileId())) {
      data.setActiveProfileId(profiles.get(0).getId());
    }
    return MutationResult.applied();
  }

  private static void handleUpdateProfile(ServerPlayerEntity player, UpdateProfilePayload payload) {
    // Defense-in-depth: clientCanEdit already reflects operator status in sync payload,
    // but we guard explicitly at the packet boundary.
    if (!isOperator(player)) {
      ServerToClientPackets.sendAuthoritativeSync(player);
      return;
    }
    if (LootLock.PLAYER_DATA_MANAGER == null) {
      return;
    }
    applyAndSync(
        player,
        applyUpdateProfile(
            LootLock.PLAYER_DATA_MANAGER.get(player),
            payload,
            LootLock.SERVER_CONFIG.allowDeleteRejectedItems()));
  }

  private static void handleActivateProfile(
      ServerPlayerEntity player, ActivateProfilePayload payload) {
    if (!isOperator(player)) {
      ServerToClientPackets.sendAuthoritativeSync(player);
      return;
    }
    if (LootLock.PLAYER_DATA_MANAGER == null) {
      return;
    }
    applyAndSync(player, applyActivateProfile(LootLock.PLAYER_DATA_MANAGER.get(player), payload));
  }

  private static void handleCreateProfile(ServerPlayerEntity player, CreateProfilePayload payload) {
    if (!isOperator(player)) {
      ServerToClientPackets.sendAuthoritativeSync(player);
      return;
    }
    if (LootLock.PLAYER_DATA_MANAGER == null) {
      return;
    }
    applyAndSync(player, applyCreateProfile(LootLock.PLAYER_DATA_MANAGER.get(player), payload));
  }

  private static void handleDeleteProfile(ServerPlayerEntity player, DeleteProfilePayload payload) {
    if (!isOperator(player)) {
      ServerToClientPackets.sendAuthoritativeSync(player);
      return;
    }
    if (LootLock.PLAYER_DATA_MANAGER == null) {
      return;
    }
    applyAndSync(player, applyDeleteProfile(LootLock.PLAYER_DATA_MANAGER.get(player), payload));
  }

  private static void handleUpdateServerPolicy(
      ServerPlayerEntity player, UpdateServerPolicyPayload payload) {
    if (!isOperator(player)) {
      ServerToClientPackets.sendAuthoritativeSync(player);
      return;
    }
    ServerPolicyService.updateAllowDeleteRejectedItems(
        player.getServer(), payload.allowDeleteRejectedItems());
    for (ServerPlayerEntity connectedPlayer :
        player.getServer().getPlayerManager().getPlayerList()) {
      ServerToClientPackets.sendAuthoritativeSync(connectedPlayer);
    }
  }

  private static void applyAndSync(ServerPlayerEntity player, MutationResult result) {
    if (LootLock.PLAYER_DATA_MANAGER == null) {
      return;
    }
    if (!result.success()) {
      LootLock.LOGGER.debug(
          "Rejected C2S profile mutation for {}: {}", player.getUuid(), result.reason());
    }
    if (result.success()) {
      // The manager returns the cached mutable player data entry, so these in-place mutations are
      // persisted when markDirty is called.
      LootLock.PLAYER_DATA_MANAGER.markDirty(player);
    }
    ServerToClientPackets.sendAuthoritativeSync(player);
  }

  private static boolean isEditable(LootLockPlayerData data) {
    return data != null && data.isClientCanEdit();
  }

  private static boolean isOperator(ServerPlayerEntity player) {
    return player != null && player.hasPermissionLevel(2);
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

  private static LootLockProfile sanitizeProfile(
      LootLockProfile profile, UUID profileId, boolean allowDeleteRejectedItems) {
    // Profile ID is taken from the existing record, not payload data, to prevent ID-spoofing.
    return cloneProfile(
        profile, profileId, sanitizeName(profile.getName()), allowDeleteRejectedItems);
  }

  private static boolean hasDuplicateProfileName(
      List<LootLockProfile> profiles, String profileName) {
    for (LootLockProfile profile : profiles) {
      if (profile != null && profileName.equalsIgnoreCase(profile.getName())) {
        return true;
      }
    }
    return false;
  }

  private static LootLockProfile cloneProfile(
      LootLockProfile source, UUID profileId, String name, boolean allowDeleteRejectedItems) {
    if (source == null || profileId == null || name.isBlank()) {
      return null;
    }

    List<RuleEntry> sanitizedRules = new ArrayList<>();
    List<RuleEntry> rules = source.getRules() == null ? List.of() : source.getRules();
    if (rules.size() > PacketLimits.MAX_RULES_PER_PROFILE) {
      return null;
    }

    for (RuleEntry rule : rules) {
      if (rule == null || rule.itemId() == null || rule.itemId().isBlank()) {
        return null;
      }
      String itemId = rule.itemId().trim();
      if (itemId.length() > PacketLimits.MAX_RULE_ID_LENGTH) {
        return null;
      }
      sanitizedRules.add(new RuleEntry(itemId));
    }

    RejectedItemAction action =
        Optional.ofNullable(source.getRejectedItemAction())
            .orElse(RejectedItemAction.LEAVE_ON_GROUND);
    if (!allowDeleteRejectedItems && action == RejectedItemAction.DELETE) {
      action = RejectedItemAction.LEAVE_ON_GROUND;
    }

    return new LootLockProfile(
        profileId,
        name,
        Optional.ofNullable(source.getMode()).orElse(FilterMode.DENYLIST),
        action,
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
    if (sanitized.length() > PacketLimits.MAX_PROFILE_NAME_LENGTH) {
      return "";
    }
    return sanitized;
  }

  private static void writeProfile(PacketByteBuf buf, LootLockProfile profile) {
    buf.writeUuid(profile.getId());
    buf.writeString(profile.getName(), PacketLimits.MAX_PROFILE_NAME_LENGTH);
    buf.writeEnumConstant(profile.getMode());
    buf.writeEnumConstant(profile.getRejectedItemAction());
    buf.writeBoolean(profile.isEnabled());
    buf.writeVarInt(profile.getRules().size());
    for (RuleEntry rule : profile.getRules()) {
      buf.writeString(rule.itemId(), PacketLimits.MAX_RULE_ID_LENGTH);
    }
  }

  private static LootLockProfile readProfile(PacketByteBuf buf) {
    UUID profileId = buf.readUuid();
    String profileName = buf.readString(PacketLimits.MAX_PROFILE_NAME_LENGTH);
    FilterMode mode = buf.readEnumConstant(FilterMode.class);
    RejectedItemAction action = buf.readEnumConstant(RejectedItemAction.class);
    boolean enabled = buf.readBoolean();
    int ruleCount = buf.readVarInt();
    List<RuleEntry> rules = new ArrayList<>(ruleCount);
    for (int i = 0; i < ruleCount; i++) {
      rules.add(new RuleEntry(buf.readString(PacketLimits.MAX_RULE_ID_LENGTH)));
    }
    return new LootLockProfile(profileId, profileName, mode, action, enabled, rules);
  }

  record HelloPayload(String clientVersion, int schemaVersion) {}

  record UpdateProfilePayload(long baseRevision, LootLockProfile profile) {}

  record ActivateProfilePayload(long baseRevision, UUID profileId) {}

  record CreateProfilePayload(long baseRevision, String name, LootLockProfile copyFromProfile) {}

  record DeleteProfilePayload(long baseRevision, UUID profileId) {}

  record UpdateServerPolicyPayload(boolean allowDeleteRejectedItems) {}

  record MutationResult(boolean success, MutationRejectionReason reason) {
    static MutationResult applied() {
      return new MutationResult(true, MutationRejectionReason.NONE);
    }

    static MutationResult rejected(MutationRejectionReason reason) {
      return new MutationResult(false, reason);
    }
  }

  enum MutationRejectionReason {
    NONE,
    STALE,
    NOT_FOUND,
    INVALID,
    TOO_MANY,
    NOT_EDITABLE,
    DUPLICATE_NAME
  }
}
