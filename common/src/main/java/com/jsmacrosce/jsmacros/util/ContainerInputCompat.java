package com.jsmacrosce.jsmacros.util;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
//? if >=26.1 {
/*import net.minecraft.world.inventory.ContainerInput;
*///?} else {
import net.minecraft.world.inventory.ClickType;
//?}

public final class ContainerInputCompat {
    public static final int PICKUP = 0;
    public static final int QUICK_MOVE = 1;
    public static final int SWAP = 2;
    public static final int CLONE = 3;
    public static final int THROW = 4;
    public static final int QUICK_CRAFT = 5;
    public static final int PICKUP_ALL = 6;

    private ContainerInputCompat() {}

    public static void dispatch(MultiPlayerGameMode man, int syncId, int slotId, int button, int action, Player player) {
        //? if >=26.1 {
        /*man.handleContainerInput(syncId, slotId, button, toAction(action), player);
        *///?} else {
        man.handleInventoryMouseClick(syncId, slotId, button, toAction(action), player);
        //?}
    }

    //? if >=26.1 {
    /*private static ContainerInput toAction(int action) {
        return switch (action) {
            case PICKUP -> ContainerInput.PICKUP;
            case QUICK_MOVE -> ContainerInput.QUICK_MOVE;
            case SWAP -> ContainerInput.SWAP;
            case CLONE -> ContainerInput.CLONE;
            case THROW -> ContainerInput.THROW;
            case QUICK_CRAFT -> ContainerInput.QUICK_CRAFT;
            case PICKUP_ALL -> ContainerInput.PICKUP_ALL;
            default -> throw new IllegalArgumentException("Unknown click action: " + action);
        };
    }
    *///?} else {
    private static ClickType toAction(int action) {
        return ClickType.values()[action];
    }
    //?}
}
