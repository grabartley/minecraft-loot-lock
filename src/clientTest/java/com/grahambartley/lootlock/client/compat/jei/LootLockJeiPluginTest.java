package com.grahambartley.lootlock.client.compat.jei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grahambartley.lootlock.client.compat.RecipeViewerBridge;
import com.grahambartley.lootlock.client.compat.RecipeViewerBridge.Area;
import com.grahambartley.lootlock.client.compat.RecipeViewerBridge.HoverSource;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler.Target;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IBookmarkOverlay;
import mezz.jei.api.runtime.IIngredientListOverlay;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class LootLockJeiPluginTest {
  private static final Area AREA = new Area(5, 6, 70, 80);

  private MockedStatic<RecipeViewerBridge> bridge;
  private final LootLockJeiPlugin plugin = new LootLockJeiPlugin();
  private final InventoryScreen screen = mock(InventoryScreen.class);

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @BeforeEach
  void setUp() {
    bridge = mockStatic(RecipeViewerBridge.class);
  }

  @AfterEach
  void tearDown() {
    bridge.close();
  }

  @Test
  void pluginUidIsNamespacedToLootLock() {
    assertEquals("loot-lock:jei_plugin", plugin.getPluginUid().toString());
  }

  @Test
  void registerGuiHandlersAddsExclusionAndDropTargetForInventory() {
    IGuiHandlerRegistration registration = mock(IGuiHandlerRegistration.class);

    plugin.registerGuiHandlers(registration);

    verify(registration)
        .addGuiContainerHandler(
            eq(InventoryScreen.class), any(LootLockJeiPlugin.PanelExclusion.class));
    verify(registration)
        .addGhostIngredientHandler(
            eq(InventoryScreen.class), any(LootLockJeiPlugin.PanelDropTarget.class));
  }

  @Test
  void runtimeLifecycleRegistersAndRemovesTheSameHoverSource() {
    plugin.onRuntimeAvailable(mock(IJeiRuntime.class));
    HoverSource registered = capturedHoverSource();

    plugin.onRuntimeUnavailable();

    bridge.verify(() -> RecipeViewerBridge.unregisterHoverSource(registered));
    assertTrue(registered.hoveredStack().isEmpty());
    assertFalse(registered.isTyping());
  }

  static Stream<Arguments> hoverCases() {
    ItemStack dirt = new ItemStack(Items.DIRT);
    ItemStack diamond = new ItemStack(Items.DIAMOND);
    return Stream.of(
        Arguments.of("list wins", dirt, diamond, Items.DIRT),
        Arguments.of("bookmark fallback", null, diamond, Items.DIAMOND),
        Arguments.of("empty list falls back", ItemStack.EMPTY, diamond, Items.DIAMOND),
        Arguments.of("nothing hovered", null, null, Items.AIR));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("hoverCases")
  void hoverSourceReadsListThenBookmarks(
      String label, ItemStack fromList, ItemStack fromBookmarks, Object expected) {
    IJeiRuntime runtime = mock(IJeiRuntime.class);
    IIngredientListOverlay list = mock(IIngredientListOverlay.class);
    IBookmarkOverlay bookmarks = mock(IBookmarkOverlay.class);
    when(runtime.getIngredientListOverlay()).thenReturn(list);
    when(runtime.getBookmarkOverlay()).thenReturn(bookmarks);
    when(list.getIngredientUnderMouse(VanillaTypes.ITEM_STACK)).thenReturn(fromList);
    when(bookmarks.getIngredientUnderMouse(VanillaTypes.ITEM_STACK)).thenReturn(fromBookmarks);

    plugin.onRuntimeAvailable(runtime);

    assertSame(expected, capturedHoverSource().hoveredStack().getItem());
  }

  @ParameterizedTest(name = "keyboard focus {0}")
  @MethodSource("booleans")
  void hoverSourceTypingFollowsListKeyboardFocus(boolean focused) {
    IJeiRuntime runtime = mock(IJeiRuntime.class);
    IIngredientListOverlay list = mock(IIngredientListOverlay.class);
    when(runtime.getIngredientListOverlay()).thenReturn(list);
    when(list.hasKeyboardFocus()).thenReturn(focused);

    plugin.onRuntimeAvailable(runtime);

    assertEquals(focused, capturedHoverSource().isTyping());
  }

  static Stream<Boolean> booleans() {
    return Stream.of(true, false);
  }

  @Test
  void exclusionCoversOpenPanel() {
    bridge.when(() -> RecipeViewerBridge.panelArea(screen)).thenReturn(Optional.of(AREA));

    List<Rect2i> areas = new LootLockJeiPlugin.PanelExclusion().getGuiExtraAreas(screen);

    assertEquals(1, areas.size());
    assertRect(areas.get(0));
  }

  @Test
  void exclusionEmptyWhenPanelHidden() {
    assertTrue(new LootLockJeiPlugin.PanelExclusion().getGuiExtraAreas(screen).isEmpty());
  }

  static Stream<Arguments> noTargetCases() {
    return Stream.of(
        Arguments.of("not an item", false, true, true),
        Arguments.of("cannot add", true, false, true),
        Arguments.of("panel hidden", true, true, false));
  }

  @ParameterizedTest(name = "{0} offers no drop target")
  @MethodSource("noTargetCases")
  void dropTargetsEmptyUnlessItemCanLandOnPanel(
      String label, boolean isItem, boolean canAdd, boolean panelShown) {
    ITypedIngredient<Object> ingredient = ingredient(isItem);
    bridge.when(RecipeViewerBridge::canAdd).thenReturn(canAdd);
    bridge
        .when(() -> RecipeViewerBridge.panelArea(screen))
        .thenReturn(panelShown ? Optional.of(AREA) : Optional.empty());

    assertTrue(
        new LootLockJeiPlugin.PanelDropTarget()
            .getTargetsTyped(screen, ingredient, true)
            .isEmpty());
  }

  @Test
  void dropTargetCoversPanelAndAddsDroppedItem() {
    ITypedIngredient<Object> ingredient = ingredient(true);
    bridge.when(RecipeViewerBridge::canAdd).thenReturn(true);
    bridge.when(() -> RecipeViewerBridge.panelArea(screen)).thenReturn(Optional.of(AREA));

    List<Target<Object>> targets =
        new LootLockJeiPlugin.PanelDropTarget().getTargetsTyped(screen, ingredient, true);

    assertEquals(1, targets.size());
    assertRect(targets.get(0).getArea());
    targets.get(0).accept(new Object());
    bridge.verify(() -> RecipeViewerBridge.add(any(), any(ItemStack.class)));
  }

  @Test
  void dropTargetIgnoresNonItemOnAccept() {
    ITypedIngredient<Object> ingredient = ingredient(false);

    new LootLockJeiPlugin.PanelTarget<>(new Rect2i(0, 0, 1, 1), ingredient).accept(new Object());

    bridge.verify(() -> RecipeViewerBridge.add(any(), any()), never());
  }

  private HoverSource capturedHoverSource() {
    ArgumentCaptor<HoverSource> captor = ArgumentCaptor.forClass(HoverSource.class);
    bridge.verify(() -> RecipeViewerBridge.registerHoverSource(captor.capture()));
    return captor.getValue();
  }

  @SuppressWarnings("unchecked")
  private static ITypedIngredient<Object> ingredient(boolean isItem) {
    ITypedIngredient<Object> ingredient = mock(ITypedIngredient.class);
    when(ingredient.getItemStack())
        .thenReturn(isItem ? Optional.of(new ItemStack(Items.DIRT)) : Optional.empty());
    return ingredient;
  }

  private static void assertRect(Rect2i rect) {
    assertEquals(AREA.x(), rect.getX());
    assertEquals(AREA.y(), rect.getY());
    assertEquals(AREA.width(), rect.getWidth());
    assertEquals(AREA.height(), rect.getHeight());
  }
}
