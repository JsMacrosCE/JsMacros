package com.jsmacrosce.jsmacros.client.api.helper;

import net.minecraft.advancements.critereon.NbtPredicate;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * @since 1.9.1
 */
public class NbtPredicateHelper extends BaseHelper<NbtPredicate> {

    public NbtPredicateHelper(NbtPredicate base) {
        super(base);
    }

    /**
     * @since 1.9.1
     */
    public boolean test(EntityHelper<?> entity) {
        return base.matches(entity.getRaw());
    }

    /**
     * @since 1.9.1
     */
    public boolean test(ItemStackHelper itemStack) {
        return base.matches(itemStack.getRaw());
    }

    /**
     * @since 1.9.1
     */
    public boolean test(NBTElementHelper<?> nbtElement) {
        return base.matches(nbtElement.getRaw());
    }

}
