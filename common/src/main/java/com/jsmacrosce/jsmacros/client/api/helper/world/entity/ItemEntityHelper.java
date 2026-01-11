package com.jsmacrosce.jsmacros.client.api.helper.world.entity;

import net.minecraft.world.entity.item.ItemEntity;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;

@SuppressWarnings("unused")
public class ItemEntityHelper extends EntityHelper<ItemEntity> {
    public ItemEntityHelper(ItemEntity e) {
        super(e);
    }

    public ItemStackHelper getContainedItemStack() {
        return new ItemStackHelper(base.getItem());
    }

    @Override
    public String toString() {
        return String.format("ItemEntityHelper:{\"containedStack\": %s}", getContainedItemStack().toString());
    }

}
