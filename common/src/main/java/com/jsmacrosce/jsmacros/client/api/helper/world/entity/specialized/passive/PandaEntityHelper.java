package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.panda.Panda;
*///? } else {
import net.minecraft.world.entity.animal.Panda;
//?}

/**
 * the panda, which is the only mob here with two genes instead of one appearance.
 * <p>
 * A panda carries a main gene and a hidden gene, and everything that makes it look and
 * behave like itself comes out of resolving those two. A recessive gene - and only {@code
 * brown} and {@code weak} are recessive - only wins when the hidden gene is the same one;
 * otherwise it is swallowed and the panda comes out as a plain {@code normal}. That is why
 * {@link #isMainGeneRecessive() isMainGeneRecessive} and {@link #isHiddenGeneRecessive()
 * isHiddenGeneRecessive} do not by themselves tell a script what the panda looks like, and
 * why the six predicates further down this class are the ones to read.
 * <p>
 * There is a third reading, {@link #isIdle() isIdle}, which is not about genes at all: it is
 * the game asking whether the panda is free to start something new, and it goes down while
 * the panda is on its back, eating, rolling, sitting, or panicked by a thunderstorm.
 * example:
 * <pre>
 * const PandaEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.PandaEntityHelper");
 * const pandas = World.getEntities(32, "panda");
 * if (pandas !== null) {
 *   for (const entity of pandas) {
 *     const panda = PandaEntityHelper.class.cast(entity);
 *     Chat.log(`main ${panda.getMainGeneName()}, hidden ${panda.getHiddenGeneName()}`);
 *     Chat.log(`  lazy ${panda.isLazy()}, worried ${panda.isWorried()}, `
 *       + `brown ${panda.isBrown()}, weak ${panda.isWeak()}, `
 *       + `aggressive ${panda.isAttacking()}`);
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

    public PandaEntityHelper(Panda base) {
        super(base);
    }

    /**
     * The number the game stores the main gene as, from {@code 0} for {@code normal} to
     * {@code 6} for {@code aggressive}. The id rather than the name, so it is the figure to
     * compare against.
     *
     * @return the id of this panda's main gene.
     * @since 1.8.4
     */
    public int getMainGene() {
        return base.getMainGene().getId();
    }

    /**
     * The main gene by name: {@code normal}, {@code lazy}, {@code worried}, {@code playful},
     * {@code brown}, {@code weak} or {@code aggressive}. That is the gene itself, not what
     * the two genes together came out as.
     *
     * @return the name of this panda's main gene.
     * @since 1.8.4
     */
    @DocletReplaceReturn("PandaGene")
    public String getMainGeneName() {
        return base.getMainGene().getSerializedName();
    }

    /**
     * Whether the main gene is a recessive one. Only {@code brown} and {@code weak} are, so
     * a panda answering {@code true} here is one whose main gene may be overridden by the
     * hidden one.
     *
     * @return {@code true} if this panda's main gene is recessive, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isMainGeneRecessive() {
        return base.getMainGene().isRecessive();
    }

    /**
     * The number the game stores the hidden gene as, from {@code 0} for {@code normal} to
     * {@code 6} for {@code aggressive}.
     *
     * @return the id of this panda's hidden gene.
     * @since 1.8.4
     */
    public int getHiddenGene() {
        return base.getHiddenGene().getId();
    }

    /**
     * The hidden gene by name, from the same seven as the main one. The game keeps it hidden
     * because a recessive main gene and a matching hidden gene is what produces a brown or a
     * weak panda.
     *
     * @return the name of this panda's hidden gene.
     * @since 1.8.4
     */
    @DocletReplaceReturn("PandaGene")
    public String getHiddenGeneName() {
        return base.getHiddenGene().getSerializedName();
    }

    /**
     * Whether the hidden gene is a recessive one, which only matters when the main gene is
     * recessive too and they are the same gene.
     *
     * @return {@code true} if this panda's hidden gene is recessive, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isHiddenGeneRecessive() {
        return base.getHiddenGene().isRecessive();
    }

    /**
     * Whether the panda is free to start doing something, which the game asks before most
     * of what it wants the panda to do. It is the opposite of being busy rather than a
     * behaviour of its own: it goes down while the panda is lying on its back, eating,
     * rolling, sitting, or panicked by a thunderstorm, and comes back up when it is not.
     * example:
     * <pre>
     * const PandaEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.PandaEntityHelper");
     * const pandas = World.getEntities(24, "panda");
     * if (pandas !== null) {
     *   for (const entity of pandas) {
     *     const panda = PandaEntityHelper.class.cast(entity);
     *     if (panda.isIdle()) {
     *       Chat.log("that panda is up and about");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this panda is not busy with something else, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isIdle() {
        return base.canPerformAction();
    }

    /**
     * Whether the panda is sneezing, which is the routine the gene that is {@code worried}
     * spends its day doing. It comes and goes on the panda's own rather than being set by
     * anything a player does.
     *
     * @return {@code true} if this panda is currently sneezing, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSneezing() {
        return base.isSneezing();
    }

    /**
     * Whether the panda is rolling, which is what a playful panda - or any baby panda,
     * whatever its genes - spends a moment doing before going back to what it was. It is
     * the same state {@link #isIdle() isIdle} goes down for.
     *
     * @return {@code true} if this panda is rolling, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isPlaying() {
        return base.isRolling();
    }

    /**
     * Whether the panda has been told to sit and is sitting. Nothing in the game sets this
     * by itself for a panda; it is a thing a player does, and it survives until something
     * else takes the panda's attention.
     *
     * @return {@code true} if this panda is sitting, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSitting() {
        return base.isSitting();
    }

    /**
     * Whether the panda is on its back. This is what a panda looks like after a bad fall or
     * when it is not feeling itself, and it is also one of the states that keeps {@link
     * #isIdle() isIdle} down.
     *
     * @return {@code true} if this panda is lying on its back, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isLyingOnBack() {
        return base.isOnBack();
    }

    /**
     * Whether the two genes together came out as {@code lazy}, which is the panda that eats
     * and does nothing much. A recessive main gene of {@code brown} or {@code weak} with a
     * different hidden gene does not count: that panda is {@code normal}.
     * example:
     * <pre>
     * const PandaEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.PandaEntityHelper");
     * const pandas = World.getEntities(32, "panda");
     * if (pandas !== null) {
     *   for (const entity of pandas) {
     *     const panda = PandaEntityHelper.class.cast(entity);
     *     if (panda.isLazy()) {
     *       Chat.log(`a lazy one, main gene ${panda.getMainGeneName()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this panda's genes make it lazy, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isLazy() {
        return base.isLazy();
    }

    /**
     * Whether the two genes together came out as {@code worried}, which is the panda that
     * spends its day sneezing and panicking. It is also the panda that {@link
     * #isScaredByThunderstorm() isScaredByThunderstorm} can come true for, since that asks
     * for this as well as for a thunderstorm.
     *
     * @return {@code true} if this panda's genes make it worried, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isWorried() {
        return base.isWorried();
    }

    /**
     * Whether the panda has run off because of a thunderstorm. It asks for two things rather
     * than one - a worried panda and a thunderstorm actually going on - so a worried panda
     * on a clear day answers {@code false} here and a worried one in a storm answers
     * {@code true}.
     *
     * @return {@code true} if this panda is scared by an active thunderstorm, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isScaredByThunderstorm() {
        return base.isScared();
    }

    /**
     * Whether the two genes together came out as {@code playful}, which is the panda that
     * rolls about and clambers rather than doing one thing all day.
     *
     * @return {@code true} if this panda's genes make it playful, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isPlayful() {
        return base.isPlayful();
    }

    /**
     * Whether the two genes together came out as {@code brown}. Because brown is recessive,
     * this needs the main gene to be brown and the hidden gene to match it; a panda with a
     * brown main gene and anything else hidden is {@code normal} and answers {@code false}
     * here.
     * example:
     * <pre>
     * const PandaEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.PandaEntityHelper");
     * const pandas = World.getEntities(32, "panda");
     * if (pandas !== null) {
     *   for (const entity of pandas) {
     *     const panda = PandaEntityHelper.class.cast(entity);
     *     if (panda.isBrown()) {
     *       Chat.log(`both genes brown: ${panda.getMainGeneName()}`
     *         + ` and ${panda.getHiddenGeneName()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this panda's genes make it brown, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isBrown() {
        return base.isBrown();
    }

    /**
     * Whether the two genes together came out as {@code weak}. Like brown it is recessive,
     * so it needs the main gene to be weak and the hidden gene to match. A weak panda is
     * also the one the game gives far less health than the rest.
     *
     * @return {@code true} if this panda's genes make it weak, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isWeak() {
        return base.isWeak();
    }

    /**
     * Whether the two genes together came out as {@code aggressive}, which is the panda that
     * goes after anybody who comes near it. It is a gene rather than a state: a panda that is
     * already mid-attack on a clear day still answers {@code true}, and a playful one
     * answers {@code false} however much it is rolling around.
     * example:
     * <pre>
     * const PandaEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.PandaEntityHelper");
     * const pandas = World.getEntities(32, "panda");
     * if (pandas !== null) {
     *   for (const entity of pandas) {
     *     const panda = PandaEntityHelper.class.cast(entity);
     *     if (panda.isAttacking()) {
     *       Chat.log(`leave that one alone at ${panda.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this panda's genes make it aggressive, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isAttacking() {
        return base.isAggressive();
    }

}
