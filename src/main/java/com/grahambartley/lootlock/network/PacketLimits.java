package com.grahambartley.lootlock.network;

public final class PacketLimits {
  public static final int MAX_PROFILE_NAME_LENGTH = 64;
  public static final int MAX_RULE_ID_LENGTH = 256;
  public static final int MAX_PROFILES = 9;
  public static final int MAX_RULES_PER_PROFILE = 1024;
  public static final int MAX_SHARE_CODE_LENGTH = 4096;

  private PacketLimits() {}
}
