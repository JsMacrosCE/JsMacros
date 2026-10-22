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
 * A way into the game's registries, and from there into the helpers the rest of the api works with.
 * <p>
 * Two kinds of call live here. The id taking ones turn a name into a helper for a thing that is
 * registered with the game, and the listing ones give every id in one registry so a script can find
 * the one it wants. A name may be written with or without its namespace, so {@code stone} and
 * {@code minecraft:stone} are the same thing throughout.
 * <p>
 * There is one thing worth knowing before relying on an id taking call. Several of the registries
 * have a default entry, and asking for an id that is not in one of those gives that entry rather
 * than failing, so a mistyped item id reads as the air item and a mistyped block id as the air
 * block. The registries without a default behave the other way round, so {@code getStatusEffect}
 * and {@code getEnchantment} fail on a bad id instead. Reading the id back off the helper is how a
 * script tells which of the two it got.
 * <p>
 * A script gets one through {@code Client.getRegistryManager()}, which makes a fresh handle each
 * time and keeps nothing alive.
 * example:
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
     * Create a registry-aware NBT ops backed by the current client's registry access.
     * Use this instead of NbtOps.INSTANCE when encoding anything that may contain
     * registry-backed components (enchantments, trims, etc).
     * This is the pair of them a script wants when it is building a string and has to write the
     * components into it, and it is the one to use instead of the plain nbt operations because
     * those know nothing about the registry and so cannot name an enchantment or a trim.
     * <p>
     * What comes back is derived from the connection rather than cached, so it is a fresh pair
     * each call and reflects the registry as it is at the time. With no connection there is no
     * registry access to build it from, which is a failure rather than an empty result.
     * example:
     * <pre>
     * // registry aware nbt operations, which are what anything holding a
     * // registry backed component has to encode with
     * const reg = Client.getRegistryManager();
     * const RegistryHelper = Java.type("com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper");
     * const ops = RegistryHelper.getNbtOps();
     * const item = reg.getItemStack("diamond_sword");
     * Chat.log(`nbt ops ready: ${ops !== null}`);
     * </pre>
     *
     * @throws IllegalStateException if there is no connection, which is the case before a world
     *         has been joined
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
     * This is the same registry access that {@link #getNbtOps()} builds on, handed over on its own
     * for the calls that want the provider rather than something made from it. It is derived
     * from the connection rather than cached, so it reflects the registry as it is at the time,
     * and with no connection there is no registry access to hand over.
     * example:
     * <pre>
     * // the raw registry access, for the calls that want the provider
     * // rather than something built from it
     * const reg = Client.getRegistryManager();
     * const RegistryHelper = Java.type("com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper");
     * const lookup = RegistryHelper.getWrapperLookup();
     * Chat.log(`lookup ready: ${lookup !== null}`);
     * </pre>
     *
     * @throws IllegalStateException if there is no connection, which is the case before a world
     *         has been joined
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
     * The id may be written with or without its namespace, so {@code diamond} and
     * {@code minecraft:diamond} are the same item. An id that is not in the registry is not an
     * error: the item registry has a default entry, and that is what comes back instead, which
     * for items is the air item. So a mistyped id gives a helper that answers as air rather than
     * something the script can notice, and checking the id back is how a mistake is caught.
     * example:
     * <pre>
     * // an item by id, with or without the namespace. A mistyped id comes
     * // back as air rather than failing, so the id is read back to check
     * const reg = Client.getRegistryManager();
     * const item = reg.getItem("diamond");
     * Chat.log(`${item.getId()} is called ${item.getName()}`);
     * </pre>
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
     * This is a stack of exactly one of the item, with no components on it at all, which is what a
     * creative inventory entry is. To build a stack with enchantments, a custom name or damage
     * on it, use {@link #getItemStack(String, String)} instead. As with {@link #getItem(String)}
     * an id that is not in the registry gives the registry's default, which is the air item.
     * example:
     * <pre>
     * // a stack of one, with nothing on it. Components go on through the
     * // two argument form rather than here
     * const reg = Client.getRegistryManager();
     * const stack = reg.getItemStack("diamond");
     * Chat.log(`${stack.getCount()} of ${stack.getName()}`);
     * </pre>
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
     * The nbt is appended straight onto the id and read by the same parser a command uses, so the
     * syntax is the game's: the property list goes in square brackets after the id, with no
     * space between. The count is one whatever the components say, since this builds the item
     * and not a stack of it. Reading the components needs the registry, so this needs a
     * connection and is a failure without one.
     * <p>
     * A malformed property list is a {@link CommandSyntaxException} rather than a wrong stack, so
     * the difference between a typo in the nbt and a wrong answer is an exception rather than
     * something to check afterwards.
     * example:
     * <pre>
     * // a stack with components on it, written in the game's own syntax
     * // and read by the same parser a command uses
     * const reg = Client.getRegistryManager();
     * const named = reg.getItemStack("diamond_sword", '{CustomName:"\"excalibur\""}');
     * Chat.log(`${named.getCount()} of ${named.getName()}`);
     * </pre>
     *
     * @param id  the item's id
     * @param nbt the item's nbt
     * @return an {@link ItemStackHelper} for the given item and nbt data.
     * @throws CommandSyntaxException if the nbt data is invalid.
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
     * This is every id in the item registry, which on a normal game is well over a thousand, so the
     * list is large enough to be worth filtering rather than reading. An id from here is the
     * full form with its namespace, so it can be handed straight back to
     * {@link #getItem(String)}.
     * example:
     * <pre>
     * // every item id, which is over a thousand on a normal game, so it
     * // is worth filtering rather than reading through
     * const reg = Client.getRegistryManager();
     * const ids = reg.getItemIds();
     * Chat.log(`${ids.size()} items registered`);
     * let ores = 0;
     * for (const id of ids) {
     *   if (id.endsWith("_ore")) {
     *     ores = ores + 1;
     *   }
     * }
     * Chat.log(`${ores} of them are ores`);
     * </pre>
     *
     * @return a list of all registered item ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<ItemId>")
    public List<String> getItemIds() {
        return BuiltInRegistries.ITEM.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * This is every item rather than every id, so building the list is a good deal more work than
     * {@link #getItemIds()} is and there is one helper per item. It is the form to reach for when
     * something is wanted from each of them rather than just their names.
     * example:
     * <pre>
     * // every item as a helper, which is more work than the id list and
     * // is the form to use when something is wanted from each of them
     * const reg = Client.getRegistryManager();
     * const items = reg.getItems();
     * const first = items.get(0);
     * Chat.log(`${items.size()} items, the first is ${first.getName()}`);
     * </pre>
     *
     * @return a list of all registered items.
     * @since 1.8.4
     */
    public List<ItemHelper> getItems() {
        return BuiltInRegistries.ITEM.stream().map(ItemHelper::new).collect(Collectors.toList());
    }

    /**
     * The id may be written with or without its namespace, so {@code stone} and
     * {@code minecraft:stone} are the same block. An id that is not in the registry is not an
     * error: the block registry has a default entry, and that is what comes back instead, which
     * for blocks is the air block. So a mistyped id gives a helper that answers as air rather
     * than something the script can notice, and reading the id back is how a mistake is caught.
     * example:
     * <pre>
     * // a block by id, with or without the namespace. A mistyped id comes
     * // back as air rather than failing, so the id is read back to check
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
     * This is the block's default state, so a block whose variants differ gives the one the game
     * treats as its plain form rather than any particular variant. To pick a variant, use
     * {@link #getBlockState(String, String)} instead. As with {@link #getBlock(String)} an id that
     * is not in the registry gives the registry's default, which is the air block.
     * example:
     * <pre>
     * // the default state of a block, which for a block with variants is
     * // the one the game treats as its plain form
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
     * The id may be written with or without its namespace. This differs from the item and block
     * lookups in one important way: the status effect registry has no default entry, so an id
     * that is not in it produces a helper wrapping nothing rather than some sensible fallback,
     * and the first thing asked of that helper fails. Reading the id back is how a mistake is
     * caught. The helper carries no duration, so it is an effect rather than an effect on
     * something.
     * example:
     * <pre>
     * // a status effect by id. Unlike items and blocks this registry has
     * // no default, so a bad id gives a helper wrapping nothing
     * const reg = Client.getRegistryManager();
     * const effect = reg.getStatusEffect("minecraft:speed");
     * Chat.log(`${effect.getId()} is the effect asked for`);
     * </pre>
     *
     * @param id the status effect's id
     * @return an {@link StatusEffectHelper} for the given status effect with 0 ticks duration.
     */
    @DocletReplaceParams("id: CanOmitNamespace<StatusEffectId>")
    public StatusEffectHelper getStatusEffect(String id) {
        return new StatusEffectHelper(BuiltInRegistries.MOB_EFFECT.getValue(parseIdentifier(id)));
    }

    /**
     * This is every status effect in the registry as a helper, each of them without any duration.
     * There are about forty of them, so unlike the item and block lists this is small enough to
     * read through.
     * example:
     * <pre>
     * // every status effect, which is about forty and so is small
     * // enough to read through
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
     * The property list is appended straight onto the id and read by the same parser a command uses,
     * so the syntax is the game's: the variants go in square brackets after the id, written as
     * name and value pairs separated by commas, with no space after the bracket. This is the way
     * to name a particular variant rather than the block's default one.
     * <p>
     * A malformed property list, an unknown property or a value that property does not allow is
     * a {@link CommandSyntaxException} rather than a state that quietly comes out different, so
     * the failure is an exception to catch rather than a result to check.
     * example:
     * <pre>
     * // a particular variant rather than the default one, written in the
     * // game's own syntax and read by the same parser a command uses
     * const reg = Client.getRegistryManager();
     * const stairs = reg.getBlockState("oak_stairs", "[facing=north,half=top]");
     * Chat.log(`${stairs.getId()} solid: ${stairs.isSolid()}`);
     * </pre>
     *
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
     * This is every id in the block registry, which on a normal game is well over a thousand, so the
     * list is large enough to be worth filtering rather than reading. An id from here is the
     * full form with its namespace, so it can be handed straight back to
     * {@link #getBlock(String)}.
     * example:
     * <pre>
     * // every block id, which is over a thousand on a normal game, so it
     * // is worth filtering rather than reading through
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
     * This is every block rather than every id, so building the list is a good deal more work than
     * {@link #getBlockIds()} is and there is one helper per block. It is the form to reach for
     * when something is wanted from each of them rather than just their names.
     * example:
     * <pre>
     * // every block as a helper, which is more work than the id list and
     * // is the form to use when something is wanted from each of them
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
     * This is the enchantment with no level on it, and a level of zero is not a level any enchanted
     * item has, so what comes back is the enchantment itself rather than an enchantment as it
     * would appear on something. To ask for a level, use
     * {@link #getEnchantment(String, int)} instead.
     * <p>
     * This is one of the lookups with no default entry, so an id that is not in the registry is
     * a failure rather than a fallback, and it needs a connection to read the registry at all.
     * example:
     * <pre>
     * // an enchantment with no level on it, which is a level no enchanted
     * // item actually has
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
     * This is the enchantment as it would appear on an item at that level, and the level is taken as
     * given rather than being clamped, so a level above the maximum is a number the helper
     * reports but one no item can carry. The registry is the connected server's rather than the
     * game's own, which is what makes a data pack's own enchantments reachable and what means a
     * connection is needed.
     * <p>
     * An id that is not in that registry is a failure rather than a fallback, since there is no
     * default to fall back to.
     * example:
     * <pre>
     * // an enchantment at a level, taken from the server's own registry
     * // rather than the game's built in one
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
     * This is every id in the connected server's enchantment registry, so it reflects whatever the
     * server and its data packs have rather than only what the game ships with. That needs a
     * connection, and there is no handling for there not being one, so before a world is joined
     * this is a failure rather than an empty list.
     * example:
     * <pre>
     * // every enchantment the server has, which is its own registry
     * // rather than only what the game ships with
     * const reg = Client.getRegistryManager();
     * const ids = reg.getEnchantmentIds();
     * Chat.log(`${ids.size()} enchantments on this server`);
     * </pre>
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
     * This is every enchantment the connected server has, as a helper with no level on any of them,
     * so it is the same set as {@link #getEnchantmentIds()} and costs more to build. That needs a
     * connection, and there is no handling for there not being one, so before a world is joined
     * this is a failure rather than an empty list.
     * example:
     * <pre>
     * // every enchantment the server has, as a helper with no level on
     * // any of them
     * const reg = Client.getRegistryManager();
     * const enchs = reg.getEnchantments();
     * Chat.log(`${enchs.size()} enchantments on this server`);
     * const first = enchs.get(0);
     * Chat.log(`one of them is ${first.getId()}`);
     * </pre>
     *
     * @return a list of all registered enchantments.
     * @since 1.8.4
     * @throws NullPointerException if there is no connection to read the registry from
     */
    public List<EnchantmentHelper> getEnchantments() {
        return mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().map(EnchantmentHelper::new).collect(Collectors.toList());
    }

    /**
     * This builds an actual entity rather than a description of one, which is what an entity selector
     * in a command does and why the entity exists before it is asked to do anything. It is not
     * placed in the world, so it has no position and is not ticked until it is.
     * <p>
     * The id may be written with or without its namespace, and an id that is not in the registry
     * gives the registry's default rather than failing, which for entities is the pig. Making the
     * entity needs a world, so with none loaded this fails rather than giving an entity with
     * nowhere to be. A type the world has switched off, and a type that is only a placeholder for
     * others, give nothing to wrap and fail here too.
     * example:
     * <pre>
     * // a real entity built from a type, not placed in the world. A bad
     * // id gives the registry default, which for entities is the pig
     * const reg = Client.getRegistryManager();
     * const pig = reg.getEntity("minecraft:pig");
     * Chat.log(`made a ${pig.getName()}`);
     * </pre>
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
     * This is the description rather than an entity made from it, so nothing is built and no world is
     * needed. It is the type's own registry entry, which carries what the type is rather than an
     * instance of it, and it is not one of the script's helper classes.
     * <p>
     * The id may be written with or without its namespace, and an id that is not in the registry
     * gives the registry's default rather than failing, which for entities is the pig.
     * example:
     * <pre>
     * // the type rather than an entity made from it, so nothing is built
     * // and no world is needed
     * const reg = Client.getRegistryManager();
     * const type = reg.getRawEntityType("minecraft:villager");
     * Chat.log(`the type is ${type}`);
     * </pre>
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
     * This is every id in the entity type registry. It is a list of kinds rather than of things in the
     * world, so most of what is in it is not anywhere near the player, and a handful of entries
     * are placeholders for families of types rather than something that can be made.
     * example:
     * <pre>
     * // every entity type, which is a list of kinds rather than of
     * // things in the world
     * const reg = Client.getRegistryManager();
     * const ids = reg.getEntityTypeIds();
     * Chat.log(`${ids.size()} entity types`);
     * Chat.log(`${ids.get(0)} is one of them`);
     * </pre>
     *
     * @return a list of all entity type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<EntityId>")
    public List<String> getEntityTypeIds() {
        return BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * This is the fluid's default state rather than a level of it, so a fluid that can be at different
     * levels gives the one the game treats as its plain form. To ask whether a block is empty or
     * what amount is there, a block's own state is the thing to read.
     * <p>
     * The id may be written with or without its namespace, and an id that is not in the registry
     * gives the registry's default rather than failing, which for fluids is the empty fluid.
     * example:
     * <pre>
     * // the fluid's default state rather than a level of it. A bad id
     * // gives the registry default, which for fluids is the empty fluid
     * const reg = Client.getRegistryManager();
     * const lava = reg.getFluidState("minecraft:lava");
     * Chat.log(`${lava.getId()} level ${lava.getLevel()}, empty: ${lava.isEmpty()}`);
     * </pre>
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
     * This is the feature registry, which is the world's own generation features, so an id from here
     * names a thing that decides where terrain generates rather than a block or an item. This reads the game's own built in registry rather than the connected
     * server's, so it needs no connection and gives the same list with no world joined as with one.
     * <p>
     * That is the trade for not needing a connection: a data pack on the server can replace
     * or add entries, and none of that is reflected here.
     * example:
     * <pre>
     * // the world's own generation features, which is what an id here
     * // names rather than a block
     * const reg = Client.getRegistryManager();
     * const ids = reg.getFeatureIds();
     * Chat.log(`${ids.size} registered`);
     * Chat.log(`${ids.get(0)} is one of them`);
     * </pre>
     *
     * @return a list of all feature ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<FeatureId>")
    public List<String> getFeatureIds() {
        return BuiltInRegistries.FEATURE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * This is the structure piece registry, which holds the pieces a structure is assembled from and
     * not the structures themselves, so an id from here names a template rather than a whole
     * generated thing. This reads the game's own built in registry rather than the connected
     * server's, so it needs no connection and gives the same list with no world joined as with one.
     * <p>
     * That is the trade for not needing a connection: a data pack on the server can replace
     * or add entries, and none of that is reflected here.
     * example:
     * <pre>
     * // structure pieces, which is the pieces a structure is made
     * // from rather than the structures themselves
     * const reg = Client.getRegistryManager();
     * const ids = reg.getStructureFeatureIds();
     * Chat.log(`${ids.size} registered`);
     * Chat.log(`${ids.get(0)} is one of them`);
     * </pre>
     *
     * @return a list of all structure feature ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<StructureFeatureId>")
    public List<String> getStructureFeatureIds() {
        return BuiltInRegistries.STRUCTURE_PIECE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * This is the connected server's painting variant registry, so it reflects whatever the server
     * and its data packs have rather than only what the game ships with. That needs a connection,
     * and there is no handling for there not being one, so before a world is joined this is a
     * failure rather than an empty list. An id from here is a painting rather than a block or an
     * item.
     * example:
     * <pre>
     * // the server's own painting variants, so a data pack's paintings
     * // are in here and reading it needs a connection
     * const reg = Client.getRegistryManager();
     * const ids = reg.getPaintingIds();
     * Chat.log(`${ids.size()} paintings on this server`);
     * </pre>
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
     * This is the particle type registry, which names the kinds of particle rather than the places
     * they appear or the ways they can be configured, so an id from here is a type and not a
     * full particle description. This reads the game's own built in registry rather than the connected
     * server's, so it needs no connection and gives the same list with no world joined as with one.
     * <p>
     * That is the trade for not needing a connection: a data pack on the server can replace
     * or add entries, and none of that is reflected here.
     * example:
     * <pre>
     * // particle types, which are the kinds rather than the full
     * // descriptions a particle command takes
     * const reg = Client.getRegistryManager();
     * const ids = reg.getParticleTypeIds();
     * Chat.log(`${ids.size} registered`);
     * Chat.log(`${ids.get(0)} is one of them`);
     * </pre>
     *
     * @return a list of all particle type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<ParticleTypeId>")
    public List<String> getParticleTypeIds() {
        return BuiltInRegistries.PARTICLE_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * This is the game event registry, which names the events the game dispatches and listens for,
     * such as a block being placed or a mob entering a room. This reads the game's own built in registry rather than the connected
     * server's, so it needs no connection and gives the same list with no world joined as with one.
     * <p>
     * That is the trade for not needing a connection: a data pack on the server can replace
     * or add entries, and none of that is reflected here.
     * example:
     * <pre>
     * // game events, which are what the game dispatches and
     * // listens for
     * const reg = Client.getRegistryManager();
     * const ids = reg.getGameEventNames();
     * Chat.log(`${ids.size} registered`);
     * Chat.log(`${ids.get(0)} is one of them`);
     * </pre>
     *
     * @return a list of all game event names.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<GameEventName>")
    public List<String> getGameEventNames() {
        return BuiltInRegistries.GAME_EVENT.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * This is the status effect registry, and it is the same set that
     * {@link #getStatusEffects()} hands over as helpers, so an id from here can be given straight
     * to {@link #getStatusEffect(String)}. This reads the game's own built in registry rather than the connected
     * server's, so it needs no connection and gives the same list with no world joined as with one.
     * <p>
     * That is the trade for not needing a connection: a data pack on the server can replace
     * or add entries, and none of that is reflected here.
     * example:
     * <pre>
     * // status effect ids, which is the same set the effect helper
     * // list is built from
     * const reg = Client.getRegistryManager();
     * const ids = reg.getStatusEffectIds();
     * Chat.log(`${ids.size} registered`);
     * Chat.log(`${ids.get(0)} is one of them`);
     * </pre>
     *
     * @return a list of all status effect ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<StatusEffectId>")
    public List<String> getStatusEffectIds() {
        return BuiltInRegistries.MOB_EFFECT.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * This is every id in the block entity type registry, which is what a block's own
     * block entity type is named by. A block with no block entity is not in here at all, since
     * the registry holds one entry per kind rather than one per block.
     * example:
     * <pre>
     * // every block entity type id, which is one entry per kind of block
     * // entity rather than one per block that has one
     * const reg = Client.getRegistryManager();
     * const ids = reg.getBlockEntityTypeIds();
     * Chat.log(`${ids.size()} block entity types`);
     * Chat.log(`${ids.get(0)} is one of them`);
     * </pre>
     *
     * @return a list of all block entity type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<BlockEntityTypeId>")
    public List<String> getBlockEntityTypeIds() {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * This is the screen handler registry, which is what a container's screen is looked up by, so an
     * id from here names a menu rather than the block that opens it. This reads the game's own built in registry rather than the connected
     * server's, so it needs no connection and gives the same list with no world joined as with one.
     * <p>
     * That is the trade for not needing a connection: a data pack on the server can replace
     * or add entries, and none of that is reflected here.
     * example:
     * <pre>
     * // screen handlers, which is what a container's screen is
     * // looked up by rather than the block that opens it
     * const reg = Client.getRegistryManager();
     * const ids = reg.getScreenHandlerIds();
     * Chat.log(`${ids.size} registered`);
     * Chat.log(`${ids.get(0)} is one of them`);
     * </pre>
     *
     * @return a list of all screen handler ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<ScreenHandlerId>")
    public List<String> getScreenHandlerIds() {
        return BuiltInRegistries.MENU.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * This is the recipe type registry, which names the kinds of recipe rather than the recipes, so
     * an id from here is a type and not a recipe a player can craft. This reads the game's own built in registry rather than the connected
     * server's, so it needs no connection and gives the same list with no world joined as with one.
     * <p>
     * That is the trade for not needing a connection: a data pack on the server can replace
     * or add entries, and none of that is reflected here.
     * example:
     * <pre>
     * // recipe types, which are the kinds of recipe rather than
     * // the recipes themselves
     * const reg = Client.getRegistryManager();
     * const ids = reg.getRecipeTypeIds();
     * Chat.log(`${ids.size} registered`);
     * Chat.log(`${ids.get(0)} is one of them`);
     * </pre>
     *
     * @return a list of all recipe type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<RecipeTypeId>")
    public List<String> getRecipeTypeIds() {
        return BuiltInRegistries.RECIPE_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * This is the villager type registry, which is the profession a villager trades as, so an id from
     * here is a job rather than a biome or a block. This reads the game's own built in registry rather than the connected
     * server's, so it needs no connection and gives the same list with no world joined as with one.
     * <p>
     * That is the trade for not needing a connection: a data pack on the server can replace
     * or add entries, and none of that is reflected here.
     * example:
     * <pre>
     * // villager types, which is the job a villager trades as
     * const reg = Client.getRegistryManager();
     * const ids = reg.getVillagerTypeIds();
     * Chat.log(`${ids.size} registered`);
     * Chat.log(`${ids.get(0)} is one of them`);
     * </pre>
     *
     * @return a list of all villager type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<VillagerTypeId>")
    public List<String> getVillagerTypeIds() {
        return BuiltInRegistries.VILLAGER_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * This is the villager profession registry, which is the narrower thing a villager's type is built
     * from, so an id from here and one from {@link #getVillagerTypeIds()} are not the same set
     * and the two are not interchangeable. This reads the game's own built in registry rather than the connected
     * server's, so it needs no connection and gives the same list with no world joined as with one.
     * <p>
     * That is the trade for not needing a connection: a data pack on the server can replace
     * or add entries, and none of that is reflected here.
     * example:
     * <pre>
     * // villager professions, which is the narrower set the types
     * // are built from
     * const reg = Client.getRegistryManager();
     * const ids = reg.getVillagerProfessionIds();
     * Chat.log(`${ids.size} registered`);
     * Chat.log(`${ids.get(0)} is one of them`);
     * </pre>
     *
     * @return a list of all villager profession ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<VillagerProfession>")
    public List<String> getVillagerProfessionIds() {
        return BuiltInRegistries.VILLAGER_PROFESSION.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * This is the point of interest registry, which is the registry of what a villager will path to,
     * such as a bed or a work station. This reads the game's own built in registry rather than the connected
     * server's, so it needs no connection and gives the same list with no world joined as with one.
     * <p>
     * That is the trade for not needing a connection: a data pack on the server can replace
     * or add entries, and none of that is reflected here.
     * example:
     * <pre>
     * // point of interest types, which is what a villager will
     * // path to
     * const reg = Client.getRegistryManager();
     * const ids = reg.getPointOfInterestTypeIds();
     * Chat.log(`${ids.size} registered`);
     * Chat.log(`${ids.get(0)} is one of them`);
     * </pre>
     *
     * @return a list of all point of interest type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<PointOfInterestTypeId>")
    public List<String> getPointOfInterestTypeIds() {
        return BuiltInRegistries.POINT_OF_INTEREST_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * This is the memory module registry, which is the brain memory a mob can hold, so an id from
     * here is a kind of memory rather than a mob or a goal. This reads the game's own built in registry rather than the connected
     * server's, so it needs no connection and gives the same list with no world joined as with one.
     * <p>
     * That is the trade for not needing a connection: a data pack on the server can replace
     * or add entries, and none of that is reflected here.
     * example:
     * <pre>
     * // memory module types, which are the kinds of memory a mob
     * // can hold
     * const reg = Client.getRegistryManager();
     * const ids = reg.getMemoryModuleTypeIds();
     * Chat.log(`${ids.size} registered`);
     * Chat.log(`${ids.get(0)} is one of them`);
     * </pre>
     *
     * @return a list of all memory module type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<MemoryModuleTypeId>")
    public List<String> getMemoryModuleTypeIds() {
        return BuiltInRegistries.MEMORY_MODULE_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * This is the sensor registry, which is the sense a mob's brain uses to notice things, so an id
     * from here is a sense rather than the thing being sensed. This reads the game's own built in registry rather than the connected
     * server's, so it needs no connection and gives the same list with no world joined as with one.
     * <p>
     * That is the trade for not needing a connection: a data pack on the server can replace
     * or add entries, and none of that is reflected here.
     * example:
     * <pre>
     * // sensor types, which are the senses a mob's brain uses
     * const reg = Client.getRegistryManager();
     * const ids = reg.getSensorTypeIds();
     * Chat.log(`${ids.size} registered`);
     * Chat.log(`${ids.get(0)} is one of them`);
     * </pre>
     *
     * @return a list of all villager sensor type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<SensorTypeId>")
    public List<String> getSensorTypeIds() {
        return BuiltInRegistries.SENSOR_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * This is the activity registry, which is what a villager spends its day doing, so an id from here
     * is an occupation of the day rather than a job or a biome. This reads the game's own built in registry rather than the connected
     * server's, so it needs no connection and gives the same list with no world joined as with one.
     * <p>
     * That is the trade for not needing a connection: a data pack on the server can replace
     * or add entries, and none of that is reflected here.
     * example:
     * <pre>
     * // activity types, which is what a villager spends its day
     * // doing
     * const reg = Client.getRegistryManager();
     * const ids = reg.getActivityTypeIds();
     * Chat.log(`${ids.size} registered`);
     * Chat.log(`${ids.get(0)} is one of them`);
     * </pre>
     *
     * @return a list of all villager activity type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<ActivityTypeId>")
    public List<String> getActivityTypeIds() {
        return BuiltInRegistries.ACTIVITY.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * This is the stat registry, which is the game's own list of statistics and what each one's
     * counter is called, so an id from here is a statistic rather than an advancement. This reads the game's own built in registry rather than the connected
     * server's, so it needs no connection and gives the same list with no world joined as with one.
     * <p>
     * That is the trade for not needing a connection: a data pack on the server can replace
     * or add entries, and none of that is reflected here.
     * example:
     * <pre>
     * // stat types, which is the game's own list of statistics
     * const reg = Client.getRegistryManager();
     * const ids = reg.getStatTypeIds();
     * Chat.log(`${ids.size} registered`);
     * Chat.log(`${ids.get(0)} is one of them`);
     * </pre>
     *
     * @return a list of all stat type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<StatTypeId>")
    public List<String> getStatTypeIds() {
        return BuiltInRegistries.STAT_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * This is the entity attribute registry, which is the list of things an entity can be measured on,
     * such as its speed or its health, so an id from here names a measurement rather than a
     * value. This reads the game's own built in registry rather than the connected
     * server's, so it needs no connection and gives the same list with no world joined as with one.
     * <p>
     * That is the trade for not needing a connection: a data pack on the server can replace
     * or add entries, and none of that is reflected here.
     * example:
     * <pre>
     * // entity attributes, which are the things an entity can be
     * // measured on
     * const reg = Client.getRegistryManager();
     * const ids = reg.getEntityAttributeIds();
     * Chat.log(`${ids.size} registered`);
     * Chat.log(`${ids.get(0)} is one of them`);
     * </pre>
     *
     * @return a list of all entity attribute ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<EntityAttributeId>")
    public List<String> getEntityAttributeIds() {
        return BuiltInRegistries.ATTRIBUTE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * This is the potion type registry, which is the list of brews rather than the list of effects a
     * brew carries, so an id from here names a potion and not what it does. This reads the game's own built in registry rather than the connected
     * server's, so it needs no connection and gives the same list with no world joined as with one.
     * <p>
     * That is the trade for not needing a connection: a data pack on the server can replace
     * or add entries, and none of that is reflected here.
     * example:
     * <pre>
     * // potion types, which are the brews rather than the effects
     * // they carry
     * const reg = Client.getRegistryManager();
     * const ids = reg.getPotionTypeIds();
     * Chat.log(`${ids.size} registered`);
     * Chat.log(`${ids.get(0)} is one of them`);
     * </pre>
     *
     * @return a list of all potion type ids.
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaList<PotionTypeId>")
    public List<String> getPotionTypeIds() {
        return BuiltInRegistries.POTION.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /**
     * This is the same as {@link #parseIdentifier(String)} but as a method on the registry rather than
     * a static one, and it is the call to use for an id that another call will take. The
     * namespace is filled in when the id has none, so a bare name comes back fully qualified.
     * example:
     * <pre>
     * // the identifier an id names, with the namespace filled in when the
     * // id left it out
     * const reg = Client.getRegistryManager();
     * const id = reg.getIdentifier("stone");
     * Chat.log(`${id} from "stone"`);
     * </pre>
     *
     * @param identifier the String representation of the identifier, with the namespace and path
     * @return the raw minecraft Identifier.
     * @since 1.8.4
     */
    public ResourceLocation getIdentifier(String identifier) {
        return parseIdentifier(identifier);
    }

    /**
     * reads an item id into the game's own identifier, filling in the namespace.
     * <p>
     * A bare name gains the {@code minecraft} namespace and anything with a colon is left as it is,
     * so this is what turns what a script types into the form every other call here wants. A name
     * the game cannot accept as an identifier is a failure here rather than something that comes
     * back as itself, and the rule the game uses is stricter than it looks: an upper case letter is
     * not allowed anywhere in it.
     * <p>
     * This is the same as {@link #getIdentifier(String)} and the same as
     * {@link #parseNameSpace(String)} followed by the game's own parse, and it touches no registry,
     * so a name that is well formed but not registered still comes back as an identifier.
     * example:
     * <pre>
     * // a name that is well formed but not registered still comes back
     * // as an identifier, because nothing here looks at a registry
     * const RegistryHelper = Java.type("com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper");
     * const id = RegistryHelper.parseIdentifier("not_a_real_block");
     * Chat.log(`${id} is a well formed name`);
     * </pre>
     *
     * @param id the item's id, with or without the minecraft namespace
     * @return the raw minecraft Identifier.
     * @since 1.8.4
     */
    public static ResourceLocation parseIdentifier(String id) {
        return ResourceLocation.parse(parseNameSpace(id));
    }

    /**
     * fills in the minecraft namespace on an id that has none.
     * <p>
     * This is the one place the namespace rule lives, and every id taking call here runs the id
     * through it, so a bare name works everywhere and a name with a colon is passed through
     * untouched. Whether the namespace is one that exists is not checked, so a name for a mod that
     * is not installed comes back unchanged rather than failing.
     * <p>
     * The check is for a colon anywhere in the string, not for a prefix, so an id that merely
     * contains a colon is left alone rather than being given a namespace as well.
     * example:
     * <pre>
     * // a bare name gains the namespace, and one that has a namespace
     * // already is left exactly as it was
     * const RegistryHelper = Java.type("com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper");
     * Chat.log(RegistryHelper.parseNameSpace("stone"));
     * Chat.log(RegistryHelper.parseNameSpace("minecraft:stone"));
     * Chat.log(RegistryHelper.parseNameSpace("mymod:stone"));
     * </pre>
     *
     * @param id the item's id, with or without the minecraft namespace
     * @return the same id with {@code minecraft:} in front of it when it had no namespace of its
     *         own, and unchanged when it did
     * @since 1.8.4
     */
    public static String parseNameSpace(String id) {
        return id.indexOf(':') != -1 ? id : "minecraft:" + id;
    }

}
