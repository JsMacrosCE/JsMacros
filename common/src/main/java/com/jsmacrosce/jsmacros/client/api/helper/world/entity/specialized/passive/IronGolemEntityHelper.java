package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.golem.IronGolem;
*///? } else {
import net.minecraft.world.entity.animal.IronGolem;
//?}

/**
 * Provides access to iron-golem state, including whether it was player-created.
 * <pre>
 * const IronGolemEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.IronGolemEntityHelper");
 * const golems = World.getEntities(48, "iron_golem");
 * if (golems !== null) {
 *   for (const entity of golems) {
 *     const golem = IronGolemEntityHelper.class.cast(entity);
 *     if (golem.isPlayerCreated()) {
 *       Chat.log(`a player-built golem at ${golem.getPos()}`);
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
public class IronGolemEntityHelper extends MobEntityHelper<IronGolem> {

    /**
     * Wraps the supplied iron golem and retains its reference for this helper.
     *
     * @param base iron golem entity to wrap
     */
    public IronGolemEntityHelper(IronGolem base) {
        super(base);
    }

    /**
     * Returns whether this golem's player-created flag is set. This flag makes its attack-eligibility
     * check reject players and is saved with the entity; it does not prevent every possible damage path.
     *
     * @return {@code true} when the player-created flag is set; {@code false} otherwise
     * @since 1.8.4
     */
    public boolean isPlayerCreated() {
        return base.isPlayerCreated();
    }

}
