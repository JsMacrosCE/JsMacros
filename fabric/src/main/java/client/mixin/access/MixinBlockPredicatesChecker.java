package xyz.wagyourtail.jsmacros.client.mixin.access;

import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.advancements.critereon.BlockPredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(AdventureModePredicate.class)
public interface MixinBlockPredicatesChecker {

    @Accessor
    List<BlockPredicate> getPredicates();

}
