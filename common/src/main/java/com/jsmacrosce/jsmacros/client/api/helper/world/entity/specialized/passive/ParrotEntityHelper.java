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
 * A helper for a parrot entity.
 * <p>
 * {@link #isSitting() isSitting} reports the current sitting pose, unlike
 * {@link TameableEntityHelper#isSitting() isSitting}, which reports the sit order.
 * <p>
 * {@link #isFlying() isFlying} reports whether the parrot is off the ground.
 * <pre>
 * const parrots = World.getEntities(32, "parrot");
 * if (parrots !== null) {
 *   for (const entity of parrots) {
 *     if (!(entity instanceof EntityHelpers.Parrot)) continue;
 *     Chat.log(`${entity.getVariant()} parrot: flying ${entity.isFlying()}, `
 *       + `sitting ${entity.isSitting()}, `
 *       + `partying ${entity.isPartying()}, `
 *       + `standing ${entity.isStanding()}`);
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

    /**
     * Wraps the supplied parrot and retains its reference for this helper.
     *
     * @param base parrot entity to wrap
     */
    public ParrotEntityHelper(Parrot base) {
        super(base);
    }

    /**
     * Returns the parrot variant's serialized name, such as {@code red_blue}.
     *
     * @return the variant of this parrot.
     * @since 1.8.4
     */
    @DocletReplaceReturn("ParrotVariant")
    public String getVariant() {
        return base.getVariant().getSerializedName();
    }

    /**
     * Returns whether the parrot is currently in its sitting pose, not whether it was ordered to sit.
     *
     * @return {@code true} if this parrot is sitting, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSitting() {
        return base.isInSittingPose();
    }

    /**
     * Returns whether the parrot is off the ground.
     *
     * @return {@code true} if this parrot is flying, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isFlying() {
        return base.isFlying();
    }

    /**
     * Returns whether the parrot's party state is active.
     *
     * @return {@code true} if this parrot is dancing to music, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isPartying() {
        return base.isPartyParrot();
    }

    /**
     * Returns whether the parrot is neither partying, flying, nor sitting. This is derived from those three states.
     *
     * @return {@code true} if this parrot is just standing around, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isStanding() {
        return !isPartying() && !isFlying() && !isSitting();
    }

    /**
     * Checks whether this sitting parrot's UUID appears in either shoulder-entity tag of a player in the client level.
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
