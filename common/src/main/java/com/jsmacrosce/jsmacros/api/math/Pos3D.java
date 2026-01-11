package com.jsmacrosce.jsmacros.api.math;

import org.jetbrains.annotations.NotNull;
import com.jsmacrosce.jsmacros.api.coordinate.ICoordinateConverter;

import java.util.Objects;

/**
 * @author Wagyourtail
 * @since 1.2.6 [citation needed]
 */
public class Pos3D extends Pos2D {
    public static final Pos3D ZERO = new Pos3D(0, 0, 0);
    public double z;

    /**
     * Creates a Pos3D from a platform-specific vector using the converter
     * @param vec3d native vector object
     * @param converter platform coordinate converter
     * @return converted position
     */
    public static Pos3D fromVec3d(Object vec3d, ICoordinateConverter converter) {
        return converter.convertFromVec3d(vec3d);
    }

    public Pos3D(double x, double y, double z) {
        super(x, y);
        this.z = z;
    }

    public double getZ() {
        return z;
    }

    public Pos3D add(Pos3D pos) {
        return new Pos3D(x + pos.x, y + pos.y, z + pos.z);
    }

    /**
     * @param x
     * @param y
     * @param z
     * @return
     * @since 1.6.3
     */
    public Pos3D add(double x, double y, double z) {
        return new Pos3D(this.x + x, this.y + y, this.z + z);
    }

    /**
     * @param pos the position to subtract
     * @return the new position.
     * @since 1.8.4
     */
    public Pos3D sub(Pos3D pos) {
        return new Pos3D(x - pos.x, y - pos.y, z - pos.z);
    }

    /**
     * @param x the x coordinate to subtract
     * @param y the y coordinate to subtract
     * @param z the z coordinate to subtract
     * @return the new position.
     * @since 1.8.4
     */
    public Pos3D sub(double x, double y, double z) {
        return new Pos3D(this.x - x, this.y - y, this.z - z);
    }

    public Pos3D multiply(Pos3D pos) {
        return new Pos3D(x * pos.x, y * pos.y, z * pos.z);
    }

    /**
     * @param x
     * @param y
     * @param z
     * @return
     * @since 1.6.3
     */
    public Pos3D multiply(double x, double y, double z) {
        return new Pos3D(this.x * x, this.y * y, this.z * z);
    }

    /**
     * @param pos the position to divide by
     * @return the new position.
     * @since 1.8.4
     */
    public Pos3D divide(Pos3D pos) {
        return new Pos3D(x / pos.x, y / pos.y, z / pos.z);
    }

    /**
     * @param x the x coordinate to divide by
     * @param y the y coordinate to divide by
     * @param z the z coordinate to divide by
     * @return the new position.
     * @since 1.8.4
     */
    public Pos3D divide(double x, double y, double z) {
        return new Pos3D(this.x / x, this.y / y, this.z / z);
    }

    /**
     * @param scale
     * @return
     * @since 1.6.3
     */
    @Override
    public Pos3D scale(double scale) {
        return new Pos3D(x * scale, y * scale, z * scale);
    }

    public String toString() {
        return String.format("%f, %f, %f", x, y, z);
    }

    @Override
    public Vec3D toVector() {
        return new Vec3D(ZERO, this);
    }

    /**
     * @param start_pos
     * @return
     * @since 1.6.4
     */
    @Override
    public Vec3D toVector(Pos2D start_pos) {
        return toVector(start_pos.to3D());
    }

    /**
     * @param start_pos
     * @return
     * @since 1.6.4
     */
    public Vec3D toVector(Pos3D start_pos) {
        return new Vec3D(start_pos, this);
    }

    /**
     * @param start_x
     * @param start_y
     * @param start_z
     * @return
     * @since 1.6.4
     */
    public Vec3D toVector(double start_x, double start_y, double start_z) {
        return new Vec3D(start_x, start_y, start_z, this.x, this.y, this.z);
    }

    /**
     * @return
     * @since 1.6.4
     */
    public Vec3D toReverseVector() {
        return new Vec3D(this, ZERO);
    }

    @Override
    public Vec3D toReverseVector(Pos2D end_pos) {
        return toReverseVector(end_pos.to3D());
    }

    /**
     * @param end_pos
     * @return
     * @since 1.6.4
     */
    public Vec3D toReverseVector(Pos3D end_pos) {
        return new Vec3D(this, end_pos);
    }

    /**
     * @param end_x
     * @param end_y
     * @param end_z
     * @return
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
     * Convert to platform-specific block position
     * @param converter platform coordinate converter
     * @return native block position object
     * @since 1.8.0
     */
    public Object toRawBlockPos(ICoordinateConverter converter) {
        return converter.convertToBlockPos(this);
    }

    /**
     * Convert to platform-specific vector
     * @param converter platform coordinate converter
     * @return native vector object
     * @since 1.8.4
     */
    public Object toMojangDoubleVector(ICoordinateConverter converter) {
        return converter.convertToVec3d(this);
    }

    /**
     * @deprecated Use {@link #toRawBlockPos(ICoordinateConverter)} with platform-specific converter
     */
    @Deprecated
    public Object toRawBlockPos() {
        throw new UnsupportedOperationException("Use toRawBlockPos(ICoordinateConverter) with platform-specific converter");
    }

    /**
     * @deprecated Use {@link #toMojangDoubleVector(ICoordinateConverter)} with platform-specific converter
     */
    @Deprecated
    public Object toMojangDoubleVector() {
        throw new UnsupportedOperationException("Use toMojangDoubleVector(ICoordinateConverter) with platform-specific converter");
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

    public int compareTo(@NotNull Pos3D o) {
        int i = super.compareTo(o);
        if (i == 0) {
            i = Double.compare(z, o.z);
        }
        return i;
    }
}
