package com.jsmacrosce.jsmacros.client.access;

import com.jsmacrosce.doclet.DocletIgnore;
@DocletIgnore
public interface IMixinEntity {

    void jsmacros_setGlowingColor(int glowingColor);

    void jsmacros_resetColor();

    /**
     * @param glowing {@code 0} forces glowing off, {@code 2} forces it on, and any other value leaves the entity's normal glowing result unchanged
     */
    void jsmacros_setForceGlowing(int glowing);

}
