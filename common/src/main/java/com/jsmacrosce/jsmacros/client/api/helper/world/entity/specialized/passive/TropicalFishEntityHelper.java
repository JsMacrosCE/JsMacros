package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.fish.TropicalFish;
*///? } else {
import net.minecraft.world.entity.animal.TropicalFish;
//?}

/**
 * Provides a tropical fish's pattern, body size, and base and pattern colours.
 * <pre>
 * const TropicalFishEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.TropicalFishEntityHelper");
 * const fish = World.getEntities(32, "tropical_fish");
 * if (fish !== null) {
 *   for (const entity of fish) {
 *     const tf = TropicalFishEntityHelper.class.cast(entity);
 *     Chat.log(`${tf.getVariant()} on a ${tf.getSize().toLowerCase()} body, `
 *       + `base 0x${tf.getBaseColor().toString(16)}, `
 *       + `pattern 0x${tf.getPatternColor().toString(16)}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class TropicalFishEntityHelper extends FishEntityHelper<TropicalFish> {

    /**
     * Wraps the supplied tropical fish and retains its reference for this helper.
     *
     * @param base tropical fish entity to wrap
     */
    public TropicalFishEntityHelper(TropicalFish base) {
        super(base);
    }

    /**
     * The fish pattern's serialized name, without a namespace: one of {@code kob}, {@code sunstreak}, {@code snooper}, {@code dasher},
     * {@code brinely}, {@code spotty}, {@code flopper}, {@code stripey}, {@code glitter},
     * {@code blockfish}, {@code betty} or {@code clayfish}.
     *
     * @return the variant of this tropical fish.
     * @since 1.8.4
     */
    @DocletReplaceReturn("TropicalVariant")
    public String getVariant() {
        return base.getPattern().getSerializedName();
    }

    /**
     * The body size associated with the fish's pattern: {@code SMALL} or {@code LARGE}.
     *
     * @return the size of this tropical fish's variant.
     * @since 1.8.4
     */
    @DocletReplaceReturn("TropicalSize")
    public String getSize() {
        return base.getPattern().base().name();
    }

    /**
     * The base dye colour's packed texture-diffuse RGB value.
     *
     * @return the base dye color's packed texture-diffuse RGB value.
     * @since 1.8.4
     */
    public int getBaseColor() {
        return base.getBaseColor().getTextureDiffuseColor();
    }

    /**
     * The pattern dye colour's packed texture-diffuse RGB value.
     *
     * @return the pattern dye color's packed texture-diffuse RGB value.
     * @since 1.8.4
     */
    public int getPatternColor() {
        return base.getPatternColor().getTextureDiffuseColor();
    }

    /**
     * The packed variant integer encoding the fish's pattern and colours.
     *
     * @return the packed variant integer encoding the fish's pattern and colors.
     * @since 1.8.4
     */
    public int getVarietyId() {
        //? if >=1.21.11 {
        /*return base.getPattern().getPackedId();
        *///? } else {
        return base.getPackedVariant();
        //?}
    }

}
