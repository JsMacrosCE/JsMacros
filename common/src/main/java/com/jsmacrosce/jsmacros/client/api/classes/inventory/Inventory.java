package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import com.jsmacrosce.jsmacros.client.util.GuiCompat;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
*///? } else {
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
//? }
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.*;
import com.jsmacrosce.jsmacros.api.math.Pos2D;
import com.jsmacrosce.jsmacros.client.JsMacros;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.access.IAbstractMountInventoryScreen;
import com.jsmacrosce.jsmacros.client.access.IInventory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.library.impl.FClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

//? if >=26.1 {
/*import net.minecraft.world.inventory.ContainerInput;
*///? } else {
import net.minecraft.world.inventory.ClickType;
//? }

/**
 * Wraps a container screen's menu; specialized subclasses represent recognized screen types.
 * Use {@code instanceof Inventories.Crafting} and the other {@code Inventories} names to branch
 * on those wrapper types; {@link #getType()} returns the screen name used for identification.
 * Slot indices refer to the menu's full slot list. {@link #getMap()} groups applicable indices
 * by section name; available sections depend on the screen.
 * <pre>
 * const inv = Player.openInventory();
 * // which kind of container is this?
 * Chat.log(`this is ${inv.getType()}`);
 * // the named sections, rather than slot numbers that move per screen
 * Chat.log(`main: ${inv.getSlots("main")}`);
 * Chat.log(`hotbar: ${inv.getSlots("hotbar")}`);
 * // count what is in there, without touching any slot
 * const counts = inv.getItemCount();
 * for (const id of counts.keySet()) {
 *   Chat.log(`${id} x ${counts.get(id)}`);
 * }
 * </pre>
 * @param <T> concrete container screen type wrapped by this inventory
 *
 * @author Wagyourtail
 * @since 1.0.8
 */
@DocletCategory("Inventory")
@SuppressWarnings("unused")
public class Inventory<T extends AbstractContainerScreen<?>> {
    private enum InventoryAction {
        PICKUP,
        CLONE,
        QUICK_CRAFT,
        THROW,
        QUICK_MOVE,
        PICKUP_ALL,
        SWAP
    }

    protected T inventory;
    protected AbstractContainerMenu handler;
    protected Map<String, int[]> map;
    protected final MultiPlayerGameMode man;
    protected final int syncId;
    protected final LocalPlayer player;
    protected static Minecraft mc = Minecraft.getInstance();

    /**
     * Wraps the current container screen, or the player's inventory if no container screen is open.
     * The fallback is a synthetic screen wrapper, so {@link #getSlotUnderMouse()} cannot be used
     * until its screen is current.
     *
     * @return a wrapper for the open container or the player's inventory
     * @since 1.0.8
     */
    public static Inventory<?> create() {
        Inventory<?> inv = create(GuiCompat.screen(mc));
        // TODO: What to do with horses? The horse inventory would need to be opened with a packet
        if (inv == null) {
            assert mc.player != null;
            if (mc.player.getAbilities().instabuild) {
                return new CreativeInventory(new CreativeModeInventoryScreen(mc.player, mc.level.enabledFeatures(), mc.player.canUseGameMasterBlocks()));
            }
            return new PlayerInventory(new InventoryScreen(mc.player));
        }
        return inv;
    }

