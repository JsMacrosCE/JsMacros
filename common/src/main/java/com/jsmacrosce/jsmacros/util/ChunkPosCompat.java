package com.jsmacrosce.jsmacros.util;

import net.minecraft.world.level.ChunkPos;

/** Provides cross-version access to {@link ChunkPos} coordinates. */
public final class ChunkPosCompat {

    private ChunkPosCompat() {
    }

    /** Returns the supplied position's X coordinate in chunks, not blocks.
     * @param pos non-null chunk position to read
     * @return X coordinate in chunks
     */
    public static int x(ChunkPos pos) {
        //? if >=26.1 {
        /*return pos.x();
        *///? } else {
        return pos.x;
        //? }
    }

    /** Returns the supplied position's Z coordinate in chunks, not blocks.
     * @param pos non-null chunk position to read
     * @return Z coordinate in chunks
     */
    public static int z(ChunkPos pos) {
        //? if >=26.1 {
        /*return pos.z();
        *///? } else {
        return pos.z;
        //? }
    }
}
