package com.grahambartley.lootlock.client.screen.inventory;

import com.grahambartley.lootlock.client.LootLockClient;
import com.grahambartley.lootlock.client.network.ClientMutationSync;
import com.grahambartley.lootlock.client.state.ClientLootLockState;
import com.grahambartley.lootlock.client.state.ClientLootLockState.ClientDraftSaveRequest;
import com.grahambartley.lootlock.data.LootLockPlayerData;
import com.grahambartley.lootlock.data.LootLockProfile;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class ProfileColors {
  private ProfileColors() {}

  static Consumer<ClientDraftSaveRequest> saveRequestDispatcher =
      ClientMutationSync::sendSaveRequest;

  public static int colorForProfile(LootLockProfile profile) {
    int color = profile.getColor();
    return color == 0 ? Palette.PROFILE_COLORS[0] : color;
  }

  static int nextProfileColor(int currentColor) {
    int[] palette = Palette.PROFILE_COLORS;
    int currentIndex = 0;
    for (int i = 0; i < palette.length; i++) {
      if (palette[i] == currentColor) {
        currentIndex = i;
        break;
      }
    }
    return palette[(currentIndex + 1) % palette.length];
  }

  static void cycleProfileColor(UUID profileId) {
    if (profileId == null) {
      return;
    }
    ClientLootLockState state = LootLockClient.getState();
    Optional<LootLockPlayerData> snapshotOptional = state.getSnapshot();
    if (snapshotOptional.isEmpty()) {
      return;
    }
    LootLockProfile profile = null;
    for (LootLockProfile candidate : snapshotOptional.get().getProfiles()) {
      if (profileId.equals(candidate.getId())) {
        profile = candidate;
        break;
      }
    }
    if (profile == null) {
      return;
    }
    int nextColor = nextProfileColor(profile.getColor());
    state
        .beginDraft(profileId)
        .ifPresent(
            draft -> {
              draft.setColor(nextColor);
              state.buildSaveRequest().ifPresent(saveRequestDispatcher);
            });
  }
}
