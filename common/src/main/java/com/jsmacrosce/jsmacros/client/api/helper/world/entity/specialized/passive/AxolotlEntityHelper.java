package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.axolotl.Axolotl;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;

/**
 * the axolotl, which comes in five colour variants and can be talked into playing dead.
 * <p>
 * The variant is available twice over: {@link #getVariantId() getVariantId} gives the small
 * number the game stores and {@link #getVariantName() getVariantName} gives the name, and
 * both name the same five - {@code lucy}, {@code wild}, {@code gold}, {@code cyan} and
 * {@code blue}. Four of those are the common ones and only the blue is the rare variant,
 * which is not something this class reports.
 * <p>
 * An axolotl caught with a bucket keeps that fact for life, which is what {@link
 * #isFromBucket() isFromBucket} reports, and it is why the game will not let one despawn.
 * example:
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

    public AxolotlEntityHelper(Axolotl base) {
        super(base);
    }

    /**
     * The number the game stores the variant as, running from {@code 0} for {@code lucy} up
     * to {@code 4} for the rare blue one. It is the id rather than the name, so it is the
     * figure to compare against, and {@link #getVariantName() getVariantName} is the one to
     * print.
     *
     * @return the id of this axolotl's variant.
     * @since 1.8.4
     */
    public int getVariantId() {
        return base.getVariant().getId();
    }

    /**
     * The variant name as the game writes it in its save data: one of {@code lucy},
     * {@code wild}, {@code gold}, {@code cyan} or {@code blue}. It is the bare name with no
     * namespace on the front, unlike the cat and frog variants elsewhere in this package.
     *
     * @return the name of this axolotl's variant.
     * @since 1.8.4
     */
    @DocletReplaceReturn("AxolotlVariant")
    public String getVariantName() {
        return base.getVariant().getName();
    }

    /**
     * Whether the axolotl has gone limp and is lying on its side. It is the game's own
     * playing-dead state, which it falls into when badly hurt and leaves again on its own,
     * so it is a few seconds rather than something a player sets.
     *
     * @return {@code true} if the axolotl is playing dead, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isPlayingDead() {
        return base.isPlayingDead();
    }

    /**
     * Whether this axolotl was caught with a bucket rather than found in the world. The game
     * keeps the flag for good and uses it to stop the axolotl being unloaded, so one of
     * these is never a despawn candidate.
     *
     * @return {@code true} if the axolotl came from a bucket, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isFromBucket() {
        return base.fromBucket();
    }

}
