package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.monster.Strider;
import com.jsmacrosce.doclet.DocletCategory;

/**
 * the strider, which is not really passive but is filed here anyway because everything else
 * about it is animal.
 * <p>
 * {@link #isShivering() isShivering} is a strange name for what it reports. The game calls
 * the state suffocating, and it means the strider is cold: it is not on warm blocks and not
 * standing in lava. The game takes it out on the strider's speed while it lasts, which is why
 * a cold strider looks so slow rather than merely looking cold.
 * example:
 * <pre>
 * const StriderEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.StriderEntityHelper");
 * const striders = World.getEntities(32, "strider");
 * if (striders !== null) {
 *   for (const entity of striders) {
 *     const strider = StriderEntityHelper.class.cast(entity);
 *     Chat.log(`saddled ${strider.isSaddled()}, shivering ${strider.isShivering()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class StriderEntityHelper extends AnimalEntityHelper<Strider> {

    public StriderEntityHelper(Strider base) {
        super(base);
    }

    /**
     * Whether there is a saddle in the strider's saddle slot. Unlike a horse the game lets a
     * player saddle a strider without taming it, and there is no tame flag on a strider at
     * all, so this is the only saddle question there is.
     *
     * @return {@code true} if this strider is saddled, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSaddled() {
        return base.isSaddled();
    }

    /**
     * Whether the strider is cold, which is what the game calls suffocating. The flag goes up
     * when the strider is standing somewhere that is not a warm block and not in lava, and
     * it comes down again as soon as it steps somewhere warmer - or onto a warm strider,
     * which is the other thing the game checks.
     * <p>
     * While it is up the game applies a penalty to the strider's movement speed, so a cold
     * strider is genuinely slower and not merely shivering.
     * example:
     * <pre>
     * const StriderEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.StriderEntityHelper");
     * const striders = World.getEntities(24, "strider");
     * if (striders !== null) {
     *   for (const entity of striders) {
     *     const strider = StriderEntityHelper.class.cast(entity);
     *     if (strider.isShivering()) {
     *       Chat.log(`that one needs lava at ${strider.getBlockPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this strider is shivering in the cold, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isShivering() {
        return base.isSuffocating();
    }

}
