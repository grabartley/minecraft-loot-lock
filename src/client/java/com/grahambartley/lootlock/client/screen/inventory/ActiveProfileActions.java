package com.grahambartley.lootlock.client.screen.inventory;

import com.grahambartley.lootlock.client.LootLockClient;
import com.grahambartley.lootlock.client.network.ClientMutationSync;
import com.grahambartley.lootlock.client.state.ClientDraftProfile;
import com.grahambartley.lootlock.client.state.ClientLootLockState;
import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockPlayerData;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.text.LootLockLang;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

final class ActiveProfileActions {
  private ActiveProfileActions() {}

  static Optional<LootLockProfile> activeProfile() {
    return LootLockClient.getState().getSnapshot().flatMap(LootLockPlayerData::getActiveProfile);
  }

  static boolean globallyEnabled() {
    return LootLockClient.getState()
        .getSnapshot()
        .map(LootLockPlayerData::isGloballyEnabled)
        .orElse(true);
  }

  static void cycle(int direction) {
    Optional<LootLockPlayerData> snapshotOptional = LootLockClient.getState().getSnapshot();
    if (snapshotOptional.isEmpty()) {
      return;
    }
    LootLockPlayerData snapshot = snapshotOptional.get();
    List<LootLockProfile> profiles = snapshot.getProfiles();
    if (profiles.isEmpty()) {
      return;
    }
    int currentIndex = 0;
    for (int i = 0; i < profiles.size(); i++) {
      if (profiles.get(i).getId().equals(snapshot.getActiveProfileId())) {
        currentIndex = i;
        break;
      }
    }
    int nextIndex = (currentIndex + direction + profiles.size()) % profiles.size();
    activate(profiles.get(nextIndex).getId());
  }

  static void activate(UUID profileId) {
    LootLockClient.getState()
        .getSnapshot()
        .ifPresent(
            snapshot -> ClientMutationSync.sendActivateRequest(snapshot.getRevision(), profileId));
  }

  static void setMode(FilterMode mode) {
    mutate(draft -> draft.setMode(mode));
  }

  static void setAction(RejectedItemAction action) {
    if (action == RejectedItemAction.DELETE
        && !LootLockClient.getState().isAllowDeleteRejectedItems()) {
      return;
    }
    if (action == RejectedItemAction.DELETE && shouldConfirmEnableDelete()) {
      openDeleteConfirmScreen();
      return;
    }
    mutate(draft -> draft.setRejectedItemAction(action));
  }

  private static void openDeleteConfirmScreen() {
    MinecraftClient client = MinecraftClient.getInstance();
    Screen current = client == null ? null : client.currentScreen;
    if (client == null || current == null) {
      return;
    }
    client.setScreen(
        new ConfirmScreen(
            confirmed -> {
              if (confirmed) {
                mutate(draft -> draft.setRejectedItemAction(RejectedItemAction.DELETE));
              }
              client.setScreen(current);
            },
            Text.translatable(LootLockLang.CONFIRM_ENABLE_DELETE_TITLE),
            Text.translatable(LootLockLang.CONFIRM_ENABLE_DELETE_BODY)));
  }

  private static boolean shouldConfirmEnableDelete() {
    Optional<LootLockProfile> active = activeProfile();
    if (active.isEmpty() || active.get().getRejectedItemAction() == RejectedItemAction.DELETE) {
      return false;
    }
    return LootLockClient.getClientSettingsManager() != null
        && LootLockClient.getClientSettingsManager()
            .getSettingsCopy()
            .isConfirmBeforeEnablingDelete();
  }

  private static void mutate(Consumer<ClientDraftProfile> mutator) {
    ClientLootLockState state = LootLockClient.getState();
    state
        .getSnapshot()
        .flatMap(data -> state.beginDraft(data.getActiveProfileId()))
        .ifPresent(
            draft -> {
              mutator.accept(draft);
              state.buildSaveRequest().ifPresent(ClientMutationSync::sendSaveRequest);
            });
  }
}
