package com.jsmacrosce.jsmacros.client.api.helper.world;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.FluidState;
import com.jsmacrosce.jsmacros.api.math.Pos3D;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class FluidStateHelper extends StateHelper<FluidState> {

    public FluidStateHelper(FluidState base) {
        super(base);
    }

    /**
     * @return the fluid's id.
     * @since 1.8.4
     */
    public String getId() {
        return BuiltInRegistries.FLUID.getKey(base.getType()).toString();
    }

    /**
     * @return {@code true} if this fluid is still, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isStill() {
        return base.isSource();
    }

    /**
     * @return {@code true} if this fluid is empty (the default fluid state for non fluid blocks),
     * {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isEmpty() {
        return base.isEmpty();
    }

    /**
     * @return the height of this state.
     * @since 1.8.4
     */
    public float getHeight() {
        return base.getOwnHeight();
    }

    /**
     * @return the level of this state.
     * @since 1.8.4
     */
    public int getLevel() {
        return base.getAmount();
    }

    /**
     * @return {@code true} if the fluid has some random tick logic (only used by lava to do the
     * fire spread), {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasRandomTicks() {
        return base.isRandomlyTicking();
    }

    /**
     * @param pos the position in the world
     * @return the velocity that will be applied to entities at the given position.
     * @since 1.8.4
     */
    public Pos3D getVelocity(BlockPosHelper pos) {
        var velocity = base.getFlow(Minecraft.getInstance().level, pos.getRaw());
        return new Pos3D(velocity.x, velocity.y, velocity.z);
    }

    /**
     * @return the block state of this fluid.
     * @since 1.8.4
     */
    public BlockStateHelper getBlockState() {
        return new BlockStateHelper(base.createLegacyBlock());
    }

    /**
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