    /**
     * Wraps a container screen without falling back to another inventory. Recognized screen types
     * receive specialized wrappers; other container screens receive a base wrapper.
     *
     * @param s the screen to describe
     * @return a handler for the container behind it, or {@code null} if it is not a container
     *         screen
     * @since 1.0.8
     */
    @Nullable
    public static Inventory<?> create(@Nullable Screen s) {
        if (s instanceof AbstractContainerScreen) {
            if (s instanceof MerchantScreen) {
                return new VillagerInventory((MerchantScreen) s);
            } else if (s instanceof EnchantmentScreen) {
                return new EnchantInventory((EnchantmentScreen) s);
            } else if (s instanceof LoomScreen) {
                return new LoomInventory((LoomScreen) s);
            } else if (s instanceof BeaconScreen) {
                return new BeaconInventory((BeaconScreen) s);
            } else if (s instanceof AnvilScreen) {
                return new AnvilInventory((AnvilScreen) s);
            } else if (s instanceof BrewingStandScreen) {
                return new BrewingStandInventory((BrewingStandScreen) s);
            } else if (s instanceof CartographyTableScreen) {
                return new CartographyInventory((CartographyTableScreen) s);
            } else if (s instanceof FurnaceScreen) {
                return new FurnaceInventory((FurnaceScreen) s);
            } else if (s instanceof GrindstoneScreen) {
                return new GrindStoneInventory((GrindstoneScreen) s);
            } else if (s instanceof SmithingScreen) {
                return new SmithingInventory((SmithingScreen) s);
            } else if (s instanceof StonecutterScreen) {
                return new StoneCutterInventory((StonecutterScreen) s);
            } else if (s instanceof CraftingScreen) {
                return new CraftingInventory((CraftingScreen) s);
            } else if (s instanceof InventoryScreen) {
                return new PlayerInventory((InventoryScreen) s);
            } else if (s instanceof CreativeModeInventoryScreen) {
                return new CreativeInventory((CreativeModeInventoryScreen) s);
            } else if (s instanceof HorseInventoryScreen) {
                return new HorseInventory((HorseInventoryScreen) s);
            } else if (s instanceof ContainerScreen || s instanceof DispenserScreen || s instanceof HopperScreen || s instanceof ShulkerBoxScreen) {
                return new ContainerInventory<>((AbstractContainerScreen<?>) s);
            }
            return new Inventory<>((AbstractContainerScreen<?>) s);
        }
        return null;
    }

    protected Inventory(T inventory) {
        this.inventory = inventory;
        this.handler = inventory.getMenu();
        this.player = mc.player;
        assert player != null;
        this.man = mc.gameMode;
        this.syncId = handler.containerId;
    }

    private void clickContainer(int slot, int button, InventoryAction action) {
        //? if >=26.1 {
        /*man.handleContainerInput(syncId, slot, button, switch (action) {
            case PICKUP -> ContainerInput.PICKUP;
            case CLONE -> ContainerInput.CLONE;
            case QUICK_CRAFT -> ContainerInput.QUICK_CRAFT;
            case THROW -> ContainerInput.THROW;
            case QUICK_MOVE -> ContainerInput.QUICK_MOVE;
            case PICKUP_ALL -> ContainerInput.PICKUP_ALL;
            case SWAP -> ContainerInput.SWAP;
        }, player);
        *///? } else {
        man.handleInventoryMouseClick(syncId, slot, button, switch (action) {
            case PICKUP -> ClickType.PICKUP;
            case CLONE -> ClickType.CLONE;
            case QUICK_CRAFT -> ClickType.QUICK_CRAFT;
            case THROW -> ClickType.THROW;
            case QUICK_MOVE -> ClickType.QUICK_MOVE;
            case PICKUP_ALL -> ClickType.PICKUP_ALL;
            case SWAP -> ClickType.SWAP;
        }, player);
        //? }
    }

    /**
     * Performs a primary-button pickup click on a menu slot.
     * @param slot index in the menu's full slot list
     * @return this inventory for chaining
     * @since 1.5.0
     */
    public Inventory<T> click(int slot) {
        click(slot, 0);
        return this;
    }

    /**
     * Performs a pickup click, except button {@code 2} clones the stack.
     *
     * @param slot index in the menu's full slot list
     * @param mouseButton {@code 2} to clone; all other values perform a pickup click
     * @return this inventory wrapper
     * @since 1.0.8
     */
    @DocletReplaceParams("slot: int, mouseButton: Trit")
    public Inventory<T> click(int slot, int mouseButton) {
        clickContainer(slot, mouseButton, mouseButton == 2 ? InventoryAction.CLONE : InventoryAction.PICKUP);
        return this;
    }

