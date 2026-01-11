package com.jsmacrosce.jsmacros.api.coordinate;

import com.jsmacrosce.jsmacros.api.math.Pos3D;

/**
 * Platform-agnostic coordinate conversion interface.
 * Platform-specific implementations should provide conversions to/from native Minecraft types.
 */
public interface ICoordinateConverter {

    /**
     * Converts a Pos3D to the platform's native block position type
     * @param pos position to convert
     * @return native block position object
     */
    Object convertToBlockPos(Pos3D pos);

    /**
     * Converts the platform's native block position type to Pos3D
     * @param blockPos native block position object
     * @return converted position
     */
    Pos3D convertFromBlockPos(Object blockPos);

    /**
     * Converts a Pos3D to the platform's native vector type
     * @param pos position to convert
     * @return native vector object
     */
    Object convertToVec3d(Pos3D pos);

    /**
     * Converts the platform's native vector type to Pos3D
     * @param vec3d native vector object
     * @return converted position
     */
    Pos3D convertFromVec3d(Object vec3d);

    /**
     * Creates a new block position at the given coordinates
     * @param x x coordinate
     * @param y y coordinate
     * @param z z coordinate
     * @return native block position object
     */
    Object createBlockPos(double x, double y, double z);

    /**
     * Creates a new vector at the given coordinates
     * @param x x coordinate
     * @param y y coordinate
     * @param z z coordinate
     * @return native vector object
     */
    Object createVec3d(double x, double y, double z);
}