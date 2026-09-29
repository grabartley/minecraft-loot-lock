package com.grahambartley.lootlock.client.screen;

import com.grahambartley.lootlock.client.LootLockClient;
import com.grahambartley.lootlock.client.network.ClientMutationSync;
import com.grahambartley.lootlock.client.screen.inventory.LootLockInventoryPanel;
import com.grahambartley.lootlock.client.screen.inventory.LootLockToast;
import com.grahambartley.lootlock.client.screen.inventory.ProfileShareController;
import com.grahambartley.lootlock.data.LootLockPlayerData;
import com.grahambartley.lootlock.network.PacketLimits;
import com.grahambartley.lootlock.text.LootLockLang;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public final class ProfileImportScreen extends Screen {
  static final int TITLE_Y = 15;
  static final int CONTENT_WIDTH = 300;
  static final int FIELD_HEIGHT = 20;
  static final int BUTTON_WIDTH = 150;
  static final int BUTTON_HEIGHT = 20;
  static final int BUTTON_GAP = 8;
  static final int FOOTER_OFFSET = 27;
  private static final int LINE_HEIGHT = 10;
  private static final int ROW_GAP = 8;
  private static final int DESCRIPTION_MAX_LINES = 2;
  private static final int DESCRIPTION_COLOR = 0xFFA0A0A0;

  private final Screen returnTo;
  private TextFieldWidget codeField;
  private Text inlineError;
  private int contentLeft;
  private int contentWidth;
  private int descriptionY;
  private int errorY;

  public ProfileImportScreen(Screen returnTo) {
    super(Text.translatable(LootLockLang.IMPORT_MODAL_TITLE));
    this.returnTo = returnTo;
  }

  static int contentTop(int screenHeight) {
    int blockHeight = LINE_HEIGHT * DESCRIPTION_MAX_LINES + ROW_GAP + FIELD_HEIGHT + ROW_GAP;
    return Math.max(
        TITLE_Y + LINE_HEIGHT + ROW_GAP, (screenHeight - blockHeight) / 2 - LINE_HEIGHT);
  }

  static int footerButtonsLeft(int screenWidth) {
    return (screenWidth - (BUTTON_WIDTH * 2 + BUTTON_GAP)) / 2;
  }

  @Override
  protected void init() {
    super.init();
    contentWidth = Math.min(CONTENT_WIDTH, width - 20);
    contentLeft = (width - contentWidth) / 2;
    descriptionY = contentTop(height);
    int fieldY = descriptionY + LINE_HEIGHT * DESCRIPTION_MAX_LINES + ROW_GAP;
    errorY = fieldY + FIELD_HEIGHT + ROW_GAP;

    codeField =
        new TextFieldWidget(
            textRenderer,
            contentLeft,
            fieldY,
            contentWidth,
            FIELD_HEIGHT,
            Text.translatable(LootLockLang.IMPORT_MODAL_PLACEHOLDER));
    codeField.setMaxLength(PacketLimits.MAX_SHARE_CODE_LENGTH);
    codeField.setPlaceholder(Text.translatable(LootLockLang.IMPORT_MODAL_PLACEHOLDER));
    addDrawableChild(codeField);
    setInitialFocus(codeField);

    int buttonsLeft = footerButtonsLeft(width);
    int buttonY = height - FOOTER_OFFSET;
    addDrawableChild(
        ButtonWidget.builder(Text.translatable(LootLockLang.IMPORT_MODAL_CANCEL), button -> close())
            .dimensions(buttonsLeft, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());
    addDrawableChild(
        ButtonWidget.builder(
                Text.translatable(LootLockLang.IMPORT_MODAL_CONFIRM), button -> attemptImport())
            .dimensions(
                buttonsLeft + BUTTON_WIDTH + BUTTON_GAP, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    super.render(context, mouseX, mouseY, delta);
    context.drawCenteredTextWithShadow(textRenderer, title, width / 2, TITLE_Y, 0xFFFFFFFF);
    int lineY = descriptionY;
    for (OrderedText line :
        textRenderer.wrapLines(
            Text.translatable(LootLockLang.IMPORT_MODAL_DESCRIPTION), contentWidth)) {
      context.drawCenteredTextWithShadow(textRenderer, line, width / 2, lineY, DESCRIPTION_COLOR);
      lineY += LINE_HEIGHT;
    }
    if (inlineError != null) {
      context.drawCenteredTextWithShadow(textRenderer, inlineError, width / 2, errorY, 0xFFFFFFFF);
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
