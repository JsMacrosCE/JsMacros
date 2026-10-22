package com.jsmacrosce.jsmacros.client.util;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

/**
 * Provides version-compatible access to a {@link Camera}'s position and rotations.
 * Each method calls the accessor available in the current Minecraft source target.
 */
public final class CameraCompat {

    private CameraCompat() {
    }

    /**
     * Returns the camera's stored position vector directly.
     * @param camera non-null camera to read
     * @return the camera's raw position vector
     */
    public static Vec3 position(Camera camera) {
        //? if >=1.21.11 {
        /*return camera.position();
        *///? } else {
        return camera.getPosition();
        //? }
    }

    /**
     * Returns the camera's pitch (X rotation) in degrees.
     * @param camera non-null camera to read
     * @return the camera's pitch in degrees
     */
    public static float xRot(Camera camera) {
        //? if >=1.21.11 {
        /*return camera.xRot();
        *///? } else {
        return camera.getXRot();
        //? }
    }

    /**
     * Returns the camera's yaw (Y rotation) in degrees.
     * @param camera non-null camera to read
     * @return the camera's yaw in degrees
     */
    public static float yRot(Camera camera) {
        //? if >=1.21.11 {
        /*return camera.yRot();
        *///? } else {
        return camera.getYRot();
        //? }
    }
}
