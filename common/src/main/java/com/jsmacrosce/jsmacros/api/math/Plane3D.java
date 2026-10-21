package com.jsmacrosce.jsmacros.api.math;

import java.util.Objects;

/**
 * Represents three points in 3D space and derives their edge vectors and normal on demand. The
 * public coordinates are writable, so changing them affects subsequent results. The normal is the
 * unnormalized cross product of the first two edges; its direction follows the points' order and
 * its magnitude is twice the triangle's area. Collinear or repeated points yield a zero normal.
 * @since 1.6.5
 */
public class Plane3D {
    /**
     * The x coordinate of the first point.
     */
    public double x1;
    /**
     * The y coordinate of the first point.
     */
    public double y1;
    /**
     * The z coordinate of the first point.
     */
    public double z1;
    /**
     * The x coordinate of the second point.
     */
    public double x2;
    /**
     * The y coordinate of the second point.
     */
    public double y2;
    /**
     * The z coordinate of the second point.
     */
    public double z2;
    /**
     * The x coordinate of the third point.
     */
    public double x3;
    /**
     * The y coordinate of the third point.
     */
    public double y3;
    /**
     * The z coordinate of the third point.
     */
    public double z3;

    /**
     * Stores three points used to define the plane. Their order determines the direction of
     * {@link #getNormalVector()}; collinear or repeated points are accepted but yield a zero normal.
     *
     * @param x1 the x coordinate of the first point
     * @param y1 the y coordinate of the first point
     * @param z1 the z coordinate of the first point
     * @param x2 the x coordinate of the second point
     * @param y2 the y coordinate of the second point
     * @param z2 the z coordinate of the second point
     * @param x3 the x coordinate of the third point
     * @param y3 the y coordinate of the third point
     * @param z3 the z coordinate of the third point
     * @since 1.6.5
     */
    public Plane3D(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3) {
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
        this.x3 = x3;
        this.y3 = y3;
        this.z3 = z3;
    }

    /**
     * Returns the cross product of the first and second edges, as a vector from the origin. Its
     * direction follows the right-hand rule for the points' order, and its magnitude is twice the
     * triangle's area. It is not normalized. Collinear or repeated points yield a zero vector;
     * normalizing that vector produces {@code NaN} coordinates.
     *
     * @return a new vector from the origin to this plane's normal, parallel to the right hand
     *         rule around the three points in the order they were given
     * @since 1.6.5
     */
    public Vec3D getNormalVector() {
        return new Vec3D(x1, y1, z1, x2, y2, z2).crossProduct(new Vec3D(x2, y2, z2, x3, y3, z3));
    }

    /**
     * Returns the edge from the first point to the second.
     *
     * @return a new vector from point one to point two
     * @since 1.6.5
     */
    public Vec3D getVec12() {
        return new Vec3D(x1, y1, z1, x2, y2, z2);
    }

    /**
     * Returns the segment from the first point to the third.
     *
     * @return a new vector from point one to point three
     * @since 1.6.5
     */
    public Vec3D getVec13() {
        return new Vec3D(x1, y1, z1, x3, y3, z3);
    }

    /**
     * Returns the edge from the second point to the third.
     *
     * @return a new vector from point two to point three
     * @since 1.6.5
     */
    public Vec3D getVec23() {
        return new Vec3D(x2, y2, z2, x3, y3, z3);
    }

    /**
     * Compares the ordered coordinates of all three points; this does not compare the infinite
     * planes or treat reordered points as equivalent. Only the exact same runtime class compares
     * equal. Coordinates use {@link Double#compare(double, double)} semantics, including equal NaNs
     * and distinct signed zeroes.
     *
     * @param o the value to compare
     * @return {@code true} for this instance or an instance of the same class with equal ordered
     *         coordinates; {@code false} for {@code null} or any other value
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Plane3D plane3D = (Plane3D) o;
        return Double.compare(x1, plane3D.x1) == 0
                && Double.compare(y1, plane3D.y1) == 0
                && Double.compare(z1, plane3D.z1) == 0
                && Double.compare(x2, plane3D.x2) == 0
                && Double.compare(y2, plane3D.y2) == 0
                && Double.compare(z2, plane3D.z2) == 0
                && Double.compare(x3, plane3D.x3) == 0
                && Double.compare(y3, plane3D.y3) == 0
                && Double.compare(z3, plane3D.z3) == 0;
    }

    /**
     * Computes a hash from the current coordinates of all three ordered points. Since the
     * coordinates are mutable, changing them can change the hash; do not assume this is a stable
     * hash key while the coordinates change.
     *
     * @return the hash of the current coordinates
     */
    @Override
    public int hashCode() {
        return Objects.hash(x1, y1, z1, x2, y2, z2, x3, y3, z3);
    }

}
