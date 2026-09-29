package com.jsmacrosce.jsmacros.client.api.helper.inventory;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Style;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper;
import com.jsmacrosce.jsmacros.client.api.helper.BlockPredicateHelper;
import com.jsmacrosce.jsmacros.client.api.helper.NBTElementHelper;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinBlockPredicatesChecker;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A stack of an item: what it is, how many, and everything the game has written onto it. This is
 * what an inventory slot holds, what {@link ItemHelper} counts, and what most item work in a script
 * starts from.<br>
 * The line between this and {@link ItemHelper} is the stack against the kind. Anything that is the
 * same for every stack of an item, such as the stack size or whether it is food, is on the item;
 * anything a particular stack can differ on, such as its name, its damage, its enchantments or its
 * lore, is here.<br>
 * A stack is a value rather than a handle: reading it never changes it, and the comparisons at the
 * bottom of the class are the ways to ask whether two stacks are the same. To build one up rather
 * than only read it, {@link #getCreative()} wraps the same stack with setters on it.
 * <p>
 * A script builds a stack from
 * {@link com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper#getItemStack(String)} with or
 * without components, or gets one from an entity with
 * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.LivingEntityHelper#getMainHand()}.
 * example:
 * <pre>
 * // a stack, and the things worth knowing about it
 * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_sword", `[{id:"minecraft:sharpness",lvl:5}]`);
 * Chat.log(`${stack.getCount()} of ${stack.getName().getString()}`);
 * Chat.log(`${stack.getItemId()}, ${stack.getDurability()} of ${stack.getMaxDurability()} durability`);
 *
 * // the enchantments on it, with their levels
 * for (const ench of stack.getEnchantments()) {
 *   Chat.log(ench.getRomanLevelName().getString());
 * }
 *
 * // and the two ways of asking whether two stacks match
 * const fresh = Client.getRegistryManager().getItemStack("minecraft:diamond_sword");
 * Chat.log(`same item: ${stack.isItemEqual(fresh)}`);
 * Chat.log(`same everything: ${stack.isNBTEqual(fresh)}`);
 * </pre>
 *
 * @author Wagyourtail
 */
@DocletCategory("Items/Enchantments")
@SuppressWarnings("unused")
public class ItemStackHelper extends BaseHelper<ItemStack> {
    private static final Style LORE_STYLE = Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withItalic(true);
    protected static final Minecraft mc = Minecraft.getInstance();

    @DocletReplaceParams("id: CanOmitNamespace<ItemId>, count: int")
    public ItemStackHelper(String id, int count) {
        super(new ItemStack(BuiltInRegistries.ITEM.getValue(RegistryHelper.parseIdentifier(id)), count));
    }

    public ItemStackHelper(ItemStack i) {
        super(i);
    }

    /**
     * Sets the item damage value.
     * You should use {@link CreativeItemStackHelper#setDamage(int)} instead.
     * You may want to use {@link ItemStackHelper#copy()} first.
     * example:
     * <pre>
     * // read the damage, repair the stack, and read it back. copy() first, so the
     * // stack that was handed in is not the one that changed
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_pickaxe");
     * const before = stack.getDamage();
     * stack.copy().getCreative().setDamage(500);
     * Chat.log(`${before} damage, and the original is still at ${stack.getDamage()}`);
     *
     * // the repairable form, which is the one to prefer
     * const fresh = stack.copy().getCreative().setDamage(500);
     * Chat.log(`${fresh.getDurability()} durability left`);
     * </pre>
     *
     * @param damage the damage to set on this stack.
     * @return self
     * @since 1.2.0
     * @deprecated use {@link CreativeItemStackHelper#setDamage(int)} instead, which returns the
     * creative helper so the result can be used directly.
     */
    @Deprecated
    public ItemStackHelper setDamage(int damage) {
        base.setDamageValue(damage);
        return this;
    }

    /**
     * Whether this item can take damage at all, which is a property of the item rather than of this
     * stack. A tool or a piece of armour answers true, and so does a stack of it that has not
     * actually been damaged.
     * example:
     * <pre>
     * // what wears out
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:diamond_pickaxe", "minecraft:leather_chestplate", "minecraft:stone"]) {
     *   const stack = reg.getItemStack(id);
     *   Chat.log(`${id}: damageable ${stack.isDamageable()}, at ${stack.getDamage()} damage`);
     * }
     * </pre>
     *
     * @return {@code true} if this item can be damaged, {@code false} otherwise.
     * @since 1.2.0
     */
    public boolean isDamageable() {
        return base.isDamageableItem();
    }

    /**
     * Whether this stack is unbreakable, which is a flag on the stack rather than a property of the
     * item: the same item can be a stack with the flag and a stack without it. {@link #isUnbreakableHidden()}
     * is about whether the game's tooltip says so, not about whether it is.
     * example:
     * <pre>
     * // an unbreakable copy and an ordinary one of the same item
     * const reg = Client.getRegistryManager();
     * const plain = reg.getItemStack("minecraft:diamond_pickaxe");
     * const forever = reg.getItemStack("minecraft:diamond_pickaxe").getCreative().setUnbreakable(true);
     * Chat.log(`plain ${plain.isUnbreakable()}, unbreakable ${forever.isUnbreakable()}`);
     *
     * // the flag does not change what the item is
     * Chat.log(`${plain.getItemId()} and ${forever.getItemId()}`);
     * </pre>
     *
     * @return {@code true} if this item is unbreakable, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isUnbreakable() {
        return base.get(DataComponents.UNBREAKABLE) != null;
    }

    /**
     * Whether the game's own enchanting can put anything on this stack, which needs two things:
     * the item has to be marked enchantable, and the stack has to have no enchantments on it yet.
     * A stack that already carries an enchantment answers false however enchantable the item is.
     * example:
     * <pre>
     * // which items an enchanting table will work on
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:diamond_sword", "minecraft:book", "minecraft:stone", "minecraft:golden_apple"]) {
     *   Chat.log(`${id} is enchantable: ${reg.getItemStack(id).isEnchantable()}`);
     * }
     * </pre>
     *
     * @return {@code true} if this item can be enchanted, {@code false} otherwise.
     * @since 1.2.0
     */
    public boolean isEnchantable() {
        return base.isEnchantable();
    }

    /**
     * Whether this stack has any enchantment on it, which is the test before reading
     * {@link #getEnchantments()}. This is a question about what is on the stack rather than about
     * the item, so a stack of an item the game has not marked enchantable can still answer true if
     * something has been put on it.
     * example:
     * <pre>
     * // enchanted against not
     * const reg = Client.getRegistryManager();
     * const plain = reg.getItemStack("minecraft:diamond_sword");
     * const enchanted = reg.getItemStack("minecraft:diamond_sword", `[{id:"minecraft:sharpness",lvl:3}]`);
     * Chat.log(`${plain.isEnchanted()} and ${enchanted.isEnchanted()}`);
     * </pre>
     *
     * @return {@code true} if the item is enchanted, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isEnchanted() {
        return base.isEnchanted();
    }

    /**
     * Every enchantment on this stack, each with the level this stack has it at. A stack with none
     * on it gives an empty list rather than {@code null}.<br>
     * The list is built fresh each call, so a script can sort or filter it without touching the
     * stack.
     * example:
     * <pre>
     * // everything on a tool, with the levels
     * const stack = Client.getRegistryManager()
     *   .getItemStack("minecraft:diamond_sword", `[{id:"minecraft:sharpness",lvl:5},{id:"minecraft:unbreaking",lvl:3}]`);
     * for (const ench of stack.getEnchantments()) {
     *   Chat.log(`${ench.getId()} at level ${ench.getLevel()}`);
     * }
     *
     * // and just the curses, which is what a build check wants
     * let curses = 0;
     * for (const ench of stack.getEnchantments()) {
     *   if (ench.isCursed()) {
     *     curses += 1;
     *   }
     * }
     * Chat.log(`${curses} curses on it`);
     * </pre>
     *
     * @return a list of all enchantments on this item.
     * @since 1.8.4
     */
    public List<EnchantmentHelper> getEnchantments() {
        List<EnchantmentHelper> enchantments = new ArrayList<>();
        ItemEnchantments lv = base.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> enchantment : lv.keySet()) {
            enchantments.add(new EnchantmentHelper(enchantment, lv.getLevel(enchantment)));
        }
        return enchantments;
    }

    /**
     * One enchantment on this stack, with the level this stack has it at, or {@code null} if the
     * stack does not carry it.<br>
     * The id may be written with or without its namespace, and the enchantment's translated name is
     * accepted in place of the id as well, so a stack found by the word on its tooltip is the same
     * stack found by the id.
     * example:
     * <pre>
     * // one enchantment off a stack, either way of naming it
     * const stack = Client.getRegistryManager()
     *   .getItemStack("minecraft:diamond_sword", `[{id:"minecraft:sharpness",lvl:5}]`);
     * const sharp = stack.getEnchantment("minecraft:sharpness");
     * if (sharp !== null) {
     *   Chat.log(`level ${sharp.getLevel()}, ${sharp.getRomanLevelName().getString()}`);
     * }
     *
     * // an enchantment that is not on it
     * Chat.log(stack.getEnchantment("minecraft:looting") === null);
     * </pre>
     *
     * @param id the id of the enchantment to check for
     * @return the enchantment instance, containing the level, or {@code null} if the item is not
     * enchanted with the specified enchantment.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: CanOmitNamespace<EnchantmentId>")
    @Nullable
    public EnchantmentHelper getEnchantment(String id) {
        String fullId = RegistryHelper.parseNameSpace(id);
        // name filter stays for backward compatibility
        return getEnchantments().stream().filter(enchantmentHelper -> enchantmentHelper.getId().equals(fullId) || enchantmentHelper.getName().equals(id)).findFirst().orElse(null);
    }

    /**
     * Whether this enchantment could go on this stack, taking into account what is already on it.
     * An enchantment that conflicts with something the stack already carries answers false here,
     * where {@link EnchantmentHelper#canBeApplied(ItemHelper)} only asks about the item type.
     * example:
     * <pre>
     * // whether an enchantment still fits on a stack that already has things on it
     * const reg = Client.getRegistryManager();
     * const stack = reg.getItemStack("minecraft:diamond_sword", `[{id:"minecraft:sharpness",lvl:5}]`);
     * const unbreaking = reg.getEnchantment("minecraft:unbreaking");
     * const looting = reg.getEnchantment("minecraft:looting");
     * Chat.log(`unbreaking fits: ${unbreaking.canBeApplied(stack)}`);
     * Chat.log(`looting fits: ${looting.canBeApplied(stack)}`);
     * </pre>
     *
     * @param enchantment the enchantment to check
     * @return {@code true} if the specified enchantment can be applied to this item, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean canBeApplied(EnchantmentHelper enchantment) {
        return enchantment.canBeApplied(this);
    }

    /**
     * Whether this stack already carries the given enchantment.<br>
     * The level on the helper decides how careful the match is. An enchantment taken off a stack
     * carries a level, and one from the registry carries none, and a helper with no level matches
     * the stack whatever level it is at. Two helpers that both carry a level only match when the
     * levels are the same.
     * example:
     * <pre>
     * // the same enchantment, asked about at a level and without one
     * const reg = Client.getRegistryManager();
     * const stack = reg.getItemStack("minecraft:diamond_sword", `[{id:"minecraft:sharpness",lvl:5}]`);
     * const anyLevel = reg.getEnchantment("minecraft:sharpness");
     * const levelFive = reg.getEnchantment("minecraft:sharpness", 5);
     * const levelThree = reg.getEnchantment("minecraft:sharpness", 3);
     * Chat.log(`any level: ${stack.hasEnchantment(anyLevel)}`);
     * Chat.log(`level five: ${stack.hasEnchantment(levelFive)}`);
     * Chat.log(`level three: ${stack.hasEnchantment(levelThree)}`);
     * </pre>
     *
     * @param enchantment the enchantment to check for
     * @return {@code true} if the item is enchanted with the specified enchantment, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean hasEnchantment(EnchantmentHelper enchantment) {
        return getEnchantments().stream().anyMatch(enchantment::equals);
    }

    /**
     * Whether this stack already carries the given enchantment, named by id rather than by helper.
     * The level is not part of the question here, so any level of that enchantment matches.
     * example:
     * <pre>
     * // a build check over a set of enchantments
     * const stack = Client.getRegistryManager()
     *   .getItemStack("minecraft:diamond_sword", `[{id:"minecraft:sharpness",lvl:5}]`);
     * for (const id of ["minecraft:sharpness", "minecraft:looting", "minecraft:mending"]) {
     *   Chat.log(`${id}: ${stack.hasEnchantment(id)}`);
     * }
     * </pre>
     *
     * @param enchantment the id of the enchantment to check for
     * @return {@code true} if the item is enchanted with the given enchantment, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: CanOmitNamespace<EnchantmentId>")
    public boolean hasEnchantment(String enchantment) {
        String toCheck = RegistryHelper.parseNameSpace(enchantment);
        return getEnchantments().stream().anyMatch(e -> e.getId().equals(toCheck));
    }

    /**
     * Every enchantment in the registry that would go on this stack, with nothing already on the
     * stack taken into account. This walks the whole enchantment registry, so it is worth doing
     * once and keeping rather than inside a loop.
     * example:
     * <pre>
     * // everything that would go on a pickaxe
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_pickaxe");
     * const possible = stack.getPossibleEnchantments();
     * Chat.log(`${possible.size()} enchantments fit this item`);
     * for (const ench of possible) {
     *   Chat.log(`  ${ench.getId()} ${ench.getMinLevel()} to ${ench.getMaxLevel()}`);
     * }
     * </pre>
     *
     * @return a list of all enchantments that can be applied to this item.
     * @since 1.8.4
     */
    public List<EnchantmentHelper> getPossibleEnchantments() {
        return mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
            .filter(enchantment -> enchantment.value().canEnchant(base))
            .map(EnchantmentHelper::new).toList();
    }

    /**
     * The subset of {@link #getPossibleEnchantments()} that an enchanting table would actually
     * offer, which is the ones the game has not marked as coming from somewhere else. A table never
     * offers a curse or a treasure.
     * example:
     * <pre>
     * // what an enchanting table would offer for this item
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_sword");
     * const fromTable = stack.getPossibleEnchantmentsFromTable();
     * const all = stack.getPossibleEnchantments();
     * Chat.log(`${fromTable.size()} from a table out of ${all.size()} in total`);
     * for (const ench of fromTable) {
     *   Chat.log(`  ${ench.getId()}, treasure ${ench.isTreasure()}, cursed ${ench.isCursed()}`);
     * }
     * </pre>
     *
     * @return a list of all enchantments that can be applied to this item through an enchanting table.
     * @since 1.8.4
     */
    public List<EnchantmentHelper> getPossibleEnchantmentsFromTable() {
        return mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.IN_ENCHANTING_TABLE)
            .map(registryEntries -> registryEntries.stream()
                .filter(enchantment -> enchantment.value().canEnchant(base))
                .map(EnchantmentHelper::new).toList())
            .orElse(Collections.emptyList());
    }

    /**
     * The lines of lore on this stack, as a fresh list of text. A stack with no lore gives an empty
     * list rather than {@code null}, and each line is the text the game would show, so it can carry
     * its own formatting.
     * <p>
     * The returned list is a copy of the original list and can be modified without affecting the
     * original item. For editing the actual lore see
     * {@link CreativeItemStackHelper#addLore(Object...)}.
     * example:
     * <pre>
     * // read the lore off a stack
     * const stack = Client.getRegistryManager()
     *   .getItemStack("minecraft:diamond_sword", "[lore=['Forged in the first age']]").getCreative();
     * for (const line of stack.getLore()) {
     *   Chat.log(line.getString());
     *   Chat.log(`  without formatting: ${line.getStringStripFormatting()}`);
     * }
     *
     * // and put some on
     * stack.setLore("First line", "Second line");
     * Chat.log(`${stack.getLore().size()} lines`);
     * </pre>
     *
     * @return a list of all lines of lore on this item.
     * @since 1.8.4
     */
    public List<TextHelper> getLore() {
        List<TextHelper> texts = new ArrayList<>();
        ItemLore component = base.get(DataComponents.LORE);
        if (component != null) {
            component.lines().forEach(text -> texts.add(TextHelper.wrap(text)));
        }
        return texts;
    }

    /**
     * The most damage this item can take before it breaks, which is a property of the item and is
     * the same for every stack of it. {@link #getDamage()} is what this stack has already taken,
     * and {@link #getDurability()} is what is left of this number.
     * example:
     * <pre>
     * // the item's limit against what this stack has used
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_pickaxe").getCreative();
     * stack.setDamage(1000);
     * Chat.log(`${stack.getDamage()} of ${stack.getMaxDurability()}, ${stack.getDurability()} left`);
     *
     * // an item that cannot be damaged gives zero
     * Chat.log(Client.getRegistryManager().getItemStack("minecraft:stone").getMaxDurability());
     * </pre>
     *
     * @return the maximum durability of this item.
     * @since 1.8.4
     */
    public int getMaxDurability() {
        return base.getMaxDamage();
    }

    /**
     * How much durability is left on this stack, which is the item's maximum less what this stack
     * has taken. Two stacks of a damaged item therefore give the same number, since the damage is
     * what is on the stack and not a property of the item.
     * example:
     * <pre>
     * // durability left, and what that works out as a fraction
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_pickaxe").getCreative();
     * stack.setDurability(stack.getMaxDurability());
     * Chat.log(`${stack.getDurability()} of ${stack.getMaxDurability()}`);
     * stack.setDurability(0);
     * Chat.log(`${stack.getDurability()}, which is broken`);
     * </pre>
     *
     * @return the current durability of this item.
     * @since 1.8.4
     */
    public int getDurability() {
        return base.getMaxDamage() - base.getDamageValue();
    }

    /**
     * What this stack costs to repair, in experience levels. A stack that has never been repaired
     * gives {@code 0} here rather than having no answer, so the number itself is the test.
     * example:
     * <pre>
     * // the repair cost, for stacks that have one
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:diamond_pickaxe", "minecraft:netherite_pickaxe"]) {
     *   const stack = reg.getItemStack(id, "[repair_cost=7]");
     *   Chat.log(`${id}: ${stack.getRepairCost()} levels`);
     * }
     * // and for one that has never been repaired
     * Chat.log(reg.getItemStack("minecraft:diamond_pickaxe").getRepairCost());
     * </pre>
     *
     * @return the current repair cost of this item.
     * @since 1.8.4
     */
    public int getRepairCost() {
        Integer i = base.get(DataComponents.REPAIR_COST);
        if (i == null) {
            return 0;
        }
        return i;
    }

    /**
     * The damage this stack has taken, which counts up towards the item's
     * {@link #getMaxDurability()} and is the same thing as the durability {@link #getDurability()}
     * has left, spelled the other way round.
     * example:
     * <pre>
     * // damage and durability are the same number seen from two ends
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_pickaxe").getCreative();
     * stack.setDamage(250);
     * Chat.log(`${stack.getDamage()} taken, ${stack.getDurability()} left of ${stack.getMaxDurability()}`);
     * </pre>
     *
     * @return the damage taken by this item.
     * @since 1.8.4
     */
    public int getDamage() {
        return base.getDamageValue();
    }

    /**
     * The most damage this stack can take before it breaks. This is the stack's own number rather
     * than the item's, so it follows whatever the stack has been set to, and it is the same thing
     * {@link #getMaxDurability()} answers.
     * example:
     * <pre>
     * // the stack's own limit, set on the stack
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_pickaxe");
     * Chat.log(stack.getMaxDamage());
     * Chat.log(`and through the item: ${stack.getItem().getMaxDurability()}`);
     * </pre>
     *
     * @return the maximum damage this item can take.
     * @since 1.8.4
     */
    public int getMaxDamage() {
        return base.getMaxDamage();
    }

    /**
     * The attack damage this stack gives in the player's main hand, worked out from the attribute
     * modifiers on it and from the player's own base value for the attribute. An item with no
     * modifiers of its own gives the player's bare hand number back, so this is never about the
     * item on its own but about the item as the player is holding it.<br>
     * A local player is needed for the base value, so with no world joined this cannot be asked.
     * example:
     * <pre>
     * // what the player would hit for holding this
     * const sword = Client.getRegistryManager().getItemStack("minecraft:diamond_sword");
     * const stick = Client.getRegistryManager().getItemStack("minecraft:stick");
     * const bare = Client.getRegistryManager().getItemStack("minecraft:air");
     * Chat.log(`sword ${sword.getAttackDamage()}, stick ${stick.getAttackDamage()}`);
     * Chat.log(`and with nothing in the hand ${bare.getAttackDamage()}`);
     *
     * // the two spellings of the same question
     * const player = Player.getPlayer();
     * Chat.log(`${player.getMainHand().getItemId()} at ${player.getMainHand().getAttackDamage()}`);
     * </pre>
     *
     * @return the default attack damage of this item.
     * @since 1.8.4
     */
    public double getAttackDamage() {
        assert mc.player != null;
        ItemAttributeModifiers lv = base.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        return lv.compute(
                //? if >=1.21.11 {
                /*Attributes.ATTACK_DAMAGE,
                *///? }
                mc.player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE),
                EquipmentSlot.MAINHAND
        );
    }

    /**
     * The name the game would show for this item if nothing had been set on the stack, which is the
     * item's own name. A stack with a custom name on it gives a different answer here than from
     * {@link #getName()}.
     * example:
     * <pre>
     * // the item's own name against the stack's
     * const stack = Client.getRegistryManager().getItemStack("minecraft:stone").getCreative()
     *   .setName("Foundation Stone");
     * Chat.log(`stack: ${stack.getName().getString()}`);
     * Chat.log(`item: ${stack.getDefaultName().getString()}`);
     * </pre>
     *
     * @return the item's own name, which was a string before 1.6.5.
     * @since 1.2.0
     */
    public TextHelper getDefaultName() {
        return TextHelper.wrap(base.getItem().getName(
                //? if >=26.1 {
                /*base.getItem().getDefaultInstance()
                *///? }
        ));
    }

    /**
     * The name the game shows for this stack, which is a custom name when one has been set on it
     * and the item's own name otherwise. It is text rather than a string because a custom name can
     * carry formatting.
     * example:
     * <pre>
     * // a named stack against an ordinary one
     * const reg = Client.getRegistryManager();
     * const plain = reg.getItemStack("minecraft:stone");
     * const named = reg.getItemStack("minecraft:stone").getCreative().setName("Foundation Stone");
     * Chat.log(`plain: ${plain.getName().getString()}`);
     * Chat.log(`named: ${named.getName().getString()}`);
     * </pre>
     *
     * @return the name shown for this stack, which was a string before 1.6.5.
     * @since 1.2.0
     */
    public TextHelper getName() {
        return TextHelper.wrap(base.getHoverName());
    }

    /**
     * How many of the item are in this stack. This is not capped by the item's own stack size, so a
     * stack built through {@link CreativeItemStackHelper#setCount(int)} can read back more than
     * {@link #getMaxCount()}.
     * example:
     * <pre>
     * // the count of a stack, and the most that will fit in one
     * const stack = Client.getRegistryManager().getItemStack("minecraft:dirt").getCreative().setCount(32);
     * Chat.log(`${stack.getCount()} of ${stack.getMaxCount()}`);
     *
     * // and one built past the cap
     * const over = stack.getCreative().setCount(99);
     * Chat.log(`${over.getCount()}, which is more than ${over.getMaxCount()}`);
     * </pre>
     *
     * @return the item count this stack is holding.
     */
    public int getCount() {
        return base.getCount();
    }

    /**
     * The largest stack this item can be in, which is a property of the item rather than of this
     * stack. {@link #getCount()} is what this stack actually holds and is not capped by it.
     * example:
     * <pre>
     * // the cap against what is held
     * const stack = Client.getRegistryManager().getItemStack("minecraft:ender_pearl").getCreative().setCount(5);
     * Chat.log(`${stack.getCount()} held, ${stack.getMaxCount()} is the most`);
     *
     * // and a stack filled to the cap
     * const full = stack.getCreative().setCount(stack.getMaxCount());
     * Chat.log(`${full.getCount()}`);
     * </pre>
     *
     * @return the maximum amount of items this stack can hold.
     */
    public int getMaxCount() {
        return base.getMaxStackSize();
    }

    /**
     * Everything written onto this stack, as an nbt compound, or {@code null} if nothing is. A
     * plain stack of an item with no components on it gives {@code null} here rather than an empty
     * compound, so this is the test for whether there is anything at all to read.
     * <p>
     * This was a map of names to strings before 1.5.1, and the key names are the component names
     * the game uses, so a renamed stack reads back its name under the custom name component.
     * example:
     * <pre>
     * // read everything off a stack that has something on it
     * const stack = Client.getRegistryManager()
     *   .getItemStack("minecraft:diamond_sword", `[{id:"minecraft:sharpness",lvl:5}]`).getCreative();
     * stack.setName("Excalibur");
     * const nbt = stack.getNBT();
     * if (nbt === null) {
     *   Chat.log("nothing on it");
     * } else {
     *   for (const key of nbt.getKeys()) {
     *     Chat.log(`${key}: ${nbt.asString(key)}`);
     *   }
     * }
     *
     * // and a plain stack, which has nothing
     * Chat.log(Client.getRegistryManager().getItemStack("minecraft:stone").getNBT() === null);
     * </pre>
     *
     * @return the components on this stack, or {@code null} if it has none.
     * @since 1.1.6, was a {@link String} until 1.5.1
     */
    @Nullable
    @DocletReplaceReturn("NBTElementHelper$NBTCompoundHelper")
    public NBTElementHelper<?> getNBT() {
        DataComponentPatch changes = base.getComponentsPatch();
        if (changes.isEmpty()) return null;
        Tag elem = DataComponentPatch.CODEC.encodeStart(RegistryHelper.getNbtOps(), changes).getOrThrow();
        return NBTElementHelper.wrap(elem);
    }

    /**
     * The names of the creative tabs this stack's item appears in, as a list of text. A stack can
     * be in more than one tab, and an item in none gives an empty list.<br>
     * This walks every creative tab and every item in it, so it is a heavy call and worth doing
     * once rather than inside a loop over a hotbar.
     * example:
     * <pre>
     * // which tabs a held item is in
     * const held = Player.getPlayer().getMainHand();
     * const tabs = held.getCreativeTab();
     * for (let i = 0; i !== tabs.size(); i += 1) {
     *   Chat.log(`${held.getItemId()} is in ${tabs.get(i).getString()}`);
     * }
     * </pre>
     *
     * @return the display names of the creative tabs this item appears in.
     * @since 1.1.3
     */
    public List<TextHelper> getCreativeTab() {
        return CreativeModeTabs.allTabs().parallelStream().filter(group -> !group.isAlignedRight() && group.getDisplayItems().parallelStream().anyMatch(e -> ItemStack.isSameItem(e, base))).map(CreativeModeTab::getDisplayName).map(TextHelper::wrap).collect(Collectors.toList());
    }

    /**
     * The item's registry id, in full, which is the old spelling of {@link #getItemId()}.
     *
     * @return the item's id in full.
     * @deprecated use {@link #getItemId()} instead, which this just calls.
     */
    @DocletReplaceReturn("ItemId")
    @Deprecated
    public String getItemID() {
        return getItemId();
    }

    /**
     * The item's registry id, in full. This is the same string
     * {@link com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper#getItemStack(String)} takes, so
     * it can be handed straight back to look the item up again.
     * example:
     * <pre>
     * const reg = Client.getRegistryManager();
     * const stack = reg.getItemStack("diamond_sword");
     * // the id comes back in full even though it was asked for short
     * Chat.log(stack.getItemId());
     * Chat.log(reg.getItemStack(stack.getItemId()).getItemId());
     * </pre>
     *
     * @return the item's id in full.
     * @since 1.6.4
     */
    @DocletReplaceReturn("ItemId")
    public String getItemId() {
        return BuiltInRegistries.ITEM.getKey(base.getItem()).toString();
    }

    /**
     * Every item tag this stack's item is in, as the tag ids in full. The tags are how the game
     * groups items for recipes, for what mines them, and for the creative tabs, so this is the way
     * to ask what an item is a member of.
     * example:
     * <pre>
     * // everything a held item is a member of
     * const tags = Player.getPlayer().getMainHand().getTags();
     * for (let i = 0; i !== tags.size(); i += 1) {
     *   Chat.log(tags.get(i));
     * }
     *
     * // planks against a block
     * const reg = Client.getRegistryManager();
     * Chat.log(`${reg.getItemStack("minecraft:oak_planks").getTags().size()} tags on planks`);
     * </pre>
     *
     * @return a list of the item tags this item is in.
     * @since 1.8.2
     */
    @DocletReplaceReturn("JavaList<ItemTag>")
    public List<String> getTags() {
        //? if >=26.1 {
        /*return base.typeHolder().tags().map(t -> t.location().toString()).collect(Collectors.toList());
        *///? } else {
        return base.getItemHolder().tags().map(t -> t.location().toString()).collect(Collectors.toList());
        //? }
    }

    /**
     * Whether this stack's item can be eaten, which is a property of the item rather than of this
     * stack.
     * example:
     * <pre>
     * // what is food
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:bread", "minecraft:golden_apple", "minecraft:stone"]) {
     *   Chat.log(`${id} is food: ${reg.getItemStack(id).isFood()}`);
     * }
     * </pre>
     *
     * @return {@code true} if this item can be eaten, {@code false} otherwise.
     * @since 1.8.2
     */
    public boolean isFood() {
        return base.get(DataComponents.FOOD) != null;
    }

    /**
     * Whether this stack's item is a tool, which is the flag the game sets on mining and fighting
     * tools.
     * example:
     * <pre>
     * // which items are tools
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:diamond_pickaxe", "minecraft:shears", "minecraft:stick"]) {
     *   Chat.log(`${id} is a tool: ${reg.getItemStack(id).isTool()}`);
     * }
     * </pre>
     *
     * @return {@code true} if this item is a tool, {@code false} otherwise.
     * @since 1.8.2
     */
    public boolean isTool() {
        return base.get(DataComponents.TOOL) != null;
    }

    /**
     * Whether this stack's item can be worn in an armour slot, which is the flag the game checks when
     * it is deciding whether an equippable slot accepts it.
     * example:
     * <pre>
     * // what can be worn
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:diamond_helmet", "minecraft:elytra", "minecraft:diamond"]) {
     *   Chat.log(`${id} is wearable: ${reg.getItemStack(id).isWearable()}`);
     * }
     * </pre>
     *
     * @return {@code true} if this item can be worn, {@code false} otherwise.
     * @since 1.8.2
     */
    public boolean isWearable() {
        return base.getComponents().get(DataComponents.EQUIPPABLE) != null;
    }

    /**
     * Whether this stack holds nothing at all, which is what the game uses for an empty inventory
     * slot. A stack of air is an empty stack, so an empty slot reads back as air here rather than
     * giving {@code null}.
     * example:
     * <pre>
     * // an empty slot against a full one
     * const reg = Client.getRegistryManager();
     * Chat.log(reg.getItemStack("minecraft:air").isEmpty());
     * Chat.log(reg.getItemStack("minecraft:stone").isEmpty());
     *
     * // and a stack taken past zero
     * const stack = reg.getItemStack("minecraft:stone").getCreative().setCount(0);
     * Chat.log(stack.isEmpty());
     * </pre>
     *
     * @return {@code true} if this stack holds nothing, {@code false} otherwise.
     */
    public boolean isEmpty() {
        return base.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("ItemStackHelper:{\"id\": \"%s\", \"damage\": %d, \"count\": %d}", this.getItemId(), base.getDamageValue(), base.getCount());
    }

    /**
     * Whether this stack and the given one are the same all the way down: same item, same count, same
     * damage, and the same things written on it. This is the strictest of the comparisons here and
     * the one to use when a script needs to know that two stacks really are interchangeable.
     * example:
     * <pre>
     * // two stacks of the same item, one of them changed
     * const reg = Client.getRegistryManager();
     * const a = reg.getItemStack("minecraft:stone").getCreative().setCount(5);
     * const b = reg.getItemStack("minecraft:stone").getCreative().setCount(5);
     * Chat.log(`before: ${a.equals(b)}`);
     * b.getCreative().setCount(6);
     * Chat.log(`after a count change: ${a.equals(b)}`);
     * </pre>
     *
     * @param ish the other stack to compare against.
     * @return {@code true} if the two stacks are the same item, count, damage and components.
     * @since 1.1.3 [citation needed]
     */
    public boolean equals(ItemStackHelper ish) {
        // ItemStack doesn't overwrite the equals method, so we have to do it ourselves
        return equals(ish.base);
    }

    /**
     * Whether this stack and a raw stack are the same all the way down: same item, same count, same
     * damage, and the same things written on it. A raw stack is {@code null}-unsafe, so a raw
     * stack that is {@code null} raises rather than answering {@code false}.
     * example:
     * <pre>
     * // against a raw stack from the game itself
     * const stack = Player.getPlayer().getMainHand();
     * Chat.log(stack.equals(stack.getRaw()));
     *
     * // and against a fresh copy of the same thing
     * const copy = stack.copy();
     * Chat.log(stack.equals(copy.getRaw()));
     * </pre>
     *
     * @param is the raw stack to compare against.
     * @return {@code true} if the two stacks are the same item, count, damage and components.
     * @since 1.1.3 [citation needed]
     */
    public boolean equals(ItemStack is) {
        return ItemStack.isSameItemSameComponents(base, is);
    }

    /**
     * Whether this stack and the given one are the same item in the same state, ignoring what has
     * been written on them. Damage counts as part of the state, so two stacks of a damaged item
     * with the same damage match and two at different damage do not.
     * example:
     * <pre>
     * // same item, different things written on them
     * const reg = Client.getRegistryManager();
     * const plain = reg.getItemStack("minecraft:diamond_sword");
     * const named = reg.getItemStack("minecraft:diamond_sword").getCreative().setName("Excalibur");
     * Chat.log(`same item: ${plain.isItemEqual(named)}`);
     * Chat.log(`same everything: ${plain.isNBTEqual(named)}`);
     *
     * // and the damage check, which is part of the state
     * const worn = plain.getCreative().setDamage(10);
     * Chat.log(`after damage: ${plain.isItemEqual(worn)}`);
     * </pre>
     *
     * @param ish the other stack to compare against.
     * @return {@code true} if the two stacks are the same item at the same damage.
     * @since 1.1.3 [citation needed]
     */
    public boolean isItemEqual(ItemStackHelper ish) {
        return ItemStack.isSameItem(base, ish.getRaw()) && base.getDamageValue() == ish.getRaw().getDamageValue();
    }

    /**
     * Whether this stack and a raw stack are the same item in the same state, ignoring what has been
     * written on them.
     * example:
     * <pre>
     * // against a raw stack, which is the form the game hands out
     * const held = Player.getPlayer().getMainHand();
     * Chat.log(held.isItemEqual(held.getRaw()));
     * </pre>
     *
     * @param is the raw stack to compare against.
     * @return {@code true} if the two stacks are the same item at the same damage.
     * @since 1.1.3 [citation needed]
     */
    public boolean isItemEqual(ItemStack is) {
        return ItemStack.isSameItem(is, base) && base.getDamageValue() == is.getDamageValue();
    }

    /**
     * Whether this stack and the given one are the same item, ignoring the damage and everything
     * written on them. This is the loosest of the comparisons here and the one to use when a script
     * only cares what something is rather than what state it is in.
     * example:
     * <pre>
     * // a damaged and named stack against a plain one
     * const reg = Client.getRegistryManager();
     * const plain = reg.getItemStack("minecraft:diamond_sword");
     * const worn = reg.getItemStack("minecraft:diamond_sword").getCreative()
     *   .setName("Excalibur")
     *   .setDamage(100);
     * Chat.log(`same item: ${plain.isItemEqualIgnoreDamage(worn)}`);
     * Chat.log(`same item and damage: ${plain.isItemEqual(worn)}`);
     *
     * // and against a different item entirely
     * Chat.log(plain.isItemEqualIgnoreDamage(reg.getItemStack("minecraft:stone")));
     * </pre>
     *
     * @param ish the other stack to compare against.
     * @return {@code true} if the two stacks are the same item, whatever else differs.
     * @since 1.1.3 [citation needed]
     */
    public boolean isItemEqualIgnoreDamage(ItemStackHelper ish) {
        return ItemStack.isSameItem(ish.getRaw(), base);
    }

    /**
     * Whether this stack and a raw stack are the same item, ignoring the damage and everything
     * written on them.
     * example:
     * <pre>
     * // against a raw stack
     * const held = Player.getPlayer().getMainHand();
     * Chat.log(held.isItemEqualIgnoreDamage(held.getRaw()));
     * </pre>
     *
     * @param is the raw stack to compare against.
     * @return {@code true} if the two stacks are the same item, whatever else differs.
     * @since 1.1.3 [citation needed]
     */
    public boolean isItemEqualIgnoreDamage(ItemStack is) {
        return ItemStack.isSameItem(is, base);
    }

    /**
     * Whether this stack and the given one carry exactly the same things written on them, ignoring
     * the item, the count and the damage. This is the comparison for a build check: two stacks of
     * different tools carrying the same enchantments answer true.
     * example:
     * <pre>
     * // the same enchantments on two different tools
     * const reg = Client.getRegistryManager();
     * const sword = reg.getItemStack("minecraft:diamond_sword", `[{id:"minecraft:sharpness",lvl:5}]`);
     * const axe = reg.getItemStack("minecraft:diamond_axe", `[{id:"minecraft:sharpness",lvl:5}]`);
     * Chat.log(`same components: ${sword.isNBTEqual(axe)}`);
     * Chat.log(`same item: ${sword.isItemEqual(axe)}`);
     *
     * // and against a stack with something else on it
     * const named = sword.getCreative().setName("Excalibur");
     * Chat.log(`after naming: ${sword.isNBTEqual(named)}`);
     * </pre>
     *
     * @param ish the other stack to compare against.
     * @return {@code true} if the two stacks carry the same components.
     * @since 1.1.3 [citation needed]
     */
    public boolean isNBTEqual(ItemStackHelper ish) {
        return Objects.equals(base.getComponents(), ish.getRaw().getComponents());
    }

    /**
     * Whether this stack and a raw stack carry exactly the same things written on them, ignoring
     * the item, the count and the damage.
     * example:
     * <pre>
     * // against a raw stack
     * const held = Player.getPlayer().getMainHand();
     * Chat.log(held.isNBTEqual(held.getRaw()));
     * </pre>
     *
     * @param is the raw stack to compare against.
     * @return {@code true} if the two stacks carry the same components.
     * @since 1.1.3 [citation needed]
     */
    public boolean isNBTEqual(ItemStack is) {
        return Objects.equals(base.getComponents(), is.getComponents());
    }

    /**
     * Whether the player is on this item's cooldown right now, which is the question for an item
     * with a delay on it such as an ender pearl or a shield. The answer is about the local player
     * rather than about the stack.
     * example:
     * <pre>
     * // can the player throw one of these again yet
     * const pearl = Client.getRegistryManager().getItemStack("minecraft:ender_pearl");
     * const held = Player.getPlayer().getMainHand();
     * if (held.getItemId() === pearl.getItemId()) {
     *   Chat.log(`on cooldown: ${held.isOnCooldown()}`);
     *   Chat.log(`${Math.round(held.getCooldownProgress() * 100)}% through it`);
     * }
     * </pre>
     *
     * @return {@code true} if the item is on cooldown, {@code false} otherwise.
     * @since 1.6.5
     */
    public boolean isOnCooldown() {
        return Minecraft.getInstance().player.getCooldowns().isOnCooldown(base);
    }

    /**
     * How far through its cooldown this item is, from zero to one, worked out against the partial
     * tick the client is currently drawing. An item that is not on cooldown gives zero, and one
     * whose cooldown has just finished gives one.
     * <p>
     * The item identity is what the game matches on rather than the stack, so a stack of a
     * different count or a different damage of the same item shares the cooldown.
     * example:
     * <pre>
     * // watch a cooldown run down
     * const stack = Client.getRegistryManager().getItemStack("minecraft:ender_pearl");
     * Chat.log(`${stack.getCooldownProgress()} to go, on cooldown ${stack.isOnCooldown()}`);
     *
     * // the same item, different stack, same cooldown
     * const other = stack.getCreative().setCount(3);
     * Chat.log(other.getCooldownProgress() === stack.getCooldownProgress());
     *
     * // and an item with no cooldown at all
     * Chat.log(Client.getRegistryManager().getItemStack("minecraft:stone").getCooldownProgress());
     * </pre>
     *
     * @return how far through its cooldown this item is, from zero to one.
     * @since 1.6.5
     */
    public float getCooldownProgress() {
        return mc.player.getCooldowns().getCooldownPercent(base, mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }

    /**
     * Whether the given block drops its items when broken with this stack. The question is asked of
     * the block's default state, so for a block that comes in variants it is the answer for
     * whichever one the game treats as the plain form.
     * example:
     * <pre>
     * // which blocks the held tool is the right one for
     * const held = Player.getPlayer().getMainHand();
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:stone", "minecraft:diamond_ore", "minecraft:oak_log", "minecraft:dirt"]) {
     *   Chat.log(`${id}: ${held.isSuitableFor(reg.getBlock(id))}`);
     * }
     * </pre>
     *
     * @param block the block to check
     * @return {@code true} if the given block can be mined and drops when broken with this item,
     * {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSuitableFor(BlockHelper block) {
        return base.isCorrectToolForDrops(block.getDefaultState().getRaw());
    }

    /**
     * Whether the given block state drops its items when broken with this stack, which is the form
     * that asks about a particular variant rather than about the block in general.
     * example:
     * <pre>
     * // a particular state rather than the block
     * const held = Player.getPlayer().getMainHand();
     * const ore = Client.getRegistryManager().getBlockState("minecraft:deepslate_diamond_ore");
     * Chat.log(`held tool on deepslate diamond ore: ${held.isSuitableFor(ore)}`);
     * </pre>
     *
     * @param block the block to check
     * @return {@code true} if the given block can be mined and drops when broken with this item,
     * {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSuitableFor(BlockStateHelper block) {
        return base.isCorrectToolForDrops(block.getRaw());
    }

    /**
     * The same stack with setters on it, so a script can build one up rather than only read it.
     * This is not a copy: it wraps the very stack this was called on, so anything done through it
     * changes this one.
     * example:
     * <pre>
     * // build a stack up through the setters
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_sword");
     * stack.getCreative()
     *   .setName("Excalibur")
     *   .addEnchantment("minecraft:sharpness", 5)
     *   .setLore("Forged in the first age");
     * Chat.log(`${stack.getName().getString()}, ${stack.getLore().size()} lore lines`);
     *
     * // it is the same stack, not a copy of it
     * Chat.log(stack.isEnchanted());
     * </pre>
     *
     * @return a {@link CreativeItemStackHelper} instance for this item.
     * @since 1.8.4
     */
    public CreativeItemStackHelper getCreative() {
        return new CreativeItemStackHelper(base);
    }

    /**
     * The kind of item this stack is, with the count, the damage and everything written on it left
     * off. This is the form to use when a question is about the item rather than about the stack.
     * example:
     * <pre>
     * // the item behind a stack
     * const stack = Player.getPlayer().getMainHand();
     * const item = stack.getItem();
     * Chat.log(`${item.getId()} is called ${item.getName()}`);
     * Chat.log(`stack size ${item.getMaxCount()}, a tool: ${item.isTool()}`);
     *
     * // and against the stack it came from
     * Chat.log(stack.getItemId() === item.getId());
     * </pre>
     *
     * @return the item this stack is made of.
     * @since 1.8.4
     */
    public ItemHelper getItem() {
        return new ItemHelper(base.getItem());
    }

    /**
     * A copy of this stack, with everything on it, so a script can change the copy without changing
     * the one it was given. This is what to reach for before any of the setters on
     * {@link CreativeItemStackHelper} when the original matters.
     * example:
     * <pre>
     * // change a copy, keep the original
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_sword");
     * const renamed = stack.copy().getCreative().setName("Excalibur");
     * Chat.log(`copy: ${renamed.getName().getString()}`);
     * Chat.log(`original: ${stack.getName().getString()}`);
     *
     * // a copy is equal to what it was copied from
     * Chat.log(stack.isNBTEqual(renamed.copy()));
     * </pre>
     *
     * @return a copy of this stack, with everything on it.
     * @since 1.2.0
     */
    public ItemStackHelper copy() {
        return new ItemStackHelper(base.copy());
    }

    /**
     * Whether this stack carries a can-destroy restriction, which is the adventure mode flag that
     * decides which blocks a player in adventure mode may break with it. The flag is on the stack
     * and says nothing about what the item can actually mine.
     * <p>
     * This flag only affects players in adventure mode and makes sure only specified blocks can be
     * destroyed by this item.
     * example:
     * <pre>
     * // a stack carrying an adventure mode restriction
     * const stack = Client.getRegistryManager()
     *   .getItemStack("minecraft:diamond_pickaxe", `[can_break=[{blocks:"minecraft:stone"}]]`)
     *   .getCreative();
     * Chat.log(`has the flag ${stack.hasDestroyRestrictions()}`);
     * const filters = stack.getDestroyRestrictions();
     * for (let i = 0; i !== filters.size(); i += 1) {
     *   const blocks = filters.get(i).getBlocks();
     *   Chat.log(`  filter ${i} names ${blocks === null ? 0 : blocks.size()} blocks`);
     * }
     *
     * // a stack with no restriction at all
     * Chat.log(Client.getRegistryManager().getItemStack("minecraft:diamond_pickaxe").hasDestroyRestrictions());
     * </pre>
     *
     * @return {@code true} if the can destroy flag is set, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasDestroyRestrictions() {
        return base.get(DataComponents.CAN_BREAK) != null;
    }

    /**
     * Whether this stack carries a can-place-on restriction, the other of the two adventure mode
     * flags, which decides which blocks a player in adventure mode may put this down on.
     * <p>
     * This flag only affects players in adventure mode and makes sure this item can only be placed
     * on specified blocks.
     * example:
     * <pre>
     * // a stack carrying the other adventure mode restriction
     * const stack = Client.getRegistryManager()
     *   .getItemStack("minecraft:diamond_pickaxe", `[can_place_on=[{blocks:"minecraft:stone"}]]`)
     *   .getCreative();
     * Chat.log(`has the flag ${stack.hasPlaceRestrictions()}`);
     * const filters = stack.getPlaceRestrictions();
     * for (let i = 0; i !== filters.size(); i += 1) {
     *   Chat.log(`  filter ${i} allows ${filters.get(i).getBlocks() === null ? 0 : filters.get(i).getBlocks().size()} blocks`);
     * }
     * </pre>
     *
     * @return {@code true} if the can place on flag is set, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasPlaceRestrictions() {
        return base.get(DataComponents.CAN_PLACE_ON) != null;
    }

    /**
     * The block filters on this stack's can-destroy restriction, one per entry. A stack with no
     * restriction gives an empty list rather than {@code null}, and so does one whose restriction
     * names no blocks.
     * <p>
     * Each filter is a block predicate, which is more than a list of blocks: it can also carry a
     * state filter and an nbt filter, and {@link BlockPredicateHelper#test(BlockPosHelper)} is what
     * runs the whole thing.
     * example:
     * <pre>
     * // the filters on a restricted stack
     * const stack = Client.getRegistryManager()
     *   .getItemStack("minecraft:diamond_pickaxe", `[can_break=[{blocks:"minecraft:stone"}]]`)
     *   .getCreative();
     * const filters = stack.getDestroyRestrictions();
     * Chat.log(`${filters.size()} filters`);
     * for (let i = 0; i !== filters.size(); i += 1) {
     *   const filter = filters.get(i);
     *   const blocks = filter.getBlocks();
     *   if (blocks !== null) {
     *     for (let b = 0; b !== blocks.size(); b += 1) {
     *       Chat.log(`  allows ${blocks.get(b).getId()}`);
     *     }
     *   }
     *   Chat.log(`  a stone passes: ${filter.test(PositionCommon.createBlockPos(0, 64, 0))}`);
     * }
     *
     * // a stack with no restriction gives an empty list
     * Chat.log(Client.getRegistryManager().getItemStack("minecraft:stone").getDestroyRestrictions().size());
     * </pre>
     *
     * @return a list of all filters set for the can destroy flag.
     * @since 1.8.4
     */
    public List<BlockPredicateHelper> getDestroyRestrictions() {
        AdventureModePredicate bpc = base.get(DataComponents.CAN_BREAK);
        if (bpc != null) {
            return ((MixinBlockPredicatesChecker) bpc).getPredicates().stream().map(BlockPredicateHelper::new).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * The block filters on this stack's can-place-on restriction, one per entry, the same shape
     * {@link #getDestroyRestrictions()} gives. A stack with no restriction gives an empty list.
     * example:
     * <pre>
     * // the filters on a restricted stack
     * const stack = Client.getRegistryManager()
     *   .getItemStack("minecraft:diamond_pickaxe", `[can_place_on=[{blocks:"minecraft:stone"}]]`)
     *   .getCreative();
     * const filters = stack.getPlaceRestrictions();
     * for (let i = 0; i !== filters.size(); i += 1) {
     *   const blocks = filters.get(i).getBlocks();
     *   if (blocks !== null) {
     *     for (let b = 0; b !== blocks.size(); b += 1) {
     *       Chat.log(`may be placed on ${blocks.get(b).getId()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return a list of all filters set for the can place on flag.
     * @since 1.8.4
     */
    public List<BlockPredicateHelper> getPlaceRestrictions() {
        AdventureModePredicate nbtList = base.get(DataComponents.CAN_PLACE_ON);
        if (nbtList != null) {
            return ((MixinBlockPredicatesChecker) nbtList).getPredicates().stream().map(BlockPredicateHelper::new).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Whether the enchantments on this stack are hidden from the game's tooltip. This reads the
     * tooltip display component and asks it about the tooltip display component, so the answer it
     * gives is not about the enchantments: a stack built with
     * {@link CreativeItemStackHelper#hideEnchantments(boolean)} still answers {@code false} here.
     * The enchantments really are hidden from the tooltip, this just does not report it.<br>
     * {@link ItemStackHelper#getEnchantments()} still reads them back either way.
     * example:
     * <pre>
     * // the stack is really hiding them, and this still says no
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_sword").getCreative()
     *   .addEnchantment("minecraft:sharpness", 5)
     *   .hideEnchantments(true);
     * Chat.log(`areEnchantmentsHidden: ${stack.areEnchantmentsHidden()}`);
     * Chat.log(`but it is enchanted: ${stack.isEnchanted()}, and ${stack.getEnchantments().size()} of them`);
     *
     * // the three hidden flags agree with each other, which is the tell
     * Chat.log(stack.areEnchantmentsHidden() === stack.areModifiersHidden());
     * Chat.log(stack.areModifiersHidden() === stack.isUnbreakableHidden());
     * </pre>
     *
     * @return {@code true} if enchantments are hidden, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean areEnchantmentsHidden() {
        return isHidden(DataComponents.TOOLTIP_DISPLAY);
    }

    /**
     * Whether the attribute modifiers on this stack are hidden from the game's tooltip. This reads
     * the tooltip display component and asks it about the tooltip display component, so the answer
     * it gives is not about the modifiers, and it is the same answer
     * {@link #areEnchantmentsHidden()} and {@link #isUnbreakableHidden()} give.
     * example:
     * <pre>
     * // the stack is really hiding them, and this still says no
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_sword").getCreative()
     *   .hideModifiers(true);
     * Chat.log(`areModifiersHidden: ${stack.areModifiersHidden()}`);
     * Chat.log(`and the damage it gives is still ${stack.getAttackDamage()}`);
     *
     * // the flags that do report correctly, for contrast
     * const dyed = Client.getRegistryManager()
     *   .getItemStack("minecraft:leather_helmet", `[dyed_color={color:"minecraft:red",rgb:0}]`)
     *   .getCreative()
     *   .hideDye(true);
     * Chat.log(`isDyeHidden: ${dyed.isDyeHidden()}`);
     * </pre>
     *
     * @return {@code true} if modifiers are hidden, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean areModifiersHidden() {
        return isHidden(DataComponents.TOOLTIP_DISPLAY);
    }

    /**
     * Whether the unbreakable marker on this stack is hidden from the game's tooltip. This reads the
     * tooltip display component and asks it about the tooltip display component, so the answer it
     * gives is not about the unbreakable marker, and it is the same answer
     * {@link #areEnchantmentsHidden()} and {@link #areModifiersHidden()} give.
     * example:
     * <pre>
     * // the stack is really hiding it, and this still says no
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_pickaxe").getCreative()
     *   .setUnbreakable(true)
     *   .hideUnbreakable(true);
     * Chat.log(`isUnbreakableHidden: ${stack.isUnbreakableHidden()}`);
     * Chat.log(`but it is unbreakable: ${stack.isUnbreakable()}`);
     * </pre>
     *
     * @return {@code true} if the unbreakable flag is hidden, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isUnbreakableHidden() {
        return isHidden(DataComponents.TOOLTIP_DISPLAY);
    }

    /**
     * Whether the can-destroy restriction on this stack is hidden from the game's tooltip. This one
     * reads the can destroy component itself, so it does report what was set.
     * example:
     * <pre>
     * // a restricted stack with the flag hidden
     * const stack = Client.getRegistryManager()
     *   .getItemStack("minecraft:diamond_pickaxe", `[can_break=[{blocks:"minecraft:stone"}]]`)
     *   .getCreative()
     *   .hideCanDestroy(true);
     * Chat.log(`hidden ${stack.isCanDestroyHidden()}, still restricted ${stack.hasDestroyRestrictions()}`);
     * </pre>
     *
     * @return {@code true} if the can destroy flag is hidden, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCanDestroyHidden() {
        return isHidden(DataComponents.CAN_BREAK);
    }

    /**
     * Whether the can-place-on restriction on this stack is hidden from the game's tooltip. This one
     * reads the can place on component itself, so it does report what was set.
     * example:
     * <pre>
     * // a restricted stack with the flag hidden
     * const stack = Client.getRegistryManager()
     *   .getItemStack("minecraft:diamond_pickaxe", `[can_place_on=[{blocks:"minecraft:stone"}]]`)
     *   .getCreative()
     *   .hideCanPlace(true);
     * Chat.log(`hidden ${stack.isCanPlaceHidden()}, still restricted ${stack.hasPlaceRestrictions()}`);
     * </pre>
     *
     * @return {@code true} if the can place flag is hidden, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCanPlaceHidden() {
        return isHidden(DataComponents.CAN_PLACE_ON);
    }


    /**
     * Whether the colour of coloured leather armour on this stack is hidden from the game's
     * tooltip. This one reads the dyed colour component itself, so it does report what was set.
     * example:
     * <pre>
     * // dyed armour with the colour hidden
     * const stack = Client.getRegistryManager()
     *   .getItemStack("minecraft:leather_helmet", `[dyed_color={color:"minecraft:red",rgb:0}]`)
     *   .getCreative()
     *   .hideDye(true);
     * Chat.log(`hidden ${stack.isDyeHidden()}`);
     *
     * // and back again
     * stack.hideDye(false);
     * Chat.log(`now ${stack.isDyeHidden()}`);
     * </pre>
     *
     * @return {@code true} if dye of colored leather armor is hidden, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isDyeHidden() {
        return isHidden(DataComponents.DYED_COLOR);
    }

    private boolean isHidden(DataComponentType<?> type) {
        var display = base.get(DataComponents.TOOLTIP_DISPLAY);
        return display != null && display.hiddenComponents().contains(type);
    }

}
