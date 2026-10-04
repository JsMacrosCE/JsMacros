package com.jsmacrosce.jsmacros.client.api.helper;

import net.minecraft.world.item.DyeColor;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * one of the sixteen dye colours.
 * <p>
 * A dye colour is a fixed entry rather than an arbitrary colour, and that is what makes this
 * class more than a name: the game records three different shades against each of the sixteen,
 * because it tints the same dye differently depending on what it is dyeing. The tint on dyed
 * wool, on a shulker shell and on a collar is one shade, a firework star is drawn in a second,
 * and the text on a sign in a third, and this class is where those shades are read.
 * <p>
 * The three values do not share a shape. The firework shade is a packed {@code 0xRRGGBB} number
 * with nothing above it, but the other two carry a leading alpha byte of {@code 0xFF} and so
 * come back as {@code 0xFFRRGGBB}. A script that unpacks one of them into red, green and blue
 * has to mask the alpha off first, which is not what {@link StyleHelper#getCustomColor()} needs
 * — that one is a plain {@code 0xRRGGBB} and divides directly. Which shade to ask for depends
 * on what the colour is being used for, and picking the wrong one gives a colour that is
 * subtly off.
 * <p>
 * A colour is reached from whatever is dyed — a sheep's or a shulker's colour, a wolf's or a
 * cat's collar — through that entity's own helper, for instance
 * {@code SheepEntityHelper.getColor()}. A tinted thing that has not actually been dyed has no
 * colour, and the getter gives {@code null} rather than a default, so a null check belongs
 * before any of the calls here.
 * <br>
 * <b>No example is given because the generated TypeScript cannot express the path.</b> The
 * specialized entity helpers are reached through {@code World.getEntities}, which declares its
 * element type as {@code EntityTypeFromId<E>}, and the shipped definition of that alias is
 * {@code type EntityTypeFromId<E> = EntityHelper} — it discards {@code E}. The result is that
 * {@code World.getEntities("minecraft:sheep")[0].getColor()} works at runtime but is a type
 * error in a TypeScript script, because the element is typed as the base entity helper, which
 * has no colour call. A script in JavaScript can use the path above directly; a TypeScript
 * script has to reach the colour some other way until that alias carries its parameter.
 *
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class DyeColorHelper extends BaseHelper<DyeColor> {

    public DyeColorHelper(DyeColor base) {
        super(base);
    }

    /**
     * the name of the dye colour, which is the same in the resource pack and on an item.
     * <p>
     * This is the identifier form rather than a display name, so it is the string to match
     * against and the string an item id ends in: {@code "light_blue"} rather than
     * "Light Blue". The sixteen are {@code white}, {@code orange}, {@code magenta},
     * {@code light_blue}, {@code yellow}, {@code lime}, {@code pink}, {@code gray},
     * {@code light_gray}, {@code cyan}, {@code purple}, {@code blue}, {@code brown},
     * {@code green}, {@code red} and {@code black}.
     *
     * @return the name of the color.
     * @since 1.8.4
     */
    @DocletReplaceReturn("DyeColorName")
    public String getName() {
        return base.getName();
    }

    /**
     * where this colour sits in the sixteen, counting from 0 for white.
     * <p>
     * This is the game's own ordering, and it is the number the game stores for the colour on
     * an item. White is 0 and black is 15, and the order in between is not alphabetical, so it
     * is worth reading off rather than guessed. The sixteen ids run in the same order as the
     * sixteen names, but a name does not tell you its number: {@code red} is 14 and
     * {@code green} is 13, so the two run in opposite orders and neither can be derived from
     * the other by a rule worth relying on.
     *
     * @return the color's identifier.
     * @since 1.8.4
     */
    public int getId() {
        return base.getId();
    }

    /**
     * the shade this colour is drawn at on the item itself, as a packed rgb number.
     * <p>
     * This is the dye's own colour, used for the dyed item and for the animal it is on. It is
     * one of three independent per-colour constants rather than a value the other two are
     * computed from, so there is no rule relating them: the firework shade is a different
     * number for every colour and the sign shade is a third number again. This is the one to
     * use for anything representing the dye itself.
     * <br>
     * The value is {@code 0xFFRRGGBB} — the alpha byte the game stores is always {@code FF} —
     * so a script that wants the three channels has to mask it off with
     * {@code value and 0xFFFFFF} first, and then divide by 65536 for red, by 256 and take the
     * remainder for green, and the remainder for blue. Dividing the full number by 65536 without
     * masking first does not give the red: it gives {@code 0xFF00} plus the red byte, so the
     * answer is 65280 too large.
     *
     * @return the color's rgb value.
     * @since 1.8.4
     */
    public int getColorValue() {
        return base.getTextureDiffuseColor();
    }

    /**
     * the shade this colour is drawn at in a firework, as a packed rgb number.
     * <p>
     * This is not a lightened version of {@link #getColorValue()}: it is a second constant the
     * game records for the dye, and which of the two is larger varies by colour. Taking the rgb
     * part of each, the firework shade is the smaller number for eleven of the sixteen and the
     * larger one for the other five, so a script must not treat it as brighter. The number this
     * returns also has a different shape from the other two — it is a plain {@code 0xRRGGBB}
     * with no alpha byte, so comparing it directly against {@link #getColorValue()} will always
     * put it far below, whatever the colours are. This is the one for anything a player sees as
     * a firework, and the other one for the dye itself.
     *
     * @return the color's variation when used in fireworks.
     * @since 1.8.4
     */
    public int getFireworkColor() {
        return base.getFireworkColor();
    }

    /**
     * the shade this colour is drawn at as sign text, as a packed rgb number.
     * <p>
     * Sign text is a third constant again, not a transformation of either of the other two, and
     * like {@link #getColorValue()} it comes back as {@code 0xFFRRGGBB} with the alpha byte set.
     * Taking the rgb part of each, the sign shade is the larger number for twelve of the
     * sixteen and the smaller one for the four the game gives no bright sign text to —
     * {@code blue}, {@code black}, {@code cyan} and {@code green} — so the same caution as for
     * the firework shade applies here. Only the colours that are used on signs matter for this,
     * and the rest still answer with whatever the game has recorded for them. Use
     * {@link #getColorValue()} for the dye itself and {@link #getFireworkColor()} for a
     * firework; this is specifically the sign text.
     *
     * @return the color's variation when used on signs.
     * @since 1.8.4
     */
    public int getSignColor() {
        return base.getTextColor();
    }

}
