package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.vehicle;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

//?  if >=1.21.11 {
/*import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
*///? } else {
import net.minecraft.world.entity.vehicle.MinecartTNT;
//? }

/**
 * a TNT minecart, the minecart that explodes when it is lit.
 * <p>
 * Unlike a primed block of TNT, a TNT minecart exists before it goes off: it is a minecart
 * that happens to be full of TNT, and it can be lit and then go off later, or be hit hard
 * enough to go off early. So it has two states rather than one, and
 * {@link #isPrimed() isPrimed()} is what tells them apart. {@code -1} from
 * {@link #getRemainingTime() getRemainingTime()} is how a minecart that has not been lit
 * answers.
 * example:
 * <pre>
 * const TntMinecartEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.vehicle.TntMinecartEntityHelper");
 * const carts = World.getEntities(32, "tnt_minecart");
 * if (carts !== null) {
 *   for (const entity of carts) {
 *     const cart = TntMinecartEntityHelper.class.cast(entity);
 *     if (cart.isPrimed()) {
 *       Chat.log(`it is lit, and goes off in ${cart.getRemainingTime()} ticks`);
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
public class TntMinecartEntityHelper extends EntityHelper<MinecartTNT> {

    public TntMinecartEntityHelper(MinecartTNT base) {
        super(base);
    }

    /**
     * the remaining time in ticks before the tnt explodes, or {@code -1} if the tnt is not
     * primed.
     * <p>
     * The number counts down once a tick and the minecart explodes when it reaches zero, so a
     * value of 0 means it is going off this tick. An unlit minecart answers {@code -1} rather
     * than 0, which is the same rule the game uses to decide whether it is primed, so this and
     * {@link #isPrimed() isPrimed()} can never disagree. Once lit, the fuse is a random number
     * of ticks under forty rather than a fixed one.
     * example:
     * <pre>
     * const TntMinecartEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.vehicle.TntMinecartEntityHelper");
     * const carts = World.getEntities(32, "tnt_minecart");
     * if (carts !== null) {
     *   for (const entity of carts) {
     *     const cart = TntMinecartEntityHelper.class.cast(entity);
     *     const fuse = cart.getRemainingTime();
     *     // -1 is an unlit minecart, which is the same thing isPrimed tests
     *     if (0 > fuse) {
     *       Chat.log("not lit yet");
     *     } else {
     *       Chat.log(`goes off in ${fuse} ticks`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the remaining time in ticks before the tnt explodes, or {@code -1} if the tnt is not
     * primed.
     * @since 1.8.4
     */
    public int getRemainingTime() {
        return base.getFuse();
    }

    /**
     * {@code true} if the tnt is primed, {@code false} otherwise.
     * <p>
     * Primed means lit, which is the same as {@link #getRemainingTime() getRemainingTime()}
     * being above minus one, so a lit minecart is primed and an unlit one is not, whichever
     * order the two are asked in. This is the only thing that makes a TNT minecart different
     * from an ordinary one, and a lit minecart is the one that is about to go off.
     * example:
     * <pre>
     * const TntMinecartEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.vehicle.TntMinecartEntityHelper");
     * const carts = World.getEntities(32, "tnt_minecart");
     * if (carts !== null) {
     *   for (const entity of carts) {
     *     const cart = TntMinecartEntityHelper.class.cast(entity);
     *     // primed is the same as a fuse above minus one
     *     if (cart.isPrimed()) {
     *       if (cart.getRemainingTime() >= 0) {
     *         Chat.log("lit and counting down");
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the tnt is primed, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isPrimed() {
        return base.isPrimed();
    }

}
