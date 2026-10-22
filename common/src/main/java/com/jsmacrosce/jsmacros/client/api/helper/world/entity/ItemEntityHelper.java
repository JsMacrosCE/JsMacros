package com.jsmacrosce.jsmacros.client.api.helper.world.entity;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.world.entity.item.ItemEntity;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;

/**
 * A dropped stack of items in the world.
 * <pre>
 * // Log dropped stacks within ten blocks.
 * for (const entity of World.getEntities(10, "item")) {
 *   const stack = entity.asItem().getContainedItemStack();
 *   Chat.log(`${stack.getCount()} ${stack.getItemId()} at ${entity.getPos()}`);
 * }
 * </pre>
 */
@SuppressWarnings("unused")
@DocletCategory("Entity Helpers")
public class ItemEntityHelper extends EntityHelper<ItemEntity> {
    /**
     * Wraps the supplied item entity.
     * @param e raw item entity to wrap
     */
    public ItemEntityHelper(ItemEntity e) {
        super(e);
    }

    /**
     * Returns the stack currently represented by this entity. The result may be empty;
     * reading it does not pick up the items.
     *
     * @return the entity's current stack, possibly empty
     */
    public ItemStackHelper getContainedItemStack() {
        return new ItemStackHelper(base.getItem());
    }

    /**
     * Returns a diagnostic representation of this item entity and its contained stack.
     * @return diagnostic representation of the item entity and its contained stack
     */
    @Override
    public String toString() {
        return String.format("ItemEntityHelper:{\"containedStack\": %s}", getContainedItemStack().toString());
    }

}
