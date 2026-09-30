package com.grahambartley.lootlock.client.compat;

import com.grahambartley.lootlock.client.LootLockClient;
import com.grahambartley.lootlock.client.screen.inventory.DragToAddRouter;
import com.grahambartley.lootlock.client.screen.inventory.LootLockInventoryPanel;
import com.grahambartley.lootlock.client.screen.inventory.LootLockPanelHolder;
import com.grahambartley.lootlock.client.screen.inventory.LootLockToast;
import com.grahambartley.lootlock.client.screen.inventory.PanelTab;
import com.grahambartley.lootlock.client.screen.inventory.RuleMutations;
import com.grahambartley.lootlock.client.state.ClientLootLockState;
import com.grahambartley.lootlock.data.LootLockPlayerData;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RuleEntry;
import com.grahambartley.lootlock.text.LootLockLang;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class RecipeViewerBridge {
  public interface HoverSource {
    ItemStack hoveredStack();

    boolean isTyping();
  }

  public record Area(int x, int y, int width, int height) {
    public boolean contains(double px, double py) {
      return px >= x && px < x + width && py >= y && py < y + height;
    }
  }

  private static final Set<HoverSource> SOURCES = new CopyOnWriteArraySet<>();

  private RecipeViewerBridge() {}

  public static void registerHoverSource(HoverSource source) {
    if (source != null) {
      SOURCES.add(source);
    }
  }

  public static void unregisterHoverSource(HoverSource source) {
    SOURCES.remove(source);
  }

  public static boolean hasHoverSources() {
    return !SOURCES.isEmpty();
  }

  public static boolean isTyping() {
    return SOURCES.stream().anyMatch(HoverSource::isTyping);
  }

  public static Optional<ItemStack> hoveredStack() {
    return SOURCES.stream()
        .map(HoverSource::hoveredStack)
        .filter(stack -> stack != null && !stack.isEmpty())
        .findFirst();
  }

  public static boolean addHovered(MinecraftClient client) {
    return hoveredStack().map(stack -> add(client, stack)).orElse(false);
  }

  public static boolean canAdd() {
    ClientLootLockState state = LootLockClient.getState();
    return state.isSynced() && editableActiveProfile(state).isPresent();
  }

  public static boolean add(MinecraftClient client, ItemStack stack) {
    String itemId = DragToAddRouter.itemIdOf(stack);
    ClientLootLockState state = LootLockClient.getState();
    if (itemId == null || !state.isSynced()) {
      return false;
    }
    Optional<LootLockProfile> profile = editableActiveProfile(state);
    if (profile.isEmpty()) {
      return false;
    }
    boolean alreadyListed = profile.get().getRules().contains(new RuleEntry(itemId));
    if (!alreadyListed && !RuleMutations.addToActiveProfile(List.of(itemId))) {
      return false;
    }
    confirm(client, stack, profile.get(), alreadyListed);
    return true;
  }

  public static Optional<Area> panelArea(Screen screen) {
    return openPanel(screen)
        .filter(LootLockInventoryPanel::fitsOnScreen)
        .map(
            panel ->
                new Area(
                    panel.getPanelX(),
                    panel.getPanelY(),
                    LootLockInventoryPanel.WIDTH,
                    panel.getCurrentHeight()));
  }

  private static Optional<LootLockProfile> editableActiveProfile(ClientLootLockState state) {
    return state
        .getSnapshot()
        .filter(LootLockPlayerData::isClientCanEdit)
        .flatMap(LootLockPlayerData::getActiveProfile);
  }

  private static Optional<LootLockInventoryPanel> openPanel(Screen screen) {
    if (!(screen instanceof LootLockPanelHolder holder)) {
      return Optional.empty();
    }
    return Optional.ofNullable(holder.lootlock$getPanel()).filter(LootLockInventoryPanel::isOpen);
  }

  private static void confirm(
      MinecraftClient client, ItemStack stack, LootLockProfile profile, boolean alreadyListed) {
    Optional<LootLockInventoryPanel> panel =
        client == null ? Optional.empty() : openPanel(client.currentScreen);
    if (panel.isPresent() && !alreadyListed) {
      panel.get().setTab(PanelTab.RULES);
      panel.get().flashDropSuccess();
      return;
    }
    String titleKey = alreadyListed ? LootLockLang.TOAST_ALREADY_IN : LootLockLang.TOAST_ADDED_TO;
    LootLockToast.show(client, Text.translatable(titleKey, profile.getName()), stack.getName());
  }
}
