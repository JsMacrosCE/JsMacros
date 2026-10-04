package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinAbstractPiglinEntity;

/**
 * the piglin family, and the one thing the two kinds of piglin have in common that is worth
 * asking about.
 * <p>
 * There are two of them: the ordinary piglin, which reaches a script as
 * {@code PiglinEntityHelper}, and the piglin brute, which reaches it as this class. The
 * brute is the one case here with nothing but the conversion flag on it, so this is a thin
 * class on purpose.
 * <p>
 * What the conversion actually depends on is not only the flag:
 * {@link #canBeZombified() canBeZombified} is the first term of the game's own check, and
 * the other two — the piglin having its AI on, and the dimension's zombification rule — are
 * not on this class.
 * example:
 * <pre>
 * const AbstractPiglinEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.AbstractPiglinEntityHelper");
 * const brute = World.getEntities(32, "piglin_brute");
 * if (brute !== null) {
 *   for (const entity of brute) {
 *     // a brute reaches a script as this class, a piglin as PiglinEntityHelper
 *     const piglin = AbstractPiglinEntityHelper.class.cast(entity);
 *     if (piglin.canBeZombified()) {
 *       Chat.log(`the brute at ${piglin.getPos()} carries no immunity flag`);
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class AbstractPiglinEntityHelper<T extends AbstractPiglin> extends MobEntityHelper<T> {

    public AbstractPiglinEntityHelper(T base) {
        super(base);
    }

    /**
     * Whether this piglin is <em>not</em> carrying the immunity flag, so this is the flag
     * read backwards. The game defines the flag as {@code false} on a new piglin and only
     * ever writes it back from saved data, so an ordinary piglin answers {@code true}.
     * <p>
     * The dimension is not consulted here. The game's own decision to turn a piglin is this
     * flag plus the piglin having its AI on plus the dimension's zombification rule, and
     * this is the first of those three on its own.
     * example:
     * <pre>
     * const PiglinEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.PiglinEntityHelper");
     * const piglins = World.getEntities(32, "piglin");
     * if (piglins !== null) {
     *   for (const entity of piglins) {
     *     const piglin = PiglinEntityHelper.class.cast(entity);
     *     // this is only the flag; whether the piglin actually turns is the
     *     // dimension's call, and this never sees the dimension
     *     if (piglin.canBeZombified()) {
     *       Chat.log(`a piglin at ${piglin.getPos()} carries no immunity flag`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this piglin is not flagged immune to zombification,
     * {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean canBeZombified() {
        return !((MixinAbstractPiglinEntity) base).invokeIsImmuneToZombification();
    }

}
