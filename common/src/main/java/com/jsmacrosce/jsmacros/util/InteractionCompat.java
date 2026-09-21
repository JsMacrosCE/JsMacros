package com.jsmacrosce.jsmacros.util;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
//? if >=26.1 {
/*import net.minecraft.world.phys.EntityHitResult;
*///?}

public final class InteractionCompat {
    private InteractionCompat() {}

    public static InteractionResult interact(MultiPlayerGameMode gameMode, LocalPlayer player, Entity entity, InteractionHand hand) {
        //? if >=26.1 {
        /*return gameMode.interact(player, entity, new EntityHitResult(entity), hand);
        *///?} else {
        return gameMode.interact(player, entity, hand);
        //?}
    }
}
