package com.jsmacrosce.jsmacros.client.api.helper;

import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.FluidStateHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * @since 1.9.1
 */
public class StatePredicateHelper extends BaseHelper<StatePropertiesPredicate> {

    public StatePredicateHelper(StatePropertiesPredicate base) {
        super(base);
    }

    /**
     * @since 1.9.1
     */
    public boolean test(BlockStateHelper state) {
        return base.matches(state.getRaw());
    }

    /**
     * @since 1.9.1
     */
    public boolean test(FluidStateHelper state) {
        return base.matches(state.getRaw());
    }

}
