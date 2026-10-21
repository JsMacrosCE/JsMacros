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
 * Calls the entity-interaction overload across Minecraft API versions.
 * <p>
 * On Minecraft 26.1 and later, constructs an {@code EntityHitResult} for the entity;
 * earlier versions pass the entity directly.
 */
public final class InteractionCompat {

    private InteractionCompat() {
    }

    /**
     * Delegates to the supplied game mode's entity-interaction API. On Minecraft 26.1 and later, the hit result is
     * constructed from {@code entity} rather than supplied by the caller.
     *
     * @param gameMode the non-null client game-mode controller handling the interaction
     * @param player the non-null interacting player
     * @param entity the non-null target entity
     * @param hand the non-null hand to use
     * @return the interaction result returned by the game mode; this does not indicate server acceptance
     */
    public static InteractionResult interact(MultiPlayerGameMode gameMode, Player player, Entity entity, InteractionHand hand) {
        //? if >=26.1 {
        /*return gameMode.interact(player, entity, new EntityHitResult(entity), hand);
        *///? } else {
        return gameMode.interact(player, entity, hand);
        //? }
    }
}
