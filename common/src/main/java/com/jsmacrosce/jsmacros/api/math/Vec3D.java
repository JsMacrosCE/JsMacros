package com.jsmacrosce.jsmacros.api.math;

import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Objects;

/**
 * A 3D line segment with mutable endpoints. Length and angle measurements include z.
 * Inherited {@link Vec2D} overloads still operate in 2D: {@code add(Vec2D)} and
 * {@code multiply(Vec2D)} return a plain {@code Vec2D}, while {@code dotProduct(Vec2D)} and
 * {@code compareTo(Vec2D)} ignore z.
 * The 3D arithmetic overloads return new segments; {@link #normalize()} divides both endpoints by
 * the magnitude, moving the start unless it is at the origin.
 * <pre>
 * // Player.getPlayer() is null outside a world, so there is nothing to aim there
 * const player = Player.getPlayer();
 * if (player !== null) {
 *   // a unit length segment from the origin along the direction the player looks in
 *   const look = PositionCommon.createLookingVector(player.getYaw(), player.getPitch());
 *   // the same angles again, read back off the segment itself
 *   Chat.log(`looking yaw ${look.getYaw()}, pitch ${look.getPitch()}`);
 *   // push the end of it five blocks out to get a point in the world
 *   const far = look.getEnd().scale(5);
 *   Chat.log(`which lands on ${far}`);
 * }
 * </pre>
 * @author Wagyourtail
 * @since 1.2.6
 */
public class Vec3D extends Vec2D {
    /**
     * The z coordinate of the segment's start.
     */
    public double z1;
    /**
     * The z coordinate of the segment's end.
     */
    public double z2;

    /**
     * makes a segment from the coordinates of its two ends.
     *
     * @param x1 the x coordinate of the start
     * @param y1 the y coordinate of the start
     * @param z1 the z coordinate of the start
     * @param x2 the x coordinate of the end
     * @param y2 the y coordinate of the end
     * @param z2 the z coordinate of the end
     * @since 1.2.6
     */
    public Vec3D(double x1, double y1, double z1, double x2, double y2, double z2) {
        super(x1, y1, x2, y2);
        this.z1 = z1;
        this.z2 = z2;
    }

    /**
     * makes a segment running from one position to another.
     *
     * @param start the position the segment starts at
     * @param end the position the segment ends at
     * @since 1.2.6
     */
    public Vec3D(Pos3D start, Pos3D end) {
        super(start, end);
        this.z1 = start.z;
        this.z2 = end.z;
    }

    /**
     * copies another vector. The result is a separate object, so writing to it afterwards does not
     * touch {@code vec}.
     *
     * @param vec the vector to copy
     * @since 1.2.6
     */
    public Vec3D(Vec3D vec) {
        super(vec);
        this.z1 = vec.z1;
        this.z2 = vec.z2;
    }

    /**
     * the z coordinate of the start of the segment, the same value as the {@code z1} field.
     *
     * @return the z coordinate of the start
     * @since 1.2.6
     */
    public double getZ1() {
        return z1;
    }

    /**
     * the z coordinate of the end of the segment, the same value as the {@code z2} field.
     *
     * @return the z coordinate of the end
     * @since 1.2.6
     */
    public double getZ2() {
        return z2;
    }

    /**
     * how far the segment runs along z, that is its end's z minus its start's z. It is positive
     * when the end is to the south of the start and negative when it is to the north, in the same
     * sense that a positive x difference points east.
     *
     * @return the z distance covered by this segment
     * @since 1.2.6
     */
    public double getDeltaZ() {
        return z2 - z1;
    }

    /**
     * a new position at the start of the segment, with the z coordinate from {@code z1} as well as
     * the flat ones. This is a copy, so writing to it does not move the segment.
     *
     * @return a new {@link Pos3D} at the start of this segment
     * @since 1.2.6
     */
    @Override
    public Pos3D getStart() {
        return new Pos3D(x1, y1, z1);
    }

    /**
     * Returns a copy of the segment's end position, including its z coordinate. Modifying the
     * returned position does not change this segment.
     *
     * @return a new {@link Pos3D} at the end of this segment
     * @since 1.2.6
     */
    @Override
    public Pos3D getEnd() {
        return new Pos3D(x2, y2, z2);
    }

