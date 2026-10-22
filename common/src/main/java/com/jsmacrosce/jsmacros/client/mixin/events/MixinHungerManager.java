package com.jsmacrosce.jsmacros.client.mixin.events;

import com.jsmacrosce.doclet.DocletIgnore;

import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.jsmacrosce.jsmacros.client.api.event.impl.player.EventHungerChange;

@Mixin(FoodData.class)
@DocletIgnore
public class MixinHungerManager {

    @Shadow
    private int foodLevel;

    @Inject(at = @At("HEAD"), method = "setFoodLevel")
    public void onSetFoodLevel(int foodLevel, CallbackInfo info) {
        if (foodLevel != this.foodLevel) {
            new EventHungerChange(foodLevel).trigger();
        }
    }

}
