package com.jsmacrosce.jsmacros.client.api.helper;

import net.minecraft.world.item.DyeColor;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class DyeColorHelper extends BaseHelper<DyeColor> {

    public DyeColorHelper(DyeColor base) {
        super(base);
    }

    /**
     * @return the name of the color.
     * @since 1.8.4
     */
    @DocletReplaceReturn("DyeColorName")
    public String getName() {
        return base.getName();
    }

    /**
     * @return the color's identifier.
     * @since 1.8.4
     */
    public int getId() {
        return base.getId();
    }

    /**
     * @return the color's rgb value.
     * @since 1.8.4
     */
    public int getColorValue() {
        return base.getTextureDiffuseColor();
    }

    /**
     * @return the color's variation when used in fireworks.
     * @since 1.8.4
     */
    public int getFireworkColor() {
        return base.getFireworkColor();
    }

    /**
     * @return the color's variation when used on signs.
     * @since 1.8.4
     */
    public int getSignColor() {
        return base.getTextColor();
    }

}