    /**
     * Returns the Euclidean length across x, y, and z. Unlike {@link Vec2D#getMagnitude()}, this
     * includes vertical displacement.
     *
     * @return the length of this segment
     * @since 1.2.6
     */
    @Override
    public double getMagnitude() {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Returns the squared Euclidean length across x, y, and z.
     *
     * @return the squared length of this segment
     * @since 1.6.5
     */
    @Override
    public double getMagnitudeSq() {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Returns a new segment with each endpoint coordinate added to the corresponding coordinate of
     * {@code vec}; this segment is unchanged. A {@link Vec2D} argument instead selects the inherited
     * 2D overload, which returns a {@code Vec2D} and ignores z.
     *
     * @param vec the segment whose endpoints are added to this one's
     * @return a new segment with every endpoint coordinate added to its matching one
     * @since 1.2.6
     */
    public Vec3D add(Vec3D vec) {
        return new Vec3D(
                this.x1 + vec.x1,
                this.y1 + vec.y1,
                this.z1 + vec.z1,
                this.x2 + vec.x2,
                this.y2 + vec.y2,
                this.z2 + vec.z2
        );
    }

    /**
     * Returns a new segment with its start offset by {@code pos} and its end unchanged. This segment
     * is not modified.
     *
     * @param pos the position to move the start by
     * @return a new segment starting at the moved point and ending where this one did
     * @since 1.6.4
     */
    public Vec3D addStart(Pos3D pos) {
        return new Vec3D(this.x1 + pos.x, this.y1 + pos.y, this.z1 + pos.z, this.x2, this.y2, this.z2);
    }

    /**
     * Returns a new segment with its end offset by {@code pos} and its start unchanged. This segment
     * is not modified.
     *
     * @param pos the position to move the end by
     * @return a new segment ending at the moved point and starting where this one did
     * @since 1.6.4
     */
    public Vec3D addEnd(Pos3D pos) {
        return new Vec3D(this.x1, this.y1, this.z1, this.x2 + pos.x, this.y2 + pos.y, this.z2 + pos.z);
    }

    /**
     * Returns a new segment with its start offset by the given amounts and its end unchanged; this
     * segment is not modified.
     *
     * @param x the amount to add to the start's x coordinate
     * @param y the amount to add to the start's y coordinate
     * @param z the amount to add to the start's z coordinate
     * @return a new segment starting at the moved point and ending where this one did
     * @since 1.6.4
     */
    public Vec3D addStart(double x, double y, double z) {
        return new Vec3D(this.x1 + x, this.y1 + y, this.z1 + z, this.x2, this.y2, this.z2);
    }

    /**
     * Returns a new segment with its end offset by the given amounts and its start unchanged; this
     * segment is not modified.
     *
     * @param x the amount to add to the end's x coordinate
     * @param y the amount to add to the end's y coordinate
     * @param z the amount to add to the end's z coordinate
     * @return a new segment ending at the moved point and starting where this one did
     * @since 1.6.4
     */
    public Vec3D addEnd(double x, double y, double z) {
        return new Vec3D(this.x1, this.y1, this.z1, this.x2 + x, this.y2 + y, this.z2 + z);
    }

    /**
     * Returns a new segment with its start offset by the first three amounts and its end by the last
     * three. Equal offsets translate the segment; this segment is unchanged.
     *
     * @param x1 the amount to add to the start's x coordinate
     * @param y1 the amount to add to the start's y coordinate
     * @param z1 the amount to add to the start's z coordinate
     * @param x2 the amount to add to the end's x coordinate
     * @param y2 the amount to add to the end's y coordinate
     * @param z2 the amount to add to the end's z coordinate
     * @return a new segment offset endpoint for endpoint
     * @since 1.6.3
     */
    public Vec3D add(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new Vec3D(this.x1 + x1, this.y1 + y1, this.z1 + z1, this.x2 + x2, this.y2 + y2, this.z2 + z2);
    }

    /**
     * Returns a new segment whose endpoint coordinates are multiplied componentwise by those of
     * {@code vec}; this is not a uniform scale, and this segment is unchanged. A {@link Vec2D}
     * argument instead selects the inherited 2D overload, which returns a {@code Vec2D} and ignores
     * z.
     *
     * @param vec the segment whose endpoint coordinates are the factors
     * @return a new segment with every endpoint coordinate multiplied by its matching one
     * @since 1.2.6
     */
    public Vec3D multiply(Vec3D vec) {
        return new Vec3D(
                this.x1 * vec.x1,
                this.y1 * vec.y1,
                this.z1 * vec.z1,
                this.x2 * vec.x2,
                this.y2 * vec.y2,
                this.z2 * vec.z2
        );
    }

    /**
     * Returns a new segment with each endpoint coordinate multiplied by its corresponding factor;
     * the start and end can therefore be scaled independently. This segment is unchanged. For one
     * factor applied to all coordinates, use {@link #scale(double)}.
     *
     * @param x1 the factor for the start's x coordinate
     * @param y1 the factor for the start's y coordinate
     * @param z1 the factor for the start's z coordinate
     * @param x2 the factor for the end's x coordinate
     * @param y2 the factor for the end's y coordinate
     * @param z2 the factor for the end's z coordinate
     * @return a new segment with each endpoint coordinate multiplied by its factor
     * @since 1.6.3
     */
    public Vec3D multiply(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new Vec3D(this.x1 * x1, this.y1 * y1, this.z1 * z1, this.x2 * x2, this.y2 * y2, this.z2 * z2);
    }

    /**
     * Returns a new segment with all six endpoint coordinates multiplied by {@code scale}. This
     * scales both endpoints about the origin; this segment is unchanged.
     *
     * @param scale the factor to multiply all six endpoint coordinates by
     * @return a new segment with all six endpoint coordinates scaled
     * @since 1.6.3
     */
    @Override
    public Vec3D scale(double scale) {
        return new Vec3D(x1 * scale, y1 * scale, z1 * scale, x2 * scale, y2 * scale, z2 * scale);
    }

    /**
     * Returns a new segment with both endpoints divided by this segment's length. The resulting
     * segment has length one, but its start is also scaled and is preserved only if it is the
     * origin. If the segment has zero length, division produces {@code NaN} coordinates.
     *
     * @return a new segment with the same direction but a length of 1
     * @since 1.6.5
     */
    @Override
    public Vec3D normalize() {
        double mag = getMagnitude();
        return new Vec3D(x1 / mag, y1 / mag, z1 / mag, x2 / mag, y2 / mag, z2 / mag);
    }

    /**
     * Returns the segment's pitch in degrees: {@code 0} is level, negative is up, and positive is
     * down. It uses the y difference and the combined horizontal distance across x and z; a
     * zero-length segment returns {@code 0}.
     *
     * @return the pitch of this segment's direction in degrees
     * @since 1.2.6
     */
    public float getPitch() {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double xz = Math.sqrt(dx * dx + dz * dz);
        return (float) Math.toDegrees(Math.atan2(-dy, xz));
    }

    /**
     * Returns a copy of this segment, including both z coordinates.
     *
     * @return a new Vec3D containing all six endpoint coordinates
     */
    @Override
    public Vec3D to3D() {
        return new Vec3D(this);
    }

    /**
     * Returns the segment's yaw in degrees, using the view convention: {@code 0} is south,
     * {@code 90} is west, and {@code -90} is east. Only x and z differences matter. The result is
     * in {@code (-180, 180]}; exactly north returns {@code 180}. With no horizontal displacement,
     * including a zero-length segment, the result is {@code 0}.
     *
     * @return the yaw of this segment's direction in degrees, in {@code (-180, 180]}
     * @since 1.2.6
     */
    public float getYaw() {
        double dx = x2 - x1;
        double dz = z2 - z1;
        return -(float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(dx, dz)));
    }

    /**
     * Returns the dot product of the segments' endpoint differences. Its sign indicates whether
     * their directions are less than, equal to, or greater than a right angle apart; it is zero if
     * either segment has zero length. The result is not normalized, so its magnitude depends on
     * both segment lengths. A {@link Vec2D} argument selects the inherited 2D overload, which
     * ignores z.
     *
     * @param vec the segment to compare this one's direction against
     * @return the dot product of the two direction differences
     * @since 1.2.6
     */
    public double dotProduct(Vec3D vec) {
        double dz1 = z2 - z1;
        double dz2 = vec.z2 - vec.z1;
        return super.dotProduct(vec) + dz1 * dz2;
    }

    /**
     * Returns the cross product of the endpoint differences as a segment from the origin. Its
     * direction follows the right-hand rule from this segment toward {@code vec}; swapping the
     * arguments negates it. Parallel directions produce a zero vector. The result is not
     * normalized.
     *
     * @param vec the other segment to take the perpendicular to
     * @return a new vector from the origin to the vector perpendicular to both segments
     * @since 1.2.6
     */
    public Vec3D crossProduct(Vec3D vec) {
        double dx1 = x2 - x1;
        double dx2 = vec.x2 - vec.x1;
        double dy1 = y2 - y1;
        double dy2 = vec.y2 - vec.y1;
        double dz1 = z2 - z1;
        double dz2 = vec.z2 - vec.z1;
        return new Vec3D(0, 0, 0, dy1 * dz2 - dz1 * dy2, dz1 * dx2 - dx1 * dz2, dx1 * dy2 - dy1 * dx2);
    }

    /**
     * Returns a new segment with its endpoints swapped; this segment is unchanged.
     *
     * @return a new segment from the end of this one to its start
     * @since 1.2.6
     */
    @Override
    public Vec3D reverse() {
        return new Vec3D(x2, y2, z2, x1, y1, z1);
    }

    /**
     * Formats the ordered start and end coordinates in x, y, z order. Finite coordinates have six
     * fractional digits; formatting uses the default locale, so this text is not a stable
     * serialization.
     *
     * @return the formatted endpoint coordinates
     */
    @Override
    public String toString() {
        return String.format("%f, %f, %f -> %f, %f, %f", x1, y1, z1, x2, y2, z2);
    }

    /**
     * Returns the endpoint difference ({@code x2 - x1, y2 - y1, z2 - z1}) as a float vector, not
     * a position. Each component is narrowed from {@code double} to {@code float} and may lose
     * precision.
     *
     * @return a new {@code Vector3f} holding the coordinate differences of this segment
     * @since 1.6.5
     */
    public Vector3f toMojangFloatVector() {
        return new Vector3f((float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1));
    }

    /**
     * Compares this segment with another object by exact runtime class and all six ordered endpoint
     * coordinates. Coordinates use {@link Double#compare(double, double)}: NaN values compare
     * equal, while positive and negative zero compare differently. This compares the segment's
     * endpoints, not just its displacement or length.
     *
     * @param o the object to compare with this segment
     * @return {@code true} if {@code o} is this object or has the same runtime class and equal
     *         endpoint coordinates; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vec3D vec3D = (Vec3D) o;
        return Double.compare(x1, vec3D.x1) == 0
                && Double.compare(y1, vec3D.y1) == 0
                && Double.compare(x2, vec3D.x2) == 0
                && Double.compare(y2, vec3D.y2) == 0
                && Double.compare(z1, vec3D.z1) == 0
                && Double.compare(z2, vec3D.z2) == 0;
    }

    /**
     * Computes a hash code from the current ordered endpoint coordinates, including both z
     * coordinates. Because this segment is mutable, its hash may change when its coordinates change;
     * avoid using it as a hash key while mutating it.
     *
     * @return the hash code for the current coordinates
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), z1, z2);
    }

    /**
     * Compares segments lexicographically by start position, then end position; each position is
     * compared by x, y, then z using {@link Pos3D#compareTo(Pos3D)}. This coordinate ordering does
     * not compare segment lengths or whether they intersect.
     *
     * @param o the segment to compare this one against
     * @return a negative number, zero, or a positive number as this segment is before, the same
     *         as, or after {@code o}
     * @since 1.2.6
     */
    public int compareTo(@NotNull Vec3D o) {
        int i = getStart().compareTo(o.getStart());
        if (i == 0) {
            i = getEnd().compareTo(o.getEnd());
        }
        return i;
    }
}
