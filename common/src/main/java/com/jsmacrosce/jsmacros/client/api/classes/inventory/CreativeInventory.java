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
import com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinCreativeInventoryScreen;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class CreativeInventory extends Inventory<CreativeModeInventoryScreen> {

    private final CreativeModeInventoryScreen.ItemPickerMenu handler;

    protected CreativeInventory(CreativeModeInventoryScreen inventory) {
        super(inventory);
        this.handler = inventory.getMenu();
    }

    /**
     * The total scroll value is always clamp between 0 and 1.
     *
     * @param amount the amount to scroll by, between -1 and 1
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeInventory scroll(double amount) {
        scrollTo((float) (((MixinCreativeInventoryScreen) inventory).getScrollPosition() + amount));
        return this;
    }

    /**
     * The total scroll value is always clamp between 0 and 1.
     *
     * @param position the position to scroll to, between 0 and 1
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
     * @return a list of all shown items.
     * @since 1.8.4
     */
    public List<ItemStackHelper> getShownItems() {
        return handler.items.stream().map(ItemStackHelper::new).collect(Collectors.toList());
    }

    /**
     * @param search the string to search for
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
     * Select the search tab.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeInventory selectSearch() {
        selectTab(CreativeModeTabs.searchTab());
        return this;
    }

    /**
     * Select the inventory tab.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeInventory selectInventory() {
        CreativeModeTabs.allTabs().stream().filter(e -> e.getType().equals(CreativeModeTab.Type.INVENTORY)).findFirst().ifPresent(this::selectTab);
        return this;
    }

    /**
     * Select the tab where the hotbars are stored.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeInventory selectHotbar() {
        CreativeModeTabs.allTabs().stream().filter(e -> e.getType().equals(CreativeModeTab.Type.HOTBAR)).findFirst().ifPresent(this::selectTab);
        return this;
    }

    /**
     * @param tabName the name of the tab to select
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeInventory selectTab(String tabName) {
        //TODO detect if translatable and use translate id instead
        selectTab(CreativeModeTabs.allTabs().stream().filter(e -> e.getDisplayName().getString().equals(tabName)).findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid tab name")));
        return this;
    }

    public List<String> getTabNames() {
        return CreativeModeTabs.allTabs().stream().map(e -> e.getDisplayName().getString()).collect(Collectors.toList());
    }

    public List<TextHelper> getTabTexts() {
        return CreativeModeTabs.allTabs().stream().map(e -> TextHelper.wrap(e.getDisplayName())).collect(Collectors.toList());
    }

    private CreativeInventory selectTab(CreativeModeTab group) {
        mc.execute(() -> ((MixinCreativeInventoryScreen) inventory).invokeSetSelectedTab(group));
        return this;
    }

    /**
     * Destroys the currently held item.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeInventory destroyHeldItem() {
        handler.setCarried(ItemStack.EMPTY);
        return this;
    }

    /**
     * Destroys all items in the player's inventory.
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
     * @param stack the item stack to drag
     * @return self for chaining.
     * @see RegistryHelper#getItemStack(String, String)
     * @since 1.8.4
     */
    public CreativeInventory setCursorStack(ItemStackHelper stack) {
        handler.setCarried(stack.getRaw());
        return this;
    }

    /**
     * @param slot  the slot to insert the item into
     * @param stack the item stack to insert
     * @return self for chaining.
     * @see RegistryHelper#getItemStack(String, String)
     * @since 1.8.4
     */
    public CreativeInventory setStack(int slot, ItemStackHelper stack) {
        Minecraft.getInstance().gameMode.handleCreativeModeItemAdd(stack.getRaw(), slot);
        return this;
    }

    /**
     * @param index the index to save the hotbar to, from 0 to 8
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeInventory saveHotbar(int index) {
        CreativeModeInventoryScreen.handleHotbarLoadOrSave(Minecraft.getInstance(), index, false, true);
        return this;
    }

    /**
     * @param index the index to save the hotbar to, from 0 to 8
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeInventory restoreHotbar(int index) {
        CreativeModeInventoryScreen.handleHotbarLoadOrSave(Minecraft.getInstance(), index, true, false);
        return this;
    }

    /**
     * @param index the index to save the hotbar to, from 0 to 8
     * @return a list of all items in the saved hotbar.
     * @since 1.8.4
     */
    public List<ItemStackHelper> getSavedHotbar(int index) {
        HotbarManager hotbarStorage = mc.getHotbarManager();
        return hotbarStorage.get(index).load(Objects.requireNonNull(mc.getConnection()).registryAccess()).stream().map(ItemStackHelper::new).collect(Collectors.toList());
    }

    /**
     * @param slot the slot to check
     * @return {@code true} if the slot is in the hotbar or the offhand slot, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isInHotbar(int slot) {
        return InventoryMenu.isHotbarSlot(slot);
    }

    /**
     * @return the item in the offhand.
     * @since 1.8.4
     */
    public ItemStackHelper getOffhand() {
        return getSlot(40);
    }

    /**
     * @return the equipped helmet item.
     * @since 1.8.4
     */
    public ItemStackHelper getHelmet() {
        return getSlot(39);
    }

    /**
     * @return the equipped chestplate item.
     * @since 1.8.4
     */
    public ItemStackHelper getChestplate() {
        return getSlot(38);
    }

    /**
     * @return the equipped leggings item.
     * @since 1.8.4
     */
    public ItemStackHelper getLeggings() {
        return getSlot(37);
    }

    /**
     * @return the equipped boots item.
     * @since 1.8.4
     */
    public ItemStackHelper getBoots() {
        return getSlot(36);
    }

    @Override
    public String toString() {
        return String.format("CreativeInventory:{}");
    }

}
