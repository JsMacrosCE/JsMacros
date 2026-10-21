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
import com.jsmacrosce.doclet.DocletIgnore;
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
 * A script-facing view of an item stack, including its count and components. Use
 * {@link com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper#getItemStack(String)} to obtain
 * one from the registry, or get one from an entity with
 * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.LivingEntityHelper#getMainHand()}.
 * {@link #getCreative()} exposes setters that modify this same stack; use {@link #copy()} first to
 * preserve the original.
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

    /** Wraps a newly constructed stack for the item registry entry identified by {@code id}.
     *
     * @param id item identifier; if it has no namespace, {@code minecraft} is used
     * @param count requested stack count passed to the native stack constructor
     */
    @DocletReplaceParams("id: CanOmitNamespace<ItemId>, count: int")
    public ItemStackHelper(String id, int count) {
        super(new ItemStack(BuiltInRegistries.ITEM.getValue(RegistryHelper.parseIdentifier(id)), count));
    }

    /** Wraps the supplied stack reference without copying it.
     *
     * @param i stack to wrap
     */
    public ItemStackHelper(ItemStack i) {
        super(i);
    }

    /**
     * Sets the damage value. This mutates the stack; {@link #copy()} it first to preserve the
     * original. Prefer {@link CreativeItemStackHelper#setDamage(int)} for the creative-helper return.
     *
     * @param damage the damage to set on this stack.
     * @return self
     * @since 1.2.0
     * @deprecated use {@link CreativeItemStackHelper#setDamage(int)} instead, which returns the
     * creative helper so the result can be used directly.
     */
    @DocletIgnore
    @Deprecated
    public ItemStackHelper setDamage(int damage) {
        base.setDamageValue(damage);
        return this;
    }

    /**
     * Whether this stack can take damage. Returns {@code false} if it is unbreakable or lacks any
     * of the max-damage and damage components.
     *
     * @return {@code true} if this stack can take damage, {@code false} otherwise.
     * @since 1.2.0
     */
    public boolean isDamageable() {
        return base.isDamageableItem();
    }

    /**
     * Whether this stack has the unbreakable component. This is distinct from
     * {@link #isUnbreakableHidden()}, which reports tooltip visibility.
     *
     * @return {@code true} if this item is unbreakable, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isUnbreakable() {
        return base.get(DataComponents.UNBREAKABLE) != null;
    }

    /**
     * Whether this stack has the enchantable component and has no enchantments yet.
     *
     * @return {@code true} if this item can be enchanted, {@code false} otherwise.
     * @since 1.2.0
     */
    public boolean isEnchantable() {
        return base.isEnchantable();
    }

    /**
     * Whether this stack has any enchantments, regardless of whether its item is enchantable.
     *
     * @return {@code true} if the item is enchanted, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isEnchanted() {
        return base.isEnchanted();
    }

    /**
     * Returns each enchantment on this stack with its level. No enchantments produces an empty
     * list; each call creates a new list.
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
     * Returns the matching enchantment and its level, or {@code null} if the stack does not carry
     * it. Accepts a namespaced or unnamespaced id, or the enchantment's translated name.
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
     * Whether this enchantment supports this stack's item type and is compatible with all
     * enchantments already on it. Unlike {@link EnchantmentHelper#canBeApplied(ItemHelper)}, this
     * checks the stack's current enchantments too.
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
     * Whether this stack carries the given enchantment. A helper without a level matches any level;
     * two helpers with levels match only when their levels are equal.
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
     * Whether this stack carries the enchantment with the given id, regardless of its level.
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
     * Enchantments in the registry whose item-type criteria accept this stack. Existing
     * enchantments and their compatibility are not considered.
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
     * The subset of {@link #getPossibleEnchantments()} whose enchantments are in the game's
     * {@code IN_ENCHANTING_TABLE} tag and can be applied to this stack.
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
     * Returns this stack's lore lines wrapped as text. An absent lore component produces an empty
     * list; changing the returned list does not change the stack. Use {@link #getCreative()} to
     * edit the stack's lore.
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
     * The stack's effective maximum damage value.
     * {@link #getDamage()} is what this stack has already taken,
     * and {@link #getDurability()} is what is left of this number.
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
     * The stack's effective maximum damage value minus the damage it has taken.
     *
     * @return the current durability of this item.
     * @since 1.8.4
     */
    public int getDurability() {
        return base.getMaxDamage() - base.getDamageValue();
    }

    /**
     * The stack's repair-cost component, or {@code 0} if it is absent. This is not necessarily
     * the experience-level cost of a particular repair operation.
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
     * The damage value stored on this stack.
     *
     * @return the damage taken by this item.
     * @since 1.8.4
     */
    public int getDamage() {
        return base.getDamageValue();
    }

    /**
     * The maximum damage value for this stack; unlike {@link #getMaxDurability()}, this comes from
     * the stack's item.
     *
     * @return the maximum damage value for the stack's item.
     * @since 1.8.4
     */
    public int getMaxDamage() {
        return base.getMaxDamage();
    }

    /**
     * Computes this stack's main-hand attack damage using its attribute modifiers and the local
     * player's base attack-damage attribute value. Requires a local player.
     *
     * @return the computed main-hand attack damage.
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
     * Returns the item's name, regardless of any custom name set on this stack.
     *
     * @return the item's default name.
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
     * Returns the name shown for this stack, including any custom name.
     *
     * @return the stack's displayed name.
     * @since 1.2.0
     */
    public TextHelper getName() {
        return TextHelper.wrap(base.getHoverName());
    }

    /**
     * Returns the number of items in this stack. It can exceed {@link #getMaxCount()} when set
     * through {@link CreativeItemStackHelper#setCount(int)}.
     *
     * @return this stack's item count.
     */
    public int getCount() {
        return base.getCount();
    }

    /**
     * Returns the maximum stack size for this stack's item; the current count may exceed it.
     *
     * @return the item's maximum stack size.
     */
    public int getMaxCount() {
        return base.getMaxStackSize();
    }

    /**
     * Returns this stack's component changes encoded as an NBT compound, or {@code null} when the
     * component patch is empty. The returned data uses Minecraft's component names.
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
     * Returns the display names of creative tabs containing an item equal to this stack. Tabs
     * aligned to the right are excluded; an item in no included tab produces an empty list.
     *
     * @return included creative-tab display names.
     * @since 1.1.3
     */
    public List<TextHelper> getCreativeTab() {
        return CreativeModeTabs.allTabs().parallelStream().filter(group -> !group.isAlignedRight() && group.getDisplayItems().parallelStream().anyMatch(e -> ItemStack.isSameItem(e, base))).map(CreativeModeTab::getDisplayName).map(TextHelper::wrap).collect(Collectors.toList());
    }

    /**
     * Deprecated alias for {@link #getItemId()}.
     *
     * @return the item's full registry id.
     * @deprecated use {@link #getItemId()} instead, which this just calls.
     */
    @DocletReplaceReturn("ItemId")
    @DocletIgnore
    @Deprecated
    public String getItemID() {
        return getItemId();
    }

    /**
     * Returns this item's full registry id.
     *
     * @return the item's full registry id.
     * @since 1.6.4
     */
    @DocletReplaceReturn("ItemId")
    public String getItemId() {
        return BuiltInRegistries.ITEM.getKey(base.getItem()).toString();
    }

    /**
     * Returns the full registry ids of the tags containing this stack's item.
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
     * Returns whether this stack has a food component.
     *
     * @return {@code true} if this item can be eaten, {@code false} otherwise.
     * @since 1.8.2
     */
    public boolean isFood() {
        return base.get(DataComponents.FOOD) != null;
    }

    /**
     * Returns whether this stack has a tool component.
     *
     * @return {@code true} if this item is a tool, {@code false} otherwise.
     * @since 1.8.2
     */
    public boolean isTool() {
        return base.get(DataComponents.TOOL) != null;
    }

    /**
     * Returns whether this stack has an equippable component.
     *
     * @return {@code true} if this item can be worn, {@code false} otherwise.
     * @since 1.8.2
     */
    public boolean isWearable() {
        return base.getComponents().get(DataComponents.EQUIPPABLE) != null;
    }

    /**
     * Returns whether this stack has no contents. Air stacks and stacks with a non-positive count
     * are empty.
     *
     * @return {@code true} if this stack holds nothing, {@code false} otherwise.
     */
    public boolean isEmpty() {
        return base.isEmpty();
    }

    /** Returns a diagnostic string containing this stack's item id, damage value, and count.
     *
     * @return a formatted diagnostic representation of the stack
     */
    @Override
    public String toString() {
        return String.format("ItemStackHelper:{\"id\": \"%s\", \"damage\": %d, \"count\": %d}", this.getItemId(), base.getDamageValue(), base.getCount());
    }

    /**
     * Compares item and components with another helper, ignoring count.
     *
     * @param ish the other stack to compare against.
     * @return {@code true} if the two stacks have the same item and components, ignoring count
     * @since 1.2.0
     */
    public boolean equals(ItemStackHelper ish) {
        // ItemStack doesn't overwrite the equals method, so we have to do it ourselves
        return equals(ish.base);
    }

    /**
     * Compares item and components with a raw stack, ignoring count. A {@code null} raw stack
     * causes a null-pointer exception.
     *
     * @param is the raw stack to compare against.
     * @return {@code true} if the two stacks have the same item and components, ignoring count
     * @since 1.2.0
     */
    public boolean equals(ItemStack is) {
        return ItemStack.isSameItemSameComponents(base, is);
    }

    /**
     * Compares item identity and damage with another helper; other components are ignored.
     *
     * @param ish the other stack to compare against.
     * @return {@code true} if the two stacks are the same item at the same damage.
     * @since 1.2.0
     */
    public boolean isItemEqual(ItemStackHelper ish) {
        return ItemStack.isSameItem(base, ish.getRaw()) && base.getDamageValue() == ish.getRaw().getDamageValue();
    }

    /**
     * Compares item identity and damage with a raw stack; other components are ignored.
     *
     * @param is the raw stack to compare against.
     * @return {@code true} if the two stacks are the same item at the same damage.
     * @since 1.2.0
     */
    public boolean isItemEqual(ItemStack is) {
        return ItemStack.isSameItem(is, base) && base.getDamageValue() == is.getDamageValue();
    }

    /**
     * Compares item identity with another helper, ignoring damage and all components.
     *
     * @param ish the other stack to compare against.
     * @return {@code true} if the two stacks are the same item, whatever else differs.
     * @since 1.2.0
     */
    public boolean isItemEqualIgnoreDamage(ItemStackHelper ish) {
        return ItemStack.isSameItem(ish.getRaw(), base);
    }

    /**
     * Compares item identity with a raw stack, ignoring damage and all components.
     *
     * @param is the raw stack to compare against.
     * @return {@code true} if the two stacks are the same item, whatever else differs.
     * @since 1.2.0
     */
    public boolean isItemEqualIgnoreDamage(ItemStack is) {
        return ItemStack.isSameItem(is, base);
    }

    /**
     * Compares component maps with another helper; item identity and count are not checked
     * separately, and the maps include item component defaults.
     * <pre>
     * // count is ignored, but damage is part of the component state
     * const reg = Client.getRegistryManager();
     * const sword = reg.getItemStack("minecraft:diamond_sword", `[{id:"minecraft:sharpness",lvl:5}]`);
     * const sameItem = reg.getItemStack("minecraft:diamond_sword", `[{id:"minecraft:sharpness",lvl:5}]`)
     *   .getCreative().setCount(2);
     * Chat.log(`same components: ${sword.isNBTEqual(sameItem)}`);
     * const worn = sword.copy().getCreative().setDamage(1);
     * Chat.log(`damage differs: ${sword.isNBTEqual(worn)}`);
     *
     * // and against a stack with something else on it
     * const named = sword.getCreative().setName("Excalibur");
     * Chat.log(`after naming: ${sword.isNBTEqual(named)}`);
     * </pre>
     *
     * @param ish the other stack to compare against.
     * @return {@code true} if the component maps are equal.
     * @since 1.2.0
     */
    public boolean isNBTEqual(ItemStackHelper ish) {
        return Objects.equals(base.getComponents(), ish.getRaw().getComponents());
    }

    /**
     * Compares component maps with a raw stack; item identity and count are not checked
     * separately, and the maps include item component defaults.
     *
     * @param is the raw stack to compare against.
     * @return {@code true} if the component maps are equal.
     * @since 1.2.0
     */
    public boolean isNBTEqual(ItemStack is) {
        return Objects.equals(base.getComponents(), is.getComponents());
    }

    /**
     * Checks whether the local player's cooldown manager has a cooldown for this stack's cooldown group.
     *
     * @return {@code true} if the item is on cooldown, {@code false} otherwise.
     * @since 1.6.5
     */
    public boolean isOnCooldown() {
        return Minecraft.getInstance().player.getCooldowns().isOnCooldown(base);
    }

    /**
     * Returns this stack's cooldown progress: {@code 1} at the start, decreasing to {@code 0} at
     * expiry; it is {@code 0} when no cooldown exists. The result uses the current partial tick.
     * Cooldowns are keyed by group, normally item identity but possibly overridden by the stack's
     * use-cooldown component.
     *
     * @return the remaining cooldown fraction, from zero to one
     * @since 1.6.5
     */
    public float getCooldownProgress() {
        return mc.player.getCooldowns().getCooldownPercent(base, mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }

    /**
     * Checks whether this stack's tool component is correct for the block's default state.
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
     * Checks whether this stack's tool component is correct for the given block state.
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
     * Returns a mutable helper around the same stack, not a copy. Its setters mutate this stack;
     * call {@link #copy()} first to preserve the original. This is a client-side stack operation,
     * not a direct inventory or server update.
     *
     * @return a {@link CreativeItemStackHelper} instance for this item.
     * @since 1.8.4
     */
    public CreativeItemStackHelper getCreative() {
        return new CreativeItemStackHelper(base);
    }

    /**
     * Returns a helper for the item type, without this stack's count or components.
     *
     * @return the item this stack is made of.
     * @since 1.8.4
     */
    public ItemHelper getItem() {
        return new ItemHelper(base.getItem());
    }

    /**
     * Returns an independent copy of this stack, including its count and components.
     *
     * @return a copy of this stack, with everything on it.
     * @since 1.2.0
     */
    public ItemStackHelper copy() {
        return new ItemStackHelper(base.copy());
    }

    /**
     * Returns whether this stack has a can-destroy restriction component.
     *
     * @return {@code true} if the can destroy flag is set, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasDestroyRestrictions() {
        return base.get(DataComponents.CAN_BREAK) != null;
    }

    /**
     * Returns whether this stack has a can-place-on restriction component.
     *
     * @return {@code true} if the can place on flag is set, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasPlaceRestrictions() {
        return base.get(DataComponents.CAN_PLACE_ON) != null;
    }

    /**
     * Returns the block predicates in this stack's can-destroy restriction, or an empty list if
     * the component is absent. Predicates can include block, state, and block-entity NBT criteria.
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
     * Returns the block predicates in this stack's can-place-on restriction, or an empty list if
     * the component is absent. Predicates can include block, state, and block-entity NBT criteria.
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
     * Returns whether the enchantments component is marked hidden in this stack's tooltip display.
     *
     * @return {@code true} if enchantments are hidden, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean areEnchantmentsHidden() {
        return isHidden(DataComponents.ENCHANTMENTS);
    }

    /**
     * Returns whether the attribute modifiers component is marked hidden in this stack's tooltip display.
     *
     * @return {@code true} if modifiers are hidden, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean areModifiersHidden() {
        return isHidden(DataComponents.ATTRIBUTE_MODIFIERS);
    }

    /**
     * Returns whether the unbreakable component is marked hidden in this stack's tooltip display.
     *
     * @return {@code true} if the unbreakable flag is hidden, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isUnbreakableHidden() {
        return isHidden(DataComponents.UNBREAKABLE);
    }

    /**
     * Returns whether the can-destroy component is marked hidden in this stack's tooltip display,
     * independently of whether that restriction component is present.
     *
     * @return {@code true} if the can destroy flag is hidden, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCanDestroyHidden() {
        return isHidden(DataComponents.CAN_BREAK);
    }

    /**
     * Returns whether the can-place-on component is marked hidden in this stack's tooltip display.
     *
     * @return {@code true} if the component is marked hidden, or {@code false} if it is not or no tooltip display is set.
     * @since 1.8.4
     */
    public boolean isCanPlaceHidden() {
        return isHidden(DataComponents.CAN_PLACE_ON);
    }


    /**
     * Returns whether the dyed-color component is marked hidden in this stack's tooltip display.
     *
     * @return {@code true} if the component is marked hidden, or {@code false} if it is not or no tooltip display is set.
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
