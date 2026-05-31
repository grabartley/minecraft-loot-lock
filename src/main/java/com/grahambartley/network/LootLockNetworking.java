package com.grahambartley.network;

public final class LootLockNetworking {
  private LootLockNetworking() {}

  public static void initializeServer() {
    ClientToServerPackets.register();
  }
}
