package com.grahambartley.lootlock.server;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
  void tickRunsOnceOnWhicheverWorldIsOpen() {
    ServerPlayerDataManager firstWorld = mock(ServerPlayerDataManager.class);
    ServerPlayerDataManager secondWorld = mock(ServerPlayerDataManager.class);

    LootLock.PLAYER_DATA_MANAGER = firstWorld;
    ServerTickEvents.END_SERVER_TICK.invoker().onEndTick(server);
    LootLock.PLAYER_DATA_MANAGER = secondWorld;
    ServerTickEvents.END_SERVER_TICK.invoker().onEndTick(server);

    verify(firstWorld, times(1)).tick(server);
    verify(secondWorld, times(1)).tick(server);
  }

  @Test
  void stoppingFlushesWhicheverWorldIsOpenOnce() {
    ServerPlayerDataManager firstWorld = mock(ServerPlayerDataManager.class);
    ServerPlayerDataManager secondWorld = mock(ServerPlayerDataManager.class);

    LootLock.PLAYER_DATA_MANAGER = firstWorld;
    ServerLifecycleEvents.SERVER_STOPPING.invoker().onServerStopping(server);
    LootLock.PLAYER_DATA_MANAGER = secondWorld;
    ServerLifecycleEvents.SERVER_STOPPING.invoker().onServerStopping(server);

    verify(firstWorld, times(1)).flushAll();
    verify(secondWorld, times(1)).flushAll();
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
