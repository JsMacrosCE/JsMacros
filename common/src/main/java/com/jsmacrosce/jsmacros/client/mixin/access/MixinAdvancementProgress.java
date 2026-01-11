package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.CriterionProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(AdvancementProgress.class)
public interface MixinAdvancementProgress {

    @Accessor
    AdvancementRequirements getRequirements();

    @Invoker("countCompletedRequirements")
    int invokeCountObtainedRequirements();

    @Accessor("criteria")
    Map<String, CriterionProgress> getCriteriaProgresses();

}
