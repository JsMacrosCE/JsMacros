package com.jsmacrosce.jsmacros.client.api.classes;

import com.jsmacrosce.doclet.DocletCategory;
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

//? if >=26.1 {
/*import net.minecraft.commands.arguments.item.ItemInput;
*///? }

/**
 * Provides registry lookups and listings as JsMacros helpers. Identifier arguments may omit the
 * {@code minecraft:} namespace. Unknown item and block ids resolve to their registry defaults
 * (air); unknown status effect and enchantment ids fail rather than falling back.
 * Obtain an instance with {@code Client.getRegistryManager()}.
 * <pre>
 * // a block by name, the block state it defaults to, and the three
 * // ways of listing what is there
 * const reg = Client.getRegistryManager();
 * const block = reg.getBlock("stone");
 * Chat.log(`${block.getId()} is ${block.getName()}`);
 * const state = reg.getBlockState("oak_stairs", "[facing=north]");
 * Chat.log(`a stair facing north is solid: ${state.isSolid()}`);
 * Chat.log(`${reg.getBlockIds().size()} blocks and ${reg.getItemIds().size()} items`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class RegistryHelper {

    private final Minecraft mc = Minecraft.getInstance();

    /**
     * Creates registry-aware NBT operations from the current connection's registry access. Use
     * these instead of {@code NbtOps.INSTANCE} to encode registry-backed data. Each call creates
     * new operations; without a connection this throws {@link IllegalStateException}.
     *
     * @throws IllegalStateException if there is no connection, which is the case before a world
     *         has been joined
     * @return new registry-aware NBT operations for the current connection
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
     * Returns the current connection's registry lookup provider, the provider used to create
     * {@link #getNbtOps()}. Without a connection this throws {@link IllegalStateException}.
     *
     * @throws IllegalStateException if there is no connection, which is the case before a world
     *         has been joined
     * @return the current connection's registry lookup provider
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
     * Looks up an item by id, which may omit the {@code minecraft:} namespace. Unknown ids
     * resolve to the item registry's default, air.
     *
     * @param id the item's id
     * @return an {@link ItemHelper} for the given item.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: CanOmitNamespace<ItemId>")
    public ItemHelper getItem(String id) {
        return new ItemHelper(BuiltInRegistries.ITEM.getValue(parseIdentifier(id)));
    }

    /**
     * Creates a one-item stack without components. Use {@link #getItemStack(String, String)} to
     * parse components. Unknown ids resolve to the item registry's default, air.
     *
     * @param id the item's id
     * @return an {@link ItemStackHelper} for the given item.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: CanOmitNamespace<ItemId>")
    public ItemStackHelper getItemStack(String id) {
        return new CreativeItemStackHelper(new ItemStack(BuiltInRegistries.ITEM.getValue(parseIdentifier(id))));
    }

    /**
     * Parses item components using command item-argument syntax appended directly to the id
     * (for example, {@code [damage=12]}). The returned stack has count one. A connection is
     * required; invalid syntax throws {@link CommandSyntaxException}.
     *
     * @param id  the item's id
     * @param nbt component data in the game's item-argument syntax, including square brackets
     * @return an {@link ItemStackHelper} for the item with the parsed components.
     * @throws CommandSyntaxException if the component syntax is invalid.
     * @since 1.8.4
     * @throws NullPointerException if there is no connection, since the components are read
     *         through the connected server's registry
     */
    @DocletReplaceParams("id: CanOmitNamespace<ItemId>, nbt: string")
    public ItemStackHelper getItemStack(String id, String nbt) throws CommandSyntaxException {
        ItemParser reader = new ItemParser(Objects.requireNonNull(mc.getConnection()).registryAccess());
        //? if >=26.1 {
        /*ItemInput itemInput = reader.parse(new StringReader(parseNameSpace(id) + nbt));
        ItemStack stack = itemInput.createItemStack(1);
        *///? } else {
        ItemParser.ItemResult itemResult = reader.parse(new StringReader(parseNameSpace(id) + nbt));
        ItemStack stack = new ItemStack(itemResult.item(), 1, itemResult.components());
        //? }

        return new CreativeItemStackHelper(stack);
    }

    /**
     * Returns all item ids in namespaced form, suitable for {@link #getItem(String)}.
     *
     * @return a list of all registered item ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<ItemId>")
    public List<String> getItemIds() {
        return BuiltInRegistries.ITEM.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * Returns a helper for each registered item.
     *
     * @return a list of all registered items.
     * @since 1.8.4
     */
    public List<ItemHelper> getItems() {
        return BuiltInRegistries.ITEM.stream().map(ItemHelper::new).collect(Collectors.toList());
    }

    /**
     * Looks up a block by id, with or without its namespace. An unknown id resolves to the block
     * registry's default, air, rather than reporting an error.
     * <pre>
     * // Unknown ids resolve to air, so read back the id to verify the lookup.
     * const reg = Client.getRegistryManager();
     * const block = reg.getBlock("stone");
     * Chat.log(`${block.getId()} is ${block.getName()}`);
     * </pre>
     *
     * @param id the block's id
     * @return an {@link BlockHelper} for the given block.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: CanOmitNamespace<BlockId>")
    public BlockHelper getBlock(String id) {
        return new BlockHelper(BuiltInRegistries.BLOCK.getValue(parseIdentifier(id)));
    }

    /**
     * Returns the block's default state; use {@link #getBlockState(String, String)} to select
     * properties. An unknown id resolves to the default block, air.
     * <pre>
     * // Get the default state; supply properties to select a variant.
     * const reg = Client.getRegistryManager();
     * const state = reg.getBlockState("oak_stairs");
     * Chat.log(`${state.getId()} facing ${state.getPistonBehaviour()} is solid: ${state.isSolid()}`);
     * </pre>
     *
     * @param id the block's id
     * @return an {@link BlockStateHelper} for the given block.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: CanOmitNamespace<BlockId>")
    public BlockStateHelper getBlockState(String id) {
        return new BlockStateHelper(BuiltInRegistries.BLOCK.getValue(parseIdentifier(id)).defaultBlockState());
    }

    /**
     * Looks up a status effect by id, with or without its namespace. An unknown id does not produce
     * a usable helper: constructing it with the missing effect fails.
     *
     * @param id the status effect's id
     * @return a helper for the effect with 0 ticks duration
     */
    @DocletReplaceParams("id: CanOmitNamespace<StatusEffectId>")
    public StatusEffectHelper getStatusEffect(String id) {
        return new StatusEffectHelper(BuiltInRegistries.MOB_EFFECT.getValue(parseIdentifier(id)));
    }

    /**
     * Returns each registered status effect as a helper without a duration.
     * <pre>
     * // List all registered status effects.
     * const reg = Client.getRegistryManager();
     * const effects = reg.getStatusEffects();
     * Chat.log(`${effects.size()} status effects`);
     * const first = effects.get(0);
     * Chat.log(`the first is ${first.getId()}`);
     * </pre>
     *
     * @return a list of all registered status effects as {@link StatusEffectHelper}s with 0 ticks duration.
     * @since 1.8.4
     */
    public List<StatusEffectHelper> getStatusEffects() {
        return BuiltInRegistries.MOB_EFFECT.stream().map(StatusEffectHelper::new).collect(Collectors.toList());
    }

    /**
     * Parses block properties in the game's command syntax, appended directly after the block id
     * (for example, {@code [facing=north]}). Invalid syntax, block ids, properties, or values throw
     * {@link CommandSyntaxException}.
     * <pre>
     * // Select a particular state using the game's block-property syntax.
     * const reg = Client.getRegistryManager();
     * const stairs = reg.getBlockState("oak_stairs", "[facing=north,half=top]");
     * Chat.log(`${stairs.getId()} solid: ${stairs.isSolid()}`);
     * </pre>
     *
     * @param id  the block's id
     * @param nbt the block-state properties in command syntax, such as {@code [facing=north]}
     * @return the block state with the specified properties
     * @throws CommandSyntaxException if the property syntax, block, property, or value is invalid
     * @since 1.8.4
     */
    @DocletReplaceParams("id: CanOmitNamespace<BlockId>, nbt: string")
    public BlockStateHelper getBlockState(String id, String nbt) throws CommandSyntaxException {
        return new BlockStateHelper(BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.freeze(), parseNameSpace(id) + nbt, false).blockState());
    }

    /**
     * Returns all registered block ids in namespaced form, suitable for
     * {@link #getBlock(String)}.
     * <pre>
     * // Filter the namespaced ids as needed.
     * const reg = Client.getRegistryManager();
     * const ids = reg.getBlockIds();
     * Chat.log(`${ids.size()} blocks registered`);
     * let ores = 0;
     * for (const id of ids) {
     *   if (id.endsWith("_ore")) {
     *     ores = ores + 1;
     *   }
     * }
     * Chat.log(`${ores} of them are ores`);
     * </pre>
     *
     * @return a list of all registered block ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<BlockId>")
    public List<String> getBlockIds() {
        return BuiltInRegistries.BLOCK.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * Returns every registered block as a helper.
     * <pre>
     * // Access each registered block through its helper.
     * const reg = Client.getRegistryManager();
     * const blocks = reg.getBlocks();
     * const first = blocks.get(0);
     * Chat.log(`${blocks.size()} blocks, the first is ${first.getId()}`);
     * </pre>
     *
     * @return a list of all registered blocks.
     * @since 1.8.4
     */
    public List<BlockHelper> getBlocks() {
        return BuiltInRegistries.BLOCK.stream().map(BlockHelper::new).collect(Collectors.toList());
    }

    /**
     * Looks up an enchantment in the connected server's registry. The returned helper has level
     * zero; use {@link #getEnchantment(String, int)} to supply a level. Unknown ids fail rather
     * than falling back, and a server connection is required.
     * <pre>
     * // Look up the enchantment without assigning it an item level.
     * const reg = Client.getRegistryManager();
     * const ench = reg.getEnchantment("minecraft:sharpness");
     * Chat.log(`${ench.getId()} level ${ench.getLevel()} of ${ench.getMaxLevel()}`);
     * </pre>
     *
     * @param id the enchantment's id
     * @return an {@link EnchantmentHelper} for the given enchantment.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: EnchantmentId")
    public EnchantmentHelper getEnchantment(String id) {
        return getEnchantment(id, 0);
    }

    /**
     * Looks up an enchantment in the connected server's registry and associates the supplied level
     * with its helper. The level is not clamped to the enchantment's maximum. Unknown ids fail
     * rather than falling back, and a server connection is required.
     * <pre>
     * // Associate a level with an enchantment from the server registry.
     * const reg = Client.getRegistryManager();
     * const ench = reg.getEnchantment("sharpness", 5);
     * Chat.log(`${ench.getId()} ${ench.getLevel()} of ${ench.getMaxLevel()}`);
     * </pre>
     *
     * @param id    the enchantment's id
     * @param level the level of the enchantment
     * @return an {@link EnchantmentHelper} for the given enchantment with the specified level.
     * @since 1.8.4
     * @throws java.util.NoSuchElementException if the id is not in the connected server's
     *         enchantment registry, since that registry has no default entry to fall back to
     * @throws NullPointerException if there is no connection to read the registry from
     */
    @DocletReplaceParams("id: CanOmitNamespace<EnchantmentId>, level: int")
    public EnchantmentHelper getEnchantment(String id, int level) {
        return new EnchantmentHelper(mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(parseIdentifier(id)).orElseThrow(), level);
    }

    /**
     * Returns the ids in the connected server's enchantment registry. A connection is required;
     * without one, dereferencing it fails rather than returning an empty list.
     *
     * @return a list of all registered enchantment ids.
     * @since 1.8.4
     * @throws NullPointerException if there is no connection to read the registry from
     */
    @DocletReplaceReturn("JavaList<EnchantmentId>")
    public List<String> getEnchantmentIds() {
        return mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * Returns helpers for the enchantments in the connected server's registry, each with its
     * default helper level. A connection is required; without one, dereferencing it fails.
     *
     * @return a list of all registered enchantments.
     * @since 1.8.4
     * @throws NullPointerException if there is no connection to read the registry from
     */
    public List<EnchantmentHelper> getEnchantments() {
        return mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().map(EnchantmentHelper::new).collect(Collectors.toList());
    }

    /**
     * Creates an entity of the registry type named by {@code type}, using command spawn reason;
     * the entity is not added to the world. A world must be loaded. Unknown ids resolve to the
     * built-in registry default (the pig). Types that cannot be created in the current world
     * produce no entity, which fails when wrapping the result.
     *
     * @param type the id of the entity's type
     * @return an {@link EntityHelper} for the given entity.
     * @since 1.8.4
     * @throws NullPointerException if there is no world to make the entity in, or if the type
     *         named is one the world has switched off or a placeholder for other types, either
     *         of which leaves nothing to wrap
     */
    @DocletReplaceTypeParams("E extends CanOmitNamespace<EntityId>")
    @DocletReplaceParams("type: E")
    @DocletReplaceReturn("EntityTypeFromId<E>")
    public EntityHelper<?> getEntity(String type) {
        return EntityHelper.create(BuiltInRegistries.ENTITY_TYPE.getValue(parseIdentifier(type)).create(Minecraft.getInstance().level, EntitySpawnReason.COMMAND));
    }

    /**
     * Returns the built-in entity type for {@code type}; this does not create an entity and needs
     * no world. Unknown ids resolve to the registry default (the pig).
     *
     * @param type the id of the entity's type
     * @return an {@link EntityType} for the given entity.
     * @since 1.8.4
     */
    @DocletReplaceParams("type: CanOmitNamespace<EntityId>")
    public EntityType<?> getRawEntityType(String type) {
        return BuiltInRegistries.ENTITY_TYPE.getValue(parseIdentifier(type));
    }

    /**
     * Returns ids from the built-in entity type registry, including types that are placeholders
     * and cannot be instantiated.
     *
     * @return a list of all entity type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<EntityId>")
    public List<String> getEntityTypeIds() {
        return BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * Returns the default state of the built-in fluid named by {@code id}, not a state sampled
     * from a world. Unknown ids resolve to the registry default (the empty fluid).
     *
     * @param id the fluid's id
     * @return an {@link FluidStateHelper} for the given fluid.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: CanOmitNamespace<FluidId>")
    public FluidStateHelper getFluidState(String id) {
        return new FluidStateHelper(BuiltInRegistries.FLUID.getValue(parseIdentifier(id)).defaultFluidState());
    }

    /**
     * Returns ids from the built-in world-generation feature registry, independent of the
     * connected server's data packs.
     *
     * @return a list of all feature ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<FeatureId>")
    public List<String> getFeatureIds() {
        return BuiltInRegistries.FEATURE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * Returns ids from the built-in structure-piece registry, not ids for complete structures;
     * connected-server data packs are not reflected.
     *
     * @return a list of all structure feature ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<StructureFeatureId>")
    public List<String> getStructureFeatureIds() {
        return BuiltInRegistries.STRUCTURE_PIECE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * Returns ids from the connected server's painting variant registry. A connection is required;
     * without one, dereferencing it fails rather than returning an empty list.
     *
     * @return a list of all painting motive ids.
     * @since 1.8.4
     * @throws NullPointerException if there is no connection to read the registry from
     */
    @DocletReplaceReturn("JavaList<PaintingId>")
    public List<String> getPaintingIds() {
        return mc.getConnection().registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT).keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * Returns identifiers for particle types in the local built-in registry, not individual
     * particle instances or their parameters.
     *
     * @return a list of all particle type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<ParticleTypeId>")
    public List<String> getParticleTypeIds() {
        return BuiltInRegistries.PARTICLE_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * Returns identifiers for game events in the local built-in registry.
     *
     * @return a list of all game event names.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<GameEventName>")
    public List<String> getGameEventNames() {
        return BuiltInRegistries.GAME_EVENT.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * Returns identifiers for status effects in the local built-in registry.
     *
     * @return a list of all status effect ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<StatusEffectId>")
    public List<String> getStatusEffectIds() {
        return BuiltInRegistries.MOB_EFFECT.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * Returns identifiers for block entity types in the local built-in registry.
     *
     * @return a list of all block entity type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<BlockEntityTypeId>")
    public List<String> getBlockEntityTypeIds() {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * Returns identifiers for screen handlers in the local built-in registry.
     *
     * @return a list of all screen handler ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<ScreenHandlerId>")
    public List<String> getScreenHandlerIds() {
        return BuiltInRegistries.MENU.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * Returns identifiers for recipe types in the local built-in registry, not individual recipes.
     *
     * @return a list of all recipe type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<RecipeTypeId>")
    public List<String> getRecipeTypeIds() {
        return BuiltInRegistries.RECIPE_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * Returns identifiers for villager types in the local built-in registry.
     *
     * @return a list of all villager type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<VillagerTypeId>")
    public List<String> getVillagerTypeIds() {
        return BuiltInRegistries.VILLAGER_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * Returns identifiers for villager professions in the local built-in registry.
     *
     * @return a list of all villager profession ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<VillagerProfession>")
    public List<String> getVillagerProfessionIds() {
        return BuiltInRegistries.VILLAGER_PROFESSION.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * Returns identifiers for point of interest types in the local built-in registry.
     *
     * @return a list of all point of interest type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<PointOfInterestTypeId>")
    public List<String> getPointOfInterestTypeIds() {
        return BuiltInRegistries.POINT_OF_INTEREST_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * Returns identifiers for memory module types in the local built-in registry.
     *
     * @return a list of all memory module type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<MemoryModuleTypeId>")
    public List<String> getMemoryModuleTypeIds() {
        return BuiltInRegistries.MEMORY_MODULE_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * Returns identifiers for the built-in sensor types; these represent senses a mob's brain uses
     * to notice things, not the things sensed. The list comes from the local built-in registry,
     * independent of a server connection and without server data-pack additions.
     *
     * @return the built-in sensor type identifiers.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<SensorTypeId>")
    public List<String> getSensorTypeIds() {
        return BuiltInRegistries.SENSOR_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * Returns identifiers for built-in activities, which represent activities a villager can
     * perform. The list comes from the local built-in registry, independent of a server connection
     * and without server data-pack additions.
     *
     * @return the built-in activity identifiers.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<ActivityTypeId>")
    public List<String> getActivityTypeIds() {
        return BuiltInRegistries.ACTIVITY.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * Returns identifiers for built-in statistic types, not advancements. The list comes from the
     * local built-in registry, independent of a server connection and without server data-pack additions.
     *
     * @return the built-in statistic type identifiers.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<StatTypeId>")
    public List<String> getStatTypeIds() {
        return BuiltInRegistries.STAT_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * Returns identifiers for built-in entity attributes, such as health and movement speed. The
     * list comes from the local built-in registry, independent of a server connection and without
     * server data-pack additions.
     *
     * @return the built-in entity attribute identifiers.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<EntityAttributeId>")
    public List<String> getEntityAttributeIds() {
        return BuiltInRegistries.ATTRIBUTE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * Returns identifiers for built-in potion types (brews), not the effects they carry. The list
     * comes from the local built-in registry, independent of a server connection and without server
     * data-pack additions.
     *
     * @return the built-in potion type identifiers.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<PotionTypeId>")
    public List<String> getPotionTypeIds() {
        return BuiltInRegistries.POTION.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * Parses an identifier string without looking it up in a registry. A missing namespace is
     * supplied as {@code minecraft}.
     *
     * @param identifier an identifier string, with or without a namespace
     * @return the parsed identifier
     * @since 1.8.4
     */
    public ResourceLocation getIdentifier(String identifier) {
        return parseIdentifier(identifier);
    }

    /**
     * Parses an identifier string without checking whether it is registered. A missing namespace
     * is supplied as {@code minecraft}; invalid identifier syntax is rejected by Minecraft.
     *
     * @param id an identifier string, with or without a namespace
     * @return the parsed identifier
     * @since 1.8.4
     */
    public static ResourceLocation parseIdentifier(String id) {
        return ResourceLocation.parse(parseNameSpace(id));
    }

    /**
     * Prepends {@code minecraft:} when the input contains no colon; otherwise returns it unchanged.
     *
     * @param id the string to prefix when it contains no colon
     * @return the input prefixed with {@code minecraft:} if it contains no colon, or unchanged otherwise
     * @since 1.8.4
     */
    public static String parseNameSpace(String id) {
        return id.indexOf(':') != -1 ? id : "minecraft:" + id;
    }

}
