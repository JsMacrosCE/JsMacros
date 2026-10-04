package com.jsmacrosce.jsmacros.api.math;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A point in the world, held as three plain {@code double} coordinates. {@code x} and {@code y}
 * are inherited from {@link Pos2D} and are public writable fields just like its own
 * {@code z}, so a position can be read off and nudged directly instead of only through the
 * accessors.<br>
 * Every arithmetic member here builds and returns a brand new position and leaves the one it was
 * called on exactly as it was, so they are safe to chain. The three-argument forms keep the result
 * three dimensional, the inherited two-argument ones do not: {@link Pos2D#add(double, double) add},
 * {@link Pos2D#sub(double, double) sub}, {@link Pos2D#multiply(double, double) multiply} and
 * {@link Pos2D#divide(double, double) divide} are still the {@link Pos2D} versions and still hand
 * back a {@code Pos2D}, silently dropping z. The same is true of
 * {@link #toVector(Pos2D) toVector(Pos2D)} and
 * {@link #toReverseVector(Pos2D) toReverseVector(Pos2D)}, which
 * lift the {@code Pos2D} they are given with {@link Pos2D#to3D()} and therefore start or end it at
 * z {@code 0}.<br>
 * Positions normally arrive from other parts of the API rather than being built by hand, and
 * {@link com.jsmacrosce.jsmacros.client.api.library.impl.FPositionCommon the PositionCommon library}
 * is the in-scope factory for new ones.
 * example:
 * <pre>
 * // Player.getPlayer() is null outside a world, so there is nothing to measure there
 * const player = Player.getPlayer();
 * if (player !== null) {
 *   const feet = player.getPos();
 *   const eyes = player.getEyePos();
 *   // one block east, one block up and two south, along the world axes rather than
 *   // in front of wherever the player happens to be looking
 *   const target = feet.add(1, 1, 2);
 *   // the length of the segment from the eyes down to the feet, in blocks
 *   const eyeHeight = eyes.toVector(feet).getMagnitude();
 *   Chat.log(`that lands on ${target}, and the eyes sit ${eyeHeight} up`);
 * }
 * </pre>
 * @author Wagyourtail
 * @since 1.2.6
 */
public class Pos3D extends Pos2D {
    /**
     * a shared position at {@code 0, 0, 0}, the origin the no-argument vector conversions below
     * start from. It is a separate constant from {@link Pos2D#ZERO}, so writing to one of them
     * leaves the other alone.<br>
     * It is a plain constant rather than a frozen one, so it can be written to like any other
     * position, and doing so changes the start of every later vector built from a position that
     * relies on it.
     */
    public static final Pos3D ZERO = new Pos3D(0, 0, 0);
    /**
     * the z coordinate, in whatever unit the position came from. Public and writable, so assigning
     * to it moves the position without going through any of the arithmetic below.
     */
    public double z;

    /**
     * copies a vanilla three component vector into a position.
     *
     * @param vec the vector to copy the coordinates out of
     * @since 1.2.6
     */
    public Pos3D(Vec3 vec) {
        this(vec.x(), vec.y(), vec.z());
    }

    /**
     * copies another position. The result is a separate object, so writing to it afterwards does
     * not touch {@code pos}.
     *
     * @param pos the position to copy
     * @since 1.2.6
     */
    public Pos3D(Pos3D pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * makes a position from raw coordinates.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @since 1.2.6
     */
    public Pos3D(double x, double y, double z) {
        super(x, y);
        this.z = z;
    }

    /**
     * the z coordinate, the same value as the {@code z} field. There is no reason to prefer one
     * over the other, both read the same live field.
     *
     * @return the z coordinate
     * @since 1.2.6
     */
    public double getZ() {
        return z;
    }

    /**
     * a new position offset from this one by another three dimensional position, that is with each
     * coordinate added separately. This position is not changed.<br>
     * Passing a plain {@link Pos2D} instead selects the inherited
     * {@link Pos2D#add(Pos2D) add(Pos2D)} and hands back a
     * {@code Pos2D} with the z coordinate gone.
     *
     * @param pos the position to add to this one
     * @return a new position at {@code x + pos.x, y + pos.y, z + pos.z}
     * @since 1.2.6
     */
    public Pos3D add(Pos3D pos) {
        return new Pos3D(x + pos.x, y + pos.y, z + pos.z);
    }

    /**
     * a new position offset from this one by the given amounts. This position is not changed.
     *
     * @param x the amount to add to the x coordinate
     * @param y the amount to add to the y coordinate
     * @param z the amount to add to the z coordinate
     * @return a new position at {@code this.x + x, this.y + y, this.z + z}
     * @since 1.6.3
     */
    public Pos3D add(double x, double y, double z) {
        return new Pos3D(this.x + x, this.y + y, this.z + z);
    }

    /**
     * a new position that is this one moved back by another three dimensional position, that is
     * with each coordinate subtracted separately. This position is not changed.<br>
     * Passing a plain {@link Pos2D} instead selects the inherited
     * {@link Pos2D#sub(Pos2D) sub(Pos2D)} and hands back a
     * {@code Pos2D} with the z coordinate gone.
     *
     * @param pos the position to subtract
     * @return a new position at {@code x - pos.x, y - pos.y, z - pos.z}.
     * @since 1.8.4
     */
    public Pos3D sub(Pos3D pos) {
        return new Pos3D(x - pos.x, y - pos.y, z - pos.z);
    }

    /**
     * a new position that is this one moved back by the given amounts. This position is not
     * changed.
     *
     * @param x the x coordinate to subtract
     * @param y the y coordinate to subtract
     * @param z the z coordinate to subtract
     * @return a new position at {@code this.x - x, this.y - y, this.z - z}.
     * @since 1.8.4
     */
    public Pos3D sub(double x, double y, double z) {
        return new Pos3D(this.x - x, this.y - y, this.z - z);
    }

    /**
     * a new position with each of this position's coordinates multiplied by the matching coordinate
     * of another three dimensional position. This is a componentwise product, not a scale, so a
     * position that straddles the origin does not simply get bigger. This position is not
     * changed.<br>
     * Passing a plain {@link Pos2D} instead selects the inherited
     * {@link Pos2D#multiply(Pos2D) multiply(Pos2D)} and hands
     * back a {@code Pos2D} with the z coordinate gone.
     *
     * @param pos the position to multiply by, coordinate for coordinate
     * @return a new position at {@code x * pos.x, y * pos.y, z * pos.z}
     * @since 1.2.6
     */
    public Pos3D multiply(Pos3D pos) {
        return new Pos3D(x * pos.x, y * pos.y, z * pos.z);
    }

    /**
     * a new position with each of this position's coordinates multiplied by the matching factor
     * given here. To scale a position about the origin by one factor, use {@link #scale(double)}
     * instead of passing the same number three times. This position is not changed.
     *
     * @param x the factor for the x coordinate
     * @param y the factor for the y coordinate
     * @param z the factor for the z coordinate
     * @return a new position at {@code this.x * x, this.y * y, this.z * z}
     * @since 1.6.3
     */
    public Pos3D multiply(double x, double y, double z) {
        return new Pos3D(this.x * x, this.y * y, this.z * z);
    }

    /**
     * a new position with each of this position's coordinates divided by the matching coordinate
     * of another three dimensional position. Nothing guards against a zero divisor, so a zero in
     * {@code pos} gives infinity for a non zero coordinate here and {@code NaN} for a zero one.
     * This position is not changed.<br>
     * Passing a plain {@link Pos2D} instead selects the inherited
     * {@link Pos2D#divide(Pos2D) divide(Pos2D)} and hands back a
     * {@code Pos2D} with the z coordinate gone.
     *
     * @param pos the position to divide by, coordinate for coordinate
     * @return a new position at {@code x / pos.x, y / pos.y, z / pos.z}.
     * @since 1.8.4
     */
    public Pos3D divide(Pos3D pos) {
        return new Pos3D(x / pos.x, y / pos.y, z / pos.z);
    }

    /**
     * a new position with each of this position's coordinates divided by the matching divisor
     * given here. Nothing guards against a zero divisor, so dividing by zero gives infinity for a
     * non zero coordinate and {@code NaN} for a zero one. This position is not changed.
     *
     * @param x the x coordinate to divide by
     * @param y the y coordinate to divide by
     * @param z the z coordinate to divide by
     * @return a new position at {@code this.x / x, this.y / y, this.z / z}.
     * @since 1.8.4
     */
    public Pos3D divide(double x, double y, double z) {
        return new Pos3D(this.x / x, this.y / y, this.z / z);
    }

    /**
     * a new position with all three coordinates multiplied by one factor, which scales the
     * position about the origin along the line from the origin to it. This position is not
     * changed.
     *
     * @param scale the factor to multiply all three coordinates by
     * @return a new position at {@code this.x * scale, this.y * scale, this.z * scale}
     * @since 1.6.3
     */
    @Override
    public Pos3D scale(double scale) {
        return new Pos3D(x * scale, y * scale, z * scale);
    }

    public String toString() {
        return String.format("%f, %f, %f", x, y, z);
    }

    /**
     * a new vector from the world origin to this position, so the position becomes the end of the
     * vector and the start is {@code 0, 0, 0}. Use {@link #toReverseVector()} for the other
     * direction.<br>
     * Note that the origin used here is this class's own {@link #ZERO}, not the two dimensional
     * {@link Pos2D#ZERO}.
     *
     * @return a new {@link Vec3D} from {@code 0, 0, 0} to this position
     * @since 1.2.6
     */
    @Override
    public Vec3D toVector() {
        return new Vec3D(ZERO, this);
    }

    /**
     * a new vector from the given two dimensional position to this one, so this position is the end
     * of the vector.<br>
     * The start is lifted to three dimensions with {@link Pos2D#to3D()}, so it is taken at z
     * {@code 0} and this position's own z is not involved at all. Pass a {@link Pos3D} to
     * {@link #toVector(Pos3D) toVector(Pos3D)} when the start's z matters.
     *
     * @param start_pos the position the vector starts at, with its z taken as {@code 0}
     * @return a new {@link Vec3D} from {@code start_pos.to3D()} to this position
     * @since 1.6.4
     */
    @Override
    public Vec3D toVector(Pos2D start_pos) {
        return toVector(start_pos.to3D());
    }

    /**
     * a new vector from the given three dimensional position to this one, so this position is the
     * end of the vector.
     *
     * @param start_pos the position the vector starts at
     * @return a new {@link Vec3D} from {@code start_pos} to this position
     * @since 1.6.4
     */
    public Vec3D toVector(Pos3D start_pos) {
        return new Vec3D(start_pos, this);
    }

    /**
     * a new vector from the given coordinates to this position, so this position is the end of the
     * vector.
     *
     * @param start_x the x coordinate the vector starts at
     * @param start_y the y coordinate the vector starts at
     * @param start_z the z coordinate the vector starts at
     * @return a new {@link Vec3D} from {@code start_x, start_y, start_z} to this position
     * @since 1.6.4
     */
    public Vec3D toVector(double start_x, double start_y, double start_z) {
        return new Vec3D(start_x, start_y, start_z, this.x, this.y, this.z);
    }

    /**
     * a new vector from this position back to the world origin, the opposite direction to
     * {@link #toVector()}.
     *
     * @return a new {@link Vec3D} from this position to {@code 0, 0, 0}
     * @since 1.6.4
     */
    public Vec3D toReverseVector() {
        return new Vec3D(this, ZERO);
    }

    /**
     * a new vector from this position to the given two dimensional one, so this position is the
     * start of the vector.<br>
     * The end is lifted to three dimensions with {@link Pos2D#to3D()}, so it is taken at z
     * {@code 0} and this position's own z is not involved at all. Pass a {@link Pos3D} to
     * {@link #toReverseVector(Pos3D) toReverseVector(Pos3D)} when the end's z matters.
     *
     * @param end_pos the position the vector ends at, with its z taken as {@code 0}
     * @return a new {@link Vec3D} from this position to {@code end_pos.to3D()}
     * @since 1.6.4
     */
    @Override
    public Vec3D toReverseVector(Pos2D end_pos) {
        return toReverseVector(end_pos.to3D());
    }

    /**
     * a new vector from this position to the given three dimensional one, so this position is the
     * start of the vector. Note that the argument is the far end, not the near one, so this is not
     * the reverse of {@link #toVector(Pos3D) toVector(Pos3D)} for the same argument.
     *
     * @param end_pos the position the vector ends at
     * @return a new {@link Vec3D} from this position to {@code end_pos}
     * @since 1.6.4
     */
    public Vec3D toReverseVector(Pos3D end_pos) {
        return new Vec3D(this, end_pos);
    }

    /**
     * a new vector from this position to the given coordinates, so this position is the start of
     * the vector. Note that the arguments are the far end, not the near one, so this is not the
     * reverse of {@link #toVector(double, double, double) toVector(double, double, double)} for the
     * same arguments.
     *
     * @param end_x the x coordinate the vector ends at
     * @param end_y the y coordinate the vector ends at
     * @param end_z the z coordinate the vector ends at
     * @return a new {@link Vec3D} from this position to {@code end_x, end_y, end_z}
     * @since 1.6.4
     */
    public Vec3D toReverseVector(double end_x, double end_y, double end_z) {
        return new Vec3D(this, new Pos3D(end_x, end_y, end_z));
    }

//    /**
//     * @return
//     * @since 1.8.0
//     */
//    public BlockPosHelper toBlockPos() {
//        return new BlockPosHelper(BlockPos.ofFloored(x, y, z));
//    }

    /**
     * the block this position falls in, as the game's own unwrapped block position object rather
     * than a JsMacros wrapper. Each coordinate is floored first, so any position inside the block,
     * including the exact face, lands on that block.<br>
     * What comes back is a raw game object, so it carries none of the JsMacros conveniences, and
     * the way to turn it back into something that can report a block state, render and convert
     * itself to a position again is
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper BlockPosHelper}, whose
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper#toPos3D() toPos3D()}
     * goes the other way. If the position is already known to be a whole block position, building
     * that wrapper directly is simpler than the round trip.
     *
     * @return a new raw block position of the block containing this position
     * @since 1.8.0
     */
    public BlockPos toRawBlockPos() {
        return new BlockPos((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
    }

    /**
     * the game's own three component vector with the same coordinates as this position, as a raw
     * game object rather than a JsMacros wrapper. It is the same three numbers either way, so
     * there is little reason to leave the JsMacros type for this, and no reason to expect the
     * returned object to have anything on it.
     *
     * @return a new raw vanilla vector at {@code x, y, z}
     * @since 1.8.4
     */
    public Vec3 toMojangDoubleVector() {
        return new Vec3(x, y, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pos3D pos3D = (Pos3D) o;
        return Double.compare(x, pos3D.x) == 0 && Double.compare(y, pos3D.y) == 0 && Double.compare(z, pos3D.z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), z);
    }

    /**
     * orders this position against another one, by x, then y, then z, each only looked at when the
     * ones before it compared equal. That makes it a usable sort key for a list of positions,
     * though the comparison is a raw one on the coordinates: it separates {@code 0.0} from
     * {@code -0.0}, and two positions that are only almost equal still order against each other
     * rather than tying.<br>
     * This is an overload rather than an override of
     * {@link Pos2D#compareTo(Pos2D) Pos2D.compareTo(Pos2D)}. Handing it something typed as a
     * plain {@code Pos2D} therefore silently selects the two coordinate version, which leaves z
     * out of the comparison altogether and reports two positions that differ only in z as
     * equal. The z is only compared when the argument is typed as a {@code Pos3D}.
     *
     * @param o the position to compare this one against
     * @return a negative number, zero, or a positive number as this position is before, the same
     *         as, or after {@code o}
     * @since 1.2.6
     */
    public int compareTo(@NotNull Pos3D o) {
        int i = super.compareTo(o);
        if (i == 0) {
            i = Double.compare(z, o.z);
        }
        return i;
    }
}
