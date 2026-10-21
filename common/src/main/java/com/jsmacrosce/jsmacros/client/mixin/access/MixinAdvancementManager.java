package com.jsmacrosce.jsmacros.client.mixin.access;

import com.jsmacrosce.doclet.DocletIgnore;

import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.Set;

/**
 * Exposes the advancement tree's node map and non-root advancement nodes.
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(AdvancementTree.class)
@DocletIgnore
public interface MixinAdvancementManager {

    @Accessor("nodes")
    Map<ResourceLocation, AdvancementNode> getAdvancements();

    @Accessor("tasks")
    Set<AdvancementNode> getDependents();

}
