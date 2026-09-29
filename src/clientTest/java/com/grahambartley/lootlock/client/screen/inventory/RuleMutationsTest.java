package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

import com.grahambartley.lootlock.client.LootLockClient;
import com.grahambartley.lootlock.client.network.ClientMutationSync;
import com.grahambartley.lootlock.client.state.ClientLootLockState.ClientDraftSaveRequest;
import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.data.RuleEntry;
import com.grahambartley.lootlock.network.ServerToClientPackets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class RuleMutationsTest {
  private static final long REVISION = 7L;

  private MockedStatic<ClientMutationSync> sync;

  @BeforeEach
  void setUp() {
    LootLockClient.getState().clear();
    sync = mockStatic(ClientMutationSync.class);
    sync.when(() -> ClientMutationSync.sendSaveRequest(any())).thenReturn(true);
  }

  @AfterEach
  void tearDown() {
    sync.close();
    LootLockClient.getState().clear();
  }

  static Stream<Arguments> rejectedWithoutState() {
    return Stream.of(
        Arguments.of("add null", (BooleanSupplier) () -> RuleMutations.addToActiveProfile(null)),
        Arguments.of(
            "add empty", (BooleanSupplier) () -> RuleMutations.addToActiveProfile(List.of())),
        Arguments.of(
            "add without snapshot",
            (BooleanSupplier) () -> RuleMutations.addToActiveProfile(List.of("minecraft:dirt"))),
        Arguments.of(
            "remove null", (BooleanSupplier) () -> RuleMutations.removeFromActiveProfile(null)),
        Arguments.of(
            "remove blank", (BooleanSupplier) () -> RuleMutations.removeFromActiveProfile(" ")),
        Arguments.of(
            "remove without snapshot",
            (BooleanSupplier) () -> RuleMutations.removeFromActiveProfile("minecraft:stone")),
        Arguments.of(
            "clear without snapshot", (BooleanSupplier) RuleMutations::clearActiveProfile));
  }

  @ParameterizedTest(name = "{0} returns false and sends nothing")
  @MethodSource("rejectedWithoutState")
  void rejectsWhenInputOrSnapshotMissing(String label, BooleanSupplier mutation) {
    assertFalse(mutation.getAsBoolean());
    sync.verify(() -> ClientMutationSync.sendSaveRequest(any()), never());
  }

  static Stream<Arguments> mutations() {
    return Stream.of(
        Arguments.of(
            "add new ids",
            List.of("minecraft:stone"),
            (BooleanSupplier)
                () ->
                    RuleMutations.addToActiveProfile(
                        Arrays.asList("minecraft:dirt", "minecraft:stone", "#minecraft:logs")),
            List.of("minecraft:stone", "minecraft:dirt", "#minecraft:logs")),
        Arguments.of(
            "add only existing ids",
            List.of("minecraft:stone"),
            (BooleanSupplier) () -> RuleMutations.addToActiveProfile(List.of("minecraft:stone")),
            null),
        Arguments.of(
            "remove existing id",
            List.of("minecraft:stone", "minecraft:dirt"),
            (BooleanSupplier) () -> RuleMutations.removeFromActiveProfile("minecraft:stone"),
            List.of("minecraft:dirt")),
        Arguments.of(
            "remove missing id",
            List.of("minecraft:stone"),
            (BooleanSupplier) () -> RuleMutations.removeFromActiveProfile("minecraft:dirt"),
            null),
        Arguments.of(
            "clear populated profile",
            List.of("minecraft:stone", "minecraft:dirt"),
            (BooleanSupplier) RuleMutations::clearActiveProfile,
            List.of()),
        Arguments.of(
            "clear empty profile",
            List.of(),
            (BooleanSupplier) RuleMutations::clearActiveProfile,
            null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("mutations")
  void sendsSaveRequestOnlyWhenRulesChange(
      String label,
      List<String> startingRules,
      BooleanSupplier mutation,
      List<String> expectedSavedRules) {
    UUID activeProfileId = syncActiveProfile(startingRules);

    boolean result = mutation.getAsBoolean();

    assertEquals(expectedSavedRules != null, result);
    if (expectedSavedRules == null) {
      sync.verify(() -> ClientMutationSync.sendSaveRequest(any()), never());
      return;
    }
    ArgumentCaptor<ClientDraftSaveRequest> request =
        ArgumentCaptor.forClass(ClientDraftSaveRequest.class);
    sync.verify(() -> ClientMutationSync.sendSaveRequest(request.capture()));
    assertEquals(REVISION, request.getValue().baseRevision());
    assertEquals(activeProfileId, request.getValue().profile().getId());
    assertEquals(expectedSavedRules, ruleIds(request.getValue().profile().getRules()));
  }

  private static UUID syncActiveProfile(List<String> ruleIds) {
    LootLockProfile profile =
        new LootLockProfile(
            UUID.randomUUID(),
            "Default",
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            ruleIds.stream().map(RuleEntry::new).toList());
    LootLockClient.getState()
        .onAuthoritativeSync(
            new ServerToClientPackets.SyncPayload(
                1, UUID.randomUUID(), REVISION, profile.getId(), List.of(profile), true, true));
    return profile.getId();
  }

  private static List<String> ruleIds(Collection<RuleEntry> rules) {
    return rules.stream().map(RuleEntry::itemId).toList();
  }
}
