package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.fish.TropicalFish;
*///? } else {
import net.minecraft.world.entity.animal.TropicalFish;
//?}

/**
 * the tropical fish, which is made of three separate numbers: a body size, a pattern, and two
 * colours.
 * <p>
 * The three get reported under three names that are easy to confuse.
 * {@link #getSize() getSize} is the body size and answers {@code SMALL} or {@code LARGE},
 * spelt in capitals as the game spelt it, and is not a length.
 * {@link #getVariant() getVariant} is the pattern - {@code stripey}, {@code spotty},
 * {@code kob} and the rest - with no namespace on the front.
 * {@link #getVarietyId() getVarietyId} is the number behind that pattern rather than a second
 * opinion about the fish.
 * <p>
 * The two colours are the base colour the pattern is drawn on and the colour the pattern
 * itself is drawn in, and both come back as packed colour numbers rather than as
 * {@code DyeColorHelper}s, so a script has to do the unpacking itself.
 * example:
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

    public TropicalFishEntityHelper(TropicalFish base) {
        super(base);
    }

    /**
     * The pattern on the fish, as the game spells it in its save data and with no namespace
     * on the front: one of {@code kob}, {@code sunstreak}, {@code snooper}, {@code dasher},
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
     * Whether the fish is on the small or the large body, as {@code SMALL} or {@code LARGE}.
     * Those are the game's own spellings and they are capitalised: a script comparing against
     * a lower case string will not match.
     * <p>
     * It is a body shape rather than a size, and it is decided by the pattern rather than
     * chosen separately, which is why a script cannot ask for a large fish with a small
     * pattern.
     * example:
     * <pre>
     * const TropicalFishEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.TropicalFishEntityHelper");
     * const fish = World.getEntities(24, "tropical_fish");
     * if (fish !== null) {
     *   for (const entity of fish) {
     *     const tf = TropicalFishEntityHelper.class.cast(entity);
     *     if (tf.getSize() === "LARGE") {
     *       Chat.log(`a big one at ${tf.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the size of this tropical fish's variant.
     * @since 1.8.4
     */
    @DocletReplaceReturn("TropicalSize")
    public String getSize() {
        return base.getPattern().base().name();
    }

    /**
     * The colour the pattern is drawn on, as a packed colour number rather than as a dye
     * colour. The three bytes of it are red, green and blue, so the usual way to read it in a
     * script is to shift them apart rather than to compare the whole number against a colour
     * name.
     * <p>
     * Nothing here turns this back into a {@code DyeColorHelper}, so a script that wants the
     * name has to unpack it.
     * example:
     * <pre>
     * const TropicalFishEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.TropicalFishEntityHelper");
     * const fish = World.getEntities(24, "tropical_fish");
     * if (fish !== null) {
     *   for (const entity of fish) {
     *     const tf = TropicalFishEntityHelper.class.cast(entity);
     *     const packed = tf.getBaseColor();
     *     const red = Math.floor(packed / 65536) % 256;
     *     const green = Math.floor(packed / 256) % 256;
     *     const blue = packed % 256;
     *     Chat.log(`rgb ${red}, ${green}, ${blue}`);
     *   }
     * }
     * </pre>
     *
     * @return the base color of this tropical fish's pattern.
     * @since 1.8.4
     */
    public int getBaseColor() {
        return base.getBaseColor().getTextureDiffuseColor();
    }

    /**
     * The colour the pattern itself is drawn in, as a packed colour number, in the same
     * three bytes as {@link #getBaseColor() getBaseColor}. On a fish where the pattern and
     * the body are the same colour the two answers are the same number.
     *
     * @return the pattern color of this tropical fish's pattern.
     * @since 1.8.4
     */
    public int getPatternColor() {
        return base.getPatternColor().getTextureDiffuseColor();
    }

    /**
     * The number behind the fish's look rather than a second opinion about it. Nothing on a
     * tropical fish is stored separately from this, so it moves only when the fish's
     * appearance does - which makes it a figure to compare against rather than to read.
     * <p>
     * {@link #getVariant() getVariant} is the figure to read; this is for comparing against
     * whatever the game or a script is holding elsewhere.
     *
     * @return the id of this tropical fish's variant.
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
