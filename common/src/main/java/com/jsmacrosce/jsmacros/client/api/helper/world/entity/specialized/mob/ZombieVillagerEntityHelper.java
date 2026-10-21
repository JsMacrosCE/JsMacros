package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.core.registries.BuiltInRegistries;
import com.jsmacrosce.doclet.DocletCategory;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.monster.zombie.ZombieVillager;
*///? } else {
import net.minecraft.world.entity.monster.ZombieVillager;
//? }

/**
 * Provides access to a zombie villager's conversion state and villager data.
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

    /** Wraps the supplied zombie villager and retains its reference for this helper.
     * @param base zombie villager entity to wrap
     */
    public ZombieVillagerEntityHelper(ZombieVillager base) {
        super(base);
    }

    /**
     * Whether conversion to a villager is in progress. The game exposes a flag, not its
     * conversion countdown.
     *
     * @return {@code true} if this zombie villager is currently being converted back to a
     * villager, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isConvertingToVillager() {
        return base.isConverting();
    }

    /**
     * The registry name of the villager type this zombie villager carries, including its
     * namespace (for example, {@code minecraft:snow}).
     *
     * @return the registry name of this zombie villager's biome type.
     * @since 1.8.4
     */
    public String getVillagerBiomeType() {
        return BuiltInRegistries.VILLAGER_TYPE.getKey(base.getVillagerData().type().value()).toString();
    }

    /**
     * The registry name of the profession this zombie villager carries, including its
     * namespace (or {@code minecraft:none}).
     *
     * @return the registry name of this zombie villager's profession.
     * @since 1.8.4
     */
    public String getProfession() {
        return base.getVillagerData().profession().getRegisteredName();
    }

    /**
     * The trade level carried in this zombie villager's villager data.
     *
     * @return this zombie villager's trade level.
     * @since 1.8.4
     */
    public int getLevel() {
        return base.getVillagerData().level();
    }

}
