package com.jsmacrosce.jsmacros.util;

import net.minecraft.world.level.ChunkPos;

/**
 * Bridges the 26.1 {@link ChunkPos} record accessors against the 1.21.x public fields.
 * <p>
 * 1.21.x exposes {@code public final int x} / {@code z}; 26.1 turns the class into a
 * record, so the same values are only reachable through {@code x()} / {@code z()}.
 * <p>
 * This deliberately hands back plain ints rather than wrapping the position. The real
 * {@code ChunkPos} keeps flowing through the codebase, so nothing has to be kept in sync
 * and members beyond x/z stay reachable without a second code path. A new need (say
 * {@code center()} or {@code isValid()}) is one more method here.
 */
public final class ChunkPosCompat {

    private ChunkPosCompat() {
    }

    public static int x(ChunkPos pos) {
        //? if >=26.1 {
        /*return pos.x();
        *///? } else {
        return pos.x;
        //? }
    }

    public static int z(ChunkPos pos) {
        //? if >=26.1 {
        /*return pos.z();
        *///? } else {
        return pos.z;
        //? }
    }
}
