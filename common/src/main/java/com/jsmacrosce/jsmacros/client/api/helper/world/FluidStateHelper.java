package com.jsmacrosce.jsmacros.client.api.helper.world;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.FluidState;
import com.jsmacrosce.jsmacros.api.math.Pos3D;

/**
 * A fluid state describes a fluid type and its state properties. {@link BlockStateHelper#getFluidState()}
 * returns an empty state rather than {@code null} for blocks without fluid; registry lookups by
 * fluid id return the fluid type's default state. Properties are exposed through
 * {@link StateHelper#toMap()} and {@link StateHelper#with(String, String)}.
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

    /**
     * Wraps the supplied fluid state.
     *
     * @param base raw fluid state to wrap.
     */
    public FluidStateHelper(FluidState base) {
        super(base);
    }

    /**
     * Returns the fluid type's registry id (for example, {@code minecraft:water}); it identifies
     * the fluid, not the block containing it.
     *
     * @return the fluid's id.
     * @since 1.8.4
     */
    public String getId() {
        return BuiltInRegistries.FLUID.getKey(base.getType()).toString();
    }

    /**
     * Returns whether this state is a source state.
     *
     * @return {@code true} if this fluid is still, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isStill() {
        return base.isSource();
    }

    /**
     * Returns whether this is the empty fluid state.
     *
     * @return {@code true} if this fluid is empty; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isEmpty() {
        return base.isEmpty();
    }

    /**
     * Returns this state type's own height as a fraction of a block, without considering the
     * surrounding world. This may differ from the world- and position-dependent fluid height.
     *
     * @return this state's own height as a fraction of a block.
     * @since 1.8.4
     */
    public float getHeight() {
        return base.getOwnHeight();
    }

    /**
     * Returns the fluid amount on the game's scale. Source states return {@code 8}; flowing states
     * return their {@code level} property, in the range one through eight. Unlike flowing states,
     * source states have no {@code level} property in {@link StateHelper#toMap()}. This amount is
     * distinct from {@link #getHeight()}, which is defined by the fluid type.
     *
     * @return the fluid amount on the game's scale.
     * @since 1.8.4
     */
    public int getLevel() {
        return base.getAmount();
    }

    /**
     * Returns whether this fluid type is marked as randomly ticking.
     *
     * @return {@code true} if this fluid type is randomly ticking; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasRandomTicks() {
        return base.isRandomlyTicking();
    }

    /**
     * Returns the flow vector computed for this fluid state and position using the currently loaded
     * client level. A loaded level is required.
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
     * @param pos position used to compute the flow
     * @return the computed flow vector.
     * @since 1.8.4
     */
    public Pos3D getVelocity(BlockPosHelper pos) {
        var velocity = base.getFlow(Minecraft.getInstance().level, pos.getRaw());
        return new Pos3D(velocity.x, velocity.y, velocity.z);
    }

    /**
     * Returns the block state produced from this fluid state by the game's legacy block conversion.
     *
     * @return the block state of this fluid.
     * @since 1.8.4
     */
    public BlockStateHelper getBlockState() {
        return new BlockStateHelper(base.createLegacyBlock());
    }

    /**
     * Returns the explosion resistance of this fluid type; it does not vary by fluid state.
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

    /**
     * Returns a diagnostic representation containing this fluid's registry id and state properties.
     *
     * @return a diagnostic representation of the wrapped fluid state.
     */
    @Override
    public String toString() {
        return String.format("FluidStateHelper:{\"id\": \"%s\", \"properties\": %s}", getId(), toMap());
    }

}
