package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.TropicalFish;
import com.jsmacrosce.doclet.DocletReplaceReturn;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class TropicalFishEntityHelper extends FishEntityHelper<TropicalFish> {

    public TropicalFishEntityHelper(TropicalFish base) {
        super(base);
    }

    /**
     * @return the variant of this tropical fish.
     * @since 1.8.4
     */
    @DocletReplaceReturn("TropicalVariant")
    public String getVariant() {
        return base.getPattern().getSerializedName();
    }

    /**
     * @return the size of this tropical fish's variant.
     * @since 1.8.4
     */
    @DocletReplaceReturn("TropicalSize")
    public String getSize() {
        return base.getPattern().base().name();
    }

    /**
     * @return the base color of this tropical fish's pattern.
     * @since 1.8.4
     */
    public int getBaseColor() {
        return base.getBaseColor().getTextureDiffuseColor();
    }

    /**
     * @return the pattern color of this tropical fish's pattern.
     * @since 1.8.4
     */
    public int getPatternColor() {
        return base.getPatternColor().getTextureDiffuseColor();
    }

    /**
     * @return the id of this tropical fish's variant.
     * @since 1.8.4
     */
    public int getVarietyId() {
        return base.getPackedVariant();
    }

}
