package com.jsmacrosce.jsmacros.client.api.classes.types;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.inventory.*;
import com.jsmacrosce.jsmacros.core.library.ScriptNamespace;

/**
 * Lets scripts identify inventory wrappers through the {@code Inventories} global,
 * instead of matching screen-name strings.
 * <pre>
 * const inv = Player.openInventory();
 * if (inv instanceof Inventories.Villager) {
 *   Chat.log(inv.getTrades());
 * }
 * </pre>
 */
@ScriptNamespace("Inventories")
@DocletCategory("Script Type Namespaces")
@SuppressWarnings("rawtypes")
public final class Inventories {
    private Inventories() {
    }

    public static final Class<Inventory> Inventory = Inventory.class;
    public static final Class<RecipeInventory> Recipe = RecipeInventory.class;
    public static final Class<ContainerInventory> Container = ContainerInventory.class;
    public static final Class<PlayerInventory> Player = PlayerInventory.class;
    public static final Class<CreativeInventory> Creative = CreativeInventory.class;
    public static final Class<VillagerInventory> Villager = VillagerInventory.class;
    public static final Class<AnvilInventory> Anvil = AnvilInventory.class;
    public static final Class<BeaconInventory> Beacon = BeaconInventory.class;
    public static final Class<BrewingStandInventory> BrewingStand = BrewingStandInventory.class;
    public static final Class<CartographyInventory> Cartography = CartographyInventory.class;
    public static final Class<CraftingInventory> Crafting = CraftingInventory.class;
    public static final Class<EnchantInventory> Enchant = EnchantInventory.class;
    public static final Class<FurnaceInventory> Furnace = FurnaceInventory.class;
    public static final Class<GrindStoneInventory> GrindStone = GrindStoneInventory.class;
    public static final Class<HorseInventory> Horse = HorseInventory.class;
    public static final Class<LoomInventory> Loom = LoomInventory.class;
    public static final Class<SmithingInventory> Smithing = SmithingInventory.class;
    public static final Class<StoneCutterInventory> StoneCutter = StoneCutterInventory.class;
}
