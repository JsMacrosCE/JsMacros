package com.jsmacrosce.jsmacros.client.api.helper.world.entity;

import net.minecraft.world.entity.npc.Villager;
import com.jsmacrosce.doclet.DocletReplaceReturn;

/**
 * @since 1.6.3
 */
@SuppressWarnings("unused")
public class VillagerEntityHelper extends MerchantEntityHelper<Villager> {

    public VillagerEntityHelper(Villager e) {
        super(e);
    }

    /**
     * @return
     * @since 1.6.3
     */
    @DocletReplaceReturn("VillagerProfession")
    public String getProfession() {
        return base.getVillagerData().profession().getRegisteredName();
    }

    /**
     * @return
     * @since 1.6.3
     */
    @DocletReplaceReturn("VillagerStyle")
    public String getStyle() {
        return base.getVillagerData().type().toString();
    }

    /**
     * @return
     * @since 1.6.3
     */
    public int getLevel() {
        return base.getVillagerData().level();
    }

}
