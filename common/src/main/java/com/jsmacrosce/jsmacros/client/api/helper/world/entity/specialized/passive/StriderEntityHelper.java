package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.monster.Strider;
import com.jsmacrosce.doclet.DocletCategory;

/**
 * Provides access to a strider's saddle and suffocating state.
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

    /** Wraps the strider and retains its reference.
     * @param base strider entity to wrap
     */
    public StriderEntityHelper(Strider base) {
        super(base);
    }

    /**
     * Returns whether the strider has a saddle equipped.
     *
     * @return {@code true} if this strider is saddled, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSaddled() {
        return base.isSaddled();
    }

    /**
     * Returns the game's suffocating flag, which is set when the strider is not on a warm
     * block or in lava, or when its vehicle is a suffocating strider. The state applies a
     * movement-speed penalty.
     *
     * @return {@code true} when the strider is suffocating, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isShivering() {
        return base.isSuffocating();
    }

}
