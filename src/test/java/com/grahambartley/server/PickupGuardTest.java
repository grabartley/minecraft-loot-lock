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

		assertEquals(PickupDecision.ALLOW, guard.evaluate(playerUuid, Identifier.tryParse("minecraft:diamond")));
	}

	@Test
	void evaluateReturnsAllowWhenPlayerDataHasNoActiveProfile() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
		PickupGuard guard = new PickupGuard(dataManager);

		UUID playerUuid = UUID.randomUUID();
		LootLockPlayerData playerData = dataManager.getOrLoad(playerUuid);
		playerData.setActiveProfileId(null);

		assertEquals(PickupDecision.ALLOW, guard.evaluate(playerUuid, Identifier.tryParse("minecraft:diamond")));
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

		assertEquals(PickupDecision.REJECT_LEAVE, guard.evaluate(playerUuid, Identifier.tryParse("minecraft:cobblestone")));
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

		assertEquals(PickupDecision.ALLOW, guard.evaluate(playerUuid, Identifier.tryParse("minecraft:diamond")));
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

		assertEquals(PickupDecision.REJECT_DELETE, guard.evaluate(playerUuid, Identifier.tryParse("minecraft:cobblestone")));
	}

	@Test
	void evaluateReturnsAllowForListedItemInAllowlist() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
		PickupGuard guard = new PickupGuard(dataManager);

		UUID playerUuid = UUID.randomUUID();
		LootLockPlayerData playerData = dataManager.getOrLoad(playerUuid);
		LootLockProfile profile = playerData.getActiveProfile().orElseThrow();
		profile.setMode(FilterMode.ALLOWLIST);
		profile.setRules(List.of(new RuleEntry("minecraft:diamond")));
		profile.compileRules();

		assertEquals(PickupDecision.ALLOW, guard.evaluate(playerUuid, Identifier.tryParse("minecraft:diamond")));
	}

	@Test
	void evaluateReturnsRejectLeaveForUnlistedItemInAllowlist() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
		PickupGuard guard = new PickupGuard(dataManager);

		UUID playerUuid = UUID.randomUUID();
		LootLockPlayerData playerData = dataManager.getOrLoad(playerUuid);
		LootLockProfile profile = playerData.getActiveProfile().orElseThrow();
		profile.setMode(FilterMode.ALLOWLIST);
		profile.setRules(List.of(new RuleEntry("minecraft:diamond")));
		profile.compileRules();

		assertEquals(PickupDecision.REJECT_LEAVE, guard.evaluate(playerUuid, Identifier.tryParse("minecraft:cobblestone")));
	}

	@Test
	void evaluateWithDisabledProfileReturnsAllowEvenForUnknownItemId() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
		PickupGuard guard = new PickupGuard(dataManager);

		UUID playerUuid = UUID.randomUUID();
		LootLockPlayerData playerData = dataManager.getOrLoad(playerUuid);
		LootLockProfile profile = playerData.getActiveProfile().orElseThrow();
		profile.setEnabled(false);
		profile.setRules(List.of(new RuleEntry("oldmod:removed_item")));
		profile.compileRules();

		assertEquals(PickupDecision.ALLOW, guard.evaluate(playerUuid, Identifier.tryParse("oldmod:removed_item")));
	}

	@Test
	void evaluateWithUnknownItemIdDoesNotMatch() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
		PickupGuard guard = new PickupGuard(dataManager);

		UUID playerUuid = UUID.randomUUID();
		LootLockPlayerData playerData = dataManager.getOrLoad(playerUuid);
		LootLockProfile profile = playerData.getActiveProfile().orElseThrow();
		profile.setRules(List.of(new RuleEntry("oldmod:removed_item")));
		profile.compileRules();

		assertEquals(PickupDecision.ALLOW, guard.evaluate(playerUuid, Identifier.tryParse("minecraft:diamond")));
		assertEquals(PickupDecision.REJECT_LEAVE, guard.evaluate(playerUuid, Identifier.tryParse("oldmod:removed_item")));
	}

	@Test
	void tryNotifyWithNullPlayerReturnsTrueAndSkipsCooldownStamp() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
		PickupGuard guard = new PickupGuard(dataManager);

		UUID playerUuid = UUID.randomUUID();

		assertTrue(guard.tryNotify(playerUuid, null, false, 100));
		assertFalse(guard.hasNotificationCooldown(playerUuid));
	}

	@Test
	void tryNotifyWithPlayerStampsCooldown() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
		PickupGuard guard = new PickupGuard(dataManager);

		UUID playerUuid = UUID.randomUUID();

		assertTrue(guard.tryNotify(playerUuid, null, false, 100));
		assertFalse(guard.hasNotificationCooldown(playerUuid), "null player should not stamp cooldown");
	}

	@Test
	void clearNotificationCooldownRemovesTracking() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
		PickupGuard guard = new PickupGuard(dataManager);

		UUID playerUuid = UUID.randomUUID();

		assertFalse(guard.hasNotificationCooldown(playerUuid));

		guard.clearNotificationCooldown(playerUuid);
		assertFalse(guard.hasNotificationCooldown(playerUuid));
	}

	@Test
	void clearNotificationCooldownOnUnknownPlayerDoesNotThrow() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
		PickupGuard guard = new PickupGuard(dataManager);

		guard.clearNotificationCooldown(UUID.randomUUID());
	}
}
