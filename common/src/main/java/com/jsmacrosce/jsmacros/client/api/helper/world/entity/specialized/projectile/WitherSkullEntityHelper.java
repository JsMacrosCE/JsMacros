package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
*///? } else {
import net.minecraft.world.entity.projectile.WitherSkull;
//? }

/**
 * a wither skull, the projectile the wither's three heads fire.
 * <p>
  * There are two kinds and this one call is what tells them apart. A charged skull is the kind
 * the wither lobs out at whatever is nearby while it circles, and an ordinary one is the rare
 * aimed shot at a player, so the charged skull is the common one rather than the special one.
 * The flag is a property of the individual skull, so the same wither fires both and a script
 * has to look at each skull rather than at the boss.
 * example:
 * <pre>
 * const WitherSkullEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile.WitherSkullEntityHelper");
 * const skulls = World.getEntities(32, "wither_skull");
 * if (skulls !== null) {
 *   for (const entity of skulls) {
 *     const skull = WitherSkullEntityHelper.class.cast(entity);
 *     if (skull.isCharged()) {
 *       Chat.log("a charged one, which is the one worth dodging");
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
public class WitherSkullEntityHelper extends EntityHelper<WitherSkull> {

    public WitherSkullEntityHelper(WitherSkull base) {
        super(base);
    }

    /**
     * {@code true} if the wither skull is charged, {@code false} otherwise.
     * <p>
          * A charged skull is the one the wither lobs out at a random nearby point while it is
     * circling, and it is the common kind rather than the rare one: the aimed shot the wither
     * takes at a player is charged only about one time in a thousand, and then only from the
     * middle head. The difference is not only the damage. A charged skull has less inertia so
     * it carries further, and it breaks blocks it passes through, so it is the one that
     * wrecks the terrain around a fight.
     * <p>
     * The flag is {@code false} by default, so a summoned or placed skull answers
     * {@code false}, and nothing on this class changes it: the wither sets it as it fires.
     * example:
     * <pre>
     * const WitherSkullEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile.WitherSkullEntityHelper");
     * const skulls = World.getEntities(32, "wither_skull");
     * if (skulls !== null) {
     *   for (const entity of skulls) {
     *     const skull = WitherSkullEntityHelper.class.cast(entity);
     *     if (skull.isCharged()) {
     *       Chat.log(`a charged skull at ${entity.getPos()}, which breaks what it passes through`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the wither skull is charged, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCharged() {
        return base.isDangerous();
    }

}
