package com.grahambartley.lootlock.network;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LootLockNetworkingTest {

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @Test
  void initializeRegistersAServerReceiverForEveryClientToServerPacket() {
    LootLockNetworking.initializeNetworking();

    Set<Identifier> receivers = ServerPlayNetworking.getGlobalReceivers();
    Set<Identifier> clientToServerIds = packetIdsEndingWith("_C2S");
    assertTrue(
        receivers.containsAll(clientToServerIds),
        "missing receivers for " + clientToServerIds + " in " + receivers);
  }

  @Test
  void initializeCanRunMoreThanOnce() {
    LootLockNetworking.initializeNetworking();

    assertDoesNotThrow(LootLockNetworking::initializeNetworking);
  }

  private static Set<Identifier> packetIdsEndingWith(String suffix) {
    return Arrays.stream(PacketIds.class.getDeclaredFields())
        .filter(field -> Modifier.isStatic(field.getModifiers()))
        .filter(field -> field.getName().endsWith(suffix))
        .map(LootLockNetworkingTest::identifierValue)
        .collect(Collectors.toSet());
  }

  private static Identifier identifierValue(Field field) {
    try {
      return (Identifier) field.get(null);
    } catch (IllegalAccessException ex) {
      throw new AssertionError(ex);
    }
  }
}
