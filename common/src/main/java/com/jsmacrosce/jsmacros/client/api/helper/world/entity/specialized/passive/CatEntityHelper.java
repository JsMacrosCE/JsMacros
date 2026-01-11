package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.animal.Cat;
import com.jsmacrosce.jsmacros.client.api.helper.DyeColorHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class CatEntityHelper extends TameableEntityHelper<Cat> {
    private static final Minecraft mc = Minecraft.getInstance();

    public CatEntityHelper(Cat base) {
        super(base);
    }

    /**
     * @return {@code true} if this cat is sleeping, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSleeping() {
        return base.isSleeping();
    }

    /**
     * @return the color of this cat's collar.
     * @since 1.8.4
     */
    public DyeColorHelper getCollarColor() {
        return new DyeColorHelper(base.getCollarColor());
    }

    /**
     * @return the variant of this cat.
     * @since 1.8.4
     */
    public String getVariant() {
        return mc.getConnection().registryAccess().lookupOrThrow(Registries.CAT_VARIANT).getKey(base.getVariant().value()).toString();
    }

}
