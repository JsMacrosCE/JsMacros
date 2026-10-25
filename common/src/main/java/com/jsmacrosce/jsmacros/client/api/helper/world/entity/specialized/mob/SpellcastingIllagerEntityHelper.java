package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletDeclareType;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinSpellcastingIllagerEntityHelper;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
*///? } else {
import net.minecraft.world.entity.monster.SpellcasterIllager;
//? }

/**
 * Provides spell-casting state and the mapped spell name for a spellcasting illager.
 * <p>
 * {@link #isCastingSpell()} reports whether it is casting; {@link #getCastedSpell()} returns
 * the spell name, including {@code NONE} when idle and {@code ERROR} for an unmapped value.
 * Its arm pose is {@code SPELLCASTING} while casting.
 * <pre>
 * const evokers = World.getEntities(32, "evoker");
 * if (evokers !== null) {
 *   for (const entity of evokers) {
 *     if (!(entity instanceof EntityHelpers.SpellcastingIllager)) continue;
 *     if (entity.isCastingSpell()) {
 *       // the spell is a name, and the pose is SPELLCASTING while it runs
 *       Chat.log(`an evoker at ${entity.getPos()} is casting ${entity.getCastedSpell()}`);
 *     }
 *   }
 * }
 * </pre>
 *
 * @param <T> the spellcasting illager type wrapped by this helper
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class SpellcastingIllagerEntityHelper<T extends SpellcasterIllager> extends IllagerEntityHelper<T> {

    /** Wraps the supplied spellcasting illager and retains its reference for this helper.
     * @param base spellcasting illager entity to wrap
     */
    public SpellcastingIllagerEntityHelper(T base) {
        super(base);
    }

    /**
     * Returns whether the spellcaster is casting. This state is true during the spell's
     * wind-up and remains true until its casting timer expires; while true, its arm pose is
     * {@code SPELLCASTING}.
     *
     * @return {@code true} if this spell caster is currently casting a spell, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isCastingSpell() {
        return base.isCastingSpell();
    }

    /**
     * Returns the spell name mapped from the synchronized spell ID. The game defines
     * {@code NONE}, {@code SUMMON_VEX}, {@code FANGS}, {@code WOLOLO}, {@code DISAPPEAR},
     * and {@code BLINDNESS}; an unmapped ID returns {@code ERROR}. Idle spellcasters return
     * {@code NONE}, so use {@link #isCastingSpell()} to distinguish an active cast.
     *
     * @return the mapped spell name; {@code NONE} means no spell, and {@code ERROR} means the synchronized value is unmapped.
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
