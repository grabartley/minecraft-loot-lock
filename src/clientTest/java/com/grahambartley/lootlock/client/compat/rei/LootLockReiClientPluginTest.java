package com.grahambartley.lootlock.client.compat.rei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grahambartley.lootlock.client.compat.RecipeViewerBridge;
import com.grahambartley.lootlock.client.compat.RecipeViewerBridge.Area;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.REIRuntime;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import me.shedaniel.rei.api.client.gui.widgets.TextField;
import me.shedaniel.rei.api.client.overlay.OverlayListWidget;
import me.shedaniel.rei.api.client.overlay.ScreenOverlay;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZonesProvider;
import me.shedaniel.rei.api.client.registry.screen.OverlayDecider;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class LootLockReiClientPluginTest {
  private static final Area AREA = new Area(5, 6, 70, 80);

  private MockedStatic<RecipeViewerBridge> bridge;
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
  void registerScreensAddsDropVisitorAndHoverSource() {
    ScreenRegistry registry = mock(ScreenRegistry.class);

    new LootLockReiClientPlugin().registerScreens(registry);

    verify(registry)
        .registerDraggableStackVisitor(any(LootLockReiClientPlugin.PanelDropVisitor.class));
    bridge.verify(
        () -> RecipeViewerBridge.registerHoverSource(LootLockReiClientPlugin.HOVER_SOURCE));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @ParameterizedTest(name = "panel shown {0}")
  @MethodSource("booleans")
  void exclusionZoneFollowsPanelArea(boolean shown) {
    ExclusionZones zones = mock(ExclusionZones.class);
    new LootLockReiClientPlugin().registerExclusionZones(zones);
    ArgumentCaptor<ExclusionZonesProvider> provider =
        ArgumentCaptor.forClass(ExclusionZonesProvider.class);
    verify(zones).register(eq(InventoryScreen.class), provider.capture());
    bridge
        .when(() -> RecipeViewerBridge.panelArea(screen))
        .thenReturn(shown ? Optional.of(AREA) : Optional.empty());

    Collection<Rectangle> rects = provider.getValue().provide(screen);

    assertEquals(shown ? List.of(areaRect()) : List.of(), List.copyOf(rects));
  }

  static Stream<Boolean> booleans() {
    return Stream.of(true, false);
  }

  static Stream<Arguments> entries() {
    ItemStack diamond = new ItemStack(Items.DIAMOND);
    return Stream.of(
        Arguments.of("null entry", null, Items.AIR),
        Arguments.of("empty entry", entry(diamond, true), Items.AIR),
        Arguments.of("non-item entry", entry("water", false), Items.AIR),
        Arguments.of("item entry", entry(diamond, false), Items.DIAMOND));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("entries")
  void itemOfOnlyAcceptsItemEntries(String label, EntryStack<?> entry, Object expected) {
    assertSame(expected, LootLockReiClientPlugin.itemOf(entry).getItem());
  }

  static Stream<Arguments> hoverCases() {
    return Stream.of(
        Arguments.of("no overlay", false, true, true, Items.DIRT, null, Items.AIR),
        Arguments.of("overlay toggled off", true, false, true, Items.DIRT, null, Items.AIR),
        Arguments.of("overlay not drawn on screen", true, true, false, Items.DIRT, null, Items.AIR),
        Arguments.of("entry list wins", true, true, true, Items.DIRT, Items.DIAMOND, Items.DIRT),
        Arguments.of("favorites fallback", true, true, true, null, Items.DIAMOND, Items.DIAMOND),
        Arguments.of("nothing hovered", true, true, true, null, null, Items.AIR));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("hoverCases")
  void hoverSourceReadsEntryListThenFavorites(
      String label,
      boolean hasOverlay,
      boolean toggledOn,
      boolean shownOnScreen,
      Item listItem,
      Item favoriteItem,
      Item expected) {
    REIRuntime runtime = mock(REIRuntime.class);
    ScreenOverlay overlay = mock(ScreenOverlay.class);
    OverlayListWidget entryList = listFocusing(listItem);
    OverlayListWidget favorites = listFocusing(favoriteItem);
    when(runtime.getOverlay()).thenReturn(hasOverlay ? Optional.of(overlay) : Optional.empty());
    when(runtime.isOverlayVisible()).thenReturn(toggledOn);
    when(overlay.getEntryList()).thenReturn(entryList);
    when(overlay.getFavoritesList()).thenReturn(Optional.of(favorites));
    MinecraftClient client = mock(MinecraftClient.class);
    client.currentScreen = screen;
    ScreenRegistry registry =
        registryDeciding(shownOnScreen ? ActionResult.SUCCESS : ActionResult.FAIL);

    try (MockedStatic<REIRuntime> rei = mockStatic(REIRuntime.class);
        MockedStatic<ScreenRegistry> screens = mockStatic(ScreenRegistry.class);
        MockedStatic<MinecraftClient> mc = mockStatic(MinecraftClient.class)) {
      rei.when(REIRuntime::getInstance).thenReturn(runtime);
      screens.when(ScreenRegistry::getInstance).thenReturn(registry);
      mc.when(MinecraftClient::getInstance).thenReturn(client);

      assertSame(expected, LootLockReiClientPlugin.HOVER_SOURCE.hoveredStack().getItem());
    }
  }

  static Stream<Arguments> deciderCases() {
    return Stream.of(
        Arguments.of("no deciders", List.of(), false),
        Arguments.of("only pass", List.of(ActionResult.PASS), false),
        Arguments.of("fail", List.of(ActionResult.FAIL), false),
        Arguments.of("success", List.of(ActionResult.SUCCESS), true),
        Arguments.of("pass then success", List.of(ActionResult.PASS, ActionResult.SUCCESS), true),
        Arguments.of(
            "fail beats later success", List.of(ActionResult.FAIL, ActionResult.SUCCESS), false));
  }

  @ParameterizedTest(name = "{0} -> shown {2}")
  @MethodSource("deciderCases")
  void overlayShownOnFollowsFirstDecisiveDecider(
      String label, List<ActionResult> results, boolean expected) {
    ScreenRegistry registry = registryDeciding(results.toArray(ActionResult[]::new));
    try (MockedStatic<ScreenRegistry> screens = mockStatic(ScreenRegistry.class)) {
      screens.when(ScreenRegistry::getInstance).thenReturn(registry);

      assertEquals(expected, LootLockReiClientPlugin.overlayShownOn(screen));
    }
  }

  @Test
  void overlayNotShownWithoutScreen() {
    assertFalse(LootLockReiClientPlugin.overlayShownOn(null));
  }

  static Stream<Arguments> typingCases() {
    return Stream.of(
        Arguments.of("no search field", false, false, false),
        Arguments.of("search unfocused", true, false, false),
        Arguments.of("search focused", true, true, true));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("typingCases")
  void hoverSourceTypingFollowsSearchFocus(
      String label, boolean hasField, boolean focused, boolean expected) {
    REIRuntime runtime = mock(REIRuntime.class);
    TextField field = mock(TextField.class);
    when(field.isFocused()).thenReturn(focused);
    when(runtime.getSearchTextField()).thenReturn(hasField ? field : null);

    try (MockedStatic<REIRuntime> rei = mockStatic(REIRuntime.class)) {
      rei.when(REIRuntime::getInstance).thenReturn(runtime);

      assertEquals(expected, LootLockReiClientPlugin.HOVER_SOURCE.isTyping());
    }
  }

  static Stream<Arguments> handledScreens() {
    return Stream.of(
        Arguments.of("inventory with panel", true, true, true),
        Arguments.of("inventory without panel", true, false, false),
        Arguments.of("other screen", false, true, false));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("handledScreens")
  void visitorHandlesOnlyInventoryWithPanel(
      String label, boolean inventory, boolean panelShown, boolean expected) {
    Screen target = inventory ? screen : mock(Screen.class);
    bridge
        .when(() -> RecipeViewerBridge.panelArea(target))
        .thenReturn(panelShown ? Optional.of(AREA) : Optional.empty());

    assertEquals(expected, new LootLockReiClientPlugin.PanelDropVisitor().isHandingScreen(target));
  }

  static Stream<Arguments> dropCases() {
    return Stream.of(
        Arguments.of(
            "item on panel", true, new Point(10, 10), true, DraggedAcceptorResult.ACCEPTED),
        Arguments.of("add refused", true, new Point(10, 10), false, DraggedAcceptorResult.PASS),
        Arguments.of("outside panel", true, new Point(1, 1), true, DraggedAcceptorResult.PASS),
        Arguments.of("no position", true, null, true, DraggedAcceptorResult.PASS),
        Arguments.of("not an item", false, new Point(10, 10), true, DraggedAcceptorResult.PASS));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("dropCases")
  void acceptDraggedStackAddsItemsDroppedOnPanel(
      String label, boolean isItem, Point at, boolean addSucceeds, DraggedAcceptorResult expected) {
    DraggingContext<Screen> context = context(at);
    DraggableStack dragged = dragged(isItem);
    bridge.when(() -> RecipeViewerBridge.panelArea(screen)).thenReturn(Optional.of(AREA));
    bridge.when(() -> RecipeViewerBridge.add(any(), any())).thenReturn(addSucceeds);

    assertEquals(
        expected,
        new LootLockReiClientPlugin.PanelDropVisitor().acceptDraggedStack(context, dragged));
    if (!isItem) {
      bridge.verify(() -> RecipeViewerBridge.add(any(), any()), never());
    }
  }

  static Stream<Arguments> boundsCases() {
    return Stream.of(
        Arguments.of("droppable", true, true, 1L),
        Arguments.of("cannot add", true, false, 0L),
        Arguments.of("not an item", false, true, 0L));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("boundsCases")
  void acceptingBoundsHighlightPanelOnlyWhenDroppable(
      String label, boolean isItem, boolean canAdd, long expectedCount) {
    bridge.when(RecipeViewerBridge::canAdd).thenReturn(canAdd);
    bridge.when(() -> RecipeViewerBridge.panelArea(screen)).thenReturn(Optional.of(AREA));

    Stream<DraggableStackVisitor.BoundsProvider> bounds =
        new LootLockReiClientPlugin.PanelDropVisitor()
            .getDraggableAcceptingBounds(context(new Point(0, 0)), dragged(isItem));

    assertEquals(expectedCount, bounds.count());
  }

  @Test
  void toRectCopiesArea() {
    assertEquals(areaRect(), LootLockReiClientPlugin.toRect(AREA));
  }

  private DraggingContext<Screen> context(Point at) {
    @SuppressWarnings("unchecked")
    DraggingContext<Screen> context = mock(DraggingContext.class);
    when(context.getScreen()).thenReturn(screen);
    when(context.getCurrentPosition()).thenReturn(at);
    return context;
  }

  private static DraggableStack dragged(boolean isItem) {
    DraggableStack dragged = mock(DraggableStack.class);
    EntryStack<?> entry = isItem ? entry(new ItemStack(Items.DIRT), false) : entry("lava", false);
    when(dragged.getStack()).thenAnswer(invocation -> entry);
    return dragged;
  }

  private static OverlayListWidget listFocusing(Item item) {
    OverlayListWidget list = mock(OverlayListWidget.class);
    EntryStack<?> entry = item == null ? null : entry(new ItemStack(item), false);
    when(list.getFocusedStack()).thenAnswer(invocation -> entry);
    return list;
  }

  private static EntryStack<?> entry(Object value, boolean empty) {
    @SuppressWarnings("unchecked")
    EntryStack<Object> entry = mock(EntryStack.class);
    when(entry.isEmpty()).thenReturn(empty);
    when(entry.getValue()).thenReturn(value);
    return entry;
  }

  private ScreenRegistry registryDeciding(ActionResult... results) {
    List<OverlayDecider> deciders = new ArrayList<>();
    for (ActionResult result : results) {
      OverlayDecider decider = mock(OverlayDecider.class);
      when(decider.shouldScreenBeOverlaid(screen)).thenReturn(result);
      deciders.add(decider);
    }
    ScreenRegistry registry = mock(ScreenRegistry.class);
    when(registry.getDeciders(screen)).thenReturn(deciders);
    return registry;
  }

  private static Rectangle areaRect() {
    return new Rectangle(AREA.x(), AREA.y(), AREA.width(), AREA.height());
  }
}
