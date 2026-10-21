package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.DyeColorHelper;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.feline.Cat;
*///? } else {
import net.minecraft.world.entity.animal.Cat;
//?}

/**
 * A tameable cat with a coat variant and collar colour.
 * <p>
 * {@link #getVariant() getVariant} is the registry id of the cat's coat - {@code
 * minecraft:tabby} and the like - with the namespace on the front. That is unlike most of
 * the variant names in this package, which are the bare serialized name such as {@code
 * red_blue}, so a script that compares variant strings across animals will not match here.
 * <p>
 * The collar is there whether or not the cat is tamed, so a collar colour is not evidence of
 * anything having happened to the cat.
 * <pre>
 * const CatEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.CatEntityHelper");
 * const cats = World.getEntities(32, "cat");
 * if (cats !== null) {
 *   for (const entity of cats) {
  *     const cat = CatEntityHelper.class.cast(entity);
 *     Chat.log(`a ${cat.getVariant()} cat in ${cat.getCollarColor().getName()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class CatEntityHelper extends TameableEntityHelper<Cat> {
    private static final Minecraft mc = Minecraft.getInstance();

    /**
     * Wraps the supplied cat, forwarding its reference to the superclass for storage.
     *
     * @param base the cat entity to wrap
     */
    public CatEntityHelper(Cat base) {
        super(base);
    }

    /**
     * Whether the cat has a sleeping position. This is distinct from its sitting order
     * ({@link TameableEntityHelper#isSitting() isSitting}) and sitting pose.
     *
     * @return {@code true} if this cat is sleeping, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSleeping() {
        return base.isSleeping();
    }

    /**
     * Returns the cat's collar colour, whether or not it is tamed.
     *
     * @return the color of this cat's collar.
     * @since 1.8.4
     */
    public DyeColorHelper getCollarColor() {
        return new DyeColorHelper(base.getCollarColor());
    }

    /**
     * Returns the coat's registry id, including its namespace (for example,
     * {@code minecraft:tabby}). The id is obtained from the cat variant registry.
     *
     * @return the namespaced registry id of this cat's coat.
     * @since 1.8.4
     */
    public String getVariant() {
        return mc.getConnection().registryAccess().lookupOrThrow(Registries.CAT_VARIANT).getKey(base.getVariant().value()).toString();
    }

}
