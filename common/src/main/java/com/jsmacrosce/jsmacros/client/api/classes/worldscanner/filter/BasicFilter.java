package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IAdvancedFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.logical.AndFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.logical.NotFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.logical.OrFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.logical.XorFilter;

/**
 * the base every combinable filter in the scanner builds on.
 * <p>
 * It supplies the four combination methods and nothing else. A subclass only has to
 * implement the test, and gets combining for free; the combination methods are what
 * {@link IAdvancedFilter} declares, and this class implements all four by wrapping the
 * receiver and the argument in one of the logical filters. Every one of them returns a
 * <i>new</i> filter and leaves the receiver alone, so a filter that has already gone into a
 * chain can still start another.
 * <p>
 * Six subclasses are reachable from the scanner builder: the two that reflect on a helper and
 * compare what a method returned, the one that stringifies a helper and compares the result
 * as text, and the three logical filters this class creates. A script reaches none of them by name
 * — see the note on {@link IAdvancedFilter} for the one script-facing route — and this base
 * class in particular is never visible to a script, since the builder never hands a filter
 * back and never exposes its type.
 * example:
 * <pre>
 * // every and and or step on the builder ends up calling one of the four
 * // methods here, on the filter the builder is already holding. This is the
 * // whole of what the base class contributes: withBlockFilter makes the
 * // first filter, andStateFilter wraps it together with the next one
 * const scanner = World.getWorldScanner()
 *   .withBlockFilter("getHardness").is(">=", 10)
 *   .andStateFilter("isToolRequired").is(false)
 *   .build();
 * const found = scanner.scanAroundPlayer(2);
 * Chat.log(`${found.size()} hard blocks that need no tool`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public abstract class BasicFilter<T> implements IAdvancedFilter<T> {

    /**
     * combines this filter and another so that a value is kept only when both accept it.
     * <p>
     * This wraps the two in an and filter and returns that. Nothing about this filter
     * changes, so the result can be combined again or negated without disturbing anything
     * this filter was already part of.
     * <p>
     * A script reaches this through the {@code and} steps on the scanner builder, which
     * pass the filter it already holds as the receiver.
     * example:
     * <pre>
     * // andBlockFilter is the builder calling this on the block filter it
     * // already holds, with the one named in the step as the argument
     * const scanner = World.getWorldScanner()
     *   .withBlockFilter("getHardness").is(">=", 10)
     *   .andBlockFilter("getSlipperiness").is(">", 0.5)
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} hard and slippery blocks`);
     * </pre>
     *
     * @param filter the filter that also has to accept a value for it to be kept
     * @return a new filter accepting a value only when both this and {@code filter} do
     * @since 1.6.5
     */
    @Override
    public IAdvancedFilter<T> and(IFilter<T> filter) {
        return new AndFilter<>(this, filter);
    }

    /**
     * combines this filter and another so that a value is kept when either one accepts it.
     * <p>
     * This wraps the two in an or filter and returns that. Nothing about this filter
     * changes, so the result can be combined again or negated without disturbing anything
     * this filter was already part of.
     * <p>
     * A script reaches this through the {@code or} steps on the scanner builder, which
     * pass the filter it already holds as the receiver.
     * example:
     * <pre>
     * // orBlockFilter is the builder calling this on the block filter it
     * // already holds, with the one named in the step as the argument
     * const scanner = World.getWorldScanner()
     *   .withBlockFilter("getHardness").is(">=", 10)
     *   .orBlockFilter("getSlipperiness").is("==", 1.0)
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} hard or slippery blocks`);
     * </pre>
     *
     * @param filter the filter that can accept a value instead of this one
     * @return a new filter accepting a value when this or {@code filter} does
     * @since 1.6.5
     */
    @Override
    public IAdvancedFilter<T> or(IFilter<T> filter) {
        return new OrFilter<>(this, filter);
    }

    /**
     * combines this filter and another so that a value is kept only when exactly one of the
     * two accepts it.
     * <p>
     * This wraps the two in an xor filter and returns that. Both of them are always
     * evaluated, since an exclusive or cannot short circuit.
     * <p>
     * <b>No script can reach this.</b> The scanner builder has {@code and}, {@code or} and
     * {@code not} steps but none for an exclusive or, so nothing a script writes calls this
     * method and the filter it would build is never created.
     * example:
     * <pre>
     * // there is no builder step for an exclusive or, so a script cannot get
     * // here. A Java caller would wrap the pair in an XorFilter directly, and
     * // unlike and and or that filter asks both of them even when the first
     * // has already decided the answer
     * Chat.log("an exclusive or is not reachable from a script");
     * </pre>
     *
     * @param filter the filter that must disagree with this one for a value to be kept
     * @return a new filter accepting a value when exactly one of the two does
     * @since 1.6.5
     */
    @Override
    public IAdvancedFilter<T> xor(IFilter<T> filter) {
        return new XorFilter<>(this, filter);
    }

    /**
     * negates this filter, so a value is kept exactly when this one rejects it.
     * <p>
     * This wraps the receiver alone in a not filter and returns that. The receiver is
     * unchanged, so negating a filter that is already in a chain leaves that chain intact.
     * <p>
     * A script reaches this through the {@code notStateFilter} and {@code notBlockFilter}
     * steps on the scanner builder, which negate the whole filter of that category built so
     * far and throw if nothing of that category exists yet.
     * example:
     * <pre>
     * // notBlockFilter negates the whole block filter, so it follows a block
     * // filter that exists. It is the last step before build here
     * const scanner = World.getWorldScanner()
     *   .withBlockFilter("getHardness").is(">=", 10)
     *   .notBlockFilter()
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} blocks softer than 10`);
     * </pre>
     *
     * @return a new filter accepting a value exactly when this one rejects it
     * @since 1.6.5
     */
    @Override
    public IAdvancedFilter<T> not() {
        return new NotFilter<>(this);
    }

}
