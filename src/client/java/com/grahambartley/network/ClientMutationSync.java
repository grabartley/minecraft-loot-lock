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
    if (saveRequest == null
        || !ClientPlayNetworking.canSend(ClientToServerPackets.UpdateProfilePayload.ID)) {
      return false;
    }

    ClientPlayNetworking.send(
        new ClientToServerPackets.UpdateProfilePayload(
            saveRequest.baseRevision(), saveRequest.profile()));
    return true;
  }

  public static boolean sendActivateRequest(long baseRevision, UUID profileId) {
    if (profileId == null
        || !ClientPlayNetworking.canSend(ClientToServerPackets.ActivateProfilePayload.ID)) {
      return false;
    }

    ClientPlayNetworking.send(
        new ClientToServerPackets.ActivateProfilePayload(baseRevision, profileId));
    return true;
  }

  public static boolean sendCreateRequest(
      long baseRevision, String name, LootLockProfile copyFromProfile) {
    if (name == null
        || !ClientPlayNetworking.canSend(ClientToServerPackets.CreateProfilePayload.ID)) {
      return false;
    }

    ClientPlayNetworking.send(
        new ClientToServerPackets.CreateProfilePayload(baseRevision, name, copyFromProfile));
    return true;
  }

  public static boolean sendDeleteRequest(long baseRevision, UUID profileId) {
    if (profileId == null
        || !ClientPlayNetworking.canSend(ClientToServerPackets.DeleteProfilePayload.ID)) {
      return false;
    }

    ClientPlayNetworking.send(
        new ClientToServerPackets.DeleteProfilePayload(baseRevision, profileId));
    return true;
  }

  public static boolean sendServerPolicyUpdateRequest(boolean allowDeleteRejectedItems) {
    if (!ClientPlayNetworking.canSend(ClientToServerPackets.UpdateServerPolicyPayload.ID)) {
      return false;
    }
    ClientPlayNetworking.send(
        new ClientToServerPackets.UpdateServerPolicyPayload(allowDeleteRejectedItems));
    return true;
  }

  public static boolean sendUpdateGlobalEnableRequest(long baseRevision, boolean enabled) {
    if (!ClientPlayNetworking.canSend(ClientToServerPackets.UpdateGlobalEnablePayload.ID)) {
      return false;
    }
    ClientPlayNetworking.send(
        new ClientToServerPackets.UpdateGlobalEnablePayload(baseRevision, enabled));
    return true;
  }
}
