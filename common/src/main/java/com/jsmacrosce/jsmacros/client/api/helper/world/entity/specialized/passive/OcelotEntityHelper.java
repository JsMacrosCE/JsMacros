package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinOcelotEntity;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.feline.Ocelot;
*///? } else {
import net.minecraft.world.entity.animal.Ocelot;
//?}

/**
 * Provides access to an ocelot's trust state.
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

    /** Wraps the supplied ocelot and retains its reference for this helper.
     *
     * @param base ocelot entity to wrap
     */
    public OcelotEntityHelper(Ocelot base) {
        super(base);
    }

    /**
     * Returns whether this ocelot has its trust state set. Trusting ocelots do not avoid nearby players and are exempt from distance-based despawning.
     *
     * @return {@code true} if trusting; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isTrusting() {
        return ((MixinOcelotEntity) base).invokeIsTrusting();
    }

}
