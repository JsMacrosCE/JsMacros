package com.jsmacrosce.jsmacros.client.access;

import com.jsmacrosce.doclet.DocletIgnore;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@DocletIgnore
public interface IInventory {
    int jsmacros$getX();

    int jsmacros$getY();

    Slot jsmacros_getSlotUnder(double x, double y);

}
