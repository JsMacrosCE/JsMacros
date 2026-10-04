package com.jsmacrosce.jsmacros.api.math;

import java.util.Objects;

/**
 * A line segment in the x/y plane, held as two public writable endpoints: {@code x1}/{@code y1}
 * for the start and {@code x2}/{@code y2} for the end. It is a segment rather than a direction, so
 * most of what it can tell you is a difference between the two ends, and that is what
 * {@link #getDeltaX() getDeltaX()}, {@link #getDeltaY() getDeltaY()} and
 * {@link #getMagnitude() getMagnitude()} read off.<br>
 * Nothing here modifies the vector it is called on. Every member builds and returns a brand new
 * {@code Vec2D}, so the arithmetic is safe to chain and safe to keep a reference to. The endpoints
 * themselves are the exception, since assigning to them does change this vector.<br>
 * The arithmetic here treats the whole segment as the thing being operated on, which is not always
 * what a direction needs. {@link #add(Vec2D) add} and {@link #multiply(Vec2D) multiply} move or
 * resize both endpoints at once, so they translate or resize the segment rather than changing its
 * direction, and {@link #normalize() normalize()} divides both endpoints by the magnitude, which
 * rescales the segment towards the origin instead of just its length. For a vector from one place
 * to another, build it with {@link Pos2D#toVector(Pos2D) toVector()} or
 * {@link Pos2D#toReverseVector(Pos2D) toReverseVector()} and start the segment at
 * {@code 0, 0} if you want it to behave like a plain direction.
 * example:
 * <pre>
 * const a = PositionCommon.createPos(0, 0);
 * const b = PositionCommon.createPos(3, 4);
 * const segment = b.toVector(a);
 * // 3, 4 for the segment across the plane and 5 for its length
 * Chat.log(`delta ${segment.getDeltaX()}, ${segment.getDeltaY()}, length ${segment.getMagnitude()}`);
 * // the same direction, now one long. normalize() divides both ends by the
 * // length, which only looks like a pure rescale because this starts at 0, 0
 * const direction = segment.normalize();
 * Chat.log(`normalized to ${direction}`);
 * </pre>
 * @author Wagyourtail
 * @since 1.2.6 [citation needed]
 */
public class Vec2D {
    /**
     * the x coordinate of the start of the segment. Public and writable, so assigning to it moves
     * the start without going through any of the arithmetic below.
     */
    public double x1;
    /**
     * the y coordinate of the start of the segment. Public and writable, so assigning to it moves
     * the start without going through any of the arithmetic below.
     */
    public double y1;
    /**
     * the x coordinate of the end of the segment. Public and writable, so assigning to it moves the
     * end without going through any of the arithmetic below.
     */
    public double x2;
    /**
     * the y coordinate of the end of the segment. Public and writable, so assigning to it moves the
     * end without going through any of the arithmetic below.
     */
    public double y2;

