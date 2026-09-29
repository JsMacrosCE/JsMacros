package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Witch;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * the witch, and the potion it is drinking.
 * <p>
 * A witch throws a potion, drinks a healing one, and there is a flag for the drinking: what
 * the game is doing here is a held item with a use timer behind it, so
 * {@link #isDrinkingPotion() isDrinkingPotion} is the timer running and
 * {@link #getPotion() getPotion} is what is in the hand while it runs.
 * example:
 * <pre>
 * const WitchEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.WitchEntityHelper");
 * const witches = World.getEntities(32, "witch");
 * if (witches !== null) {
 *   for (const entity of witches) {
 *     const witch = WitchEntityHelper.class.cast(entity);
 *     if (witch.isDrinkingPotion()) {
 *       // the potion is the main hand, and the game empties it once the drink is done
 *       Chat.log(`a witch at ${witch.getPos()} is drinking ${witch.getPotion().getName().getString()}`);
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
public class WitchEntityHelper extends MobEntityHelper<Witch> {

    public WitchEntityHelper(Witch base) {
        super(base);
    }

    /**
     * Whether the witch is in the middle of drinking. The game raises the flag as it starts
     * a drink and lowers it as the drink's timer runs out, at which point it empties the
     * hand, so this is {@code false} for a witch that is only holding a potion rather than
     * drinking one.
     * example:
     * <pre>
     * const WitchEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.WitchEntityHelper");
     * const witches = World.getEntities(32, "witch");
     * if (witches !== null) {
     *   for (const entity of witches) {
     *     const witch = WitchEntityHelper.class.cast(entity);
     *     if (witch.isDrinkingPotion()) {
     *       Chat.log(`drinking at ${witch.getPos()}, health ${witch.getHealth()} of ${witch.getMaxHealth()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this witch is drinking a potion, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isDrinkingPotion() {
        return base.isDrinkingPotion();
    }

    /**
     * The witch's main hand item, which is the potion it is drinking. The game puts the
     * potion in the main hand as the drink starts and empties that slot when the timer runs
     * out, so this is a live view of the hand rather than a copy of the potion.
     * <p>
     * That means it is worth reading alongside {@link #isDrinkingPotion()
     * isDrinkingPotion}: a witch between drinks still has whatever its main hand holds, and
     * that is not a potion.
     * example:
     * <pre>
     * const WitchEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.WitchEntityHelper");
     * const witches = World.getEntities(32, "witch");
     * if (witches !== null) {
     *   for (const entity of witches) {
     *     const witch = WitchEntityHelper.class.cast(entity);
     *     // the main hand, so it is only a potion while a drink is actually running
     *     if (witch.isDrinkingPotion()) {
     *       const potion = witch.getPotion();
     *       if (!potion.isEmpty()) {
     *         Chat.log(`a witch at ${witch.getPos()} has ${potion.getName().getString()} in hand`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the held potion item.
     * @since 1.8.4
     */
    public ItemStackHelper getPotion() {
        return getMainHand();
    }

}
