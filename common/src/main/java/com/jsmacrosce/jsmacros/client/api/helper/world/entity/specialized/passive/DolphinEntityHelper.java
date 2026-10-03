package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;
import org.jetbrains.annotations.Nullable;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.dolphin.Dolphin;
*///? } else {
import net.minecraft.world.entity.animal.Dolphin;
//?}

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class DolphinEntityHelper extends MobEntityHelper<Dolphin> {

    public DolphinEntityHelper(Dolphin base) {
        super(base);
    }

    /**
     * @return {@code true} if the dolphin has a fish in its mouth, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasFish() {
        return base.gotFish();
    }

    /**
     * @return the position of the treasure the dolphin is looking for, or null if not available on the client.
     * @since 1.8.4
     */
    @Nullable
    public BlockPosHelper getTreasurePos() {
        return base.treasurePos == null ? null : new BlockPosHelper(base.treasurePos);
    }

    /**
     * @return the moisture level of the dolphin.
     * @since 1.8.4
     */
    public int getMoistness() {
        return base.getMoistnessLevel();
    }

}
