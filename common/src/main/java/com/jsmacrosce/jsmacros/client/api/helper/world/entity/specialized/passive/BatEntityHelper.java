package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.ambient.Bat;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * the bat, which is the one mob here with a single state worth asking about: whether it is
 * hanging up out of the way or is flying about.
 * <p>
 * Nothing in this class changes anything. The flag is the game telling the client what its
 * bat is doing, and the game itself is what decides it, so this is a read of the last state
 * the server sent rather than something a script can push.
 * example:
 * <pre>
 * const BatEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.BatEntityHelper");
 * const bats = World.getEntities(24, "bat");
 * if (bats !== null) {
 *   for (const entity of bats) {
 *     const bat = BatEntityHelper.class.cast(entity);
 *     if (!bat.isResting()) {
 *       Chat.log(`a bat out at ${bat.getPos()}`);
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
public class BatEntityHelper extends MobEntityHelper<Bat> {

    public BatEntityHelper(Bat base) {
        super(base);
    }

    /**
     * Whether the bat is hanging upside down, which is where it spends the day and where it
     * goes back to when the light is right. A bat that is flying about answers {@code
     * false}, and it is worth knowing that the game uses the same flag to keep it quiet: a
     * resting bat often makes no ambient noise at all.
     *
     * @return {@code true} if the bat is hanging upside down, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isResting() {
        return base.isResting();
    }

}
