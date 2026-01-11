package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.monster.ZombieVillager;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class ZombieVillagerEntityHelper extends ZombieEntityHelper<ZombieVillager> {

    public ZombieVillagerEntityHelper(ZombieVillager base) {
        super(base);
    }

    /**
     * @return {@code true} if this zombie villager is currently being converted back to a villager,
     * {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isConvertingToVillager() {
        return base.isConverting();
    }

    /**
     * @return the type of biome the villager belonged to it was converted to a zombie.
     * @since 1.8.4
     */
    public String getVillagerBiomeType() {
        return BuiltInRegistries.VILLAGER_TYPE.getKey(base.getVillagerData().type().value()).toString();
    }

    /**
     * @return the profession of the villager before it was converted to a zombie.
     * @since 1.8.4
     */
    public String getProfession() {
        return base.getVillagerData().profession().getRegisteredName();
    }

    /**
     * @return the level of the villager before it was converted to a zombie.
     * @since 1.8.4
     */
    public int getLevel() {
        return base.getVillagerData().level();
    }

}
