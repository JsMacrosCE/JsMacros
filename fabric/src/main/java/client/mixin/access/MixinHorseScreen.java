package xyz.wagyourtail.jsmacros.client.mixin.access;

import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xyz.wagyourtail.jsmacros.client.access.IHorseScreen;

@Mixin(HorseInventoryScreen.class)
public class MixinHorseScreen implements IHorseScreen {
    @Shadow
    @Final
    private AbstractHorse horse;

    @Override
    public Entity jsmacros_getEntity() {
        return horse;
    }

}
