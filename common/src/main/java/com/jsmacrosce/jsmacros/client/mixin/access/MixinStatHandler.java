package com.jsmacrosce.jsmacros.client.mixin.access;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StatsCounter.class)
public interface MixinStatHandler {
    @Accessor("stats")
    Object2IntMap<Stat<?>> getStatMap();

}
