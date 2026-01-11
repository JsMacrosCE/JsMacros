package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(EditBox.class)
public interface MixinTextFieldWidget {

    @Accessor("isEditable")
    boolean getEditable();

    @Accessor
    int getMaxLength();

}
