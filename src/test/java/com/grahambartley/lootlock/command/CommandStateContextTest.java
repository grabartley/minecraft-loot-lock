package com.grahambartley.lootlock.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.grahambartley.lootlock.data.LootLockPlayerData;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.server.ServerPlayerDataManager;
import java.util.UUID;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CommandStateContextTest {
  private static final UUID TARGET_UUID = UUID.randomUUID();

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @Test
  void withProfileReplacesOnlyTheProfile() {
    ServerPlayerDataManager dataManager = mock(ServerPlayerDataManager.class);
    CommandStateContext original = offlineState(dataManager);
    LootLockProfile replacement = LootLockProfile.createDefault();

    CommandStateContext swapped = original.withProfile(replacement);

    assertSame(replacement, swapped.profile());
    assertEquals(original.targetUuid(), swapped.targetUuid());
    assertEquals(original.displayName(), swapped.displayName());
    assertEquals(original.onlineTarget(), swapped.onlineTarget());
    assertEquals(original.isSelfTargeted(), swapped.isSelfTargeted());
    assertSame(original.dataManager(), swapped.dataManager());
    assertSame(original.data(), swapped.data());
  }

  @Test
  void markDirtyUsesOnlinePlayerWhenTargetIsOnline() {
    ServerPlayerDataManager dataManager = mock(ServerPlayerDataManager.class);
    ServerPlayerEntity online = mock(ServerPlayerEntity.class);
    LootLockPlayerData data = LootLockPlayerData.createDefault(TARGET_UUID);
    CommandStateContext state =
        new CommandStateContext(
            TARGET_UUID,
            "Steve",
            online,
            true,
            dataManager,
            data,
            data.getActiveProfile().orElseThrow());
    ServerCommandSource source = mock(ServerCommandSource.class);

    state.markDirty(source);

    verify(dataManager).markDirty(online);
    verifyNoInteractions(source);
  }

  @ParameterizedTest(name = "server present={0} marks dirty at tick {1}")
  @CsvSource({
    "true,  1234",
    "false, 0",
  })
  void markDirtyUsesUuidAndServerTickWhenTargetIsOffline(boolean serverPresent, long expectedTick) {
    ServerPlayerDataManager dataManager = mock(ServerPlayerDataManager.class);
    ServerCommandSource source = mock(ServerCommandSource.class);
    if (serverPresent) {
      MinecraftServer server = mock(MinecraftServer.class);
      when(server.getTicks()).thenReturn((int) expectedTick);
      when(source.getServer()).thenReturn(server);
    }

    offlineState(dataManager).markDirty(source);

    verify(dataManager).markDirty(TARGET_UUID, expectedTick);
  }

  @Test
  void syncIfOnlineDoesNothingForOfflineTarget() {
    ServerPlayerDataManager dataManager = mock(ServerPlayerDataManager.class);

    offlineState(dataManager).syncIfOnline();

    verifyNoInteractions(dataManager);
  }

  private static CommandStateContext offlineState(ServerPlayerDataManager dataManager) {
    LootLockPlayerData data = LootLockPlayerData.createDefault(TARGET_UUID);
    return new CommandStateContext(
        TARGET_UUID, "Alex", null, false, dataManager, data, data.getActiveProfile().orElseThrow());
  }
}
