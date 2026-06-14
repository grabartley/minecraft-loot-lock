package com.grahambartley.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import net.minecraft.util.Identifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PacketIdsTest {

  static Stream<Arguments> packetIds() {
    return Stream.of(
        Arguments.of(PacketIds.HELLO_C2S, "loot-lock:hello_c2s"),
        Arguments.of(PacketIds.SYNC_PLAYER_DATA_S2C, "loot-lock:sync_player_data_s2c"),
        Arguments.of(PacketIds.REQUEST_SYNC_C2S, "loot-lock:request_sync_c2s"),
        Arguments.of(PacketIds.SERVER_CAPABILITIES_S2C, "loot-lock:server_capabilities_s2c"),
        Arguments.of(PacketIds.UPDATE_PROFILE_C2S, "loot-lock:update_profile_c2s"),
        Arguments.of(PacketIds.ACTIVATE_PROFILE_C2S, "loot-lock:activate_profile_c2s"),
        Arguments.of(PacketIds.CREATE_PROFILE_C2S, "loot-lock:create_profile_c2s"),
        Arguments.of(PacketIds.DELETE_PROFILE_C2S, "loot-lock:delete_profile_c2s"),
        Arguments.of(PacketIds.UPDATE_SERVER_POLICY_C2S, "loot-lock:update_server_policy_c2s"),
        Arguments.of(PacketIds.UPDATE_GLOBAL_ENABLE_C2S, "loot-lock:update_global_enable_c2s"),
        Arguments.of(PacketIds.BLOCKED_NOTICE_S2C, "loot-lock:blocked_notice_s2c"));
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("packetIds")
  void packetIdMatchesDesignChannel(Identifier id, String expected) {
    assertEquals(expected, id.toString());
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("packetIds")
  void packetIdUsesLootLockNamespace(Identifier id, String expected) {
    assertEquals("loot-lock", id.getNamespace());
  }
}
