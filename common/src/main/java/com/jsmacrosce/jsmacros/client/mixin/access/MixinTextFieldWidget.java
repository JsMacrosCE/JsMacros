package com.jsmacrosce.jsmacros.client.mixin.access;

import com.jsmacrosce.doclet.DocletIgnore;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accessors for private state on Minecraft's text-editing widget.
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(EditBox.class)
@DocletIgnore
public interface MixinTextFieldWidget {

    /** Returns whether the widget is editable. */
    @Accessor("isEditable")
    boolean getEditable();

    /** Returns the maximum text length in Java string characters; zero allows no text. */
    @Accessor
    int getMaxLength();

    /** Returns the selection anchor, as a UTF-16 string index. */
    @Accessor("highlightPos")
    int getHighlightPos();

}
