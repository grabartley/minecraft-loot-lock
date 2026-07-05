package com.grahambartley.lootlock.network;

import com.grahambartley.lootlock.LootLock;
import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockPlayerData;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.data.RuleEntry;
import com.grahambartley.lootlock.server.ServerPolicyService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class ClientToServerPackets {
  private static final int MAX_CLIENT_VERSION_LENGTH = 64;

  private ClientToServerPackets() {}

  // Play payload receivers already run on the server thread, no execute() hop is needed.
  public static void register() {
    ServerPlayNetworking.registerGlobalReceiver(
        HelloPayload.ID,
        (payload, context) -> {
          ServerPlayerEntity player = context.player();
          LootLock.LOGGER.debug(
              "Received hello from {}: version={}, schema={}.",
              player.getUuid(),
              payload.clientVersion(),
              payload.schemaVersion());
          ServerToClientPackets.sendAuthoritativeSync(player);
        });

    ServerPlayNetworking.registerGlobalReceiver(
        RequestSyncPayload.ID,
        (payload, context) -> ServerToClientPackets.sendAuthoritativeSync(context.player()));

    ServerPlayNetworking.registerGlobalReceiver(
        UpdateProfilePayload.ID,
        (payload, context) -> handleUpdateProfile(context.player(), payload));

    ServerPlayNetworking.registerGlobalReceiver(
        ActivateProfilePayload.ID,
        (payload, context) -> handleActivateProfile(context.player(), payload));

    ServerPlayNetworking.registerGlobalReceiver(
        CreateProfilePayload.ID,
        (payload, context) -> handleCreateProfile(context.player(), payload));

    ServerPlayNetworking.registerGlobalReceiver(
        DeleteProfilePayload.ID,
        (payload, context) -> handleDeleteProfile(context.player(), payload));

    ServerPlayNetworking.registerGlobalReceiver(
        UpdateServerPolicyPayload.ID,
        (payload, context) -> handleUpdateServerPolicy(context.player(), payload));

    ServerPlayNetworking.registerGlobalReceiver(
        UpdateGlobalEnablePayload.ID,
        (payload, context) -> handleUpdateGlobalEnable(context.player(), payload));
  }

  static MutationResult applyUpdateGlobalEnable(
      LootLockPlayerData data, UpdateGlobalEnablePayload payload) {
    if (isStale(data, payload.baseRevision())) {
      return MutationResult.rejected(MutationRejectionReason.STALE);
    }

    data.setEnabledForAll(payload.enabled());
    return MutationResult.applied();
  }

  static MutationResult applyUpdateProfile(LootLockPlayerData data, UpdateProfilePayload payload) {
    return applyUpdateProfile(data, payload, true);
  }

  static MutationResult applyUpdateProfile(
      LootLockPlayerData data, UpdateProfilePayload payload, boolean allowDeleteRejectedItems) {
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
    if (LootLock.PLAYER_DATA_MANAGER == null) {
      return;
    }
    applyAndSync(player, applyActivateProfile(LootLock.PLAYER_DATA_MANAGER.get(player), payload));
  }

  private static void handleCreateProfile(ServerPlayerEntity player, CreateProfilePayload payload) {
    if (LootLock.PLAYER_DATA_MANAGER == null) {
      return;
    }
    applyAndSync(player, applyCreateProfile(LootLock.PLAYER_DATA_MANAGER.get(player), payload));
  }

  private static void handleDeleteProfile(ServerPlayerEntity player, DeleteProfilePayload payload) {
    if (LootLock.PLAYER_DATA_MANAGER == null) {
      return;
    }
    applyAndSync(player, applyDeleteProfile(LootLock.PLAYER_DATA_MANAGER.get(player), payload));
  }

  private static void handleUpdateGlobalEnable(
      ServerPlayerEntity player, UpdateGlobalEnablePayload payload) {
    if (LootLock.PLAYER_DATA_MANAGER == null) {
      return;
    }
    applyAndSync(
        player, applyUpdateGlobalEnable(LootLock.PLAYER_DATA_MANAGER.get(player), payload));
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

  static boolean isOperator(ServerPlayerEntity player) {
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
      if (!isValidRuleEntryToken(itemId)) {
        continue;
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
        source.getColor(),
        sanitizedRules);
  }

  private static LootLockProfile createDefaultProfile(String profileName) {
    return new LootLockProfile(
        UUID.randomUUID(),
        profileName,
        FilterMode.DENYLIST,
        RejectedItemAction.LEAVE_ON_GROUND,
        true,
        0,
        List.of());
  }

  static boolean isValidRuleEntryToken(String token) {
    if (token == null || token.isBlank()) {
      return false;
    }
    String parseTarget =
        token.startsWith(RuleEntry.TAG_PREFIX)
            ? token.substring(RuleEntry.TAG_PREFIX.length())
            : token;
    if (parseTarget.isBlank()) {
      return false;
    }
    Identifier parsed = Identifier.tryParse(parseTarget);
    return parsed != null && !parsed.getPath().isEmpty();
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

  public record HelloPayload(String clientVersion, int schemaVersion) implements CustomPayload {
    public static final CustomPayload.Id<HelloPayload> ID =
        new CustomPayload.Id<>(PacketIds.HELLO_C2S);
    public static final PacketCodec<PacketByteBuf, HelloPayload> CODEC =
        PacketCodec.of(
            (payload, buf) -> {
              buf.writeString(
                  payload.clientVersion() == null ? "unknown" : payload.clientVersion(),
                  MAX_CLIENT_VERSION_LENGTH);
              buf.writeVarInt(payload.schemaVersion());
            },
            buf -> new HelloPayload(buf.readString(MAX_CLIENT_VERSION_LENGTH), buf.readVarInt()));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record RequestSyncPayload() implements CustomPayload {
    public static final RequestSyncPayload INSTANCE = new RequestSyncPayload();
    public static final CustomPayload.Id<RequestSyncPayload> ID =
        new CustomPayload.Id<>(PacketIds.REQUEST_SYNC_C2S);
    public static final PacketCodec<PacketByteBuf, RequestSyncPayload> CODEC =
        PacketCodec.unit(INSTANCE);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record UpdateProfilePayload(long baseRevision, LootLockProfile profile)
      implements CustomPayload {
    public static final CustomPayload.Id<UpdateProfilePayload> ID =
        new CustomPayload.Id<>(PacketIds.UPDATE_PROFILE_C2S);
    public static final PacketCodec<PacketByteBuf, UpdateProfilePayload> CODEC =
        PacketCodec.of(
            (payload, buf) -> {
              buf.writeVarLong(payload.baseRevision());
              LootLockPayloads.PROFILE_CODEC.encode(buf, payload.profile());
            },
            buf ->
                new UpdateProfilePayload(
                    buf.readVarLong(), LootLockPayloads.PROFILE_CODEC.decode(buf)));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record ActivateProfilePayload(long baseRevision, UUID profileId) implements CustomPayload {
    public static final CustomPayload.Id<ActivateProfilePayload> ID =
        new CustomPayload.Id<>(PacketIds.ACTIVATE_PROFILE_C2S);
    public static final PacketCodec<PacketByteBuf, ActivateProfilePayload> CODEC =
        PacketCodec.of(
            (payload, buf) -> {
              buf.writeVarLong(payload.baseRevision());
              buf.writeUuid(payload.profileId());
            },
            buf -> new ActivateProfilePayload(buf.readVarLong(), buf.readUuid()));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record CreateProfilePayload(
      long baseRevision, String name, LootLockProfile copyFromProfile) implements CustomPayload {
    public static final CustomPayload.Id<CreateProfilePayload> ID =
        new CustomPayload.Id<>(PacketIds.CREATE_PROFILE_C2S);
    public static final PacketCodec<PacketByteBuf, CreateProfilePayload> CODEC =
        PacketCodec.of(
            (payload, buf) -> {
              buf.writeVarLong(payload.baseRevision());
              buf.writeString(
                  payload.name() == null ? "" : payload.name(),
                  PacketLimits.MAX_PROFILE_NAME_LENGTH);
              buf.writeBoolean(payload.copyFromProfile() != null);
              if (payload.copyFromProfile() != null) {
                LootLockPayloads.PROFILE_CODEC.encode(buf, payload.copyFromProfile());
              }
            },
            buf -> {
              long baseRevision = buf.readVarLong();
              String name = buf.readString(PacketLimits.MAX_PROFILE_NAME_LENGTH);
              LootLockProfile copyFrom =
                  buf.readBoolean() ? LootLockPayloads.PROFILE_CODEC.decode(buf) : null;
              return new CreateProfilePayload(baseRevision, name, copyFrom);
            });

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record DeleteProfilePayload(long baseRevision, UUID profileId) implements CustomPayload {
    public static final CustomPayload.Id<DeleteProfilePayload> ID =
        new CustomPayload.Id<>(PacketIds.DELETE_PROFILE_C2S);
    public static final PacketCodec<PacketByteBuf, DeleteProfilePayload> CODEC =
        PacketCodec.of(
            (payload, buf) -> {
              buf.writeVarLong(payload.baseRevision());
              buf.writeUuid(payload.profileId());
            },
            buf -> new DeleteProfilePayload(buf.readVarLong(), buf.readUuid()));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record UpdateServerPolicyPayload(boolean allowDeleteRejectedItems)
      implements CustomPayload {
    public static final CustomPayload.Id<UpdateServerPolicyPayload> ID =
        new CustomPayload.Id<>(PacketIds.UPDATE_SERVER_POLICY_C2S);
    public static final PacketCodec<PacketByteBuf, UpdateServerPolicyPayload> CODEC =
        PacketCodec.of(
            (payload, buf) -> buf.writeBoolean(payload.allowDeleteRejectedItems()),
            buf -> new UpdateServerPolicyPayload(buf.readBoolean()));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record UpdateGlobalEnablePayload(long baseRevision, boolean enabled)
      implements CustomPayload {
    public static final CustomPayload.Id<UpdateGlobalEnablePayload> ID =
        new CustomPayload.Id<>(PacketIds.UPDATE_GLOBAL_ENABLE_C2S);
    public static final PacketCodec<PacketByteBuf, UpdateGlobalEnablePayload> CODEC =
        PacketCodec.of(
            (payload, buf) -> {
              buf.writeVarLong(payload.baseRevision());
              buf.writeBoolean(payload.enabled());
            },
            buf -> new UpdateGlobalEnablePayload(buf.readVarLong(), buf.readBoolean()));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

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
    DUPLICATE_NAME
  }
}
