package com.grahambartley.lootlock.client.screen.inventory;

import com.grahambartley.lootlock.client.LootLockClient;
import com.grahambartley.lootlock.client.network.ClientMutationSync;
import com.grahambartley.lootlock.client.screen.RuleListController;
import com.grahambartley.lootlock.client.state.ClientDraftProfile;
import com.grahambartley.lootlock.client.state.ClientLootLockState;
import com.grahambartley.lootlock.client.state.ClientLootLockState.ClientDraftSaveRequest;
import com.grahambartley.lootlock.data.LootLockPlayerData;
import com.grahambartley.lootlock.data.RuleEntry;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class RuleMutations {
  private RuleMutations() {}

  public static boolean addToActiveProfile(Collection<String> itemIds) {
    if (itemIds == null || itemIds.isEmpty()) {
      return false;
    }
    ClientLootLockState state = LootLockClient.getState();
    Optional<LootLockPlayerData> snapshotOptional = state.getSnapshot();
    if (snapshotOptional.isEmpty()) {
      return false;
    }
    LootLockPlayerData snapshot = snapshotOptional.get();
    Optional<ClientDraftProfile> draftOptional = state.beginDraft(snapshot.getActiveProfileId());
    if (draftOptional.isEmpty()) {
      return false;
    }
    ClientDraftProfile draft = draftOptional.get();
    List<RuleEntry> next =
        RuleListController.withRulesAdded(draft.getDraft().getRules(), List.copyOf(itemIds));
    draft.setRules(next);
    Optional<ClientDraftSaveRequest> saveRequest = state.buildSaveRequest();
    saveRequest.ifPresent(ClientMutationSync::sendSaveRequest);
    return saveRequest.isPresent();
  }

  public static boolean removeFromActiveProfile(String itemId) {
    if (itemId == null || itemId.isBlank()) {
      return false;
    }
    ClientLootLockState state = LootLockClient.getState();
    Optional<LootLockPlayerData> snapshotOptional = state.getSnapshot();
    if (snapshotOptional.isEmpty()) {
      return false;
    }
    LootLockPlayerData snapshot = snapshotOptional.get();
    Optional<ClientDraftProfile> draftOptional = state.beginDraft(snapshot.getActiveProfileId());
    if (draftOptional.isEmpty()) {
      return false;
    }
    ClientDraftProfile draft = draftOptional.get();
    List<RuleEntry> next = RuleListController.withRuleRemoved(draft.getDraft().getRules(), itemId);
    draft.setRules(next);
    Optional<ClientDraftSaveRequest> saveRequest = state.buildSaveRequest();
    saveRequest.ifPresent(ClientMutationSync::sendSaveRequest);
    return saveRequest.isPresent();
  }

  public static boolean clearActiveProfile() {
    ClientLootLockState state = LootLockClient.getState();
    Optional<LootLockPlayerData> snapshotOptional = state.getSnapshot();
    if (snapshotOptional.isEmpty()) {
      return false;
    }
    LootLockPlayerData snapshot = snapshotOptional.get();
    Optional<ClientDraftProfile> draftOptional = state.beginDraft(snapshot.getActiveProfileId());
    if (draftOptional.isEmpty()) {
      return false;
    }
    ClientDraftProfile draft = draftOptional.get();
    draft.setRules(List.of());
    Optional<ClientDraftSaveRequest> saveRequest = state.buildSaveRequest();
    saveRequest.ifPresent(ClientMutationSync::sendSaveRequest);
    return saveRequest.isPresent();
  }
}
