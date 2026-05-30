package com.grahambartley.rules;

import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.data.RuleEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleEngineTest {

	@Test
	void denylistRejectsListedItem() {
		LootLockProfile profile = new LootLockProfile(
			UUID.randomUUID(),
			"Test",
			FilterMode.DENYLIST,
			RejectedItemAction.LEAVE_ON_GROUND,
			true,
			List.of(new RuleEntry("minecraft:dirt"))
		);

		assertTrue(profile.shouldReject(true));
	}

	@Test
	void denylistAcceptsUnlistedItem() {
		LootLockProfile profile = new LootLockProfile(
			UUID.randomUUID(),
			"Test",
			FilterMode.DENYLIST,
			RejectedItemAction.LEAVE_ON_GROUND,
			true,
			List.of(new RuleEntry("minecraft:dirt"))
		);

		assertFalse(profile.shouldReject(false));
	}

	@Test
	void allowlistAcceptsListedItem() {
		LootLockProfile profile = new LootLockProfile(
			UUID.randomUUID(),
			"Test",
			FilterMode.ALLOWLIST,
			RejectedItemAction.LEAVE_ON_GROUND,
			true,
			List.of(new RuleEntry("minecraft:diamond"))
		);

		assertFalse(profile.shouldReject(true));
	}

	@Test
	void allowlistRejectsUnlistedItem() {
		LootLockProfile profile = new LootLockProfile(
			UUID.randomUUID(),
			"Test",
			FilterMode.ALLOWLIST,
			RejectedItemAction.LEAVE_ON_GROUND,
			true,
			List.of(new RuleEntry("minecraft:diamond"))
		);

		assertTrue(profile.shouldReject(false));
	}

	@Test
	void disabledProfileAcceptsAll() {
		LootLockProfile profile = new LootLockProfile(
			UUID.randomUUID(),
			"Test",
			FilterMode.DENYLIST,
			RejectedItemAction.LEAVE_ON_GROUND,
			false,
			List.of(new RuleEntry("minecraft:dirt"))
		);

		assertFalse(profile.shouldReject(true));
		assertFalse(profile.shouldReject(false));
	}

	@Test
	void disabledAllowlistProfileAcceptsAll() {
		LootLockProfile profile = new LootLockProfile(
			UUID.randomUUID(),
			"Test",
			FilterMode.ALLOWLIST,
			RejectedItemAction.LEAVE_ON_GROUND,
			false,
			List.of(new RuleEntry("minecraft:diamond"))
		);

		assertFalse(profile.shouldReject(true));
		assertFalse(profile.shouldReject(false));
	}

	@Test
	void missingActiveProfileAcceptsAll() {
		LootLockPlayerData data = new LootLockPlayerData();
		data.setActiveProfileId(UUID.randomUUID());

		Optional<LootLockProfile> active = data.getActiveProfile();
		assertTrue(active.isEmpty());
	}

	@Test
	void emptyDenylistAcceptsAll() {
		LootLockProfile profile = new LootLockProfile(
			UUID.randomUUID(),
			"Empty Denylist",
			FilterMode.DENYLIST,
			RejectedItemAction.LEAVE_ON_GROUND,
			true,
			List.of()
		);

		assertTrue(profile.getCompiledRuleSet().isEmpty());
		assertFalse(profile.shouldReject(false));
	}

	@Test
	void emptyAllowlistRejectsAll() {
		LootLockProfile profile = new LootLockProfile(
			UUID.randomUUID(),
			"Empty Allowlist",
			FilterMode.ALLOWLIST,
			RejectedItemAction.LEAVE_ON_GROUND,
			true,
			List.of()
		);

		assertTrue(profile.getCompiledRuleSet().isEmpty());
		assertTrue(profile.shouldReject(false));
	}

	@Test
	void profileDefaultsToDenylistEnabled() {
		LootLockProfile profile = LootLockProfile.createDefault();

		assertEquals(FilterMode.DENYLIST, profile.getMode());
		assertTrue(profile.isEnabled());
		assertEquals("Default", profile.getName());
		assertEquals(RejectedItemAction.LEAVE_ON_GROUND, profile.getRejectedItemAction());
		assertTrue(profile.getRules().isEmpty());
	}
}
