package com.grahambartley.network;

import com.grahambartley.client.state.ClientLootLockState.ClientDraftSaveRequest;
import com.grahambartley.data.LootLockProfile;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientDraftSync {
  private ClientDraftSync() {}

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
}
