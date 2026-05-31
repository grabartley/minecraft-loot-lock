package com.grahambartley.client.state;

import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.data.RuleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ClientDraftProfile {
  private final UUID profileId;
  private final long baseRevision;
  private final LootLockProfile draft;
  private boolean dirty;

  public ClientDraftProfile(UUID profileId, long baseRevision, LootLockProfile draft) {
    this.profileId = profileId;
    this.baseRevision = baseRevision;
    this.draft = cloneProfile(draft);
    this.dirty = false;
  }

  public UUID getProfileId() {
    return profileId;
  }

  public long getBaseRevision() {
    return baseRevision;
  }

  public LootLockProfile getDraft() {
    return draft;
  }

  public boolean isDirty() {
    return dirty;
  }

  public void setName(String name) {
    draft.setName(name);
    dirty = true;
  }

  public void setMode(FilterMode mode) {
    draft.setMode(mode);
    dirty = true;
  }

  public void setRejectedItemAction(RejectedItemAction action) {
    draft.setRejectedItemAction(action);
    dirty = true;
  }

  public void setEnabled(boolean enabled) {
    draft.setEnabled(enabled);
    dirty = true;
  }

  public void setRules(List<RuleEntry> rules) {
    draft.setRules(rules == null ? List.of() : rules);
    dirty = true;
  }

  private static LootLockProfile cloneProfile(LootLockProfile source) {
    List<RuleEntry> copiedRules = new ArrayList<>();
    if (source != null && source.getRules() != null) {
      for (RuleEntry rule : source.getRules()) {
        copiedRules.add(new RuleEntry(rule.itemId()));
      }
    }

    return new LootLockProfile(
        source.getId(),
        source.getName(),
        source.getMode(),
        source.getRejectedItemAction(),
        source.isEnabled(),
        copiedRules);
  }
}
