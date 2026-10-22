package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display;

import net.minecraft.world.entity.Display;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;

/**
 * Displays an item entity; its displayed stack is empty when no item is set. The transform
 * identifier may be {@code null} until its client render state is available.
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

    /** Wraps the supplied item display entity, retaining its reference.
     * @param base item display entity to wrap
     */
    public ItemDisplayEntityHelper(Display.ItemDisplay base) {
        super(base);
    }

    /**
     * Returns the displayed stack, empty when the display has no item.
     *
     * @return the item stack being displayed, empty if the display has no item
     * @since 1.9.1
     */
    public ItemStackHelper getItem() {
        return new ItemStackHelper(base.getSlot(0).get());
    }

    /**
     * Returns the item's serialized display transform identifier from the client render state.
     *
     * @return the transform identifier, or {@code null} until the client builds the render state
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
