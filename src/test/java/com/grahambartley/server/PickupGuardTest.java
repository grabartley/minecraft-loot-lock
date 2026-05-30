package com.grahambartley.server;

import com.grahambartley.api.PickupDecision;
import com.grahambartley.config.ConfigManager;
import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.data.RuleEntry;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PickupGuardTest {

	@TempDir
	Path tempDir;

	@Test
	void constructorCreatesGuard() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
		PickupGuard guard = new PickupGuard(dataManager);
		assertNotNull(guard);
	}

	@Test
	void evaluateReturnsAllowWhenProfileIsDisabled() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
		PickupGuard guard = new PickupGuard(dataManager);

		UUID playerUuid = UUID.randomUUID();
		LootLockPlayerData playerData = dataManager.getOrLoad(playerUuid);
		LootLockProfile profile = playerData.getActiveProfile().orElseThrow();
		profile.setEnabled(false);

		assertEquals(PickupDecision.ALLOW, guard.evaluate(playerUuid, Identifier.tryParse("minecraft:diamond"), dataManager));
	}

	@Test
	void evaluateReturnsAllowWhenPlayerDataHasNoActiveProfile() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
		PickupGuard guard = new PickupGuard(dataManager);

		UUID playerUuid = UUID.randomUUID();
		LootLockPlayerData playerData = dataManager.getOrLoad(playerUuid);
		playerData.setActiveProfileId(null);

		assertEquals(PickupDecision.ALLOW, guard.evaluate(playerUuid, Identifier.tryParse("minecraft:diamond"), dataManager));
	}

	@Test
	void evaluateReturnsRejectLeaveForDeniedItemInDenylist() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
		PickupGuard guard = new PickupGuard(dataManager);

		UUID playerUuid = UUID.randomUUID();
		LootLockPlayerData playerData = dataManager.getOrLoad(playerUuid);
		LootLockProfile profile = playerData.getActiveProfile().orElseThrow();
		profile.setMode(FilterMode.DENYLIST);
		profile.setRules(List.of(new RuleEntry("minecraft:cobblestone")));
		profile.compileRules();

		assertEquals(PickupDecision.REJECT_LEAVE, guard.evaluate(playerUuid, Identifier.tryParse("minecraft:cobblestone"), dataManager));
	}

	@Test
	void evaluateReturnsAllowForUnlistedItemInDenylist() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
		PickupGuard guard = new PickupGuard(dataManager);

		UUID playerUuid = UUID.randomUUID();
		LootLockPlayerData playerData = dataManager.getOrLoad(playerUuid);
		LootLockProfile profile = playerData.getActiveProfile().orElseThrow();
		profile.setMode(FilterMode.DENYLIST);
		profile.setRules(List.of(new RuleEntry("minecraft:cobblestone")));
		profile.compileRules();

		assertEquals(PickupDecision.ALLOW, guard.evaluate(playerUuid, Identifier.tryParse("minecraft:diamond"), dataManager));
	}

	@Test
	void evaluateReturnsRejectDeleteForDeniedItemWithDeleteAction() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
		PickupGuard guard = new PickupGuard(dataManager);

		UUID playerUuid = UUID.randomUUID();
		LootLockPlayerData playerData = dataManager.getOrLoad(playerUuid);
		LootLockProfile profile = playerData.getActiveProfile().orElseThrow();
		profile.setMode(FilterMode.DENYLIST);
		profile.setRejectedItemAction(RejectedItemAction.DELETE);
		profile.setRules(List.of(new RuleEntry("minecraft:cobblestone")));
		profile.compileRules();

		assertEquals(PickupDecision.REJECT_DELETE, guard.evaluate(playerUuid, Identifier.tryParse("minecraft:cobblestone"), dataManager));
	}

	@Test
	void checkNotificationCooldownReturnsTrueOnFirstCall() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
		PickupGuard guard = new PickupGuard(dataManager);

		UUID playerUuid = UUID.randomUUID();

		assertTrue(guard.checkNotificationCooldown(playerUuid, 100));
	}

	@Test
	void checkNotificationCooldownReturnsFalseWithinSameTick() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
		PickupGuard guard = new PickupGuard(dataManager);

		UUID playerUuid = UUID.randomUUID();

		assertTrue(guard.checkNotificationCooldown(playerUuid, 100));
		assertFalse(guard.checkNotificationCooldown(playerUuid, 100));
	}

	@Test
	void checkNotificationCooldownReturnsFalseBeforeCooldownExpires() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
		PickupGuard guard = new PickupGuard(dataManager);

		UUID playerUuid = UUID.randomUUID();

		assertTrue(guard.checkNotificationCooldown(playerUuid, 100));
		assertFalse(guard.checkNotificationCooldown(playerUuid, 139));
	}

	@Test
	void checkNotificationCooldownReturnsTrueAfterCooldownExpires() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
		PickupGuard guard = new PickupGuard(dataManager);

		UUID playerUuid = UUID.randomUUID();

		assertTrue(guard.checkNotificationCooldown(playerUuid, 100));
		assertTrue(guard.checkNotificationCooldown(playerUuid, 140));
	}

	@Test
	void checkNotificationCooldownTracksLastTick() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
		PickupGuard guard = new PickupGuard(dataManager);

		UUID playerUuid = UUID.randomUUID();

		guard.checkNotificationCooldown(playerUuid, 100);
		assertEquals(100, guard.getNotificationCooldownTick(playerUuid));

		guard.checkNotificationCooldown(playerUuid, 200);
		assertEquals(200, guard.getNotificationCooldownTick(playerUuid));
	}

	@Test
	void clearNotificationCooldownRemovesTracking() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
		PickupGuard guard = new PickupGuard(dataManager);

		UUID playerUuid = UUID.randomUUID();

		guard.checkNotificationCooldown(playerUuid, 100);
		assertTrue(guard.hasNotificationCooldown(playerUuid));

		guard.clearNotificationCooldown(playerUuid);
		assertFalse(guard.hasNotificationCooldown(playerUuid));
	}

	@Test
	void notificationCooldownsArePerPlayer() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
		PickupGuard guard = new PickupGuard(dataManager);

		UUID playerA = UUID.randomUUID();
		UUID playerB = UUID.randomUUID();

		guard.checkNotificationCooldown(playerA, 100);
		assertFalse(guard.checkNotificationCooldown(playerA, 100));
		assertTrue(guard.checkNotificationCooldown(playerB, 100));
	}
}
