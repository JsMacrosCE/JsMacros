package com.jsmacrosce.jsmacros.client.util;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

/**
 * Bridges the {@link Camera} position accessor rename.
 * <p>
 * 1.21.5 only has {@code getPosition()}; 1.21.8 onwards also expose {@code position()},
 * and 26.1 dropped the old name. Keeping the switch here means render code does not
 * repeat the version gate at every camera lookup.
 */
public final class CameraCompat {

    private CameraCompat() {
    }

    public static Vec3 position(Camera camera) {
        //? if >=1.21.11 {
        /*return camera.position();
        *///? } else {
        return camera.getPosition();
        //? }
    }
}