    /**
     * Distributes the carried stack across the specified menu slots using quick-craft.
     *
     * @param slots the slots to spread across, which do not have to be adjacent or in order
     * @param mouseButton {@code 0} selects even distribution; any other value selects greedy distribution
     * @return this inventory wrapper
     */
    @DocletReplaceParams("slots: int[], mouseButton: Bit")
    public Inventory<T> dragClick(int[] slots, int mouseButton) {
        int quickCraftType = mouseButton == 0
                ? AbstractContainerMenu.QUICKCRAFT_TYPE_CHARITABLE
                : AbstractContainerMenu.QUICKCRAFT_TYPE_GREEDY;
        // start drag click
        clickContainer(AbstractContainerMenu.SLOT_CLICKED_OUTSIDE, AbstractContainerMenu.getQuickcraftMask(AbstractContainerMenu.QUICKCRAFT_HEADER_START, quickCraftType), InventoryAction.QUICK_CRAFT);
        for (int i : slots) {
            clickContainer(i, AbstractContainerMenu.getQuickcraftMask(AbstractContainerMenu.QUICKCRAFT_HEADER_CONTINUE, quickCraftType), InventoryAction.QUICK_CRAFT);
        }
        clickContainer(AbstractContainerMenu.SLOT_CLICKED_OUTSIDE, AbstractContainerMenu.getQuickcraftMask(AbstractContainerMenu.QUICKCRAFT_HEADER_END, quickCraftType), InventoryAction.QUICK_CRAFT);

        return this;
    }

    /**
     * Drops one item from a menu slot.
     * @param slot index in the menu's full slot list
     * @return this inventory wrapper, for chaining
     * @since 1.5.0
     */
    public Inventory<T> dropSlot(int slot) {
        clickContainer(slot, 0, InventoryAction.THROW);
        return this;
    }

    /**
     * Drops one item or the whole stack from a menu slot.
     * @param slot index in the menu's full slot list
     * @param stack {@code true} to drop the whole stack; {@code false} to drop one item
     * @return this inventory wrapper
     * @since 1.8.4
     */
    public Inventory<T> dropSlot(int slot, boolean stack) {
        clickContainer(slot, stack ? 1 : 0, InventoryAction.THROW);
        return this;
    }

    /**
     * @param item stack to compare against
     * @return {@code true} if any inventory stack equals {@code item}
     * @since 1.8.4
     */
    public boolean contains(ItemStackHelper item) {
        return getItems().stream().anyMatch(stack -> stack.equals(item));
    }

    /**
     * @param item item ID to search for
     * @return {@code true} if any inventory stack has this item ID
     * @since 1.8.4
     */
    @DocletReplaceParams("item: ItemId")
    public boolean contains(String item) {
        return getItems().stream().anyMatch(stack -> stack.getItemId().equals(item));
    }

    /**
     * @return the first empty slot in the {@code main} or {@code hotbar} sections, or {@code -1}
     * if neither section has an empty slot.
     * @since 1.8.4
     */
    public int findFreeInventorySlot() {
        return findFreeSlot("main", "hotbar");
    }

    /**
     * @return the first empty slot in the {@code hotbar} section, or {@code -1} if it has no empty
     * slot.
     * @since 1.8.4
     */
    public int findFreeHotbarSlot() {
        return findFreeSlot("hotbar");
    }

