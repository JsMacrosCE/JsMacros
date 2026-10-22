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
 * the cat, which is a tameable animal with a collar and a coat.
 * <p>
 * {@link #getVariant() getVariant} is the registry id of the cat's coat - {@code
 * minecraft:tabby} and the like - with the namespace on the front. That is unlike most of
 * the variant names in this package, which are the bare serialized name such as {@code
 * red_blue}, so a script that compares variant strings across animals will not match here.
 * <p>
 * The collar is there whether or not the cat is tamed, so a collar colour is not evidence of
 * anything having happened to the cat.
 * example:
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

    public CatEntityHelper(Cat base) {
        super(base);
    }

    /**
     * Whether the cat is asleep in a bed or on the floor. This is the generic sleeping flag
     * the game keeps for every mob that can nod off, not the sitting pose, so it is a
     * different figure from {@link TameableEntityHelper#isSitting() isSitting} and from the
     * pose a cat holds while sitting up.
     * example:
     * <pre>
     * const CatEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.CatEntityHelper");
     * const cats = World.getEntities(32, "cat");
     * if (cats !== null) {
     *   for (const entity of cats) {
     *     const cat = CatEntityHelper.class.cast(entity);
     *     if (cat.isSleeping()) {
     *       Chat.log(`asleep at ${cat.getBlockPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this cat is sleeping, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSleeping() {
        return base.isSleeping();
    }

    /**
     * The colour of the cat's collar. The game keeps a colour on every cat whatever its
     * state, so this answers the same {@code red} on a stray that has never been tamed as
     * on one that has.
     *
     * @return the color of this cat's collar.
     * @since 1.8.4
     */
    public DyeColorHelper getCollarColor() {
        return new DyeColorHelper(base.getCollarColor());
    }

    /**
     * The registry id of the cat's coat, with the namespace on the front: {@code
     * minecraft:tabby}, {@code minecraft:black}, and so on. It is looked up in the cat
     * variant registry rather than read off a fixed list, so a pack that adds coats adds
     * answers here too.
     * example:
     * <pre>
     * const CatEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.CatEntityHelper");
     * const cats = World.getEntities(32, "cat");
     * if (cats !== null) {
     *   for (const entity of cats) {
     *     const cat = CatEntityHelper.class.cast(entity);
     *     if (cat.getVariant() === "minecraft:black") {
     *       Chat.log("a black cat");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the variant of this cat.
     * @since 1.8.4
     */
    public String getVariant() {
        return mc.getConnection().registryAccess().lookupOrThrow(Registries.CAT_VARIANT).getKey(base.getVariant().value()).toString();
    }

}
