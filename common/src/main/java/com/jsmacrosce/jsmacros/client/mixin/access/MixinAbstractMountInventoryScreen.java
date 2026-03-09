package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.jsmacrosce.jsmacros.client.access.IAbstractMountInventoryScreen;

@Mixin(targets = "net.minecraft.client.gui.screens.inventory.AbstractMountInventoryScreen")
public class MixinAbstractMountInventoryScreen implements IAbstractMountInventoryScreen {
    @Shadow
    protected LivingEntity mount;

    @Override
    public LivingEntity jsmacros_getEntity() {
        return mount;
    }

}
