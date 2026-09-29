package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.monster.zombie.Zombie;
*///? } else {
import net.minecraft.world.entity.monster.Zombie;
//? }

/**
 * the zombie, and the conversion the game has running on it.
 * <p>
 * A zombie left in water long enough turns into a drowned, and the game runs that as a
 * countdown the client is told about, so {@link #isConvertingToDrowned()
 * isConvertingToDrowned} is the flag behind a conversion that is already under way — not a
 * check of whether the zombie is standing in water at this instant.
 * <p>
 * Two kinds of zombie that have a class of their own never start that conversion at all:
 * the drowned and the zombie villager both turn the game-side check off, so on either of
 * them this reads {@code false} for as long as it is loaded. Both of those have their own
 * class, and both of those classes reach this one.
 * example:
 * <pre>
 * const ZombieEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.ZombieEntityHelper");
 * const zombies = World.getEntities(32, "zombie");
 * if (zombies !== null) {
 *   for (const entity of zombies) {
 *     const zombie = ZombieEntityHelper.class.cast(entity);
 *     // the flag is up only once the conversion is already running
 *     if (zombie.isConvertingToDrowned()) {
 *       Chat.log(`a zombie at ${zombie.getPos()} is turning, and has ${zombie.getMaxHealth()} health`);
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
public class ZombieEntityHelper<T extends Zombie> extends MobEntityHelper<T> {

    public ZombieEntityHelper(T base) {
        super(base);
    }

    /**
     * Whether the zombie is already converting to a drowned. The game raises the flag once
     * the water time has been served and starts counting a conversion down, so this is not
     * a check of whether the zombie is in water right now — a zombie that has been taken
     * back out answers {@code true} for as long as the conversion runs.
     * <p>
     * The two kinds of zombie that reach this class through a class of their own — the
     * drowned and the zombie villager — both turn the conversion off in the game, so on
     * either of them this stays {@code false} and the only way to know is that it never
     * changes.
     * example:
     * <pre>
     * const ZombieEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.ZombieEntityHelper");
     * const zombies = World.getEntities(32, "zombie", "drowned", "zombie_villager");
     * if (zombies !== null) {
     *   for (const entity of zombies) {
     *     const zombie = ZombieEntityHelper.class.cast(entity);
     *     // only a plain zombie can be converting; the other two have it switched off
     *     if (zombie.isConvertingToDrowned()) {
     *       Chat.log(`${zombie.getType()} at ${zombie.getPos()} is turning into a drowned`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this zombie is converting to a drowned, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isConvertingToDrowned() {
        return base.isUnderWaterConverting();
    }

}
