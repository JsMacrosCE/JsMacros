package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Witch;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * Provides access to a witch's drinking state and main-hand item. While drinking,
 * the witch holds a potion; outside that state, its main hand may be empty.
 * <pre>
 * const witches = World.getEntities(32, "witch");
 * if (witches !== null) {
 *   for (const entity of witches) {
 *     if (!(entity instanceof EntityHelpers.Witch)) continue;
 *     if (entity.isDrinkingPotion()) {
 *       Chat.log(`a witch at ${entity.getPos()} is drinking ${entity.getPotion().getName().getString()}`);
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

    /**
     * Wraps the supplied witch and retains its reference for this helper.
     *
     * @param base witch entity to wrap
     */
    public WitchEntityHelper(Witch base) {
        super(base);
    }

    /**
     * Whether the witch is currently using a potion to drink. A potion held without
     * drinking does not make this return {@code true}.
     *
     * @return {@code true} if this witch is drinking a potion, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isDrinkingPotion() {
        return base.isDrinkingPotion();
    }

    /**
     * The witch's main-hand stack. It contains the drinking potion while the witch is
     * drinking and is empty after the drink; see {@link #isDrinkingPotion()} to check
     * whether a drink is in progress.
     *
     * @return the main-hand item stack; empty when the hand is empty.
     * @since 1.8.4
     */
    public ItemStackHelper getPotion() {
        return getMainHand();
    }

}
