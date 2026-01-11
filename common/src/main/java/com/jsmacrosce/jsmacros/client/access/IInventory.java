package com.jsmacrosce.jsmacros.client.access;

import net.minecraft.world.inventory.Slot;

public interface IInventory {
    int jsmacros$getX();

    int jsmacros$getY();

    Slot jsmacros_getSlotUnder(double x, double y);

}
