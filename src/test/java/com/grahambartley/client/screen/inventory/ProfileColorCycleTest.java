package com.grahambartley.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.client.LootLockClient;
import com.grahambartley.client.state.ClientDraftProfile;
import com.grahambartley.client.state.ClientLootLockState.ClientDraftSaveRequest;
import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.network.ServerToClientPackets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ProfileColorCycleTest {

  private static final int LAST_PALETTE_INDEX = Palette.PROFILE_COLORS.length - 1;

  private Consumer<ClientDraftSaveRequest> originalDispatcher;
  private List<ClientDraftSaveRequest> captured;

  @BeforeEach
  void swapDispatcher() {
    LootLockClient.getState().clear();
    originalDispatcher = LootLockInventoryPanel.saveRequestDispatcher;
    captured = new ArrayList<>();
    LootLockInventoryPanel.saveRequestDispatcher = captured::add;
  }

  @AfterEach
  void restoreDispatcher() {
    LootLockInventoryPanel.saveRequestDispatcher = originalDispatcher;
    LootLockClient.getState().clear();
  }

  static Stream<Arguments> nextColorCases() {
    return Stream.of(
        Arguments.of("advance by one", Palette.PROFILE_COLORS[0], Palette.PROFILE_COLORS[1]),
        Arguments.of("advance past mid", Palette.PROFILE_COLORS[4], Palette.PROFILE_COLORS[5]),
        Arguments.of(
            "wrap from last to first",
            Palette.PROFILE_COLORS[LAST_PALETTE_INDEX],
            Palette.PROFILE_COLORS[0]),
        Arguments.of("treat unset (0) as index 0", 0, Palette.PROFILE_COLORS[1]));
  }

  @ParameterizedTest(name = "{0}: {1} -> {2}")
  @MethodSource("nextColorCases")
  void nextProfileColorAdvancesOrWraps(String label, int current, int expected) {
    assertEquals(expected, LootLockInventoryPanel.nextProfileColor(current));
  }

  static Stream<Arguments> colorForProfileCases() {
    return Stream.of(
        Arguments.of(
            "legacy (color 0) falls back to palette default", 0, Palette.PROFILE_COLORS[0]),
        Arguments.of(
            "persisted color is returned", Palette.PROFILE_COLORS[3], Palette.PROFILE_COLORS[3]));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("colorForProfileCases")
  void colorForProfileReturnsExpected(String label, int storedColor, int expected) {
    LootLockProfile profile = newProfile(storedColor);

    assertEquals(expected, LootLockInventoryPanel.colorForProfile(profile));
  }

  @Test
  void cycleProfileColorMarksDraftDirtyAndProducesSaveRequest() {
    LootLockProfile profile = newProfile(0);
    primeClientState(profile);

    new LootLockInventoryPanel().cycleProfileColor(profile.getId());

    ClientDraftProfile draft = LootLockClient.getState().getDraftProfile().orElseThrow();
    assertTrue(draft.isDirty());
    assertEquals(Palette.PROFILE_COLORS[1], draft.getDraft().getColor());

    assertEquals(1, captured.size());
    ClientDraftSaveRequest saveRequest = captured.get(0);
    assertEquals(Palette.PROFILE_COLORS[1], saveRequest.profile().getColor());
    assertNotEquals(0, saveRequest.profile().getColor());
    assertEquals(7L, saveRequest.baseRevision());
  }

  @Test
  void cycleProfileColorWrapsLastPaletteEntryBackToFirst() {
    LootLockProfile profile = newProfile(Palette.PROFILE_COLORS[LAST_PALETTE_INDEX]);
    primeClientState(profile);

    new LootLockInventoryPanel().cycleProfileColor(profile.getId());

    ClientDraftProfile draft = LootLockClient.getState().getDraftProfile().orElseThrow();
    assertEquals(Palette.PROFILE_COLORS[0], draft.getDraft().getColor());
    assertEquals(1, captured.size());
    assertEquals(Palette.PROFILE_COLORS[0], captured.get(0).profile().getColor());
  }

  @Test
  void cycleProfileColorIsNoOpWhenProfileMissing() {
    LootLockProfile profile = newProfile(0);
    primeClientState(profile);

    new LootLockInventoryPanel().cycleProfileColor(UUID.randomUUID());

    assertTrue(LootLockClient.getState().getDraftProfile().isEmpty());
    assertTrue(captured.isEmpty());
  }

  @Test
  void cycleProfileColorIsNoOpWithoutSnapshot() {
    new LootLockInventoryPanel().cycleProfileColor(UUID.randomUUID());

    assertTrue(LootLockClient.getState().getDraftProfile().isEmpty());
    assertTrue(captured.isEmpty());
  }

  private static LootLockProfile newProfile(int color) {
    return new LootLockProfile(
        UUID.randomUUID(),
        "Profile",
        FilterMode.DENYLIST,
        RejectedItemAction.LEAVE_ON_GROUND,
        true,
        color,
        List.of());
  }

  private static void primeClientState(LootLockProfile profile) {
    LootLockClient.getState()
        .onAuthoritativeSync(
            new ServerToClientPackets.SyncPayload(
                1, UUID.randomUUID(), 7L, profile.getId(), List.of(profile), true, true));
  }
}
