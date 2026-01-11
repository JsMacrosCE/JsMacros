package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.animal.frog.Frog;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class FrogEntityHelper extends AnimalEntityHelper<Frog> {
    Minecraft mc = Minecraft.getInstance();

    public FrogEntityHelper(Frog base) {
        super(base);
    }

    /**
     * @return the variant of this frog.
     * @since 1.8.4
     */
    @DocletReplaceReturn("FrogVariant")
    public String getVariant() {
        return mc.getConnection().registryAccess().lookupOrThrow(Registries.FROG_VARIANT).getKey(base.getVariant().value()).toString();
    }

    /**
     * @return the target of this frog, or {@code null} if it has none.
     * @since 1.8.4
     */
    @Nullable
    public EntityHelper<?> getTarget() {
        return base.getTongueTarget().map(EntityHelper::create).orElse(null);
    }

    /**
     * @return {@code true} if this frog is croaking, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCroaking() {
        return base.croakAnimationState.isStarted();
    }

}
