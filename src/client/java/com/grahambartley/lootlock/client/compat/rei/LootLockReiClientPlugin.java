package com.grahambartley.lootlock.client.compat.rei;

import com.grahambartley.lootlock.client.compat.RecipeViewerBridge;
import com.grahambartley.lootlock.client.compat.RecipeViewerBridge.Area;
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
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;

public final class LootLockReiClientPlugin implements REIClientPlugin {
  static final RecipeViewerBridge.HoverSource HOVER_SOURCE = new ReiHoverSource();

  @Override
  public void registerScreens(ScreenRegistry registry) {
    registry.registerDraggableStackVisitor(new PanelDropVisitor());
    RecipeViewerBridge.registerHoverSource(HOVER_SOURCE);
  }

  @Override
  public void registerExclusionZones(ExclusionZones zones) {
    zones.register(
        InventoryScreen.class,
        (InventoryScreen screen) ->
            RecipeViewerBridge.panelArea(screen).map(LootLockReiClientPlugin::toRect).stream()
                .toList());
  }

  static Rectangle toRect(Area area) {
    return new Rectangle(area.x(), area.y(), area.width(), area.height());
  }

  static ItemStack itemOf(EntryStack<?> entry) {
    if (entry == null || entry.isEmpty() || !(entry.getValue() instanceof ItemStack stack)) {
      return ItemStack.EMPTY;
    }
    return stack;
  }

  static final class ReiHoverSource implements RecipeViewerBridge.HoverSource {
    @Override
    public ItemStack hoveredStack() {
      Optional<ScreenOverlay> overlay = REIRuntime.getInstance().getOverlay();
      if (overlay.isEmpty()) {
        return ItemStack.EMPTY;
      }
      ItemStack fromList = itemOf(overlay.get().getEntryList().getFocusedStack());
      if (!fromList.isEmpty()) {
        return fromList;
      }
      return overlay
          .get()
          .getFavoritesList()
          .map(OverlayListWidget::getFocusedStack)
          .map(LootLockReiClientPlugin::itemOf)
          .orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean isTyping() {
      TextField search = REIRuntime.getInstance().getSearchTextField();
      return search != null && search.isFocused();
    }
  }

  static final class PanelDropVisitor implements DraggableStackVisitor<Screen> {
    @Override
    public <R extends Screen> boolean isHandingScreen(R screen) {
      return screen instanceof InventoryScreen && RecipeViewerBridge.panelArea(screen).isPresent();
    }

    @Override
    public DraggedAcceptorResult acceptDraggedStack(
        DraggingContext<Screen> context, DraggableStack stack) {
      ItemStack item = itemOf(stack.getStack());
      Point at = context.getCurrentPosition();
      boolean overPanel =
          at != null
              && RecipeViewerBridge.panelArea(context.getScreen())
                  .filter(area -> area.contains(at.x, at.y))
                  .isPresent();
      if (item.isEmpty() || !overPanel) {
        return DraggedAcceptorResult.PASS;
      }
      return RecipeViewerBridge.add(MinecraftClient.getInstance(), item)
          ? DraggedAcceptorResult.ACCEPTED
          : DraggedAcceptorResult.PASS;
    }

    @Override
    public Stream<BoundsProvider> getDraggableAcceptingBounds(
        DraggingContext<Screen> context, DraggableStack stack) {
      if (itemOf(stack.getStack()).isEmpty() || !RecipeViewerBridge.canAdd()) {
        return Stream.empty();
      }
      return RecipeViewerBridge.panelArea(context.getScreen())
          .map(area -> BoundsProvider.ofRectangle(toRect(area)))
          .stream();
    }
  }
}
