package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(ClientAdvancements.class)
public interface MixinClientAdvancementManager {

    @Accessor("progress")
    Map<AdvancementHolder, AdvancementProgress> getAdvancementProgresses();

}
