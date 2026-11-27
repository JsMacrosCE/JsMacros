package com.jsmacrosce.jsmacros.client.mixin.access;

import com.jsmacrosce.doclet.DocletIgnore;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.jetbrains.annotations.ApiStatus;

@Mixin(StatsCounter.class)
@ApiStatus.Internal
@DocletIgnore
public interface MixinStatHandler {
    @Accessor("stats")
    Object2IntMap<Stat<?>> getStatMap();

}
