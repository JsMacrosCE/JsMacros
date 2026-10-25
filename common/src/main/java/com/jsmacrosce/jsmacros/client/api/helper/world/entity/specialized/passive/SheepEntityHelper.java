package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.sheep.Sheep;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.DyeColorHelper;

/**
 * Provides access to a sheep's wool state and color. {@link #isSheared()} reports whether its
 * fleece is off; {@link #isShearable()} additionally requires that it is alive and not a baby.
 * {@link #isJeb()} checks for the exact custom name {@code jeb_}.
 * <pre>
 * const sheep = World.getEntities(32, "sheep");
 * if (sheep !== null) {
 *   for (const entity of sheep) {
 *     if (!(entity instanceof EntityHelpers.Sheep)) continue;
 *     Chat.log(`${entity.getColor().getName()}, sheared ${entity.isSheared()}, `
 *       + `shearable ${entity.isShearable()}, rainbow ${entity.isJeb()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class SheepEntityHelper extends AnimalEntityHelper<Sheep> {

    /** Wraps the supplied sheep and retains its reference for this helper.
     *
     * @param base sheep entity to wrap
     */
    public SheepEntityHelper(Sheep base) {
        super(base);
    }

    /**
     * Whether this sheep's fleece is off. Its wool color is retained while sheared.
     *
     * @return {@code true} if this sheep is sheared, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSheared() {
        return base.isSheared();
    }

    /**
     * Whether this sheep is alive, is not a baby, and is not sheared.
     *
     * @return {@code true} if this sheep can be sheared, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isShearable() {
        return base.readyForShearing();
    }

    /**
     * The sheep's wool color, retained while sheared.
     *
     * @return the color of this sheep.
     * @since 1.8.4
     */
    public DyeColorHelper getColor() {
        return new DyeColorHelper(base.getColor());
    }

    /**
     * Whether the sheep has the exact custom name {@code jeb_}, which makes its rendered wool
     * cycle through colors.
     *
     * @return {@code true} if the sheep's custom name is exactly {@code jeb_}; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isJeb() {
        return base.hasCustomName() && "jeb_".equals(base.getName().getString());
    }

}
