package com.grahambartley.client.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.data.LootLockProfile;
import com.grahambartley.network.ServerToClientPackets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClientLootLockStateTest {
  @Test
  void onLoginResetsSupportSyncAndSnapshot() {
    ClientLootLockState state = new ClientLootLockState();
    state.onAuthoritativeSync(createPayload());

    state.onLogin();

    assertFalse(state.isServerSupportsLootLock());
    assertFalse(state.isSynced());
    assertTrue(state.getSnapshot().isEmpty());
  }

  @Test
  void onAuthoritativeSyncStoresSnapshotAndMarksSynced() {
    ClientLootLockState state = new ClientLootLockState();
    ServerToClientPackets.SyncPayload payload = createPayload();

    state.onAuthoritativeSync(payload);

    assertTrue(state.isServerSupportsLootLock());
    assertTrue(state.isSynced());
    assertTrue(state.getSnapshot().isPresent());
    assertEquals(payload.revision(), state.getSnapshot().orElseThrow().getRevision());
    assertEquals(payload.activeProfileId(), state.getSnapshot().orElseThrow().getActiveProfileId());
    assertEquals(payload.profiles().size(), state.getSnapshot().orElseThrow().getProfiles().size());
  }

  @Test
  void clearResetsAllStateOnDisconnect() {
    ClientLootLockState state = new ClientLootLockState();
    state.onAuthoritativeSync(createPayload());

    state.clear();

    assertFalse(state.isServerSupportsLootLock());
    assertFalse(state.isSynced());
    assertTrue(state.getSnapshot().isEmpty());
  }

  @Test
  void onServerCapabilitiesFalseClearsSnapshotAndSync() {
    ClientLootLockState state = new ClientLootLockState();
    state.onAuthoritativeSync(createPayload());

    state.onServerCapabilities(false);

    assertFalse(state.isServerSupportsLootLock());
    assertFalse(state.isSynced());
    assertTrue(state.getSnapshot().isEmpty());
  }

  @Test
  void onServerCapabilitiesTrueMarksSupportedWithoutSyncing() {
    ClientLootLockState state = new ClientLootLockState();

    state.onServerCapabilities(true);

    assertTrue(state.isServerSupportsLootLock());
    assertFalse(state.isSynced());
    assertTrue(state.getSnapshot().isEmpty());
  }

  @Test
  void beginDraftBuildsMutableCopyAndDoesNotMutateSnapshot() {
    ClientLootLockState state = new ClientLootLockState();
    ServerToClientPackets.SyncPayload payload = createPayload();
    state.onAuthoritativeSync(payload);

    ClientDraftProfile draft = state.beginDraft(payload.activeProfileId()).orElseThrow();
    draft.setName("Renamed");

    assertTrue(draft.isDirty());
    assertNotSame(payload.profiles().get(0), draft.getDraft());
    assertEquals("Default", state.getSnapshot().orElseThrow().getProfiles().get(0).getName());
  }

  @Test
  void buildSaveRequestOnlyReturnsWhenDraftIsDirty() {
    ClientLootLockState state = new ClientLootLockState();
    ServerToClientPackets.SyncPayload payload = createPayload();
    state.onAuthoritativeSync(payload);

    state.beginDraft(payload.activeProfileId());
    assertTrue(state.buildSaveRequest().isEmpty());

    state.getDraftProfile().orElseThrow().setEnabled(false);
    ClientLootLockState.ClientDraftSaveRequest saveRequest = state.buildSaveRequest().orElseThrow();

    assertEquals(payload.revision(), saveRequest.baseRevision());
    assertFalse(saveRequest.profile().isEnabled());
  }

  @Test
  void authoritativeSyncRefreshesDraftToServerState() {
    ClientLootLockState state = new ClientLootLockState();
    ServerToClientPackets.SyncPayload initialPayload = createPayload();
    state.onAuthoritativeSync(initialPayload);
    state.beginDraft(initialPayload.activeProfileId()).orElseThrow().setName("Client Edited");

    LootLockProfile serverProfile = LootLockProfile.createDefault();
    serverProfile.setId(initialPayload.activeProfileId());
    serverProfile.setName("Server Truth");

    state.onAuthoritativeSync(
        new ServerToClientPackets.SyncPayload(
            1,
            initialPayload.playerUuid(),
            initialPayload.revision() + 1,
            initialPayload.activeProfileId(),
            List.of(serverProfile),
            true));

    ClientDraftProfile refreshed = state.getDraftProfile().orElseThrow();
    assertFalse(refreshed.isDirty());
    assertEquals(initialPayload.revision() + 1, refreshed.getBaseRevision());
    assertEquals("Server Truth", refreshed.getDraft().getName());
  }

  private static ServerToClientPackets.SyncPayload createPayload() {
    LootLockProfile profile = LootLockProfile.createDefault();
    UUID playerUuid = UUID.randomUUID();
    return new ServerToClientPackets.SyncPayload(
        1, playerUuid, 7L, profile.getId(), List.of(profile), true);
  }
}
