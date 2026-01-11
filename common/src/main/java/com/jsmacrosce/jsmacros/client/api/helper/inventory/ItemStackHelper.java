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
 * @author Wagyourtail
 */
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
     *
     * @param damage
     * @return self
     * @since 1.2.0
     */
    @Deprecated
    public ItemStackHelper setDamage(int damage) {
        base.setDamageValue(damage);
        return this;
    }

    /**
     * @return
     * @since 1.2.0
     */
    public boolean isDamageable() {
        return base.isDamageableItem();
    }

    /**
     * @return {@code true} if this item is unbreakable, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isUnbreakable() {
        return base.get(DataComponents.UNBREAKABLE) != null;
    }

    /**
     * @return
     * @since 1.2.0
     */
    public boolean isEnchantable() {
        return base.isEnchantable();
    }

    /**
     * @return {@code true} if the item is enchanted, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isEnchanted() {
        return base.isEnchanted();
    }

    /**
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
     * @param enchantment the enchantment to check for
     * @return {@code true} if the specified enchantment can be applied to this item, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean canBeApplied(EnchantmentHelper enchantment) {
        return enchantment.canBeApplied(this);
    }

    /**
     * @param enchantment the enchantment to check for
     * @return {@code true} if the item is enchanted with the specified enchantment of the same
     * level, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasEnchantment(EnchantmentHelper enchantment) {
        return getEnchantments().stream().anyMatch(enchantment::equals);
    }

    /**
     * @param enchantment the id of the enchantment to check for
     * @return {@code true} if the item is enchanted with the specified enchantment, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: CanOmitNamespace<EnchantmentId>")
    public boolean hasEnchantment(String enchantment) {
        String toCheck = RegistryHelper.parseNameSpace(enchantment);
        return getEnchantments().stream().anyMatch(e -> e.getId().equals(toCheck));
    }

    /**
     * @return a list of all enchantments that can be applied to this item.
     * @since 1.8.4
     */
    public List<EnchantmentHelper> getPossibleEnchantments() {
        return mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
            .filter(enchantment -> enchantment.value().canEnchant(base))
            .map(EnchantmentHelper::new).toList();
    }

    /**
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
     * The returned list is a copy of the original list and can be modified without affecting the
     * original item. For editing the actual lore see
     * {@link CreativeItemStackHelper#addLore(Object...)}.
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
     * @return the maximum durability of this item.
     * @since 1.8.4
     */
    public int getMaxDurability() {
        return base.getMaxDamage();
    }

    /**
     * @return the current durability of this item.
     * @since 1.8.4
     */
    public int getDurability() {
        return base.getMaxDamage() - base.getDamageValue();
    }

    /**
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
     * @return the damage taken by this item.
     * @see #getDurability()
     */
    public int getDamage() {
        return base.getDamageValue();
    }

    /**
     * @return the maximum damage this item can take.
     * @see #getMaxDurability()
     */
    public int getMaxDamage() {
        return base.getMaxDamage();
    }

    /**
     * @return the default attack damage of this item.
     * @since 1.8.4
     */
    public double getAttackDamage() {
        assert mc.player != null;
        ItemAttributeModifiers lv = base.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        return lv.compute(mc.player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE), EquipmentSlot.MAINHAND);
    }

    /**
     * @return was string before 1.6.5
     * @since 1.2.0
     */
    public TextHelper getDefaultName() {
        return TextHelper.wrap(base.getItem().getName());
    }

    /**
     * @return was string before 1.6.5
     */
    public TextHelper getName() {
        return TextHelper.wrap(base.getHoverName());
    }

    /**
     * @return the item count this stack is holding.
     */
    public int getCount() {
        return base.getCount();
    }

    /**
     * @return the maximum amount of items this stack can hold.
     */
    public int getMaxCount() {
        return base.getMaxStackSize();
    }

    /**
     * @return
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
     * @return
     * @since 1.1.3
     */
    public List<TextHelper> getCreativeTab() {
        return CreativeModeTabs.allTabs().parallelStream().filter(group -> !group.isAlignedRight() && group.getDisplayItems().parallelStream().anyMatch(e -> ItemStack.isSameItem(e, base))).map(CreativeModeTab::getDisplayName).map(TextHelper::wrap).collect(Collectors.toList());
    }

    /**
     * @return
     */
    @DocletReplaceReturn("ItemId")
    @Deprecated
    public String getItemID() {
        return getItemId();
    }

    /**
     * @return
     * @since 1.6.4
     */
    @DocletReplaceReturn("ItemId")
    public String getItemId() {
        return BuiltInRegistries.ITEM.getKey(base.getItem()).toString();
    }

    /**
     * @return
     * @since 1.8.2
     */
    @DocletReplaceReturn("JavaList<ItemTag>")
    public List<String> getTags() {
        return base.getItemHolder().tags().map(t -> t.location().toString()).collect(Collectors.toList());
    }

    /**
     * @return
     * @since 1.8.2
     */
    public boolean isFood() {
        return base.get(DataComponents.FOOD) != null;
    }

    /**
     * @return
     * @since 1.8.2
     */
    public boolean isTool() {
        return base.get(DataComponents.TOOL) != null;
    }

    /**
     * @return
     * @since 1.8.2
     */
    public boolean isWearable() {
        return base.getComponents().get(DataComponents.EQUIPPABLE) != null;
    }

    /**
     * @return
     */
    public boolean isEmpty() {
        return base.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("ItemStackHelper:{\"id\": \"%s\", \"damage\": %d, \"count\": %d}", this.getItemId(), base.getDamageValue(), base.getCount());
    }

    /**
     * @param ish
     * @return
     * @since 1.1.3 [citation needed]
     */
    public boolean equals(ItemStackHelper ish) {
        // ItemStack doesn't overwrite the equals method, so we have to do it ourselves
        return equals(ish.base);
    }

    /**
     * @param is
     * @return
     * @since 1.1.3 [citation needed]
     */
    public boolean equals(ItemStack is) {
        return ItemStack.isSameItemSameComponents(base, is);
    }

    /**
     * @param ish
     * @return
     * @since 1.1.3 [citation needed]
     */
    public boolean isItemEqual(ItemStackHelper ish) {
        return ItemStack.isSameItem(base, ish.getRaw()) && base.getDamageValue() == ish.getRaw().getDamageValue();
    }

    /**
     * @param is
     * @return
     * @since 1.1.3 [citation needed]
     */
    public boolean isItemEqual(ItemStack is) {
        return ItemStack.isSameItem(is, base) && base.getDamageValue() == is.getDamageValue();
    }

    /**
     * @param ish
     * @return
     * @since 1.1.3 [citation needed]
     */
    public boolean isItemEqualIgnoreDamage(ItemStackHelper ish) {
        return ItemStack.isSameItem(ish.getRaw(), base);
    }

    /**
     * @param is
     * @return
     * @since 1.1.3 [citation needed]
     */
    public boolean isItemEqualIgnoreDamage(ItemStack is) {
        return ItemStack.isSameItem(is, base);
    }

    /**
     * @param ish
     * @return
     * @since 1.1.3 [citation needed]
     */
    public boolean isNBTEqual(ItemStackHelper ish) {
        return Objects.equals(base.getComponents(), ish.getRaw().getComponents());
    }

    /**
     * @param is
     * @return
     * @since 1.1.3 [citation needed]
     */
    public boolean isNBTEqual(ItemStack is) {
        return Objects.equals(base.getComponents(), is.getComponents());
    }

    /**
     * @return
     * @since 1.6.5
     */
    public boolean isOnCooldown() {
        return Minecraft.getInstance().player.getCooldowns().isOnCooldown(base);
    }

    /**
     * @return
     * @since 1.6.5
     */
    public float getCooldownProgress() {
        return mc.player.getCooldowns().getCooldownPercent(base, mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }

    /**
     * @param block the block to check
     * @return {@code true} if the given block can be mined and drops when broken with this item,
     * {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSuitableFor(BlockHelper block) {
        return base.isCorrectToolForDrops(block.getDefaultState().getRaw());
    }

    /**
     * @param block the block to check
     * @return {@code true} if the given block can be mined and drops when broken with this item,
     * {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSuitableFor(BlockStateHelper block) {
        return base.isCorrectToolForDrops(block.getRaw());
    }

    /**
     * {@link CreativeItemStackHelper} adds methods for manipulating the item's nbt data.
     *
     * @return a {@link CreativeItemStackHelper} instance for this item.
     * @since 1.8.4
     */
    public CreativeItemStackHelper getCreative() {
        return new CreativeItemStackHelper(base);
    }

    /**
     * @return the item this stack is made of.
     * @since 1.8.4
     */
    public ItemHelper getItem() {
        return new ItemHelper(base.getItem());
    }

    /**
     * @return
     * @since 1.2.0
     */
    public ItemStackHelper copy() {
        return new ItemStackHelper(base.copy());
    }

    /**
     * This flag only affects players in adventure mode and makes sure only specified blocks can be
     * destroyed by this item.
     *
     * @return {@code true} if the can destroy flag is set, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasDestroyRestrictions() {
        return base.get(DataComponents.CAN_BREAK) != null;
    }

    /**
     * This flag only affects players in adventure mode and makes sure this item can only be placed
     * on specified blocks.
     *
     * @return {@code true} if the can place on flag is set, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasPlaceRestrictions() {
        return base.get(DataComponents.CAN_PLACE_ON) != null;
    }

    /**
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
     * @return {@code true} if enchantments are hidden, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean areEnchantmentsHidden() {
        return isHidden(DataComponents.TOOLTIP_DISPLAY);
    }

    /**
     * @return {@code true} if modifiers are hidden, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean areModifiersHidden() {
        return isHidden(DataComponents.TOOLTIP_DISPLAY);
    }

    /**
     * @return {@code true} if the unbreakable flag is hidden, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isUnbreakableHidden() {
        return isHidden(DataComponents.TOOLTIP_DISPLAY);
    }

    /**
     * @return {@code true} if the can destroy flag is hidden, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCanDestroyHidden() {
        return isHidden(DataComponents.CAN_BREAK);
    }

    /**
     * @return {@code true} if the can place flag is hidden, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCanPlaceHidden() {
        return isHidden(DataComponents.CAN_PLACE_ON);
    }


    /**
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
