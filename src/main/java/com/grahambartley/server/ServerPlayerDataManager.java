package com.grahambartley.server;

import com.grahambartley.config.ConfigManager;
import com.grahambartley.data.LootLockPlayerData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// All access must be on the server main thread.
// Fabric lifecycle events (JOIN, DISCONNECT, SERVER_STOPPING, END_SERVER_TICK)
// all fire on the main thread. Adding command handlers, async chat, or
// networking callbacks that touch this cache requires synchronization.
public final class ServerPlayerDataManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerPlayerDataManager.class);
  static final long SAVE_DEBOUNCE_TICKS = 40;

  private final ConfigManager configManager;
  private final Map<UUID, CachedEntry> cache = new HashMap<>();

  public ServerPlayerDataManager(ConfigManager configManager) {
    this.configManager = configManager;
  }

  public LootLockPlayerData get(ServerPlayerEntity player) {
    return getOrLoad(player.getUuid());
  }

  public void markDirty(ServerPlayerEntity player) {
    markDirty(player.getUuid(), player.getServer() != null ? player.getServer().getTicks() : 0);
  }

  public void saveOnDisconnect(UUID playerUuid) {
    CachedEntry entry = cache.remove(playerUuid);
    if (entry != null && entry.dirty) {
      configManager.savePlayerData(entry.data);
      LOGGER.debug(
          "Saved player data for {} on disconnect (revision {})",
          playerUuid,
          entry.data.getRevision());
    }
  }

  public void recompileAllProfiles() {
    for (CachedEntry entry : cache.values()) {
      entry.data.compileProfiles();
    }
  }

  public int flushAll() {
    int saved = 0;
    for (Map.Entry<UUID, CachedEntry> entry : cache.entrySet()) {
      if (entry.getValue().dirty) {
        configManager.savePlayerData(entry.getValue().data);
        entry.getValue().dirty = false;
        saved++;
        LOGGER.debug(
            "Flushed player data for {} (revision {})",
            entry.getKey(),
            entry.getValue().data.getRevision());
      }
    }
    return saved;
  }

  public void tick(MinecraftServer server) {
    tick(server.getTicks());
  }

  public LootLockPlayerData getOrLoad(UUID playerUuid) {
    CachedEntry entry = cache.get(playerUuid);
    if (entry == null) {
      ConfigManager.LoadResult loaded = configManager.loadPlayerData(playerUuid);
      LootLockPlayerData data = loaded.data();
      data.compileProfiles();
      entry = new CachedEntry(data);
      entry.dirty = loaded.createdDefault();
      cache.put(playerUuid, entry);
      if (loaded.createdDefault()) {
        LOGGER.debug(
            "Created default player data for {} (revision {})", playerUuid, data.getRevision());
      } else {
        LOGGER.debug("Loaded player data for {} (revision {})", playerUuid, data.getRevision());
      }
    }
    return entry.data;
  }

  public void markDirty(UUID playerUuid, long currentTick) {
    CachedEntry entry = cache.get(playerUuid);
    if (entry != null) {
      entry.data.incrementRevision();
      entry.dirty = true;
      entry.dirtyTick = currentTick;
    }
  }

  boolean isDirty(UUID playerUuid) {
    CachedEntry entry = cache.get(playerUuid);
    return entry != null && entry.dirty;
  }

  long getDirtyTick(UUID playerUuid) {
    CachedEntry entry = cache.get(playerUuid);
    return entry != null ? entry.dirtyTick : -1;
  }

  void tick(long currentTick) {
    for (Map.Entry<UUID, CachedEntry> entry : cache.entrySet()) {
      CachedEntry cached = entry.getValue();
      if (cached.dirty && (currentTick - cached.dirtyTick >= SAVE_DEBOUNCE_TICKS)) {
        configManager.savePlayerData(cached.data);
        cached.dirty = false;
        LOGGER.debug(
            "Debounced save for {} (revision {})", entry.getKey(), cached.data.getRevision());
      }
    }
  }

  private static final class CachedEntry {
    final LootLockPlayerData data;
    boolean dirty;
    long dirtyTick;

    CachedEntry(LootLockPlayerData data) {
      this.data = data;
      this.dirty = false;
      this.dirtyTick = 0;
    }
  }
}
