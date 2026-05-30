package com.grahambartley.server;

import com.grahambartley.config.ConfigManager;
import com.grahambartley.data.LootLockPlayerData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
			LOGGER.debug("Saved player data for {} on disconnect (revision {})", playerUuid, entry.data.getRevision());
		}
	}

	public void flushAll() {
		for (Map.Entry<UUID, CachedEntry> entry : cache.entrySet()) {
			if (entry.getValue().dirty) {
				configManager.savePlayerData(entry.getValue().data);
				entry.getValue().dirty = false;
				LOGGER.debug("Flushed player data for {} (revision {})", entry.getKey(), entry.getValue().data.getRevision());
			}
		}
	}

	public void tick(MinecraftServer server) {
		tick(server.getTicks());
	}

	public int getCacheSize() {
		return cache.size();
	}

	LootLockPlayerData getOrLoad(UUID playerUuid) {
		CachedEntry entry = cache.get(playerUuid);
		if (entry == null) {
			LootLockPlayerData data = configManager.loadPlayerData(playerUuid);
			data.compileProfiles();
			entry = new CachedEntry(data);
			cache.put(playerUuid, entry);
			LOGGER.debug("Loaded player data for {} (revision {})", playerUuid, data.getRevision());
		}
		return entry.data;
	}

	void markDirty(UUID playerUuid, long currentTick) {
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
				LOGGER.debug("Debounced save for {} (revision {})", entry.getKey(), cached.data.getRevision());
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
