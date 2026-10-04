package com.jsmacrosce.jsmacros.api.math;

import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Objects;

/**
 * A line segment in the world, held as two public writable endpoints: {@code x1}/{@code y1} and
 * {@code z1} for the start, {@code x2}/{@code y2} and {@code z2} for the end. It extends
 * {@link Vec2D}, so it is also a two dimensional segment and the flat measurements there work on
 * it, but everything that reports a length or an angle is overridden to include z.<br>
 * {@link #to3D()} returns a new segment containing all six endpoint coordinates, including
 * both z coordinates.<br>
 * Most of what it can tell you is a difference between the two ends, which is what
 * {@link #getDeltaX() getDeltaX()}, {@link #getDeltaY() getDeltaY()},
 * {@link #getDeltaZ() getDeltaZ()} and {@link #getMagnitude() getMagnitude()} read off, plus
 * {@link #getYaw() getYaw()} and {@link #getPitch() getPitch()}, which report the direction it runs
 * in as the same pair of angles the game itself uses for a player's view.<br>
 * Nothing here modifies the vector it is called on. Every member builds and returns a brand new
 * {@code Vec3D}, so the arithmetic is safe to chain. The endpoints themselves are the exception,
 * since assigning to them does change this vector.<br>
 * The arithmetic treats the whole segment as the thing being operated on. {@link #add(Vec3D) add}
 * and {@link #multiply(Vec3D) multiply} act on both endpoints, so they translate or resize the
 * segment rather than changing its direction, and {@link #normalize() normalize()} divides every
 * endpoint coordinate by the length, which rescales the segment towards the origin. There are also
 * {@link #addStart(Pos3D) addStart} and {@link #addEnd(Pos3D) addEnd} for moving one end and
 * leaving the other where it is, which is what a look direction needs. Passing a plain
 * {@link Vec2D} to the inherited two dimensional forms still selects those, and hands back a
 * {@code Vec2D} with z dropped.
 * example:
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
 * @since 1.2.6 [citation needed]
 */
public class Vec3D extends Vec2D {
    /**
     * the z coordinate of the start of the segment. Public and writable, so assigning to it moves
     * the start without going through any of the arithmetic below.
     */
    public double z1;
    /**
     * the z coordinate of the end of the segment. Public and writable, so assigning to it moves the
     * end without going through any of the arithmetic below.
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
     * a new position at the end of the segment, with the z coordinate from {@code z2} as well as
     * the flat ones. This is a copy, so writing to it does not move the segment.
     *
     * @return a new {@link Pos3D} at the end of this segment
     * @since 1.2.6
     */
    @Override
    public Pos3D getEnd() {
        return new Pos3D(x2, y2, z2);
    }

    /**
     * the Euclidean length of the segment through all three axes, the square root of
     * {@link #getDeltaX() getDeltaX()}, {@link #getDeltaY() getDeltaY()} and
     * {@link #getDeltaZ() getDeltaZ()} added together as squares. This overrides the flat
     * {@link Vec2D#getMagnitude()}, so a segment that climbs will not report {@code 0} just
     * because it is straight up and down. A segment whose ends are the same point has length
     * {@code 0}, and no segment has a negative one.
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
     * the length of the segment squared, taken through all three axes, that is the value inside
     * the square root that {@link #getMagnitude()} takes. Comparing magnitudes this way avoids
     * the square root, and it is the right thing to reach for when all you want to know is which
     * of two segments is longer, since squaring keeps the order.
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
     * a new segment with all six of this one's endpoint coordinates moved by the matching endpoint
     * coordinate of another one. Since both ends move by the same amount this translates the
     * segment and leaves its length and direction alone. This vector is not changed.<br>
     * Passing a plain {@link Vec2D} instead selects the inherited
     * {@link Vec2D#add(Vec2D) add(Vec2D)} and hands back a {@code Vec2D} with the z coordinates
     * gone.
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
     * a new segment with the start moved by the given position and the end left where it is, which
     * tilts the segment without changing how far it is. This vector is not changed.
     *
     * @param pos the position to move the start by
     * @return a new segment starting at the moved point and ending where this one did
     * @since 1.6.4
     */
    public Vec3D addStart(Pos3D pos) {
        return new Vec3D(this.x1 + pos.x, this.y1 + pos.y, this.z1 + pos.z, this.x2, this.y2, this.z2);
    }

    /**
     * a new segment with the end moved by the given position and the start left where it is, which
     * tilts the segment without changing how far it is. This vector is not changed.
     *
     * @param pos the position to move the end by
     * @return a new segment ending at the moved point and starting where this one did
     * @since 1.6.4
     */
    public Vec3D addEnd(Pos3D pos) {
        return new Vec3D(this.x1, this.y1, this.z1, this.x2 + pos.x, this.y2 + pos.y, this.z2 + pos.z);
    }

