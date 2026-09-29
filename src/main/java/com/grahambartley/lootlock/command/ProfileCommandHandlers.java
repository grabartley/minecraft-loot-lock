package com.grahambartley.lootlock.command;

import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.network.PacketLimits;
import com.grahambartley.lootlock.share.ProfileShareCodec;
import com.grahambartley.lootlock.text.LootLockLang;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

final class ProfileCommandHandlers {
  private ProfileCommandHandlers() {}

  static int handleProfileList(ServerCommandSource source, CommandStateContext state) {
    Text header =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_PROFILE_LIST_HEADER_SELF)
            : Text.translatable(
                LootLockLang.COMMAND_PROFILE_LIST_HEADER_TARGET, state.displayName());
    source.sendFeedback(() -> header, false);
    for (LootLockProfile profile : state.data().getProfiles()) {
      if (profile == null) {
        continue;
      }
      String rowKey =
          profile.getId().equals(state.data().getActiveProfileId())
              ? LootLockLang.COMMAND_PROFILE_LIST_ROW_ACTIVE
              : LootLockLang.COMMAND_PROFILE_LIST_ROW_INACTIVE;
      String name = profile.getName();
      source.sendFeedback(() -> Text.translatable(rowKey, name), false);
    }
    return 1;
  }

  static int handleProfileCreate(
      ServerCommandSource source, CommandStateContext state, String requestedName) {
    String profileName = CommandProfileEdits.normalizeProfileName(requestedName);
    if (profileName == null) {
      source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_PROFILE_NAME_LENGTH));
      return 0;
    }

    if (!CommandProfileEdits.canCreateProfile(state.data())) {
      Text error =
          state.isSelfTargeted()
              ? Text.translatable(
                  LootLockLang.COMMAND_ERROR_PROFILE_MAX_SELF, PacketLimits.MAX_PROFILES)
              : Text.translatable(
                  LootLockLang.COMMAND_ERROR_PROFILE_MAX_TARGET,
                  state.displayName(),
                  PacketLimits.MAX_PROFILES);
      source.sendError(error);
      return 0;
    }

    if (CommandProfileEdits.findProfileByName(state.data(), profileName).isPresent()) {
      source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_PROFILE_DUPLICATE));
      return 0;
    }

    LootLockProfile created = CommandProfileEdits.createProfileWithDefaults(profileName);
    CommandProfileEdits.appendProfile(state.data(), created);
    state.markDirty(source);
    Text message =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_PROFILE_CREATE_SELF, profileName)
            : Text.translatable(
                LootLockLang.COMMAND_PROFILE_CREATE_TARGET, profileName, state.displayName());
    source.sendFeedback(() -> message, false);
    state.syncIfOnline();
    return 1;
  }

  static int handleProfileExport(
      ServerCommandSource source, CommandStateContext state, String requestedName) {
    Optional<LootLockProfile> found =
        CommandProfileEdits.findProfileByName(state.data(), requestedName);
    if (found.isEmpty()) {
      source.sendError(
          Text.translatable(LootLockLang.COMMAND_ERROR_PROFILE_NOT_FOUND, requestedName));
      return 0;
    }

    LootLockProfile target = found.get();
    String code = ProfileShareCodec.encode(target);
    String profileNameValue = target.getName();
    int ruleCount = target.getRules().size();
    int codeLength = code.length();

    Text header =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_PROFILE_EXPORT_HEADER_SELF, profileNameValue)
            : Text.translatable(
                LootLockLang.COMMAND_PROFILE_EXPORT_HEADER_TARGET,
                profileNameValue,
                state.displayName());
    source.sendFeedback(() -> header, false);

    Style codeStyle =
        Style.EMPTY
            .withFormatting(Formatting.YELLOW)
            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, code))
            .withHoverEvent(
                new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Text.translatable(LootLockLang.COMMAND_PROFILE_EXPORT_HOVER)));
    source.sendFeedback(() -> Text.literal(code).setStyle(codeStyle), false);

    String summaryKey =
        ruleCount == 1
            ? LootLockLang.COMMAND_PROFILE_EXPORT_SUMMARY_ONE
            : LootLockLang.COMMAND_PROFILE_EXPORT_SUMMARY_MANY;
    source.sendFeedback(() -> Text.translatable(summaryKey, ruleCount, codeLength), false);
    return 1;
  }

  static int handleProfileImport(
      ServerCommandSource source, CommandStateContext state, String rawCode) {
    ProfileShareCodec.DecodeResult result = ProfileShareCodec.decode(rawCode);
    if (result instanceof ProfileShareCodec.DecodeResult.Err err) {
      source.sendError(Text.translatable(shareCodeErrorKey(err.reason())));
      return 0;
    }
    ProfileShareCodec.DecodeResult.Ok ok = (ProfileShareCodec.DecodeResult.Ok) result;
    LootLockProfile decoded = ok.profile();

    if (!CommandProfileEdits.canCreateProfile(state.data())) {
      Text error =
          state.isSelfTargeted()
              ? Text.translatable(
                  LootLockLang.COMMAND_ERROR_PROFILE_MAX_SELF, PacketLimits.MAX_PROFILES)
              : Text.translatable(
                  LootLockLang.COMMAND_ERROR_PROFILE_MAX_TARGET,
                  state.displayName(),
                  PacketLimits.MAX_PROFILES);
      source.sendError(error);
      return 0;
    }

    String requestedName = decoded.getName();
    boolean renamed =
        CommandProfileEdits.findProfileByName(state.data(), requestedName).isPresent();
    String finalName =
        renamed
            ? CommandProfileEdits.nextAvailableName(state.data(), requestedName)
            : requestedName;
    String normalizedName = CommandProfileEdits.normalizeProfileName(finalName);
    if (normalizedName == null) {
      source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_PROFILE_NAME_LENGTH));
      return 0;
    }

    LootLockProfile created =
        new LootLockProfile(
            UUID.randomUUID(),
            normalizedName,
            decoded.getMode(),
            decoded.getRejectedItemAction(),
            true,
            0,
            new ArrayList<>(decoded.getRules()));
    CommandProfileEdits.appendProfile(state.data(), created);
    state.markDirty(source);

    int ruleCount = created.getRules().size();
    if (renamed) {
      Text feedback =
          Text.translatable(
              LootLockLang.COMMAND_PROFILE_IMPORT_FEEDBACK_RENAMED,
              normalizedName,
              ruleCount,
              requestedName);
      source.sendFeedback(() -> feedback, false);
    } else {
      Text feedback =
          state.isSelfTargeted()
              ? Text.translatable(
                  LootLockLang.COMMAND_PROFILE_IMPORT_FEEDBACK_SELF, normalizedName, ruleCount)
              : Text.translatable(
                  LootLockLang.COMMAND_PROFILE_IMPORT_FEEDBACK_TARGET,
                  normalizedName,
                  ruleCount,
                  state.displayName());
      source.sendFeedback(() -> feedback, false);
    }
    state.syncIfOnline();
    return 1;
  }

  static String shareCodeErrorKey(String reason) {
    return switch (reason) {
      case "empty" -> LootLockLang.COMMAND_ERROR_SHARE_CODE_EMPTY;
      case "too_long" -> LootLockLang.COMMAND_ERROR_SHARE_CODE_TOO_LONG;
      case "bad_prefix" -> LootLockLang.COMMAND_ERROR_SHARE_CODE_BAD_PREFIX;
      case "bad_base64", "bad_deflate", "bad_json", "bad_version" ->
          LootLockLang.COMMAND_ERROR_SHARE_CODE_BAD_PAYLOAD;
      default -> LootLockLang.COMMAND_ERROR_SHARE_CODE_BAD_FIELD;
    };
  }

  static int handleProfileDelete(
      ServerCommandSource source, CommandStateContext state, String requestedName) {
    if (state.data().getProfiles().size() <= 1) {
      Text error =
          state.isSelfTargeted()
              ? Text.translatable(LootLockLang.COMMAND_ERROR_PROFILE_LAST_SELF)
              : Text.translatable(
                  LootLockLang.COMMAND_ERROR_PROFILE_LAST_TARGET, state.displayName());
      source.sendError(error);
      return 0;
    }

    Optional<LootLockProfile> found =
        CommandProfileEdits.findProfileByName(state.data(), requestedName);
    if (found.isEmpty()) {
      source.sendError(
          Text.translatable(LootLockLang.COMMAND_ERROR_PROFILE_NOT_FOUND, requestedName));
      return 0;
    }

    LootLockProfile target = found.get();
    String targetName = target.getName();
    CommandProfileEdits.removeProfileById(state.data(), target.getId());
    state.markDirty(source);
    Text message =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_PROFILE_DELETE_SELF, targetName)
            : Text.translatable(
                LootLockLang.COMMAND_PROFILE_DELETE_TARGET, targetName, state.displayName());
    source.sendFeedback(() -> message, false);
    state.syncIfOnline();
    return 1;
  }

  static int handleProfileActivate(
      ServerCommandSource source, CommandStateContext state, String requestedName) {
    Optional<LootLockProfile> found =
        CommandProfileEdits.findProfileByName(state.data(), requestedName);
    if (found.isEmpty()) {
      source.sendError(
          Text.translatable(LootLockLang.COMMAND_ERROR_PROFILE_NOT_FOUND, requestedName));
      return 0;
    }

    LootLockProfile target = found.get();
    String targetName = target.getName();
    state.data().setActiveProfileId(target.getId());
    state.markDirty(source);
    Text message =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_PROFILE_ACTIVATE_SELF, targetName)
            : Text.translatable(
                LootLockLang.COMMAND_PROFILE_ACTIVATE_TARGET, targetName, state.displayName());
    source.sendFeedback(() -> message, false);
    SettingsCommandHandlers.sendStatus(source, state.withProfile(target));
    state.syncIfOnline();
    return 1;
  }
}
