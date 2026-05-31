package com.grahambartley.client.state;

import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.network.ServerToClientPackets;
import java.util.Optional;

public final class ClientLootLockState {
  private boolean serverSupportsLootLock;
  private boolean synced;
  private LootLockPlayerData snapshot;

  public void onLogin() {
    serverSupportsLootLock = false;
    synced = false;
    snapshot = null;
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
  }

  public void clear() {
    serverSupportsLootLock = false;
    synced = false;
    snapshot = null;
  }

  public boolean isServerSupportsLootLock() {
    return serverSupportsLootLock;
  }

  public boolean isSynced() {
    return synced;
  }

  public Optional<LootLockPlayerData> getSnapshot() {
    // Snapshot stays mutable by design for upcoming draft-edit workflows.
    // Read-only UI paths should treat this as immutable and copy before mutation.
    return Optional.ofNullable(snapshot);
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
}
