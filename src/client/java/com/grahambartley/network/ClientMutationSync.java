package com.grahambartley.network;

import com.grahambartley.client.state.ClientLootLockState.ClientDraftSaveRequest;
import com.grahambartley.data.LootLockProfile;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

// sendSaveRequest uses the ClientDraftProfile lifecycle for multi-field
// profile UPDATEs that capture baseRevision at edit-start time.
// sendCreate/sendDelete/sendActivate are one-shot mutations with no
// editing lifecycle, they capture the current revision at send time.
public final class ClientMutationSync {
  private ClientMutationSync() {}

  public static boolean sendSaveRequest(ClientDraftSaveRequest saveRequest) {
    if (saveRequest == null || !ClientPlayNetworking.canSend(PacketIds.UPDATE_PROFILE_C2S)) {
      return false;
    }

    ClientPlayNetworking.send(
        PacketIds.UPDATE_PROFILE_C2S,
        ClientToServerPackets.writeUpdateProfilePayload(
            saveRequest.baseRevision(), saveRequest.profile()));
    return true;
  }

  public static boolean sendActivateRequest(long baseRevision, UUID profileId) {
    if (profileId == null || !ClientPlayNetworking.canSend(PacketIds.ACTIVATE_PROFILE_C2S)) {
      return false;
    }

    ClientPlayNetworking.send(
        PacketIds.ACTIVATE_PROFILE_C2S,
        ClientToServerPackets.writeActivateProfilePayload(baseRevision, profileId));
    return true;
  }

  public static boolean sendCreateRequest(
      long baseRevision, String name, LootLockProfile copyFromProfile) {
    if (name == null || !ClientPlayNetworking.canSend(PacketIds.CREATE_PROFILE_C2S)) {
      return false;
    }

    ClientPlayNetworking.send(
        PacketIds.CREATE_PROFILE_C2S,
        ClientToServerPackets.writeCreateProfilePayload(baseRevision, name, copyFromProfile));
    return true;
  }

  public static boolean sendDeleteRequest(long baseRevision, UUID profileId) {
    if (profileId == null || !ClientPlayNetworking.canSend(PacketIds.DELETE_PROFILE_C2S)) {
      return false;
    }

    ClientPlayNetworking.send(
        PacketIds.DELETE_PROFILE_C2S,
        ClientToServerPackets.writeDeleteProfilePayload(baseRevision, profileId));
    return true;
  }

  public static boolean sendServerPolicyUpdateRequest(boolean allowDeleteRejectedItems) {
    if (!ClientPlayNetworking.canSend(PacketIds.UPDATE_SERVER_POLICY_C2S)) {
      return false;
    }
    ClientPlayNetworking.send(
        PacketIds.UPDATE_SERVER_POLICY_C2S,
        ClientToServerPackets.writeUpdateServerPolicyPayload(allowDeleteRejectedItems));
    return true;
  }

  public static boolean sendUpdateGlobalEnableRequest(long baseRevision, boolean enabled) {
    if (!ClientPlayNetworking.canSend(PacketIds.UPDATE_GLOBAL_ENABLE_C2S)) {
      return false;
    }
    ClientPlayNetworking.send(
        PacketIds.UPDATE_GLOBAL_ENABLE_C2S,
        ClientToServerPackets.writeUpdateGlobalEnablePayload(baseRevision, enabled));
    return true;
  }
}
