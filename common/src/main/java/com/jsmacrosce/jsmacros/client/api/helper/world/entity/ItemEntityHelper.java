package com.jsmacrosce.jsmacros.client.api.helper.world.entity;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.world.entity.item.ItemEntity;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;

/**
 * a dropped stack of items lying in the world, which the game calls an item entity.
 * <p>
 * An entity of this kind is one stack, not a container, so there is a single thing to
 * read off it and no inventory to walk. It is a snapshot wrapper like the other entity
 * helpers: {@code World.getEntities("item")} hands these back, and the stack a
 * particular one holds can change if a player picks it up and drops a different one in
 * the same spot.
 * example:
 * <pre>
 * // every dropped stack within ten blocks, as item entities
 * for (const entity of World.getEntities(10, "item")) {
 *   const stack = entity.asItem().getContainedItemStack();
 *   Chat.log(`${stack.getCount()} ${stack.getItemId()} at ${entity.getPos()}`);
 * }
 * </pre>
 */
@SuppressWarnings("unused")
@DocletCategory("Entity Helpers")
public class ItemEntityHelper extends EntityHelper<ItemEntity> {
    public ItemEntityHelper(ItemEntity e) {
        super(e);
    }

    /**
     * the stack this entity is holding right now, which is the whole of what an item
     * entity has. It can be an empty stack, which is what {@code isEmpty()} on the
     * result is there to answer.
     * <p>
     * A dropped stack is not picked up by reading it. Nothing on this class changes the
     * world, so a script that wants the items has to be the one that picks them up.
     * example:
     * <pre>
     * for (const entity of World.getEntities(10, "item")) {
     *   const stack = entity.asItem().getContainedItemStack();
     *   if (stack.isEmpty()) {
     *     continue;
     *   }
     *   // a count of zero and an empty stack are not the same thing
     *   Chat.log(`${stack.getCount()} of ${stack.getItemId()}, named ${stack.getName()}`);
     * }
     * </pre>
     *
     * @return the stack this item entity is holding.
     */
    public ItemStackHelper getContainedItemStack() {
        return new ItemStackHelper(base.getItem());
    }

    @Override
    public String toString() {
        return String.format("ItemEntityHelper:{\"containedStack\": %s}", getContainedItemStack().toString());
    }

}
