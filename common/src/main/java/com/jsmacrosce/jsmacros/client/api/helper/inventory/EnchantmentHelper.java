package com.jsmacrosce.jsmacros.client.api.helper.inventory;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.List;
import java.util.Objects;

/**
 * One enchantment, and optionally the level it is at.<br>
 * The level is the thing that makes this more than a lookup. A helper built from the registry has
 * no level and reports zero, and a helper taken off an item has the level that item carries. The
 * level is also what makes two helpers for the same enchantment at different levels compare as
 * different, and it is what the two name calls take.<br>
 * Compatibility is a property of the enchantments themselves, and the game holds a set of pairs
 * that cannot sit on the same item. There is one thing about it that catches people out: an
 * enchantment is <b>not</b> compatible with itself, so {@link #isCompatible(EnchantmentHelper)}
 * against this same enchantment answers false and {@link #conflictsWith(EnchantmentHelper)} against
 * it answers true. Both of the list calls follow from that one fact, and they fall on opposite
 * sides of it: a helper is never in its own {@link #getCompatibleEnchantments()} list, and is
 * always in its own {@link #getConflictingEnchantments()} one, whichever of the two the helper came
 * from and whatever level it carries.
 * example:
 * <pre>
 * // an enchantment on its own, from the registry
 * const sharpness = Client.getRegistryManager().getEnchantment("minecraft:sharpness");
 * Chat.log(`${sharpness.getName()} ${sharpness.getMinLevel()} to ${sharpness.getMaxLevel()}`);
 * Chat.log(`weight ${sharpness.getWeight()}, cursed ${sharpness.isCursed()}`);
 *
 * // and the same one at a level, which is what an item gives
 * const onSword = Client.getRegistryManager().getEnchantment("minecraft:sharpness", 3);
 * Chat.log(`level ${onSword.getLevel()}: ${onSword.getRomanLevelName()}`);
 *
 * // compatibility is a property of the pair, and never true of one with itself
 * const looting = Client.getRegistryManager().getEnchantment("minecraft:looting");
 * Chat.log(`sharpness with looting: ${sharpness.isCompatible(looting)}`);
 * Chat.log(`sharpness with itself: ${sharpness.isCompatible(sharpness)}`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Items/Enchantments")
@SuppressWarnings("unused")
public class EnchantmentHelper extends BaseHelper<Holder<Enchantment>> {
    private static final Minecraft mc = Minecraft.getInstance();

    private final int level;

    public EnchantmentHelper(Holder<Enchantment> base) {
        this(base, 0);
    }

    public EnchantmentHelper(Holder<Enchantment> base, int level) {
        super(base);
        this.level = level;
    }

    @DocletReplaceParams("enchantment: CanOmitNamespace<EnchantmentId>")
    public EnchantmentHelper(String enchantment) {
        this(mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ResourceLocation.parse(enchantment)).orElseThrow());
    }

    /**
     * The level this helper is at, or zero when it was built without one. A helper from
     * {@link ItemStackHelper#getEnchantments()} carries the level the item has it at, while one
     * from the registry carries none, and every call that takes a level argument does so precisely
     * because this one is not always meaningful.
     * example:
     * <pre>
     * // from the registry, with no level at all
     * const bare = Client.getRegistryManager().getEnchantment("minecraft:sharpness");
     * Chat.log(`registry level ${bare.getLevel()}`);
     *
     * // and from an item, which does have one
     * const sword = Client.getRegistryManager().getItemStack("minecraft:diamond_sword", `[{id:"minecraft:sharpness",lvl:4}]`);
     * const found = sword.getEnchantment("minecraft:sharpness");
     * if (found !== null) {
     *   Chat.log(`on the sword it is level ${found.getLevel()}`);
     * }
     * </pre>
     *
     * @return the level of this enchantment.
     * @since 1.8.4
     */
    public int getLevel() {
        return level;
    }

    /**
     * The lowest level this enchantment can be at, as the game defines it. An enchantment that has
     * only one level gives the same number as {@link #getMaxLevel()}.
     * example:
     * <pre>
     * // the level range of a few enchantments
     * for (const id of ["minecraft:sharpness", "minecraft:protection", "minecraft:mending"]) {
     *   const ench = Client.getRegistryManager().getEnchantment(id);
     *   Chat.log(`${id}: ${ench.getMinLevel()} to ${ench.getMaxLevel()}`);
     * }
     * </pre>
     *
     * @return the minimum possible level of this enchantment that one can get in vanilla.
     * @since 1.8.4
     */
    public int getMinLevel() {
        return base.value().getMinLevel();
    }

    /**
     * The highest level this enchantment can be at, as the game defines it. This is the cap for
     * anything that applies the enchantment, so a level above it in a script is a level the game
     * will not produce.
     * example:
     * <pre>
     * // the cap, and a check against it
     * const ench = Client.getRegistryManager().getEnchantment("minecraft:sharpness");
     * const max = ench.getMaxLevel();
     * for (let lvl = 1; lvl !== max + 2; lvl += 1) {
     *   const at = Client.getRegistryManager().getEnchantment("minecraft:sharpness", lvl);
     *   Chat.log(`level ${lvl}: ${at.getRomanLevelName().getString()}`);
     * }
     * </pre>
     *
     * @return the maximum possible level of this enchantment that one can get in vanilla.
     * @since 1.8.4
     */
    public int getMaxLevel() {
        return base.value().getMaxLevel();
    }

    /**
     * The translated name of this enchantment at the given level, as a plain string rather than as
     * text, so the colouring and the styling the game uses for it are not part of what comes back.
     * The level is taken as an argument rather than read from this helper, so this works on a
     * helper that has no level of its own.
     * example:
     * <pre>
     * // the name at a level, for a plain string to put in a command or a file
     * const ench = Client.getRegistryManager().getEnchantment("minecraft:sharpness");
     * for (let lvl = ench.getMinLevel(); lvl !== ench.getMaxLevel() + 1; lvl += 1) {
     *   Chat.log(ench.getLevelName(lvl));
     * }
     * </pre>
     *
     * @param level the level for the name
     * @return the translated name of this enchantment for the given level.
     * @since 1.8.4
     */
    public String getLevelName(int level) {
        return Enchantment.getFullname(base, level).getString();
    }

    /**
     * The name of this enchantment at this helper's own level, as text, so it carries the colouring
     * and the styling the game uses: grey for an ordinary enchantment and red for a curse.
     * <p>
     * The level is written in roman numerals, and because roman numerals only cover one to
     * three thousand and nine hundred and ninety-nine, a level outside that range comes back as an
     * ordinary number instead.
     * <p>
     * A helper with no level of its own has a level of zero, and a level of zero is not the same as
     * no level at all: the numeral is still written on, so that name reads with a zero after it.
     * example:
     * <pre>
     * // the styled name, at the level the helper carries
     * const atFive = Client.getRegistryManager().getEnchantment("minecraft:sharpness", 5);
     * Chat.log(atFive.getRomanLevelName());
     * Chat.log(atFive.getRomanLevelName().getStringStripFormatting());
     *
     * // and at a level asked for, which is how to go past the cap
     * Chat.log(atFive.getRomanLevelName(12));
     * </pre>
     *
     * @return the translated name of this enchantment for the given level in roman numerals.
     * @since 1.8.4
     */
    public TextHelper getRomanLevelName() {
        return getRomanLevelName(level);
    }

    /**
     * The name of this enchantment at the given level, as text, with the level written in roman
     * numerals and the colouring the game uses: grey for an ordinary enchantment and red for a
     * curse.
     * <p>
     * Because roman numerals only cover one to three thousand nine hundred and ninety-nine, a level
     * outside that range comes back as an ordinary number instead. A level of one on an
     * enchantment that has more than one possible level still gets the numeral written on it, which
     * is how the game draws it.
     * example:
     * <pre>
     * // every level of an enchantment, as the game would draw it
     * const ench = Client.getRegistryManager().getEnchantment("minecraft:sharpness");
     * for (let lvl = ench.getMinLevel(); lvl !== ench.getMaxLevel() + 1; lvl += 1) {
     *   Chat.log(ench.getRomanLevelName(lvl));
     * }
     *
     * // a curse is drawn in red rather than grey
     * const curse = Client.getRegistryManager().getEnchantment("minecraft:binding_curse");
     * Chat.log(curse.getRomanLevelName(1));
     * </pre>
     *
     * @param level the level for the name
     * @return the translated name of this enchantment for the given level in roman numerals.
     * @since 1.8.4
     */
    public TextHelper getRomanLevelName(int level) {
        MutableComponent mutableText = base.value().description().copy();
        mutableText.withStyle(base.is(EnchantmentTags.CURSE) ? ChatFormatting.RED : ChatFormatting.GRAY);
        if (level != 1 || this.getMaxLevel() != 1) {
            mutableText.append(" ").append(getRomanNumeral(level));
        }
        return TextHelper.wrap(mutableText);
    }

    private static String getRomanNumeral(int number) {
        if (number > 3999 || number < 1) {
            return String.valueOf(number);
        }
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] letters = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder romanNumeral = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (number >= values[i]) {
                number = number - values[i];
                romanNumeral.append(letters[i]);
            }
        }
        return romanNumeral.toString();
    }

    /**
     * The translated name of this enchantment on its own, with no level written on it. This is the
     * plain description the game keeps for the enchantment, which is the same word for every level.
     * example:
     * <pre>
     * // the bare name, against the name with a level on it
     * const ench = Client.getRegistryManager().getEnchantment("minecraft:sharpness", 3);
     * Chat.log(ench.getName());
     * Chat.log(ench.getRomanLevelName().getString());
     *
     * // the names of every enchantment in the registry
     * const all = Client.getRegistryManager().getEnchantments();
     * for (let i = 0; i !== all.size(); i += 1) {
     *   Chat.log(`${all.get(i).getId()}: ${all.get(i).getName()}`);
     * }
     * </pre>
     *
     * @return the name of this enchantment.
     * @since 1.8.4
     */
    public String getName() {
        return base.value().description().getString();
    }

    /**
     * The enchantment's registry id, in full. The id may be written with or without its namespace,
     * and this is the form that comes back either way, so it can be handed straight to the registry
     * lookups.
     * example:
     * <pre>
     * // the id, and the same enchantment looked up by it
     * const ench = Client.getRegistryManager().getEnchantment("sharpness");
     * Chat.log(ench.getId());
     * Chat.log(Client.getRegistryManager().getEnchantment(ench.getId()).getId());
     * </pre>
     *
     * @return the id of this enchantment.
     * @since 1.8.4
     */
    @DocletReplaceReturn("EnchantmentId")
    public String getId() {
        return base.getRegisteredName();
    }

    /**
     * Every enchantment in the registry that cannot sit on the same item as this one. The list is
     * built by walking the whole enchantment registry and keeping everything the game does not
     * count as compatible with this one, and <b>this enchantment is in that list too</b>, because
     * the game does not count an enchantment as compatible with itself. That is the same fact that
     * makes {@link #conflictsWith(EnchantmentHelper)} answer true against itself, read as a list
     * rather than as an answer, and it is the mirror image of
     * {@link #getCompatibleEnchantments()}, which leaves this enchantment out.<br>
     * The returned helpers carry no level, since the question is about the enchantments and not
     * about any level of them. Because this enchantment is in there as well, a script that wants
     * only the others rather than all of them has to drop the entry whose {@link #getId()} is its
     * own.
     * example:
     * <pre>
     * // what cannot share an item with sharpness
     * const sharp = Client.getRegistryManager().getEnchantment("minecraft:sharpness");
     * const clashes = sharp.getConflictingEnchantments();
     * Chat.log(`${clashes.size()} enchantments conflict with it`);
     * for (let i = 0; i !== clashes.size(); i += 1) {
     *   Chat.log(`  ${clashes.get(i).getId()}`);
     * }
     *
     * // and the list holds sharpness itself, against itself
     * for (let i = 0; i !== clashes.size(); i += 1) {
     *   if (clashes.get(i).getId() === sharp.getId()) {
     *     Chat.log(`which is in there at index ${i}`);
     *   }
     * }
     * </pre>
     *
     * @return a list of all enchantments that conflict with this one.
     * @since 1.8.4
     */
    public List<EnchantmentHelper> getConflictingEnchantments() {
        return getConflictingEnchantments(false);
    }

    /**
     * Every enchantment in the registry that cannot sit on the same item as this one, the same list
     * {@link #getConflictingEnchantments()} gives.<br>
     * The argument was meant to narrow the list to enchantments of the same target type, and the
     * check it asks for is not made: the whole registry is walked and the target type plays no part
     * in the answer. Passing either value gives the same list, so this is here for the signature
     * rather than for what it does.
     * example:
     * <pre>
     * // the argument makes no difference to the list
     * const ench = Client.getRegistryManager().getEnchantment("minecraft:sharpness");
     * const a = ench.getConflictingEnchantments(false);
     * const b = ench.getConflictingEnchantments(true);
     * Chat.log(`${a.size()} and ${b.size()}`);
     * </pre>
     *
     * @param ignoreType unused. The list is not narrowed by target type.
     * @return a list of all enchantments that conflict with this one.
     * @since 1.8.4
     */
    public List<EnchantmentHelper> getConflictingEnchantments(boolean ignoreType) {
        return mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
            .filter(e -> !Enchantment.areCompatible(e, base))
            .map(EnchantmentHelper::new)
            .toList();
    }

    /**
     * Every enchantment in the registry that can sit on the same item as this one. The list is
     * built by walking the whole enchantment registry, and it does not include this enchantment
     * itself, since the game does not count an enchantment as compatible with itself.<br>
     * The returned helpers carry no level, since the question is about the enchantments and not
     * about any level of them.
     * example:
     * <pre>
     * // what can share an item with sharpness
     * const sharp = Client.getRegistryManager().getEnchantment("minecraft:sharpness");
     * const friends = sharp.getCompatibleEnchantments();
     * Chat.log(`${friends.size()} enchantments go with it`);
     * for (let i = 0; i !== friends.size(); i += 1) {
     *   Chat.log(`  ${friends.get(i).getId()} at ${friends.get(i).getMinLevel()} to ${friends.get(i).getMaxLevel()}`);
     * }
     * </pre>
     *
     * @return a list of all enchantments that can be combined with this one.
     * @since 1.8.4
     */
    public List<EnchantmentHelper> getCompatibleEnchantments() {
        return getCompatibleEnchantments(false);
    }

    /**
     * Every enchantment in the registry that can sit on the same item as this one, the same list
     * {@link #getCompatibleEnchantments()} gives.<br>
     * The argument was meant to narrow the list to enchantments of the same target type, and the
     * check it asks for is not made: the whole registry is walked and the target type plays no part
     * in the answer. Passing either value gives the same list, so this is here for the signature
     * rather than for what it does.
     * example:
     * <pre>
     * // the argument makes no difference to the list
     * const ench = Client.getRegistryManager().getEnchantment("minecraft:sharpness");
     * Chat.log(`${ench.getCompatibleEnchantments(false).size()} and ${ench.getCompatibleEnchantments(true).size()}`);
     * </pre>
     *
     * @param ignoreType unused. The list is not narrowed by target type.
     * @return a list of all enchantments that can be combined with this one.
     * @since 1.8.4
     */
    public List<EnchantmentHelper> getCompatibleEnchantments(boolean ignoreType) {
        return mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
            .filter(e -> Enchantment.areCompatible(e, base))
            .map(EnchantmentHelper::new)
            .toList();
    }

    /**
     * The weight the game uses when it picks this enchantment to apply, as a number relative to the
     * others in the same slot. A larger weight makes it more likely to be chosen, and the weight
     * comes from the enchantment's rarity.
     * example:
     * <pre>
     * // the weights within one slot, largest first
     * const all = Client.getRegistryManager().getEnchantments();
     * const rows = [];
     * for (let i = 0; i !== all.size(); i += 1) {
     *     rows.push({ id: all.get(i).getId(), weight: all.get(i).getWeight() });
     *   }
     *   rows.sort(function (a, b) { return b.weight - a.weight; });
     *   for (const row of rows.slice(0, 5)) {
     *     Chat.log(`${row.id}: weight ${row.weight}`);
     *   }
     *
     * // and what a treasure weighs next to an ordinary one
     * const ordinary = Client.getRegistryManager().getEnchantment("minecraft:sharpness");
     * const treasure = Client.getRegistryManager().getEnchantment("minecraft:mending");
     * Chat.log(`sharpness ${ordinary.getWeight()}, mending ${treasure.getWeight()}`);
     * </pre>
     *
     * The weight of an enchantment is bound to its rarity. The higher the weight, the more likely
     * it is to be chosen.
     *
     * @return the relative probability of this enchantment being applied to an enchanted item
     * through the enchanting table or a loot table.
     * @since 1.8.4
     */
    public int getWeight() {
        return base.value().getWeight();
    }

    /**
     * Whether this enchantment is a curse. A curse is an enchantment that cannot be removed from an
     * item by an anvil, and it is drawn in red rather than grey, which is the same distinction
     * {@link #getRomanLevelName(int)} colours by.
     * example:
     * <pre>
     * // the curses, and how they differ from the rest
     * const all = Client.getRegistryManager().getEnchantments();
     * for (let i = 0; i !== all.size(); i += 1) {
     *     const ench = all.get(i);
     *     if (ench.isCursed()) {
     *       Chat.log(`${ench.getId()}: cursed, treasure ${ench.isTreasure()}, weight ${ench.getWeight()}`);
     *     }
     *   }
     * </pre>
     *
     * Curses are enchantments that can't be removed from the item they were applied to. They
     * usually only have one possible level and can't be upgraded. When combining items with curses
     * on them, they are transferred like any other enchantment. They can't be obtained through
     * enchantment tables, but rather from loot chests, fishing or trading with villagers.
     *
     * @return {@code true} if this enchantment is a curse, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCursed() {
        return base.is(EnchantmentTags.CURSE);
    }

    /**
     * Whether this enchantment is a treasure, which is the flag for the enchantments that do not
     * come out of an enchanting table. It is a separate flag from {@link #isCursed()}, and an
     * enchantment can be one without being the other.
     * example:
     * <pre>
     * // which enchantments cannot come from a table
     * const all = Client.getRegistryManager().getEnchantments();
     * for (let i = 0; i !== all.size(); i += 1) {
     *     const ench = all.get(i);
     *     if (ench.isTreasure()) {
     *       Chat.log(`${ench.getId()}: treasure, cursed ${ench.isCursed()}`);
     *     }
     *   }
     * </pre>
     *
     * Treasures are enchantments that can't be obtained through enchantment tables, but rather from
     * loot chests, fishing or trading with villagers.
     *
     * @return {@code true} if this enchantment is a treasure, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isTreasure() {
        return base.is(EnchantmentTags.TREASURE);
    }

    /**
     * Whether this enchantment can be applied to the given item at all. This asks about the item
     * type alone and takes no account of what is already on a particular stack, so it is the
     * question of whether the enchantment fits the item rather than whether this particular stack
     * could take it.
     * example:
     * <pre>
     * // which items an enchantment fits at all
     * const ench = Client.getRegistryManager().getEnchantment("minecraft:sharpness");
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:diamond_sword", "minecraft:iron_axe", "minecraft:diamond_pickaxe", "minecraft:stone"]) {
     *     Chat.log(`${id}: ${ench.canBeApplied(reg.getItem(id))}`);
     *   }
     * </pre>
     *
     * @param item the item to check
     * @return {@code true} if this enchantment can be applied to the given item type, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean canBeApplied(ItemHelper item) {
        return base.value().canEnchant(item.getRaw().getDefaultInstance());
    }

    /**
     * Whether this enchantment can be applied to the given stack. This is
     * {@link #canBeApplied(ItemHelper)} for the item type and the extra question of whether every
     * enchantment already on the stack is one this can sit beside, so a stack that already carries
     * something conflicting answers false even though the item itself would take it.
     * example:
     * <pre>
     * // a stack that already has something on it
     * const reg = Client.getRegistryManager();
     * const stack = reg.getItemStack("minecraft:diamond_sword", `[{id:"minecraft:sharpness",lvl:2}]`);
     * const onIt = stack.getEnchantments();
     * for (let i = 0; i !== onIt.size(); i += 1) {
     *     const ench = onIt.get(i);
     *     Chat.log(`${ench.getId()}: fits this sword ${ench.canBeApplied(stack)}`);
     *   }
     * </pre>
     *
     * @param item the item to check
     * @return {@code true} if this enchantment can be applied to the given item, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean canBeApplied(ItemStackHelper item) {
        return base.value().canEnchant(item.getRaw()) && item.getRaw().getEnchantments().keySet().stream().allMatch(e -> Enchantment.areCompatible(e, base));
    }

    /**
     * Every item type this enchantment will go on, as the whole list the game keeps for it. This is
     * a property of the enchantment rather than of any item in the world, and it is the list
     * {@link #canBeApplied(ItemHelper)} checks an item against.
     * example:
     * <pre>
     * // everything one enchantment will go on
     * const ench = Client.getRegistryManager().getEnchantment("minecraft:sharpness");
     * const items = ench.getAcceptableItems();
     * Chat.log(`${items.size()} item types`);
     * for (let i = 0; i !== items.size(); i += 1) {
     *   Chat.log(`  ${items.get(i).getId()}`);
     * }
     *
     * // and the other way round, everything one item will take
     * const sword = Client.getRegistryManager().getItem("minecraft:diamond_sword");
     * const all = Client.getRegistryManager().getEnchantments();
     * for (let i = 0; i !== all.size(); i += 1) {
     *   if (all.get(i).canBeApplied(sword)) {
     *     Chat.log(`  ${all.get(i).getId()}`);
     *   }
     * }
     * </pre>
     *
     * @return a list of all acceptable item ids for this enchantment.
     * @since 1.8.4
     */
    public List<ItemHelper> getAcceptableItems() {
        return base.value().definition().supportedItems().stream().map(e -> new ItemHelper(e.value())).toList();
    }

    /**
     * Whether this enchantment can sit on the same item as the named one. An enchantment is never
     * compatible with itself, so asking about this same enchantment answers {@code false}.<br>
     * The id is looked up in the connected server's registry, so an id that is not in it is an
     * error rather than a {@code false}.
     * example:
     * <pre>
     * // two enchantments against each other, both ways
     * const sharp = Client.getRegistryManager().getEnchantment("minecraft:sharpness");
     * const looting = Client.getRegistryManager().getEnchantment("minecraft:looting");
     * Chat.log(`sharpness with looting: ${sharp.isCompatible("minecraft:looting")}`);
     * Chat.log(`looting with sharpness: ${looting.isCompatible("minecraft:sharpness")}`);
     * Chat.log(`sharpness with sharpness: ${sharp.isCompatible("minecraft:sharpness")}`);
     * </pre>
     *
     * @param enchantment the enchantment to check
     * @return {@code true} if this enchantment is compatible with the given enchantment,
     * {@code false} otherwise.
     * @throws java.util.NoSuchElementException if the id is not in the enchantment registry.
     * @since 1.8.4
     */
    @DocletReplaceParams("enchantment: CanOmitNamespace<EnchantmentId>")
    public boolean isCompatible(String enchantment) {
        return Enchantment.areCompatible(mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .get(ResourceLocation.parse(enchantment)).orElseThrow(), base);
    }

    /**
     * Whether this enchantment can sit on the same item as the given one. The answer does not depend
     * on either level, so two helpers for the same enchantment at different levels answer the same
     * as the pair does. An enchantment is never compatible with itself.
     * example:
     * <pre>
     * // the level plays no part in the answer
     * const sharpAtOne = Client.getRegistryManager().getEnchantment("minecraft:sharpness", 1);
     * const sharpAtFive = Client.getRegistryManager().getEnchantment("minecraft:sharpness", 5);
     * const looting = Client.getRegistryManager().getEnchantment("minecraft:looting", 3);
     * Chat.log(`${sharpAtOne.isCompatible(looting)} and ${sharpAtFive.isCompatible(looting)}`);
     * Chat.log(`against itself: ${sharpAtOne.isCompatible(sharpAtFive)}`);
     * </pre>
     *
     * @param enchantment the enchantment to check
     * @return {@code true} if this enchantment is compatible with the given enchantment,
     * {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCompatible(EnchantmentHelper enchantment) {
        return Enchantment.areCompatible(enchantment.getRaw(), base);
    }

    /**
     * The opposite of {@link #isCompatible(String)}: whether the named enchantment cannot
     * sit on the same item as this one. Because the game never counts an enchantment as compatible
     * with itself, this answers {@code true} when the two are the same enchantment.
     * example:
     * <pre>
     * // conflicts, which is what compatibility is not
     * const sharp = Client.getRegistryManager().getEnchantment("minecraft:sharpness");
     * Chat.log(`sharpness with looting: ${sharp.conflictsWith("minecraft:looting")}`);
     * Chat.log(`sharpness with sharpness: ${sharp.conflictsWith("minecraft:sharpness")}`);
     * </pre>
     *
     * @param enchantment the enchantment to check
     * @return {@code true} if this enchantment conflicts with the given enchantment, {@code false}
     * otherwise.
     * @throws java.util.NoSuchElementException if the id is not in the enchantment registry.
     * @since 1.8.4
     */
    @DocletReplaceParams("enchantment: EnchantmentId")
    public boolean conflictsWith(String enchantment) {
        return !isCompatible(enchantment);
    }

    /**
     * The opposite of {@link #isCompatible(EnchantmentHelper)}: whether the given enchantment
     * cannot sit on the same item as this one, which is true when the two are the same enchantment.
     * example:
     * <pre>
     * // the two spellings of the same question
     * const sharp = Client.getRegistryManager().getEnchantment("minecraft:sharpness");
     * const looting = Client.getRegistryManager().getEnchantment("minecraft:looting");
     * Chat.log(`sharpness conflicts with looting: ${sharp.conflictsWith(looting)}`);
     * Chat.log(`and is its own conflict: ${sharp.conflictsWith(sharp)}`);
     *
     * // which of a set of enchantments all fit together
     * const wanted = ["minecraft:sharpness", "minecraft:unbreaking", "minecraft:looting"];
     * let clashes = 0;
     * for (const a of wanted) {
     *     for (const b of wanted) {
     *       if (a !== b) {
     *         if (Client.getRegistryManager().getEnchantment(a).conflictsWith(b)) {
     *           clashes += 1;
     *         }
     *       }
     *     }
     *   }
     *   Chat.log(`${clashes} conflicting pairs`);
     * </pre>
     *
     * @param enchantment the enchantment to check
     * @return {@code true} if this enchantment conflicts with the given enchantment, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean conflictsWith(EnchantmentHelper enchantment) {
        return !isCompatible(enchantment);
    }

    @Override
    public String toString() {
        return String.format("EnchantmentHelper:{\"id\": \"%s\", \"level\": %d}", getId(), getLevel());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EnchantmentHelper) || !super.equals(o)) {
            return false;
        }
        EnchantmentHelper that = (EnchantmentHelper) o;
        return level == 0 || that.level == 0 || level == that.level;
    }

    @Override
    public int hashCode() {
        return Objects.hash(base, level);
    }

}
