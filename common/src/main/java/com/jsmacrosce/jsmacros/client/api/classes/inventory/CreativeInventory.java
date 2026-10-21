package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinCreativeInventoryScreen;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Handles the creative inventory screen and its item tabs.
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Inventory")
@SuppressWarnings("unused")
public class CreativeInventory extends Inventory<CreativeModeInventoryScreen> {

    private final CreativeModeInventoryScreen.ItemPickerMenu handler;

    protected CreativeInventory(CreativeModeInventoryScreen inventory) {
        super(inventory);
        this.handler = inventory.getMenu();
    }

    /**
     * Scrolls the creative item list by a relative amount when it has a scrollbar.
     *
     * @param amount relative change to the current scroll position; the resulting position is clamped to {@code 0..1}
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeInventory scroll(double amount) {
        scrollTo((float) (((MixinCreativeInventoryScreen) inventory).getScrollPosition() + amount));
        return this;
    }

    /**
     * Sets the creative item list's scroll position when it has a scrollbar; otherwise, does nothing.
     *
     * @param position scroll position, clamped to {@code 0..1}
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeInventory scrollTo(double position) {
        if (((MixinCreativeInventoryScreen) inventory).invokeHasScrollbar()) {
            position = Mth.clamp(position, 0, 1);
            handler.scrollTo((float) position);
        }
        return this;
    }

    /**
     * @return the items currently displayed in the creative item list.
     * @since 1.8.4
     */
    public List<ItemStackHelper> getShownItems() {
        return handler.items.stream().map(ItemStackHelper::new).collect(Collectors.toList());
    }

