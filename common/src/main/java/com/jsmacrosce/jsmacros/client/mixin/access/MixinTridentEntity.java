package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.network.syncher.EntityDataAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
*///? } else {
import net.minecraft.world.entity.projectile.ThrownTrident;
//? }

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
