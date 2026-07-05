package com.grahambartley.lootlock.network;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.grahambartley.lootlock.data.LootLockProfile;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ClientMutationSyncTest {

  static Stream<Arguments> nullArgumentCases() {
    BooleanSupplier saveWithNullRequest = () -> ClientMutationSync.sendSaveRequest(null);
    BooleanSupplier activateWithNullProfileId =
        () -> ClientMutationSync.sendActivateRequest(1L, null);
    BooleanSupplier createWithNullName =
        () -> ClientMutationSync.sendCreateRequest(1L, null, LootLockProfile.createDefault());
    BooleanSupplier deleteWithNullProfileId = () -> ClientMutationSync.sendDeleteRequest(1L, null);
    return Stream.of(
        Arguments.of("sendSaveRequest(null)", saveWithNullRequest),
        Arguments.of("sendActivateRequest(_, null)", activateWithNullProfileId),
        Arguments.of("sendCreateRequest(_, null, _)", createWithNullName),
        Arguments.of("sendDeleteRequest(_, null)", deleteWithNullProfileId));
  }

  @ParameterizedTest(name = "{0} returns false")
  @MethodSource("nullArgumentCases")
  void rejectsNullArguments(String label, BooleanSupplier call) {
    assertFalse(call.getAsBoolean());
  }
}
