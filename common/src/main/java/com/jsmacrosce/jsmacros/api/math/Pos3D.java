package com.jsmacrosce.jsmacros.api.math;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A mutable position represented by three {@code double} coordinates. Arithmetic methods return
 * new positions without changing the receiver. Inherited two-coordinate arithmetic methods return
 * {@link Pos2D} and omit z; use the three-coordinate overloads to retain all coordinates. The
 * {@link #toVector(Pos2D)} and {@link #toReverseVector(Pos2D)} overloads treat the 2D argument's z
 * coordinate as {@code 0}.
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
     * Mutable origin used by no-argument vector conversions; changes affect later conversions.
     */
    public static final Pos3D ZERO = new Pos3D(0, 0, 0);
    /**
     * The mutable z coordinate.
     */
    public double z;

    /**
     * Copies the coordinates from a vanilla three-component vector.
     *
     * @param vec the vector to copy the coordinates out of
     * @since 1.2.6
     */
    public Pos3D(Vec3 vec) {
        this(vec.x(), vec.y(), vec.z());
    }

    /**
     * Copies another position into a separate object.
     *
     * @param pos the position to copy
     * @since 1.2.6
     */
    public Pos3D(Pos3D pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Creates a position with the given coordinates.
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
     * Returns the current z coordinate.
     *
     * @return the z coordinate
     * @since 1.2.6
     */
    public double getZ() {
        return z;
    }

    /**
     * Returns a new position with the corresponding coordinates added; this position is unchanged.
     *
     * @param pos the position to add to this one
     * @return a new position at {@code x + pos.x, y + pos.y, z + pos.z}
     * @since 1.2.6
     */
    public Pos3D add(Pos3D pos) {
        return new Pos3D(x + pos.x, y + pos.y, z + pos.z);
    }

    /**
     * Returns a new position with the given amounts added to its coordinates; this position is unchanged.
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
     * Returns a new position with the corresponding coordinates of {@code pos} subtracted; this position is unchanged.
     *
     * @param pos the position to subtract
     * @return a new position at {@code x - pos.x, y - pos.y, z - pos.z}.
     * @since 1.8.4
     */
    public Pos3D sub(Pos3D pos) {
        return new Pos3D(x - pos.x, y - pos.y, z - pos.z);
    }

    /**
     * Returns a new position with the given amounts subtracted coordinate by coordinate.
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
     * Returns a new position with each coordinate multiplied by the corresponding coordinate of
     * {@code pos}.<br>
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
     * Returns a new position with each coordinate multiplied by its corresponding factor.
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
     * Returns a new position with each coordinate divided by the corresponding coordinate of
     * {@code pos}. Division follows Java {@code double} semantics, including division by zero.<br>
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
     * Returns a new position with each coordinate divided by its corresponding divisor. Division
     * follows Java {@code double} semantics, including division by zero.
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
     * Returns a new position with all three coordinates multiplied by the same factor.
     *
     * @param scale the factor to multiply all three coordinates by
     * @return a new position at {@code this.x * scale, this.y * scale, this.z * scale}
     * @since 1.6.3
     */
    @Override
    public Pos3D scale(double scale) {
        return new Pos3D(x * scale, y * scale, z * scale);
    }

    /**
     * Formats the coordinates in x, y, z order; finite coordinates have six fractional digits.
     * The decimal separator follows the default formatting locale; this is display text, not a stable serialization.
     *
     * @return the formatted coordinates
     */
    public String toString() {
        return String.format("%f, %f, %f", x, y, z);
    }

    /**
     * Returns a vector from {@link #ZERO} to this position. The shared {@code ZERO} object is
     * mutable, so changing it affects subsequent calls.
     *
     * @return a new {@link Vec3D} from {@code 0, 0, 0} to this position
     * @since 1.2.6
     */
    @Override
    public Vec3D toVector() {
        return new Vec3D(ZERO, this);
    }

    /**
     * Returns a vector from the given 2D position, lifted to z {@code 0}, to this position.
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
     * Returns a vector from the given position to this position.
     *
     * @param start_pos the position the vector starts at
     * @return a new {@link Vec3D} from {@code start_pos} to this position
     * @since 1.6.4
     */
    public Vec3D toVector(Pos3D start_pos) {
        return new Vec3D(start_pos, this);
    }

    /**
     * Returns a vector from the given coordinates to this position.
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
     * Returns a new vector from this position to the origin, opposite to {@link #toVector()}.
     *
     * @return a new {@link Vec3D} from this position to {@code 0, 0, 0}
     * @since 1.6.4
     */
    public Vec3D toReverseVector() {
        return new Vec3D(this, ZERO);
    }

    /**
     * Returns a vector from this position (including its own z) to the given 2D position lifted to z {@code 0}.
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
     * Returns a vector from this position to the given 3D position; this position is the start.
     *
     * @param end_pos the position the vector ends at
     * @return a new {@link Vec3D} from this position to {@code end_pos}
     * @since 1.6.4
     */
    public Vec3D toReverseVector(Pos3D end_pos) {
        return new Vec3D(this, end_pos);
    }

    /**
     * Returns a vector from this position to the given coordinates; this position is the start.
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
     * Returns the raw block position containing this position, flooring each coordinate.
     *
     * @return a new raw block position of the block containing this position
     * @since 1.8.0
     */
    public BlockPos toRawBlockPos() {
        return new BlockPos((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
    }

    /**
     * Returns a raw Minecraft vector with the same coordinates as this position.
     *
     * @return a new raw vanilla vector at {@code x, y, z}
     * @since 1.8.4
     */
    public Vec3 toMojangDoubleVector() {
        return new Vec3(x, y, z);
    }

    /**
     * Compares this position with another object using exact runtime-class and coordinate equality.
     * Each coordinate is compared with {@link Double#compare(double, double)}: NaN values compare equal,
     * while positive and negative zero compare differently.
     *
     * @param o the object to compare with this position
     * @return {@code true} if {@code o} is this object or has the same runtime class and equal coordinates; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pos3D pos3D = (Pos3D) o;
        return Double.compare(x, pos3D.x) == 0 && Double.compare(y, pos3D.y) == 0 && Double.compare(z, pos3D.z) == 0;
    }

    /**
     * Computes a hash from the current x, y, and z coordinates. Because this position is mutable,
     * its hash can change when its coordinates change; avoid using it as a hash key while mutating it.
     *
     * @return the hash code for the current coordinates
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), z);
    }

    /**
     * Compares positions lexicographically by x, y, then z using {@link Double#compare(double, double)}.
     * This overload compares z only when the argument is typed as {@code Pos3D}; a {@code Pos2D}
     * argument selects the inherited comparison, which ignores z.
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
