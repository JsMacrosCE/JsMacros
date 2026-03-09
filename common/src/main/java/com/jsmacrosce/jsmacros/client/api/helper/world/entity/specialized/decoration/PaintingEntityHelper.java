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
     * @return the width of this painting.
     * @since 1.8.4
     */
    public int getWidth() {
        return base.getVariant().value().width();
    }

    /**
     * @return the height of this painting.
     * @since 1.8.4
     */
    public int getHeight() {
        return base.getVariant().value().height();
    }

    /**
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
