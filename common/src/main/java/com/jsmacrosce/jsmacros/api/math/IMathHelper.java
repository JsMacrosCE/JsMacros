package com.jsmacrosce.jsmacros.api.math;

/**
 * Platform-agnostic math helper interface for angle calculations and transformations.
 * Platform-specific implementations should provide the actual math operations.
 */
public interface IMathHelper {

    /**
     * Wraps degrees between -180 and 180
     * @param degrees input angle in degrees
     * @return wrapped angle
     */
    float wrapDegrees(float degrees);

    /**
     * Converts degrees to radians
     * @param degrees angle in degrees
     * @return angle in radians
     */
    default float degToRad(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    /**
     * Converts radians to degrees
     * @param radians angle in radians
     * @return angle in degrees
     */
    default float radToDeg(float radians) {
        return (float) Math.toDegrees(radians);
    }

    /**
     * Normalizes an angle between 0 and 360 degrees
     * @param angle input angle in degrees
     * @return normalized angle
     */
    default float normalizeAngle(float angle) {
        angle = angle % 360;
        if (angle < 0) {
            angle += 360;
        }
        return angle;
    }
}