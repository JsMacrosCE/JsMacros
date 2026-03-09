package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.jsmacrosce.jsmacros.client.access.IMerchantEntity;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.npc.villager.AbstractVillager;
*///? } else {
import net.minecraft.world.entity.npc.AbstractVillager;
//? }

@Mixin(AbstractVillager.class)
public class MixinMerchantEntity implements IMerchantEntity {
    @Shadow
    @Nullable
    protected MerchantOffers offers;

    @Override
    public void jsmacros_refreshOffers() {
        this.offers = null;
    }

}
