package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.monster.illager.AbstractIllager;
*///? } else {
import net.minecraft.world.entity.monster.AbstractIllager;
//? }

/**
 * the raiders, which is the family rather than any one of them.
 * <p>
 * A raider's whole visible state is one enum: which of its arms it has raised. The game
 * calls it an arm pose, {@link #getState() getState} hands it back as a name, and the
 * names are exactly the eight the enum has — {@code CROSSED}, {@code ATTACKING},
 * {@code SPELLCASTING}, {@code BOW_AND_ARROW}, {@code CROSSBOW_HOLD},
 * {@code CROSSBOW_CHARGE}, {@code CELEBRATING} and {@code NEUTRAL} — so a script can
 * compare against a plain string.
 * <p>
 * {@link #isCelebrating() isCelebrating} is the one mood that also has a flag behind it.
 * Which raider a script gets depends on what it is: the vindicator and the pillager get
 * their own classes, the evoker and the illusioner get the spellcasting one, and anything
 * else gets this.
 * example:
 * <pre>
 * const IllagerEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.IllagerEntityHelper");
 * const raiders = World.getEntities(32, "vindicator", "pillager", "evoker");
 * if (raiders !== null) {
 *   for (const entity of raiders) {
 *     // a vindicator and a pillager have their own classes, so this cast is the
 *     // shared part rather than the whole of what they can do
 *     const raider = IllagerEntityHelper.class.cast(entity);
 *     Chat.log(`${raider.getType()} at ${raider.getPos()} is ${raider.getState()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class IllagerEntityHelper<T extends AbstractIllager> extends MobEntityHelper<T> {

    public IllagerEntityHelper(T base) {
        super(base);
    }

    /**
     * Whether the raider is celebrating, which is the flag the game raises through the
     * raider's celebration goal. That goal only runs while the raider has a current raid
     * and that raid is in its loss state, so a raider that has not been in a raid that
     * ended that way has never had this set on it.
     * <p>
     * This is the flag behind the {@code CELEBRATING} arm pose, so on a raider the two
     * agree: this asks the flag and {@link #getState() getState} asks the pose, which the
     * game derives from that flag.
     * example:
     * <pre>
     * const IllagerEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.IllagerEntityHelper");
     * const raiders = World.getEntities(32, "vindicator");
     * if (raiders !== null) {
     *   for (const entity of raiders) {
     *     const raider = IllagerEntityHelper.class.cast(entity);
     *     if (raider.isCelebrating()) {
     *       // the pose reads CELEBRATING for the same raider
     *       Chat.log(`a celebrating raider at ${raider.getPos()}, pose ${raider.getState()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this raider is celebrating, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCelebrating() {
        return base.isCelebrating();
    }

    /**
     * The arm pose the raider is in, as the name the game stores it under rather than as a
     * number or a position in a list, so comparing it against a string is enough.
     * <p>
     * There are eight poses and all eight are mapped, so there is no fallback case to
     * handle. Which ones a script actually sees depends on the raider: a pillager answers
     * with {@code CROSSBOW_CHARGE}, {@code CROSSBOW_HOLD}, {@code ATTACKING} or
     * {@code NEUTRAL}, a vindicator with {@code ATTACKING}, {@code CELEBRATING} or
     * {@code CROSSED}, and a spellcaster with {@code SPELLCASTING}, {@code CELEBRATING} or
     * {@code CROSSED}. {@code BOW_AND_ARROW} is one of the eight names but none of those
     * raiders puts it on.
     * example:
     * <pre>
     * const IllagerEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.IllagerEntityHelper");
     * const raiders = World.getEntities(32, "pillager");
     * if (raiders !== null) {
     *   for (const entity of raiders) {
     *     const raider = IllagerEntityHelper.class.cast(entity);
     *     // a name, so a plain string comparison is enough
     *     if (raider.getState() === "CROSSBOW_CHARGE") {
     *       Chat.log(`a pillager at ${raider.getPos()} is charging its crossbow`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the raider's current arm pose, as one of the eight names the game stores.
     * @since 1.8.4
     */
    public String getState() {
        // Yarn and mojang mappings have the same names
        switch (base.getArmPose()) {
            case CROSSED:
                return "CROSSED";
            case ATTACKING:
                return "ATTACKING";
            case SPELLCASTING:
                return "SPELLCASTING";
            case BOW_AND_ARROW:
                return "BOW_AND_ARROW";
            case CROSSBOW_HOLD:
                return "CROSSBOW_HOLD";
            case CROSSBOW_CHARGE:
                return "CROSSBOW_CHARGE";
            case CELEBRATING:
                return "CELEBRATING";
            case NEUTRAL:
                return "NEUTRAL";
            default:
                throw new IllegalArgumentException();
        }
    }

}
