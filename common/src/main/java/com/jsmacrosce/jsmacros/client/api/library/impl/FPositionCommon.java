package com.jsmacrosce.jsmacros.client.api.library.impl;

import net.minecraft.world.phys.Vec3;
import com.jsmacrosce.jsmacros.api.math.Pos2D;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.api.math.Vec2D;
import com.jsmacrosce.jsmacros.api.math.Vec3D;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.library.BaseLibrary;
import com.jsmacrosce.jsmacros.core.library.Library;

/**
 * position helper classes
 *
 * @since 1.6.3
 */
@Library("PositionCommon")
@SuppressWarnings("unused")
public class FPositionCommon extends BaseLibrary {

    public FPositionCommon(Core<?, ?> runner) {
        super(runner);
    }

    /**
     * create a new vector object
     *
     * @param x1
     * @param y1
     * @param z1
     * @param x2
     * @param y2
     * @param z2
     * @return
     * @since 1.6.3
     */
    public Vec3D createVec(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new Vec3D(x1, y1, z1, x2, y2, z2);
    }

    /**
     * @param entity
     * @return
     * @since 1.8.4
     */
    public Vec3D createLookingVector(EntityHelper<?> entity) {
        Vec3 rotation = entity.getRaw().getLookAngle();
        return new Vec3D(0, 0, 0, rotation.x, rotation.y, rotation.z);
    }

    /**
     * @param yaw
     * @param pitch
     * @return
     * @since 1.8.4
     */
    public Vec3D createLookingVector(double yaw, double pitch) {
        Vec3 rotation = Vec3.directionFromRotation((float) pitch, (float) yaw);
        return new Vec3D(0, 0, 0, rotation.x, rotation.y, rotation.z);
    }

    /**
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     * @since 1.6.3
     */
    public Vec2D createVec(double x1, double y1, double x2, double y2) {
        return new Vec2D(x1, y1, x2, y2);
    }

    /**
     * @param x
     * @param y
     * @param z
     * @return
     * @since 1.6.3
     */
    public Pos3D createPos(double x, double y, double z) {
        return new Pos3D(x, y, z);
    }

    /**
     * @param x
     * @param y
     * @return
     * @since 1.6.3
     */
    public Pos2D createPos(double x, double y) {
        return new Pos2D(x, y);
    }

    /**
     * @param x the x position of the block
     * @param y the y position of the block
     * @param z the z position of the block
     * @return a {@link BlockPosHelper} for the given coordinates.
     * @since 1.8.4
     */
    public BlockPosHelper createBlockPos(int x, int y, int z) {
        return new BlockPosHelper(x, y, z);
    }

}
