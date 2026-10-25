package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.animal.frog.Frog;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

/**
 * Provides variant, tongue-target, and croaking-state information for a frog.
 * <p>
 * <pre>
 * const frogs = World.getEntities(32, "frog");
 * if (frogs !== null) {
 *   for (const entity of frogs) {
 *     if (!(entity instanceof EntityHelpers.Frog)) continue;
 *     Chat.log(`${entity.getVariant()}, croaking ${entity.isCroaking()}`);
 *     const prey = entity.getTarget();
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

    /**
     * Wraps the supplied frog and retains its reference for this helper.
     *
     * @param base frog entity to wrap
     */
    public FrogEntityHelper(Frog base) {
        super(base);
    }

    /**
     * Returns the namespaced registry id of this frog's variant, including variants added by data packs.
     *
     * @return the variant of this frog.
     * @since 1.8.4
     */
    @DocletReplaceReturn("FrogVariant")
    public String getVariant() {
        return mc.getConnection().registryAccess().lookupOrThrow(Registries.FROG_VARIANT).getKey(base.getVariant().value()).toString();
    }

    /**
     * Returns the entity currently recorded as this frog's tongue target, if that entity is present in its level.
     *
     * @return the target of this frog, or {@code null} if it has none.
     * @since 1.8.4
     */
    @Nullable
    public EntityHelper<?> getTarget() {
        return base.getTongueTarget().map(EntityHelper::create).orElse(null);
    }

    /**
     * Returns whether the frog's croaking animation state has started.
     *
     * @return {@code true} if this frog is croaking, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCroaking() {
        return base.croakAnimationState.isStarted();
    }

}
