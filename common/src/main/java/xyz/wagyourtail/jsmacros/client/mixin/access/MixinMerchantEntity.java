package xyz.wagyourtail.jsmacros.client.mixin.access;

import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xyz.wagyourtail.jsmacros.client.access.IMerchantEntity;

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
