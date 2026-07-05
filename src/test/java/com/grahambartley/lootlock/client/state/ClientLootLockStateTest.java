package com.grahambartley.lootlock.client.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.network.ServerToClientPackets;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ClientLootLockStateTest {

  static Stream<Arguments> resetActions() {
    Consumer<ClientLootLockState> onLogin = ClientLootLockState::onLogin;
    Consumer<ClientLootLockState> clear = ClientLootLockState::clear;
    Consumer<ClientLootLockState> onServerCapabilitiesFalse =
        state -> state.onServerCapabilities(false);
    return Stream.of(
        Arguments.of("onLogin", onLogin),
        Arguments.of("clear", clear),
        Arguments.of("onServerCapabilities(false)", onServerCapabilitiesFalse));
  }

  @ParameterizedTest(name = "{0} clears support, sync, and snapshot")
  @MethodSource("resetActions")
  void actionsThatResetState(String label, Consumer<ClientLootLockState> action) {
    ClientLootLockState state = new ClientLootLockState();
    state.onAuthoritativeSync(createPayload());

    action.accept(state);

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
  void mutationMarksDraftDirtyWhenValueChanges() {
    ClientLootLockState state = new ClientLootLockState();
    ServerToClientPackets.SyncPayload payload = createPayload();
    state.onAuthoritativeSync(payload);

    state.beginDraft(payload.activeProfileId()).orElseThrow().setEnabled(false);

    ClientLootLockState.ClientDraftSaveRequest saveRequest = state.buildSaveRequest().orElseThrow();
    assertEquals(payload.revision(), saveRequest.baseRevision());
    assertFalse(saveRequest.profile().isEnabled());
  }

  @Test
  void noOpMutationDoesNotProduceSaveRequest() {
    ClientLootLockState state = new ClientLootLockState();
    ServerToClientPackets.SyncPayload payload = createPayload();
    state.onAuthoritativeSync(payload);

    ClientDraftProfile draft = state.beginDraft(payload.activeProfileId()).orElseThrow();
    draft.setEnabled(draft.getDraft().isEnabled());

    assertTrue(state.buildSaveRequest().isEmpty());
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
            true,
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
        1, playerUuid, 7L, profile.getId(), List.of(profile), true, true);
  }
}
