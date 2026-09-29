package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.dolphin.Dolphin;
*///? } else {
import net.minecraft.world.entity.animal.Dolphin;
//?}

/**
 * the dolphin, which is the one passive mob here that has something of its own to be doing:
 * it carries a fish, it has a chest of water it has to stay wet in, and every so often it
 * goes off to lead a player to buried treasure.
 * <p>
 * {@link #getTreasurePos() getTreasurePos} is the one to read carefully, and not because
 * the treasure is hard to find. The position is a plain field on the dolphin rather than
 * synced data, and only the server ever writes to it, so on the client it is normally
 * nothing at all. See that method before reading coordinates off it.
 * <p>
 * {@link #getMoistness() getMoistness} is a countdown rather than a gauge: the
 * game refills it while the dolphin is in water or in rain and drains it by one every tick
 * outside, and the dolphin starts taking damage once it runs all the way down.
 * example:
 * <pre>
 * const DolphinEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.DolphinEntityHelper");
 * const dolphins = World.getEntities(32, "dolphin");
 * if (dolphins !== null) {
 *   for (const entity of dolphins) {
 *     const dolphin = DolphinEntityHelper.class.cast(entity);
 *     Chat.log(`${dolphin.getType()}: carrying a fish ${dolphin.hasFish()}, `
 *       + `${dolphin.getMoistness()} ticks of water left`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class DolphinEntityHelper extends MobEntityHelper<Dolphin> {

    public DolphinEntityHelper(Dolphin base) {
        super(base);
    }

    /**
     * Whether the dolphin has picked up a fish, which is what it is carrying rather than
     * eating. The flag is set when it scoops a fish off the sea floor and stays on, so a
     * dolphin that answers {@code true} here is one that has something in its mouth.
     *
     * @return {@code true} if the dolphin has a fish in its mouth, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasFish() {
        return base.gotFish();
    }

    /**
     * The block the dolphin is leading somebody towards, and on the client usually nothing:
     * the game only ever picks this on the server, stores it in a plain field rather than
     * in synced data, and tells the client that a treasure was found rather than where it
     * is.
     * <p>
     * That makes the helper returned here one wrapped around nothing rather than one at
     * {@code 0 0 0}, so reading a coordinate off it will fail rather than quietly give the
     * world origin. Check {@code getRaw()} on it first, or use it only from a server-side
     * script where the dolphin was given the position itself.
     * example:
     * <pre>
     * const DolphinEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.DolphinEntityHelper");
     * const dolphins = World.getEntities(32, "dolphin");
     * if (dolphins !== null) {
     *   for (const entity of dolphins) {
     *     const dolphin = DolphinEntityHelper.class.cast(entity);
     *     const treasure = dolphin.getTreasurePos();
     *     if (treasure.getRaw() !== null) {
     *       Chat.log(`heading for ${treasure.getX()}, `
     *         + `${treasure.getY()}, ${treasure.getZ()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the position of the treasure the dolphin is looking for.
     * @since 1.8.4
     */
    public BlockPosHelper getTreasurePos() {
        return new BlockPosHelper(base.treasurePos);
    }

    /**
     * How much wet the dolphin has left, counting down rather than up. The game refills it
     * to its full value while the dolphin is in water or in rain, and outside that it drops
     * by one every tick and the dolphin starts taking damage once it reaches nothing.
     * <p>
     * The full value is not something this class reports, and it is not small: it is the
     * two minutes' worth of ticks the game gives a dolphin.
     *
     * @return the moisture level of the dolphin.
     * @since 1.8.4
     */
    public int getMoistness() {
        return base.getMoistnessLevel();
    }

}
