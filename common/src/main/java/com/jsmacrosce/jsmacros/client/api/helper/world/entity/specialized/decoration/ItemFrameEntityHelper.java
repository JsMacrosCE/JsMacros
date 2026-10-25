package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration;

import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

/**
 * Provides the item frame's type, displayed-item rotation, and item stack.
 * <pre>
 * const frames = World.getEntities(32, "item_frame", "glow_item_frame");
 * if (frames !== null) {
 *   for (const entity of frames) {
 *     if (!(entity instanceof EntityHelpers.ItemFrame)) continue;
 *     // The query includes both frame types; distinguish them with isGlowingFrame().
 *     Chat.log(`${entity.isGlowingFrame() ? "glow" : "plain"} frame, rotation ${entity.getRotation()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class ItemFrameEntityHelper extends EntityHelper<ItemFrame> {

    /** Wraps the supplied item frame and retains its reference.
     * @param base the item frame to wrap
     */
    public ItemFrameEntityHelper(ItemFrame base) {
        super(base);
    }

    /**
     * Returns whether the wrapped frame is a glow item frame.
     *
     * @return {@code true} if the item frame is glowing, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isGlowingFrame() {
        return base instanceof GlowItemFrame;
    }

    /**
     * Returns the framed item's rotation as one of eight 45-degree steps, from {@code 0} to {@code 7}.
     *
     * @return the framed item's rotation index.
     * @since 1.8.4
     */
    public int getRotation() {
        return base.getRotation();
    }

    /**
     * Returns the current item stack; an empty frame returns an empty stack.
     *
     * @return the framed item, or an empty stack if the frame holds no item.
     * @since 1.8.4
     */
    public ItemStackHelper getItem() {
        return new ItemStackHelper(base.getItem());
    }

}
