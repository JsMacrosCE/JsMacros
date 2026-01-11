package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class MixinPlayerEntity extends LivingEntity {

    @Unique
    private Component realCustomName;


    protected MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        realCustomName = name;
    }

    @Inject(at = @At("HEAD"), method = "getName", cancellable = true)
    private void getName(CallbackInfoReturnable<Component> cir) {
        if (realCustomName != null) {
            cir.setReturnValue(realCustomName);
        }
    }

}
