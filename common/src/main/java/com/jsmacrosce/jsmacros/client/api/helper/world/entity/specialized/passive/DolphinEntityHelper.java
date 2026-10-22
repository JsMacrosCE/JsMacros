package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;
import org.jetbrains.annotations.Nullable;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.dolphin.Dolphin;
*///? } else {
import net.minecraft.world.entity.animal.Dolphin;
//?}

/**
 * Provides access to dolphin-specific state, including whether it carries a fish,
 * its treasure destination, and its moisture level.
 * <p>
 * {@link #getTreasurePos()} may return {@code null}; its destination is not synchronized
 * to clients.
 * <p>
 * <pre>
 * const DolphinEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.DolphinEntityHelper");
 * const dolphins = World.getEntities(32, "dolphin");
 * if (dolphins !== null) {
 *   for (const entity of dolphins) {
 *     const dolphin = DolphinEntityHelper.class.cast(entity);
 *     Chat.log(`${dolphin.getType()}: carrying a fish ${dolphin.hasFish()}, `
 *       + `${dolphin.getMoistness()} ticks of water left`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class DolphinEntityHelper extends MobEntityHelper<Dolphin> {

    /**
     * Wraps the dolphin and retains its reference.
     *
     * @param base dolphin entity to wrap
     */
    public DolphinEntityHelper(Dolphin base) {
        super(base);
    }

    /**
     * Whether the dolphin's synchronized fish-carrying flag is set.
     *
     * @return {@code true} if the dolphin has a fish in its mouth, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasFish() {
        return base.gotFish();
    }

    /**
     * Returns the dolphin's treasure destination, if one is currently set. The position
     * is not synchronized to clients.
     *
     * @return the destination, or {@code null} if none is set.
     * @since 1.8.4
     */
    @Nullable
    public BlockPosHelper getTreasurePos() {
        return base.treasurePos == null ? null : new BlockPosHelper(base.treasurePos);
    }

    /**
     * Returns the dolphin's remaining moisture, in ticks. It resets to 2400 in water or
     * rain; while outside both, it decreases each tick and the dolphin takes damage at zero.
     *
     * @return the remaining moisture in ticks.
     * @since 1.8.4
     */
    public int getMoistness() {
        return base.getMoistnessLevel();
    }

}
