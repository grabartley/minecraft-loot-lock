package com.grahambartley.lootlock.server;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grahambartley.lootlock.LootLock;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ServerLifecycleHooksTest {
  private static final UUID PLAYER = UUID.randomUUID();
  private final MinecraftServer server = mock(MinecraftServer.class);

  @BeforeAll
  static void registerAsIfTwoWorldsStarted() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
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
  void disconnectSavesAndClearsCooldownOnTheOpenWorldOnce() {
    ServerPlayerDataManager manager = mock(ServerPlayerDataManager.class);
    PickupGuard guard = mock(PickupGuard.class);
    LootLock.PLAYER_DATA_MANAGER = manager;
    LootLock.PICKUP_GUARD = guard;

    ServerPlayConnectionEvents.DISCONNECT.invoker().onPlayDisconnect(handlerFor(PLAYER), server);

    verify(manager, times(1)).saveOnDisconnect(PLAYER);
    verify(guard, times(1)).clearNotificationCooldown(PLAYER);
  }

  @Test
  void listenersIgnoreEventsWhenNoWorldIsOpen() {
    ServerPlayNetworkHandler handler = handlerFor(PLAYER);

    assertDoesNotThrow(
        () -> {
          ServerTickEvents.END_SERVER_TICK.invoker().onEndTick(server);
          ServerLifecycleEvents.SERVER_STOPPING.invoker().onServerStopping(server);
          ServerPlayConnectionEvents.DISCONNECT.invoker().onPlayDisconnect(handler, server);
        });
  }

  private static ServerPlayNetworkHandler handlerFor(UUID playerUuid) {
    ServerPlayerEntity player = mock(ServerPlayerEntity.class);
    when(player.getUuid()).thenReturn(playerUuid);
    ServerPlayNetworkHandler handler = mock(ServerPlayNetworkHandler.class);
    handler.player = player;
    return handler;
  }
}
