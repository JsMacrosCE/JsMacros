package com.jsmacrosce.jsmacros.client.mixin.events;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SyncedDataHolder;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.ObjectUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.jsmacrosce.jsmacros.client.api.event.impl.world.EventNameChange;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinEntity2;

import java.util.Optional;

@Mixin(SynchedEntityData.class)
public class MixinDataTracker {

    @Shadow
    @Final
    private SyncedDataHolder entity;

    @Inject(method = "assignValue", at = @At("HEAD"), cancellable = true)
    public void onCopyToFrom(SynchedEntityData.DataItem<Optional<Component>> to, SynchedEntityData.DataValue<Optional<Component>> from, CallbackInfo ci) {
        if (entity instanceof Entity) {
            int id = to.getAccessor().id();
            if (id != ((MixinEntity2) entity).getCustomNameKey().id() || id != from.id()) return;

            Component newName = from.value().orElse(null);
            Component oldName = to.getValue().orElse(null);
            if (ObjectUtils.notEqual(oldName, newName)) {
                EventNameChange event = new EventNameChange((Entity) entity, oldName, newName);
                event.trigger();
                if (event.isCanceled()) ci.cancel();
                else {
                    TextHelper res = event.newName;
                    Component modified = res == null ? null : res.getRaw();
                    if (ObjectUtils.notEqual(newName, modified)) {
                        ci.cancel();
                        to.setValue(Optional.ofNullable(modified));
                    }
                }
            }
        }
    }

}
