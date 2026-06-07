package com.grahambartley.data;

public enum FilterMode {
  DENYLIST,
  ALLOWLIST;

  public String displayName() {
    if (this == ALLOWLIST) {
      return "Allowlist";
    }
    return "Denylist";
  }
}
