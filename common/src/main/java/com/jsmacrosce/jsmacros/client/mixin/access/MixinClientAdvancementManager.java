package com.jsmacrosce.jsmacros.client.mixin.access;

import com.jsmacrosce.doclet.DocletIgnore;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(ClientAdvancements.class)
@ApiStatus.Internal
@DocletIgnore
public interface MixinClientAdvancementManager {

    @Accessor("progress")
    Map<AdvancementHolder, AdvancementProgress> getAdvancementProgresses();

}
