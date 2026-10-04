package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration;

import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

/**
 * an end crystal, the healing crystal on the obsidian pillars in the end and the thing the
 * ender dragon is fought with.
 * <p>
 * A crystal is what restores the dragon's health, so the two things worth reading are whether
 * the dragon is currently using it, which is what {@link #getBeamTarget() getBeamTarget()} is
 * for, and whether its base is displayed. Despite its name, {@link #isNatural() isNatural()}
 * reports that display flag rather than proving who placed the crystal.
 * example:
 * <pre>
 * const EndCrystalEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.EndCrystalEntityHelper");
 * const crystals = World.getEntities(64, "end_crystal");
 * if (crystals !== null) {
 *   for (const entity of crystals) {
 *     const crystal = EndCrystalEntityHelper.class.cast(entity);
 *     const beam = crystal.getBeamTarget();
 *     if (beam !== null) {
 *       // a target means the dragon has latched onto this one
 *       Chat.log(`the dragon is healing from the crystal at ${entity.getPos()}`);
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
public class EndCrystalEntityHelper extends EntityHelper<EndCrystal> {

    public EndCrystalEntityHelper(EndCrystal base) {
        super(base);
    }

    /**
     * Tests whether the crystal displays a base, usually indicative it was spawned naturally. A
     * crystal can be created with this flag independently of whether a player placed it.
     *
     * The value is stored on the crystal. The renderer uses it to decide
     * whether the bedrock block under the crystal is drawn at all, and it is saved and sent
     * with the entity under the name {@code ShowBottom}, so it survives a chunk reload.
     * Nothing on this class sets it, so the answer is whatever the game last decided rather
     * than a way to tell who placed the crystal.
     * example:
     * <pre>
     * const EndCrystalEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.EndCrystalEntityHelper");
     * const crystals = World.getEntities(64, "end_crystal");
     * if (crystals !== null) {
     *   for (const entity of crystals) {
     *     const crystal = EndCrystalEntityHelper.class.cast(entity);
     *     // the bedrock base under the crystal, which is a setting rather than a
     *     // question of who placed it
     *     if (crystal.isNatural()) {
     *       Chat.log(`has a bedrock base at ${entity.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the crystal displays its base.
     * @since 1.8.4
     */
    public boolean isNatural() {
        return base.showsBottom();
    }

    /**
     * the block the crystal's healing beam points at, or {@code null} if there is none.
     * <p>
     * This is the block the dragon is drawing the crystal's healing through, so a non-{@code null}
     * answer means this crystal is the one currently keeping the dragon alive. It is set by the
     * dragon and is {@code null} on a crystal nothing has latched onto, which is the normal
     * state for a crystal that is just sitting there. Nothing on this class sets it, so a
     * script cannot make a crystal heal by writing to it.
     * example:
     * <pre>
     * const EndCrystalEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.EndCrystalEntityHelper");
     * const crystals = World.getEntities(64, "end_crystal");
     * if (crystals !== null) {
     *   for (const entity of crystals) {
     *     const crystal = EndCrystalEntityHelper.class.cast(entity);
     *     const target = crystal.getBeamTarget();
     *     // null on an idle crystal, and a block position on one the dragon is using
     *     if (target !== null) {
     *       Chat.log(`beam running to ${target.getX()}, ${target.getY()}, ${target.getZ()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the target of the crystal's beam, or {@code null} if there is none.
     * @since 1.8.4
     */
    @Nullable
    public BlockPosHelper getBeamTarget() {
        return base.getBeamTarget() == null ? null : new BlockPosHelper(base.getBeamTarget());
    }

}
