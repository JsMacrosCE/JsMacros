package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other;

import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.access.IMixinInteractionEntity;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinInteractionEntity2;

/**
 * an interaction entity, the invisible marker used to give a region of the world a name, a
 * command or a response, the thing behind every shop, every gate and every area title.
 * <p>
 * The entity is invisible and cannot be hurt, and everything interesting about it is a setting
 * rather than a piece of state: how big it is, whether it answers an interaction, and the two
 * players it remembers. {@link #setCanHit(boolean) setCanHit()} is the one call here that
 * changes anything, and it changes the client rather than the server.
 * example:
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

    public InteractionEntityHelper(Interaction base) {
        super(base);
    }

    /**
     * whether a sword swing or a click passes through this entity to whatever is behind it.
     * <p>
          * This is an override rather than a game setting: the entity is pickable to begin with and
     * this replaces the answer outright. A pickable entity is one a ray cast stops at, so
     * {@code false} here is what lets a swing or a click reach whatever is behind the marker
     * instead of landing on an invisible box.
     * <p>
     * The change is on the client and is not written to the entity data, so it goes away when
     * the client builds a new instance of the entity, which is on a chunk reload or a dimension
     * change, and nothing on this class writes it back.
     * example:
     * <pre>
     * const InteractionEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other.InteractionEntityHelper");
     * const markers = World.getEntities(32, "interaction");
     * if (markers !== null) {
     *   for (const entity of markers) {
     *     const marker = InteractionEntityHelper.class.cast(entity);
          * // a ray cast now goes past the marker to whatever is behind it
     *     marker.setCanHit(false);
     *   }
     * }
     * </pre>
     *
     * @param value {@code true} to let attacks land on this entity, {@code false} to let them
     *              through it
     * @since 1.9.1
     */
    public void setCanHit(boolean value) {
        ((IMixinInteractionEntity) base).jsmacros_setCanHitOverride(value);
    }

    /**
     * the player who last attacked this entity, or {@code null} if none has.
     * <p>
          * This is the player who attacked the entity, which the game records when a player's swing
     * lands on it. It is a different player from {@link #getLastInteracted()
     * getLastInteracted()} in the usual case, since that is the player who used it rather than
     * hit it. The player has to still be in the world for this to be anything: the game stores
     * a UUID and looks the player up, so one who has left reads as {@code null} even though
     * they were the last to attack.
     * example:
     * <pre>
     * const InteractionEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other.InteractionEntityHelper");
     * const markers = World.getEntities(32, "interaction");
     * if (markers !== null) {
     *   for (const entity of markers) {
     *     const marker = InteractionEntityHelper.class.cast(entity);
     *     const attacker = marker.getLastAttacker();
     *     if (attacker !== null) {
     *       Chat.log(`someone swung at the gate at ${entity.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the player who last attacked this entity, or {@code null} if there is none
     * @since 1.9.1
     */
    @Nullable
    public EntityHelper<?> getLastAttacker() {
        LivingEntity e = base.getLastAttacker();
        return e == null ? null : EntityHelper.create(e);
    }

    /**
     * the player who last right clicked this entity, or {@code null} if none has.
     * <p>
          * This is the player who used the entity, which the game records when a player interacts
     * with it, so for a name region or a shop it is the player a script actually wants. The
     * counterpart is {@link #getLastAttacker() getLastAttacker()}, which is the player who
     * attacked it rather than used it, and one of the two is usually {@code null}. As with the
     * attacker, the game stores a UUID and looks the player up, so a player who has since left
     * reads as {@code null}.
     * example:
     * <pre>
     * const InteractionEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other.InteractionEntityHelper");
     * const markers = World.getEntities(32, "interaction");
     * if (markers !== null) {
     *   for (const entity of markers) {
     *     const marker = InteractionEntityHelper.class.cast(entity);
     *     const user = marker.getLastInteracted();
     *     if (user !== null) {
     *       Chat.log(`the gate at ${entity.getPos()} was last opened by ${user.getName().getString()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the player who last used this entity, or {@code null} if there is none
     * @since 1.9.1
     */
    @Nullable
    public EntityHelper<?> getLastInteracted() {
        LivingEntity e = base.getTarget();
        return e == null ? null : EntityHelper.create(e);
    }

    /**
     * how wide the interaction area is, in blocks, and {@code 1.0} is what a marker with no
     * width set reports.
     * <p>
          * This is the full width rather than a half extent, so an interaction entity of width
     * {@code 3.0} reaches a block and a half either side of its own position. The area is a box
     * built from this and {@link #getHeight() getHeight()}, half the width either side of the
     * entity on x and z, so a marker with a large width reaches past a wall it is buried in.
     * example:
     * <pre>
     * const InteractionEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other.InteractionEntityHelper");
     * const markers = World.getEntities(32, "interaction");
     * if (markers !== null) {
     *   for (const entity of markers) {
     *     const marker = InteractionEntityHelper.class.cast(entity);
     *     // the whole width, not half of it
     *     Chat.log(`a ${marker.getWidth()} by ${marker.getHeight()} area`);
     *   }
     * }
     * </pre>
     *
     * @return the width of the interaction area in blocks, 1.0 by default
     * @since 1.9.1
     */
    public float getWidth() {
        return ((MixinInteractionEntity2) base).callGetInteractionWidth();
    }

    /**
     * how tall the interaction area is, in blocks, and {@code 1.0} is what a marker with no
     * height set reports.
     * <p>
     * The vertical counterpart to {@link #getWidth() getWidth()}, measured up from the entity's
     * own feet rather than centred on it, so a marker of height {@code 3.0} reaches three
     * blocks above where it stands and none below.
     * example:
     * <pre>
     * const InteractionEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other.InteractionEntityHelper");
     * const markers = World.getEntities(32, "interaction");
     * if (markers !== null) {
     *   for (const entity of markers) {
     *     const marker = InteractionEntityHelper.class.cast(entity);
     *     // a ceiling marker is a wide flat one, and the height is what makes it flat
     *     if (1.0 > marker.getHeight()) {
     *       Chat.log("a flat area, for a ceiling title");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the height of the interaction area in blocks, 1.0 by default
     * @since 1.9.1
     */
    public float getHeight() {
        return ((MixinInteractionEntity2) base).callGetInteractionHeight();
    }

    /**
     * whether the entity answers a click with a visible response rather than swallowing it.
     * <p>
          * This is the {@code response} setting, and {@code false} is the default, so most
     * interaction entities in the world answer {@code false}.
     * <p>
     * What it changes is how the interaction reads to the player rather than what the entity
     * does. With it off, an interaction is consumed quietly, so a right click gives no visible
     * sign it was noticed; with it on, the interaction reports success, which is what makes a
     * name region show its title. The same flag also decides whether an attack on the entity is
     * reported at all, so an entity with it off swallows a swing as well as a click. None of
     * that is about the commands the entity runs: those are a separate part of the entity that
     * this class does not reach.
     * example:
     * <pre>
     * const InteractionEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other.InteractionEntityHelper");
     * const markers = World.getEntities(32, "interaction");
     * if (markers !== null) {
     *   for (const entity of markers) {
     *     const marker = InteractionEntityHelper.class.cast(entity);
     *     if (marker.shouldRespond()) {
     *       Chat.log("this one answers a click with something visible");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the entity responds to being used, {@code false} otherwise
     * @since 1.9.1
     */
    public boolean shouldRespond() {
        return ((MixinInteractionEntity2) base).callShouldRespond();
    }

}
