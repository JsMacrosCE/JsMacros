package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(Entity.class)
public interface MixinEntity2 {

    // Don't make this static, it will disable the compile and reload feature!
    @Accessor("DATA_CUSTOM_NAME")
    EntityDataAccessor<Optional<Component>> getCustomNameKey();

}
