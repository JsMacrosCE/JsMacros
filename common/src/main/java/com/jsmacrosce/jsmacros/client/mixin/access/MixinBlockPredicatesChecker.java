package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.world.item.AdventureModePredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

//? if >=1.21.11 {
/*import net.minecraft.advancements.criterion.BlockPredicate;
*///? } else {
import net.minecraft.advancements.critereon.BlockPredicate;
//? }

@Mixin(AdventureModePredicate.class)
public interface MixinBlockPredicatesChecker {

    @Accessor
    List<BlockPredicate> getPredicates();

}
