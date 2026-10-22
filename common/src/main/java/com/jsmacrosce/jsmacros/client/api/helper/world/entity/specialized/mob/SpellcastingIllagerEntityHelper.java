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
 * the raiders that cast, which in the current game are the evoker and the illusioner.
 * <p>
 * Everything shared with the other raiders is on {@code IllagerEntityHelper} and reached
 * through it, so the arm pose and the celebration flag are still one call away. What this
 * adds is the spell: {@link #isCastingSpell() isCastingSpell} is whether the cast is
 * running, and {@link #getCastedSpell() getCastedSpell} is which one it is.
 * <p>
 * The two agree about the cast rather than duplicating it. The game works out the arm pose
 * from the same spell it syncs, so on a spellcaster a casting one has the
 * {@code SPELLCASTING} pose, and the spell name is which one it is winding up to cast.
 * example:
 * <pre>
 * const SpellcastingIllagerEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.SpellcastingIllagerEntityHelper");
 * const evokers = World.getEntities(32, "evoker");
 * if (evokers !== null) {
 *   for (const entity of evokers) {
 *     const evoker = SpellcastingIllagerEntityHelper.class.cast(entity);
 *     if (evoker.isCastingSpell()) {
 *       // the spell is a name, and the pose is SPELLCASTING while it runs
 *       Chat.log(`an evoker at ${evoker.getPos()} is casting ${evoker.getCastedSpell()}`);
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
public class SpellcastingIllagerEntityHelper<T extends SpellcasterIllager> extends IllagerEntityHelper<T> {

    public SpellcastingIllagerEntityHelper(T base) {
        super(base);
    }

    /**
     * Whether the spellcaster is in the middle of a cast. The game raises the spell as its
     * use-spell goal starts and puts it back to {@code NONE} as that goal stops, so this
     * answers {@code true} for the whole wind-up — before the spell takes effect as well as
     * after — rather than only on the tick the spell lands.
     * <p>
     * This is the same state the game works the {@code SPELLCASTING} arm pose out of, so
     * {@link #getState() getState} on a casting spellcaster answers
     * {@code "SPELLCASTING"} — and {@link #getCastedSpell() getCastedSpell} is the other
     * half of the same cast.
     * example:
     * <pre>
     * const SpellcastingIllagerEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.SpellcastingIllagerEntityHelper");
     * const evokers = World.getEntities(32, "evoker");
     * if (evokers !== null) {
     *   for (const entity of evokers) {
     *     const evoker = SpellcastingIllagerEntityHelper.class.cast(entity);
     *     if (evoker.isCastingSpell()) {
     *       // a casting evoker is also in the SPELLCASTING pose
     *       Chat.log(`casting ${evoker.getCastedSpell()}, pose ${evoker.getState()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this spell caster is currently casting a spell, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isCastingSpell() {
        return base.isCastingSpell();
    }

    /**
     * The spell the spellcaster is casting, as one of the six names the game has:
     * {@code NONE}, {@code SUMMON_VEX}, {@code FANGS}, {@code WOLOLO}, {@code DISAPPEAR}
     * or {@code BLINDNESS}. {@code NONE} is what an idle spellcaster holds, so this is a
     * name to read rather than a check to make first.
     * <p>
     * The name is looked up out of a synced number, and a number outside the six the game
     * has comes back as {@code ERROR}. A spellcaster that is not casting anything reads
     * {@code NONE}, so a script wanting only real spells reads
     * {@link #isCastingSpell() isCastingSpell} as well as this.
     * example:
     * <pre>
     * const SpellcastingIllagerEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.SpellcastingIllagerEntityHelper");
     * const evokers = World.getEntities(32, "evoker");
     * if (evokers !== null) {
     *   for (const entity of evokers) {
     *     const evoker = SpellcastingIllagerEntityHelper.class.cast(entity);
     *     // an idle evoker reads NONE, so a real spell is anything but that
     *     if (evoker.getCastedSpell() !== "NONE") {
     *       if (evoker.isCastingSpell()) {
     *         Chat.log(`an evoker at ${evoker.getPos()} is on ${evoker.getCastedSpell()}`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
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
