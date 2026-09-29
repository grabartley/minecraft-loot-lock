package com.grahambartley.lootlock.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.grahambartley.lootlock.LootLock;
import com.grahambartley.lootlock.config.ConfigPaths;
import com.grahambartley.lootlock.config.LootLockConfig;
import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ServerPolicyServiceTest {
  @TempDir Path worldDir;

  @AfterEach
  void resetPolicy() {
    LootLock.SERVER_CONFIG = LootLockConfig.defaults();
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void updatePersistsAndAppliesThePolicy(boolean allowDelete) {
    MinecraftServer server = mock(MinecraftServer.class);
    when(server.getSavePath(WorldSavePath.ROOT)).thenReturn(worldDir);
    LootLock.SERVER_CONFIG = new LootLockConfig(!allowDelete);

    boolean updated = ServerPolicyService.updateAllowDeleteRejectedItems(server, allowDelete);

    assertTrue(updated);
    assertEquals(allowDelete, LootLock.SERVER_CONFIG.allowDeleteRejectedItems());
    assertEquals(
        allowDelete,
        LootLockConfig.load(new ConfigPaths(worldDir).getServerPolicyPath())
            .allowDeleteRejectedItems());
  }

  @Test
  void updateWithoutAServerChangesNothing() {
    boolean updated = ServerPolicyService.updateAllowDeleteRejectedItems(null, false);

    assertFalse(updated);
    assertTrue(LootLock.SERVER_CONFIG.allowDeleteRejectedItems());
  }
}
