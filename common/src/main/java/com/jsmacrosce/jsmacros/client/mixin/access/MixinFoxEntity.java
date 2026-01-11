package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Fox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.stream.Stream;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(Fox.class)
public interface MixinFoxEntity {

    @Invoker("isDefending")
    boolean invokeIsAggressive();

    @Invoker
    Stream<EntityReference<LivingEntity>> invokeGetTrustedEntities();

    @Invoker("trusts")
    boolean invokeCanTrust(LivingEntity entity);

}
