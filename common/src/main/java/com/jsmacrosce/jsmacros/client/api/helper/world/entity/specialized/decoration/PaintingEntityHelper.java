package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration;

import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.decoration.painting.Painting;
*///? } else {
import net.minecraft.world.entity.decoration.Painting;
//? }

/**
 * a painting, the entity that hangs a picture on a wall.
 * <p>
 * The size and the art both come from the painting variant, which is the registry entry for
 * the particular picture, so {@link #getWidth() getWidth()} and {@link #getHeight()
 * getHeight()} describe the art rather than the entity: two paintings of the same art are the
 * same size whichever wall they are on, and the size follows the art rather than the wall.
 * <p>
 * The variant is also where the identifier comes from, and it is the only part of a painting
 * that names it. The painting itself has no name of its own and hangs flush against the wall
 * on the block it is attached to, so the width and the height are how much wall it covers.
 * example:
 * <pre>
 * const PaintingEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.PaintingEntityHelper");
 * const paintings = World.getEntities(32, "painting");
 * if (paintings !== null) {
 *   for (const entity of paintings) {
 *     const painting = PaintingEntityHelper.class.cast(entity);
 *     // the art decides the size, so this is a property of the picture not of the wall
 *     Chat.log(`${painting.getIdentifier()}: ${painting.getWidth()} by ${painting.getHeight()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class PaintingEntityHelper extends EntityHelper<Painting> {

    public PaintingEntityHelper(Painting base) {
        super(base);
    }

    /**
     * the width of this painting, in blocks.
     * <p>
     * This is the horizontal extent of the art, so it is how far the painting runs along the
     * wall rather than out from it: the depth is always one sixteenth of a block whatever this
     * says, because a painting is flat against the wall. The value comes from the painting's
     * variant and is between 1 and 16, and it never changes once the painting is placed.
     * example:
     * <pre>
     * const PaintingEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.PaintingEntityHelper");
     * const paintings = World.getEntities(32, "painting");
     * if (paintings !== null) {
     *   for (const entity of paintings) {
     *     const painting = PaintingEntityHelper.class.cast(entity);
     *     // the wall it needs, rather than the depth it sticks out
     *     Chat.log(`covers ${painting.getWidth()} blocks of wall`);
     *   }
     * }
     * </pre>
     *
     * @return the width of this painting.
     * @since 1.8.4
     */
    public int getWidth() {
        return base.getVariant().value().width();
    }

    /**
     * the height of this painting, in blocks.
     * <p>
     * The vertical counterpart to {@link #getWidth() getWidth()}, and like it this is the size
     * of the art rather than anything about the wall. The two are independent: a painting can
     * be one block wide and sixteen tall or the other way round, and the range is 1 to 16 for
     * each of them separately.
     * example:
     * <pre>
     * const PaintingEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.PaintingEntityHelper");
     * const paintings = World.getEntities(32, "painting");
     * if (paintings !== null) {
     *   for (const entity of paintings) {
     *     const painting = PaintingEntityHelper.class.cast(entity);
     *     // which way round the art is, which varies from one picture to the next
     *     if (painting.getHeight() > painting.getWidth()) {
     *       Chat.log(`a tall one, ${painting.getHeight()} up and ${painting.getWidth()} across`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the height of this painting.
     * @since 1.8.4
     */
    public int getHeight() {
        return base.getVariant().value().height();
    }

    /**
     * the identifier of this painting's art, such as {@code minecraft:kebab}.
     * <p>
     * This is the registry entry the picture is stored under rather than a file name or the
     * name of the painting item, and it is the only thing that distinguishes one painting from
     * another, since two paintings of the same art share every other value. It is {@code null}
     * rather than an empty string in the case where the art is not a registry entry, which
     * does not normally happen for a painting that is in the world.
     * example:
     * <pre>
     * const PaintingEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.PaintingEntityHelper");
     * const paintings = World.getEntities(32, "painting");
     * if (paintings !== null) {
     *   for (const entity of paintings) {
     *     const painting = PaintingEntityHelper.class.cast(entity);
     *     // the art id, with the namespace, so it survives a rename of the picture
     *     const id = painting.getIdentifier();
     *     if (id !== null) {
     *       Chat.log(`art is ${id}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the identifier of this painting's art.
     * @since 1.8.4
     */
    @Nullable
    @DocletReplaceReturn("PaintingId")
    public String getIdentifier() {
        return base.getVariant().unwrapKey().map(paintingVariantRegistryKey ->
                paintingVariantRegistryKey
                        //? if >=1.21.11 {
                        /*.identifier()
                        *///? } else {
                        .location()
                        //? }
                        .toString()
        ).orElse(null);
    }

}
