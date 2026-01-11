package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.projectile.ThrownTrident;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(ThrownTrident.class)
public interface MixinTridentEntity {

    // Don't make this static, it will disable the compile and reload feature!
    @Accessor("ID_LOYALTY")
    EntityDataAccessor<Byte> getLoyalty();

}
