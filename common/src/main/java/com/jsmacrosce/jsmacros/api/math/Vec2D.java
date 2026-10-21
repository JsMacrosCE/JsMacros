package com.jsmacrosce.jsmacros.api.math;

import java.util.Objects;

/**
 * A 2D line segment with mutable start and end coordinates. Measurements use the displacement
 * from start to end; arithmetic methods return new segments. {@link #normalize()} divides both
 * endpoints by the segment length, so the start moves unless it is at the origin.
 * <pre>
 * const a = PositionCommon.createPos(0, 0);
 * const b = PositionCommon.createPos(3, 4);
 * const segment = b.toVector(a);
 * // 3, 4 for the segment across the plane and 5 for its length
 * Chat.log(`delta ${segment.getDeltaX()}, ${segment.getDeltaY()}, length ${segment.getMagnitude()}`);
 * // The displacement has unit length; both endpoints are divided by the length.
 * const direction = segment.normalize();
 * Chat.log(`normalized to ${direction}`);
 * </pre>
 * @author Wagyourtail
 * @since 1.2.6
 */
public class Vec2D {
    /**
     * The x coordinate of the segment's start.
     */
    public double x1;
    /**
     * The y coordinate of the segment's start.
     */
    public double y1;
    /**
     * The x coordinate of the segment's end.
     */
    public double x2;
    /**
     * The y coordinate of the segment's end.
     */
    public double y2;

    /**
     * Constructs a segment from its start and end coordinates.
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
     * Constructs a segment from two positions, copying their coordinates.
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
     * Copies the endpoint coordinates of another segment.
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
     * Returns the x coordinate of the segment's start.
     *
     * @return the x coordinate of the start
     * @since 1.2.6
     */
    public double getX1() {
        return x1;
    }

    /**
     * Returns the y coordinate of the segment's start.
     *
     * @return the y coordinate of the start
     * @since 1.2.6
     */
    public double getY1() {
        return y1;
    }

    /**
     * Returns the x coordinate of the segment's end.
     *
     * @return the x coordinate of the end
     * @since 1.2.6
     */
    public double getX2() {
        return x2;
    }

    /**
     * Returns the y coordinate of the segment's end.
     *
     * @return the y coordinate of the end
     * @since 1.2.6
     */
    public double getY2() {
        return y2;
    }

    /**
     * Returns the x displacement from the segment's start to its end ({@code x2 - x1}).
     *
     * @return the x distance covered by this segment
     * @since 1.2.6
     */
    public double getDeltaX() {
        return x2 - x1;
    }

    /**
     * Returns the y displacement from the segment's start to its end ({@code y2 - y1}).
     *
     * @return the y distance covered by this segment
     * @since 1.2.6
     */
    public double getDeltaY() {
        return y2 - y1;
    }

    /**
     * Returns a copy of the segment's start position; modifying it does not change this segment.
     *
     * @return a new {@link Pos2D} at the start of this segment
     * @since 1.2.6
     */
    public Pos2D getStart() {
        return new Pos2D(x1, y1);
    }

    /**
     * Returns a copy of the segment's end position; modifying it does not change this segment.
     *
     * @return a new {@link Pos2D} at the end of this segment
     * @since 1.2.6
     */
    public Pos2D getEnd() {
        return new Pos2D(x2, y2);
    }

    /**
     * Returns the Euclidean length of the segment, {@code sqrt(dx * dx + dy * dy)} where {@code dx}
     * and {@code dy} are the endpoint displacements.
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
     * Returns the squared Euclidean length of the segment. {@link Vec3D} overrides this to include
     * the z displacement.
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
     * Returns a new segment with each endpoint coordinate increased by the corresponding coordinate
     * of {@code vec}; this segment is unchanged.
     *
     * @param vec the segment whose endpoints are added to this one's
     * @return a new segment with {@code x1 + vec.x1, y1 + vec.y1, x2 + vec.x2, y2 + vec.y2}
     * @since 1.2.6
     */
    public Vec2D add(Vec2D vec) {
        return new Vec2D(x1 + vec.x1, y1 + vec.y1, x2 + vec.x2, y2 + vec.y2);
    }

