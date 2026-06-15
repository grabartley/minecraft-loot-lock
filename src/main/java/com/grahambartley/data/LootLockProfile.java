package com.grahambartley.data;

import com.grahambartley.api.PickupDecision;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.util.Identifier;

public final class LootLockProfile {
  private UUID id;
  private String name;
  private FilterMode mode;
  private RejectedItemAction rejectedItemAction;
  private boolean enabled;
  private int color;
  private List<RuleEntry> rules;

  private transient RuleSet compiledRuleSet;

  public LootLockProfile() {
    this(
        UUID.randomUUID(),
        "Default",
        FilterMode.DENYLIST,
        RejectedItemAction.LEAVE_ON_GROUND,
        true,
        new ArrayList<>());
  }

  public LootLockProfile(
      UUID id,
      String name,
      FilterMode mode,
      RejectedItemAction rejectedItemAction,
      boolean enabled,
      List<RuleEntry> rules) {
    this(id, name, mode, rejectedItemAction, enabled, 0, rules);
  }

  public LootLockProfile(
      UUID id,
      String name,
      FilterMode mode,
      RejectedItemAction rejectedItemAction,
      boolean enabled,
      int color,
      List<RuleEntry> rules) {
    this.id = id == null ? UUID.randomUUID() : id;
    this.name = (name == null || name.isBlank()) ? "Default" : name;
    this.mode = mode == null ? FilterMode.DENYLIST : mode;
    this.rejectedItemAction =
        rejectedItemAction == null ? RejectedItemAction.LEAVE_ON_GROUND : rejectedItemAction;
    this.enabled = enabled;
    this.color = color;
    this.rules = rules == null ? new ArrayList<>() : new ArrayList<>(rules);
    compileRules();
  }

  public static LootLockProfile createDefault() {
    return new LootLockProfile();
  }

  public void compileRules() {
    compiledRuleSet = RuleSet.fromRuleEntries(rules);
  }

  public PickupDecision evaluate(Identifier itemId) {
    if (!enabled) {
      return PickupDecision.ALLOW;
    }

    boolean matched = getCompiledRuleSet().contains(itemId);
    boolean reject =
        switch (mode) {
          case DENYLIST -> matched;
          case ALLOWLIST -> !matched;
        };

    if (!reject) {
      return PickupDecision.ALLOW;
    }

    return rejectedItemAction == RejectedItemAction.DELETE
        ? PickupDecision.REJECT_DELETE
        : PickupDecision.REJECT_LEAVE;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = Objects.requireNonNullElseGet(id, UUID::randomUUID);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = (name == null || name.isBlank()) ? "Default" : name;
  }

  public FilterMode getMode() {
    return mode;
  }

  public void setMode(FilterMode mode) {
    this.mode = mode == null ? FilterMode.DENYLIST : mode;
  }

  public RejectedItemAction getRejectedItemAction() {
    return rejectedItemAction;
  }

  public void setRejectedItemAction(RejectedItemAction rejectedItemAction) {
    this.rejectedItemAction =
        rejectedItemAction == null ? RejectedItemAction.LEAVE_ON_GROUND : rejectedItemAction;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /** Persisted ARGB profile colour. {@code 0} means unset; renderers fall back to the default. */
  public int getColor() {
    return color;
  }

  public void setColor(int color) {
    this.color = color;
  }

  public List<RuleEntry> getRules() {
    return Collections.unmodifiableList(rules);
  }

  public void setRules(List<RuleEntry> rules) {
    this.rules = rules == null ? new ArrayList<>() : new ArrayList<>(rules);
    compileRules();
  }

  public RuleSet getCompiledRuleSet() {
    if (compiledRuleSet == null) {
      compileRules();
    }
    return compiledRuleSet;
  }
}
