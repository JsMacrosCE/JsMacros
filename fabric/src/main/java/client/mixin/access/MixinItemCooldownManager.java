package xyz.wagyourtail.jsmacros.client.mixin.access;

import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xyz.wagyourtail.jsmacros.client.access.IItemCooldownEntry;
import xyz.wagyourtail.jsmacros.client.access.IItemCooldownManager;

import java.util.Map;

@Mixin(ItemCooldowns.class)
public class MixinItemCooldownManager implements IItemCooldownManager {

    @Shadow
    @Final
    private Map<Item, IItemCooldownEntry> cooldowns;

    @Shadow
    private int tickCount;

    @Override
    public Map<Item, IItemCooldownEntry> jsmacros_getCooldownItems() {
        return cooldowns;
    }

    @Override
    public int jsmacros_getManagerTicks() {
        return tickCount;
    }

}
