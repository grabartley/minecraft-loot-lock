package com.grahambartley.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.network.PacketByteBuf;
import org.junit.jupiter.api.Test;

class ClientToServerPacketsTest {
  @Test
  void helloPayloadRoundTripsVersionAndSchema() {
    PacketByteBuf buf = ClientToServerPackets.writeHelloPayload("1.2.3", 7);

    ClientToServerPackets.HelloPayload payload = ClientToServerPackets.readHelloPayload(buf);

    assertEquals("1.2.3", payload.clientVersion());
    assertEquals(7, payload.schemaVersion());
  }
}
