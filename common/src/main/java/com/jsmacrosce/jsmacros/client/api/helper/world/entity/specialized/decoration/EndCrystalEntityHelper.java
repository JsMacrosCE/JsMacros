package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration;

import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

/**
 * Exposes an end crystal's base-display flag and optional beam target.
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class EndCrystalEntityHelper extends EntityHelper<EndCrystal> {

    /** Wraps the supplied end crystal reference.
     * @param base the end crystal to wrap
     */
    public EndCrystalEntityHelper(EndCrystal base) {
        super(base);
    }

    /**
     * Returns whether the crystal is configured to display its base.
     *
     * @return {@code true} if the crystal displays its base.
     * @since 1.8.4
     */
    public boolean isNatural() {
        return base.showsBottom();
    }

    /**
     * Returns the configured beam target, or {@code null} if none is set.
     *
     * @return the configured target, or {@code null} if none is set.
     * @since 1.8.4
     */
    @Nullable
    public BlockPosHelper getBeamTarget() {
        return base.getBeamTarget() == null ? null : new BlockPosHelper(base.getBeamTarget());
    }

}
