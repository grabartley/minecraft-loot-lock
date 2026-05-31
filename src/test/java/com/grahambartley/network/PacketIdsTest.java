package com.grahambartley.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PacketIdsTest {
  @Test
  void packetIdsMatchDesignChannels() {
    assertEquals("loot-lock:hello_c2s", PacketIds.HELLO_C2S.toString());
    assertEquals("loot-lock:sync_player_data_s2c", PacketIds.SYNC_PLAYER_DATA_S2C.toString());
    assertEquals("loot-lock:request_sync_c2s", PacketIds.REQUEST_SYNC_C2S.toString());
    assertEquals("loot-lock:server_capabilities_s2c", PacketIds.SERVER_CAPABILITIES_S2C.toString());
  }

  @Test
  void allPacketIdsUseLootLockNamespace() {
    assertTrue(
        Stream.of(
                PacketIds.HELLO_C2S,
                PacketIds.SYNC_PLAYER_DATA_S2C,
                PacketIds.REQUEST_SYNC_C2S,
                PacketIds.SERVER_CAPABILITIES_S2C)
            .allMatch(id -> "loot-lock".equals(id.getNamespace())));
  }
}
