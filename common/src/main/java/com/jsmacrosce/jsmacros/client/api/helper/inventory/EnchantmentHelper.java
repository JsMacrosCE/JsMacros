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
 * Represents an enchantment and an optional level. Helpers constructed without a level report zero;
 * helpers obtained from an item carry its enchantment level. Compatibility is independent of level;
 * an enchantment is incompatible with itself, so it appears in its conflicting list and not its
 * compatible list.
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

    /** Wraps an enchantment holder and stores level zero.
     * @param base the enchantment holder to wrap
     */
    public EnchantmentHelper(Holder<Enchantment> base) {
        this(base, 0);
    }

    /** Wraps an enchantment holder and stores the supplied level without validation or clamping.
     * @param base the enchantment holder to wrap
     * @param level the level stored by this helper
     */
    public EnchantmentHelper(Holder<Enchantment> base, int level) {
        super(base);
        this.level = level;
    }

    /** Resolves an enchantment identifier through the current connection's enchantment registry.
     * Requires a client connection and a matching registry entry; lookup failures propagate. The
     * resulting helper stores level zero.
     * @param enchantment the identifier to look up
     */
    @DocletReplaceParams("enchantment: CanOmitNamespace<EnchantmentId>")
    public EnchantmentHelper(String enchantment) {
        this(mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ResourceLocation.parse(enchantment)).orElseThrow());
    }

    /**
     * The level carried by this helper, or zero if it was constructed without one. Helpers from
     * {@link ItemStackHelper#getEnchantments()} carry the item's level.
     *
     * @return the level of this enchantment.
     * @since 1.8.4
     */
    public int getLevel() {
        return level;
    }

    /**
     * The minimum level defined for this enchantment.
     *
     * @return the minimum possible level of this enchantment that one can get in vanilla.
     * @since 1.8.4
     */
    public int getMinLevel() {
        return base.value().getMinLevel();
    }

    /**
     * The maximum level defined for this enchantment; this does not restrict levels passed to the
     * name methods.
     *
     * @return the maximum possible level of this enchantment that one can get in vanilla.
     * @since 1.8.4
     */
    public int getMaxLevel() {
        return base.value().getMaxLevel();
    }

    /**
     * The translated full name at {@code level}, as a plain string without text styling. The level
     * is supplied explicitly and need not be the helper's stored level.
     *
     * @param level the level for the name
     * @return the translated name and, when applicable, the translated level suffix
     * @since 1.8.4
     */
    public String getLevelName(int level) {
        return Enchantment.getFullname(base, level).getString();
    }

    /**
     * The styled name at this helper's stored level. The level suffix uses Roman numerals for levels
     * 1–3999 and decimal digits otherwise; level 1 is omitted only when the enchantment's maximum is
     * 1. A helper constructed without a level therefore produces a level-zero suffix.
     *
     * @return the translated name of this enchantment for the given level in roman numerals.
     * @since 1.8.4
     */
    public TextHelper getRomanLevelName() {
        return getRomanLevelName(level);
    }

    /**
     * The styled name at {@code level}, with a Roman numeral suffix for levels 1–3999 and decimal
     * digits otherwise. Level 1 is omitted only when the enchantment's maximum level is 1.
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
     * The translated description of this enchantment without a level suffix.
     *
     * @return the name of this enchantment.
     * @since 1.8.4
     */
    public String getName() {
        return base.value().description().getString();
    }

    /**
     * The full registry id of this enchantment.
     *
     * @return the id of this enchantment.
     * @since 1.8.4
     */
    @DocletReplaceReturn("EnchantmentId")
    public String getId() {
        return base.getRegisteredName();
    }

    /**
     * Enchantments from the registry that are incompatible with this one, including this
     * enchantment itself. Returned helpers have level zero. The self-entry follows from
     * {@link Enchantment#areCompatible(Holder, Holder)} treating an enchantment as incompatible
     * with itself; {@link #getCompatibleEnchantments()} omits it.
     *
     * @return a list of all enchantments that conflict with this one.
     * @since 1.8.4
     */
    public List<EnchantmentHelper> getConflictingEnchantments() {
        return getConflictingEnchantments(false);
    }

    /**
     * Returns the same incompatible-enchantment list as {@link #getConflictingEnchantments()}.
     * The parameter is unused; results are not filtered by target type. Returned helpers have level zero.
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
     * Enchantments from the registry compatible with this one. The list omits this enchantment
     * itself, and returned helpers have level zero.
     *
     * @return a list of all enchantments that can be combined with this one.
     * @since 1.8.4
     */
    public List<EnchantmentHelper> getCompatibleEnchantments() {
        return getCompatibleEnchantments(false);
    }

    /**
     * Returns the same compatible-enchantment list as {@link #getCompatibleEnchantments()}.
     * The parameter is unused; results are not filtered by target type. Returned helpers have level zero.
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
     * The enchantment's configured weight, used by the game when selecting enchantments.
     *
     * @return the configured weight.
     * @since 1.8.4
     */
    public int getWeight() {
        return base.value().getWeight();
    }

    /**
     * Whether this enchantment has the curse tag. Curses are styled red by
     * {@link #getRomanLevelName(int)}.
     *
     * @return {@code true} if this enchantment is a curse, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCursed() {
        return base.is(EnchantmentTags.CURSE);
    }

    /**
     * Whether this enchantment has the treasure tag. This tag is independent of the curse tag
     * checked by {@link #isCursed()}.
     *
     * @return {@code true} if this enchantment is a treasure, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isTreasure() {
        return base.is(EnchantmentTags.TREASURE);
    }

    /**
     * Whether this enchantment supports the item's type. This checks the item type only; it does not
     * check existing enchantments on a stack.
     *
     * @param item the item to check
     * @return {@code true} if the item's type is supported, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean canBeApplied(ItemHelper item) {
        return base.value().canEnchant(item.getRaw().getDefaultInstance());
    }

    /**
     * Whether this enchantment supports the stack's item type and is compatible with every
     * enchantment already on the stack.
     *
     * @param item the item to check
     * @return {@code true} if both checks pass, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean canBeApplied(ItemStackHelper item) {
        return base.value().canEnchant(item.getRaw()) && item.getRaw().getEnchantments().keySet().stream().allMatch(e -> Enchantment.areCompatible(e, base));
    }

    /**
     * The item types supported by this enchantment. This is the item-type criterion used by
     * {@link #canBeApplied(ItemHelper)}; it does not account for stack contents.
     *
     * @return the supported item types
     * @since 1.8.4
     */
    public List<ItemHelper> getAcceptableItems() {
        return base.value().definition().supportedItems().stream().map(e -> new ItemHelper(e.value())).toList();
    }

    /**
     * Whether this enchantment is compatible with the enchantment identified by {@code enchantment}.
     * An enchantment is not compatible with itself. The id is resolved in the connected server's
     * registry; an unknown id throws rather than returning {@code false}.
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
     * Whether this enchantment is compatible with the given enchantment. Levels do not affect
     * compatibility, and an enchantment is not compatible with itself.
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
     * Whether the enchantment identified by {@code enchantment} is incompatible with this one.
     * An enchantment conflicts with itself. The id is resolved in the connected server's registry;
     * an unknown id throws rather than returning {@code false}.
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
     * Whether the given enchantment is incompatible with this one. An enchantment conflicts with
     * itself.
     *
     * @param enchantment the enchantment to check
     * @return {@code true} if this enchantment conflicts with the given enchantment, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean conflictsWith(EnchantmentHelper enchantment) {
        return !isCompatible(enchantment);
    }

    /** Returns a diagnostic string containing this helper's enchantment id and stored level; it is not a serialization format.
     * @return a diagnostic representation of this helper
     */
    @Override
    public String toString() {
        return String.format("EnchantmentHelper:{\"id\": \"%s\", \"level\": %d}", getId(), getLevel());
    }

    /** Compares this helper with another enchantment helper. The wrapped holders must compare equal; level zero on either helper matches any level, so equality is not transitive for distinct nonzero levels.
     * @param o the object to compare
     * @return {@code true} for the same instance or a matching helper under the holder and level rules; {@code false} otherwise
     */
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

    /** Computes a hash from the wrapped holder and stored level. Because equality treats level zero as a wildcard, equal helpers can have different hash codes; this can affect hash-based collections.
     * @return the hash of the wrapped holder and stored level
     */
    @Override
    public int hashCode() {
        return Objects.hash(base, level);
    }

}
