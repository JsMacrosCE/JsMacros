package com.jsmacrosce.jsmacros.client.mixin.access;

//? if <1.21.11 {
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.jsmacrosce.jsmacros.client.access.IAbstractMountInventoryScreen;

import net.minecraft.world.entity.animal.horse.AbstractHorse;

@Mixin(HorseInventoryScreen.class)
public class MixinHorseInventoryScreen implements IAbstractMountInventoryScreen {
    @Shadow
    @Final
    private AbstractHorse horse;

    @Override
    public Entity jsmacros_getEntity() {
        return horse;
    }

}
//? } else {
/*// Dummy class to avoid mixin errors
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(HorseInventoryScreen.class)
public class MixinHorseInventoryScreen {}
*///? }