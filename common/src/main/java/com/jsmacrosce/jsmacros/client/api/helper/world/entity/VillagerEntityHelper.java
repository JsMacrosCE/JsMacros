package com.jsmacrosce.jsmacros.client.api.helper.world.entity;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.npc.villager.Villager;
*///? } else {
import net.minecraft.world.entity.npc.Villager;
//? }

/**
 * a villager, which is a merchant with a profession, a biome type and a trading level
 * on top of it.
 * <p>
 * All three of those are readable on the client, because the server sends them with the
 * entity, but the trade list that goes with the level is not: see
 * {@link MerchantEntityHelper#getTrades()} for why, and for where the offers actually
 * come from.
 * example:
 * <pre>
 * const villagers = World.getEntities("villager");
 * if (villagers !== null) {
 *   for (const entity of villagers) {
 *     const villager = entity.asVillager();
 *     Chat.log(`a level ${villager.getLevel()} ${villager.getProfession()} `
 *       + `holding ${villager.getExperience()} xp`);
 *   }
 * }
 * </pre>
 *
 * @since 1.6.3
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class VillagerEntityHelper extends MerchantEntityHelper<Villager> {

    public VillagerEntityHelper(Villager e) {
        super(e);
    }

    /**
     * the villager's job, as the registry id of the profession rather than as a
     * translated name, so it is {@code minecraft:armorer} or {@code minecraft:farmer} and
     * not "Armorer". The namespace is on the front, and a profession that has not been
     * registered comes back as the bracketed text {@code [unregistered]} rather than as
     * an id, so a mod that registers its own can be read here like any other.
     * <p>
     * There is also a {@code nitwit} id, which is the entry a villager with no job has
     * rather than a job in its own right.
     * example:
     * <pre>
     * const villagers = World.getEntities("villager");
     * if (villagers !== null) {
     *   for (const entity of villagers) {
     *     const villager = entity.asVillager();
     *     // the id, not the display name, so it compares cleanly
     *     if (villager.getProfession() === "minecraft:farmer") {
     *       Chat.log(`a farmer on ${villager.getExperience()} xp`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the registry id of this villager's profession
     * @since 1.6.3
     */
    @DocletReplaceReturn("VillagerProfession")
    public String getProfession() {
        return base.getVillagerData().profession().getRegisteredName();
    }

    /**
     * the villager's biome type, which is what decides the look rather than the job. The
     * string this returns is the game's own {@code toString()} on the registry holder
     * around the type rather than the type's own name, so it comes out in the holder's
     * {@code Reference{...}} form and includes a Java object hash, which is a different
     * value on every run and is not something to compare or store.
     * <p>
     * That is worth knowing before reaching for this. The readable form of the biome
     * type is the name the registry uses, one of {@code minecraft:plains},
     * {@code minecraft:desert}, {@code minecraft:jungle}, {@code minecraft:savanna},
     * {@code minecraft:snow}, {@code minecraft:swamp} or {@code minecraft:taiga}, and
     * this method is not what hands it back. To log something stable, read the biome at
     * the villager's position instead, which names the place it is standing in rather
     * than the type it was made as.
     * example:
     * <pre>
     * const villagers = World.getEntities("villager");
     * if (villagers !== null) {
     *   for (const entity of villagers) {
     *     const villager = entity.asVillager();
     *     // this string changes from run to run, so it is only useful as a label
     *     Chat.log(`type holder: ${villager.getStyle()}`);
     *     // the biome it is standing in is a name that does not
     *     const at = villager.getPos();
     *     Chat.log(`standing in ${World.getBiomeAt(Math.floor(at.x), Math.floor(at.z))}`);
     *   }
     * }
     * </pre>
     *
     * @return the holder's own string form for this villager's biome type
     * @since 1.6.3
     */
    @DocletReplaceReturn("VillagerStyle")
    public String getStyle() {
        return base.getVillagerData().type().toString();
    }

    /**
     * the villager's trading level, which counts from one to five. One is a villager
     * that has never traded and five is the top of the ladder; the game defines no level
     * below one, so a zero is not a level anybody is on.
     * <p>
     * This is the level itself rather than the experience behind it, and the two are
     * read separately: {@link #getExperience()} is the experience banked, and the game
     * works out for itself when that is enough to move the level up, at thresholds of
     * 10, 70, 150 and 250 experience. The level only ever goes up in the game; a
     * player's reputation with the villager changes what the trades cost rather than the
     * level itself.
     * example:
     * <pre>
     * const villagers = World.getEntities("villager");
     * if (villagers !== null) {
     *       for (const entity of villagers) {
     *         const villager = entity.asVillager();
     *         const level = villager.getLevel();
     *         // one through five, with one being a villager that has not traded
     *         Chat.log(`level ${level} ${villager.getProfession()} `
     *           + `on ${villager.getExperience()} xp`);
     *       }
     *     }
     * </pre>
     *
     * @return this villager's trading level, from one to five
     * @since 1.6.3
     */
    public int getLevel() {
        return base.getVillagerData().level();
    }

}
