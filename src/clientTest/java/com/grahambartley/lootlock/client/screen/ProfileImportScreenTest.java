package com.grahambartley.lootlock.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProfileImportScreenTest {

  @ParameterizedTest(name = "width {0} -> buttons start at {1}")
  @CsvSource({"854, 273", "427, 59", "308, 0"})
  void footerButtonsAreCentredAsAPair(int screenWidth, int expectedLeft) {
    assertEquals(expectedLeft, ProfileImportScreen.footerButtonsLeft(screenWidth));
  }

  @ParameterizedTest(name = "height {0} -> content starts at {1}")
  @CsvSource({"480, 202", "240, 82", "60, 33"})
  void contentIsCentredButNeverOverlapsTheTitle(int screenHeight, int expectedTop) {
    assertEquals(expectedTop, ProfileImportScreen.contentTop(screenHeight));
  }
}
