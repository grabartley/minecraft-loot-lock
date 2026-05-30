package com.grahambartley.config;

import com.grahambartley.config.ConfigSerializer.ConfigDeserializationException;
import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.data.RuleEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigSerializerTest {

	@Test
	void defaultProfileRoundtrip() throws ConfigDeserializationException {
		UUID playerUuid = UUID.randomUUID();
		LootLockPlayerData original = LootLockPlayerData.createDefault(playerUuid);

		String json = ConfigSerializer.serialize(original);
		LootLockPlayerData restored = ConfigSerializer.deserialize(json, playerUuid);

		assertEquals(original.getSchemaVersion(), restored.getSchemaVersion());
		assertEquals(original.getPlayerUuid(), restored.getPlayerUuid());
		assertEquals(original.getActiveProfileId(), restored.getActiveProfileId());
		assertEquals(original.getRevision(), restored.getRevision());
		assertEquals(original.getProfiles().size(), restored.getProfiles().size());

		LootLockProfile originalProfile = original.getActiveProfile().orElseThrow();
		LootLockProfile restoredProfile = restored.getActiveProfile().orElseThrow();
		assertEquals(originalProfile.getId(), restoredProfile.getId());
		assertEquals(originalProfile.getName(), restoredProfile.getName());
		assertEquals(originalProfile.getMode(), restoredProfile.getMode());
		assertEquals(originalProfile.getRejectedItemAction(), restoredProfile.getRejectedItemAction());
		assertEquals(originalProfile.isEnabled(), restoredProfile.isEnabled());
		assertEquals(originalProfile.getRules().size(), restoredProfile.getRules().size());
	}

	@Test
	void multipleProfilesRoundtrip() throws ConfigDeserializationException {
		UUID playerUuid = UUID.randomUUID();
		LootLockPlayerData original = LootLockPlayerData.createDefault(playerUuid);

		UUID miningId = UUID.randomUUID();
		LootLockProfile mining = new LootLockProfile(
			miningId,
			"Mining",
			FilterMode.DENYLIST,
			RejectedItemAction.DELETE,
			true,
			List.of(
				new RuleEntry("minecraft:cobblestone"),
				new RuleEntry("minecraft:granite"),
				new RuleEntry("minecraft:diorite"),
				new RuleEntry("minecraft:andesite")
			)
		);

		UUID farmingId = UUID.randomUUID();
		LootLockProfile farming = new LootLockProfile(
			farmingId,
			"Farming",
			FilterMode.DENYLIST,
			RejectedItemAction.LEAVE_ON_GROUND,
			true,
			List.of(
				new RuleEntry("minecraft:wheat_seeds"),
				new RuleEntry("minecraft:egg")
			)
		);

		UUID mobFarmId = UUID.randomUUID();
		LootLockProfile mobFarm = new LootLockProfile(
			mobFarmId,
			"Mob Farm",
			FilterMode.ALLOWLIST,
			RejectedItemAction.LEAVE_ON_GROUND,
			true,
			List.of(new RuleEntry("minecraft:gunpowder"))
		);

		original.setProfiles(List.of(mining, farming, mobFarm));
		original.setActiveProfileId(farmingId);

		String json = ConfigSerializer.serialize(original);
		LootLockPlayerData restored = ConfigSerializer.deserialize(json, playerUuid);

		assertEquals(3, restored.getProfiles().size());
		assertEquals(farmingId, restored.getActiveProfileId());

		LootLockProfile restoredMining = restored.getProfiles().stream()
			.filter(p -> p.getId().equals(miningId))
			.findFirst().orElseThrow();
		assertEquals("Mining", restoredMining.getName());
		assertEquals(FilterMode.DENYLIST, restoredMining.getMode());
		assertEquals(RejectedItemAction.DELETE, restoredMining.getRejectedItemAction());
		assertEquals(4, restoredMining.getRules().size());

		LootLockProfile restoredFarming = restored.getProfiles().stream()
			.filter(p -> p.getId().equals(farmingId))
			.findFirst().orElseThrow();
		assertEquals("Farming", restoredFarming.getName());
		assertEquals(2, restoredFarming.getRules().size());

		LootLockProfile restoredMobFarm = restored.getProfiles().stream()
			.filter(p -> p.getId().equals(mobFarmId))
			.findFirst().orElseThrow();
		assertEquals("Mob Farm", restoredMobFarm.getName());
		assertEquals(FilterMode.ALLOWLIST, restoredMobFarm.getMode());
		assertEquals(1, restoredMobFarm.getRules().size());
	}

	@Test
	void unknownItemIdIsRetained() throws ConfigDeserializationException {
		UUID playerUuid = UUID.randomUUID();
		LootLockPlayerData original = LootLockPlayerData.createDefault(playerUuid);

		UUID profileId = UUID.randomUUID();
		LootLockProfile profile = new LootLockProfile(
			profileId,
			"Test",
			FilterMode.DENYLIST,
			RejectedItemAction.LEAVE_ON_GROUND,
			true,
			List.of(
				new RuleEntry("minecraft:dirt"),
				new RuleEntry("oldmod:removed_item"),
				new RuleEntry("anothermod:unknown_item")
			)
		);
		original.setProfiles(List.of(profile));

		String json = ConfigSerializer.serialize(original);
		LootLockPlayerData restored = ConfigSerializer.deserialize(json, playerUuid);

		LootLockProfile restoredProfile = restored.getProfiles().get(0);
		assertEquals(3, restoredProfile.getRules().size());

		List<String> itemIds = restoredProfile.getRules().stream()
			.map(RuleEntry::itemId)
			.toList();
		assertTrue(itemIds.contains("minecraft:dirt"));
		assertTrue(itemIds.contains("oldmod:removed_item"));
		assertTrue(itemIds.contains("anothermod:unknown_item"));
	}

	@Test
	void schemaVersionIsPreserved() throws ConfigDeserializationException {
		UUID playerUuid = UUID.randomUUID();
		LootLockPlayerData original = LootLockPlayerData.createDefault(playerUuid);
		original.setSchemaVersion(1);

		String json = ConfigSerializer.serialize(original);
		LootLockPlayerData restored = ConfigSerializer.deserialize(json, playerUuid);

		assertEquals(1, restored.getSchemaVersion());
	}

	@Test
	void revisionIsPreserved() throws ConfigDeserializationException {
		UUID playerUuid = UUID.randomUUID();
		LootLockPlayerData original = LootLockPlayerData.createDefault(playerUuid);
		original.setRevision(42);

		String json = ConfigSerializer.serialize(original);
		LootLockPlayerData restored = ConfigSerializer.deserialize(json, playerUuid);

		assertEquals(42, restored.getRevision());
	}

	@Test
	void deserializeWithWrongUuidFails() {
		UUID playerUuid = UUID.randomUUID();
		UUID wrongUuid = UUID.randomUUID();
		LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);

		String json = ConfigSerializer.serialize(data);

		assertThrows(ConfigDeserializationException.class, () ->
			ConfigSerializer.deserialize(json, wrongUuid)
		);
	}

	@Test
	void deserializeEmptyJsonFails() {
		assertThrows(ConfigDeserializationException.class, () ->
			ConfigSerializer.deserialize("", UUID.randomUUID())
		);
	}

	@Test
	void deserializeInvalidJsonFails() {
		assertThrows(ConfigDeserializationException.class, () ->
			ConfigSerializer.deserialize("{not valid json}", UUID.randomUUID())
		);
	}

	@Test
	void deserializeNullJsonFails() {
		assertThrows(ConfigDeserializationException.class, () ->
			ConfigSerializer.deserialize("null", UUID.randomUUID())
		);
	}
}
