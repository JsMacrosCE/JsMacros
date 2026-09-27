package com.jsmacrosce.jsmacros.client.util;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
//? if >=26.1 {
/*import net.minecraft.world.phys.EntityHitResult;
*///?}

/**
 * Bridges the {@code MultiPlayerGameMode.interact} entity overload.
 * <p>
 * Up to 1.21.x the target {@link Entity} is the last argument; 26.1 inserts an
 * {@code EntityHitResult} before the hand. Callers pass the entity and the hand, and
 * the synthetic hit result is built here.
 */
public final class InteractionCompat {

    private InteractionCompat() {
    }

    public static InteractionResult interact(MultiPlayerGameMode gameMode, Player player, Entity entity, InteractionHand hand) {
        //? if >=26.1 {
        /*return gameMode.interact(player, entity, new EntityHitResult(entity), hand);
        *///? } else {
        return gameMode.interact(player, entity, hand);
        //? }
    }
}
