package com.jsmacrosce.jsmacros.client.api.helper.world.entity;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.npc.villager.Villager;
*///? } else {
import net.minecraft.world.entity.npc.Villager;
//? }

/**
 * A villager helper exposing the villager's profession, type, level and experience.
 * <p>
 * The trade list is not available from the entity on the client; see
 * {@link MerchantEntityHelper#getTrades()} for the client-side source of displayed offers.
 * <pre>
 * const villagers = World.getEntities("villager");
 * if (villagers !== null) {
 *   for (const entity of villagers) {
 *     if (!(entity instanceof EntityHelpers.Villager)) continue;
 *     Chat.log(`a level ${entity.getLevel()} ${entity.getProfession()} `
 *       + `holding ${entity.getExperience()} xp`);
 *   }
 * }
 * </pre>
 *
 * @since 1.6.3
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class VillagerEntityHelper extends MerchantEntityHelper<Villager> {

    /** Wraps the supplied villager, retaining the same reference.
     * @param e villager to wrap
     */
    public VillagerEntityHelper(Villager e) {
        super(e);
    }

    /**
     * Returns the profession's registered name, including its namespace, rather than
     * its translated display name. A villager without a job has the {@code nitwit}
     * profession.
     *
     * @return the registry id of this villager's profession
     * @since 1.6.3
     */
    @DocletReplaceReturn("VillagerProfession")
    public String getProfession() {
        return base.getVillagerData().profession().getRegisteredName();
    }

    /**
     * Returns the villager's biome type, which determines its appearance rather than
     * its profession. This is the holder's {@code toString()} representation, not a
     * stable registry id; use the biome at the villager's position to identify where it
     * currently stands.
     *
     * @return the holder's own string form for this villager's biome type
     * @since 1.6.3
     */
    @DocletReplaceReturn("VillagerStyle")
    public String getStyle() {
        return base.getVillagerData().type().toString();
    }

    /**
     * Returns the villager's trading level, clamped by the game to a minimum of one
     * and with five as the maximum. This is distinct from accumulated trading
     * experience, returned by {@link #getExperience()}.
     *
     * @return this villager's trading level, from one to five
     * @since 1.6.3
     */
    public int getLevel() {
        return base.getVillagerData().level();
    }

}
