package com.jsmacrosce.jsmacros.client.api.helper.world;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * A helper for one of Minecraft's six cardinal directions. Directions can be queried for their
 * axis and angles, converted to a one-block vector, or turned; the quarter-turn methods reject
 * vertical directions.
 * <pre>
 * // whichever way the player is looking
 * const player = Player.getPlayer();
 * const facing = player.getFacingDirection();
 * Chat.log(`${facing.getName()} is on the ${facing.getAxis()} axis at yaw ${facing.getYaw()}`);
 *
 * // turning a direction gives a new one, never a change to the old
 * const behind = facing.getOpposite();
 * Chat.log(`${facing.getName()} is ahead, ${behind.getName()} is behind`);
 *
 * // and a direction is also the one block step it stands for
 * const step = facing.getVector();
 * Chat.log(`one block that way is ${step.getX()}, ${step.getY()}, ${step.getZ()}`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class DirectionHelper extends BaseHelper<Direction> {

    /**
     * Wraps the supplied direction for helper access.
     *
     * @param base the direction to wrap
     */
    public DirectionHelper(Direction base) {
        super(base);
    }

    /**
     * Returns the lowercase serialized name of this direction, such as {@code north}.
     *
     * @return the name of this direction.
     * @since 1.8.4
     */
    public String getName() {
        return base.getName();
    }

    /**
     * Returns the axis name ({@code x}, {@code y}, or {@code z}) of this direction.
     *
     * @return the name of the axis this direction is aligned to.
     * @since 1.8.4
     */
    public String getAxis() {
        return base.getAxis().getName();
    }

    /**
     * Returns whether this direction is vertical (aligned with the {@code y} axis).
     *
     * @return {@code true} if this direction is vertical, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isVertical() {
        return base.getAxis().isVertical();
    }

    /**
     * Returns whether this direction is horizontal (aligned with the {@code x} or {@code z} axis).
     *
     * @return {@code true} if this direction is horizontal, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isHorizontal() {
        return base.getAxis().isHorizontal();
    }

    /**
     * Returns whether this direction points toward increasing coordinates on its axis.
     *
     * @return {@code true} if this direction is pointing in a positive direction, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isTowardsPositive() {
        return base.getAxisDirection().getStep() == 1;
    }

    /**
     * Returns this direction's yaw in degrees. Vertical directions share a yaw value, so it is
     * meaningful as a heading only for horizontal directions.
     *
     * @return the yaw of this direction.
     * @since 1.8.4
     */
    public float getYaw() {
        return base.toYRot();
    }

    /**
     * Returns this direction's pitch in degrees: zero for horizontal directions and {@code -90}
     * or {@code 90} for vertical directions.
     *
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
     * Returns the direction opposite this one, including for vertical directions.
     *
     * @return the opposite direction.
     * @since 1.8.4
     */
    public DirectionHelper getOpposite() {
        return new DirectionHelper(base.getOpposite());
    }

    /**
     * Returns the horizontal direction one quarter turn anticlockwise from this one. Vertical
     * directions cannot be turned and cause an {@link IllegalStateException}.
     *
     * @return the direction to the left.
     * @throws IllegalStateException if this direction is vertical, since there is no side to turn
     *                               towards.
     * @since 1.8.4
     */
    public DirectionHelper getLeft() {
        return new DirectionHelper(base.getCounterClockWise());
    }

    /**
     * Returns the horizontal direction one quarter turn clockwise from this one. Vertical
     * directions cannot be turned and cause an {@link IllegalStateException}.
     *
     * @return the direction to the right.
     * @throws IllegalStateException if this direction is vertical, since there is no side to turn
     *                               towards.
     * @since 1.8.4
     */
    public DirectionHelper getRight() {
        return new DirectionHelper(base.getClockWise());
    }

    /**
     * Returns this direction's one-block offset: one coordinate is {@code 1} or {@code -1}, and
     * the other two are zero.
     *
     * @return the direction as a directional vector.
     * @since 1.8.4
     */
    public Pos3D getVector() {
        Vec3i vec = base.getUnitVec3i();
        return new Pos3D(vec.getX(), vec.getY(), vec.getZ());
    }

    /**
     * Tests whether {@code yaw} points into this direction's horizontal half-plane. Vertical
     * directions always return {@code false}; an exact boundary between directions also returns
     * {@code false}.
     *
     * @param yaw the yaw to check
     * @return {@code true} when the yaw points into this direction's horizontal half-plane;
     *         {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean pointsTo(double yaw) {
        return base.isFacingAngle((float) yaw);
    }

    /**
     * Returns a diagnostic representation of the wrapped direction.
     *
     * @return a diagnostic representation containing the direction's name, yaw, and pitch
     */
    @Override
    public String toString() {
        return String.format("DirectionHelper:{\"name\": \"%s\", \"yaw\": %f, \"pitch\": %f}", getName(), getYaw(), getPitch());
    }

}
