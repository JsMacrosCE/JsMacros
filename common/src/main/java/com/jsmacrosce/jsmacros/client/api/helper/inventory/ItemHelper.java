package com.jsmacrosce.jsmacros.client.api.helper.inventory;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.*;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Describes an item type, independently of any particular stack; use {@link ItemStackHelper} for
 * stack-specific state. Obtain an item from
 * {@link com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper#getItem(String)}.
 * <pre>
 * const item = Client.getRegistryManager().getItem("minecraft:diamond_sword");
 * const stack = item.getDefaultStack();
 * Chat.log(`${item.getId()}: max stack ${item.getMaxCount()}, current count ${stack.getCount()}`);
 * </pre>
 * <pre>
 * const item = Client.getRegistryManager().getItem("minecraft:diamond_sword");
 * Chat.log(`${item.getId()} is called ${item.getName()}`);
 * Chat.log(`stack of ${item.getMaxCount()}, a tool: ${item.isTool()}, a block item: ${item.isBlockItem()}`);
 *
 * // what it is made of, and what it repairs with
 * Chat.log(`enchantability ${item.getEnchantability()}, max durability ${item.getMaxDurability()}`);
 * Chat.log(`repairs with cobblestone: ${item.canBeRepairedWith(
 *   Client.getRegistryManager().getItemStack("minecraft:cobblestone"))}`);
 *
 * // and a stack of one to work with
 * const stack = item.getDefaultStack();
 * Chat.log(`${stack.getCount()} of ${stack.getItemId()}`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Items/Enchantments")
@SuppressWarnings("unused")
public class ItemHelper extends BaseHelper<Item> {
    private static final Minecraft mc = Minecraft.getInstance();

    /** Wraps the supplied raw item type.
     * @param base raw item type to wrap
     */
    public ItemHelper(Item base) {
        super(base);
    }

    private Stream<CreativeModeTab> getGroups() {
        return CreativeModeTabs.allTabs().parallelStream().filter(group -> !group.isAlignedRight() && group.getDisplayItems().parallelStream().anyMatch(e -> e.is(base)));
    }

    /**
     * Returns the display names of creative tabs containing this item, or an empty list if none.
     *
     * @return the display names of the creative tabs this item is in, empty if it is in none.
     * @since 1.8.4
     */
    public List<TextHelper> getCreativeTab() {
        return getGroups().map(CreativeModeTab::getDisplayName).map(TextHelper::wrap).collect(Collectors.toList());
    }

    /**
     * Returns the icon stacks for creative tabs containing this item, in the same order as
     * {@link #getCreativeTab()}.
     *
     * @return the icon stacks for the tabs this item is in, or {@code null}.
     * @since 1.8.4
     */
    @Nullable
    public List<ItemStackHelper> getGroupIcon() {
        return getGroups() == null ? null : getGroups().map(CreativeModeTab::getIconItem).map(ItemStackHelper::new).collect(Collectors.toList());
    }

    /**
     * Whether the given stack can be used to repair stacks of this item at an anvil. The item's own
     * data is the list of materials the game accepts, and the question is whether this stack is one
     * of them.<br>
     * An item with no repairable component returns {@code false}.
     * <pre>
     * // what a tool can be repaired with
     * const reg = Client.getRegistryManager();
     * const sword = reg.getItem("minecraft:diamond_sword");
     * for (const id of ["minecraft:cobblestone", "minecraft:iron_ingot", "minecraft:diamond", "minecraft:dirt"]) {
     *   Chat.log(`${id}: ${sword.canBeRepairedWith(reg.getItemStack(id))}`);
     * }
     * </pre>
     *
     * @param stack the possible repair material
     * @return {@code true} if the given item stack can be used to repair item stacks of this item,
     * {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean canBeRepairedWith(ItemStackHelper stack) {
        var repair = base.components().get(DataComponents.REPAIRABLE);
        return repair != null && repair.isValidRepairItem(stack.getRaw());
    }

    /**
     * Checks whether this item is a correct tool for drops against the block's default state.
     *
     * @param block the block to check
     * @return {@code true} if this item is a correct tool for drops for the default state;
     * {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSuitableFor(BlockHelper block) {
        return base.isCorrectToolForDrops(base.getDefaultInstance(), block.getDefaultState().getRaw());
    }

    /**
     * Checks whether this item is a correct tool for drops against this specific block state.
     *
     * @param block the block to check
     * @return {@code true} if this item is a correct tool for drops for this state; {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isSuitableFor(BlockStateHelper block) {
        return base.isCorrectToolForDrops(base.getDefaultInstance(), block.getRaw());
    }

    /**
     * Whether this item is a block item; see {@link #getBlock()} for its associated block.
     *
     * @return {@code true} if the item has a block representation, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isBlockItem() {
        return base instanceof BlockItem;
    }

    /**
     * Returns the block associated with this block item, or {@code null} if this item is not a
     * block item.
     *
     * @return the block representation of this item or {@code null} if this item has no
     * corresponding block.
     * @since 1.8.4
     */
    @Nullable
    public BlockHelper getBlock() {
        if (isBlockItem()) {
            return new BlockHelper(((BlockItem) base).getBlock());
        }
        return null;
    }

    /**
     * Returns this item's mining-speed multiplier for the given block state. Without tool data, the
     * multiplier is {@code 1}.
     *
     * @param state the block state to check
     * @return the mining-speed multiplier; {@code 1} when this item has no tool data.
     * @since 1.8.4
     */
    public float getMiningSpeedMultiplier(BlockStateHelper state) {
        // At least in vanilla the item stack is never used
        return base.getDestroySpeed(base.getDefaultInstance(), state.getRaw());
    }

    /**
     * Whether this item's default stack is damageable.
     *
     * @return {@code true} if the item has durability, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isDamageable() {
        return base.getDefaultInstance().isDamageableItem();
    }

    /**
     * Whether this item has a crafting remainder.
     *
     * @return {@code true} if this item has a crafting remainder; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasRecipeRemainder() {
        //? if >=26.1 {
        /*return base.getCraftingRemainder() != null;
        *///? } else {
        return !base.getCraftingRemainder().isEmpty();
        //? }
    }

    /**
     * Returns this item's crafting remainder, or {@code null} if it has none.
     *
     * @return the remainder as a one-count item stack, or {@code null} if none exists.
     * @since 1.8.4
     */
    @Nullable
    public ItemStackHelper getRecipeRemainder() {
        //? if >=26.1 {
        /*ItemStackTemplate remainder = base.getCraftingRemainder();
        if (remainder == null) {
            return null;
        }
        return new ItemStackHelper(remainder.create());
        *///? } else {
        ItemStack remainder = base.getCraftingRemainder();
        if (remainder.isEmpty()) {
            return null;
        }
        return new ItemStackHelper(remainder);
        //? }
    }

    /**
     * Returns this item's enchantability component value, or {@code 0} if it has no such component.
     *
     * @return this item's enchantability value, or {@code 0} if absent.
     * @since 1.8.4
     */
    public int getEnchantability() {
        var enchant = base.components().get(DataComponents.ENCHANTABLE);
        if (enchant != null) {
            return enchant.value();
        }
        return 0;
    }

    /**
     * Returns this item's name as a string in the current language. For a stack's custom name, see
     * {@link ItemStackHelper#getName()}.
     *
     * @return the name of this item, translated to the current language.
     * @since 1.8.4
     */
    public String getName() {
        return base.getName(base.getDefaultInstance()).getString();
    }

    /**
     * Returns this item's full registry identifier.
     *
     * @return the identifier of this item.
     * @since 1.8.4
     */
    @DocletReplaceReturn("ItemId")
    public String getId() {
        return BuiltInRegistries.ITEM.getKey(base).toString();
    }

    /**
     * Returns this item's default maximum stack size; items without a configured size default to
     * {@code 1}.
     *
     * @return the maximum amount of items in a stack of this item.
     * @since 1.8.4
     */
    public int getMaxCount() {
        return base.getDefaultMaxStackSize();
    }

    /**
     * Returns this item's maximum damage value, or {@code 0} if no maximum is configured.
     *
     * @return the maximum damage value.
     * @since 1.8.4
     */
    public int getMaxDurability() {
        return base.components().getOrDefault(DataComponents.MAX_DAMAGE, 0);
    }

    /**
     * Whether this item's damage-resistance component targets the fire damage-type tag. Items with
     * no damage-resistance component return {@code false}.
     *
     * @return {@code true} if fire damage is resisted according to the component; {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isFireproof() {
        var types = base.components().get(DataComponents.DAMAGE_RESISTANT);
        if (types == null) {
            return false;
        }
        //? if >=26.1 {
        /*return types.types().unwrapKey().filter(DamageTypeTags.IS_FIRE::equals).isPresent();
        *///? } else {
        return DamageTypeTags.IS_FIRE.equals(types.types());
        //? }
    }

    /**
     * Whether this item has a tool component.
     *
     * @return {@code true} if this item is a tool, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isTool() {
        return base.components().get(DataComponents.TOOL) != null;
    }

    /**
     * Whether this item has an equippable component.
     *
     * @return {@code true} if this item has an equippable component; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isWearable() {
        return base.components().get(DataComponents.EQUIPPABLE) != null;
    }

    /**
     * Whether this item has a food component.
     *
     * @return {@code true} if this item is food, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isFood() {
        return base.components().get(DataComponents.FOOD) != null;
    }

    /**
     * Returns this item's food component, or {@code null} if it has none.
     *
     * @return the food component of this item or {@code null} if this item is not food.
     * @since 1.8.4
     */
    @Nullable
    public FoodComponentHelper getFood() {
        if (isFood()) {
            return new FoodComponentHelper(base.components().get(DataComponents.FOOD));
        }
        return null;
    }

    /**
     * Whether this item is allowed inside container items such as bundles.
     *
     * @return {@code true} if this item can be nested, i.e. put into a bundle or shulker box,
     * {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean canBeNested() {
        return base.canFitInsideContainerItems();
    }

    /**
     * Returns a fresh default stack of this item, with a count of one.
     *
     * @return the default item stack of this item with a stack size of {@code 1}.
     * @since 1.8.4
     */
    public ItemStackHelper getDefaultStack() {
        return new ItemStackHelper(base.getDefaultInstance());
    }

    /**
     * Parses {@code nbt} as the component syntax appended to this item's ID and returns a stack with
     * those components. The stack count is always one.
     * <pre>
     * // a named and damaged tool
     * const sword = Client.getRegistryManager().getItem("minecraft:diamond_sword");
     * const named = sword.getStackWithNbt('[custom_name=\'"Excalibur"\', damage=10]');
     * Chat.log(`${named.getName()} with ${named.getDurability()} durability left`);
     *
     * // an enchanted one
     * const enchanted = sword.getStackWithNbt('[{id:"minecraft:sharpness",lvl:5}]');
     * const found = enchanted.getEnchantment("minecraft:sharpness");
     * Chat.log(found === null ? "no sharpness" : found.getRomanLevelName());
     *
     * // a text that will not parse is an error rather than an empty stack
     * try {
     *   sword.getStackWithNbt("[not_a_component]");
     * } catch (e) {
     *   Chat.log("that component list did not parse");
     * }
     * </pre>
     *
     * @param nbt the component syntax to append to this item's ID.
     * @return a stack with the parsed components and a count of one.
     * @throws CommandSyntaxException if the nbt data is invalid.
     * @since 1.8.4
     */
    public ItemStackHelper getStackWithNbt(String nbt) throws CommandSyntaxException {
        ItemParser reader = new ItemParser(Objects.requireNonNull(mc.getConnection()).registryAccess());
        var itemResult = reader.parse(new StringReader(getId() + nbt));
        //? if >=26.1 {
        /*return new ItemStackHelper(itemResult.createItemStack(1));
        *///? } else {
        return new ItemStackHelper(new ItemStack(itemResult.item(), 1, itemResult.components()));
        //? }
    }

    /** Returns a diagnostic representation containing the wrapped item's identifier.
     * @return diagnostic representation of the wrapped item type
     */
    @Override
    public String toString() {
        return String.format("ItemHelper:{\"id\": \"%s\"}", getId());
    }

}
