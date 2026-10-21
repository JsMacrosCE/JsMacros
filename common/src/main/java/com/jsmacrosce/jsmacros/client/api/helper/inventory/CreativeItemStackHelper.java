package com.jsmacrosce.jsmacros.client.api.helper.inventory;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.jsmacros.client.api.classes.TextBuilder;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;

import java.util.Arrays;

import static net.minecraft.network.chat.Component.literal;

/**
 * Wraps an {@link ItemStackHelper} stack with methods for changing its components. It holds the
 * same stack, not a copy; setters mutate it and return this helper for chaining.
 * <pre>
 * // build a named, enchanted, glowing tool
 * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_sword")
 *   .getCreative()
 *   .setName("Excalibur")
 *   .addEnchantment("minecraft:sharpness", 5)
 *   .addEnchantment("minecraft:unbreaking", 3)
 *   .setDurability(1200)
 *   .hideEnchantments(false);
 *
 * Chat.log(`${stack.getName()} at ${stack.getDurability()} durability`);
 * for (const ench of stack.getEnchantments()) {
 *   Chat.log(`  ${ench.getRomanLevelName()}`);
 * }
 *
 * // the helper and original refer to the same stack
 * Chat.log(`${stack.getCount()} of ${stack.getItemId()}`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Items/Enchantments")
@SuppressWarnings("unused")
public class CreativeItemStackHelper extends ItemStackHelper {

    /**
     * Wraps the supplied stack for component editing; the helper retains the same stack reference.
     *
     * @param itemStack the stack to wrap and edit
     */
    public CreativeItemStackHelper(ItemStack itemStack) {
        super(itemStack);
    }

    /**
     * Sets the damage value on this stack. Damage counts up; use {@link #setDurability(int)} to
     * specify the remaining durability instead.
     *
     * @param damage the damage the item should take
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper setDamage(int damage) {
        base.setDamageValue(damage);
        return this;
    }

    /**
     * Sets remaining durability by storing the item's maximum damage minus this value.
     *
     * @param durability the new durability of this item
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper setDurability(int durability) {
        base.setDamageValue(base.getMaxDamage() - durability);
        return this;
    }

    /**
     * Sets the stack count without clamping it to the item's maximum stack size.
     *
     * @param count the new count of the item
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper setCount(int count) {
        base.setCount(count);
        return this;
    }

    /**
     * Sets this stack's custom name to literal text; the item type's name is unchanged.
     *
     * @param name the new name of the item
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper setName(String name) {
        base.set(DataComponents.CUSTOM_NAME, literal(name));
        return this;
    }

    /**
     * Sets this stack's custom name to the supplied text, including its styling.
     *
     * @param name the new name of the item
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper setName(TextHelper name) {
        base.set(DataComponents.CUSTOM_NAME, name.getRaw());
        return this;
    }

    /**
     * Upgrades or adds the identified enchantment at the requested level. The id may omit its
     * namespace; the enchantment component handles level replacement and compatibility.
     *
     * @param id    the id of the enchantment
     * @param level the level of the enchantment
     * @return self for chaining.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: CanOmitNamespace<EnchantmentId>, level: int")
    public CreativeItemStackHelper addEnchantment(String id, int level) {
        return addEnchantment(mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .get(ResourceLocation.parse(id)).orElseThrow(), level);
    }

    /**
     * Adds or upgrades the enchantment using the level carried by the helper. Registry-only
     * helpers have no level; use the id-and-level overload when specifying one.
     *
     * @param enchantment the enchantment to add
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper addEnchantment(EnchantmentHelper enchantment) {
        return addEnchantment(enchantment.getRaw(), enchantment.getLevel());
    }

    /**
     * Applies the enchantment at the given level and returns this helper.
     *
     * @param enchantment the enchantment to add, as the game holds it.
     * @param level       the level to put it on at.
     * @return self for chaining.
     * @since 1.8.4
     */
    protected CreativeItemStackHelper addEnchantment(Holder<Enchantment> enchantment, int level) {
        base.enchant(enchantment, level);
        return this;
    }

    /**
     * Replaces this stack's enchantments component with the empty component.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper clearEnchantments() {
        base.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        return this;
    }

    /**
     * Removes all levels of the enchantment identified by the helper, preserving other enchantments.
     *
     * @param enchantment the enchantment to remove
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper removeEnchantment(EnchantmentHelper enchantment) {
        return removeEnchantment(enchantment.getId());
    }

    /**
     * Removes all levels of the enchantment with this identifier, preserving other enchantments.
     * The identifier must include its namespace.
     *
     * @param id the id of the enchantment to remove
     * @return self for chaining.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: EnchantmentId")
    public CreativeItemStackHelper removeEnchantment(String id) {
        ItemEnchantments enchantments = base.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(enchantments);
        builder.removeIf((e) -> e.is(ResourceLocation.parse(id)));
        base.set(DataComponents.ENCHANTMENTS, builder.toImmutable());

        return this;
    }

    /**
     * Sets this stack's lore to an empty list.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper clearLore() {
        base.set(DataComponents.LORE, ItemLore.EMPTY);
        return this;
    }

    /**
     * Replaces this stack's lore with the supplied lines.
     * <p>
     * Each argument is a line. A {@link TextHelper} is taken as text with whatever styling it
     * carries, a {@link TextBuilder} is built and taken that way, and anything else is turned into
     * literal text through its own {@code toString}.
     *
     * @param lore the new lore
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper setLore(Object... lore) {
        clearLore();
        return addLore(lore);
    }

    /**
     * Appends lore lines. A {@link TextHelper} retains its styling, a {@link TextBuilder} is built
     * first, and other values are converted to literal text using {@code toString}.
     *
     * @param lore the lore to add
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper addLore(Object... lore) {
        return addLoreInternal(Arrays.stream(lore).map(e -> {
            if (e instanceof TextHelper) {
                return ((TextHelper) e).getRaw();
            } else if (e instanceof TextBuilder) {
                return ((TextBuilder) e).build().getRaw();
            } else {
                return literal(e.toString());
            }
        }).toArray(Component[]::new));
    }

    /**
     * @param texts the lore to add
     * @return self for chaining.
     * @since 1.8.4
     */
    private CreativeItemStackHelper addLoreInternal(Component... texts) {
        base.set(DataComponents.LORE, new ItemLore(Arrays.asList(texts)));
        return this;
    }

    /**
     * Sets or removes the unbreakable component on this stack.
     *
     * @param unbreakable whether the item should be unbreakable or not
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper setUnbreakable(boolean unbreakable) {
        if (unbreakable) {
            base.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        } else {
            base.remove(DataComponents.UNBREAKABLE);
        }
        return this;
    }

    /**
     * Hides or shows the enchantment component in the stack's tooltip; this does not remove its enchantments.
     *
     * @param hide whether to hide the enchantments or not
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper hideEnchantments(boolean hide) {
        return hideComponent(DataComponents.ENCHANTMENTS, hide);
    }

    /**
     * Hides or shows the stack's attribute modifiers in its tooltip.
     *
     * @param hide whether to hide attributes and modifiers or not
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper hideModifiers(boolean hide) {
        return hideComponent(DataComponents.ATTRIBUTE_MODIFIERS, hide);
    }

    /**
     * Hides or shows the unbreakable component in the tooltip without changing whether the stack is unbreakable.
     *
     * @param hide whether to hide the unbreakable flag or not
     * @return self for chaining.
     * @since 1.8.4
     */

    public CreativeItemStackHelper hideUnbreakable(boolean hide) {
        return hideComponent(DataComponents.UNBREAKABLE, hide);
    }

    /**
     * Hides or shows this stack's can-destroy component in its tooltip without changing the
     * restriction itself.
     *
     * @param hide whether to hide the restriction in the tooltip
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper hideCanDestroy(boolean hide) {
        return hideComponent(DataComponents.CAN_BREAK, hide);
    }

    /**
     * Hides or shows this stack's can-place-on component in its tooltip without changing the
     * restriction itself.
     *
     * @param hide whether to hide the restriction in the tooltip
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper hideCanPlace(boolean hide) {
        return hideComponent(DataComponents.CAN_PLACE_ON, hide);
    }

    /**
     * Hides or shows this stack's dyed-color component in its tooltip without changing its color.
     *
     * @param hide whether to hide the dyed color in the tooltip
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper hideDye(boolean hide) {
        return hideComponent(DataComponents.DYED_COLOR, hide);
    }

    private CreativeItemStackHelper hideComponent(DataComponentType<?> type, boolean hide) {
        base.set(DataComponents.TOOLTIP_DISPLAY,
            base.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT).withHidden(type, hide));
        return this;
    }

}
