package com.grahambartley.client.screen.inventory;

import com.grahambartley.client.screen.ProfileUiController;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.share.ProfileShareCodec;
import com.grahambartley.text.LootLockLang;
import java.util.function.Consumer;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public final class ProfileShareController {
  private static final Style SUCCESS_STYLE =
      Style.EMPTY.withColor(TextColor.fromRgb(Palette.PROFILE_COLORS[1] & 0x00FFFFFF));

  private ProfileShareController() {}

  @FunctionalInterface
  public interface ToastSink {
    void show(Text title, Text subtitle);
  }

  @FunctionalInterface
  public interface CreateRequestSink {
    boolean send(long baseRevision, String name, LootLockProfile copyFromProfile);
  }

  public static boolean export(
      LootLockProfile profile, Consumer<String> clipboardSink, ToastSink toastSink) {
    if (profile == null || clipboardSink == null) {
      return false;
    }
    String code = ProfileShareCodec.encode(profile);
    clipboardSink.accept(code);
    if (toastSink != null) {
      toastSink.show(
          Text.translatable(LootLockLang.BRAND),
          Text.translatable(LootLockLang.TOAST_EXPORT_COPIED).setStyle(SUCCESS_STYLE));
    }
    return true;
  }

  public static ImportOutcome importCode(
      String code, LootLockPlayerData snapshot, CreateRequestSink createSink, ToastSink toastSink) {
    if (snapshot == null) {
      Text errorText = Text.translatable(LootLockLang.COMMAND_ERROR_NOT_READY);
      emitErrorToast(toastSink, errorText);
      return ImportOutcome.error("not_ready", errorText);
    }
    ProfileShareCodec.DecodeResult result = ProfileShareCodec.decode(code);
    if (result instanceof ProfileShareCodec.DecodeResult.Err err) {
      Text errorText = Text.translatable(shareCodeErrorKey(err.reason()));
      emitErrorToast(toastSink, errorText);
      return ImportOutcome.error(err.reason(), errorText);
    }
    LootLockProfile profile = ((ProfileShareCodec.DecodeResult.Ok) result).profile();
    if (!ProfileUiController.canCreateProfile(snapshot.getProfiles())) {
      Text errorText = Text.translatable(LootLockLang.TOAST_IMPORT_AT_CAPACITY);
      emitErrorToast(toastSink, errorText);
      return ImportOutcome.error("at_capacity", errorText);
    }
    String resolvedName =
        ProfileUiController.nextDuplicateName(snapshot.getProfiles(), profile.getName());
    if (createSink == null || !createSink.send(snapshot.getRevision(), resolvedName, profile)) {
      Text errorText = Text.translatable(LootLockLang.COMMAND_ERROR_NOT_READY);
      emitErrorToast(toastSink, errorText);
      return ImportOutcome.error("not_ready", errorText);
    }
    if (toastSink != null) {
      toastSink.show(
          Text.translatable(LootLockLang.BRAND),
          Text.translatable(LootLockLang.TOAST_IMPORT_SUCCESS, resolvedName)
              .setStyle(SUCCESS_STYLE));
    }
    return ImportOutcome.success(profile);
  }

  public static String shareCodeErrorKey(String reason) {
    return switch (reason == null ? "" : reason) {
      case "empty" -> LootLockLang.COMMAND_ERROR_SHARE_CODE_EMPTY;
      case "too_long" -> LootLockLang.COMMAND_ERROR_SHARE_CODE_TOO_LONG;
      case "bad_prefix" -> LootLockLang.COMMAND_ERROR_SHARE_CODE_BAD_PREFIX;
      case "bad_base64", "bad_deflate", "bad_json", "bad_version" ->
          LootLockLang.COMMAND_ERROR_SHARE_CODE_BAD_PAYLOAD;
      default -> LootLockLang.COMMAND_ERROR_SHARE_CODE_BAD_FIELD;
    };
  }

  private static void emitErrorToast(ToastSink sink, Text errorText) {
    if (sink == null) {
      return;
    }
    sink.show(Text.translatable(LootLockLang.BRAND), errorText.copy().formatted(Formatting.RED));
  }

  public record ImportOutcome(
      boolean success, String errorReason, Text errorText, LootLockProfile profile) {
    static ImportOutcome success(LootLockProfile profile) {
      return new ImportOutcome(true, null, null, profile);
    }

    static ImportOutcome error(String reason, Text errorText) {
      return new ImportOutcome(false, reason, errorText, null);
    }
  }
}
