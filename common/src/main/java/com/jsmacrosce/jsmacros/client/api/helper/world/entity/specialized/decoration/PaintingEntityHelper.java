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
 * Reads a painting's variant dimensions and registry identifier.
 * <pre>
 * const paintings = World.getEntities(32, "painting");
 * if (paintings !== null) {
 *   for (const entity of paintings) {
 *     if (!(entity instanceof EntityHelpers.Painting)) continue;
 *     Chat.log(`${entity.getIdentifier()}: ${entity.getWidth()} by ${entity.getHeight()}`);
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

    /** Wraps the supplied painting reference.
     * @param base painting to wrap
     */
    public PaintingEntityHelper(Painting base) {
        super(base);
    }

    /**
     * Returns the painting variant's width in blocks.
     *
     * @return the variant width.
     * @since 1.8.4
     */
    public int getWidth() {
        return base.getVariant().value().width();
    }

    /**
     * Returns the painting variant's height in blocks.
     *
     * @return the variant height.
     * @since 1.8.4
     */
    public int getHeight() {
        return base.getVariant().value().height();
    }

    /**
     * Returns the painting variant's registry identifier, or {@code null} if the variant has no registry key.
     *
     * @return the registry identifier, or {@code null} if the variant has no registry key.
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
