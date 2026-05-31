package com.grahambartley.network;

import com.grahambartley.LootLockConstants;
import net.minecraft.util.Identifier;

public final class PacketIds {
  public static final Identifier HELLO_C2S = id("hello_c2s");
  public static final Identifier SYNC_PLAYER_DATA_S2C = id("sync_player_data_s2c");
  public static final Identifier REQUEST_SYNC_C2S = id("request_sync_c2s");
  public static final Identifier SERVER_CAPABILITIES_S2C = id("server_capabilities_s2c");

  private PacketIds() {}

  private static Identifier id(String path) {
    return new Identifier(LootLockConstants.MOD_ID, path);
  }
}
