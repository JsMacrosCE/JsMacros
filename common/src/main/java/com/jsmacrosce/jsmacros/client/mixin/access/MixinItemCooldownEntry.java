package com.jsmacrosce.jsmacros.client.mixin.access;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.jsmacrosce.jsmacros.client.access.IItemCooldownEntry;

@Mixin(targets = "net.minecraft.world.item.ItemCooldowns$CooldownInstance")
public class MixinItemCooldownEntry implements IItemCooldownEntry {

    @Shadow
    @Final
    int startTime;

    @Shadow
    @Final
    int endTime;

    @Override
    public int jsmacros_getStartTick() {
        return startTime;
    }

    @Override
    public int jsmacros_getEndTick() {
        return endTime;
    }

}
