package com.grahambartley.lootlock.client.compat.jei;

import com.grahambartley.lootlock.LootLockConstants;
import com.grahambartley.lootlock.client.compat.RecipeViewerBridge;
import com.grahambartley.lootlock.client.compat.RecipeViewerBridge.Area;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public final class LootLockJeiPlugin implements IModPlugin {
  static final Identifier UID = Identifier.of(LootLockConstants.MOD_ID, "jei_plugin");

  private final JeiHoverSource hoverSource = new JeiHoverSource();

  @Override
  public Identifier getPluginUid() {
    return UID;
  }

  @Override
  public void registerGuiHandlers(IGuiHandlerRegistration registration) {
    registration.addGuiContainerHandler(InventoryScreen.class, new PanelExclusion());
    registration.addGhostIngredientHandler(InventoryScreen.class, new PanelDropTarget());
  }

  @Override
  public void onRuntimeAvailable(IJeiRuntime runtime) {
    hoverSource.runtime = runtime;
    RecipeViewerBridge.registerHoverSource(hoverSource);
  }

  @Override
  public void onRuntimeUnavailable() {
    hoverSource.runtime = null;
    RecipeViewerBridge.unregisterHoverSource(hoverSource);
  }

  static Rect2i toRect(Area area) {
    return new Rect2i(area.x(), area.y(), area.width(), area.height());
  }

  static final class JeiHoverSource implements RecipeViewerBridge.HoverSource {
    private volatile IJeiRuntime runtime;

    @Override
    public ItemStack hoveredStack() {
      IJeiRuntime current = runtime;
      if (current == null) {
        return ItemStack.EMPTY;
      }
      ItemStack fromList =
          current.getIngredientListOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
      if (fromList != null && !fromList.isEmpty()) {
        return fromList;
      }
      ItemStack fromBookmarks =
          current.getBookmarkOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
      return fromBookmarks == null ? ItemStack.EMPTY : fromBookmarks;
    }

    @Override
    public boolean isTyping() {
      IJeiRuntime current = runtime;
      return current != null && current.getIngredientListOverlay().hasKeyboardFocus();
    }
  }

  static final class PanelExclusion implements IGuiContainerHandler<InventoryScreen> {
    @Override
    public List<Rect2i> getGuiExtraAreas(InventoryScreen screen) {
      return RecipeViewerBridge.panelArea(screen).map(LootLockJeiPlugin::toRect).stream().toList();
    }
  }

  static final class PanelDropTarget implements IGhostIngredientHandler<InventoryScreen> {
    @Override
    public <I> List<Target<I>> getTargetsTyped(
        InventoryScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
      if (ingredient.getItemStack().isEmpty() || !RecipeViewerBridge.canAdd()) {
        return List.of();
      }
      return RecipeViewerBridge.panelArea(screen)
          .map(area -> List.<Target<I>>of(new PanelTarget<>(toRect(area), ingredient)))
          .orElse(List.of());
    }

    @Override
    public void onComplete() {}
  }

  record PanelTarget<I>(Rect2i area, ITypedIngredient<I> ingredient)
      implements IGhostIngredientHandler.Target<I> {
    @Override
    public Rect2i getArea() {
      return area;
    }

    @Override
    public void accept(I value) {
      ingredient
          .getItemStack()
          .ifPresent(stack -> RecipeViewerBridge.add(MinecraftClient.getInstance(), stack));
    }
  }
}
