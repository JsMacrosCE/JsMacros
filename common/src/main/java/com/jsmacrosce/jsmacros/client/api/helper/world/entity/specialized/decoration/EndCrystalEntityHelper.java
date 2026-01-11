package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration;

import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class EndCrystalEntityHelper extends EntityHelper<EndCrystal> {

    public EndCrystalEntityHelper(EndCrystal base) {
        super(base);
    }

    /**
     * Naturally generated end crystals will have a bedrock base, while player placed ones will
     * not.
     *
     * @return {@code true} if the end crystal was not placed by a player, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isNatural() {
        return base.showsBottom();
    }

    /**
     * @return the target of the crystal's beam, or {@code null} if there is none.
     * @since 1.8.4
     */
    @Nullable
    public BlockPosHelper getBeamTarget() {
        return base.getBeamTarget() == null ? null : new BlockPosHelper(base.getBeamTarget());
    }

}
