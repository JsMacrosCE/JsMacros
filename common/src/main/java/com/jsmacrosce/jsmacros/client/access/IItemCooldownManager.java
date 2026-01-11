package com.jsmacrosce.jsmacros.client.access;

import net.minecraft.world.item.Item;

import java.util.Map;

public interface IItemCooldownManager {

    Map<Item, IItemCooldownEntry> jsmacros_getCooldownItems();

    int jsmacros_getManagerTicks();

}
