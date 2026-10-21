package com.jsmacrosce.jsmacros.api.math;

import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A mutable pair of {@code double} coordinates. Arithmetic methods return new positions without
 * changing the receiver. Because {@link Pos3D} extends this class, calling an inherited two-argument
 * arithmetic method on a {@code Pos3D} returns a {@code Pos2D} and drops z; use the three-argument
 * {@link Pos3D} methods to retain all coordinates.
 * <pre>
 * const start = PositionCommon.createPos(0, 0);
 * const end = PositionCommon.createPos(3, 4);
 *
 * // Arithmetic returns a new position; start remains unchanged.
 * const offset = end.sub(start);                            // 3, 4
 * const doubled = end.scale(2);                             // 6, 8
 * const length = end.toVector(start).getMagnitude();        // 5
 * Chat.log(`offset ${offset}, doubled ${doubled}, length ${length}`);
 * </pre>
 * @author Wagyourtail
 * @since 1.2.6
 */
public class Pos2D {
    /**
     * Mutable origin used by the no-argument vector conversions; changing it affects later conversions.
     */
    public static final Pos2D ZERO = new Pos2D(0, 0);
    /**
     * The mutable x coordinate.
     */
    public double x;
    /**
     * The mutable y coordinate.
     */
    public double y;

    /**
     * Copies the coordinates from a vanilla two-component vector.
     *
     * @param vec the vector to copy the coordinates out of
     * @since 1.2.6
     */
    public Pos2D(Vec2 vec) {
        this(vec.x, vec.y);
    }

    /**
     * Copies another position into a separate object.
     *
     * @param pos the position to copy
     * @since 1.2.6
     */
    public Pos2D(Pos2D pos) {
        this(pos.getX(), pos.getY());
    }

    /**
     * Creates a position with the given coordinates.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @since 1.2.6
     */
    public Pos2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the current x coordinate.
     * @return the current x coordinate
     * @since 1.2.6
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the current y coordinate.
     * @return the current y coordinate
     * @since 1.2.6
     */
    public double getY() {
        return y;
    }

    /**
     * Returns a new position with the corresponding coordinates added; this position is unchanged.
     *
     * @param pos the position to add to this one
     * @return the coordinate-wise sum
     * @since 1.2.6
     */
    public Pos2D add(Pos2D pos) {
        return new Pos2D(x + pos.x, y + pos.y);
    }

    /**
     * Returns a new position with the given amounts added to its coordinates; this position is unchanged.
     *
     * @param x the amount to add to the x coordinate
     * @param y the amount to add to the y coordinate
     * @return a new position at {@code this.x + x, this.y + y}
     * @since 1.6.3
     */
    public Pos2D add(double x, double y) {
        return new Pos2D(this.x + x, this.y + y);
    }

    /**
     * Returns a new position with the corresponding coordinates of {@code pos} subtracted; this position is unchanged.
     *
     * @param pos the position to subtract
     * @return a new position at {@code x - pos.x, y - pos.y}.
     * @since 1.8.4
     */
    public Pos2D sub(Pos2D pos) {
        return new Pos2D(x - pos.x, y - pos.y);
    }

    /**
     * Returns a new position with the given amounts subtracted from its coordinates; this position is unchanged.
     *
     * @param x the x coordinate to subtract
     * @param y the y coordinate to subtract
     * @return a new position at {@code this.x - x, this.y - y}.
     * @since 1.8.4
     */
    public Pos2D sub(double x, double y) {
        return new Pos2D(this.x - x, this.y - y);
    }

    /**
     * Returns a new position with each coordinate multiplied by the corresponding coordinate of {@code pos}; this position is unchanged.
     *
     * @param pos the position to multiply by, coordinate for coordinate
     * @return a new position at {@code x * pos.x, y * pos.y}
     * @since 1.2.6
     */
    public Pos2D multiply(Pos2D pos) {
        return new Pos2D(x * pos.x, y * pos.y);
    }

    /**
     * Returns a new position with its x and y coordinates multiplied by the respective factors; this position is unchanged. For one factor on both coordinates, use {@link #scale(double)}.
     *
     * @param x the factor for the x coordinate
     * @param y the factor for the y coordinate
     * @return a new position at {@code this.x * x, this.y * y}
     * @since 1.6.3
     */
    public Pos2D multiply(double x, double y) {
        return new Pos2D(this.x * x, this.y * y);
    }

    /**
     * Returns a new position with each coordinate divided by the corresponding coordinate of {@code pos}; this position is unchanged. Division by zero follows Java floating-point rules.
     *
     * @param pos the position to divide by, coordinate for coordinate
     * @return a new position at {@code x / pos.x, y / pos.y}.
     * @since 1.8.4
     */
    public Pos2D divide(Pos2D pos) {
        return new Pos2D(x / pos.x, y / pos.y);
    }

