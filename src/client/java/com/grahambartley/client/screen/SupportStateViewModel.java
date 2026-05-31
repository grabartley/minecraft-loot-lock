package com.grahambartley.client.screen;

public record SupportStateViewModel(String message, int color, boolean editable) {
  public static SupportStateViewModel fromState(
      boolean supportsLootLock, boolean synced, boolean canEdit) {
    if (!supportsLootLock) {
      return new SupportStateViewModel(
          "Unsupported server, LootLock is server-side only here.", 0xE06666, false);
    }
    if (!synced) {
      return new SupportStateViewModel("Waiting for LootLock sync...", 0xE0AA4A, false);
    }
    if (!canEdit) {
      return new SupportStateViewModel(
          "Read-only, server disabled client editing.", 0xE0AA4A, false);
    }
    return new SupportStateViewModel("Supported, full editor available.", 0x84D18A, true);
  }
}
