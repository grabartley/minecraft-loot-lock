package com.grahambartley.lootlock.network;

public final class LootLockNetworking {
  private LootLockNetworking() {}

  public static void initializeNetworking() {
    LootLockPayloads.registerTypes();
    ClientToServerPackets.register();
  }
}
