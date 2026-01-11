package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration;

import net.minecraft.world.entity.decoration.Painting;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
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
        return base.getVariant().unwrapKey().map(paintingVariantRegistryKey -> paintingVariantRegistryKey.location().toString()).orElse(null);
    }

}
