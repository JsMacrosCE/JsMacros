package com.jsmacrosce.jsmacros.client.mixin.access;

import com.jsmacrosce.doclet.DocletIgnore;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.jsmacrosce.jsmacros.client.access.IAbstractMountInventoryScreen;
import org.jetbrains.annotations.ApiStatus;

@Mixin(targets = "net.minecraft.client.gui.screens.inventory.AbstractMountInventoryScreen")
@ApiStatus.Internal
@DocletIgnore
public class MixinAbstractMountInventoryScreen implements IAbstractMountInventoryScreen {
    @Shadow
    protected LivingEntity mount;

    @Override
    public LivingEntity jsmacros_getEntity() {
        return mount;
    }

}
