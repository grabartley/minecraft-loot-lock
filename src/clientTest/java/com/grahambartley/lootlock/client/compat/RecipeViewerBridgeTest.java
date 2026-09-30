package com.grahambartley.lootlock.client.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.grahambartley.lootlock.client.LootLockClient;
import com.grahambartley.lootlock.client.compat.RecipeViewerBridge.Area;
import com.grahambartley.lootlock.client.compat.RecipeViewerBridge.HoverSource;
import com.grahambartley.lootlock.client.network.ClientMutationSync;
import com.grahambartley.lootlock.client.screen.inventory.LootLockInventoryPanel;
import com.grahambartley.lootlock.client.screen.inventory.LootLockPanelHolder;
import com.grahambartley.lootlock.client.screen.inventory.LootLockToast;
import com.grahambartley.lootlock.client.screen.inventory.PanelTab;
import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.data.RuleEntry;
import com.grahambartley.lootlock.network.ServerToClientPackets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

class RecipeViewerBridgeTest {
  private MockedStatic<ClientMutationSync> sync;
  private final List<HoverSource> registered = new ArrayList<>();

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @BeforeEach
  void setUp() {
    LootLockClient.getState().clear();
    sync = mockStatic(ClientMutationSync.class);
    sync.when(() -> ClientMutationSync.sendSaveRequest(any())).thenReturn(true);
  }

  @AfterEach
  void tearDown() {
    registered.forEach(RecipeViewerBridge::unregisterHoverSource);
    sync.close();
    LootLockClient.getState().clear();
  }

