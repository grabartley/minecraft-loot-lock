package com.grahambartley.lootlock.server;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.grahambartley.lootlock.LootLock;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ServerLifecycleHooksTest {
  private final MinecraftServer server = mock(MinecraftServer.class);

  @BeforeAll
  static void registerAsIfTwoWorldsStarted() {
    ServerLifecycleHooks.register();
    ServerLifecycleHooks.register();
  }

  @AfterEach
  void clearWorldState() {
    LootLock.PLAYER_DATA_MANAGER = null;
    LootLock.PICKUP_GUARD = null;
  }

  @Test
  void tickRunsOnceOnTheCurrentWorldsManagerOnly() {
    ServerPlayerDataManager firstWorld = mock(ServerPlayerDataManager.class);
    ServerPlayerDataManager secondWorld = mock(ServerPlayerDataManager.class);
    LootLock.PLAYER_DATA_MANAGER = firstWorld;
    LootLock.PLAYER_DATA_MANAGER = secondWorld;

    ServerTickEvents.END_SERVER_TICK.invoker().onEndTick(server);

    verify(secondWorld).tick(server);
    verify(firstWorld, never()).tick(server);
  }

  @Test
  void stoppingFlushesTheCurrentWorldsManagerOnce() {
    ServerPlayerDataManager firstWorld = mock(ServerPlayerDataManager.class);
    ServerPlayerDataManager secondWorld = mock(ServerPlayerDataManager.class);
    LootLock.PLAYER_DATA_MANAGER = firstWorld;
    LootLock.PLAYER_DATA_MANAGER = secondWorld;

    ServerLifecycleEvents.SERVER_STOPPING.invoker().onServerStopping(server);

    verify(secondWorld).flushAll();
    verify(firstWorld, never()).flushAll();
  }

  @Test
  void listenersIgnoreEventsWhenNoWorldIsOpen() {
    assertDoesNotThrow(
        () -> {
          ServerTickEvents.END_SERVER_TICK.invoker().onEndTick(server);
          ServerLifecycleEvents.SERVER_STOPPING.invoker().onServerStopping(server);
        });
  }
}
