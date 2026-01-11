package com.jsmacrosce.jsmacros.client.api.helper.world;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class DirectionHelper extends BaseHelper<Direction> {

    public DirectionHelper(Direction base) {
        super(base);
    }

    /**
     * @return the name of this direction.
     * @since 1.8.4
     */
    public String getName() {
        return base.getName();
    }

    /**
     * @return the name of the axis this direction is aligned to.
     * @since 1.8.4
     */
    public String getAxis() {
        return base.getAxis().getName();
    }

    /**
     * @return {@code true} if this direction is vertical, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isVertical() {
        return base.getAxis().isVertical();
    }

    /**
     * @return {@code true} if this direction is horizontal, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isHorizontal() {
        return base.getAxis().isHorizontal();
    }

    /**
     * @return {@code true} if this direction is pointing in a positive direction, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isTowardsPositive() {
        return base.getAxisDirection().getStep() == 1;
    }

    /**
     * @return the yaw of this direction.
     * @since 1.8.4
     */
    public float getYaw() {
        return base.toYRot();
    }

    /**
     * @return the pitch of this direction.
     * @since 1.8.4
     */
    public float getPitch() {
        if (isHorizontal()) {
            return 0;
        } else {
            return base.getStepY() * 90;
        }
    }

    /**
     * @return the opposite direction.
     * @since 1.8.4
     */
    public DirectionHelper getOpposite() {
        return new DirectionHelper(base.getOpposite());
    }

    /**
     * @return the direction to the left.
     * @since 1.8.4
     */
    public DirectionHelper getLeft() {
        return new DirectionHelper(base.getCounterClockWise());
    }

    /**
     * @return the direction to the right.
     * @since 1.8.4
     */
    public DirectionHelper getRight() {
        return new DirectionHelper(base.getClockWise());
    }

    /**
     * @return the direction as a directional vector.
     * @since 1.8.4
     */
    public Pos3D getVector() {
        Vec3i vec = base.getUnitVec3i();
        return new Pos3D(vec.getX(), vec.getY(), vec.getZ());
    }

    /**
     * @param yaw the yaw to check
     * @return {@code true} if the yaw is facing this direction more than any other one,
     * {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean pointsTo(double yaw) {
        return base.isFacingAngle((float) yaw);
    }

    @Override
    public String toString() {
        return String.format("DirectionHelper:{\"name\": \"%s\", \"yaw\": %f, \"pitch\": %f}", getName(), getYaw(), getPitch());
    }

}
