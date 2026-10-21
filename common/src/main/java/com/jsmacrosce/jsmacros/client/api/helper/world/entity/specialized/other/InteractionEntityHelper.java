package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other;

import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.access.IMixinInteractionEntity;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinInteractionEntity2;

/**
 * Exposes an interaction entity's dimensions, response flag, recorded living entities, and client-side hit override.
 * <pre>
 * const InteractionEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other.InteractionEntityHelper");
 * const markers = World.getEntities(32, "interaction");
 * if (markers !== null) {
 *   for (const entity of markers) {
 *     const marker = InteractionEntityHelper.class.cast(entity);
 *     const last = marker.getLastInteracted();
 *     // a gate is an interaction entity with a name, and a name is not in the entity data
 *     Chat.log(`${entity.getName().getString()} at ${entity.getPos()}, last used by ${last === null ? "nobody" : last.getName().getString()}`);
 *   }
 * }
 * </pre>
 *
 * @author aMelonRind
 * @since 1.9.1
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class InteractionEntityHelper extends EntityHelper<Interaction> {

    /**
     * Wraps the supplied interaction entity, retaining the reference for this helper.
     *
     * @param base interaction entity to wrap
     */
    public InteractionEntityHelper(Interaction base) {
        super(base);
    }

    /**
     * Overrides this interaction entity's client-side pickability. The override is local to
     * this entity instance and is not saved in entity data.
     *
     * @param value {@code true} to make this entity pickable; {@code false} to make it unpickable
     * @since 1.9.1
     */
    public void setCanHit(boolean value) {
        ((IMixinInteractionEntity) base).jsmacros_setCanHitOverride(value);
    }

    /**
     * Returns the living entity recorded as the last attacker, or {@code null} if there is no
     * recorded attacker currently resolvable in the world. Differs from {@link #getLastInteracted()},
     * which returns the last interaction target.
     *
     * @return the last attacker, or {@code null} if none is recorded or resolvable
     * @since 1.9.1
     */
    @Nullable
    public EntityHelper<?> getLastAttacker() {
        LivingEntity e = base.getLastAttacker();
        return e == null ? null : EntityHelper.create(e);
    }

    /**
     * Returns the living entity recorded as the last interaction target, or {@code null} if
     * there is no recorded target currently resolvable in the world. This is distinct from the
     * last attacker returned by {@link #getLastAttacker()}.
     *
     * @return the last interaction target, or {@code null} if none is recorded or resolvable
     * @since 1.9.1
     */
    @Nullable
    public EntityHelper<?> getLastInteracted() {
        LivingEntity e = base.getTarget();
        return e == null ? null : EntityHelper.create(e);
    }

    /**
     * Returns the full interaction-box width in blocks; the default is {@code 1.0}.
     *
     * @return the interaction-box width in blocks
     * @since 1.9.1
     */
    public float getWidth() {
        return ((MixinInteractionEntity2) base).callGetInteractionWidth();
    }

    /**
     * Returns the interaction-box height in blocks, measured upward from the entity's position;
     * the default is {@code 1.0}.
     *
     * @return the interaction-box height in blocks
     * @since 1.9.1
     */
    public float getHeight() {
        return ((MixinInteractionEntity2) base).callGetInteractionHeight();
    }

    /**
     * Returns the entity's {@code response} setting, which defaults to {@code false}. In
     * Minecraft 1.21.8 this controls the client-side result of using the entity and whether an
     * attack interaction is consumed; it does not establish that a visible title is shown.
     *
     * @return the response setting
     * @since 1.9.1
     */
    public boolean shouldRespond() {
        return ((MixinInteractionEntity2) base).callShouldRespond();
    }

}
