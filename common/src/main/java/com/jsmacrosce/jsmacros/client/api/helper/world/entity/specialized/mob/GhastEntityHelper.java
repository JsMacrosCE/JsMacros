package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Ghast;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * the ghast, and the one moment of its cycle a script can watch for.
 * <p>
 * The game syncs a single flag for the ghast's attack, so this is a thin class on purpose:
 * {@link #isShooting() isShooting} is that flag, and nothing else about a ghast is exposed
 * here.
 * example:
 * <pre>
 * const GhastEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.GhastEntityHelper");
 * const ghasts = World.getEntities(48, "ghast");
 * if (ghasts !== null) {
 *   for (const entity of ghasts) {
 *     const ghast = GhastEntityHelper.class.cast(entity);
 *     if (ghast.isShooting()) {
 *       Chat.log(`a ghast at ${ghast.getPos()} has locked on`);
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
public class GhastEntityHelper extends MobEntityHelper<Ghast> {

    public GhastEntityHelper(Ghast base) {
        super(base);
    }

    /**
     * Whether the ghast has its attack flag raised, which is the wind-up before it fires:
     * the game raises it once the ghast has been charging for more than a handful of ticks
     * and lowers it when the shot goes out or the target is lost.
     * <p>
     * The flag the game sets is named after charging rather than shooting, so this answers
     * {@code true} for the stretch before the fireball and {@code false} on the tick the
     * fireball is actually released. It is the lead-up a script gets, not the shot.
     * example:
     * <pre>
     * const GhastEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.GhastEntityHelper");
     * const ghasts = World.getEntities(48, "ghast");
     * if (ghasts !== null) {
     *   for (const entity of ghasts) {
     *     const ghast = GhastEntityHelper.class.cast(entity);
     *     // the window a script gets is the wind-up, not the fireball itself
     *     if (ghast.isShooting()) {
     *       Chat.log(`a ghast at ${ghast.getPos()} is winding up`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this ghast is currently about to shoot a fireball, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isShooting() {
        return base.isCharging();
    }

}
