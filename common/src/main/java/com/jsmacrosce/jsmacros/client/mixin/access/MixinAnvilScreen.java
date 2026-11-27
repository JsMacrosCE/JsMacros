package com.jsmacrosce.jsmacros.client.mixin.access;

import com.jsmacrosce.doclet.DocletIgnore;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(AnvilScreen.class)
@ApiStatus.Internal
@DocletIgnore
public interface MixinAnvilScreen {

    @Accessor("name")
    EditBox getNameField();

}
