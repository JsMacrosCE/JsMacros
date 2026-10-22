package com.jsmacrosce.jsmacros.client.api.helper.world;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.FluidState;
import com.jsmacrosce.jsmacros.api.math.Pos3D;

/**
 * The state of a fluid rather than the block that carries it: what kind of fluid it is, how much of
 * it there is, and how it behaves.<br>
 * A script gets one from {@link BlockStateHelper#getFluidState()}, which every block state answers,
 * and from the registry lookup for a fluid id, which gives that fluid's own default state. The
 * first of those is the one that matters in practice: a block that is not made of a fluid answers
 * with the empty fluid state rather than {@code null}, so {@link #isEmpty()} is the test for
 * whether there is a fluid here at all.<br>
 * Because a fluid state is a state holder it also answers the shared
 * {@link StateHelper#toMap()} and {@link StateHelper#with(String, String)}, which is how the level
 * property of a flowing fluid is read.
 * example:
 * <pre>
 * // is there a fluid in the block the player is standing on
 * const under = World.getBlock(Player.getPlayer().getBlockPos().down());
 * if (under !== null) {
 *   const fluid = under.getBlockStateHelper().getFluidState();
 *   if (fluid.isEmpty()) {
 *     Chat.log("dry ground");
 *   } else {
 *     Chat.log(`${fluid.getId()} at level ${fluid.getLevel()}, still: ${fluid.isStill()}`);
 *   }
 * }
 *
 * // the same fluid, straight out of the registry
 * const lava = Client.getRegistryManager().getFluidState("minecraft:lava");
 * Chat.log(`${lava.getId()} has blast resistance ${lava.getBlastResistance()}`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class FluidStateHelper extends StateHelper<FluidState> {

    public FluidStateHelper(FluidState base) {
        super(base);
    }

    /**
     * The fluid's registry id, in full, so {@code minecraft:water} or {@code minecraft:lava}. This
     * names the kind of fluid and not the block it sits in: a lava block and a lava cauldron both
     * answer the same id here, and the state properties are what tell them apart.
     * example:
     * <pre>
     * // name the fluid in a column of blocks, skipping the dry ones
     * const base = Player.getPlayer().getBlockPos();
     * for (let y = base.getY(); y > base.getY() - 8; y -= 1) {
     *   const block = World.getBlock(base.getX(), y, base.getZ());
     *   if (block === null) {
     *     continue;
     *   }
     *   const fluid = block.getBlockStateHelper().getFluidState();
     *   if (!fluid.isEmpty()) {
     *     Chat.log(`y ${y} is ${fluid.getId()}, level ${fluid.getLevel()}`);
     *   }
     * }
     * </pre>
     *
     * @return the fluid's id.
     * @since 1.8.4
     */
    public String getId() {
        return BuiltInRegistries.FLUID.getKey(base.getType()).toString();
    }

    /**
     * Whether this is a full, still body of the fluid rather than a flowing one. A source has
     * nothing running out of it, which is what makes it a source a player can swim in or a bucket
     * can scoop from.
     * example:
     * <pre>
     * // find the top of a water column, which is the still part
     * const base = Player.getPlayer().getBlockPos();
     * for (let y = base.getY(); y > base.getY() - 12; y -= 1) {
     *   const block = World.getBlock(base.getX(), y, base.getZ());
     *   if (block === null) {
     *     continue;
     *   }
     *   const fluid = block.getBlockStateHelper().getFluidState();
     *   if (!fluid.isEmpty()) {
     *     Chat.log(`y ${y}: level ${fluid.getLevel()}, still ${fluid.isStill()}`);
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this fluid is still, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isStill() {
        return base.isSource();
    }

    /**
     * Whether there is no fluid here. Every block state answers {@link BlockStateHelper#getFluidState()}
     * whether or not it holds a fluid, and a block that does not hold one answers with the empty
     * fluid state, so this is the test rather than a null check.
     * example:
     * <pre>
     * // only the blocks that actually hold a fluid
     * const pos = Player.getPlayer().getBlockPos();
     * World.iterateBox(pos.offset(-4, -2, -4), pos.offset(4, 2, 4), JavaWrapper.methodToJava(
     *   function (block) {
     *     const fluid = block.getBlockStateHelper().getFluidState();
     *     if (!fluid.isEmpty()) {
     *       Chat.log(`${fluid.getId()} at ${block.getBlockPos()}`);
     *     }
     *   }
     * ));
     * </pre>
     *
     * @return {@code true} if this fluid is empty (the default fluid state for non fluid blocks),
     * {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isEmpty() {
        return base.isEmpty();
    }

    /**
     * How tall this state is drawn and collided with, as a fraction of a full block. It is the
     * {@link #getLevel()} divided by nine, so a still source reaches eight ninths of its block
     * rather than the whole of it, and a flowing one is shallower in proportion to how little of it
     * there is.<br>
     * Nothing here ever answers exactly one. A full block comes from the game's own version of this
     * question that also takes the world and a position, and that one gives a whole block only
     * where the same fluid carries on into the block above; read this way, a state on its own is
     * always short of the top of its block.
     * example:
     * <pre>
     * // how deep the flow is where the player is standing
     * const pos = Player.getPlayer().getBlockPos().down();
     * const block = World.getBlock(pos);
     * if (block !== null) {
     *   const fluid = block.getBlockStateHelper().getFluidState();
     *   if (!fluid.isEmpty()) {
     *     Chat.log(`${(fluid.getHeight() * 16).toFixed(1)} sixteenths of a block deep`);
     *   }
     * }
     * </pre>
     *
     * @return the height of this state.
     * @since 1.8.4
     */
    public float getHeight() {
        return base.getOwnHeight();
    }

    /**
     * How much of the fluid there is, on the level scale the game uses for a fluid's own
     * {@code level} property. A still source sits at the top of that scale and a thin sheet at the
     * bottom of it, and the number grows with the body of fluid rather than with its spread: a
     * source always answers eight whatever else is around it, and a flowing one answers its own
     * {@code level} property, which runs from one to eight. A source has no such property of its
     * own, so it is the only kind that answers a level the state cannot show you in
     * {@link StateHelper#toMap()}.<br>
     * This is not the same as {@link #getHeight()}: the level is a property value and the height is
     * what gets drawn, and the height is the level divided by nine rather than the level itself.
     * example:
     * <pre>
     * // the level property of a flowing state, read the same way as any other property
     * const flowing = Client.getRegistryManager().getFluidState("minecraft:flowing_lava");
     * for (const [name, value] of flowing.toMap()) {
     *   Chat.log(`${name} is ${value}`);
     * }
     * Chat.log(`and the same number through getLevel: ${flowing.getLevel()}`);
     *
     * // a still source has no level property, and answers 8 all the same
     * const lava = Client.getRegistryManager().getFluidState("minecraft:lava");
     * for (const [name, value] of lava.toMap()) {
     *   Chat.log(`${name} is ${value}`);
     * }
     * Chat.log(`and getLevel says ${lava.getLevel()} anyway`);
     * </pre>
     *
     * @return the level of this state.
     * @since 1.8.4
     */
    public int getLevel() {
        return base.getAmount();
    }

    /**
     * Whether this kind of fluid does anything on its own when the world ticks it. On a normal game
     * it is lava that answers true, and the answer is what the fire spreading off it hangs off, so
     * this is the test for "is this block a fire hazard to whatever is next to it".
     * example:
     * <pre>
     * // which of the two fluids spreads fire
     * for (const id of ["minecraft:water", "minecraft:lava"]) {
     *   const fluid = Client.getRegistryManager().getFluidState(id);
     *   Chat.log(`${fluid.getId()} ticks on its own: ${fluid.hasRandomTicks()}`);
     * }
     * </pre>
     *
     * @return {@code true} if the fluid has some random tick logic (only used by lava to do the
     * fire spread), {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasRandomTicks() {
        return base.isRandomlyTicking();
    }

    /**
     * The velocity the game would apply to something carried by this fluid at the given block. The
     * answer comes out of the world that is currently loaded, since flow is a question about the
     * blocks around it and not something the state knows on its own, so a world has to be joined
     * before this can be asked.
     * example:
     * <pre>
     * // which way the current runs, at the block under the player
     * const pos = Player.getPlayer().getBlockPos().down();
     * const block = World.getBlock(pos);
     * if (block !== null) {
     *   const fluid = block.getBlockStateHelper().getFluidState();
     *   if (!fluid.isEmpty()) {
     *     const flow = fluid.getVelocity(block.getBlockPos());
     *     Chat.log(`flowing ${flow.getX()}, ${flow.getY()}, ${flow.getZ()}`);
     *     if (flow.getY() > 0) {
     *       Chat.log("falling");
     *     }
     *   }
     * }
     * </pre>
     *
     * @param pos the position in the world
     * @return the velocity that will be applied to entities at the given position.
     * @since 1.8.4
     */
    public Pos3D getVelocity(BlockPosHelper pos) {
        var velocity = base.getFlow(Minecraft.getInstance().level, pos.getRaw());
        return new Pos3D(velocity.x, velocity.y, velocity.z);
    }

    /**
     * The block state the game would make out of this fluid, which is how a fluid becomes something
     * placeable. The block it names is the one for the fluid, and it carries a level worked out from
     * this state, so a flowing one and a still one give different blocks even though the id is the
     * same.
     * example:
     * <pre>
     * // the block a fluid turns into
     * for (const id of ["minecraft:water", "minecraft:lava"]) {
     *   const fluid = Client.getRegistryManager().getFluidState(id);
     *   const state = fluid.getBlockState();
     *   Chat.log(`${fluid.getId()} becomes ${state.getId()}`);
     *   for (const [name, value] of state.toMap()) {
     *     Chat.log(`  ${name} is ${value}`);
     *   }
     * }
     * </pre>
     *
     * @return the block state of this fluid.
     * @since 1.8.4
     */
    public BlockStateHelper getBlockState() {
        return new BlockStateHelper(base.createLegacyBlock());
    }

    /**
     * How well this fluid stands up to an explosion. It is a property of the kind of fluid rather
     * than of the state, so a flowing and a still answer the same.
     * example:
     * <pre>
     * // compare the two fluids
     * const water = Client.getRegistryManager().getFluidState("minecraft:water");
     * const lava = Client.getRegistryManager().getFluidState("minecraft:lava");
     * Chat.log(`water ${water.getBlastResistance()}, lava ${lava.getBlastResistance()}`);
     * </pre>
     *
     * @return the blast resistance of this fluid.
     * @since 1.8.4
     */
    public float getBlastResistance() {
        return base.getExplosionResistance();
    }

    @Override
    protected StateHelper<FluidState> create(FluidState base) {
        return new FluidStateHelper(base);
    }

    @Override
    public String toString() {
        return String.format("FluidStateHelper:{\"id\": \"%s\", \"properties\": %s}", getId(), toMap());
    }

}
