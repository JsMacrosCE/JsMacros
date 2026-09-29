package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinOcelotEntity;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.feline.Ocelot;
*///? } else {
import net.minecraft.world.entity.animal.Ocelot;
//?}

/**
 * the ocelot, which is not a tameable animal and never has been: it is won over by being fed
 * and it stays won over, and that is the whole of {@link #isTrusting() isTrusting}.
 * <p>
 * Unlike the cat beside it in this package, an ocelot has no tame flag, no owner and no collar.
 * What the trust flag actually changes is narrow and worth knowing: while it is down the game
 * has the ocelot running from nearby players, and raising it takes that goal away. Nothing
 * else about the mob's behaviour is different.
 * example:
 * <pre>
 * const OcelotEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.OcelotEntityHelper");
 * const ocelots = World.getEntities(32, "ocelot");
 * if (ocelots !== null) {
 *   for (const entity of ocelots) {
 *     const ocelot = OcelotEntityHelper.class.cast(entity);
 *     Chat.log(`${ocelot.getType()} at ${ocelot.getPos()}, `
 *       + `trusting ${ocelot.isTrusting()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class OcelotEntityHelper extends AnimalEntityHelper<Ocelot> {

    public OcelotEntityHelper(Ocelot base) {
        super(base);
    }

    /**
     * Whether this ocelot has been won over, which a player does by feeding it.
     * <p>
     * Ocelots trust players after being fed with cod or salmon. Once the flag is up the game
     * takes away the goal that makes the ocelot flee nearby players, and that is the only
     * thing the flag is used for: it does not give the ocelot an owner, does not let it be
     * told to sit, and does not put a collar on it.
     * <p>
     * A wild ocelot is the opposite in one more respect: once it has been around for a couple
     * of minutes the game quietly removes it if it is far from anything, and the trust flag
     * is what stops that happening. A trusted ocelot stays in the world.
     * example:
     * <pre>
     * const OcelotEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.OcelotEntityHelper");
     * const ocelots = World.getEntities(32, "ocelot");
     * if (ocelots !== null) {
     *   for (const entity of ocelots) {
     *     const ocelot = OcelotEntityHelper.class.cast(entity);
     *     if (ocelot.isTrusting()) {
     *       Chat.log("this one has been fed and will not run");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this ocelot trusts players and is not running from them,
     * {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isTrusting() {
        return ((MixinOcelotEntity) base).invokeIsTrusting();
    }

}
