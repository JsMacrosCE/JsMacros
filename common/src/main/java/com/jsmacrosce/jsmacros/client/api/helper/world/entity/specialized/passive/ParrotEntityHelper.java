package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.client.Minecraft;
import net.minecraft.core.UUIDUtil;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;

import java.util.Objects;
import java.util.stream.Stream;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.parrot.Parrot;
*///? } else {
import net.minecraft.world.entity.animal.Parrot;
//?}

/**
 * the parrot, which is a tameable bird with a coat and an unusual amount of state packed
 * into one small entity.
 * <p>
 * {@link #isSitting() isSitting} is the sitting <em>pose</em> here, not the sit order that
 * {@link TameableEntityHelper#isSitting() isSitting} reports on the base class. The game
 * keeps the two apart: the order is what a player asked for and survives the animal being
 * pulled away to do something else, while the pose is what the animal is actually doing right
 * now.
 * <p>
 * {@link #isFlying() isFlying} is a figure of speech. The parrot has no flying flag of its
 * own; the game answers it from whether the bird has ground under it, so a parrot hopping
 * in the same tick it lands answers {@code false}.
 * example:
 * <pre>
 * const ParrotEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.ParrotEntityHelper");
 * const parrots = World.getEntities(32, "parrot");
 * if (parrots !== null) {
 *   for (const entity of parrots) {
 *     const parrot = ParrotEntityHelper.class.cast(entity);
 *     Chat.log(`${parrot.getVariant()} parrot: flying ${parrot.isFlying()}, `
 *       + `sitting ${parrot.isSitting()}, `
 *       + `partying ${parrot.isPartying()}, `
 *       + `standing ${parrot.isStanding()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class ParrotEntityHelper extends TameableEntityHelper<Parrot> {

    public ParrotEntityHelper(Parrot base) {
        super(base);
    }

    /**
     * The coat name as the game writes it in its save data, with no namespace on the front:
     * {@code red_blue}, {@code blue}, {@code green}, {@code yellow_blue} or {@code gray}.
     *
     * @return the variant of this parrot.
     * @since 1.8.4
     */
    @DocletReplaceReturn("ParrotVariant")
    public String getVariant() {
        return base.getVariant().getSerializedName();
    }

    /**
     * Whether the parrot is in its sitting pose right now. This is the pose rather than the
     * sit order, so it is the figure that drops the moment the bird stands up to follow
     * somebody or to go somewhere else.
     * example:
     * <pre>
     * const ParrotEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.ParrotEntityHelper");
     * const parrots = World.getEntities(24, "parrot");
     * if (parrots !== null) {
     *   for (const entity of parrots) {
     *     const parrot = ParrotEntityHelper.class.cast(entity);
     *     if (parrot.isSitting()) {
     *       Chat.log(`sitting at ${parrot.getBlockPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this parrot is sitting, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSitting() {
        return base.isInSittingPose();
    }

    /**
     * Whether the parrot is off the ground. The game answers it from whether there is ground
     * under the bird rather than from a flying flag of its own, so a parrot in the middle of
     * landing answers {@code false} while it is still touching down.
     *
     * @return {@code true} if this parrot is flying, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isFlying() {
        return base.isFlying();
    }

    /**
     * Whether the parrot is dancing to a jukebox. The jukebox tells the parrot it is playing
     * and the parrot keeps that until the music stops, the jukebox is taken away, or it is
     * replaced by a different block.
     * example:
     * <pre>
     * const ParrotEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.ParrotEntityHelper");
     * const parrots = World.getEntities(24, "parrot");
     * if (parrots !== null) {
     *   for (const entity of parrots) {
     *     const parrot = ParrotEntityHelper.class.cast(entity);
     *     if (parrot.isPartying()) {
     *       Chat.log(`dancing at ${parrot.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this parrot is dancing to music, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isPartying() {
        return base.isPartyParrot();
    }

    /**
     * Whether the parrot is doing none of the three things above, which is what the method
     * works out rather than what the game stores. A parrot on the ground, not sitting and
     * not dancing answers {@code true}.
     *
     * @return {@code true} if this parrot is just standing around, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isStanding() {
        return !isPartying() && !isFlying() && !isSitting();
    }

    /**
     * @apiNote In 1.21.9 and beyond, parrot entities are removed when on player shoulders so this will always return
     * false.
     * <p>
     * On older versions this looked for the parrot among the entities riding the shoulders
     * of nearby players, and it only ever looked once the bird was in its sitting pose.
     * Since 1.21.9 a player keeps a parrot as a variant rather than as an entity, so there
     * is nothing on a shoulder to find and this answers {@code false} for every parrot.
     *
     * @return {@code true} if this parrot is sitting on any player's shoulder, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isSittingOnShoulder() {
        if (!isSitting()) return false;
        //? if >1.21.8 {
        /*return false;
        *///?} else {
        return Minecraft.getInstance().level.players().stream()
                .flatMap(e -> Stream.of(e.getShoulderEntityRight(), e.getShoulderEntityLeft()))
                .filter(Objects::nonNull)
                .flatMap(n -> n.getIntArray("UUID").stream())
                .map(UUIDUtil::uuidFromIntArray)
                .anyMatch(base.getUUID()::equals);
        //?}
    }
}
