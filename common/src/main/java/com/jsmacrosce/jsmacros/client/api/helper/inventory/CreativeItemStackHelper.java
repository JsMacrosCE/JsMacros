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
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.jsmacros.client.api.classes.TextBuilder;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;

import java.util.Arrays;

import static net.minecraft.network.chat.Component.literal;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class CreativeItemStackHelper extends ItemStackHelper {

    public CreativeItemStackHelper(ItemStack itemStack) {
        super(itemStack);
    }

    /**
     * @param damage the damage the item should take
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper setDamage(int damage) {
        base.setDamageValue(damage);
        return this;
    }

    /**
     * @param durability the new durability of this item
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper setDurability(int durability) {
        base.setDamageValue(base.getMaxDamage() - durability);
        return this;
    }

    /**
     * @param count the new count of the item
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper setCount(int count) {
        base.setCount(count);
        return this;
    }

    /**
     * @param name the new name of the item
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper setName(String name) {
        base.set(DataComponents.CUSTOM_NAME, literal(name));
        return this;
    }

    /**
     * @param name the new name of the item
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper setName(TextHelper name) {
        base.set(DataComponents.CUSTOM_NAME, name.getRaw());
        return this;
    }

    /**
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
     * @param enchantment the enchantment to add
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper addEnchantment(EnchantmentHelper enchantment) {
        return addEnchantment(enchantment.getRaw(), enchantment.getLevel());
    }

    protected CreativeItemStackHelper addEnchantment(Holder<Enchantment> enchantment, int level) {
        base.enchant(enchantment, level);
        return this;
    }

    /**
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper clearEnchantments() {
        base.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        return this;
    }

    /**
     * @param enchantment the enchantment to remove
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper removeEnchantment(EnchantmentHelper enchantment) {
        return removeEnchantment(enchantment.getId());
    }

    /**
     * @param id the id of the enchantment to remove
     * @return self for chaining.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: EnchantmentId")
    public CreativeItemStackHelper removeEnchantment(String id) {
        ItemEnchantments enchantments = base.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(enchantments);
        builder.removeIf((e) -> e.is(ResourceLocation.parse(id)));

        return this;
    }

    /**
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper clearLore() {
        base.set(DataComponents.LORE, ItemLore.EMPTY);
        return this;
    }

    /**
     * @param lore the new lore
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper setLore(Object... lore) {
        clearLore();
        return addLore(lore);
    }

    /**
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
     * @param hide whether to hide the enchantments or not
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper hideEnchantments(boolean hide) {
        return hideComponent(DataComponents.ENCHANTMENTS, hide);
    }

    /**
     * @param hide whether to hide attributes and modifiers or not
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper hideModifiers(boolean hide) {
        return hideComponent(DataComponents.ATTRIBUTE_MODIFIERS, hide);
    }

    /**
     * @param hide whether to hide the unbreakable flag or not
     * @return self for chaining.
     * @since 1.8.4
     */

    public CreativeItemStackHelper hideUnbreakable(boolean hide) {
        return hideComponent(DataComponents.UNBREAKABLE, hide);
    }

    /**
     * @param hide whether to hide the blocks this item can destroy or not
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper hideCanDestroy(boolean hide) {
        return hideComponent(DataComponents.CAN_BREAK, hide);
    }

    /**
     * @param hide whether to hide the blocks this item can be placed on or not
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper hideCanPlace(boolean hide) {
        return hideComponent(DataComponents.CAN_PLACE_ON, hide);
    }

    /**
     * @param hide whether to hide the color of colored leather armor or not
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
