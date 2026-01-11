package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.jsmacrosce.jsmacros.client.access.IResourcePackManager;

@Mixin(PackRepository.class)
public class MixinResourcePackManager implements IResourcePackManager {

    @Unique
    private boolean disableServerPacks = false;

    @Override
    public void jsmacros_disableServerPacks(boolean disable) {
        disableServerPacks = disable;
    }

    @Override
    public boolean jsmacros_isServerPacksDisabled() {
        return disableServerPacks;
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/Pack;isRequired()Z"), method = "rebuildSelected")
    public boolean onBuildPackList(Pack instance) {
        if (instance.getId().equals("server")) {
            return instance.isRequired() && !disableServerPacks;
        }
        return instance.isRequired();
    }

}
