package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.panda.Panda;
*///? } else {
import net.minecraft.world.entity.animal.Panda;
//?}

/**
 * Provides access to a panda's genes and current behavior states. Its main and hidden genes
 * resolve to {@code normal} when a recessive main gene ({@code brown} or {@code weak}) does
 * not match the hidden gene. {@link #isIdle() isIdle} is false while the panda is on its
 * back, scared by a thunderstorm, eating, rolling, or sitting.
 * <pre>
 * const pandas = World.getEntities(32, "panda");
 * if (pandas !== null) {
 *   for (const entity of pandas) {
 *     if (!(entity instanceof EntityHelpers.Panda)) continue;
 *     Chat.log(`main ${entity.getMainGeneName()}, hidden ${entity.getHiddenGeneName()}`);
 *     Chat.log(`  lazy ${entity.isLazy()}, worried ${entity.isWorried()}, `
 *       + `brown ${entity.isBrown()}, weak ${entity.isWeak()}, `
 *       + `aggressive ${entity.isAttacking()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class PandaEntityHelper extends AnimalEntityHelper<Panda> {

    /** Wraps the supplied panda entity and retains its reference for this helper.
     *
     * @param base panda entity to wrap
     */
    public PandaEntityHelper(Panda base) {
        super(base);
    }

    /**
     * Returns the numeric id of the main gene ({@code normal} is {@code 0}; {@code
     * aggressive} is {@code 6}).
     *
     * @return the id of this panda's main gene.
     * @since 1.8.4
     */
    public int getMainGene() {
        return base.getMainGene().getId();
    }

    /**
     * Returns the serialized name of the main gene, not the resolved phenotype.
     *
     * @return the name of this panda's main gene.
     * @since 1.8.4
     */
    @DocletReplaceReturn("PandaGene")
    public String getMainGeneName() {
        return base.getMainGene().getSerializedName();
    }

    /**
     * Whether the main gene is recessive; only {@code brown} and {@code weak} are recessive.
     *
     * @return {@code true} if this panda's main gene is recessive, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isMainGeneRecessive() {
        return base.getMainGene().isRecessive();
    }

    /**
     * Returns the numeric id of the hidden gene ({@code normal} is {@code 0}; {@code
     * aggressive} is {@code 6}).
     *
     * @return the id of this panda's hidden gene.
     * @since 1.8.4
     */
    public int getHiddenGene() {
        return base.getHiddenGene().getId();
    }

    /**
     * Returns the serialized name of the hidden gene.
     *
     * @return the name of this panda's hidden gene.
     * @since 1.8.4
     */
    @DocletReplaceReturn("PandaGene")
    public String getHiddenGeneName() {
        return base.getHiddenGene().getSerializedName();
    }

    /**
     * Whether the hidden gene is recessive; only {@code brown} and {@code weak} are recessive.
     *
     * @return {@code true} if this panda's hidden gene is recessive, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isHiddenGeneRecessive() {
        return base.getHiddenGene().isRecessive();
    }

    /**
     * Whether the panda can perform an action. This is false while it is on its back, scared
     * by a thunderstorm, eating, rolling, or sitting.
     *
     * @return {@code true} if this panda is not busy with something else, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isIdle() {
        return base.canPerformAction();
    }

    /**
     * Whether the panda is currently sneezing.
     *
     * @return {@code true} if this panda is currently sneezing, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSneezing() {
        return base.isSneezing();
    }

    /**
     * Whether the panda is currently rolling.
     *
     * @return {@code true} if this panda is rolling, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isPlaying() {
        return base.isRolling();
    }

    /**
     * Whether the panda is currently sitting. This is a current pose/state, not a record of
     * who caused it.
     *
     * @return {@code true} if this panda is sitting, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSitting() {
        return base.isSitting();
    }

    /**
     * Whether the panda is currently on its back. This state also makes {@link #isIdle()
     * isIdle} return {@code false}.
     *
     * @return {@code true} if this panda is lying on its back, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isLyingOnBack() {
        return base.isOnBack();
    }

    /**
     * Whether the genes resolve to {@code lazy}. Recessive {@code brown} or {@code weak}
     * main genes resolve to {@code normal} when the hidden gene differs.
     *
     * @return {@code true} if this panda's genes make it lazy, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isLazy() {
        return base.isLazy();
    }

    /**
     * Whether the genes resolve to {@code worried}.
     *
     * @return {@code true} if this panda's genes make it worried, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isWorried() {
        return base.isWorried();
    }

    /**
     * Whether the panda is worried while its level is thundering. This does not indicate
     * that it has run off.
     *
     * @return {@code true} if this panda is scared by an active thunderstorm, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isScaredByThunderstorm() {
        return base.isScared();
    }

    /**
     * Whether the genes resolve to {@code playful}.
     *
     * @return {@code true} if this panda's genes make it playful, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isPlayful() {
        return base.isPlayful();
    }

    /**
     * Whether the genes resolve to {@code brown}. Since brown is recessive, both genes must
     * be {@code brown}; otherwise a brown main gene resolves to {@code normal}.
     *
     * @return {@code true} if this panda's genes make it brown, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isBrown() {
        return base.isBrown();
    }

    /**
     * Whether the genes resolve to {@code weak}. Since weak is recessive, both genes must
     * be {@code weak}.
     *
     * @return {@code true} if this panda's genes make it weak, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isWeak() {
        return base.isWeak();
    }

    /**
     * Whether the genes resolve to {@code aggressive}; this reports the gene-derived
     * variant, not whether the panda is currently attacking.
     *
     * @return {@code true} if this panda's genes make it aggressive, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isAttacking() {
        return base.isAggressive();
    }

}
