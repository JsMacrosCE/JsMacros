package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.animal.frog.Frog;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

/**
 * the frog, which comes in a handful of varieties and spends its time either croaking at
 * something small or shooting its tongue out at it.
 * <p>
 * {@link #getVariant() getVariant} is the registry id with the namespace on the front -
 * {@code minecraft:temperate} and the like - which puts it with the cat rather than with the
 * axolotls and parrots whose variant names have no namespace.
 * <p>
 * {@link #getTarget() getTarget} is not the frog's enemy. It is whatever the frog has got
 * its tongue stuck to or is reaching for, which the game restricts to slimes and magma cubes,
 * so it stays {@code null} for most of a frog's life.
 * example:
 * <pre>
 * const FrogEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.FrogEntityHelper");
 * const frogs = World.getEntities(32, "frog");
 * if (frogs !== null) {
 *   for (const entity of frogs) {
 *     const frog = FrogEntityHelper.class.cast(entity);
 *     Chat.log(`${frog.getVariant()}, croaking ${frog.isCroaking()}`);
 *     const prey = frog.getTarget();
 *     if (prey !== null) {
 *       Chat.log(`  tongue out at ${prey.getType()}`);
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
public class FrogEntityHelper extends AnimalEntityHelper<Frog> {
    Minecraft mc = Minecraft.getInstance();

    public FrogEntityHelper(Frog base) {
        super(base);
    }

    /**
     * The registry id of the frog's variety, with the namespace on the front: {@code
     * minecraft:temperate}, {@code minecraft:swamp}, and the rest. It is looked up in the
     * frog variant registry, so a pack that adds varieties adds answers here too.
     *
     * @return the variant of this frog.
     * @since 1.8.4
     */
    @DocletReplaceReturn("FrogVariant")
    public String getVariant() {
        return mc.getConnection().registryAccess().lookupOrThrow(Registries.FROG_VARIANT).getKey(base.getVariant().value()).toString();
    }

    /**
     * Whatever the frog has its tongue out at, or {@code null} when it has not got one out.
     * This is not the frog's enemy and not the thing it is fleeing from: it is the small
     * creature it is trying to eat, and the game only ever puts a slime or a magma cube
     * there, so it is {@code null} for most of a frog's life.
     * example:
     * <pre>
     * const FrogEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.FrogEntityHelper");
     * const frogs = World.getEntities(16, "frog");
     * if (frogs !== null) {
     *   for (const entity of frogs) {
     *     const frog = FrogEntityHelper.class.cast(entity);
     *     const prey = frog.getTarget();
     *     if (prey !== null) {
     *       Chat.log(`going for the ${prey.getType()} at ${prey.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the target of this frog, or {@code null} if it has none.
     * @since 1.8.4
     */
    @Nullable
    public EntityHelper<?> getTarget() {
        return base.getTongueTarget().map(EntityHelper::create).orElse(null);
    }

    /**
     * Whether the frog is mid-croak. The figure comes from the croak animation rather than
     * from a flag, so it goes up when the frog starts croaking and comes down when it stops
     * rather than lasting for the whole of whatever made it start.
     *
     * @return {@code true} if this frog is croaking, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCroaking() {
        return base.croakAnimationState.isStarted();
    }

}
