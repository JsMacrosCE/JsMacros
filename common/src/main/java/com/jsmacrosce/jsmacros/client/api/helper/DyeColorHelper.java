package com.jsmacrosce.jsmacros.client.api.helper;

import net.minecraft.world.item.DyeColor;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * Wraps a dye color and exposes its name, index, and game-defined colors.
 * <p>
 * The item-texture diffuse value is packed {@code 0xFFRRGGBB}; mask with {@code 0xFFFFFF} to extract
 * its RGB channels. Firework and sign-text colors are separate game-defined values, not brightness
 * variants of the diffuse color. Firework color is {@code 0xRRGGBB}; sign-text color is
 * {@code 0xRRGGBB} on Minecraft 1.21.5 and {@code 0xFFRRGGBB} on 1.21.8 and later.
 *
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class DyeColorHelper extends BaseHelper<DyeColor> {

    /** Wraps the supplied raw dye color.
     * @param base raw dye color to wrap
     */
    public DyeColorHelper(DyeColor base) {
        super(base);
    }

    /**
     * Returns the serialized dye color name (for example, {@code light_blue}), not a display name.
     *
     * @return the name of the color.
     * @since 1.8.4
     */
    @DocletReplaceReturn("DyeColorName")
    public String getName() {
        return base.getName();
    }

    /**
     * Returns this color's game-defined index (white is {@code 0}; black is {@code 15}).
     *
     * @return the color's identifier.
     * @since 1.8.4
     */
    public int getId() {
        return base.getId();
    }

    /**
     * Returns the dye's item-texture diffuse color as packed {@code 0xFFRRGGBB}. Mask with
     * {@code 0xFFFFFF} to extract its RGB channels; this is distinct from the separately stored
     * firework and sign-text colors.
     *
     * @return the diffuse color with an opaque alpha byte
     * @since 1.8.4
     */
    public int getColorValue() {
        return base.getTextureDiffuseColor();
    }

    /**
     * Returns the dye's separately stored firework color as packed {@code 0xRRGGBB}, with no alpha
     * byte. It is not derived from the diffuse or sign-text color.
     *
     * @return the firework color without an alpha byte
     * @since 1.8.4
     */
    public int getFireworkColor() {
        return base.getFireworkColor();
    }

    /**
     * Returns the dye's separately stored sign-text color. It is packed {@code 0xRRGGBB} on
     * Minecraft 1.21.5 and {@code 0xFFRRGGBB} on 1.21.8 and later. The getter returns this value
     * for every dye, whether or not that dye is used for sign text.
     *
     * @return the recorded sign-text color, whose alpha format depends on the Minecraft version
     * @since 1.8.4
     */
    public int getSignColor() {
        return base.getTextColor();
    }

}
