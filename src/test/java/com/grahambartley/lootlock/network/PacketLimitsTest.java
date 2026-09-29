package com.grahambartley.lootlock.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.data.RuleEntry;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.EncoderException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.network.PacketByteBuf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PacketLimitsTest {

  static Stream<Arguments> profilesAtTheLimit() {
    return Stream.of(
        Arguments.of("name", profile("n".repeat(PacketLimits.MAX_PROFILE_NAME_LENGTH), "a:b")),
        Arguments.of(
            "rule id", profile("p", "a:" + "b".repeat(PacketLimits.MAX_RULE_ID_LENGTH - 2))));
  }

  static Stream<Arguments> profilesOverTheLimit() {
    return Stream.of(
        Arguments.of("name", profile("n".repeat(PacketLimits.MAX_PROFILE_NAME_LENGTH + 1), "a:b")),
        Arguments.of(
            "rule id", profile("p", "a:" + "b".repeat(PacketLimits.MAX_RULE_ID_LENGTH - 1))));
  }

  @ParameterizedTest(name = "{0} at the limit round-trips")
  @MethodSource("profilesAtTheLimit")
  void valuesAtTheLimitCrossTheWire(String label, LootLockProfile profile) {
    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());

    LootLockPayloads.writeProfile(profile, buf);
    LootLockProfile read = LootLockPayloads.readProfile(buf);

    assertEquals(profile.getName(), read.getName());
    assertEquals(profile.getRules(), read.getRules());
  }

  @ParameterizedTest(name = "{0} over the limit is refused")
  @MethodSource("profilesOverTheLimit")
  void valuesOverTheLimitAreRefusedOnEncode(String label, LootLockProfile profile) {
    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());

    assertThrows(EncoderException.class, () -> LootLockPayloads.writeProfile(profile, buf));
  }

  private static LootLockProfile profile(String name, String ruleId) {
    return new LootLockProfile(
        UUID.randomUUID(),
        name,
        FilterMode.DENYLIST,
        RejectedItemAction.LEAVE_ON_GROUND,
        true,
        List.of(new RuleEntry(ruleId)));
  }
}
