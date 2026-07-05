package com.grahambartley.lootlock.client.state;

import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.data.RuleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

// Mutable draft is intentionally exposed to UI callers for low-friction form binding.
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
    String before = draft.getName();
    draft.setName(name);
    if (!Objects.equals(before, draft.getName())) {
      dirty = true;
    }
  }

  public void setMode(FilterMode mode) {
    FilterMode before = draft.getMode();
    draft.setMode(mode);
    if (before != draft.getMode()) {
      dirty = true;
    }
  }

  public void setRejectedItemAction(RejectedItemAction action) {
    RejectedItemAction before = draft.getRejectedItemAction();
    draft.setRejectedItemAction(action);
    if (before != draft.getRejectedItemAction()) {
      dirty = true;
    }
  }

  public void setEnabled(boolean enabled) {
    boolean before = draft.isEnabled();
    draft.setEnabled(enabled);
    if (before != draft.isEnabled()) {
      dirty = true;
    }
  }

  public void setColor(int color) {
    int before = draft.getColor();
    draft.setColor(color);
    if (before != draft.getColor()) {
      dirty = true;
    }
  }

  public void setRules(List<RuleEntry> rules) {
    List<RuleEntry> before = new ArrayList<>(draft.getRules());
    List<RuleEntry> copiedRules = new ArrayList<>();
    if (rules != null) {
      for (RuleEntry rule : rules) {
        copiedRules.add(new RuleEntry(rule.itemId()));
      }
    }
    draft.setRules(copiedRules);
    if (!before.equals(draft.getRules())) {
      dirty = true;
    }
  }

  private static LootLockProfile cloneProfile(LootLockProfile source) {
    if (source == null) {
      throw new IllegalArgumentException("source must not be null");
    }

    List<RuleEntry> copiedRules = new ArrayList<>();
    if (source.getRules() != null) {
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
        source.getColor(),
        copiedRules);
  }
}
