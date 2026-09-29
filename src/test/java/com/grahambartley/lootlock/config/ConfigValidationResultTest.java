package com.grahambartley.lootlock.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ConfigValidationResultTest {

  static Stream<Arguments> factories() {
    Supplier<ConfigValidationResult> success = ConfigValidationResult::success;
    Supplier<ConfigValidationResult> singleFailure = () -> ConfigValidationResult.failure("bad");
    Supplier<ConfigValidationResult> listFailure =
        () -> ConfigValidationResult.failure(List.of("first", "second"));
    Supplier<ConfigValidationResult> nullErrors = () -> new ConfigValidationResult(true, null);
    return Stream.of(
        Arguments.of("success", success, true, List.of()),
        Arguments.of("failure(String)", singleFailure, false, List.of("bad")),
        Arguments.of("failure(List)", listFailure, false, List.of("first", "second")),
        Arguments.of("null errors", nullErrors, true, List.of()));
  }

  @ParameterizedTest(name = "{0} -> valid={2} errors={3}")
  @MethodSource("factories")
  void factoriesProduceExpectedState(
      String label,
      Supplier<ConfigValidationResult> factory,
      boolean expectedValid,
      List<String> expectedErrors) {
    ConfigValidationResult result = factory.get();

    assertEquals(expectedValid, result.valid());
    assertEquals(expectedErrors, result.errors());
  }

  @ParameterizedTest(name = "{0} exposes read-only errors")
  @MethodSource("factories")
  void errorsAreReadOnly(
      String label,
      Supplier<ConfigValidationResult> factory,
      boolean expectedValid,
      List<String> expectedErrors) {
    List<String> errors = factory.get().errors();

    assertThrows(UnsupportedOperationException.class, () -> errors.add("extra"));
  }

  @Test
  void successIsSharedInstance() {
    assertSame(ConfigValidationResult.success(), ConfigValidationResult.success());
  }

  @Test
  void failureResultRejectsMutationWithoutTouchingSourceList() {
    List<String> source = new ArrayList<>(List.of("first"));

    ConfigValidationResult result = ConfigValidationResult.failure(source);

    assertThrows(UnsupportedOperationException.class, () -> result.errors().clear());
    assertEquals(List.of("first"), source);
  }
}