    /**
     * a new segment with the start moved by the given amounts and the end left where it is. This
     * vector is not changed.
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
     * a new segment with the end moved by the given amounts and the start left where it is. This
     * vector is not changed.
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
     * a new segment with this one's start moved by the first three amounts and its end moved by the
     * last three, so the two ends can be moved independently rather than the whole segment being
     * translated. This vector is not changed.
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
     * a new segment with each of this one's endpoint coordinates multiplied by the matching
     * endpoint coordinate of another one. This is a componentwise product, not a scale, so unless
     * the other segment starts at the origin it will not simply make this one longer. This vector
     * is not changed.<br>
     * Passing a plain {@link Vec2D} instead selects the inherited
     * {@link Vec2D#multiply(Vec2D) multiply(Vec2D)} and hands back a {@code Vec2D} with the z
     * coordinates gone.
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
     * a new segment with each of this one's endpoint coordinates multiplied by the matching factor
     * given here, so again the two ends can be scaled independently. To scale the whole segment by
     * one factor, use {@link #scale(double)} instead of passing the same number six times. This
     * vector is not changed.
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
     * a new segment with all six endpoint coordinates multiplied by one factor, which resizes the
     * segment about the origin rather than changing only its length. To keep the start where it is
     * and only change the length, move the end with {@link #addEnd(Pos3D) addEnd()} instead. This
     * vector is not changed.
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
     * a new segment that points the same way as this one but is one long. What that means in
     * practice is that every endpoint coordinate, start included, is divided by this segment's
     * length, so the difference between the two ends comes out one long while the start slides
     * towards the origin. For a segment that already starts at {@code 0, 0, 0}, such as the one
     * {@link com.jsmacrosce.jsmacros.client.api.library.impl.FPositionCommon#createLookingVector(double, double) createLookingVector()}
     * builds, that is exactly a unit direction; for one that does not, the start is moved as well
     * and the result is a segment of length one running from a point near the origin rather than
     * from where this one started.<br>
     * A segment whose ends are the same point has length {@code 0}, and every coordinate of the
     * result is then {@code NaN} rather than an error.
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
     * how far above or below the horizon this segment points, in degrees, in the same convention
     * the game uses for a player's view: {@code 0} is level, negative is up and positive is down.<br>
     * It is worked out from how far the segment travels horizontally compared with how far it
     * climbs, where the horizontal side is a single distance across x and z together and the
     * vertical side is the y difference.<br>
     * For finite coordinates the range is {@code [-90, 90]}: straight up is {@code -90},
     * straight down is {@code 90}, and a zero-length segment returns zero.
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
     * Copies this segment without discarding either z coordinate.
     *
     * @return a new Vec3D containing all six endpoint coordinates
     */
    @Override
    public Vec3D to3D() {
        return new Vec3D(this);
    }

    /**
     * which way around the horizon this segment points, in degrees, in the same convention the game
     * uses for a player's view: {@code 0} is south, {@code 90} is west and {@code -90} is east.
     * North is the one direction that has two answers. The value is wrapped to between
     * {@code -180} and {@code 180}, but the wrap sends {@code 180} down to {@code -180} and
     * this method then negates it, so what actually comes out is the half open range from
     * just above {@code -180} up to and including {@code 180}. A segment pointing exactly
     * north therefore returns {@code 180} and never {@code -180}, and everything else lands
     * strictly between the two. The game's own {@code Mth.wrapDegrees} turns either end into
     * {@code -180}, so put the two through that before comparing yaws with {@code >} or
     * {@code <} rather than expecting north to be a single number.<br>
     * Only the x and z differences matter, so the segment's y coordinates are ignored and a
     * segment running straight up or straight down reports the yaw of its horizontal projection, or
     * {@code 0} when it has none. A segment whose ends are the same point has no direction to
     * report and also comes out as {@code 0}.
     *
     * @return the yaw of this segment's direction, in degrees, between {@code -180} and {@code 180}
     * @since 1.2.6
     */
    public float getYaw() {
        double dx = x2 - x1;
        double dz = z2 - z1;
        return -(float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(dx, dz)));
    }

    /**
     * how much two segments point the same way, the sum of the products of their matching
     * coordinate differences over all three axes. The sign is the useful part: positive when the
     * two directions are less than a right angle apart, {@code 0} when they are exactly a right
     * angle apart or one of them has no length, and negative when they are more than a right angle
     * apart. The magnitude is not an angle, it is a length squared scaled by how far apart the
     * directions are.<br>
     * The result is not normalized, so it grows with the length of both segments.
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
     * the vector perpendicular to both of these segments at once, which is the direction that is at
     * a right angle to this one and to {@code vec} together. Only the end of the result is that
     * direction; the start is always {@code 0, 0, 0}, so read it with
     * {@link #getEnd() getEnd()}.<br>
     * The direction follows the right hand rule from this segment towards {@code vec}, and the two
     * arguments are not interchangeable: swapping them negates the result. When the segments are
     * parallel the result is the zero vector rather than an error, and it is not normalized, so
     * its length is the product of the two lengths scaled by how far apart they point.
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
     * a new segment running the other way, with the ends swapped. Its direction is the opposite of
     * this one's and its length is the same. This vector is not changed.
     *
     * @return a new segment from the end of this one to its start
     * @since 1.2.6
     */
    @Override
    public Vec3D reverse() {
        return new Vec3D(x2, y2, z2, x1, y1, z1);
    }

    @Override
    public String toString() {
        return String.format("%f, %f, %f -> %f, %f, %f", x1, y1, z1, x2, y2, z2);
    }

    /**
     * the difference between the two ends of this segment as a plain three component float vector,
     * that is {@code x2 - x1, y2 - y1, z2 - z1}. That makes it a direction rather than a position:
     * the start of the segment is not in the result at all, and for a segment starting at the
     * origin the result is simply the end point. Each component is narrowed from a
     * {@code double} to a {@code float}, so a difference that is only a very small number can round
     * away to zero.
     *
     * @return a new {@code Vector3f} holding the coordinate differences of this segment
     * @since 1.6.5
     */
    public Vector3f toMojangFloatVector() {
        return new Vector3f((float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1));
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), z1, z2);
    }

    /**
     * orders this segment against another one, first by where the start sits and then, only if the
     * two starts compare equal, by where the end sits. The ordering of each end is
     * {@link Pos3D#compareTo(Pos3D)}, that is by x, then y, then z, and it is a raw comparison on
     * the coordinates rather than a distance, so two segments that cross can still order against
     * each other.
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
