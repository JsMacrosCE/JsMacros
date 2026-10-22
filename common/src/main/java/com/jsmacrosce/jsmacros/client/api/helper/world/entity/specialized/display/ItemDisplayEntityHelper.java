package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display;

import net.minecraft.world.entity.Display;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;

/**
 * a display entity that shows an item, which is the {@code item_display} entity and the one to
 * use for a floating sword or a dropped gem.
 * <p>
 * Both of the things this class adds are reached from the client's render state rather than
 * from the entity data, and {@link #getTransform() getTransform()} can be {@code null} where
 * {@link #getItem() getItem()} cannot. An item display with no item set is still a valid
 * entity and {@code getItem()} hands back an empty stack rather than {@code null}.
 * example:
 * <pre>
 * const ItemDisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.ItemDisplayEntityHelper");
 * const items = World.getEntities(32, "item_display");
 * if (items !== null) {
 *   for (const entity of items) {
 *     const display = ItemDisplayEntityHelper.class.cast(entity);
 *     const item = display.getItem();
 *     if (item.isEmpty()) {
 *       continue;
 *     }
 *     Chat.log(`${item.getCount()} of ${item.getItemID()}, shown as ${display.getTransform()}`);
 *   }
 * }
 * </pre>
 *
 * @author aMelonRind
 * @since 1.9.1
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class ItemDisplayEntityHelper extends DisplayEntityHelper<Display.ItemDisplay> {

    public ItemDisplayEntityHelper(Display.ItemDisplay base) {
        super(base);
    }

    /**
     * the item the display is showing, as a stack so the count and the damage come with it.
     * <p>
     * This reads the same slot the entity data holds the item in, and it is never
     * {@code null}: an item display with nothing in it hands back an empty stack, which
     * {@code isEmpty()} reports. The count is the display's own, so a display showing a stack
     * of three shows three of them drawn together rather than one of three.
     * example:
     * <pre>
     * const ItemDisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.ItemDisplayEntityHelper");
     * const items = World.getEntities(32, "item_display");
     * if (items !== null) {
     *   for (const entity of items) {
     *     const display = ItemDisplayEntityHelper.class.cast(entity);
     *     const item = display.getItem();
     *     // isEmpty rather than a null check: the stack is always there
     *     if (!item.isEmpty()) {
     *       if (item.getDurability() > 0) {
     *         Chat.log(`a worn ${item.getName().getString()}`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the item stack being displayed, empty if the display has no item
     * @since 1.9.1
     */
    public ItemStackHelper getItem() {
        return new ItemStackHelper(base.getSlot(0).get());
    }

    /**
     * the pose the item is drawn in, which is the same idea as the item transform on a dropped
     * item or an item in a player's hand, and {@code "none"} is what a display with no
     * transform set reports.
     * <p>
     * This is the render state the client built rather than the entity data, so it is
     * {@code null} until the client has built that state once. The id is the one the game
     * writes to the entity data, so it is fixed and does not change with a rename.
     * example:
     * <pre>
     * const ItemDisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.ItemDisplayEntityHelper");
     * const items = World.getEntities(32, "item_display");
     * if (items !== null) {
     *   for (const entity of items) {
     *     const display = ItemDisplayEntityHelper.class.cast(entity);
     *     // null until the client has built the render state
     *     const transform = display.getTransform();
     *     if (transform !== null) {
     *       Chat.log(`drawn as ${transform}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return "none", "thirdperson_lefthand", "thirdperson_righthand", "firstperson_lefthand",
     *         "firstperson_righthand", "head", "gui", "ground", "fixed" or "on_shelf", or
     *         {@code null} if the client has not built the render state
     * @since 1.9.1
     */
    @SuppressWarnings("SpellCheckingInspection")
    @Nullable
    public String getTransform() {
        Display.ItemDisplay.ItemRenderState data = base.itemRenderState();
        if (data == null) return null;
        return data.itemTransform().getSerializedName();
    }

}
