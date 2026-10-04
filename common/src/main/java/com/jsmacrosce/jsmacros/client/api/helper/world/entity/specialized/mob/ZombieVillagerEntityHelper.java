package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.core.registries.BuiltInRegistries;
import com.jsmacrosce.doclet.DocletCategory;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.monster.zombie.ZombieVillager;
*///? } else {
import net.minecraft.world.entity.monster.ZombieVillager;
//? }

/**
 * the zombie villager, which is a villager with a conversion running the other way.
 * <p>
 * The game keeps a whole villager record on a zombie villager — a biome type, a profession
 * and a trade level — and carries it through the cure, so the three getters here read that
 * record back rather than reading anything about the zombie.
 * {@link #isConvertingToVillager() isConvertingToVillager} is the conversion itself.
 * <p>
 * The two are set from different places: the type from the biome the zombie villager is
 * standing in, and the profession picked by the game when the zombie villager is made. So
 * these describe the villager it is going back to rather than a villager that was ever
 * here.
 * <p>
 * The other conversion a zombie class offers runs the other way and cannot start here: a
 * zombie villager has the water conversion switched off in the game, so the
 * {@code isConvertingToDrowned} call it inherits always answers {@code false}.
 * example:
 * <pre>
 * const ZombieVillagerEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.ZombieVillagerEntityHelper");
 * const zombievillagers = World.getEntities(32, "zombie_villager");
 * if (zombievillagers !== null) {
 *   for (const entity of zombievillagers) {
 *     const villager = ZombieVillagerEntityHelper.class.cast(entity);
 *     if (villager.isConvertingToVillager()) {
 *       Chat.log(`turning back at ${villager.getPos()}, was a ${villager.getProfession()} of level ${villager.getLevel()}`);
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
public class ZombieVillagerEntityHelper extends ZombieEntityHelper<ZombieVillager> {

    public ZombieVillagerEntityHelper(ZombieVillager base) {
        super(base);
    }

    /**
     * Whether the conversion back to a villager is running. The game raises the flag as the
     * conversion starts and it runs on a countdown the game keeps, so this is the whole of
     * the conversion a script can see — the countdown itself is not on this class.
     * <p>
     * This is the opposite direction to the {@code isConvertingToDrowned} flag the zombie
     * class offers, and the two cannot both be up: the game switches the water conversion
     * off on a zombie villager altogether, so that one stays {@code false}.
     * example:
     * <pre>
     * const ZombieVillagerEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.ZombieVillagerEntityHelper");
     * const zombievillagers = World.getEntities(32, "zombie_villager");
     * if (zombievillagers !== null) {
     *   for (const entity of zombievillagers) {
     *     const villager = ZombieVillagerEntityHelper.class.cast(entity);
     *     if (villager.isConvertingToVillager()) {
     *       // the water conversion is off on this mob, so the other one stays false
     *       Chat.log(`turning back, drowned: ${villager.isConvertingToDrowned()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this zombie villager is currently being converted back to a
     * villager, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isConvertingToVillager() {
        return base.isConverting();
    }

    /**
     * The biome villager type this zombie villager carries, as the registry name of that
     * type — the plains, desert, jungle, savanna, snow, swamp or taiga one, with the
     * {@code minecraft:} namespace in front of it.
     * <p>
     * The game picks the type from the biome the zombie villager is standing in when it is
     * created and then keeps it, and the cure copies it onto the villager it turns back
     * into. So this is where the villager it becomes belongs, which need not be the biome
     * it is standing in now.
     * example:
     * <pre>
     * const ZombieVillagerEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.ZombieVillagerEntityHelper");
     * const zombievillagers = World.getEntities(32, "zombie_villager");
     * if (zombievillagers !== null) {
     *   for (const entity of zombievillagers) {
     *     const villager = ZombieVillagerEntityHelper.class.cast(entity);
     *     // a namespaced registry name, so a plain string comparison is enough
     *     if (villager.getVillagerBiomeType() === "minecraft:snow") {
     *       Chat.log(`a snow villager at ${villager.getPos()}, now in ${villager.getBiome()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the type of biome the villager belonged to it was converted to a zombie.
     * @since 1.8.4
     */
    public String getVillagerBiomeType() {
        return BuiltInRegistries.VILLAGER_TYPE.getKey(base.getVillagerData().type().value()).toString();
    }

    /**
     * The profession this zombie villager carries, as the registry name of that
     * profession — {@code minecraft:farmer}, {@code minecraft:cleric} and so on, or
     * {@code minecraft:none} for one that never took a job.
     * <p>
     * The game picks a profession at random out of the registry when the zombie villager is
     * created and then keeps it, and the cure copies it onto the villager it turns back
     * into. So this is the job the villager will come back with, not a record of a villager
     * that existed before.
     * example:
     * <pre>
     * const ZombieVillagerEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.ZombieVillagerEntityHelper");
     * const zombievillagers = World.getEntities(32, "zombie_villager");
     * if (zombievillagers !== null) {
     *   for (const entity of zombievillagers) {
     *     const villager = ZombieVillagerEntityHelper.class.cast(entity);
     *     // a namespaced registry name, so a plain string comparison is enough
     *     if (villager.getProfession() === "minecraft:butcher") {
     *       Chat.log(`a butcher at ${villager.getPos()}, level ${villager.getLevel()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the profession of the villager before it was converted to a zombie.
     * @since 1.8.4
     */
    public String getProfession() {
        return base.getVillagerData().profession().getRegisteredName();
    }

    /**
     * The trade level the zombie villager carries, which is the number of trade tiers the
     * villager it becomes can reach rather than a level in a skill tree. The game clamps
     * the number to at least 1 and stops levelling a villager at 5.
     * example:
     * <pre>
     * const ZombieVillagerEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.ZombieVillagerEntityHelper");
     * const zombievillagers = World.getEntities(32, "zombie_villager");
     * if (zombievillagers !== null) {
     *   for (const entity of zombievillagers) {
     *     const villager = ZombieVillagerEntityHelper.class.cast(entity);
     *     // one through five, and the top of that range is the best a villager gets
     *     if (villager.getLevel() >= 5) {
     *       Chat.log(`a maxed ${villager.getProfession()} at ${villager.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the level of the villager before it was converted to a zombie.
     * @since 1.8.4
     */
    public int getLevel() {
        return base.getVillagerData().level();
    }

}
