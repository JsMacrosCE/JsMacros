package com.jsmacrosce.jsmacros.api.math;

import java.util.Objects;

/**
 * A flat plane in 3D space, described by three points that lie on it. Nothing else is stored: the
 * three points are the nine public {@code double} fields {@code x1}/{@code y1}/{@code z1} through
 * {@code x3}/{@code y3}/{@code z3}, and every member here is derived from those on demand, so
 * assigning to a field is immediately reflected in the normal and the edge vectors. They are
 * writable and nothing checks them, so a plane can be built either through the constructor or by
 * assigning to the fields after it was made.<br>
 * The three points have to actually span a plane. If they are collinear, or two of them are the
 * same point, the normal comes back as a vector of length {@code 0} rather than as an error, and
 * normalizing that result divides by zero and fills it with {@code NaN}.<br>
 * The normal is not a unit vector. It is the raw cross product of the first two edges, so its
 * length is the area of the parallelogram those edges span, which is twice the area of the triangle
 * the three points form. Read it with {@link Vec3D#getMagnitude() getMagnitude()} if the area is
 * what you are after, or turn it into a direction with
 * {@link Vec3D#normalize() normalize()}.<br>
 * Which way it points follows the right hand rule around the three points in the order they are
 * given, first through second towards third, so handing over the same three points in a different
 * order flips the normal. Each of the three edge vectors starts at the lowest numbered point of
 * the pair it covers.<br>
 * Nothing in the game hands these objects out, and no library in this API returns one. This is a
 * plain value type for a script that already has three points and needs to know which way the
 * surface they span faces, so the only way to make one is to ask the class loader for the fully
 * qualified name {@code com.jsmacrosce.jsmacros.api.math.Plane3D} and construct it from there.
 * @since 1.6.5
 */
public class Plane3D {
    /**
     * the x coordinate of the first of the three points, which is the corner every edge vector that
     * mentions point one starts from. Not checked against the other coordinates.
     */
    public double x1;
    /**
     * the y coordinate of the first of the three points. Not checked against the other coordinates.
     */
    public double y1;
    /**
     * the z coordinate of the first of the three points. Not checked against the other coordinates.
     */
    public double z1;
    /**
     * the x coordinate of the second of the three points, the one the normal's direction is worked
     * out through. Not checked against the other coordinates.
     */
    public double x2;
    /**
     * the y coordinate of the second of the three points. Not checked against the other coordinates.
     */
    public double y2;
    /**
     * the z coordinate of the second of the three points. Not checked against the other coordinates.
     */
    public double z2;
    /**
     * the x coordinate of the third of the three points, the one the normal's direction is worked
     * out towards. Not checked against the other coordinates.
     */
    public double x3;
    /**
     * the y coordinate of the third of the three points. Not checked against the other coordinates.
     */
    public double y3;
    /**
     * the z coordinate of the third of the three points. Not checked against the other coordinates.
     */
    public double z3;

    /**
     * makes a plane out of three points that lie on it. The order matters, since it decides which
     * way {@link #getNormalVector()} comes out pointing.
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
     * the vector perpendicular to this plane, from the world origin out to the normal. Only the end
     * of it is the direction; the start is always {@code 0, 0, 0}, so read the direction with
     * {@link Vec3D#getEnd() getEnd()}.<br>
     * It is the cross product of {@link #getVec12()} and {@link #getVec23()}, which gives the same
     * answer as crossing {@link #getVec12()} with {@link #getVec13()}: the two second edges differ
     * by the first one, and a vector crossed with itself contributes nothing. It is therefore
     * perpendicular to the plane, and it is not normalized: its length is the area of the
     * parallelogram the first two edges span, twice the area of the triangle. Call
     * {@link Vec3D#normalize() normalize()} on the result for a unit direction, or
     * {@link Vec3D#getMagnitude() getMagnitude()} for the area.<br>
     * For three collinear points, or any two of them equal, this is the zero vector rather than an
     * error, and normalizing it then produces {@code NaN} coordinates.
     *
     * @return a new vector from the origin to this plane's normal, parallel to the right hand
     *         rule around the three points in the order they were given
     * @since 1.6.5
     */
    public Vec3D getNormalVector() {
        return new Vec3D(x1, y1, z1, x2, y2, z2).crossProduct(new Vec3D(x2, y2, z2, x3, y3, z3));
    }

    /**
     * the first edge of the plane, running from the first of the three points to the second. This
     * is a fresh vector each call, it is not stored anywhere.
     *
     * @return a new vector from point one to point two
     * @since 1.6.5
     */
    public Vec3D getVec12() {
        return new Vec3D(x1, y1, z1, x2, y2, z2);
    }

    /**
     * the diagonal of the plane, running from the first of the three points straight to the third.
     * This is a fresh vector each call, it is not stored anywhere.
     *
     * @return a new vector from point one to point three
     * @since 1.6.5
     */
    public Vec3D getVec13() {
        return new Vec3D(x1, y1, z1, x3, y3, z3);
    }

    /**
     * the second edge of the plane, running from the second of the three points to the third. This
     * is the edge the normal is crossed with, so unlike {@link #getVec12()} and {@link #getVec13()}
     * it does not start at point one. This is a fresh vector each call, it is not stored anywhere.
     *
     * @return a new vector from point two to point three
     * @since 1.6.5
     */
    public Vec3D getVec23() {
        return new Vec3D(x2, y2, z2, x3, y3, z3);
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(x1, y1, z1, x2, y2, z2, x3, y3, z3);
    }

}