    /**
     * Sets the search text and refreshes results only while the search tab is selected.
     * Otherwise, it does nothing.
     * @param search text to search for
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeInventory search(String search) {
        if (((MixinCreativeInventoryScreen) inventory).getSelectedTab() == CreativeModeTabs.searchTab()) {
            ((MixinCreativeInventoryScreen) inventory).getSearchBox().setValue(search);
            ((MixinCreativeInventoryScreen) inventory).invokeSearch();
        }
        return this;
    }

    /**
     * Selects the search tab asynchronously on the client executor.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeInventory selectSearch() {
        selectTab(CreativeModeTabs.searchTab());
        return this;
    }

    /**
     * Selects the inventory tab asynchronously on the client executor.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeInventory selectInventory() {
        CreativeModeTabs.allTabs().stream().filter(e -> e.getType().equals(CreativeModeTab.Type.INVENTORY)).findFirst().ifPresent(this::selectTab);
        return this;
    }

    /**
     * Selects the hotbar tab asynchronously on the client executor.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeInventory selectHotbar() {
        CreativeModeTabs.allTabs().stream().filter(e -> e.getType().equals(CreativeModeTab.Type.HOTBAR)).findFirst().ifPresent(this::selectTab);
        return this;
    }

    /**
     * Selects the tab whose current display name exactly matches the argument. The selection is
     * scheduled on the client executor; comparison uses the localized display name.
     * @param tabName the display name of the tab to select
     * @return self for chaining.
     * @throws IllegalArgumentException if no tab has that display name
     * @since 1.8.4
     */
    public CreativeInventory selectTab(String tabName) {
        //TODO detect if translatable and use translate id instead
        selectTab(CreativeModeTabs.allTabs().stream().filter(e -> e.getDisplayName().getString().equals(tabName)).findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid tab name")));
        return this;
    }

    /**
     * Returns the current localized display name of each creative tab, in {@link CreativeModeTabs#allTabs()} order.
     * These names are not tab IDs and are the values matched by {@link #selectTab(String)}.
     * @return the tab display names
     * @since 1.8.4
     */
    public List<String> getTabNames() {
        return CreativeModeTabs.allTabs().stream().map(e -> e.getDisplayName().getString()).collect(Collectors.toList());
    }

    /**
     * Returns each creative tab's current localized display name as a text object, in the same order as {@link #getTabNames()}.
     *
     * @return the tab display names as text objects
     * @since 1.8.4
     */
    public List<TextHelper> getTabTexts() {
        return CreativeModeTabs.allTabs().stream().map(e -> TextHelper.wrap(e.getDisplayName())).collect(Collectors.toList());
    }

    private CreativeInventory selectTab(CreativeModeTab group) {
        mc.execute(() -> ((MixinCreativeInventoryScreen) inventory).invokeSetSelectedTab(group));
        return this;
    }

    /**
     * Clears the item stack carried by the creative inventory menu.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeInventory destroyHeldItem() {
        handler.setCarried(ItemStack.EMPTY);
        return this;
    }

    /**
     * Clears every slot in this inventory menu by sending an empty stack for each slot through the creative item-add operation.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeInventory destroyAllItems() {
        MultiPlayerGameMode interactionManager = Minecraft.getInstance().gameMode;
        for (int i = 0; i < getTotalSlots(); i++) {
            interactionManager.handleCreativeModeItemAdd(ItemStack.EMPTY, i);
        }
        return this;
    }

    /**
     * Sets the stack carried by the creative inventory menu.
     * @param stack the stack to carry
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeInventory setCursorStack(ItemStackHelper stack) {
        handler.setCarried(stack.getRaw());
        return this;
    }

    /**
     * Sets a slot through the creative item-add operation.
     * @param slot  the menu slot to set
     * @param stack the stack to set
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeInventory setStack(int slot, ItemStackHelper stack) {
        Minecraft.getInstance().gameMode.handleCreativeModeItemAdd(stack.getRaw(), slot);
        return this;
    }

    /**
     * Saves the player's current hotbar to a saved-hotbar slot.
     * @param index saved-hotbar index, from 0 to 8
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeInventory saveHotbar(int index) {
        CreativeModeInventoryScreen.handleHotbarLoadOrSave(Minecraft.getInstance(), index, false, true);
        return this;
    }

    /**
     * Restores a saved hotbar to the player's current hotbar.
     * @param index saved-hotbar index, from 0 to 8
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeInventory restoreHotbar(int index) {
        CreativeModeInventoryScreen.handleHotbarLoadOrSave(Minecraft.getInstance(), index, true, false);
        return this;
    }

    /**
     * Returns the item stacks stored in a saved hotbar.
     * @param index saved-hotbar index, from 0 to 8
     * @return the saved hotbar's item stacks
     * @since 1.8.4
     */
    public List<ItemStackHelper> getSavedHotbar(int index) {
        HotbarManager hotbarStorage = mc.getHotbarManager();
        return hotbarStorage.get(index).load(Objects.requireNonNull(mc.getConnection()).registryAccess()).stream().map(ItemStackHelper::new).collect(Collectors.toList());
    }

    /**
     * Tests whether a menu slot is one of the player's nine hotbar slots or the offhand slot.
     * @param slot menu slot index
     * @return {@code true} for hotbar slots {@code 36..44} or offhand slot {@code 45}; {@code false} otherwise
     * @since 1.8.4
     */
    public boolean isInHotbar(int slot) {
        return InventoryMenu.isHotbarSlot(slot);
    }

    /**
     * @return the item in creative-menu slot {@code 40}
     * @since 1.8.4
     */
    public ItemStackHelper getOffhand() {
        return getSlot(40);
    }

    /**
     * @return the item in creative-menu slot {@code 39}
     * @since 1.8.4
     */
    public ItemStackHelper getHelmet() {
        return getSlot(39);
    }

    /**
     * @return the item in creative-menu slot {@code 38}
     * @since 1.8.4
     */
    public ItemStackHelper getChestplate() {
        return getSlot(38);
    }

    /**
     * @return the item in creative-menu slot {@code 37}
     * @since 1.8.4
     */
    public ItemStackHelper getLeggings() {
        return getSlot(37);
    }

    /**
     * @return the item in creative-menu slot {@code 36}
     * @since 1.8.4
     */
    public ItemStackHelper getBoots() {
        return getSlot(36);
    }

    /**
     * Returns the diagnostic label {@code CreativeInventory:{}}.
     *
     * @return the fixed diagnostic label {@code CreativeInventory:{}}
     */
    @Override
    public String toString() {
        return String.format("CreativeInventory:{}");
    }

}
