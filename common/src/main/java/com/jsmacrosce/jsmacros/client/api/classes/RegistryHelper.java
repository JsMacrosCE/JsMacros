package com.jsmacrosce.jsmacros.client.api.classes;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.doclet.DocletReplaceTypeParams;
import com.jsmacrosce.jsmacros.client.api.helper.StatusEffectHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.CreativeItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.EnchantmentHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.FluidStateHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class RegistryHelper {

    private final Minecraft mc = Minecraft.getInstance();

    /**
     * Create a registry-aware NBT ops backed by the current client's registry access.
     * Use this instead of NbtOps.INSTANCE when encoding anything that may contain
     * registry-backed components (enchantments, trims, etc).
     */
    public static RegistryOps<Tag> getNbtOps() {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) {
            throw new IllegalStateException("No client connection; registry access is unavailable.");
        }
        HolderLookup.Provider provider = connection.registryAccess();
        return RegistryOps.create(NbtOps.INSTANCE, provider);
    }

    /**
     * Exposes the real HolderLookup.Provider for APIs that expect a "WrapperLookup"
     * and call createSerializationContext / getOps() on it.
     */
    public static HolderLookup.Provider getWrapperLookup() {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) {
            throw new IllegalStateException("No client connection; registry access is unavailable.");
        }
        return connection.registryAccess();
    }

    /**
     * @param id the item's id
     * @return an {@link ItemHelper} for the given item.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: CanOmitNamespace<ItemId>")
    public ItemHelper getItem(String id) {
        return new ItemHelper(BuiltInRegistries.ITEM.getValue(parseIdentifier(id)));
    }

    /**
     * @param id the item's id
     * @return an {@link ItemStackHelper} for the given item.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: CanOmitNamespace<ItemId>")
    public ItemStackHelper getItemStack(String id) {
        return new CreativeItemStackHelper(new ItemStack(BuiltInRegistries.ITEM.getValue(parseIdentifier(id))));
    }

    /**
     * @param id  the item's id
     * @param nbt the item's nbt
     * @return an {@link ItemStackHelper} for the given item and nbt data.
     * @throws CommandSyntaxException if the nbt data is invalid.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: CanOmitNamespace<ItemId>, nbt: string")
    public ItemStackHelper getItemStack(String id, String nbt) throws CommandSyntaxException {
        ItemParser reader = new ItemParser(Objects.requireNonNull(mc.getConnection()).registryAccess());
        ItemParser.ItemResult itemResult = reader.parse(new StringReader(parseNameSpace(id) + nbt));
        ItemStack stack = new ItemStack(itemResult.item());
        stack.applyComponents(itemResult.components());
        return new CreativeItemStackHelper(stack);
    }

    /**
     * @return a list of all registered item ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<ItemId>")
    public List<String> getItemIds() {
        return BuiltInRegistries.ITEM.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * @return a list of all registered items.
     * @since 1.8.4
     */
    public List<ItemHelper> getItems() {
        return BuiltInRegistries.ITEM.stream().map(ItemHelper::new).collect(Collectors.toList());
    }

    /**
     * @param id the block's id
     * @return an {@link BlockHelper} for the given block.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: CanOmitNamespace<BlockId>")
    public BlockHelper getBlock(String id) {
        return new BlockHelper(BuiltInRegistries.BLOCK.getValue(parseIdentifier(id)));
    }

    /**
     * @param id the block's id
     * @return an {@link BlockStateHelper} for the given block.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: CanOmitNamespace<BlockId>")
    public BlockStateHelper getBlockState(String id) {
        return new BlockStateHelper(BuiltInRegistries.BLOCK.getValue(parseIdentifier(id)).defaultBlockState());
    }

    /**
     * @param id the status effect's id
     * @return an {@link StatusEffectHelper} for the given status effect with 0 ticks duration.
     */
    @DocletReplaceParams("id: CanOmitNamespace<StatusEffectId>")
    public StatusEffectHelper getStatusEffect(String id) {
        return new StatusEffectHelper(BuiltInRegistries.MOB_EFFECT.getValue(parseIdentifier(id)));
    }

    /**
     * @return a list of all registered status effects as {@link StatusEffectHelper}s with 0 ticks duration.
     * @since 1.8.4
     */
    public List<StatusEffectHelper> getStatusEffects() {
        return BuiltInRegistries.MOB_EFFECT.stream().map(StatusEffectHelper::new).collect(Collectors.toList());
    }

    /**
     * @param id  the block's id
     * @param nbt the block's nbt
     * @return an {@link BlockStateHelper} for the given block with the specified nbt.
     * @throws CommandSyntaxException if the nbt data is invalid.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: CanOmitNamespace<BlockId>, nbt: string")
    public BlockStateHelper getBlockState(String id, String nbt) throws CommandSyntaxException {
        return new BlockStateHelper(BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.freeze(), parseNameSpace(id) + nbt, false).blockState());
    }

    /**
     * @return a list of all registered block ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<BlockId>")
    public List<String> getBlockIds() {
        return BuiltInRegistries.BLOCK.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * @return a list of all registered blocks.
     * @since 1.8.4
     */
    public List<BlockHelper> getBlocks() {
        return BuiltInRegistries.BLOCK.stream().map(BlockHelper::new).collect(Collectors.toList());
    }

    /**
     * @param id the enchantment's id
     * @return an {@link EnchantmentHelper} for the given enchantment.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: EnchantmentId")
    public EnchantmentHelper getEnchantment(String id) {
        return getEnchantment(id, 0);
    }

    /**
     * @param id    the enchantment's id
     * @param level the level of the enchantment
     * @return an {@link EnchantmentHelper} for the given enchantment with the specified level.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: CanOmitNamespace<EnchantmentId>, level: int")
    public EnchantmentHelper getEnchantment(String id, int level) {
        return new EnchantmentHelper(mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(parseIdentifier(id)).orElseThrow(), level);
    }

    /**
     * @return a list of all registered enchantment ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<EnchantmentId>")
    public List<String> getEnchantmentIds() {
        return mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * @return a list of all registered enchantments.
     * @since 1.8.4
     */
    public List<EnchantmentHelper> getEnchantments() {
        return mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().map(EnchantmentHelper::new).collect(Collectors.toList());
    }

    /**
     * @param type the id of the entity's type
     * @return an {@link EntityHelper} for the given entity.
     * @since 1.8.4
     */
    @DocletReplaceTypeParams("E extends CanOmitNamespace<EntityId>")
    @DocletReplaceParams("type: E")
    @DocletReplaceReturn("EntityTypeFromId<E>")
    public EntityHelper<?> getEntity(String type) {
        return EntityHelper.create(BuiltInRegistries.ENTITY_TYPE.getValue(parseIdentifier(type)).create(Minecraft.getInstance().level, EntitySpawnReason.COMMAND));
    }

    /**
     * @param type the id of the entity's type
     * @return an {@link EntityType} for the given entity.
     * @since 1.8.4
     */
    @DocletReplaceParams("type: CanOmitNamespace<EntityId>")
    public EntityType<?> getRawEntityType(String type) {
        return BuiltInRegistries.ENTITY_TYPE.getValue(parseIdentifier(type));
    }

    /**
     * @return a list of all entity type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<EntityId>")
    public List<String> getEntityTypeIds() {
        return BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * @param id the fluid's id
     * @return an {@link FluidStateHelper} for the given fluid.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: CanOmitNamespace<FluidId>")
    public FluidStateHelper getFluidState(String id) {
        return new FluidStateHelper(BuiltInRegistries.FLUID.getValue(parseIdentifier(id)).defaultFluidState());
    }

    /**
     * @return a list of all feature ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<FeatureId>")
    public List<String> getFeatureIds() {
        return BuiltInRegistries.FEATURE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * @return a list of all structure feature ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<StructureFeatureId>")
    public List<String> getStructureFeatureIds() {
        return BuiltInRegistries.STRUCTURE_PIECE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * @return a list of all painting motive ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<PaintingId>")
    public List<String> getPaintingIds() {
        return mc.getConnection().registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT).keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * @return a list of all particle type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<ParticleTypeId>")
    public List<String> getParticleTypeIds() {
        return BuiltInRegistries.PARTICLE_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * @return a list of all game event names.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<GameEventName>")
    public List<String> getGameEventNames() {
        return BuiltInRegistries.GAME_EVENT.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * @return a list of all status effect ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<StatusEffectId>")
    public List<String> getStatusEffectIds() {
        return BuiltInRegistries.MOB_EFFECT.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * @return a list of all block entity type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<BlockEntityTypeId>")
    public List<String> getBlockEntityTypeIds() {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * @return a list of all screen handler ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<ScreenHandlerId>")
    public List<String> getScreenHandlerIds() {
        return BuiltInRegistries.MENU.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * @return a list of all recipe type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<RecipeTypeId>")
    public List<String> getRecipeTypeIds() {
        return BuiltInRegistries.RECIPE_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * @return a list of all villager type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<VillagerTypeId>")
    public List<String> getVillagerTypeIds() {
        return BuiltInRegistries.VILLAGER_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * @return a list of all villager profession ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<VillagerProfession>")
    public List<String> getVillagerProfessionIds() {
        return BuiltInRegistries.VILLAGER_PROFESSION.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * @return a list of all point of interest type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<PointOfInterestTypeId>")
    public List<String> getPointOfInterestTypeIds() {
        return BuiltInRegistries.POINT_OF_INTEREST_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * @return a list of all memory module type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<MemoryModuleTypeId>")
    public List<String> getMemoryModuleTypeIds() {
        return BuiltInRegistries.MEMORY_MODULE_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * @return a list of all villager sensor type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<SensorTypeId>")
    public List<String> getSensorTypeIds() {
        return BuiltInRegistries.SENSOR_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * @return a list of all villager activity type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<ActivityTypeId>")
    public List<String> getActivityTypeIds() {
        return BuiltInRegistries.ACTIVITY.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * @return a list of all stat type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<StatTypeId>")
    public List<String> getStatTypeIds() {
        return BuiltInRegistries.STAT_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * @return a list of all entity attribute ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<EntityAttributeId>")
    public List<String> getEntityAttributeIds() {
        return BuiltInRegistries.ATTRIBUTE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * @return a list of all potion type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<PotionTypeId>")
    public List<String> getPotionTypeIds() {
        return BuiltInRegistries.POTION.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * @param identifier the String representation of the identifier, with the namespace and path
     * @return the raw minecraft Identifier.
     * @since 1.8.4
     */
    public ResourceLocation getIdentifier(String identifier) {
        return parseIdentifier(identifier);
    }

    public static ResourceLocation parseIdentifier(String id) {
        return ResourceLocation.parse(parseNameSpace(id));
    }

    public static String parseNameSpace(String id) {
        return id.indexOf(':') != -1 ? id : "minecraft:" + id;
    }

}
