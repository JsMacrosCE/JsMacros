package com.jsmacrosce.jsmacros.client.mixin.access;

import com.jsmacrosce.doclet.DocletIgnore;

import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.network.DisconnectionDetails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.jetbrains.annotations.ApiStatus;

@Mixin(DisconnectedScreen.class)
@ApiStatus.Internal
@DocletIgnore
public interface MixinDisconnectedScreen {

    @Accessor("details")
    DisconnectionDetails getInfo();

}
