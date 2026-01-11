package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.SpellcasterIllager;
import com.jsmacrosce.doclet.DocletDeclareType;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinSpellcastingIllagerEntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class SpellcastingIllagerEntityHelper<T extends SpellcasterIllager> extends IllagerEntityHelper<T> {

    public SpellcastingIllagerEntityHelper(T base) {
        super(base);
    }

    /**
     * @return {@code true} if this spell caster is currently casting a spell, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isCastingSpell() {
        return base.isCastingSpell();
    }

    /**
     * @return the spell this spell caster is currently casting.
     * @since 1.8.4
     */
    @DocletReplaceReturn("IllagerSpell")
    @DocletDeclareType(name = "IllagerSpell", type = "'NONE' | 'SUMMON_VEX' | 'FANGS' | 'WOLOLO' | 'DISAPPEAR' | 'BLINDNESS' | 'ERROR'")
    public String getCastedSpell() {
        return switch (base.getEntityData().get(((MixinSpellcastingIllagerEntityHelper) base).getSpellKey())) {
            case 0 -> "NONE";
            case 1 -> "SUMMON_VEX";
            case 2 -> "FANGS";
            case 3 -> "WOLOLO";
            case 4 -> "DISAPPEAR";
            case 5 -> "BLINDNESS";
            default -> "ERROR";
        };
    }

}
