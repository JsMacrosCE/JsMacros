package com.jsmacrosce.jsmacros.client.api.helper;

import com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.FluidStateHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

//? if >=1.21.11 {
/*import net.minecraft.advancements.criterion.StatePropertiesPredicate;
*///? } else {
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
//? }

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
