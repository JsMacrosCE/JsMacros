package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.boss;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinPhaseType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * the ender dragon, the boss of the end.
 * <p>
 * The dragon is not one entity but several. Alongside the dragon itself the game tracks eight
 * body parts, each a real entity with its own position, and they are what a hit actually lands
 * on: damaging a body part is what damages the dragon, and the dragon's own hitbox is not what
 * a sword finds. {@link #getBodyParts() getBodyParts()} and
 * {@link #getBodyPart(int) getBodyPart(int)} are how a script reaches them, and
 * {@link #getBodyParts(String) getBodyParts(String)} narrows that to one part of the body.
 * <p>
 * There are eight parts and five names for them, so the list is longer than any one name's
 * result: one head, one neck, one body, three tails and two wings. Nothing here changes what
 * any of them does.
 * example:
 * <pre>
 * const EnderDragonEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.boss.EnderDragonEntityHelper");
 * const dragons = World.getEntities(128, "ender_dragon");
 * if (dragons !== null) {
 *   for (const entity of dragons) {
 *     const dragon = EnderDragonEntityHelper.class.cast(entity);
 *     Chat.log(`phase ${dragon.getPhase()}, over ${dragon.getBodyParts().size()} parts`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class EnderDragonEntityHelper extends MobEntityHelper<EnderDragon> {

    public EnderDragonEntityHelper(EnderDragon base) {
        super(base);
    }

    /**
     * The phases are as follows:
     * <p>
     * {@code HoldingPattern}, {@code StrafePlayer}, {@code LandingApproach}, {@code Landing},
     * {@code Takeoff}, {@code SittingFlaming}, {@code SittingScanning}, {@code SittingAttacking},
     * {@code ChargingPlayer}, {@code Dying}, {@code Hover}
     *
     * This is the phase the dragon is in right now, and it is the name the game stores rather
     * than a number or a position in a list, so it is safe to compare against a string. The
     * eleven above are every phase that exists: the first five are the circling and landing
     * flight, the three {@code Sitting} ones are what it does on the fountain at the end, and
     * {@code Dying} is the one it stays in while it falls apart.
     * example:
     * <pre>
     * const EnderDragonEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.boss.EnderDragonEntityHelper");
     * const dragons = World.getEntities(128, "ender_dragon");
     * if (dragons !== null) {
     *   for (const entity of dragons) {
     *     const dragon = EnderDragonEntityHelper.class.cast(entity);
     *     // the phase is a name, so a plain string comparison is enough
     *     if (dragon.getPhase() === "Dying") {
     *       Chat.log("the dragon is falling apart");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the current phase of the dragon.
     * @since 1.8.4
     */
    @DocletReplaceReturn("DragonPhase")
    public String getPhase() {
        return ((MixinPhaseType) base.getPhaseManager().getCurrentPhase().getPhase()).getName();
    }

    /**
     * The body parts run from the head at index zero down to the wings at the end, in the order
     * the game keeps them: {@code 0} is the head, {@code 1} the neck, {@code 2} the body,
     * {@code 3} to {@code 5} the three tail segments and {@code 6} and {@code 7} the two wings.
     * There are eight of them, so the index runs from 0 to 7 and an index outside that range
     * raises rather than answering {@code null}.
     * <p>
     * This is a different way to ask the same question as
     * {@link #getBodyParts(String) getBodyParts(String)}, and it is the right one when a script
     * wants one particular segment rather than every part of a kind.
     * example:
     * <pre>
     * const EnderDragonEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.boss.EnderDragonEntityHelper");
     * const dragons = World.getEntities(128, "ender_dragon");
     * if (dragons !== null) {
     *   for (const entity of dragons) {
     *     const dragon = EnderDragonEntityHelper.class.cast(entity);
     *     // index 0 is the head, which is the part a sword most often finds
     *     const head = dragon.getBodyPart(0);
     *     Chat.log(`the head is at ${head.getPos()}`);
     *   }
     * }
     * </pre>
     *
     * @param index the index of the dragon's body part to get, from 0 to 7
     * @return the specified body part of the dragon.
     * @since 1.8.4
     */
    public EntityHelper<?> getBodyPart(int index) {
        return EntityHelper.create(base.getSubEntities()[index]);
    }

    /**
     * every body part of the dragon, all eight of them, from the head to the wings.
     * <p>
     * The order is the same as {@link #getBodyPart(int) getBodyPart(int)} uses, so index zero
     * of this list is the head. Each part is an entity in its own right and reports its own
     * position, so a script watching a dragon move wants this list rather than the dragon's
     * own position, which is a single point rather than a shape. The list is a fresh copy
     * every call, so changing it does nothing to the dragon.
     * example:
     * <pre>
     * const EnderDragonEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.boss.EnderDragonEntityHelper");
     * const dragons = World.getEntities(128, "ender_dragon");
     * if (dragons !== null) {
     *   for (const entity of dragons) {
     *     const dragon = EnderDragonEntityHelper.class.cast(entity);
     *     // eight parts, each with its own position
     *     for (const part of dragon.getBodyParts()) {
     *       Chat.log(`a body part at ${part.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return a list of all body parts of the dragon.
     * @since 1.8.4
     */
    public List<? extends EntityHelper<?>> getBodyParts() {
        return Arrays.stream(base.getSubEntities()).map(EntityHelper::create).collect(Collectors.toList());
    }

    /**
     * The name can be either {@code head}, {@code neck}, {@code body}, {@code tail} or
     * {@code wing}.
     *
     * A name can match more than one part, and in two cases it does: there are three tail
     * segments and two wings, so {@code "tail"} gives three parts and {@code "wing"} gives two,
     * while {@code "head"}, {@code "neck"} and {@code "body"} each give one. A name that
     * matches nothing gives an empty list rather than {@code null}, and the order is the same
     * as {@link #getBodyParts() getBodyParts()}.
     * <p>
     * The comparison is on the part's own name, so it is exact and lower case: {@code "Tail"}
     * or {@code "heads"} both give an empty list.
     * example:
     * <pre>
     * const EnderDragonEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.boss.EnderDragonEntityHelper");
     * const dragons = World.getEntities(128, "ender_dragon");
     * if (dragons !== null) {
     *   for (const entity of dragons) {
     *     const dragon = EnderDragonEntityHelper.class.cast(entity);
     *     // three tails and two wings, so the count is not always one
     *     Chat.log(`${dragon.getBodyParts("tail").size()} tails, ${dragon.getBodyParts("wing").size()} wings`);
     *     // an unknown name is an empty list, not a null
     *     Chat.log(`${dragon.getBodyParts("Tail").size()} for a name that is not right`);
     *   }
     * }
     * </pre>
     *
     * @param name the name of the body part to get
     * @return a list of all body parts of the dragon with the specified name.
     * @since 1.8.4
     */
    @DocletReplaceParams("name: DragonBodyPart")
    public List<? extends EntityHelper<?>> getBodyParts(String name) {
        return Arrays.stream(base.getSubEntities()).filter(e -> e.name.equals(name)).map(EntityHelper::create).collect(Collectors.toList());
    }

}
