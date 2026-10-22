package com.jsmacrosce.jsmacros.client.api.helper.world;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper;
import com.jsmacrosce.jsmacros.client.api.helper.NBTElementHelper;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.Util;

/**
 * A block in the world: the three things that go together and are almost always wanted together.
 * A {@link BlockStateHelper} is what the block is, a {@link BlockPosHelper} is where it is, and
 * this class is the pair, so a world lookup gives back one of these and everything about the block
 * at that spot is reachable from it without asking again.<br>
 * The fourth thing it carries is the block entity, the extra data some blocks keep: a chest's
 * contents, a sign's text. That is the one part that may be missing, and {@link #getNBT()} answers
 * {@code null} when it is, so that call is the test for whether this block keeps data at all.
 * <p>
 * This is what {@code World.getBlock} hands back, and what the scan and iterate calls pass to their
 * callbacks, so it is the type a script is holding more often than any other.
 * example:
 * <pre>
 * const block = World.getBlock(Player.getPlayer().getBlockPos().down());
 * if (block !== null) {
 *   Chat.log(`${block.getId()} at ${block.getBlockPos()}`);
 *   Chat.log(`it is ${block.getName()} and solid is ${block.getBlockStateHelper().isSolid()}`);
 *
 *   // the extra data, for the blocks that keep some
 *   const nbt = block.getNBT();
 *   if (nbt === null) {
 *     Chat.log("no block entity here");
 *   } else {
 *     Chat.log(nbt.asString());
 *   }
 * }
 *
 * // the same three numbers, answered straight off the helper
 * Chat.log(`${block.getX()}, ${block.getY()}, ${block.getZ()}`);
 * </pre>
 *
 * @author Wagyourtail
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class BlockDataHelper extends BaseHelper<BlockState> {
    private static final Minecraft mc = Minecraft.getInstance();

    private final Block b;
    private final BlockPos bp;
    private final BlockEntity e;

    public BlockDataHelper(BlockState b, BlockEntity e, BlockPos bp) {
        super(b);
        this.b = b.getBlock();
        this.bp = bp;
        this.e = e;
    }

    /**
     * The {@code x} coordinate of the block, straight off the position this helper was built with.
     * example:
     * <pre>
     * // the block column the player is in
     * const block = World.getBlock(Player.getPlayer().getBlockPos());
     * if (block !== null) {
     *   Chat.log(`x ${block.getX()}`);
     * }
     * </pre>
     *
     * @return the {@code x} value of the block.
     * @since 1.1.7
     */
    public int getX() {
        return bp.getX();
    }

    /**
     * The {@code y} coordinate of the block, straight off the position this helper was built with.
     * example:
     * <pre>
     * // how high up the player is
     * const block = World.getBlock(Player.getPlayer().getBlockPos());
     * if (block !== null) {
     *   Chat.log(`y ${block.getY()}`);
     * }
     * </pre>
     *
     * @return the {@code y} value of the block.
     * @since 1.1.7
     */
    public int getY() {
        return bp.getY();
    }

    /**
     * The {@code z} coordinate of the block, straight off the position this helper was built with.
     * example:
     * <pre>
     * const block = World.getBlock(Player.getPlayer().getBlockPos());
     * if (block !== null) {
     *   Chat.log(`z ${block.getZ()}`);
     * }
     * </pre>
     *
     * @return the {@code z} value of the block.
     * @since 1.1.7
     */
    public int getZ() {
        return bp.getZ();
    }

    /**
     * The block's registry id, in full. This names the kind of block rather than the state, so two
     * different states of the same block give the same string; the state properties are on
     * {@link #getBlockState()} and {@link #getBlockStateHelper()}.
     * example:
     * <pre>
     * // what the block under the player is, by id
     * const block = World.getBlock(Player.getPlayer().getBlockPos().down());
     * if (block !== null) {
     *   Chat.log(block.getId());
     *   // and the same id back through the registry gives the same block
     *   Chat.log(Client.getRegistryManager().getBlock(block.getId()).getId());
     * }
     * </pre>
     *
     * @return the item ID of the block.
     */
    @DocletReplaceReturn("BlockId")
    public String getId() {
        return BuiltInRegistries.BLOCK.getKey(b).toString();
    }

    /**
     * The block's translated name, which is the word the game shows for the kind of block rather than
     * the state, so a stair reads the same whichever way it faces.
     * example:
     * <pre>
     * const block = World.getBlock(Player.getPlayer().getBlockPos().down());
     * if (block !== null) {
     *   Chat.log(`standing on ${block.getName().getString()}`);
     * }
     * </pre>
     *
     * @return the translated name of the block. (was string before 1.6.5)
     */
    public TextHelper getName() {
        return TextHelper.wrap(b.getName());
    }

    /**
     * The extra data the block keeps, as a compound: a chest's items, a sign's lines, a spawner's
     * mob. Most blocks keep none, and those answer {@code null} here rather than an empty compound,
     * so this is the test for whether the block is one of the interesting ones.
     * <p>
     * The data is written out fresh each call from the block entity as it stands, and the block's
     * position and its id are left out of it.
     * example:
     * <pre>
     * // read a chest's contents
     * const block = World.getBlock(0, 64, 0);
     * if (block !== null) {
     *   const nbt = block.getNBT();
     *   if (nbt === null) {
     *     Chat.log(`${block.getId()} keeps no data`);
     *   } else {
     *     for (const key of nbt.getKeys()) {
     *       Chat.log(`${key}: ${nbt.asString(key)}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the block entity's data, or {@code null} if this block has no block entity.
     * @since 1.5.1, used to be a {@link Map} of {@link String} to {@link String}
     */
    @Nullable
    public NBTElementHelper.NBTCompoundHelper getNBT() {
        return e == null ? null : NBTElementHelper.wrapCompound(e.saveWithoutMetadata(RegistryHelper.getWrapperLookup()));
    }

    /**
     * The block's state, which is what {@code World.getBlock} has already worked out and what most
     * questions about a block are really questions about. Going through here rather than reaching
     * for the raw state means a script never has to know which of the two it was handed.
     * example:
     * <pre>
     * // the state of the block, and one of its properties
     * const block = World.getBlock(Player.getPlayer().getBlockPos().down());
     * if (block !== null) {
     *   const state = block.getBlockStateHelper();
     *   for (const [name, value] of state.toMap()) {
     *     Chat.log(`${name} is ${value}`);
     *   }
     *   Chat.log(`is it solid: ${state.isSolid()}`);
     * }
     * </pre>
     *
     * @return a helper over the state this block is in.
     * @since 1.6.5
     */
    public BlockStateHelper getBlockStateHelper() {
        return new BlockStateHelper(base);
    }

    /**
     * The kind of block this is, with the state left off.
     *
     * @return a helper over the kind of block.
     * @since 1.6.5
     * @deprecated use {@link #getBlock()} instead.
     */
    @Deprecated
    public BlockHelper getBlockHelper() {
        return getBlock();
    }

    /**
     * The kind of block this is, with the state left off: stone rather than a particular stair, and
     * the block rather than the item that places it. This is the same answer
     * {@link #getBlockStateHelper()} would give through its own {@code getBlock()}, reached in one
     * call instead of two.
     * example:
     * <pre>
     * // the block on its own, and the item that places it
     * const block = World.getBlock(Player.getPlayer().getBlockPos().down());
     * if (block !== null) {
     *   const kind = block.getBlock();
     *   Chat.log(`${kind.getId()} is called ${kind.getName().getString()}`);
     *   Chat.log(`placed by ${kind.getDefaultItemStack().getItemId()}`);
     * }
     * </pre>
     *
     * @return the block
     * @since 1.6.5
     */
    public BlockHelper getBlock() {
        return new BlockHelper(base.getBlock());
    }

    /**
     * The state properties as a plain map from property name to the name of the value it holds.
     * This is the same map {@link StateHelper#toMap()} gives through
     * {@link #getBlockStateHelper()}, spelled out here so a script holding a world block does not
     * have to go through a state to read it.
     * example:
     * <pre>
     * // one property of the block under the player
     * const block = World.getBlock(Player.getPlayer().getBlockPos().down());
     * if (block !== null) {
     *   const props = block.getBlockState();
     *   for (const [name, value] of props) {
     *     Chat.log(`${name} is ${value}`);
     *   }
     *
     *   // and the same names again through the state helper
     *   const throughState = block.getBlockStateHelper().toMap();
     *   for (const name of Object.keys(props)) {
     *     if (props[name] !== throughState[name]) {
     *       Chat.log(`${name} disagrees: ${props[name]} versus ${throughState[name]}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return block state data as a {@link Map}.
     * @since 1.1.7
     */
    public Map<String, String> getBlockState() {
        Map<String, String> map = new HashMap<>();
        //? if >=26.1 {
        /*base.getValues().forEach(e -> map.put(e.property().getName(), Util.getPropertyName(e.property(), e.value())));
        *///?} else {
        base.getValues().forEach((key, value) -> map.put(key.getName(), Util.getPropertyName(key, value)));
        //?}
        return map;
    }

    /**
     * Where this block is, as a position of its own. This is the same position the world lookup was
     * given, so the two always agree.
     * example:
     * <pre>
     * // the position, and a walk outwards from it
     * const block = World.getBlock(Player.getPlayer().getBlockPos());
     * if (block !== null) {
     *   const pos = block.getBlockPos();
     *   Chat.log(`${pos}, above is ${pos.up()}, east is ${pos.east()}`);
     * }
     * </pre>
     *
     * @return the block pos.
     * @since 1.2.7
     */
    public BlockPosHelper getBlockPos() {
        return new BlockPosHelper(bp);
    }

    /**
     * The kind of block as the game object itself, for the calls that want it. A script rarely needs
     * this: the wrapped {@link #getBlock()} answers everything a script asks about a block.
     * example:
     * <pre>
     * // the raw object, and the same block through the wrapped helper
     * const block = World.getBlock(0, 64, 0);
     * if (block !== null) {
     *   const raw = block.getRawBlock();
     *   Chat.log(`the raw object is the same kind of block: ${raw === block.getRawBlockState().getBlock()}`);
     *   Chat.log(block.getBlock().getId());
     * }
     * </pre>
     *
     * @return the raw block object this helper wraps.
     */
    public Block getRawBlock() {
        return b;
    }

    /**
     * The block state as the game object itself, for the calls that want it. This is what
     * {@code World.getBlock} was built from, and it is the same thing
     * {@link #getBlockStateHelper()} wraps.
     * example:
     * <pre>
     * // the raw state is the very object the helper wraps
     * const block = World.getBlock(0, 64, 0);
     * if (block !== null) {
     *   Chat.log(block.getRawBlockState() === block.getRaw());
     *   Chat.log(block.getBlockStateHelper().getId());
     * }
     * </pre>
     *
     * @return the raw block state this helper wraps.
     */
    public BlockState getRawBlockState() {
        return base;
    }

    /**
     * The block entity as the game object itself, which is {@code null} for a block that keeps no
     * data of its own. This is the object {@link #getNBT()} writes out, so the two always agree
     * about whether there is one.
     * example:
     * <pre>
     * // whether there is a block entity at all, without going through nbt
     * const block = World.getBlock(0, 64, 0);
     * if (block !== null) {
     *   const entity = block.getRawBlockEntity();
     *   if (entity === null) {
     *     Chat.log(`${block.getId()} has no block entity`);
     *   } else {
     *     Chat.log(`${block.getId()} does, and it reads back the same: ${entity !== null}`);
     *   }
     * }
     * </pre>
     *
     * @return the raw block entity, or {@code null} if this block has none.
     */
    public BlockEntity getRawBlockEntity() {
        return e;
    }

    @Override
    public String toString() {
        return String.format("BlockDataHelper:{\"x\": %d, \"y\": %d, \"z\": %d, \"id\": \"%s\"}", bp.getX(), bp.getY(), bp.getZ(), this.getId());
    }

}
