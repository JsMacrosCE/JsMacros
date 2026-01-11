package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.client.Minecraft;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.animal.Parrot;
import com.jsmacrosce.doclet.DocletReplaceReturn;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class ParrotEntityHelper extends TameableEntityHelper<Parrot> {

    public ParrotEntityHelper(Parrot base) {
        super(base);
    }

    /**
     * @return the variant of this parrot.
     * @since 1.8.4
     */
    @DocletReplaceReturn("ParrotVariant")
    public String getVariant() {
        return base.getVariant().getSerializedName();
    }

    /**
     * @return {@code true} if this parrot is sitting, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSitting() {
        return base.isInSittingPose();
    }

    /**
     * @return {@code true} if this parrot is flying, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isFlying() {
        return base.isFlying();
    }

    /**
     * @return {@code true} if this parrot is dancing to music, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isPartying() {
        return base.isPartyParrot();
    }

    /**
     * @return {@code true} if this parrot is just standing around, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isStanding() {
        return !isPartying() && !isFlying() && !isSitting();
    }

    /**
     * @return {@code true} if this parrot is sitting on any player's shoulder, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isSittingOnShoulder() {
        if (!isSitting()) return false;
        //? if >1.21.8 {
        /*// TODO: Lots of this changed in 1.21.9/1.21.10, need to fix, this is just an estimation
        /^return Minecraft.getInstance().level.players().stream()
            .flatMap(e -> {
                return Stream.of(e.getEntityData().get(e.DATA_SHOULDER_PARROT_LEFT), e.getEntityData().get(e.DATA_SHOULDER_PARROT_RIGHT));
            })
            .filter(Optional::isPresent)
            .flatMap(n -> n.get().getIntArray("UUID").stream())
            .map(UUIDUtil::uuidFromIntArray)
            .anyMatch(base.getUUID()::equals);^/
        return false;
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
