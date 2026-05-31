package com.grahambartley.network;

import com.grahambartley.client.state.ClientLootLockState.ClientDraftSaveRequest;
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
}
