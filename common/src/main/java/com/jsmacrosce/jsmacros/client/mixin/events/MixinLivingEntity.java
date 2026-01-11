package com.jsmacrosce.jsmacros.client.mixin.events;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.jsmacrosce.jsmacros.client.api.event.impl.player.EventDamage;
import com.jsmacrosce.jsmacros.client.api.event.impl.player.EventHeal;
import com.jsmacrosce.jsmacros.client.api.event.impl.player.EventHealthChange;
import com.jsmacrosce.jsmacros.client.api.event.impl.world.EventEntityDamaged;
import com.jsmacrosce.jsmacros.client.api.event.impl.world.EventEntityHealed;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {

    //IGNORE
    public MixinLivingEntity(EntityType<?> arg, Level arg2) {
        super(arg, arg2);
    }

    @Shadow
    public abstract float getMaxHealth();

    @Unique
    private float jsmacros$lastHealth;

    @Inject(at = @At("RETURN"), method = "<init>")
    private void onInit(CallbackInfo ci) {
        jsmacros$lastHealth = getMaxHealth();
    }

    @Inject(at = @At("HEAD"), method = "setHealth")
    public void onSetHealth(float health, CallbackInfo ci) {
        //fix for singleplayer worlds, when the client also has the integrated server
        if ((Object) this instanceof ServerPlayer) {
            return;
        }

        float difference = jsmacros$lastHealth - health;

        if (difference > 0) {
            if ((Object) this instanceof LocalPlayer) {
                new EventDamage(level().damageSources().generic(), health, difference).trigger();
                new EventHealthChange(health, -difference).trigger();
            }
            new EventEntityDamaged((Entity) (Object) this, health, difference).trigger();
        } else if (difference < 0) {

            difference *= -1;

            if ((Object) this instanceof LocalPlayer) {
                new EventHeal(level().damageSources().generic(), health, difference).trigger();
                new EventHealthChange(health, difference).trigger();
            }
            new EventEntityHealed((Entity) (Object) this, health, difference).trigger();
        }
        jsmacros$lastHealth = health;
    }

}
