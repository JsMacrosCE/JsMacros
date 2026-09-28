package com.jsmacrosce.jsmacros.api.math;

import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A point in the x/y plane, held as two plain {@code double} coordinates that are also public
 * writable fields, so a position can be read off and nudged directly instead of only through
 * {@link #getX()} and {@link #getY()}. They are not validated against each other, so assigning to
 * one of them is enough to move the position.<br>
 * Nothing on this class modifies the position it is called on. Every arithmetic member builds and
 * returns a brand new {@code Pos2D} and leaves this one exactly as it was, which makes them safe
 * to chain and safe to keep a reference to. There is no in-place form of any of them.<br>
 * {@link Pos3D} extends this class, so a three dimensional position is also accepted anywhere a
 * {@code Pos2D} is asked for. What that costs is worth knowing about: the inherited two-argument
 * arithmetic on a {@code Pos3D} is still the version below and still returns a {@code Pos2D}, so it
 * silently throws the z coordinate away. Use the three-argument forms
 * {@link Pos3D#add(double, double, double) add}, {@link Pos3D#sub(double, double, double) sub},
 * {@link Pos3D#multiply(double, double, double) multiply} and
 * {@link Pos3D#divide(double, double, double) divide} on a {@code Pos3D} when the result has to
 * stay three dimensional.<br>
 * Positions normally arrive from other parts of the API rather than being built by hand, and
 * {@link com.jsmacrosce.jsmacros.client.api.library.impl.FPositionCommon the PositionCommon library}
 * is the in-scope factory for new ones.
 * example:
 * <pre>
 * const start = PositionCommon.createPos(0, 0);
 * const end = PositionCommon.createPos(3, 4);
 *
 * // arithmetic runs on a copy, so `start` is still 0, 0 after this
 * const offset = end.sub(start);                            // 3, 4
 * const doubled = end.scale(2);                             // 6, 8
 * const length = end.toVector(start).getMagnitude();        // 5
 * Chat.log(`offset ${offset}, doubled ${doubled}, length ${length}`);
 * </pre>
 * @author Wagyourtail
 * @since 1.2.6 [citation needed]
 */
public class Pos2D {
    /**
     * a shared position at {@code 0, 0}, which is the origin the no-argument vector conversions
     * below start from.<br>
     * It is a plain constant rather than a frozen one, so it can be written to like any other
     * position. Writing to it changes the start of every later vector built from a position that
     * relies on it, so leave it alone.
     */
    public static final Pos2D ZERO = new Pos2D(0, 0);
    /**
     * the x coordinate, in whatever unit the position came from. Public and writable, so assigning
     * to it moves the position without going through any of the arithmetic below.
     */
    public double x;
    /**
     * the y coordinate, in whatever unit the position came from. Public and writable, so assigning
     * to it moves the position without going through any of the arithmetic below.
     */
    public double y;

    /**
     * copies a vanilla two component vector into a position.
     *
     * @param vec the vector to copy the coordinates out of
     * @since 1.2.6
     */
    public Pos2D(Vec2 vec) {
        this(vec.x, vec.y);
    }

    /**
     * copies another position. The result is a separate object, so writing to it afterwards does
     * not touch {@code pos}.
     *
     * @param pos the position to copy
     * @since 1.2.6
     */
    public Pos2D(Pos2D pos) {
        this(pos.getX(), pos.getY());
    }

    /**
     * makes a position from raw coordinates.
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
     * the x coordinate, the same value as the {@code x} field. There is no reason to prefer one
     * over the other, both read the same live field.
     *
     * @return the x coordinate
     * @since 1.2.6
     */
    public double getX() {
        return x;
    }

    /**
     * the y coordinate, the same value as the {@code y} field. There is no reason to prefer one
     * over the other, both read the same live field.
     *
     * @return the y coordinate
     * @since 1.2.6
     */
    public double getY() {
        return y;
    }

    /**
     * a new position offset from this one by another position, that is with each coordinate added
     * separately. This position is not changed.
     *
     * @param pos the position to add to this one
     * @return a new position at {@code x + pos.x, y + pos.y}
     * @since 1.2.6
     */
    public Pos2D add(Pos2D pos) {
        return new Pos2D(x + pos.x, y + pos.y);
    }

    /**
     * a new position offset from this one by the given amounts. This position is not changed.
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
     * a new position that is this one moved back by another position, that is with each coordinate
     * subtracted separately. This position is not changed.
     *
     * @param pos the position to subtract
     * @return a new position at {@code x - pos.x, y - pos.y}.
     * @since 1.8.4
     */
    public Pos2D sub(Pos2D pos) {
        return new Pos2D(x - pos.x, y - pos.y);
    }

    /**
     * a new position that is this one moved back by the given amounts. This position is not
     * changed.
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
     * a new position with each of this position's coordinates multiplied by the matching coordinate
     * of another one. This is a componentwise product, not a scale, so a position that straddles
     * the origin does not simply get bigger. This position is not changed.
     *
     * @param pos the position to multiply by, coordinate for coordinate
     * @return a new position at {@code x * pos.x, y * pos.y}
     * @since 1.2.6
     */
    public Pos2D multiply(Pos2D pos) {
        return new Pos2D(x * pos.x, y * pos.y);
    }

    /**
     * a new position with each of this position's coordinates multiplied by the matching factor
     * given here. To scale a position about the origin by one factor, use
     * {@link #scale(double)} instead of passing the same number twice. This position is not changed.
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
     * a new position with each of this position's coordinates divided by the matching coordinate
     * of another one. Nothing guards against a zero divisor, so a zero in {@code pos} gives
     * infinity for a non zero coordinate here and {@code NaN} for a zero one. This position is not
     * changed.
     *
     * @param pos the position to divide by, coordinate for coordinate
     * @return a new position at {@code x / pos.x, y / pos.y}.
     * @since 1.8.4
     */
    public Pos2D divide(Pos2D pos) {
        return new Pos2D(x / pos.x, y / pos.y);
    }

    /**
     * a new position with each of this position's coordinates divided by the matching divisor given
     * here. Nothing guards against a zero divisor, so dividing by zero gives infinity for a non
     * zero coordinate and {@code NaN} for a zero one. This position is not changed.
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
     * a new position with both coordinates multiplied by one factor, which scales the position
     * about the origin along the line from the origin to it. This position is not changed.
     *
     * @param scale the factor to multiply both coordinates by
     * @return a new position at {@code this.x * scale, this.y * scale}
     * @since 1.6.3
     */
    public Pos2D scale(double scale) {
        return new Pos2D(x * scale, y * scale);
    }

    public String toString() {
        return String.format("%f, %f", x, y);
    }

    /**
     * a new three dimensional position at the same x and y, with z at {@code 0}. A {@code Pos3D}
     * covers a different space in the game, so this only makes sense for a position that is being
     * used as a plain pair of numbers.
     *
     * @return a new {@link Pos3D} at {@code x, y, 0}
     * @since 1.2.6
     */
    public Pos3D to3D() {
        return new Pos3D(x, y, 0);
    }

    /**
     * a new vector from the origin to this position, so the position becomes the end of the vector
     * and the start is {@code 0, 0}. Use {@link #toReverseVector()} for the other direction.
     *
     * @return a new {@link Vec2D} from {@code 0, 0} to this position
     * @since 1.2.6
     */
    public Vec2D toVector() {
        return new Vec2D(ZERO, this);
    }

    /**
     * a new vector from the given position to this one, so this position is the end of the vector.
     *
     * @param start_pos the position the vector starts at
     * @return a new {@link Vec2D} from {@code start_pos} to this position
     * @since 1.6.4
     */
    public Vec2D toVector(Pos2D start_pos) {
        return new Vec2D(start_pos, this);
    }

    /**
     * a new vector from the given coordinates to this position, so this position is the end of the
     * vector.
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
     * a new vector from this position back to the origin, the opposite direction to
     * {@link #toVector()}.
     *
     * @return a new {@link Vec2D} from this position to {@code 0, 0}
     * @since 1.6.4
     */
    public Vec2D toReverseVector() {
        return new Vec2D(this, ZERO);
    }

    /**
     * a new vector from this position to the given one, so this position is the start of the
     * vector. Note that the argument is the far end, not the near one, so this is not the reverse
     * of {@link #toVector(Pos2D) toVector()} for the same argument.
     *
     * @param end_pos the position the vector ends at
     * @return a new {@link Vec2D} from this position to {@code end_pos}
     * @since 1.6.4
     */
    public Vec2D toReverseVector(Pos2D end_pos) {
        return new Vec2D(this, end_pos);
    }

    /**
     * a new vector from this position to the given coordinates, so this position is the start of
     * the vector. Note that the arguments are the far end, not the near one, so this is not the
     * reverse of {@link #toVector(double, double) toVector()} for the same arguments.
     *
     * @param end_x the x coordinate the vector ends at
     * @param end_y the y coordinate the vector ends at
     * @return a new {@link Vec2D} from this position to {@code end_x, end_y}
     * @since 1.6.4
     */
    public Vec2D toReverseVector(double end_x, double end_y) {
        return new Vec2D(this, new Pos2D(end_x, end_y));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pos2D pos2D = (Pos2D) o;
        return Double.compare(x, pos2D.x) == 0 && Double.compare(y, pos2D.y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    /**
     * orders this position against another one, first by x and then, only if the two x coordinates
     * compare equal, by y. That makes it a usable sort key for a list of positions, though the
     * comparison is a raw one on the coordinates: it separates {@code 0.0} from {@code -0.0}, and
     * two positions that are only almost equal still order against each other rather than tying.<br>
     * {@link Pos3D#compareTo(Pos3D) Pos3D.compareTo(Pos3D)} is an overload of this rather than an
     * override, so a {@code Pos3D} argument is a choice of which method runs and not a sign that
     * three coordinates are compared: anything typed as a {@code Pos2D} on either side lands here
     * and gets an x and y comparison, which reports two positions that differ only in z as equal.
     * A {@code Pos3D} on both sides selects the three coordinate version instead.
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
