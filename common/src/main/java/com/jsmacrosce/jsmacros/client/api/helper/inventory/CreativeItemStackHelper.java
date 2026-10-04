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
 * A stack of an item with the setters on it, so a script can build one up rather than only read
 * one. A script gets one from {@link ItemStackHelper#getCreative()}, which wraps the same stack
 * with these methods on top, so nothing here is a copy: a setter that works changes the stack it
 * was called on, and every one gives the stack back so calls can be chained. Each method says
 * whether it does.<br>
 * The name for this is not that the stack is in the creative inventory. It is that these are the
 * calls a script makes to author an item, and what they write is the item's own data rather than
 * anything to do with where the stack sits.
 * example:
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
 * // the whole chain is one stack, read back at the end
 * Chat.log(`${stack.getCount()} of ${stack.getItemId()}, hidden ${stack.areEnchantmentsHidden()}`);
 * const held = Player.getPlayer().getMainHand();
 * Chat.log(`and the player is holding ${held.getItemId()}`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Items/Enchantments")
@SuppressWarnings("unused")
public class CreativeItemStackHelper extends ItemStackHelper {

    public CreativeItemStackHelper(ItemStack itemStack) {
        super(itemStack);
    }

    /**
     * Set how much damage this stack has taken, as the number itself. Damage counts up towards the
     * item's maximum and a stack at the maximum is broken, so this is the opposite direction from
     * {@link #setDurability(int)}, which takes what is left rather than what has gone.
     * example:
     * <pre>
     * // a stack that is nearly broken
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_pickaxe").getCreative();
     * stack.setDamage(1200);
     * Chat.log(`${stack.getDamage()} damage taken, ${stack.getDurability()} left`);
     *
     * // and the same state reached the other way round
     * stack.setDurability(239);
     * Chat.log(`${stack.getDamage()} damage taken, ${stack.getDurability()} left`);
     * </pre>
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
     * Set how much durability is left on this stack, which is the amount it can still take before
     * it breaks. This is worked out against the item's own maximum and turned into damage, so it is
     * the same setting as {@link #setDamage(int)} spelled the other way round.
     * example:
     * <pre>
     * // a nearly fresh tool
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_pickaxe").getCreative();
     * const max = stack.getMaxDurability();
     * stack.setDurability(max);
     * Chat.log(`full durability is ${stack.getDurability()} of ${max}`);
     *
     * stack.setDurability(1);
     * Chat.log(`one left is ${stack.getDurability()}`);
     * </pre>
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
     * Set how many of the item are in this stack. There is no check against the item's own stack
     * size here, so a count larger than {@link ItemStackHelper#getMaxCount()} is accepted and left
     * as it is.
     * example:
     * <pre>
     * // a full stack, and one beyond it
     * const stack = Client.getRegistryManager().getItemStack("minecraft:dirt").getCreative();
     * stack.setCount(stack.getMaxCount());
     * Chat.log(`${stack.getCount()} of ${stack.getItemId()}`);
     *
     * stack.setCount(99);
     * Chat.log(`and ${stack.getCount()}, which is more than a stack`);
     * </pre>
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
     * Give this stack a name of its own, as plain text. This is the name the game shows for it
     * everywhere, and it is set on the stack rather than on the item, so the item itself is
     * untouched. A name set this way is literal text with no colour codes in it.
     * example:
     * <pre>
     * // a renamed stack
     * const stack = Client.getRegistryManager().getItemStack("minecraft:stone").getCreative();
     * stack.setName("Foundation Stone");
     * Chat.log(stack.getName().getString());
     * // and the item behind it is unchanged
     * Chat.log(stack.getItem().getName());
     * </pre>
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
     * Give this stack a name of its own as text, which is the form to use when the name should
     * carry a colour or any other styling. It sets exactly what is given, replacing whatever name
     * was there.
     * example:
     * <pre>
     * // a coloured name, built as text rather than as a plain string
     * const name = Chat.createTextBuilder()
     *   .append("Rune Blade")
     *   .withColor(0x55FF55)
     *   .withFormatting(true, true, false, false, false)
     *   .build();
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_sword").getCreative();
     * stack.setName(name);
     * Chat.log(stack.getName().getString());
     * Chat.log(stack.getName().getStringStripFormatting());
     * </pre>
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
     * Put an enchantment on this stack at the given level, replacing the level it was at if it
     * already had that enchantment. The id may be written with or without its namespace.<br>
     * The game applies its own rules to what goes on, so an enchantment the item will not take is
     * not put on and a conflicting one displaces what was there.
     * example:
     * <pre>
     * // an enchanted tool, built up in a chain
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_sword").getCreative()
     *   .addEnchantment("minecraft:sharpness", 5)
     *   .addEnchantment("minecraft:unbreaking", 3)
     *   .addEnchantment("minecraft:mending", 1);
     * for (const ench of stack.getEnchantments()) {
     *   Chat.log(ench.getRomanLevelName().getString());
     * }
     *
     * // setting the same enchantment again replaces the level
     * stack.addEnchantment("minecraft:sharpness", 2);
     * const sharp = stack.getEnchantment("minecraft:sharpness");
     * if (sharp !== null) {
     *   Chat.log(`now level ${sharp.getLevel()}`);
     * }
     * </pre>
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
     * Put an enchantment on this stack at the level the given helper carries. A helper that came
     * from the registry rather than from an item has no level, and a level of zero is what the
     * game reads as "take this off" rather than as "put this on".
     * example:
     * <pre>
     * // from a registry lookup, with the level spelled out
     * const ench = Client.getRegistryManager().getEnchantment("minecraft:fortune", 3);
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_pickaxe").getCreative()
     *   .addEnchantment(ench);
     * const on = stack.getEnchantment("minecraft:fortune");
     * if (on !== null) {
     *   Chat.log(`fortune ${on.getLevel()}`);
     * }
     * </pre>
     *
     * @param enchantment the enchantment to add
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper addEnchantment(EnchantmentHelper enchantment) {
        return addEnchantment(enchantment.getRaw(), enchantment.getLevel());
    }

    /**
     * The form both public enchantment calls end up at: the game is asked to put the enchantment on
     * at that level, and the stack is handed straight back.
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
     * Take every enchantment off this stack, which is the blunt version of
     * {@link #removeEnchantment(String)}. What is left behind is a stack with no enchantment
     * component set on it rather than one with an empty list.
     * example:
     * <pre>
     * // strip a tool of everything on it
     * const stack = Client.getRegistryManager()
     *   .getItemStack("minecraft:diamond_sword", `[{id:"minecraft:sharpness",lvl:5},{id:"minecraft:unbreaking",lvl:3}]`)
     *   .getCreative();
     * Chat.log(`before: ${stack.getEnchantments().size()} enchantments`);
     *
     * stack.clearEnchantments();
     * Chat.log(`after: ${stack.getEnchantments().size()}, still enchanted ${stack.isEnchanted()}`);
     * </pre>
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper clearEnchantments() {
        base.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        return this;
    }

    /**
     * Ask for one enchantment to be taken off this stack, naming the enchantment with a helper. The
     * level on the helper is not part of the question: any level of that enchantment is what gets
     * taken off.
     * <p>
     * Writes the updated enchantment component back to the stack while preserving other
     * enchantments. An enchantment that is already absent leaves the stack unchanged.
     * example:
     * <pre>
     * // remove one enchantment while keeping the others
     * const ench = Client.getRegistryManager().getEnchantment("minecraft:sharpness");
     * const stack = Client.getRegistryManager()
     *   .getItemStack("minecraft:diamond_sword").getCreative()
     *   .addEnchantment("minecraft:sharpness", 5)
     *   .addEnchantment("minecraft:unbreaking", 3);
     * stack.removeEnchantment(ench);
     * Chat.log(`sharpness removed: ${stack.getEnchantment("minecraft:sharpness") === null}`);
     *
     * // clearing them all does work
     * stack.clearEnchantments();
     * Chat.log(`after clearing: ${stack.getEnchantment("minecraft:sharpness") === null}`);
     * </pre>
     *
     * @param enchantment the enchantment to remove
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper removeEnchantment(EnchantmentHelper enchantment) {
        return removeEnchantment(enchantment.getId());
    }

    /**
     * Ask for one enchantment to be taken off this stack, naming it by id. The id must be in full
     * with its namespace, and the level is not part of the question: any level of that enchantment
     * is what is taken off.
     * <p>
     * Writes the updated enchantment component back to the stack while preserving other
     * enchantments. An enchantment that is already absent leaves the stack unchanged.
     * example:
     * <pre>
     * // remove the named enchantment
     * const stack = Client.getRegistryManager()
     *   .getItemStack("minecraft:diamond_sword").getCreative()
     *   .addEnchantment("minecraft:sharpness", 5);
     * stack.removeEnchantment("minecraft:sharpness");
     * Chat.log(stack.getEnchantment("minecraft:sharpness") === null);
     *
     * // removing the same enchantment again is harmless
     * const ench = Client.getRegistryManager().getEnchantment("minecraft:sharpness");
     * stack.removeEnchantment(ench);
     * Chat.log(stack.getEnchantments().size());
     * </pre>
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
     * Take every line of lore off this stack, leaving it with an empty lore rather than none. To
     * put lore on, {@link #setLore(Object...)} replaces it all in one go and
     * {@link #addLore(Object...)} puts more on after what is there.
     * example:
     * <pre>
     * // lore, then no lore
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_sword").getCreative();
     * stack.setLore("Forged in the first age", "Bound to its bearer");
     * for (const line of stack.getLore()) {
     *   Chat.log(line.getString());
     * }
     *
     * stack.clearLore();
     * Chat.log(`lines left: ${stack.getLore().size()}`);
     * </pre>
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper clearLore() {
        base.set(DataComponents.LORE, ItemLore.EMPTY);
        return this;
    }

    /**
     * Replace this stack's lore with the given lines. Anything already on it is replaced rather than
     * added to, so this and {@link #addLore(Object...)} together are the two ways to put lore on.
     * <p>
     * Each argument is a line. A {@link TextHelper} is taken as text with whatever styling it
     * carries, a {@link TextBuilder} is built and taken that way, and anything else is turned into
     * literal text through its own {@code toString}.
     * example:
     * <pre>
     * // lore as plain strings
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_sword").getCreative();
     * stack.setLore("Forged in the first age", "Bound to its bearer");
     * for (const line of stack.getLore()) {
     *   Chat.log(line.getString());
     * }
     *
     * // and with a styled line among them
     * const fancy = Chat.createTextBuilder()
     *   .append("Drops from the Warden")
     *   .withColor(0x00AAAA)
     *   .build();
     * stack.setLore("Forged in the first age", fancy);
     * Chat.log(stack.getLore().get(1).getStringStripFormatting());
     * </pre>
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
     * Put more lines of lore on this stack, after whatever is already there rather than instead of
     * it. A line is a {@link TextHelper} with its own styling, a {@link TextBuilder} that is built
     * first, or anything else turned into literal text through its own {@code toString}.
     * example:
     * <pre>
     * // build the lore up a line at a time
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_sword").getCreative();
     * stack.addLore("First line");
     * stack.addLore("Second line", "Third line");
     * for (const line of stack.getLore()) {
     *   Chat.log(line.getString());
     * }
     *
     * // a line built as styled text rather than as a string
     * stack.addLore(Chat.createTextBuilder().append("Rarity: legendary").withColor(0xFFAA00).build());
     * Chat.log(stack.getLore().get(3).getString());
     * </pre>
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
     * Whether this stack is unbreakable, as the flag the game reads when it decides whether to
     * take durability off it. Setting it to false takes the flag off again, which is the same as a
     * stack that never had it.
     * example:
     * <pre>
     * // an unbreakable tool
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_pickaxe").getCreative();
     * stack.setDurability(1).setUnbreakable(true);
     * stack.setDamage(1500);
     * Chat.log(`unbreakable ${stack.isUnbreakable()}, still has durability ${stack.isDamageable()}`);
     *
     * // and back again
     * stack.setUnbreakable(false);
     * Chat.log(`now ${stack.isUnbreakable()}`);
     * </pre>
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
     * Whether the enchantments on this stack are hidden from the tooltip. Hiding something is a
     * property of the stack, so a hidden stack is still enchanted and
     * {@link ItemStackHelper#getEnchantments()} still reads them back; all this changes is what the
     * game's own tooltip shows.
     * example:
     * <pre>
     * // a stack whose enchantments the tooltip will not show
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_sword").getCreative()
     *   .addEnchantment("minecraft:sharpness", 5)
     *   .hideEnchantments(true);
     * Chat.log(`hidden ${stack.areEnchantmentsHidden()}, still enchanted ${stack.isEnchanted()}`);
     *
     * stack.hideEnchantments(false);
     * Chat.log(`shown again: ${stack.areEnchantmentsHidden()}`);
     * </pre>
     *
     * @param hide whether to hide the enchantments or not
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper hideEnchantments(boolean hide) {
        return hideComponent(DataComponents.ENCHANTMENTS, hide);
    }

    /**
     * Whether the attribute modifiers on this stack are hidden from the tooltip, so the extra damage
     * or armour a stack would give is left off what the game shows.
     * example:
     * <pre>
     * // hide the extra damage a stack gives
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_sword").getCreative();
     * stack.hideModifiers(true);
     * Chat.log(`modifiers hidden ${stack.areModifiersHidden()}, damage still ${stack.getAttackDamage()}`);
     * </pre>
     *
     * @param hide whether to hide attributes and modifiers or not
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper hideModifiers(boolean hide) {
        return hideComponent(DataComponents.ATTRIBUTE_MODIFIERS, hide);
    }

    /**
     * Whether the unbreakable marker is hidden from the tooltip, so the game does not say the item
     * is unbreakable. The item still is; only the line on the tooltip is left off.
     * example:
     * <pre>
     * // an unbreakable tool that does not say so
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond_pickaxe").getCreative()
     *   .setUnbreakable(true)
     *   .hideUnbreakable(true);
     * Chat.log(`unbreakable ${stack.isUnbreakable()}, hidden ${stack.isUnbreakableHidden()}`);
     * </pre>
     *
     * @param hide whether to hide the unbreakable flag or not
     * @return self for chaining.
     * @since 1.8.4
     */

    public CreativeItemStackHelper hideUnbreakable(boolean hide) {
        return hideComponent(DataComponents.UNBREAKABLE, hide);
    }

    /**
     * Whether the can-destroy restriction on this stack is hidden from the tooltip. The flag is an
     * adventure mode restriction, so this is about a stack that is already carrying one.
     * example:
     * <pre>
     * // a stack with a can destroy filter, hidden from the tooltip
     * const stack = Client.getRegistryManager()
     *   .getItemStack("minecraft:diamond_pickaxe", `[can_break=[{blocks:"minecraft:stone"}]]`)
     *   .getCreative();
     * Chat.log(`has the flag ${stack.hasDestroyRestrictions()}, hidden ${stack.isCanDestroyHidden()}`);
     * stack.hideCanDestroy(true);
     * Chat.log(`now hidden ${stack.isCanDestroyHidden()}`);
     * </pre>
     *
     * @param hide whether to hide the blocks this item can destroy or not
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper hideCanDestroy(boolean hide) {
        return hideComponent(DataComponents.CAN_BREAK, hide);
    }

    /**
     * Whether the can-place-on restriction on this stack is hidden from the tooltip, which is the
     * other of the two adventure mode restrictions.
     * example:
     * <pre>
     * // a stack with a can place on filter
     * const stack = Client.getRegistryManager()
     *   .getItemStack("minecraft:diamond_pickaxe", `[can_place_on=[{blocks:"minecraft:stone"}]]`)
     *   .getCreative();
     * Chat.log(`has the flag ${stack.hasPlaceRestrictions()}, hidden ${stack.isCanPlaceHidden()}`);
     * stack.hideCanPlace(true);
     * Chat.log(`now hidden ${stack.isCanPlaceHidden()}`);
     * </pre>
     *
     * @param hide whether to hide the blocks this item can be placed on or not
     * @return self for chaining.
     * @since 1.8.4
     */
    public CreativeItemStackHelper hideCanPlace(boolean hide) {
        return hideComponent(DataComponents.CAN_PLACE_ON, hide);
    }

    /**
     * Whether the colour of coloured leather armour is hidden from the tooltip, so the game does not
     * say what colour the item is dyed.
     * example:
     * <pre>
     * // dyed armour that does not say what colour it is
     * const stack = Client.getRegistryManager()
     *   .getItemStack("minecraft:leather_helmet", `[dyed_color={color:"minecraft:red",rgb:0}]`)
     *   .getCreative();
     * Chat.log(`dye hidden ${stack.isDyeHidden()}`);
     * stack.hideDye(true);
     * Chat.log(`now ${stack.isDyeHidden()}`);
     * </pre>
     *
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
