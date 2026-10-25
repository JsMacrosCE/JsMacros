package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.boss;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinPhaseType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides access to the ender dragon's current phase and its eight body parts.
 * <pre>
 * const dragons = World.getEntities(128, "ender_dragon");
 * if (dragons !== null) {
 *   for (const entity of dragons) {
 *     if (!(entity instanceof EntityHelpers.EnderDragon)) continue;
 *     Chat.log(`phase ${entity.getPhase()}, over ${entity.getBodyParts().size()} parts`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class EnderDragonEntityHelper extends MobEntityHelper<EnderDragon> {

    /**
     * Wraps the supplied ender dragon and retains its reference for this helper.
     *
     * @param base ender dragon to wrap
     */
    public EnderDragonEntityHelper(EnderDragon base) {
        super(base);
    }

    /**
     * Returns the current phase's game-defined name.
     * @return the current phase name
     * @since 1.8.4
     */
    @DocletReplaceReturn("DragonPhase")
    public String getPhase() {
        return ((MixinPhaseType) base.getPhaseManager().getCurrentPhase().getPhase()).getName();
    }

    /**
     * Returns the body part at {@code index}: 0 is the head, 1 the neck, 2 the body, 3–5 the tail segments, and 6–7 the wings.
     * @param index body-part index from 0 through 7; values outside this range cause an array-index exception
     * @return the selected body part
     * @since 1.8.4
     */
    public EntityHelper<?> getBodyPart(int index) {
        return EntityHelper.create(base.getSubEntities()[index]);
    }

    /**
     * Returns all eight body parts in index order.
     * @return the dragon's body parts, from head through wings
     * @since 1.8.4
     */
    public List<? extends EntityHelper<?>> getBodyParts() {
        return Arrays.stream(base.getSubEntities()).map(EntityHelper::create).collect(Collectors.toList());
    }

    /**
     * Returns body parts whose names exactly match {@code name}; tail and wing each match three parts.
     * @param name body-part name: {@code head}, {@code neck}, {@code body}, {@code tail}, or {@code wing}
     * @return matching parts in body-part order, or an empty list if none match
     * @since 1.8.4
     */
    @DocletReplaceParams("name: DragonBodyPart")
    public List<? extends EntityHelper<?>> getBodyParts(String name) {
        return Arrays.stream(base.getSubEntities()).filter(e -> e.name.equals(name)).map(EntityHelper::create).collect(Collectors.toList());
    }

}