  static Stream<Arguments> editability() {
    return Stream.of(
        Arguments.of("not synced", false, false, false),
        Arguments.of("synced but read-only", true, false, false),
        Arguments.of("synced and editable", true, true, true));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("editability")
  void canAddRequiresSyncedEditableActiveProfile(
      String label, boolean synced, boolean canEdit, boolean expected) {
    if (synced) {
      syncActiveProfile(List.of(), canEdit);
    }
    assertEquals(expected, RecipeViewerBridge.canAdd());
  }

  static Stream<Arguments> rejectedAdds() {
    return Stream.of(
        Arguments.of("null stack", null, true, true),
        Arguments.of("empty stack", ItemStack.EMPTY, true, true),
        Arguments.of("not synced", new ItemStack(Items.DIRT), false, true),
        Arguments.of("read-only", new ItemStack(Items.DIRT), true, false));
  }

  @ParameterizedTest(name = "{0} is not added")
  @MethodSource("rejectedAdds")
  void addRejectsWhenItemOrStateUnusable(
      String label, ItemStack stack, boolean synced, boolean canEdit) {
    if (synced) {
      syncActiveProfile(List.of(), canEdit);
    }
    assertFalse(RecipeViewerBridge.add(null, stack));
    sync.verify(() -> ClientMutationSync.sendSaveRequest(any()), never());
  }

  @Test
  void addSavesNewItemToActiveProfile() {
    syncActiveProfile(List.of("minecraft:stone"), true);

    assertTrue(RecipeViewerBridge.add(null, new ItemStack(Items.DIRT)));

    sync.verify(() -> ClientMutationSync.sendSaveRequest(any()));
  }

  @Test
  void addReportsSuccessWithoutSavingWhenItemAlreadyListed() {
    syncActiveProfile(List.of("minecraft:dirt"), true);

    assertTrue(RecipeViewerBridge.add(null, new ItemStack(Items.DIRT)));

    sync.verify(() -> ClientMutationSync.sendSaveRequest(any()), never());
  }

  @Test
  void addFlashesOpenPanelInsteadOfToasting() {
    syncActiveProfile(List.of(), true);
    LootLockInventoryPanel panel = openPanel(true);
    MinecraftClient client = clientShowing(holderScreen(panel));

    assertTrue(RecipeViewerBridge.add(client, new ItemStack(Items.DIRT)));

    verify(panel).setTab(PanelTab.RULES);
    verify(panel).flashDropSuccess();
    verify(client, never()).getToastManager();
  }

  static Stream<Arguments> toastCases() {
    return Stream.of(
        Arguments.of("new item, panel closed", List.of(), false),
        Arguments.of("already listed, panel open", List.of("minecraft:dirt"), true));
  }

  @ParameterizedTest(name = "{0} shows a toast")
  @MethodSource("toastCases")
  void addShowsToastWhenPanelCannotConfirm(String label, List<String> rules, boolean panelOpen) {
    syncActiveProfile(rules, true);
    LootLockInventoryPanel panel = mock(LootLockInventoryPanel.class);
    when(panel.isOpen()).thenReturn(panelOpen);
    MinecraftClient client = clientShowing(holderScreen(panel));
    ToastManager toasts = mock(ToastManager.class);
    when(client.getToastManager()).thenReturn(toasts);

    assertTrue(RecipeViewerBridge.add(client, new ItemStack(Items.DIRT)));

    verify(toasts).add(any(LootLockToast.class));
    verify(panel, never()).flashDropSuccess();
  }

  @Test
  void hoveredStackSkipsEmptySourcesAndTakesFirstItem() {
    register(source(ItemStack.EMPTY, false));
    register(source(null, false));
    register(source(new ItemStack(Items.DIAMOND), false));

    Optional<ItemStack> hovered = RecipeViewerBridge.hoveredStack();

    assertTrue(hovered.isPresent());
    assertTrue(hovered.get().isOf(Items.DIAMOND));
  }

  @Test
  void hoveredStackIsEmptyWithoutSources() {
    assertTrue(RecipeViewerBridge.hoveredStack().isEmpty());
    assertFalse(RecipeViewerBridge.addHovered(null));
  }

  @Test
  void addHoveredAddsTheHoveredItem() {
    syncActiveProfile(List.of(), true);
    register(source(new ItemStack(Items.DIRT), false));

    assertTrue(RecipeViewerBridge.addHovered(null));

    sync.verify(() -> ClientMutationSync.sendSaveRequest(any()));
  }

  static Stream<Arguments> keyFocusCases() {
    return Stream.of(
        Arguments.of("plain screen", false, false, false, false, true),
        Arguments.of("panel idle", true, false, false, false, true),
        Arguments.of("recipe viewer search focused", false, true, false, false, false),
        Arguments.of("panel rename active", true, false, true, false, false),
        Arguments.of("panel search focused", true, false, false, true, false));
  }

  @ParameterizedTest(name = "{0} -> handled {5}")
  @MethodSource("keyFocusCases")
  void handleAddHoveredKeyStepsAsideWhileTyping(
      String label,
      boolean hasPanel,
      boolean viewerTyping,
      boolean renaming,
      boolean searching,
      boolean expected) {
    syncActiveProfile(List.of(), true);
    register(source(new ItemStack(Items.DIRT), viewerTyping));
    LootLockInventoryPanel panel = mock(LootLockInventoryPanel.class);
    when(panel.isInlineRenameActive()).thenReturn(renaming);
    when(panel.isSearchFieldFocused()).thenReturn(searching);
    Screen screen = hasPanel ? holderScreen(panel) : mock(Screen.class);

    assertEquals(expected, RecipeViewerBridge.handleAddHoveredKey(null, screen));
  }

  @ParameterizedTest(name = "jei={0}, rei={1} -> loaded {2}")
  @CsvSource({"false,false,false", "true,false,true", "false,true,true", "true,true,true"})
  void recipeViewerLoadedWhenEitherModPresent(boolean jei, boolean rei, boolean expected) {
    FabricLoader loader = mock(FabricLoader.class);
    when(loader.isModLoaded(RecipeViewerBridge.JEI_MOD_ID)).thenReturn(jei);
    when(loader.isModLoaded(RecipeViewerBridge.REI_MOD_ID)).thenReturn(rei);
    try (MockedStatic<FabricLoader> fabric = mockStatic(FabricLoader.class)) {
      fabric.when(FabricLoader::getInstance).thenReturn(loader);

      assertEquals(expected, RecipeViewerBridge.isRecipeViewerLoaded());
    }
  }

  @ParameterizedTest(name = "typing flags {0},{1} -> {2}")
  @CsvSource({"false,false,false", "true,false,true", "false,true,true"})
  void isTypingWhenAnySourceHasTextFocus(boolean first, boolean second, boolean expected) {
    register(source(ItemStack.EMPTY, first));
    register(source(ItemStack.EMPTY, second));

    assertEquals(expected, RecipeViewerBridge.isTyping());
  }

  @Test
  void registerIgnoresNullAndDeduplicatesSources() {
    HoverSource source = source(new ItemStack(Items.DIRT), false);
    RecipeViewerBridge.registerHoverSource(null);
    assertTrue(RecipeViewerBridge.hoveredStack().isEmpty());

    register(source);
    register(source);
    RecipeViewerBridge.unregisterHoverSource(source);

    assertTrue(RecipeViewerBridge.hoveredStack().isEmpty());
  }

  @ParameterizedTest(name = "text field focused {0} -> handled {1}")
  @CsvSource({"true,false", "false,true"})
  void handleAddHoveredKeyStepsAsideForFocusedTextField(boolean focused, boolean expected) {
    syncActiveProfile(List.of(), true);
    register(source(new ItemStack(Items.DIRT), false));
    TextFieldWidget field = mock(TextFieldWidget.class);
    when(field.isFocused()).thenReturn(focused);
    Screen screen = mock(Screen.class);
    when(screen.getFocused()).thenReturn(field);

    assertEquals(expected, RecipeViewerBridge.handleAddHoveredKey(null, screen));
  }

  static Stream<Arguments> panelScreens() {
    LootLockInventoryPanel closed = mock(LootLockInventoryPanel.class);
    return Stream.of(
        Arguments.of("null screen", null),
        Arguments.of("plain screen", mock(Screen.class)),
        Arguments.of("holder without panel", holderScreen(null)),
        Arguments.of("closed panel", holderScreen(closed)),
        Arguments.of("panel that does not fit", holderScreen(openPanel(false))));
  }

  @ParameterizedTest(name = "{0} has no panel area")
  @MethodSource("panelScreens")
  void panelAreaEmptyWhenPanelNotShowing(String label, Screen screen) {
    assertTrue(RecipeViewerBridge.panelArea(screen).isEmpty());
  }

  @Test
  void panelAreaMatchesOpenPanelBounds() {
    LootLockInventoryPanel panel = openPanel(true);
    when(panel.getPanelX()).thenReturn(40);
    when(panel.getPanelY()).thenReturn(12);
    when(panel.getCurrentHeight()).thenReturn(150);

    assertEquals(
        Optional.of(new Area(40, 12, LootLockInventoryPanel.WIDTH, 150)),
        RecipeViewerBridge.panelArea(holderScreen(panel)));
  }

  @ParameterizedTest(name = "({0}, {1}) inside = {2}")
  @CsvSource({
    "10,20,true",
    "39.9,59.9,true",
    "9.9,20,false",
    "40,20,false",
    "10,19.9,false",
    "10,60,false"
  })
  void areaContainsIsHalfOpen(double x, double y, boolean expected) {
    assertEquals(expected, new Area(10, 20, 30, 40).contains(x, y));
  }

  private void register(HoverSource source) {
    registered.add(source);
    RecipeViewerBridge.registerHoverSource(source);
  }

  private static HoverSource source(ItemStack stack, boolean typing) {
    return new HoverSource() {
      @Override
      public ItemStack hoveredStack() {
        return stack;
      }

      @Override
      public boolean isTyping() {
        return typing;
      }
    };
  }

  private static LootLockInventoryPanel openPanel(boolean fits) {
    LootLockInventoryPanel panel = mock(LootLockInventoryPanel.class);
    when(panel.isOpen()).thenReturn(true);
    when(panel.fitsOnScreen()).thenReturn(fits);
    return panel;
  }

  private static Screen holderScreen(LootLockInventoryPanel panel) {
    InventoryScreen screen =
        mock(InventoryScreen.class, withSettings().extraInterfaces(LootLockPanelHolder.class));
    when(((LootLockPanelHolder) screen).lootlock$getPanel()).thenReturn(panel);
    return screen;
  }

  private static MinecraftClient clientShowing(Screen screen) {
    MinecraftClient client = mock(MinecraftClient.class);
    client.currentScreen = screen;
    return client;
  }

  private static void syncActiveProfile(List<String> ruleIds, boolean canEdit) {
    LootLockProfile profile =
        new LootLockProfile(
            UUID.randomUUID(),
            "Default",
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            ruleIds.stream().map(RuleEntry::new).toList());
    LootLockClient.getState()
        .onAuthoritativeSync(
            new ServerToClientPackets.SyncPayload(
                1, UUID.randomUUID(), 3L, profile.getId(), List.of(profile), canEdit, true));
  }
}
