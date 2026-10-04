package com.jsmacrosce.jsmacros.client.api.helper.world.entity;

import net.minecraft.world.entity.Mob;
import com.jsmacrosce.doclet.DocletCategory;

/**
 * a mob: anything the game gives a brain and a target, which covers the hostile mobs,
 * the animals and the villagers as much as it covers the creatures usually meant by the
 * word.
 * <p>
 * Nothing here changes what a mob does. A wrapper is a read on the mob as the client
 * currently believes it to be, and a mob decides for itself on the server, so a flag
 * read through one of these is the last thing the client was told rather than a live
 * decision.
 * <p>
 * There is no {@code asMob()} to narrow an {@code EntityHelper} down to this, the way
 * there is one for a living entity or a player, so a script that wants one of these
 * casts for itself. The entity has to really be a mob first, which filtering by type
 * with {@code World.getEntities(String...)} is the way to arrange.
 * example:
 * <pre>
 * const MobEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper");
 * const mobs = World.getEntities("zombie", "skeleton", "creeper");
 * if (mobs !== null) {
 *   for (const entity of mobs) {
 *     const mob = MobEntityHelper.class.cast(entity);
 *     if (mob.isAiDisabled()) {
 *       Chat.log(`a ${mob.getType()} near ${mob.getPos()} has no AI and is not going anywhere`);
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
public class MobEntityHelper<T extends Mob> extends LivingEntityHelper<T> {

    public MobEntityHelper(T base) {
        super(base);
    }

    /**
     * the mob's aggressive flag, which is the one the game sets once the mob has picked
     * something to go after. Piglin and raider AI tie it straight to their chosen
     * target, so on those mobs it follows the target rather than a swing at it, and a
     * mob can read {@code true} while standing still and waiting.
     * <p>
     * It is a flag rather than a check of what the mob is doing on this tick, so a mob
     * whose flag has not been cleared can still report {@code true} after it has stopped
     * caring.
     * example:
     * <pre>
     * const MobEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper");
     * const zombies = World.getEntities(30, "zombie");
     * if (zombies !== null) {
     *   for (const entity of zombies) {
     *     const mob = MobEntityHelper.class.cast(entity);
     *     if (mob.isAttacking()) {
     *       Chat.log(`the zombie at ${mob.getPos()} has something it is going after`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the entity's aggressive flag is set, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isAttacking() {
        return base.isAggressive();
    }

    /**
     * Mobs which have there AI disabled don't move, attack, or interact with the world by
     * themselves.
     * <p>
     * This is a flag on the mob rather than a pause, so a mob with its AI off is still
     * there, still has its position tracked and still reacts to being hit; what it stops
     * doing is choosing to do anything. Nothing on this class sets the flag, so a script
     * that wants a mob's AI off has to go through a command or a packet.
     * example:
     * <pre>
     * const MobEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper");
     * const mobs = World.getEntities(20);
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     if (!entity.is("zombie", "skeleton", "creeper", "spider")) {
     *       continue;
     *     }
     *     const mob = MobEntityHelper.class.cast(entity);
     *     if (mob.isAiDisabled()) {
     *       // the flag stops it choosing to act, not the world from updating it
     *       Chat.log(`frozen at ${mob.getPos()} on ${mob.getHealth()} health`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the entity's AI is disabled, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isAiDisabled() {
        return base.isNoAi();
    }

}
