package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletDeclareType;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.doclet.DocletReplaceTypeParams;
import com.jsmacrosce.jsmacros.api.math.Pos2D;
import com.jsmacrosce.jsmacros.client.JsMacros;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.access.IHorseScreen;
import com.jsmacrosce.jsmacros.client.access.IInventory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.library.impl.FClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Wagyourtail
 * @since 1.0.8
 */
@SuppressWarnings("unused")
public class Inventory<T extends AbstractContainerScreen<?>> {
    protected T inventory;
    protected AbstractContainerMenu handler;
    protected Map<String, int[]> map;
    protected final MultiPlayerGameMode man;
    protected final int syncId;
    protected final LocalPlayer player;
    protected static Minecraft mc = Minecraft.getInstance();

    public static Inventory<?> create() {
        Inventory<?> inv = create(mc.screen);
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

    /**
     * @param slot
     * @return
     * @since 1.5.0
     */
    public Inventory<T> click(int slot) {
        click(slot, 0);
        return this;
    }

    /**
     * Clicks a slot with a mouse button.~~if the slot is a container, it will click the first slot in the container
     *
     * @param slot
     * @param mousebutton
     * @return
     * @since 1.0.8
     */
    @DocletReplaceParams("slot: int, mousebutton: Trit")
    public Inventory<T> click(int slot, int mousebutton) {
        ClickType act = mousebutton == 2 ? ClickType.CLONE : ClickType.PICKUP;
        man.handleInventoryMouseClick(syncId, slot, mousebutton, act, player);
        return this;
    }

    /**
     * Does a drag-click with a mouse button. (the slots don't have to be in order or even adjacent, but when vanilla minecraft calls the underlying function they're always sorted...)
     *
     * @param slots
     * @param mousebutton
     * @return
     */
    @DocletReplaceParams("slots: int[], mousebutton: Bit")
    public Inventory<T> dragClick(int[] slots, int mousebutton) {
        mousebutton = mousebutton == 0 ? 1 : 5;
        man.handleInventoryMouseClick(syncId, -999, mousebutton - 1, ClickType.QUICK_CRAFT, player); // start drag click
        for (int i : slots) {
            man.handleInventoryMouseClick(syncId, i, mousebutton, ClickType.QUICK_CRAFT, player);
        }
        man.handleInventoryMouseClick(syncId, -999, mousebutton + 1, ClickType.QUICK_CRAFT, player);
        return this;
    }

    /**
     * @param slot
     * @since 1.5.0
     */
    public Inventory<T> dropSlot(int slot) {
        man.handleInventoryMouseClick(syncId, slot, 0, ClickType.THROW, player);
        return this;
    }

    /**
     * @param slot  the slot to drop
     * @param stack decide whether to drop the whole stack or just a single item
     * @return self for chaining.
     * @since 1.8.4
     */
    public Inventory<T> dropSlot(int slot, boolean stack) {
        man.handleInventoryMouseClick(syncId, slot, stack ? 1 : 0, ClickType.THROW, player);
        return this;
    }

    /**
     * @param item the item to check for
     * @return {@code true} if the item is contined anywhere in the inventory, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean contains(ItemStackHelper item) {
        return getItems().stream().anyMatch(stack -> stack.equals(item));
    }

    /**
     * @param item the item to check for
     * @return {@code true} if the item is contined anywhere in the inventory, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    @DocletReplaceParams("item: ItemId")
    public boolean contains(String item) {
        return getItems().stream().anyMatch(stack -> stack.getItemId().equals(item));
    }

    /**
     * @return the first empty slot in the main inventory or {@code -1} if there are no empty
     * slots.
     * @since 1.8.4
     */
    public int findFreeInventorySlot() {
        return findFreeSlot("main", "hotbar");
    }

    /**
     * @return the first empty hotbar slot or {@code -1} if there are no empty slots.
     * @since 1.8.4
     */
    public int findFreeHotbarSlot() {
        return findFreeSlot("hotbar");
    }

    /**
     * @param mapIdentifiers the identifier of the inventory sections to check
     * @return the first empty slot in the given inventory sections, or {@code -1} if there are no
     * empty slots.
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
     * @return a map of all item ids and their total count inside the inventory.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaMap<ItemId, number>")
    public Map<String, Integer> getItemCount() {
        Object2IntOpenHashMap<String> itemMap = new Object2IntOpenHashMap<>();
        getItems().stream().filter(i -> !i.isEmpty()).forEach(item -> itemMap.addTo(item.getItemId(), item.getCount()));
        return itemMap;
    }

    /**
     * @return a list of all items in the inventory.
     * @since 1.8.4
     */
    public List<ItemStackHelper> getItems() {
        return IntStream.range(0, getTotalSlots()).mapToObj(this::getSlot).filter(i -> !i.isEmpty()).collect(Collectors.toList());
    }

    /**
     * @param mapIdentifiers the inventory sections
     * @return a list of all items in the given inventory sections.
     * @since 1.8.4
     */
    @DocletReplaceParams("...mapIdentifiers: JavaVarArgs<InvMapId>")
    public List<ItemStackHelper> getItems(String... mapIdentifiers) {
        return Arrays.stream(getSlots(mapIdentifiers)).mapToObj(this::getSlot).filter(i -> !i.isEmpty()).collect(Collectors.toList());
    }

    /**
     * @param item the item to search for
     * @return all slots containing the given item.
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
     * @param item the item to search for
     * @return all slots containing the given item.
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
     * @param mapIdentifiers the inventory sections
     * @return all slots indexes in the given inventory sections.
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
     * @return the index of the selected hotbar slot.
     * @since 1.2.5
     */
    public int getSelectedHotbarSlotIndex() {
        return player.getInventory().getSelectedSlot();
    }

    /**
     * @param index
     * @since 1.2.5
     */
    public void setSelectedHotbarSlotIndex(int index) {
        if (net.minecraft.world.entity.player.Inventory.isHotbarSlot(index)) {
            player.getInventory().setSelectedSlot(index);
        }
    }

    /**
     * closes the inventory, (if the inventory/container is visible it will close the gui). also drops any "held on mouse" items.
     *
     * @return
     */
    public Inventory<T> closeAndDrop() {
        ItemStack held = handler.getCarried();
        if (!held.isEmpty()) {
            man.handleInventoryMouseClick(syncId, -999, 0, ClickType.PICKUP, player);
        }
        close();
        return this;
    }

    /**
     * Closes the inventory, and open gui if applicable.
     */
    public void close() {
        mc.execute(player::closeContainer);
        this.inventory = null;
        this.handler = null;
    }

    /**
     * simulates a shift-click on a slot.
     * It should be safe to chain these without {@link FClient#waitTick()} at least for a bunch of the same item.
     *
     * @param slot
     * @return
     */
    public Inventory<T> quick(int slot) {
        man.handleInventoryMouseClick(syncId, slot, 0, ClickType.QUICK_MOVE, player);
        return this;
    }

    /**
     * @param slot
     * @return
     * @since 1.7.0
     */
    public int quickAll(int slot) {
        return quickAll(slot, 0);
    }

    /**
     * quicks all that match the slot
     *
     * @param slot   a slot from the section you want to move items from
     * @param button
     * @return number of items that matched
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
                man.handleInventoryMouseClick(syncId, slot2.index, button, ClickType.QUICK_MOVE, player);
            }
        }
        return count;
    }

    /**
     * @return the held (by the mouse) item.
     */
    public ItemStackHelper getHeld() {
        return new ItemStackHelper(handler.getCarried());
    }

    /**
     * @param slot
     * @return the item in the slot.
     */
    public ItemStackHelper getSlot(int slot) {
        return new ItemStackHelper(this.handler.getSlot(slot).getItem());
    }

    /**
     * @return the size of the container/inventory.
     */
    public int getTotalSlots() {
        return this.handler.slots.size();
    }

    /**
     * Splits the held stack into two slots. can be alternatively done with {@link Inventory#dragClick(int[], int)} if this one has issues on some servers.
     *
     * @param slot1
     * @param slot2
     * @return
     * @throws Exception
     */
    public Inventory<T> split(int slot1, int slot2) throws Exception {
        if (slot1 == slot2) {
            throw new Exception("must be 2 different slots.");
        }
        if (!getSlot(slot1).isEmpty() || !getSlot(slot2).isEmpty()) {
            throw new Exception("slots must be empty.");
        }
        man.handleInventoryMouseClick(syncId, slot1, 1, ClickType.PICKUP, player);
        man.handleInventoryMouseClick(syncId, slot2, 0, ClickType.PICKUP, player);
        return this;
    }

    /**
     * Does that double click thingy to turn a incomplete stack pickup into a complete stack pickup if you have more in your inventory.
     *
     * @param slot
     * @return
     */
    public Inventory<T> grabAll(int slot) {
        man.handleInventoryMouseClick(syncId, slot, 0, ClickType.PICKUP, player);
        man.handleInventoryMouseClick(syncId, slot, 0, ClickType.PICKUP_ALL, player);
        return this;
    }

    /**
     * swaps the items in two slots.
     *
     * @param slot1
     * @param slot2
     * @return
     * @deprecated use {@link Inventory#swapHotbar(int, int)} or write it yourself instead. This method is not reliable on servers due to timing issues.
     */
    @Deprecated
    public Inventory<T> swap(int slot1, int slot2) {
        boolean is1 = getSlot(slot1).isEmpty();
        boolean is2 = getSlot(slot2).isEmpty();
        if (is1 && is2) {
            return this;
        }
        if (!is1) {
            man.handleInventoryMouseClick(syncId, slot1, 0, ClickType.PICKUP, player);
        }
        man.handleInventoryMouseClick(syncId, slot2, 0, ClickType.PICKUP, player);
        if (!is2) {
            man.handleInventoryMouseClick(syncId, slot1, 0, ClickType.PICKUP, player);
        }
        return this;
    }

    /**
     *
     * equivalent to hitting the numbers or f for swapping slots to hotbar
     *
     * @param slot
     * @param hotbarSlot 0-8 or 40 for offhand
     * @return
     * @since 1.6.5 [citation needed]
     */
    @DocletReplaceParams("slot: int, hotbarSlot: HotbarSwapSlot")
    public Inventory<T> swapHotbar(int slot, int hotbarSlot) {
        if (hotbarSlot != 40) {
            if (hotbarSlot < 0 || hotbarSlot > 8) {
                throw new IllegalArgumentException("hotbarSlot must be between 0 and 8 or 40 for offhand.");
            }
        }
        man.handleInventoryMouseClick(syncId, slot, hotbarSlot, ClickType.SWAP, player);
        return this;
    }

    /**
     * @since 1.2.8
     */
    public void openGui() {
        mc.execute(() -> mc.setScreen(this.inventory));
    }

    /**
     * @return the id of the slot under the mouse.
     * @since 1.1.3
     */
    public int getSlotUnderMouse() {
        Minecraft mc = Minecraft.getInstance();
        double x = mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
        double y = mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();
        if (this.inventory != mc.screen) {
            throw new RuntimeException("Inventory screen is not open.");
        }
        Slot s = ((IInventory) this.inventory).jsmacros_getSlotUnder(x, y);
        if (s == null) {
            return -999;
        }
        return this.handler.slots.indexOf(s);
    }

    /**
     * @return the part of the mapping the slot is in.
     * @since 1.1.3
     */
    @DocletReplaceReturn("ScreenName")
    public String getType() {
        return JsMacrosClient.getScreenName(this.inventory);
    }

    /**
     * checks if this inventory type equals to any of the specified types<br>
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
     * @return the inventory mappings different depending on the type of open container/inventory.
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
     * @param slotNum
     * @return returns the part of the mapping the slot is in.
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
     * @return the x/y position of the specified slot index
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
                AbstractHorse h = (AbstractHorse) ((IHorseScreen) this.inventory).jsmacros_getEntity();
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
     * @return {@code true} if the inventory is a container, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isContainer() {
        return inventory instanceof ContainerScreen || inventory instanceof DispenserScreen || inventory instanceof HopperScreen || inventory instanceof ShulkerBoxScreen;
    }

    /**
     * @return
     * @since 1.2.3
     */
    public String getContainerTitle() {
        return this.inventory.getTitle().getString();
    }

    @DocletReplaceReturn("IScreen")
    public T getRawContainer() {
        return this.inventory;
    }

    @Override
    public String toString() {
        return String.format("Inventory:{\"Type\": \"%s\"}", this.getType());
    }

    /**
     * @return
     * @since 1.6.0
     */
    public int getCurrentSyncId() {
        return syncId;
    }

}
