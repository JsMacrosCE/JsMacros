package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(EnderDragonPhase.class)
public interface MixinPhaseType {

    @Accessor
    String getName();

}
