package com.jsmacrosce.jsmacros.client.access;

import com.jsmacrosce.doclet.DocletIgnore;
import net.minecraft.world.item.Item;

import java.util.Map;

@DocletIgnore
public interface IItemCooldownManager {

    Map<Item, IItemCooldownEntry> jsmacros_getCooldownItems();

    int jsmacros_getManagerTicks();

}
