package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ProfileColorsTest {

  private static final int LAST_PALETTE_INDEX = Palette.PROFILE_COLORS.length - 1;

  private Consumer<ClientDraftSaveRequest> originalDispatcher;
  private List<ClientDraftSaveRequest> captured;

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @BeforeEach
  void swapDispatcher() {
    LootLockClient.getState().clear();
    originalDispatcher = ProfileColors.saveRequestDispatcher;
    captured = new ArrayList<>();
    ProfileColors.saveRequestDispatcher = captured::add;
  }

  @AfterEach
  void restoreDispatcher() {
    ProfileColors.saveRequestDispatcher = originalDispatcher;
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
    assertEquals(expected, ProfileColors.nextProfileColor(current));
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
    assertEquals(expected, ProfileColors.colorForProfile(newProfile(storedColor)));
  }

  static Stream<Arguments> cycleCases() {
    return Stream.of(
        Arguments.of("unset advances to second entry", 0, Palette.PROFILE_COLORS[1]),
        Arguments.of(
            "last entry wraps to first",
            Palette.PROFILE_COLORS[LAST_PALETTE_INDEX],
            Palette.PROFILE_COLORS[0]));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("cycleCases")
  void cycleProfileColorDirtiesDraftAndDispatchesSave(String label, int stored, int expected) {
    LootLockProfile profile = newProfile(stored);
    primeClientState(profile);

    ProfileColors.cycleProfileColor(profile.getId());

    ClientDraftProfile draft = LootLockClient.getState().getDraftProfile().orElseThrow();
    assertTrue(draft.isDirty());
    assertEquals(expected, draft.getDraft().getColor());
    assertEquals(1, captured.size());
    assertEquals(expected, captured.get(0).profile().getColor());
    assertEquals(7L, captured.get(0).baseRevision());
  }

  @Test
  void cycleProfileColorIsNoOpWhenProfileMissing() {
    primeClientState(newProfile(0));

    ProfileColors.cycleProfileColor(UUID.randomUUID());

    assertTrue(LootLockClient.getState().getDraftProfile().isEmpty());
    assertTrue(captured.isEmpty());
  }

  @Test
  void cycleProfileColorIsNoOpWithoutSnapshot() {
    ProfileColors.cycleProfileColor(UUID.randomUUID());

    assertTrue(LootLockClient.getState().getDraftProfile().isEmpty());
    assertTrue(captured.isEmpty());
  }

  @Test
  void cycleProfileColorIgnoresNullId() {
    primeClientState(newProfile(0));

    ProfileColors.cycleProfileColor(null);

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
