package com.grahambartley.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.client.LootLockClient;
import com.grahambartley.client.state.ClientDraftProfile;
import com.grahambartley.client.state.ClientLootLockState.ClientDraftSaveRequest;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.network.ServerToClientPackets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the chip-click colour cycle on {@link LootLockInventoryPanel}: palette wraparound, the
 * round-trip into the draft-save pipeline, and the fall-back render colour for legacy profiles that
 * have not been recoloured yet.
 */
class ProfileColorCycleTest {
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

  @Test
  void nextProfileColorAdvancesByOne() {
    assertEquals(
        Palette.PROFILE_COLORS[1],
        LootLockInventoryPanel.nextProfileColor(Palette.PROFILE_COLORS[0]));
    assertEquals(
        Palette.PROFILE_COLORS[5],
        LootLockInventoryPanel.nextProfileColor(Palette.PROFILE_COLORS[4]));
  }

  @Test
  void nextProfileColorWrapsAroundFromLastEntry() {
    int last = Palette.PROFILE_COLORS[Palette.PROFILE_COLORS.length - 1];
    assertEquals(Palette.PROFILE_COLORS[0], LootLockInventoryPanel.nextProfileColor(last));
  }

  @Test
  void nextProfileColorTreatsUnsetAsIndexZero() {
    // Legacy profiles default to color == 0, which is not a palette entry — first click should
    // move them to PROFILE_COLORS[1], the first visible delta from the fall-back.
    assertEquals(Palette.PROFILE_COLORS[1], LootLockInventoryPanel.nextProfileColor(0));
  }

  @Test
  void colorForProfileFallsBackToPaletteDefault() {
    LootLockProfile legacy =
        new LootLockProfile(
            UUID.randomUUID(),
            "Legacy",
            com.grahambartley.data.FilterMode.DENYLIST,
            com.grahambartley.data.RejectedItemAction.LEAVE_ON_GROUND,
            true,
            List.of());
    assertEquals(0, legacy.getColor());
    assertEquals(Palette.PROFILE_COLORS[0], LootLockInventoryPanel.colorForProfile(legacy));
  }

  @Test
  void colorForProfileReturnsPersistedColorWhenSet() {
    LootLockProfile profile =
        new LootLockProfile(
            UUID.randomUUID(),
            "Tinted",
            com.grahambartley.data.FilterMode.DENYLIST,
            com.grahambartley.data.RejectedItemAction.LEAVE_ON_GROUND,
            true,
            Palette.PROFILE_COLORS[3],
            List.of());
    assertEquals(Palette.PROFILE_COLORS[3], LootLockInventoryPanel.colorForProfile(profile));
  }

  @Test
  void cycleProfileColorMarksDraftDirtyAndProducesSaveRequest() {
    LootLockProfile profile = legacyProfile();
    primeClientState(profile);
    LootLockInventoryPanel panel = new LootLockInventoryPanel();

    panel.cycleProfileColor(profile.getId());

    ClientDraftProfile draft = LootLockClient.getState().getDraftProfile().orElseThrow();
    assertTrue(draft.isDirty(), "draft should be dirty after colour cycle");
    assertEquals(Palette.PROFILE_COLORS[1], draft.getDraft().getColor());

    assertEquals(1, captured.size(), "exactly one save request should be dispatched");
    ClientDraftSaveRequest saveRequest = captured.get(0);
    assertEquals(Palette.PROFILE_COLORS[1], saveRequest.profile().getColor());
    assertNotEquals(0, saveRequest.profile().getColor());
    assertEquals(7L, saveRequest.baseRevision(), "dispatch uses the snapshot revision");
  }

  @Test
  void cycleProfileColorWrapsLastPaletteEntryBackToFirst() {
    int last = Palette.PROFILE_COLORS[Palette.PROFILE_COLORS.length - 1];
    LootLockProfile profile =
        new LootLockProfile(
            UUID.randomUUID(),
            "Last",
            com.grahambartley.data.FilterMode.DENYLIST,
            com.grahambartley.data.RejectedItemAction.LEAVE_ON_GROUND,
            true,
            last,
            List.of());
    primeClientState(profile);
    LootLockInventoryPanel panel = new LootLockInventoryPanel();

    panel.cycleProfileColor(profile.getId());

    ClientDraftProfile draft = LootLockClient.getState().getDraftProfile().orElseThrow();
    assertEquals(Palette.PROFILE_COLORS[0], draft.getDraft().getColor());
    assertEquals(1, captured.size());
    assertEquals(Palette.PROFILE_COLORS[0], captured.get(0).profile().getColor());
  }

  @Test
  void cycleProfileColorIsNoOpWhenProfileMissing() {
    LootLockProfile profile = legacyProfile();
    primeClientState(profile);
    LootLockInventoryPanel panel = new LootLockInventoryPanel();

    panel.cycleProfileColor(UUID.randomUUID());

    assertTrue(LootLockClient.getState().getDraftProfile().isEmpty());
    assertTrue(captured.isEmpty());
  }

  @Test
  void cycleProfileColorIsNoOpWithoutSnapshot() {
    LootLockInventoryPanel panel = new LootLockInventoryPanel();

    panel.cycleProfileColor(UUID.randomUUID());

    assertTrue(LootLockClient.getState().getDraftProfile().isEmpty());
    assertTrue(captured.isEmpty());
  }

  private static LootLockProfile legacyProfile() {
    return new LootLockProfile(
        UUID.randomUUID(),
        "Legacy",
        com.grahambartley.data.FilterMode.DENYLIST,
        com.grahambartley.data.RejectedItemAction.LEAVE_ON_GROUND,
        true,
        List.of());
  }

  private static void primeClientState(LootLockProfile profile) {
    ServerToClientPackets.SyncPayload payload =
        new ServerToClientPackets.SyncPayload(
            1, UUID.randomUUID(), 7L, profile.getId(), List.of(profile), true, true);
    LootLockClient.getState().onAuthoritativeSync(payload);
  }
}
