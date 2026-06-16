package com.grahambartley.client.screen;

import com.grahambartley.client.LootLockClient;
import com.grahambartley.client.screen.inventory.Chrome;
import com.grahambartley.client.screen.inventory.LootLockInventoryPanel;
import com.grahambartley.client.screen.inventory.LootLockToast;
import com.grahambartley.client.screen.inventory.ProfileShareController;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.network.ClientMutationSync;
import com.grahambartley.network.PacketLimits;
import com.grahambartley.text.LootLockLang;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public final class ProfileImportScreen extends Screen {
  private static final int CARD_WIDTH = 260;
  private static final int CARD_HEIGHT = 134;
  private static final int PADDING = 12;
  private static final int ROW_GAP = 8;
  private static final int TITLE_HEIGHT = 10;
  private static final int DESCRIPTION_HEIGHT = 9;
  private static final int FIELD_HEIGHT = 18;
  private static final int ERROR_HEIGHT = 9;
  private static final int BUTTON_HEIGHT = 20;
  private static final int BUTTON_GAP = 8;
  private static final int TITLE_COLOR = 0xFF2F2F2F;
  private static final int DESCRIPTION_COLOR = 0xFF4A4A52;
  private static final int ERROR_COLOR = 0xFFB03A30;

  private final Screen returnTo;
  private TextFieldWidget codeField;
  private Text inlineError;
  private int cardX;
  private int cardY;
  private int titleY;
  private int descriptionY;
  private int fieldY;
  private int errorY;

  public ProfileImportScreen(Screen returnTo) {
    super(Text.translatable(LootLockLang.IMPORT_MODAL_TITLE));
    this.returnTo = returnTo;
  }

  @Override
  protected void init() {
    super.init();
    cardX = (width - CARD_WIDTH) / 2;
    cardY = (height - CARD_HEIGHT) / 2;

    int innerLeft = cardX + PADDING;
    int innerRight = cardX + CARD_WIDTH - PADDING;
    int innerWidth = innerRight - innerLeft;
    int cursorY = cardY + PADDING;

    titleY = cursorY;
    cursorY += TITLE_HEIGHT + ROW_GAP;
    descriptionY = cursorY;
    cursorY += DESCRIPTION_HEIGHT + ROW_GAP;

    fieldY = cursorY;
    codeField =
        new TextFieldWidget(
            textRenderer,
            innerLeft,
            fieldY,
            innerWidth,
            FIELD_HEIGHT,
            Text.translatable(LootLockLang.IMPORT_MODAL_PLACEHOLDER));
    codeField.setMaxLength(PacketLimits.MAX_SHARE_CODE_LENGTH);
    codeField.setDrawsBackground(true);
    codeField.setPlaceholder(Text.translatable(LootLockLang.IMPORT_MODAL_PLACEHOLDER));
    addDrawableChild(codeField);
    setInitialFocus(codeField);
    cursorY += FIELD_HEIGHT + ROW_GAP;

    errorY = cursorY;
    cursorY += ERROR_HEIGHT + ROW_GAP;

    int buttonRowWidth = innerWidth;
    int buttonWidth = (buttonRowWidth - BUTTON_GAP) / 2;
    int buttonY = cardY + CARD_HEIGHT - PADDING - BUTTON_HEIGHT;
    addDrawableChild(
        ButtonWidget.builder(Text.translatable(LootLockLang.IMPORT_MODAL_CANCEL), button -> close())
            .dimensions(innerLeft, buttonY, buttonWidth, BUTTON_HEIGHT)
            .build());
    addDrawableChild(
        ButtonWidget.builder(
                Text.translatable(LootLockLang.IMPORT_MODAL_CONFIRM), button -> attemptImport())
            .dimensions(innerLeft + buttonWidth + BUTTON_GAP, buttonY, buttonWidth, BUTTON_HEIGHT)
            .build());
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    renderBackground(context);
    Chrome.guiWindow(context, cardX, cardY, CARD_WIDTH, CARD_HEIGHT);
    int innerLeft = cardX + PADDING;

    context.drawText(
        textRenderer,
        Text.translatable(LootLockLang.IMPORT_MODAL_TITLE),
        innerLeft,
        titleY,
        TITLE_COLOR,
        false);
    context.drawText(
        textRenderer,
        Text.translatable(LootLockLang.IMPORT_MODAL_DESCRIPTION),
        innerLeft,
        descriptionY,
        DESCRIPTION_COLOR,
        false);

    super.render(context, mouseX, mouseY, delta);

    if (inlineError != null) {
      context.drawText(textRenderer, inlineError, innerLeft, errorY, ERROR_COLOR, false);
    }
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
        && codeField != null
        && codeField.isFocused()) {
      attemptImport();
      return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public boolean shouldPause() {
    return false;
  }

  @Override
  public void close() {
    MinecraftClient client = MinecraftClient.getInstance();
    client.setScreen(returnTo);
  }

  void attemptImport() {
    if (codeField == null) {
      return;
    }
    MinecraftClient client = MinecraftClient.getInstance();
    Optional<LootLockPlayerData> snapshot = LootLockClient.getState().getSnapshot();
    ProfileShareController.ImportOutcome outcome =
        ProfileShareController.importCode(
            codeField.getText(),
            snapshot.orElse(null),
            ClientMutationSync::sendCreateRequest,
            (title, subtitle) -> LootLockToast.show(client, title, subtitle));
    if (outcome.success()) {
      inlineError = null;
      LootLockInventoryPanel.requestDropdownReopen();
      close();
      return;
    }
    inlineError =
        outcome.errorText() == null ? null : outcome.errorText().copy().formatted(Formatting.RED);
    setFocused(codeField);
    codeField.setFocused(true);
  }
}
