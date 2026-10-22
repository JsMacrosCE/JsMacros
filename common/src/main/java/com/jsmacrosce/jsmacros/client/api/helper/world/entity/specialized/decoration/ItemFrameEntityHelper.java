package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration;

import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

/**
 * an item frame, the entity that puts a map, a picture or an item on a wall.
 * <p>
 * The kind of frame is not chosen by the class: there is one class for both, and
 * {@link #isGlowingFrame() isGlowingFrame()} is what tells a glow item frame from a plain one.
 * They are separate entities in the game, so a script that has found item frames should check
 * this rather than assume which it has.
 * example:
 * <pre>
 * const ItemFrameEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.ItemFrameEntityHelper");
 * const frames = World.getEntities(32, "item_frame", "glow_item_frame");
 * if (frames !== null) {
 *   for (const entity of frames) {
 *     const frame = ItemFrameEntityHelper.class.cast(entity);
 *     // both kinds come back from the same search, and this is how they are told apart
 *     Chat.log(`${frame.isGlowingFrame() ? "glow" : "plain"} frame, rotation ${frame.getRotation()}`);
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

    public ItemFrameEntityHelper(ItemFrame base) {
        super(base);
    }

    /**
     * whether this is a glow item frame rather than a plain one.
     * <p>
     * A glow item frame is a separate entity in the game rather than a setting on the same one,
     * so a search for item frames has to ask for both ids. The difference shows in two places:
     * the frame's own sounds are all different, and the renderer pins the light the framed
     * item is drawn at to at least block level five whatever the surrounding light is. The
     * frame still holds an item and still has a rotation, so nothing else about it changes.
     * example:
     * <pre>
     * const ItemFrameEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.ItemFrameEntityHelper");
     * const frames = World.getEntities(32, "item_frame", "glow_item_frame");
     * if (frames !== null) {
     *   for (const entity of frames) {
     *     const frame = ItemFrameEntityHelper.class.cast(entity);
     *     if (frame.isGlowingFrame()) {
     *       Chat.log(`a glow frame at ${entity.getPos()}, lighting the wall`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the item frame is glowing, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isGlowingFrame() {
        return base instanceof GlowItemFrame;
    }

    /**
     * which of the eight ways round the item is facing, from {@code 0} to {@code 7}.
     * <p>
     * The number is a step of forty five degrees, so {@code 0} is the item as it was placed
     * and each step after that turns it a further forty five degrees all the way round. The
     * game stores the number as a remainder of eight, so this can never be outside that range
     * no matter what it was set to, and it is stored with the frame rather than being worked
     * out from where the frame is.
     * example:
     * <pre>
     * const ItemFrameEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.ItemFrameEntityHelper");
     * const frames = World.getEntities(32, "item_frame", "glow_item_frame");
     * if (frames !== null) {
     *   for (const entity of frames) {
     *     const frame = ItemFrameEntityHelper.class.cast(entity);
     *     // eight steps of forty five degrees each, so 7 is the last one round
     *     Chat.log(`turned ${frame.getRotation() * 45} degrees`);
     *   }
     * }
     * </pre>
     *
     * @return the rotation of the item inside this frame.
     * @since 1.8.4
     */
    public int getRotation() {
        return base.getRotation();
    }

    /**
     * the item inside this item frame, as a stack so the count and the damage come with it.
     * <p>
     * An empty frame is a real thing rather than a broken one, so this hands back an empty
     * stack rather than {@code null} and {@code isEmpty()} is the check to make. Whatever is in
     * the frame is what the frame is holding rather than what it was given at some point, so an
     * item taken out of a frame reads as empty from then on.
     * example:
     * <pre>
     * const ItemFrameEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.ItemFrameEntityHelper");
     * const frames = World.getEntities(32, "item_frame", "glow_item_frame");
     * if (frames !== null) {
     *   for (const entity of frames) {
     *     const frame = ItemFrameEntityHelper.class.cast(entity);
     *     const item = frame.getItem();
     *     // an empty frame is a valid frame, so check the stack rather than for null
     *     if (!item.isEmpty()) {
     *       Chat.log(`holding a ${item.getName().getString()}, turned ${frame.getRotation()} steps`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the item inside this item frame.
     * @since 1.8.4
     */
    public ItemStackHelper getItem() {
        return new ItemStackHelper(base.getItem());
    }

}
