package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.lootlock.client.LootLockClient;
import com.grahambartley.lootlock.client.state.ClientDraftProfile;
import com.grahambartley.lootlock.client.state.ClientLootLockState.ClientDraftSaveRequest;
import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.network.ServerToClientPackets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.stream.Stream;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class LootLockInventoryPanelTest {

  private static final int LAST_PALETTE_INDEX = Palette.PROFILE_COLORS.length - 1;

  private LongSupplier originalClock;
  private Consumer<ClientDraftSaveRequest> originalDispatcher;
  private List<ClientDraftSaveRequest> captured;
  private AtomicLong now;
  private LootLockInventoryPanel panel;

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @BeforeEach
  void swapStaticsAndPanel() {
    originalClock = LootLockInventoryPanel.clockMillis;
    now = new AtomicLong(1000L);
    LootLockInventoryPanel.clockMillis = now::get;
    LootLockClient.getState().clear();
    originalDispatcher = LootLockInventoryPanel.saveRequestDispatcher;
    captured = new ArrayList<>();
    LootLockInventoryPanel.saveRequestDispatcher = captured::add;
    panel = new LootLockInventoryPanel();
  }

  @AfterEach
  void restoreStatics() {
    LootLockInventoryPanel.clockMillis = originalClock;
    LootLockInventoryPanel.saveRequestDispatcher = originalDispatcher;
    LootLockClient.getState().clear();
  }

  @Test
  void dropArmedDefaultsToFalse() {
    assertFalse(panel.isDropArmed());
  }

  @ParameterizedTest(name = "setDropArmed({0}) reflects in isDropArmed")
  @ValueSource(booleans = {true, false})
  void setDropArmedPropagatesValue(boolean state) {
    panel.setDropArmed(state);

    assertEquals(state, panel.isDropArmed());
  }

  @Test
  void setDropArmedClearsAfterPreviouslyArmed() {
    panel.setDropArmed(true);
    panel.setDropArmed(false);

    assertFalse(panel.isDropArmed());
  }

  @Test
  void flashIsInactiveBeforeAnyDrop() {
    assertFalse(panel.isFlashActive());
    assertEquals(1f, panel.flashProgress());
  }

  @Test
  void flashDropSuccessActivatesFlash() {
    panel.flashDropSuccess();

    assertTrue(panel.isFlashActive());
    assertEquals(0f, panel.flashProgress());
  }

  @Test
  void flashProgressAdvancesLinearlyOverDuration() {
    panel.flashDropSuccess();
    now.addAndGet(LootLockInventoryPanel.FLASH_DURATION_MILLIS / 2);

    assertEquals(0.5f, panel.flashProgress(), 0.001f);
  }

  @Test
  void flashClearsExactlyAtDurationEnd() {
    panel.flashDropSuccess();
    now.addAndGet(LootLockInventoryPanel.FLASH_DURATION_MILLIS);

    assertFalse(panel.isFlashActive());
    assertEquals(1f, panel.flashProgress());
  }

  @Test
  void flashClearsAfterDurationOverrun() {
    panel.flashDropSuccess();
    now.addAndGet(LootLockInventoryPanel.FLASH_DURATION_MILLIS + 250L);

    assertFalse(panel.isFlashActive());
  }

  @Test
  void backToBackFlashesResetTheTimer() {
    panel.flashDropSuccess();
    now.addAndGet(LootLockInventoryPanel.FLASH_DURATION_MILLIS - 50L);
    panel.flashDropSuccess();
    now.addAndGet(50L);

    assertTrue(panel.isFlashActive());
    assertEquals(50f / LootLockInventoryPanel.FLASH_DURATION_MILLIS, panel.flashProgress(), 0.001f);
  }

  @Test
  void closedButtonDropSequenceLeavesPanelOpenOnRulesWithFlashActive() {
    panel.setOpen(false);

    panel.setOpen(true);
    panel.setTab(PanelTab.RULES);
    panel.clearRulesSearch();
    panel.flashDropSuccess();

    assertTrue(panel.isOpen());
    assertEquals(PanelTab.RULES, panel.getActiveTab());
    assertTrue(panel.isFlashActive());
  }

  @Test
  void clientPrefsModeAttachOmitsPerWorldWidgets() {
    panel.setClientPrefsMode(true);
    List<ClickableWidget> collected = new ArrayList<>();

    panel.attach(null, 0, 0, collected::add);

    assertFalse(
        collected.stream().anyMatch(w -> w instanceof ProfilePill),
        "ProfilePill should not be created in client-prefs mode");
    assertFalse(
        collected.stream().anyMatch(w -> w instanceof NavArrowButton),
        "NavArrowButton should not be created in client-prefs mode");
    assertFalse(
        collected.stream().anyMatch(w -> w instanceof SegmentedButton),
        "SegmentedButton (mode/action) should not be created in client-prefs mode");
    assertFalse(
        collected.stream().anyMatch(w -> w instanceof VanillaTab),
        "VanillaTab (tab row) should not be created in client-prefs mode");
    for (ClickableWidget widget : collected) {
      assertTrue(
          widget instanceof VanillaSwitch,
          "Only notification + safety switches should remain, got "
              + widget.getClass().getSimpleName());
    }
    assertEquals(
        4,
        collected.size(),
        "Expected 3 notification switches + 1 safety switch in client-prefs mode");
  }

  @Test
  void setClientPrefsModeAfterAttachThrows() {
    panel.setClientPrefsMode(true);
    panel.attach(null, 0, 0, w -> {});

    assertThrows(IllegalStateException.class, () -> panel.setClientPrefsMode(false));
  }

  @Test
  void inlineRenameInactiveByDefault() {
    assertFalse(panel.isInlineRenameActive());
  }

  @ParameterizedTest(
      name = "handleInlineRenameKey(keyCode={0}) returns false when no rename is active")
  @ValueSource(ints = {69, 257, 256, 263, 259})
  void handleInlineRenameKeyReturnsFalseWhenInactive(int keyCode) {
    assertFalse(panel.handleInlineRenameKey(keyCode, 0, 0));
  }

  @Test
  void handleInlineRenameCharReturnsFalseWhenInactive() {
    assertFalse(panel.handleInlineRenameChar('e', 0));
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

    panel.cycleProfileColor(profile.getId());

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

    panel.cycleProfileColor(profile.getId());

    ClientDraftProfile draft = LootLockClient.getState().getDraftProfile().orElseThrow();
    assertEquals(Palette.PROFILE_COLORS[0], draft.getDraft().getColor());
    assertEquals(1, captured.size());
    assertEquals(Palette.PROFILE_COLORS[0], captured.get(0).profile().getColor());
  }

  @Test
  void cycleProfileColorIsNoOpWhenProfileMissing() {
    LootLockProfile profile = newProfile(0);
    primeClientState(profile);

    panel.cycleProfileColor(UUID.randomUUID());

    assertTrue(LootLockClient.getState().getDraftProfile().isEmpty());
    assertTrue(captured.isEmpty());
  }

  @Test
  void cycleProfileColorIsNoOpWithoutSnapshot() {
    panel.cycleProfileColor(UUID.randomUUID());

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
