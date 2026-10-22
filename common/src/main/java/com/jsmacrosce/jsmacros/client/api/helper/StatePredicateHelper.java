package com.jsmacrosce.jsmacros.client.api.helper;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.FluidStateHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import net.minecraft.advancements.critereon.StatePropertiesPredicate;

/**
 * Tests block or fluid states against named property-value constraints. A property absent from
 * the condition is ignored; every named property must exist on the tested state's type and
 * satisfy its matcher. {@link BlockPredicateHelper#getStatePredicate()} may return {@code null}
 * when its condition has no state predicate.
 * <pre>
 * const statePredicate = condition.getStatePredicate();
 * if (statePredicate !== null) {
 *   Chat.log(statePredicate.test(block.getBlockStateHelper()));
 * }
 * </pre>
 *
 * @since 1.9.1
 */
@DocletCategory("Misc Helpers")
public class StatePredicateHelper extends BaseHelper<StatePropertiesPredicate> {

    /** Wraps the supplied state-properties predicate.
     * @param base raw state-properties predicate to wrap
     */
    public StatePredicateHelper(StatePropertiesPredicate base) {
        super(base);
    }

    /**
     * Tests whether the block state's named properties satisfy this condition; the state is not
     * read from the world.
     *
     * @param state the block state to test
     * @return {@code true} when every named property matches
     * @since 1.9.1
     */
    public boolean test(BlockStateHelper state) {
        return base.matches(state.getRaw());
    }

    /**
     * Tests whether the fluid state's named properties satisfy this condition; the state is not
     * read from the world.
     *
     * @param state the fluid state to test
     * @return {@code true} when every named property matches
     * @since 1.9.1
     */
    public boolean test(FluidStateHelper state) {
        return base.matches(state.getRaw());
    }

}
