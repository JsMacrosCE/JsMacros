package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import com.jsmacrosce.doclet.DocletCategory;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.monster.illager.Vindicator;
*///? } else {
import net.minecraft.world.entity.monster.Vindicator;
//? }

/**
 * the vindicator, which is a raider with one extra question to ask it: whether it is Johnny.
 * <p>
 * Everything a raider has is on {@code IllagerEntityHelper} and reached through it, so the
 * arm pose and the celebration flag are still one call away. {@link #isJohnny() isJohnny}
 * is the one thing this adds, and it is worth knowing that it asks the vindicator's
 * <em>name</em> rather than a flag of its own — the game keeps a separate Johnny flag for
 * its own purposes, and that flag is not what this reads.
 * example:
 * <pre>
 * const VindicatorEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.VindicatorEntityHelper");
 * const vindicators = World.getEntities(32, "vindicator");
 * if (vindicators !== null) {
 *   for (const entity of vindicators) {
 *     const vindicator = VindicatorEntityHelper.class.cast(entity);
 *     if (vindicator.isJohnny()) {
 *       // this is the name check, so the name is worth reading back
 *       Chat.log(`Johnny at ${vindicator.getPos()}: ${vindicator.getName().getString()}`);
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
public class VindicatorEntityHelper extends IllagerEntityHelper<Vindicator> {

    public VindicatorEntityHelper(Vindicator base) {
        super(base);
    }

    /**
     * Whether the vindicator's custom name is exactly {@code Johnny}, with the comparison
     * made on the plain text of the name.
     * <p>
     * Two things follow from that being a name check rather than a flag check. A vindicator
     * that has been given the name by a name tag answers {@code true} whether or not the
     * game considers it Johnny, and a vindicator with no custom name at all answers
     * {@code false} even when the game has its own Johnny flag set on it — the game keeps
     * that flag separately and this never looks at it. The match is also exact, so
     * {@code johnny} or {@code Johnny!} are not a match.
     * example:
     * <pre>
     * const VindicatorEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.VindicatorEntityHelper");
     * const vindicators = World.getEntities(32, "vindicator");
     * if (vindicators !== null) {
     *   for (const entity of vindicators) {
     *     const vindicator = VindicatorEntityHelper.class.cast(entity);
     *     // reading the name back is the only way to see why this answered what it did
     *     const name = vindicator.getName().getString();
     *     if (vindicator.isJohnny()) {
     *       Chat.log(`named ${name} at ${vindicator.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this vindicator is johnny, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isJohnny() {
        return base.hasCustomName() && base.getCustomName().getString().equals("Johnny");
    }

}
