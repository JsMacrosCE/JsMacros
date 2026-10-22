package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.monster.illager.AbstractIllager;
*///? } else {
import net.minecraft.world.entity.monster.AbstractIllager;
//? }

/**
 * Provides access to illager state shared across specialized illager helpers.
 * <pre>
 * const IllagerEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.IllagerEntityHelper");
 * const raiders = World.getEntities(32, "vindicator", "pillager", "evoker");
 * if (raiders !== null) {
 *   for (const entity of raiders) {
 *     // Access state shared by the different illager helper classes.
 *     const raider = IllagerEntityHelper.class.cast(entity);
 *     Chat.log(`${raider.getType()} at ${raider.getPos()} is ${raider.getState()}`);
 *   }
 * }
 * </pre>
 *
 * @param <T> the illager entity type wrapped by this helper
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class IllagerEntityHelper<T extends AbstractIllager> extends MobEntityHelper<T> {

    /** Wraps the supplied illager and retains its reference for this helper.
     * @param base illager entity to wrap
     */
    public IllagerEntityHelper(T base) {
        super(base);
    }

    /**
     * Returns whether this illager is celebrating.
     * @return {@code true} if celebrating; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCelebrating() {
        return base.isCelebrating();
    }

    /**
     * Returns the current arm-pose name.
     * @return one of {@code CROSSED}, {@code ATTACKING}, {@code SPELLCASTING}, {@code BOW_AND_ARROW}, {@code CROSSBOW_HOLD}, {@code CROSSBOW_CHARGE}, {@code CELEBRATING}, or {@code NEUTRAL}.
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
