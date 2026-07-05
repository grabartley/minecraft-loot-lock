package com.grahambartley.lootlock.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.data.RuleEntry;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import java.util.UUID;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.network.PacketByteBuf;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LootLockPayloadsTest {

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @Test
  void profileCodecRoundTripsThroughBuffer() {
    LootLockProfile profile =
        new LootLockProfile(
            UUID.randomUUID(),
            "Farming",
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            0xFFAA00,
            List.of(new RuleEntry("minecraft:wheat_seeds"), new RuleEntry("#minecraft:flowers")));
    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());

    LootLockPayloads.PROFILE_CODEC.encode(buf, profile);
    LootLockProfile decoded = LootLockPayloads.PROFILE_CODEC.decode(buf);

    assertEquals(profile.getId(), decoded.getId());
    assertEquals(profile.getName(), decoded.getName());
    assertEquals(profile.getMode(), decoded.getMode());
    assertEquals(profile.getRejectedItemAction(), decoded.getRejectedItemAction());
    assertEquals(profile.isEnabled(), decoded.isEnabled());
    assertEquals(profile.getColor(), decoded.getColor());
    assertEquals(profile.getRules(), decoded.getRules());
  }

  @ParameterizedTest(name = "rule count {0} is rejected at decode")
  @ValueSource(ints = {-1, PacketLimits.MAX_RULES_PER_PROFILE + 1, Integer.MAX_VALUE})
  void profileCodecRejectsOutOfBoundsRuleCount(int claimedRuleCount) {
    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
    buf.writeUuid(UUID.randomUUID());
    buf.writeString("Farming", PacketLimits.MAX_PROFILE_NAME_LENGTH);
    buf.writeEnumConstant(FilterMode.DENYLIST);
    buf.writeEnumConstant(RejectedItemAction.LEAVE_ON_GROUND);
    buf.writeBoolean(true);
    buf.writeInt(0xFFAA00);
    buf.writeVarInt(claimedRuleCount);

    assertThrows(DecoderException.class, () -> LootLockPayloads.PROFILE_CODEC.decode(buf));
  }
}
