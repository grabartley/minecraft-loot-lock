package com.grahambartley.lootlock.network;

public final class LootLockNetworking {
  private LootLockNetworking() {}

  // Called from the common ModInitializer. Payload type registration must run on both the client
  // and dedicated-server JVMs so each side encodes/decodes the same byte layout; do not move this
  // behind a server-only lifecycle hook.
  public static void initializeNetworking() {
    LootLockPayloads.registerTypes();
    ClientToServerPackets.register();
  }
}
