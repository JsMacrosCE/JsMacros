package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.golem.IronGolem;
*///? } else {
import net.minecraft.world.entity.animal.IronGolem;
//?}

/**
 * the iron golem, which is the one mob here that remembers where it came from.
 * <p>
 * A golem the village built itself answers {@code false} here; a golem a player finished
 * building answers {@code true}, and the game treats the two differently: it will not let a
 * player-made golem attack a player, and it saves and reloads the fact so a golem stays
 * player-made across a chunk unload.
 * example:
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

    public IronGolemEntityHelper(IronGolem base) {
        super(base);
    }

    /**
     * Whether a player built this golem rather than a village summoning one. The game uses
     * it to decide whether the golem will go after players at all, and it is saved with the
     * golem, so the answer survives the entity being unloaded and read back.
     *
     * @return {@code true} if this iron golem was created by a player, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isPlayerCreated() {
        return base.isPlayerCreated();
    }

}