    /**
     * Returns a new position with its coordinates divided by the respective divisors; this position is unchanged. Division by zero follows Java floating-point rules.
     *
     * @param x the x coordinate to divide by
     * @param y the y coordinate to divide by
     * @return a new position at {@code this.x / x, this.y / y}.
     * @since 1.8.4
     */
    public Pos2D divide(double x, double y) {
        return new Pos2D(this.x / x, this.y / y);
    }

    /**
     * Returns a new position with both coordinates multiplied by {@code scale}; this position is unchanged.
     *
     * @param scale the factor to multiply both coordinates by
     * @return a new position at {@code this.x * scale, this.y * scale}
     * @since 1.6.3
     */
    public Pos2D scale(double scale) {
        return new Pos2D(x * scale, y * scale);
    }

    /**
     * Formats the coordinates as {@code x, y}; finite coordinates have six fractional digits.
     * Formatting uses the default format locale, so the decimal separator may vary.
     * @return the formatted coordinates
     */
    public String toString() {
        return String.format("%f, %f", x, y);
    }

    /**
     * Returns a new {@link Pos3D} with the same x and y coordinates and z set to {@code 0}.
     *
     * @return a new {@link Pos3D} at {@code x, y, 0}
     * @since 1.2.6
     */
    public Pos3D to3D() {
        return new Pos3D(x, y, 0);
    }

    /**
     * Returns a new vector from {@code (0, 0)} to this position. Use {@link #toReverseVector()} for the opposite direction.
     *
     * @return a new {@link Vec2D} from {@code 0, 0} to this position
     * @since 1.2.6
     */
    public Vec2D toVector() {
        return new Vec2D(ZERO, this);
    }

    /**
     * Returns a vector from {@code start_pos} to this position.
     *
     * @param start_pos the position the vector starts at
     * @return a new {@link Vec2D} from {@code start_pos} to this position
     * @since 1.6.4
     */
    public Vec2D toVector(Pos2D start_pos) {
        return new Vec2D(start_pos, this);
    }

    /**
     * Returns a vector from the given coordinates to this position.
     *
     * @param start_x the x coordinate the vector starts at
     * @param start_y the y coordinate the vector starts at
     * @return a new {@link Vec2D} from {@code start_x, start_y} to this position
     * @since 1.6.4
     */
    public Vec2D toVector(double start_x, double start_y) {
        return new Vec2D(start_x, start_y, this.x, this.y);
    }

    /**
     * Returns a vector from this position to the origin, opposite to {@link #toVector()}.
     *
     * @return a new {@link Vec2D} from this position to {@code 0, 0}
     * @since 1.6.4
     */
    public Vec2D toReverseVector() {
        return new Vec2D(this, ZERO);
    }

    /**
     * Returns a vector from this position to {@code end_pos}; this is the opposite direction of
     * {@link #toVector(Pos2D)} with the same argument.
     *
     * @param end_pos the position the vector ends at
     * @return a new {@link Vec2D} from this position to {@code end_pos}
     * @since 1.6.4
     */
    public Vec2D toReverseVector(Pos2D end_pos) {
        return new Vec2D(this, end_pos);
    }

    /**
     * Returns a vector from this position to the given coordinates, opposite to
     * {@link #toVector(double, double)} with the same coordinates.
     *
     * @param end_x the x coordinate the vector ends at
     * @param end_y the y coordinate the vector ends at
     * @return a new {@link Vec2D} from this position to {@code end_x, end_y}
     * @since 1.6.4
     */
    public Vec2D toReverseVector(double end_x, double end_y) {
        return new Vec2D(this, new Pos2D(end_x, end_y));
    }

    /**
     * Returns whether {@code o} is a {@code Pos2D} of the exact same runtime class with matching coordinates.
     * {@code null} and instances of other classes return {@code false}; {@link Double#compare(double, double)}
     * treats NaNs as equal and distinguishes positive and negative zero. Thus a {@code Pos2D} does not equal a {@link Pos3D}.
     * @param o the object to compare
     * @return {@code true} if the runtime classes and both coordinates match; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pos2D pos2D = (Pos2D) o;
        return Double.compare(x, pos2D.x) == 0 && Double.compare(y, pos2D.y) == 0;
    }

    /**
     * Computes a hash code from the current x and y coordinates. Because coordinates are mutable, the result can change when they do; avoid using a mutable position as a hash key.
     * @return the hash code for the current coordinates
     */
    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    /**
     * Compares x first, then y using {@link Double#compare(double, double)}. When a {@code Pos3D}
     * is typed as {@code Pos2D}, this overload compares only x and y; use the
     * {@link Pos3D#compareTo(Pos3D)} overload with {@code Pos3D} arguments to include z.
     *
     * @param o the position to compare this one against
     * @return a negative number, zero, or a positive number as this position is before, the same
     *         as, or after {@code o}
     * @since 1.2.6
     */
    public int compareTo(@NotNull Pos2D o) {
        int i = Double.compare(x, o.x);
        if (i == 0) {
            i = Double.compare(y, o.y);
        }
        return i;
    }
}