    /**
     * @param mapIdentifiers names of the inventory sections to check; unknown names are ignored
     * @return the first empty slot in the specified sections, or {@code -1} if none is empty
     * @since 1.8.4
     */
    @DocletReplaceParams("...mapIdentifiers: JavaVarArgs<InvMapId>")
    public int findFreeSlot(String... mapIdentifiers) {
        for (int slot : getSlots(mapIdentifiers)) {
            if (getSlot(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * @return item IDs mapped to the sum of their stack counts across non-empty slots
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaMap<ItemId, number>")
    public Map<String, Integer> getItemCount() {
        Object2IntOpenHashMap<String> itemMap = new Object2IntOpenHashMap<>();
        getItems().stream().filter(i -> !i.isEmpty()).forEach(item -> itemMap.addTo(item.getItemId(), item.getCount()));
        return itemMap;
    }

    /**
     * @return non-empty stacks from the menu's full slot list
     * @since 1.8.4
     */
    public List<ItemStackHelper> getItems() {
        return IntStream.range(0, getTotalSlots()).mapToObj(this::getSlot).filter(i -> !i.isEmpty()).collect(Collectors.toList());
    }

    /**
     * @param mapIdentifiers names of the inventory sections to include; unknown names are ignored
     * @return non-empty stacks from the specified sections
     * @since 1.8.4
     */
    @DocletReplaceParams("...mapIdentifiers: JavaVarArgs<InvMapId>")
    public List<ItemStackHelper> getItems(String... mapIdentifiers) {
        return Arrays.stream(getSlots(mapIdentifiers)).mapToObj(this::getSlot).filter(i -> !i.isEmpty()).collect(Collectors.toList());
    }

    /**
     * @param item stack to compare with each slot
     * @return indices of slots whose stacks equal {@code item}
     * @since 1.8.4
     */
    public int[] findItem(ItemStackHelper item) {
        IntList slots = new IntArrayList();
        for (int i = 0; i < getTotalSlots(); i++) {
            if (getSlot(i).equals(item)) {
                slots.add(i);
            }
        }
        return slots.toIntArray();
    }

    /**
     * @param item item ID to match
     * @return indices of slots whose item ID equals {@code item}
     * @since 1.8.4
     */
    @DocletReplaceParams("item: ItemId")
    public int[] findItem(String item) {
        IntList slots = new IntArrayList();
        for (int i = 0; i < getTotalSlots(); i++) {
            if (getSlot(i).getItemId().equals(item)) {
                slots.add(i);
            }
        }
        return slots.toIntArray();
    }

    /**
     * @param mapIdentifiers names of the inventory sections to include; unknown names are ignored
     * @return menu slot indices in the specified sections, in section order
     * @since 1.8.4
     */
    @DocletReplaceParams("...mapIdentifiers: JavaVarArgs<InvMapId>")
    public int[] getSlots(String... mapIdentifiers) {
        Map<String, int[]> map = getMap();
        IntList slots = new IntArrayList();
        for (String key : mapIdentifiers) {
            if (map.containsKey(key)) {
                slots.addAll(IntList.of(map.get(key)));
            }
        }
        return slots.toIntArray();
    }

    /**
     * @return the selected hotbar slot index
     * @since 1.2.5
     */
    public int getSelectedHotbarSlotIndex() {
        return player.getInventory().getSelectedSlot();
    }

    /**
     * Sets the selected hotbar slot; indices outside {@code 0}–{@code 8} are ignored.
     *
     * @param index hotbar slot index
     * @since 1.2.5
     */
    public void setSelectedHotbarSlotIndex(int index) {
        if (net.minecraft.world.entity.player.Inventory.isHotbarSlot(index)) {
            player.getInventory().setSelectedSlot(index);
        }
    }

    /**
     * Drops the entire carried stack outside the menu, then closes the inventory.
     *
     * @return this wrapper, which now refers to the closed inventory
     */
    public Inventory<T> closeAndDrop() {
        ItemStack held = handler.getCarried();
        if (!held.isEmpty()) {
            clickContainer(AbstractContainerMenu.SLOT_CLICKED_OUTSIDE, 0, InventoryAction.PICKUP);
        }
        close();
        return this;
    }

    /**
     * Requests closure of the player's current container on the client thread and immediately clears this wrapper's screen and menu references.
     */
    public void close() {
        mc.execute(player::closeContainer);
        this.inventory = null;
        this.handler = null;
    }

    /**
     * Performs a quick-move click on a menu slot (the usual shift-click action).
     *
     * @param slot index in the menu's full slot list
     * @return this inventory wrapper, for chaining
     */
    public Inventory<T> quick(int slot) {
        clickContainer(slot, 0, InventoryAction.QUICK_MOVE);
        return this;
    }

    /**
     * Quick-moves items matching the selected slot's stack from its backing container.
     *
     * @param slot menu slot index whose stack and backing container determine the matches
     * @return the total stack count in matching slots considered for quick-move
     * @since 1.7.0
     */
    public int quickAll(int slot) {
        return quickAll(slot, 0);
    }

    /**
     * Quick-moves matching stacks from the selected slot's backing container.
     *
     * @param slot menu slot index whose stack and backing container determine the matches
     * @param button mouse button value passed with each quick-move click
     * @return the total stack count in matching slots considered for quick-move
     * @since 1.7.0
     */
    @DocletReplaceParams("slot: int, button: Bit")
    public int quickAll(int slot, int button) {
        int count = 0;
        ItemStack cursorStack = handler.slots.get(slot).getItem().copy();
        net.minecraft.world.Container hoverSlotInv = handler.slots.get(slot).container;
        for (Slot slot2 : handler.slots) {
            if (slot2 != null
                    && slot2.mayPickup(mc.player)
                    && slot2.hasItem()
                    && slot2.container == hoverSlotInv
                    && AbstractContainerMenu.canItemQuickReplace(slot2, cursorStack, true)) {
                count += slot2.getItem().getCount();
                clickContainer(slot2.index, button, InventoryAction.QUICK_MOVE);
            }
        }
        return count;
    }

    /**
     * @return the item stack currently carried by the menu
     */
    public ItemStackHelper getHeld() {
        return new ItemStackHelper(handler.getCarried());
    }

    /**
     * @param slot menu slot index
     * @return the stack in that menu slot
     */
    public ItemStackHelper getSlot(int slot) {
        return new ItemStackHelper(this.handler.getSlot(slot).getItem());
    }

    /**
     * @return the number of slots in the menu's full slot list
     */
    public int getTotalSlots() {
        return this.handler.slots.size();
    }

    /**
     * Performs a secondary-button pickup click on {@code slot1}, then a primary-button pickup click on {@code slot2}.
     * The slots must be distinct and empty, or this method throws an exception. For quick-craft distribution, use {@link Inventory#dragClick(int[], int)}.
     *
     * @param slot1 first empty menu slot
     * @param slot2 second, distinct empty menu slot
     * @return this inventory wrapper
     */
    public Inventory<T> split(int slot1, int slot2) throws Exception {
        if (slot1 == slot2) {
            throw new Exception("must be 2 different slots.");
        }
        if (!getSlot(slot1).isEmpty() || !getSlot(slot2).isEmpty()) {
            throw new Exception("slots must be empty.");
        }
        clickContainer(slot1, 1, InventoryAction.PICKUP);
        clickContainer(slot2, 0, InventoryAction.PICKUP);
        return this;
    }

    /**
     * Picks up the selected slot's stack, then attempts to collect matching items from other menu slots into the carried stack.
     *
     * @param slot slot whose stack to pick up
     * @return this inventory
     */
    public Inventory<T> grabAll(int slot) {
        clickContainer(slot, 0, InventoryAction.PICKUP);
        clickContainer(slot, 0, InventoryAction.PICKUP_ALL);
        return this;
    }

    /**
     * Attempts to swap two slots using successive pickup clicks; this sequence may be unreliable on servers.
     *
     * @param slot1 first slot
     * @param slot2 second slot
     * @return this inventory
     * @deprecated Use {@link Inventory#swapHotbar(int, int)} for hotbar swaps; this method may be unreliable on servers.
     */
    @Deprecated
    @DocletIgnore
    public Inventory<T> swap(int slot1, int slot2) {
        boolean is1 = getSlot(slot1).isEmpty();
        boolean is2 = getSlot(slot2).isEmpty();
        if (is1 && is2) {
            return this;
        }
        if (!is1) {
            clickContainer(slot1, 0, InventoryAction.PICKUP);
        }
        clickContainer(slot2, 0, InventoryAction.PICKUP);
        if (!is2) {
            clickContainer(slot1, 0, InventoryAction.PICKUP);
        }
        return this;
    }

    /**
     * Swaps a menu slot with a hotbar slot or the offhand slot.
     *
     * @param slot menu slot to swap
     * @param hotbarSlot hotbar index from {@code 0} to {@code 8}, or {@code 40} for offhand
     * @return this inventory
     * @since 1.6.4
     */
    @DocletReplaceParams("slot: int, hotbarSlot: HotbarSwapSlot")
    public Inventory<T> swapHotbar(int slot, int hotbarSlot) {
        if (hotbarSlot != 40) {
            if (hotbarSlot < 0 || hotbarSlot > 8) {
                throw new IllegalArgumentException("hotbarSlot must be between 0 and 8 or 40 for offhand.");
            }
        }
        clickContainer(slot, hotbarSlot, InventoryAction.SWAP);
        return this;
    }

    /**
     * Opens this inventory's screen on the Minecraft client thread.
     *
     * @since 1.2.8
     */
    public void openGui() {
        mc.execute(() -> GuiCompat.setScreen(mc, this.inventory));
    }

    /**
     * @return the menu slot index under the mouse, or {@code -999} if the pointer is outside all slots; throws if this inventory's screen is not open
     * @since 1.1.3
     */
    public int getSlotUnderMouse() {
        Minecraft mc = Minecraft.getInstance();
        double x = mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
        double y = mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();
        if (this.inventory != GuiCompat.screen(mc)) {
            throw new RuntimeException("Inventory screen is not open.");
        }
        Slot s = ((IInventory) this.inventory).jsmacros_getSlotUnder(x, y);
        if (s == null) {
            return AbstractContainerMenu.SLOT_CLICKED_OUTSIDE;
        }
        return this.handler.slots.indexOf(s);
    }

    /**
     * @return the screen name used to identify this inventory type
     * @since 1.1.3
     */
    @DocletReplaceReturn("ScreenName")
    public String getType() {
        return JsMacrosClient.getScreenName(this.inventory);
    }

    /**
     * Checks whether this inventory's screen name matches any supplied name.
     *
     * @param types screen names to test
     * @return {@code true} if the screen name matches one of the supplied names
     * @since 1.9.0
     */
    @SuppressWarnings("SpellCheckingInspection")
    @DocletReplaceTypeParams("T extends ScreenName")
    @DocletReplaceParams("...anyOf: JavaVarArgs<T>")
    @DocletReplaceReturn("this is T extends keyof InvNameToTypeMap ? InvNameToTypeMap[keyof InvNameToTypeMap] extends InvNameToTypeMap[T] ? Inventory : InvNameToTypeMap[T] : this")
    @DocletDeclareType(name = "InvNameToTypeMap", type =
            """
            {
                '1 Row Chest': ContainerInventory;
                '2 Row Chest': ContainerInventory;
                '3 Row Chest': ContainerInventory;
                '4 Row Chest': ContainerInventory;
                '5 Row Chest': ContainerInventory;
                '6 Row Chest': ContainerInventory;
                '7 Row Chest': ContainerInventory;
                '8 Row Chest': ContainerInventory;
                '9 Row Chest': ContainerInventory;
                '3x3 Container': ContainerInventory;
                'Anvil': AnvilInventory;
                'Beacon': BeaconInventory;
                'Blast Furnace': FurnaceInventory;
                'Brewing Stand': BrewingStandInventory;
                'Crafting Table': CraftingInventory;
                'Enchanting Table': EnchantInventory;
                'Furnace': FurnaceInventory;
                'Grindstone': GrindStoneInventory;
                'Hopper': ContainerInventory;
                'Loom': LoomInventory;
                'Villager': VillagerInventory;
                'Shulker Box': ContainerInventory;
                'Smithing Table': SmithingInventory;
                'Smoker': FurnaceInventory;
                'Cartography Table': CartographyInventory;
                'Stonecutter': StoneCutterInventory;
                'Survival Inventory': PlayerInventory;
                'Horse': HorseInventory;
                'Creative Inventory': CreativeInventory;
            }
            """
    )
    public boolean is(String ...types) {
        return Arrays.asList(types).contains(getType());
    }

    /**
     * Returns this inventory's menu slot indices grouped by region name.
     *
     * @return slot indices grouped by region name
     * @since 1.1.3
     */
    @SuppressWarnings("SpellCheckingInspection")
    @DocletReplaceReturn("JavaMap<InvMapId, JavaArray<number>>")
    @DocletDeclareType(name = "InvMapId", type =
            """
            InvMapType.All;
            declare namespace InvMapType {
                type _inv = 'hotbar' | 'main';
                type _invio = _inv | 'input' | 'output';
                type Inventory = _inv | 'offhand' | 'boots' | 'leggings' | 'chestplate' | 'helmet'
                | 'crafting_in' | 'craft_out';
                type CreativeInvInvTab = Exclude<Inventory, 'crafting_in' | 'craft_out'> | 'delete';
                type CreativeInv = 'hotbar' | 'creative';
                type Container        = _inv | 'container';
                type Beacon           = _inv | 'slot';
                type Furnace          = _invio | 'fuel';
                type BrewingStand     = _invio | 'fuel';
                type Crafting         = _invio;
                type Enchantment      = _inv | 'lapis' | 'item';
                type Loom             = _inv | 'output' | 'pattern' | 'dye' | 'banner';
                type Stonecutter      = _invio;
                type Horse            = _inv | 'saddle' | 'armor' | 'container';
                type Anvil            = _invio;
                type Merchant         = _invio;
                type Smithing         = _invio;
                type Grindstone       = _invio;
                type CartographyTable = _invio;
                type All =
                | Inventory
                | CreativeInvInvTab
                | CreativeInv
                | Container
                | Beacon
                | Furnace
                | BrewingStand
                | Crafting
                | Enchantment
                | Loom
                | Stonecutter
                | Horse
                | Anvil
                | Merchant
                | Smithing
                | Grindstone
                | CartographyTable
            }
            """
    )
    public Map<String, int[]> getMap() {
        if (map == null) {
            map = getMapInternal();
        }
        return map;
    }

    /**
     * Returns the region name mapped to a menu slot index.
     *
     * @param slotNum menu slot index
     * @return the region name containing the slot, or {@code null} if it is not mapped
     * @since 1.1.3
     */
    @DocletReplaceReturn("InvMapId | null")
    @Nullable
    public String getLocation(int slotNum) {
        if (map == null) {
            map = getMapInternal();
        }
        for (String k : map.keySet()) {
            for (int i : map.get(k)) {
                if (i == slotNum) {
                    return k;
                }
            }
        }
        return null;
    }

    /**
     * Returns the inventory screen's top-left position, not the position of the specified slot.
     *
     * @param slot menu slot index; accessing it validates the index and fails if it is out of range
     * @return the screen's top-left position
     * @since 1.8.4
     */
    public Pos2D getSlotPos(int slot) {
        Slot s = handler.getSlot(slot);
        return new Pos2D(((IInventory) inventory).jsmacros$getX(), ((IInventory) inventory).jsmacros$getY());
    }

    private Map<String, int[]> getMapInternal() {
        Map<String, int[]> map = new HashMap<>();
        int slots = getTotalSlots();
        if (this.inventory instanceof InventoryScreen || (this.inventory instanceof CreativeModeInventoryScreen && ((CreativeModeInventoryScreen) this.inventory).isInventoryOpen())) {
            if (this.inventory instanceof CreativeModeInventoryScreen) {
                --slots;
            }
            map.put("hotbar", JsMacros.range(slots - 10, slots - 1)); // range(36, 45);
            map.put("offhand", new int[]{slots - 1}); // range(45, 46);
            map.put("main", JsMacros.range(slots - 10 - 27, slots - 10)); // range(9, 36);
            map.put("boots", new int[]{slots - 10 - 27 - 1}); // range(8, 9);
            map.put("leggings", new int[]{slots - 10 - 27 - 2}); // range(7, 8);
            map.put("chestplate", new int[]{slots - 10 - 27 - 3}); // range(6, 7);
            map.put("helmet", new int[]{slots - 10 - 27 - 4}); // range(5, 6);
            map.put("crafting_in", JsMacros.range(slots - 10 - 27 - 4 - 4, slots - 10 - 27 - 4)); // range(1, 5);
            map.put("craft_out", new int[]{slots - 10 - 27 - 4 - 4 - 1});
            if (this.inventory instanceof CreativeModeInventoryScreen) {
                map.put("delete", new int[]{46});
                map.remove("crafting_in");
                map.remove("craft_out");
            }
        } else {
            map.put("hotbar", JsMacros.range(slots - 9, slots));
            map.put("main", JsMacros.range(slots - 9 - 27, slots - 9));
            if (inventory instanceof CreativeModeInventoryScreen) {
                map.remove("main");
                map.put("creative", JsMacros.range(slots - 9));
            } else if (isContainer()) {
                map.put("container", JsMacros.range(slots - 9 - 27));
            } else if (inventory instanceof BeaconScreen) {
                map.put("slot", new int[]{slots - 9 - 27 - 1});
            } else if (inventory instanceof BlastFurnaceScreen || inventory instanceof FurnaceScreen || inventory instanceof SmokerScreen) {
                map.put("output", new int[]{slots - 9 - 27 - 1});
                map.put("fuel", new int[]{slots - 9 - 27 - 2});
                map.put("input", new int[]{slots - 9 - 27 - 3});
            } else if (inventory instanceof BrewingStandScreen) {
                map.put("fuel", new int[]{slots - 9 - 27 - 1});
                map.put("input", new int[]{slots - 9 - 27 - 2});
                map.put("output", JsMacros.range(slots - 9 - 27 - 2));
            } else if (inventory instanceof CraftingScreen) {
                map.put("input", JsMacros.range(slots - 9 - 27 - 9, slots - 9 - 27));
                map.put("output", new int[]{slots - 9 - 27 - 10});
            } else if (inventory instanceof EnchantmentScreen) {
                map.put("lapis", new int[]{slots - 9 - 27 - 1});
                map.put("item", new int[]{slots - 9 - 27 - 2});
            } else if (inventory instanceof LoomScreen) {
                map.put("output", new int[]{slots - 9 - 27 - 1});
                map.put("pattern", new int[]{slots - 9 - 27 - 2});
                map.put("dye", new int[]{slots - 9 - 27 - 3});
                map.put("banner", new int[]{slots - 9 - 27 - 4});
            } else if (inventory instanceof StonecutterScreen) {
                map.put("output", new int[]{slots - 9 - 27 - 1});
                map.put("input", new int[]{slots - 9 - 27 - 2});
            } else if (inventory instanceof HorseInventoryScreen) {
                AbstractHorse h = (AbstractHorse) ((IAbstractMountInventoryScreen) this.inventory).jsmacros_getEntity();
                if (h.canUseSlot(EquipmentSlot.SADDLE)) {
                    map.put("saddle", new int[]{0});
                }
                if (h.canUseSlot(EquipmentSlot.BODY)) {
                    map.put("armor", new int[]{1});
                }
                if (h instanceof AbstractChestedHorse && ((AbstractChestedHorse) h).hasChest()) {
                    map.put("container", JsMacros.range(2, slots - 9 - 27));
                }
            } else if (inventory instanceof AnvilScreen || inventory instanceof MerchantScreen || inventory instanceof SmithingScreen || inventory instanceof GrindstoneScreen || inventory instanceof CartographyTableScreen) {
                map.put("output", new int[]{slots - 9 - 27 - 1});
                map.put("input", JsMacros.range(slots - 9 - 27 - 1));
            }
        }

        return map;
    }

    /**
     * @return {@code true} for container, dispenser, hopper, or shulker-box screens; {@code false} otherwise
     * @since 1.8.4
     */
    public boolean isContainer() {
        return inventory instanceof ContainerScreen || inventory instanceof DispenserScreen || inventory instanceof HopperScreen || inventory instanceof ShulkerBoxScreen;
    }

    /**
     * @return the screen title as plain text
     * @since 1.2.3
     */
    public String getContainerTitle() {
        return this.inventory.getTitle().getString();
    }

    /**
     * Returns the screen wrapped by this inventory, or {@code null} after {@link #close()}.
     * <p>
     * Unlike the methods around it this one carries no {@code @since}: the version it arrived in
     * is not recorded anywhere, so it is left unstated rather than guessed. [citation needed]
     *
     * @return the wrapped screen
     */
    @DocletReplaceReturn("IScreen")
    public T getRawContainer() {
        return this.inventory;
    }

    /**
     * Returns a diagnostic label containing this inventory's current screen name from {@link #getType()}.
     * The result is not JSON serialization; the screen name is inserted without JSON escaping.
     *
     * @return a label in the form {@code Inventory:{"Type": "screen name"}}
     */
    @Override
    public String toString() {
        return String.format("Inventory:{\"Type\": \"%s\"}", this.getType());
    }

    /**
     * @return the menu's synchronization ID
     * @since 1.6.0
     */
    public int getCurrentSyncId() {
        return syncId;
    }

}
