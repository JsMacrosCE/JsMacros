package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.AbstractIllager;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class IllagerEntityHelper<T extends AbstractIllager> extends MobEntityHelper<T> {

    public IllagerEntityHelper(T base) {
        super(base);
    }

    public boolean isCelebrating() {
        return base.isCelebrating();
    }

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
