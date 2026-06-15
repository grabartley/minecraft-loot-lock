package com.grahambartley.data;

public record RuleEntry(String itemId) {
  public static final String TAG_PREFIX = "#";

  public boolean isTag() {
    return itemId != null && itemId.startsWith(TAG_PREFIX);
  }

  public String tagPath() {
    return isTag() ? itemId.substring(TAG_PREFIX.length()) : null;
  }
}
