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
 * A kind of item rather than a stack of one: diamond is one of these, and so is every stack of
 * diamonds that has ever existed. It is what the registry hands out and what
 * {@link ItemStackHelper#getItem()} gives back, and it knows the item's own data without needing a
 * stack or a world.<br>
 * The distinction that runs through this class is between the item and a stack of it. Anything that
 * is really about a particular stack, such as the damage on it or the enchantments on it, is on
 * {@link ItemStackHelper} and not here. What is here is what every stack of this item shares: the
 * stack size, whether it is food, which block it places, what it repairs with, how hard it is.
 * <p>
 * A script builds one from
 * {@link com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper#getItem(String)}, which is the
 * usual way in.
 * example:
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

    public ItemHelper(Item base) {
        super(base);
    }

    private Stream<CreativeModeTab> getGroups() {
        return CreativeModeTabs.allTabs().parallelStream().filter(group -> !group.isAlignedRight() && group.getDisplayItems().parallelStream().anyMatch(e -> e.is(base)));
    }

    /**
     * The names of the creative tabs this item appears in, as a list. An item can be in more than
     * one, and an item that is in none gives an empty list rather than {@code null}, so the size of
     * what comes back is the test.
     * example:
     * <pre>
     * // which creative tabs an item is in
     * const item = Client.getRegistryManager().getItem("minecraft:oak_planks");
     * const tabs = item.getCreativeTab();
     * for (let i = 0; i !== tabs.size(); i += 1) {
     *   Chat.log(`${item.getId()} is in ${tabs.get(i).getString()}`);
     * }
     *
     * // an item that is in none
     * Chat.log(Client.getRegistryManager().getItem("minecraft:air").getCreativeTab().size());
     * </pre>
     *
     * @return the display names of the creative tabs this item is in, empty if it is in none.
     * @since 1.8.4
     */
    public List<TextHelper> getCreativeTab() {
        return getGroups().map(CreativeModeTab::getDisplayName).map(TextHelper::wrap).collect(Collectors.toList());
    }

    /**
     * The icon stacks for the creative tabs this item is in, one per tab. The tab names are in
     * {@link #getCreativeTab()} and the two are in the same order.<br>
     * An item in no tab gives an empty list rather than {@code null}, despite what the nullable
     * annotation on the signature suggests.
     * example:
     * <pre>
     * // the icon of each tab an item is in
     * const item = Client.getRegistryManager().getItem("minecraft:diamond");
     * const icons = item.getGroupIcon();
     * const names = item.getCreativeTab();
     * for (let i = 0; i !== icons.size(); i += 1) {
     *   const icon = icons.get(i);
     *   Chat.log(`${names.get(i).getString()}: ${icon.getItemId()} x${icon.getCount()}`);
     * }
     * </pre>
     *
     * @return the icon stacks for the tabs this item is in, empty if it is in none.
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
     * example:
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
     * Whether the given block drops its items when broken with this item. This is asked of the
     * block's default state, so for a block that comes in variants it is the answer for whichever
     * one the game treats as the plain form.
     * example:
     * <pre>
     * // which blocks a tool is the right one for
     * const pick = Client.getRegistryManager().getItem("minecraft:diamond_pickaxe");
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:stone", "minecraft:diamond_ore", "minecraft:oak_log", "minecraft:dirt"]) {
     *   Chat.log(`${id}: ${pick.isSuitableFor(reg.getBlock(id))}`);
     * }
     * </pre>
     *
     * @param block the block to check
     * @return {@code true} if the given block can be mined and drops when broken with this item,
     * {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSuitableFor(BlockHelper block) {
        return base.isCorrectToolForDrops(base.getDefaultInstance(), block.getDefaultState().getRaw());
    }

    /**
     * Whether the given block state drops its items when broken with this item, which is the form
     * that asks about a particular variant rather than about the block in general.
     * example:
     * <pre>
     * // a particular state rather than the block
     * const pick = Client.getRegistryManager().getItem("minecraft:diamond_pickaxe");
     * const log = Client.getRegistryManager().getBlockState("minecraft:oak_log", "[axis=x]");
     * Chat.log(`a sideways log: ${pick.isSuitableFor(log)}`);
     * </pre>
     *
     * @param block the block to check
     * @return {@code true} if the given block can be mined and drops when broken with this item,
     * {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSuitableFor(BlockStateHelper block) {
        return base.isCorrectToolForDrops(base.getDefaultInstance(), block.getRaw());
    }

    /**
     * Whether this item places a block when it is used, which is what decides whether
     * {@link #getBlock()} has anything to answer. An item with no block form gives {@code null}
     * from that call.
     * example:
     * <pre>
     * // items that place a block, and the block they place
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:stone", "minecraft:diamond", "minecraft:torch"]) {
     *   const item = reg.getItem(id);
     *   if (item.isBlockItem()) {
     *     const block = item.getBlock();
     *     Chat.log(`${id} places ${block === null ? "nothing" : block.getId()}`);
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the item has a block representation, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isBlockItem() {
        return base instanceof BlockItem;
    }

    /**
     * The block this item places, or {@code null} if the item has no block form.
     * {@link #isBlockItem()} is the test for whether there is one, and this is the block.
     * example:
     * <pre>
     * // the block behind a block item
     * const item = Client.getRegistryManager().getItem("minecraft:oak_slab");
     * if (item.isBlockItem()) {
     *   const block = item.getBlock();
     *   if (block !== null) {
     *     Chat.log(`${item.getId()} places ${block.getId()}`);
     *     // and the block's own properties
     *     Chat.log(`hardness ${block.getHardness()}, tags ${block.getTags().size()}`);
     *   }
     * }
     * </pre>
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
     * How fast this item mines the given block state, as the multiplier the game uses. The value
     * comes off the item's own tool data, and an item with no tool data of its own gives
     * {@code 1}, which is the ordinary speed of a bare hand.
     * example:
     * <pre>
     * // mining speed against a block
     * const reg = Client.getRegistryManager();
     * const stone = reg.getBlockState("minecraft:stone");
     * for (const id of ["minecraft:wooden_pickaxe", "minecraft:iron_pickaxe", "minecraft:diamond_pickaxe"]) {
     *   Chat.log(`${id}: ${reg.getItem(id).getMiningSpeedMultiplier(stone)}`);
     * }
     *
     * // and an item with no tool data of its own, which is the default speed
     * Chat.log(`a stick: ${reg.getItem("minecraft:stick").getMiningSpeedMultiplier(stone)}`);
     * </pre>
     *
     * @param state the block state to check
     * @return the mining speed of this item against the given block state, returns {@code 1} by
     * default.
     * @since 1.8.4
     */
    public float getMiningSpeedMultiplier(BlockStateHelper state) {
        // At least in vanilla the item stack is never used
        return base.getDestroySpeed(base.getDefaultInstance(), state.getRaw());
    }

    /**
     * Whether this item loses durability as it is used. A tool answers true, and so does armour, and
     * an item with no durability at all answers false.
     * example:
     * <pre>
     * // what wears out
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:diamond_sword", "minecraft:iron_helmet", "minecraft:stick", "minecraft:golden_apple"]) {
     *   const item = reg.getItem(id);
     *   Chat.log(`${id}: damageable ${item.isDamageable()}, max durability ${item.getMaxDurability()}`);
     * }
     * </pre>
     *
     * @return {@code true} if the item has durability, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isDamageable() {
        return base.getDefaultInstance().isDamageableItem();
    }

    /**
     * Whether crafting with this item leaves something behind. A bucket does, and a milk bucket
     * does, and most items do not. The question is about the item itself, so it does not depend on
     * what else is in the grid.
     * example:
     * <pre>
     * // what is left over after crafting
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:bucket", "minecraft:milk_bucket", "minecraft:stick", "minecraft:water_bucket"]) {
     *   const item = reg.getItem(id);
     *   if (item.hasRecipeRemainder()) {
     *     const rest = item.getRecipeRemainder();
     *     Chat.log(`${id} leaves ${rest === null ? "nothing" : rest.getItemId()}`);
     *   } else {
     *     Chat.log(`${id} leaves nothing`);
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if when crafter the item stack has a remainder, {@code false}
     * otherwise.
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
     * What is left behind after crafting with this item, or {@code null} if there is nothing.
     * This is the item's own remainder rather than a stack of it, so it comes back with a count of
     * one and no damage on it.<br>
     * {@link #hasRecipeRemainder()} is the test for whether there is one at all.
     * example:
     * <pre>
     * // the remainder, where there is one
     * const item = Client.getRegistryManager().getItem("minecraft:bucket");
     * const rest = item.getRecipeRemainder();
     * if (rest === null) {
     *   Chat.log("no remainder");
     * } else {
     *   Chat.log(`${item.getId()} leaves ${rest.getItemId()} x${rest.getCount()}`);
     * }
     * </pre>
     *
     * @return the recipe remainder if it exists and {@code null} otherwise.
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
     * The enchantability of this item, which is the number the game weighs an enchantment's own
     * weight against when it is deciding what to put on something. An item with no enchantability
     * data of its own gives {@code 0}, and that is the value an enchanting table treats as
     * enchantable at all.
     * example:
     * <pre>
     * // what is worth enchanting
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:wooden_sword", "minecraft:iron_sword", "minecraft:diamond_sword", "minecraft:stick"]) {
     *   Chat.log(`${id}: ${reg.getItem(id).getEnchantability()}`);
     * }
     *
     * // and which of a set are worth enchanting at all
     * const wanted = ["minecraft:golden_apple", "minecraft:iron_pickaxe", "minecraft:book"];
     * for (const id of wanted) {
     *   const value = reg.getItem(id).getEnchantability();
     *   if (value > 0) {
     *     Chat.log(`${id} can be enchanted`);
     *   }
     * }
     * </pre>
     *
     * With increased enchantability the change to get more and better enchantments increases.
     *
     * @return the enchantability of this item, returns {@code 0} by default.
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
     * The item's translated name, which is the word the game shows for the kind of item. It is a
     * string rather than a text because the item's own name is never a custom one; a renamed stack
     * is a different thing and is on {@link ItemStackHelper#getName()}.
     * example:
     * <pre>
     * // the names of a few items
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:diamond_sword", "minecraft:oak_planks", "minecraft:water_bucket"]) {
     *   Chat.log(`${id}: ${reg.getItem(id).getName()}`);
     * }
     * </pre>
     *
     * @return the name of this item, translated to the current language.
     * @since 1.8.4
     */
    public String getName() {
        return base.getName(base.getDefaultInstance()).getString();
    }

    /**
     * The item's registry id, in full. This is the same string
     * {@link com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper#getItem(String)} takes, so it
     * can be handed straight back to look the item up again.
     * example:
     * <pre>
     * const reg = Client.getRegistryManager();
     * const item = reg.getItem("diamond_sword");
     * Chat.log(item.getId());
     * // the id comes back in full even though it was asked for short
     * Chat.log(reg.getItem(item.getId()).getId());
     * </pre>
     *
     * @return the identifier of this item.
     * @since 1.8.4
     */
    @DocletReplaceReturn("ItemId")
    public String getId() {
        return BuiltInRegistries.ITEM.getKey(base).toString();
    }

    /**
     * The largest stack this item can be in, as the game has it set. An item with no stack size of
     * its own gives {@code 1} here, since that is what the game falls back to.
     * example:
     * <pre>
     * // the stack sizes
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:dirt", "minecraft:ender_pearl", "minecraft:shovel", "minecraft:bucket"]) {
     *   Chat.log(`${id}: ${reg.getItem(id).getMaxCount()}`);
     * }
     *
     * // and a stack of the largest size
     * const item = reg.getItem("minecraft:dirt");
     * const stack = item.getDefaultStack().getCreative().setCount(item.getMaxCount());
     * Chat.log(`${stack.getCount()} of ${stack.getItemId()}`);
     * </pre>
     *
     * @return the maximum amount of items in a stack of this item.
     * @since 1.8.4
     */
    public int getMaxCount() {
        return base.getDefaultMaxStackSize();
    }

    /**
     * The most damage this item can take before it breaks, as the number the game keeps on it. An
     * item with no durability of its own gives {@code 0} here, which is the same answer
     * {@link #isDamageable()} gives as a false.
     * example:
     * <pre>
     * // how tough the armour and tools are
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:wooden_sword", "minecraft:stone_sword", "minecraft:iron_sword", "minecraft:netherite_sword"]) {
     *   const item = reg.getItem(id);
     *   Chat.log(`${id}: ${item.getMaxDurability()} of ${item.getMaxDurability()}`);
     * }
     *
     * // and what a fresh stack of one has left
     * const sword = reg.getItem("minecraft:diamond_sword").getDefaultStack();
     * Chat.log(`${sword.getDurability()} durability on a new one`);
     * </pre>
     *
     * The damage an item has taken is the opposite of the durability still left.
     *
     * @return the maximum amount of damage this item can take.
     * @since 1.8.4
     */
    public int getMaxDurability() {
        return base.components().getOrDefault(DataComponents.MAX_DAMAGE, 0);
    }

    /**
     * Whether this item is immune to fire damage, which is the flag the game checks before it burns
     * a held or worn item. An item with no resistance data of its own gives {@code false}.
     * example:
     * <pre>
     * // what survives lava
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:netherite_helmet", "minecraft:leather_helmet", "minecraft:diamond", "minecraft:wooden_sword"]) {
     *   Chat.log(`${id} is fireproof: ${reg.getItem(id).isFireproof()}`);
     * }
     * </pre>
     *
     * @return {@code true} if this item is fireproof, {@code false} otherwise.
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
     * Whether this item is a tool, which is the flag the game sets on mining and fighting tools.
     * It is a property of the item rather than a guess from what the item is made of, and
     * {@link #getMiningSpeedMultiplier(BlockStateHelper)} reads the same data.
     * example:
     * <pre>
     * // which items are tools
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:diamond_pickaxe", "minecraft:shears", "minecraft:stick", "minecraft:bow"]) {
     *   Chat.log(`${id} is a tool: ${reg.getItem(id).isTool()}`);
     * }
     * </pre>
     *
     * @return {@code true} if this item is a tool, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isTool() {
        return base.components().get(DataComponents.TOOL) != null;
    }

    /**
     * Whether this item can be worn in an armour slot, which is the flag the game checks when it is
     * deciding whether an equippable slot accepts a stack.
     * example:
     * <pre>
     * // what can be worn
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:diamond_helmet", "minecraft:leather_boots", "minecraft:elytra", "minecraft:diamond"]) {
     *   Chat.log(`${id} is wearable: ${reg.getItem(id).isWearable()}`);
     * }
     * </pre>
     *
     * @return {@code true} if this item can be worn in the armor slot, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isWearable() {
        return base.components().get(DataComponents.EQUIPPABLE) != null;
    }

    /**
     * Whether this item can be eaten, which is the flag the game sets on anything with a food
     * component. {@link #getFood()} is what reads that component.
     * example:
     * <pre>
     * // what is food
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:bread", "minecraft:cooked_beef", "minecraft:poisonous_potato", "minecraft:stone"]) {
     *   Chat.log(`${id} is food: ${reg.getItem(id).isFood()}`);
     * }
     * </pre>
     *
     * @return {@code true} if this item is food, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isFood() {
        return base.components().get(DataComponents.FOOD) != null;
    }

    /**
     * The food component of this item, or {@code null} if the item is not food. This is the whole
     * component rather than one number, so the three things eating it does are all on it.
     * example:
     * <pre>
     * // what eating an item does
     * const item = Client.getRegistryManager().getItem("minecraft:golden_apple");
     * const food = item.getFood();
     * if (food === null) {
     *   Chat.log("not food");
     * } else {
     *   Chat.log(`${food.getHunger()} hunger, ${food.getSaturation()} saturation`);
     *   Chat.log(`edible on a full bar: ${food.isAlwaysEdible()}`);
     * }
     * </pre>
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
     * Whether this item can go inside a container item, which is the flag the game checks when it is
     * deciding whether a stack fits in a bundle or a shulker box. It is set per item and the game
     * answers yes for anything that has not said otherwise, so this is true for most items rather
     * than being a shortlist.
     * example:
     * <pre>
     * // what can be put in a bundle
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:dirt", "minecraft:stone", "minecraft:enchanted_book", "minecraft:bundle"]) {
     *   Chat.log(`${id} nests: ${reg.getItem(id).canBeNested()}`);
     * }
     * </pre>
     *
     * @return {@code true} if this item can be nested, i.e. put into a bundle or shulker box,
     * {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean canBeNested() {
        return base.canFitInsideContainerItems();
    }

    /**
     * A fresh stack of one of this item with nothing on it, which is what a creative inventory entry
     * is. A stack with anything on it, such as a custom name or damage, is built with
     * {@link #getStackWithNbt(String)} or by editing the result.
     * example:
     * <pre>
     * // a fresh stack of one
     * const item = Client.getRegistryManager().getItem("minecraft:stone");
     * const stack = item.getDefaultStack();
     * Chat.log(`${stack.getCount()} of ${stack.getItemId()}, damage ${stack.getDamage()}`);
     *
     * // and a full stack of the same thing
     * const many = item.getDefaultStack().getCreative().setCount(64);
     * Chat.log(`${many.getCount()} of ${many.getItemId()}`);
     * </pre>
     *
     * @return the default item stack of this item with a stack size of {@code 1}.
     * @since 1.8.4
     */
    public ItemStackHelper getDefaultStack() {
        return new ItemStackHelper(base.getDefaultInstance());
    }

    /**
     * A stack of this item carrying whatever components the argument names, read by the same parser
     * a command uses. The argument is appended straight onto this item's id, so it is the
     * component list and nothing else: square brackets, a comma separated set of component names
     * with their values, and the braces for a nested list where a component takes one.
     * <p>
     * The stack always comes back with a count of one, whatever the components say.
     * example:
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
     * @param nbt the nbt data of the item stack, which is the component list and nothing else.
     * @return the item stack of this item with a stack size of {@code 1} and the given nbt.
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

    @Override
    public String toString() {
        return String.format("ItemHelper:{\"id\": \"%s\"}", getId());
    }

}
