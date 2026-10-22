package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.allay.Allay;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinAllayEntity;

/**
 * the allay, which is the one passive mob in this package that flies and picks things up
 * rather than being something a player keeps.
 * <p>
 * The three methods are the three states an allay is usually in the middle of: it is
 * dancing to a jukebox, it is holding the thing it was given, and it is inside or outside
 * the cooldown after breeding one. {@link #canDuplicate() canDuplicate} is worth reading
 * twice because it starts out {@code true}: the game allows the first duplication without
 * anything having happened first, and only a freshly used allay answers {@code false}.
 * example:
 * <pre>
 * const AllayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.AllayEntityHelper");
 * const allays = World.getEntities(32, "allay");
 * if (allays !== null) {
 *   for (const entity of allays) {
 *     const allay = AllayEntityHelper.class.cast(entity);
 *     Chat.log(`dancing ${allay.isDancing()}, `
 *       + `holding ${allay.isHoldingItem()}, `
 *       + `ready to duplicate ${allay.canDuplicate()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class AllayEntityHelper extends MobEntityHelper<Allay> {

    public AllayEntityHelper(Allay base) {
        super(base);
    }

    /**
     * Whether the allay is dancing, which is what the game puts it into while a jukebox
     * within a few blocks of it is playing. The game clears it as soon as the jukebox stops,
     * is taken away, or is replaced by a different block, so it does not outlive the music.
     * <p>
     * This is also the state an allay has to be in before it will accept an item that
     * duplicates it.
     *
     * @return {@code true} if this allay is dancing, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isDancing() {
        return base.isDancing();
    }

    /**
     * Whether the allay would accept an item that duplicates it right now. The game allows
     * this from the moment an allay spawns and only takes it away for a while after one has
     * been used, so an allay that has never been given anything answers {@code true}.
     * <p>
     * The duplication also needs the allay to be dancing, so neither of the two conditions
     * is enough on its own.
     *
     * @return {@code true} if this allay can be duplicated, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean canDuplicate() {
        return ((MixinAllayEntity) base).invokeCanDuplicate();
    }

    /**
     * Whether anything is in the allay's main hand. An allay is given one item this way and
     * holds on to it until it uses it, so this is a good way of telling an allay that is
     * carrying something from one that is empty-handed, whatever it happens to be doing.
     *
     * @return {@code true} if this allay is holding an item, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isHoldingItem() {
        return base.hasItemInHand();
    }

}
