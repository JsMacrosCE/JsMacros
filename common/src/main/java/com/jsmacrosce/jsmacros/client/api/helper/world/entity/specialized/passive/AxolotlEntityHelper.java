package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.axolotl.Axolotl;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;

/**
 * Provides access to an axolotl's variant, playing-dead state, and bucket-origin flag.
 * <pre>
 * const AxolotlEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.AxolotlEntityHelper");
 * const axolotls = World.getEntities(32, "axolotl");
 * if (axolotls !== null) {
 *   for (const entity of axolotls) {
 *     const axolotl = AxolotlEntityHelper.class.cast(entity);
 *     Chat.log(`variant ${axolotl.getVariantName()} (${axolotl.getVariantId()}), `
 *       + `from a bucket ${axolotl.isFromBucket()}, `
 *       + `playing dead ${axolotl.isPlayingDead()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class AxolotlEntityHelper extends AnimalEntityHelper<Axolotl> {

    /**
     * Wraps the supplied axolotl and retains its reference for this helper.
     *
     * @param base the axolotl entity to wrap
     */
    public AxolotlEntityHelper(Axolotl base) {
        super(base);
    }

    /**
     * Returns the numeric identifier of this axolotl's variant.
     *
     * @return the id of this axolotl's variant.
     * @since 1.8.4
     */
    public int getVariantId() {
        return base.getVariant().getId();
    }

    /**
     * Returns the game name of this axolotl's variant.
     *
     * @return the name of this axolotl's variant.
     * @since 1.8.4
     */
    @DocletReplaceReturn("AxolotlVariant")
    public String getVariantName() {
        return base.getVariant().getName();
    }

    /**
     * Returns whether the axolotl is playing dead.
     *
     * @return {@code true} if the axolotl is playing dead, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isPlayingDead() {
        return base.isPlayingDead();
    }

    /**
     * Returns whether this axolotl has the from-bucket flag set.
     *
     * @return {@code true} if the axolotl came from a bucket, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isFromBucket() {
        return base.fromBucket();
    }

}
