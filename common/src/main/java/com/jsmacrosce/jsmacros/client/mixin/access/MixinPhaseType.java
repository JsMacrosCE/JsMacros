package com.jsmacrosce.jsmacros.client.mixin.access;

import com.jsmacrosce.doclet.DocletIgnore;

import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the name stored by an {@link EnderDragonPhase}.
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(EnderDragonPhase.class)
@DocletIgnore
public interface MixinPhaseType {

    @Accessor
    String getName();

}
