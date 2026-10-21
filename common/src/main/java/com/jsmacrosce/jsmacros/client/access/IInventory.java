package com.jsmacrosce.jsmacros.client.access;

import com.jsmacrosce.doclet.DocletIgnore;
import net.minecraft.world.inventory.Slot;

@DocletIgnore
public interface IInventory {
    int jsmacros$getX();

    int jsmacros$getY();

    Slot jsmacros_getSlotUnder(double x, double y);

}
