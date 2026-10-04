package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.bee.Bee;
*///? } else {
import net.minecraft.world.entity.animal.Bee;
//?}

/**
 * the bee, and the three separate things about a bee that read alike.
 * <p>
 * A bee can be carrying nectar back from a flower, it can be angry at somebody, and it can
 * have stung already. They are three different flags and they do not imply one another:
 * stinging stops the anger outright, and the sting does not touch the nectar at all, so a
 * bee that has stung and dropped its nectar still answers to all three.
 * <p>
 * {@link #isAngry() isAngry} is the anger <em>timer</em>, the same figure a wolf reports,
 * and not the same as the bee having a target. The game keeps a timer running for a while
 * after the bee is provoked, and the bee attacks whatever it is cross about for as long as
 * the timer lasts, whether or not it can still see them.
 * example:
 * <pre>
 * const BeeEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.BeeEntityHelper");
 * const bees = World.getEntities(32, "bee");
 * if (bees !== null) {
 *   for (const entity of bees) {
 *     const bee = BeeEntityHelper.class.cast(entity);
 *     Chat.log(`${bee.getType()} at ${bee.getPos()}: `
 *       + `nectar ${bee.hasNectar()}, `
 *       + `angry ${bee.isAngry()}, `
 *       + `stung ${bee.hasStung()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class BeeEntityHelper extends AnimalEntityHelper<Bee> {

    public BeeEntityHelper(Bee base) {
        super(base);
    }

    /**
     * Whether the bee has nectar from a flower, which is what lets it pollinate a crop
     * sitting next to it. The game raises it after the bee has spent long enough at a crop
     * and drops it again when the bee drops its nectar off somewhere, so a bee that is
     * carrying nothing is a bee between flowers rather than a bee that has never found one.
     *
     * @return {@code true} if the bee has nectar, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasNectar() {
        return base.hasNectar();
    }

    /**
     * The anger timer, so it answers for how cross the bee is rather than whether it is
     * currently looking at whoever provoked it. It is the same figure a wolf reports: the
     * game starts a timer when the mob is provoked, and everything the mob does about it -
     * picking a target, going after that target, giving up - hangs off the timer rather
     * than off a separate flag.
     * example:
     * <pre>
     * const BeeEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.BeeEntityHelper");
     * const player = Player.getPlayer();
     * const bees = World.getEntities(16, "bee");
     * if (player !== null) {
     *   if (bees !== null) {
     *     for (const entity of bees) {
     *       const bee = BeeEntityHelper.class.cast(entity);
     *       if (bee.isAngry()) {
     *         Chat.log(`that bee is cross, and it has stung: ${bee.hasStung()}`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the bee is angry at a player, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isAngry() {
        return base.isAngry();
    }

    /**
     * Whether this bee has already stung somebody. The game sets it the moment the bee
     * stings and immediately stops the bee being angry, so a bee that answers {@code true}
     * here is one that will not sting again however close a player comes.
     *
     * @return {@code true} if the bee has already stung a player, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasStung() {
        return base.hasStung();
    }

}