    /**
     * Returns a new segment with its start and end offset independently by the corresponding amounts;
     * this segment is unchanged.
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
     * Returns a new segment with each endpoint coordinate multiplied by the corresponding
     * coordinate of {@code vec}; this is componentwise multiplication, not a uniform scale.
     *
     * @param vec supplies the four coordinate factors
     * @return a new segment with {@code x1 * vec.x1, y1 * vec.y1, x2 * vec.x2, y2 * vec.y2}
     * @since 1.2.6
     */
    public Vec2D multiply(Vec2D vec) {
        return new Vec2D(x1 * vec.x1, y1 * vec.y1, x2 * vec.x2, y2 * vec.y2);
    }

    /**
     * Returns a new segment with each endpoint coordinate multiplied by its corresponding factor;
     * the four coordinates can be scaled independently.
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
     * Returns a new segment with all four endpoint coordinates multiplied by {@code scale}; this
     * scales the endpoints about the origin and does not preserve the start position.
     *
     * @param scale the factor to multiply all four endpoint coordinates by
     * @return a new segment with all four endpoint coordinates scaled
     * @since 1.6.3
     */
    public Vec2D scale(double scale) {
        return new Vec2D(x1 * scale, y1 * scale, x2 * scale, y2 * scale);
    }

    /**
     * Returns the dot product of this segment's displacement and {@code vec}'s displacement. The
     * result is not normalized, so its magnitude depends on both segment lengths. For a
     * {@link Vec3D}, this {@code Vec2D} overload ignores z; its {@code dotProduct(Vec3D)} overload
     * includes it.
     *
     * @param vec the segment to compare this one's direction against
     * @return the dot product of the endpoint displacements
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
     * Returns a new segment with this segment's endpoints swapped; this segment is unchanged.
     *
     * @return a new segment from the end of this one to its start
     * @since 1.2.6
     */
    public Vec2D reverse() {
        return new Vec2D(x2, y2, x1, y1);
    }

    /**
     * Returns a new segment with every endpoint coordinate divided by this segment's length. The
     * displacement has length 1, but the start is also divided and therefore moves unless it is at
     * the origin. If the segment has zero length, the resulting coordinates are {@code NaN}.
     * {@link Vec3D} overrides this to include z coordinates.
     *
     * @return a new segment with the same direction but a length of 1
     * @since 1.6.5
     */
    public Vec2D normalize() {
        double mag = getMagnitude();
        return new Vec2D(x1 / mag, y1 / mag, x2 / mag, y2 / mag);
    }

    /**
     * Formats the start and end coordinates in x, y order. Finite coordinates have six fractional
     * digits; the decimal separator follows the default formatting locale. This is display text,
     * not a stable serialization.
     *
     * @return the formatted coordinates
     */
    public String toString() {
        return String.format("%f, %f -> %f, %f", x1, y1, x2, y2);
    }

    /**
     * Returns a {@link Vec3D} through the same x-y endpoints with both z coordinates set to
     * {@code 0}. {@link Vec3D} overrides this to preserve its z coordinates.
     *
     * @return a new {@link Vec3D} through the same two points, flat on the z {@code 0} plane
     * @since 1.2.6
     */
    public Vec3D to3D() {
        return new Vec3D(x1, y1, 0, x2, y2, 0);
    }

    /**
     * Compares this segment with another object by exact runtime class and ordered endpoint
     * coordinates. Coordinates use {@link Double#compare(double, double)}: NaN values compare
     * equal, while positive and negative zero compare differently.
     *
     * @param o the object to compare with this segment
     * @return {@code true} if {@code o} is this object or has the same runtime class and equal
     *         coordinates; {@code false} otherwise
     */
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

    /**
     * Computes a hash from the current ordered endpoint coordinates. Because this segment is
     * mutable, its hash can change when its coordinates change; avoid using it as a hash key while
     * mutating it.
     *
     * @return the hash code for the current coordinates
     */
    @Override
    public int hashCode() {
        return Objects.hash(x1, y1, x2, y2);
    }

    /**
     * Compares segments by start position, then by end position when their starts compare equal;
     * each position is ordered by {@link Pos2D#compareTo(Pos2D)} (x, then y).
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