    /**
     * makes a segment from the coordinates of its two ends.
     *
     * @param x1 the x coordinate of the start
     * @param y1 the y coordinate of the start
     * @param x2 the x coordinate of the end
     * @param y2 the y coordinate of the end
     * @since 1.2.6
     */
    public Vec2D(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    /**
     * makes a segment running from one position to another.
     *
     * @param start the position the segment starts at
     * @param end the position the segment ends at
     * @since 1.2.6
     */
    public Vec2D(Pos2D start, Pos2D end) {
        this.x1 = start.x;
        this.y1 = start.y;
        this.x2 = end.x;
        this.y2 = end.y;
    }

    /**
     * copies another vector. The result is a separate object, so writing to it afterwards does not
     * touch {@code vec}.
     *
     * @param vec the vector to copy
     * @since 1.2.6
     */
    public Vec2D(Vec2D vec) {
        this.x1 = vec.x1;
        this.y1 = vec.y1;

        this.x2 = vec.x2;
        this.y2 = vec.y2;
    }

    /**
     * the x coordinate of the start of the segment, the same value as the {@code x1} field.
     *
     * @return the x coordinate of the start
     * @since 1.2.6
     */
    public double getX1() {
        return x1;
    }

    /**
     * the y coordinate of the start of the segment, the same value as the {@code y1} field.
     *
     * @return the y coordinate of the start
     * @since 1.2.6
     */
    public double getY1() {
        return y1;
    }

    /**
     * the x coordinate of the end of the segment, the same value as the {@code x2} field.
     *
     * @return the x coordinate of the end
     * @since 1.2.6
     */
    public double getX2() {
        return x2;
    }

    /**
     * the y coordinate of the end of the segment, the same value as the {@code y2} field.
     *
     * @return the y coordinate of the end
     * @since 1.2.6
     */
    public double getY2() {
        return y2;
    }

    /**
     * how far the segment runs along x, that is its end's x minus its start's x. It is positive
     * when the end is to the right of the start and negative when it is to the left.
     *
     * @return the x distance covered by this segment
     * @since 1.2.6
     */
    public double getDeltaX() {
        return x2 - x1;
    }

    /**
     * how far the segment runs along y, that is its end's y minus its start's y. It is positive
     * when the end is above the start and negative when it is below.
     *
     * @return the y distance covered by this segment
     * @since 1.2.6
     */
    public double getDeltaY() {
        return y2 - y1;
    }

    /**
     * a new position at the start of the segment, with x at {@code x1} and y at {@code y1}. This
     * is a copy, so writing to it does not move the segment.
     *
     * @return a new {@link Pos2D} at the start of this segment
     * @since 1.2.6
     */
    public Pos2D getStart() {
        return new Pos2D(x1, y1);
    }

    /**
     * a new position at the end of the segment, with x at {@code x2} and y at {@code y2}. This is
     * a copy, so writing to it does not move the segment.
     *
     * @return a new {@link Pos2D} at the end of this segment
     * @since 1.2.6
     */
    public Pos2D getEnd() {
        return new Pos2D(x2, y2);
    }

    /**
     * the Euclidean length of the segment, the square root of
     * {@link #getDeltaX() getDeltaX()} and {@link #getDeltaY() getDeltaY()} added together as
     * squares. For a segment from {@code 0, 0} to {@code 3, 4} that is {@code 5}. A segment whose
     * ends are the same point has length {@code 0}, and no segment has a negative one.
     *
     * @return the length of this segment
     * @since 1.2.6
     */
    public double getMagnitude() {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * the length of the segment squared, that is the value inside the square root that
     * {@link #getMagnitude()} takes. Comparing magnitudes this way avoids the square root, and
     * it is the right thing to reach for when all you want to know is which of two segments is
     * longer, since squaring keeps the order.<br>
     * For {@link Vec3D} this is overridden to include the z difference as well.
     *
     * @return the squared length of this segment
     * @since 1.6.5
     */
    public double getMagnitudeSq() {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return dx * dx + dy * dy;
    }

    /**
     * a new segment with both of this one's endpoints moved by the matching endpoint of another
     * one. Since both ends move by the same amount this translates the segment and leaves its
     * length and direction alone. This vector is not changed.
     *
     * @param vec the segment whose endpoints are added to this one's
     * @return a new segment with {@code x1 + vec.x1, y1 + vec.y1, x2 + vec.x2, y2 + vec.y2}
     * @since 1.2.6
     */
    public Vec2D add(Vec2D vec) {
        return new Vec2D(x1 + vec.x1, y1 + vec.y1, x2 + vec.x2, y2 + vec.y2);
    }

    /**
     * a new segment with this one's start moved by the first pair of amounts and its end moved by
     * the second, so this can move the two ends independently rather than translating the segment.
     * This vector is not changed.
     *
     * @param x1 the amount to add to the start's x coordinate
     * @param y1 the amount to add to the start's y coordinate
     * @param x2 the amount to add to the end's x coordinate
     * @param y2 the amount to add to the end's y coordinate
     * @return a new segment offset endpoint for endpoint
     * @since 1.6.3
     */
    public Vec2D add(double x1, double y1, double x2, double y2) {
        return new Vec2D(this.x1 + x1, this.y1 + y1, this.x2 + x2, this.y2 + y2);
    }

    /**
     * a new segment with each of this one's endpoint coordinates multiplied by the matching
     * endpoint coordinate of another one. This is a componentwise product, not a scale, so unless
     * the other segment starts at the origin it will not simply make this one longer. This vector
     * is not changed.
     *
     * @param vec the segment whose endpoint coordinates are the factors
     * @return a new segment with {@code x1 * vec.x1, y1 * vec.y1, x2 * vec.x2, y2 * vec.y2}
     * @since 1.2.6
     */
    public Vec2D multiply(Vec2D vec) {
        return new Vec2D(x1 * vec.x1, y1 * vec.y1, x2 * vec.x2, y2 * vec.y2);
    }

    /**
     * a new segment with each of this one's endpoint coordinates multiplied by the matching factor
     * given here, so again the two ends can be scaled independently. To scale the whole segment by
     * one factor, use {@link #scale(double)} instead of passing the same number four times. This
     * vector is not changed.
     *
     * @param x1 the factor for the start's x coordinate
     * @param y1 the factor for the start's y coordinate
     * @param x2 the factor for the end's x coordinate
     * @param y2 the factor for the end's y coordinate
     * @return a new segment with each endpoint coordinate multiplied by its factor
     * @since 1.6.3
     */
    public Vec2D multiply(double x1, double y1, double x2, double y2) {
        return new Vec2D(this.x1 * x1, this.y1 * y1, this.x2 * x2, this.y2 * y2);
    }

    /**
     * a new segment with all four endpoint coordinates multiplied by one factor, which resizes the
     * segment about the origin rather than changing only its length. To keep the start where it
     * is and only change the length, move just the end with
     * {@link #add(double, double, double, double) add()}, passing zeros for the start's two
     * amounts. {@link #normalize() normalize()} is not that: it divides the start as well as the
     * end, so the start slides towards the origin. This vector is not changed.
     *
     * @param scale the factor to multiply all four endpoint coordinates by
     * @return a new segment with all four endpoint coordinates scaled
     * @since 1.6.3
     */
    public Vec2D scale(double scale) {
        return new Vec2D(x1 * scale, y1 * scale, x2 * scale, y2 * scale);
    }

    /**
     * how much two segments point the same way, the sum of the products of their matching
     * coordinate differences. The sign is the useful part: positive when the two directions are
     * less than a right angle apart, {@code 0} when they are exactly a right angle apart or one of
     * them has no length, and negative when they are more than a right angle apart. The magnitude
     * is not an angle, it is a length squared scaled by how far apart the directions are.<br>
     * The result is not normalized, so it grows with the length of both segments.<br>
     * {@link Vec3D} overrides this to include the z difference.
     *
     * @param vec the segment to compare this one's direction against
     * @return the dot product of the two direction differences
     * @since 1.2.6
     */
    public double dotProduct(Vec2D vec) {
        double dx1 = x2 - x1;
        double dx2 = vec.x2 - vec.x1;
        double dy1 = y2 - y1;
        double dy2 = vec.y2 - vec.y1;
        return dx1 * dx2 + dy1 * dy2;
    }

    /**
     * a new segment running the other way, with the ends swapped. Its direction is the opposite of
     * this one's and its length is the same. This vector is not changed.
     *
     * @return a new segment from the end of this one to its start
     * @since 1.2.6
     */
    public Vec2D reverse() {
        return new Vec2D(x2, y2, x1, y1);
    }

    /**
     * a new segment that points the same way as this one but is one long. What that means in
     * practice is that every endpoint coordinate, start included, is divided by this segment's
     * length, so the difference between the two ends comes out one long while the start slides
     * towards the origin. For a segment that already starts at {@code 0, 0} that is exactly a unit
     * direction; for one that does not, the start is moved as well and the result is a segment of
     * length one running from a point near the origin rather than from where this one started.<br>
     * A segment whose ends are the same point has length {@code 0}, and every coordinate of the
     * result is then {@code NaN} rather than an error.
     * <br>
     * {@link Vec3D} overrides this to include the z coordinates.
     *
     * @return a new segment with the same direction but a length of 1
     * @since 1.6.5
     */
    public Vec2D normalize() {
        double mag = getMagnitude();
        return new Vec2D(x1 / mag, y1 / mag, x2 / mag, y2 / mag);
    }

    public String toString() {
        return String.format("%f, %f -> %f, %f", x1, y1, x2, y2);
    }

    /**
     * a new three dimensional segment through the same two points, with both z coordinates at
     * {@code 0}. The result is a {@link Vec3D}, so its length and its other measurements are taken
     * in three dimensions, and they match this segment's since z is zero throughout.<br>
     * Note that {@link Vec3D} inherits this rather than overriding it, so calling it on a
     * {@code Vec3D} reads only the four flat coordinates and quietly discards both z endpoints,
     * handing back a segment on the z {@code 0} plane. A {@code Vec3D} is already what this
     * returns, so there is nothing to gain by asking for it.
     *
     * @return a new {@link Vec3D} through the same two points, flat on the z {@code 0} plane
     * @since 1.2.6
     */
    public Vec3D to3D() {
        return new Vec3D(x1, y1, 0, x2, y2, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vec2D vec2D = (Vec2D) o;
        return Double.compare(x1, vec2D.x1) == 0
                && Double.compare(y1, vec2D.y1) == 0
                && Double.compare(x2, vec2D.x2) == 0
                && Double.compare(y2, vec2D.y2) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x1, y1, x2, y2);
    }

    /**
     * orders this segment against another one, first by where the start sits and then, only if the
     * two starts compare equal, by where the end sits. The ordering of each end is
     * {@link Pos2D#compareTo(Pos2D)}, that is by x and then y, and it is a raw comparison on the
     * coordinates rather than a distance, so two segments that cross can still order against each
     * other.
     *
     * @param other the segment to compare this one against
     * @return a negative number, zero, or a positive number as this segment is before, the same
     *         as, or after {@code other}
     * @since 1.2.6
     */
    public int compareTo(Vec2D other) {
        int i = getStart().compareTo(other.getStart());
        if (i == 0) {
            i = getEnd().compareTo(other.getEnd());
        }
        return i;
    }

}
