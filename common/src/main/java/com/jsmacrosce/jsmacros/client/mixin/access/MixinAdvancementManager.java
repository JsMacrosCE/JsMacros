package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.Set;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(AdvancementTree.class)
public interface MixinAdvancementManager {

    @Accessor("nodes")
    Map<ResourceLocation, AdvancementNode> getAdvancements();

    @Accessor("tasks")
    Set<AdvancementNode> getDependents();

}
