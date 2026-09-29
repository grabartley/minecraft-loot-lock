package com.grahambartley.lootlock.command;

import com.grahambartley.lootlock.data.RuleEntry;
import com.grahambartley.lootlock.text.LootLockLang;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

final class RuleCommandHandlers {
  private RuleCommandHandlers() {}

  static int handleRuleAdd(
      ServerCommandSource source, CommandStateContext state, Identifier itemId) {
    if (!Registries.ITEM.containsId(itemId)) {
      source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_UNKNOWN_ITEM, itemId));
      return 0;
    }

    String token = itemId.toString();
    if (!CommandProfileEdits.addRuleToProfile(state.profile(), token)) {
      source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_RULE_EXISTS, token));
      return 0;
    }

    state.markDirty(source);
    Text message =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_RULE_ADD_SELF, token)
            : Text.translatable(LootLockLang.COMMAND_RULE_ADD_TARGET, token, state.displayName());
    source.sendFeedback(() -> message, false);
    state.syncIfOnline();
    return 1;
  }

  static int handleRuleRemove(
      ServerCommandSource source, CommandStateContext state, Identifier itemId) {
    String token = itemId.toString();
    if (!CommandProfileEdits.removeRuleFromProfile(state.profile(), token)) {
      source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_RULE_NOT_FOUND, token));
      return 0;
    }

    state.markDirty(source);
    Text message =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_RULE_REMOVE_SELF, token)
            : Text.translatable(
                LootLockLang.COMMAND_RULE_REMOVE_TARGET, token, state.displayName());
    source.sendFeedback(() -> message, false);
    state.syncIfOnline();
    return 1;
  }

  static int handleRuleAddTag(
      ServerCommandSource source, CommandStateContext state, Identifier tagId) {
    String token = RuleEntry.TAG_PREFIX + tagId;
    if (!CommandProfileEdits.addRuleToProfile(state.profile(), token)) {
      source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_RULE_EXISTS, token));
      return 0;
    }
    state.markDirty(source);
    Text message =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_RULE_ADD_SELF, token)
            : Text.translatable(LootLockLang.COMMAND_RULE_ADD_TARGET, token, state.displayName());
    source.sendFeedback(() -> message, false);
    if (!tagExists(tagId)) {
      source.sendFeedback(
          () -> Text.translatable(LootLockLang.COMMAND_RULE_TAG_UNKNOWN_WARNING, token), false);
    }
    state.syncIfOnline();
    return 1;
  }

  static int handleRuleRemoveTag(
      ServerCommandSource source, CommandStateContext state, Identifier tagId) {
    String token = RuleEntry.TAG_PREFIX + tagId;
    if (!CommandProfileEdits.removeRuleFromProfile(state.profile(), token)) {
      source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_RULE_NOT_FOUND, token));
      return 0;
    }
    state.markDirty(source);
    Text message =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_RULE_REMOVE_SELF, token)
            : Text.translatable(
                LootLockLang.COMMAND_RULE_REMOVE_TARGET, token, state.displayName());
    source.sendFeedback(() -> message, false);
    state.syncIfOnline();
    return 1;
  }

  static boolean tagExists(Identifier tagId) {
    if (tagId == null) {
      return false;
    }
    return Registries.ITEM
        .getEntryList(net.minecraft.registry.tag.TagKey.of(RegistryKeys.ITEM, tagId))
        .isPresent();
  }

  static int handleRuleList(ServerCommandSource source, CommandStateContext state) {
    if (state.profile().getRules().isEmpty()) {
      Text message =
          state.isSelfTargeted()
              ? Text.translatable(LootLockLang.COMMAND_RULE_LIST_EMPTY_SELF)
              : Text.translatable(LootLockLang.COMMAND_RULE_LIST_EMPTY_TARGET, state.displayName());
      source.sendFeedback(() -> message, false);
      return 1;
    }

    String profileName = state.profile().getName();
    Text header =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_RULE_LIST_HEADER_SELF, profileName)
            : Text.translatable(
                LootLockLang.COMMAND_RULE_LIST_HEADER_TARGET, state.displayName(), profileName);
    source.sendFeedback(() -> header, false);
    int invalidRules = 0;
    for (RuleEntry rule : state.profile().getRules()) {
      if (rule == null || rule.itemId() == null || rule.itemId().isBlank()) {
        invalidRules++;
        continue;
      }
      String ruleId = rule.itemId();
      source.sendFeedback(
          () -> Text.translatable(LootLockLang.COMMAND_RULE_LIST_ROW, ruleId), false);
    }
    if (invalidRules > 0) {
      int invalidRuleCount = invalidRules;
      source.sendFeedback(
          () -> Text.translatable(LootLockLang.COMMAND_RULE_LIST_INVALID, invalidRuleCount), false);
    }
    return 1;
  }

  static int handleRuleClear(ServerCommandSource source, CommandStateContext state) {
    CommandProfileEdits.clearRulesOnProfile(state.profile());
    state.markDirty(source);
    Text message =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_RULE_CLEAR_SELF)
            : Text.translatable(LootLockLang.COMMAND_RULE_CLEAR_TARGET, state.displayName());
    source.sendFeedback(() -> message, false);
    state.syncIfOnline();
    return 1;
  }
}
