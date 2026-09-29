package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.lootlock.client.LootLockClient;
import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.network.ServerToClientPackets;
import java.util.List;
import java.util.UUID;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ActiveProfileActionsTest {

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @BeforeEach
  @AfterEach
  void clearState() {
    LootLockClient.getState().clear();
  }

  @Test
  void noSnapshotMeansNoActiveProfileAndEnabled() {
    assertTrue(ActiveProfileActions.activeProfile().isEmpty());
    assertTrue(ActiveProfileActions.globallyEnabled());
  }

  @ParameterizedTest(name = "globallyEnabled={0}")
  @ValueSource(booleans = {true, false})
  void snapshotDrivesActiveProfileAndEnabledState(boolean enabled) {
    LootLockProfile profile = profile(enabled);
    sync(profile, true);

    assertEquals(profile.getId(), ActiveProfileActions.activeProfile().orElseThrow().getId());
    assertEquals(enabled, ActiveProfileActions.globallyEnabled());
  }

  @ParameterizedTest(name = "cycle({0}) without a snapshot does nothing")
  @ValueSource(ints = {-1, 1})
  void cycleWithoutSnapshotIsNoOp(int direction) {
    ActiveProfileActions.cycle(direction);

    assertTrue(LootLockClient.getState().getSnapshot().isEmpty());
  }

  @Test
  void deleteActionIsRefusedWhenServerPolicyForbidsIt() {
    sync(profile(true), false);

    ActiveProfileActions.setAction(RejectedItemAction.DELETE);

    assertTrue(LootLockClient.getState().getDraftProfile().isEmpty());
  }

  private static LootLockProfile profile(boolean enabled) {
    return new LootLockProfile(
        UUID.randomUUID(),
        "P",
        FilterMode.DENYLIST,
        RejectedItemAction.LEAVE_ON_GROUND,
        enabled,
        0,
        List.of());
  }

  private static void sync(LootLockProfile profile, boolean allowDelete) {
    LootLockClient.getState()
        .onAuthoritativeSync(
            new ServerToClientPackets.SyncPayload(
                1, UUID.randomUUID(), 7L, profile.getId(), List.of(profile), true, allowDelete));
  }
}
