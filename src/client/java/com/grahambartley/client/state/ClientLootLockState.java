package com.grahambartley.client.state;

import com.grahambartley.LootLock;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.network.ServerToClientPackets;
import java.util.Optional;
import java.util.UUID;

public final class ClientLootLockState {
  private boolean serverSupportsLootLock;
  private boolean synced;
  private boolean allowDeleteRejectedItems;
  private LootLockPlayerData snapshot;
  private ClientDraftProfile draftProfile;

  public void onLogin() {
    serverSupportsLootLock = false;
    synced = false;
    snapshot = null;
    allowDeleteRejectedItems = true;
    draftProfile = null;
  }

  public void onServerCapabilities(boolean supportsLootLock) {
    serverSupportsLootLock = supportsLootLock;
    if (!supportsLootLock) {
      synced = false;
      snapshot = null;
    }
  }

  public void onAuthoritativeSync(ServerToClientPackets.SyncPayload payload) {
    if (payload == null) {
      return;
    }

    // Sync is authoritative proof that the server supports LootLock, even if
    // capabilities was delayed or not observed before this packet.
    serverSupportsLootLock = true;
    synced = true;
    snapshot = toSnapshot(payload);
    allowDeleteRejectedItems = payload.allowDeleteRejectedItems();
    refreshDraftFromSnapshot();
  }

  public void clear() {
    serverSupportsLootLock = false;
    synced = false;
    snapshot = null;
    allowDeleteRejectedItems = true;
    draftProfile = null;
  }

  public boolean isServerSupportsLootLock() {
    return serverSupportsLootLock;
  }

  public boolean isSynced() {
    return synced;
  }

  public boolean isAllowDeleteRejectedItems() {
    return allowDeleteRejectedItems;
  }

  public Optional<LootLockPlayerData> getSnapshot() {
    // Snapshot stays mutable by design for upcoming draft-edit workflows.
    // Read-only UI paths should treat this as immutable and copy before mutation.
    return Optional.ofNullable(snapshot);
  }

  public Optional<ClientDraftProfile> beginDraft(UUID profileId) {
    if (snapshot == null || profileId == null) {
      return Optional.empty();
    }

    Optional<LootLockProfile> sourceProfile =
        snapshot.getProfiles().stream()
            .filter(profile -> profileId.equals(profile.getId()))
            .findFirst();
    if (sourceProfile.isEmpty()) {
      return Optional.empty();
    }

    draftProfile = new ClientDraftProfile(profileId, snapshot.getRevision(), sourceProfile.get());
    return Optional.of(draftProfile);
  }

  public Optional<ClientDraftProfile> getDraftProfile() {
    return Optional.ofNullable(draftProfile);
  }

  public void discardDraft() {
    draftProfile = null;
  }

  public void markDraftDirty() {
    if (draftProfile != null) {
      draftProfile.markDirty();
    }
  }

  public Optional<ClientDraftSaveRequest> buildSaveRequest() {
    if (draftProfile == null || !draftProfile.isDirty()) {
      return Optional.empty();
    }

    return Optional.of(
        new ClientDraftSaveRequest(draftProfile.getBaseRevision(), draftProfile.getDraft()));
  }

  private static LootLockPlayerData toSnapshot(ServerToClientPackets.SyncPayload payload) {
    LootLockPlayerData data = LootLockPlayerData.createDefault(payload.playerUuid());
    data.setSchemaVersion(payload.schemaVersion());
    data.setRevision(payload.revision());
    data.setActiveProfileId(payload.activeProfileId());
    data.setProfiles(payload.profiles());
    data.setClientCanEdit(payload.clientCanEdit());
    return data;
  }

  private void refreshDraftFromSnapshot() {
    if (draftProfile == null || snapshot == null) {
      return;
    }

    Optional<LootLockProfile> refreshed =
        snapshot.getProfiles().stream()
            .filter(profile -> draftProfile.getProfileId().equals(profile.getId()))
            .findFirst();

    if (refreshed.isEmpty()) {
      if (draftProfile.isDirty()) {
        LootLock.LOGGER.debug(
            "Discarding dirty client draft for profile {} due to server sync",
            draftProfile.getProfileId());
      }
      draftProfile = null;
      return;
    }

    if (draftProfile.isDirty()) {
      LootLock.LOGGER.debug(
          "Discarding dirty client draft for profile {} due to server sync",
          draftProfile.getProfileId());
    }

    draftProfile =
        new ClientDraftProfile(
            draftProfile.getProfileId(), snapshot.getRevision(), refreshed.get());
  }

  public record ClientDraftSaveRequest(long baseRevision, LootLockProfile profile) {}
}
