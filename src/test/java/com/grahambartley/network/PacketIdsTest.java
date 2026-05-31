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
    assertEquals("loot-lock:update_profile_c2s", PacketIds.UPDATE_PROFILE_C2S.toString());
    assertEquals("loot-lock:activate_profile_c2s", PacketIds.ACTIVATE_PROFILE_C2S.toString());
    assertEquals("loot-lock:create_profile_c2s", PacketIds.CREATE_PROFILE_C2S.toString());
    assertEquals("loot-lock:delete_profile_c2s", PacketIds.DELETE_PROFILE_C2S.toString());
    assertEquals(
        "loot-lock:update_server_policy_c2s", PacketIds.UPDATE_SERVER_POLICY_C2S.toString());
    assertEquals("loot-lock:blocked_notice_s2c", PacketIds.BLOCKED_NOTICE_S2C.toString());
  }

  @Test
  void allPacketIdsUseLootLockNamespace() {
    assertTrue(
        Stream.of(
                PacketIds.HELLO_C2S,
                PacketIds.SYNC_PLAYER_DATA_S2C,
                PacketIds.REQUEST_SYNC_C2S,
                PacketIds.SERVER_CAPABILITIES_S2C,
                PacketIds.UPDATE_PROFILE_C2S,
                PacketIds.ACTIVATE_PROFILE_C2S,
                PacketIds.CREATE_PROFILE_C2S,
                PacketIds.DELETE_PROFILE_C2S,
                PacketIds.UPDATE_SERVER_POLICY_C2S,
                PacketIds.BLOCKED_NOTICE_S2C)
            .allMatch(id -> "loot-lock".equals(id.getNamespace())));
  }
}
