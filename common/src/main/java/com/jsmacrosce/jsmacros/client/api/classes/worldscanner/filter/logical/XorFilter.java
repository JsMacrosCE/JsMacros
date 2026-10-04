package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.logical;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.BasicFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;

/**
 * a filter that keeps a value only when exactly one of two filters accepts it.
 * <p>
 * It holds exactly two filters, fixed when it is built, and never changes them afterwards,
 * which is the same shape as the and and or filters. What differs is how the two are asked:
 * an exclusive or cannot be evaluated with a short circuit, because knowing that the first
 * accepted does not tell you the answer until you also know about the second. Both filters
 * are therefore always asked, in order, and the result is true only when they disagree. A
 * filter that is expensive or that would fail on some values is reached more often here than
 * under the and or or filters, since it is never skipped.
 * <p>
 * The results are the boxed values the two filters return, unboxed to be combined, so a
 * filter that answers {@code null} rather than yes or no causes a failure here.
 * <br>
 * <b>This class is dead, and no script can reach it.</b> The scanner builder offers
 * {@code and}, {@code or} and {@code not} steps but no step for an exclusive or, so nothing
 * in the mod calls the xor method that would build this filter. No step of the builder sets
 * the operation an exclusive or would need, so the branch of it that combines two filters
 * that way is never taken.
 * <br>
 * <b>This class is also not in the shipped TypeScript definitions.</b> There is nothing for a
 * script to name in the first place, let alone build.
 * example:
 * <pre>
 * // no builder step produces this. The builder has and, or and not steps
 * // and nothing for an exclusive or, so a chain cannot reach it. A Java
 * // caller would write new XorFilter(first, second) directly. Both filters
 * // are asked every time, which is the one behaviour that separates it
 * // from the or filter
 * Chat.log("an exclusive or is not reachable from a script");
 * </pre>
 *
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public class XorFilter<T> extends BasicFilter<T> {

    private final IFilter<T> filterOne;
    private final IFilter<T> filterTwo;

    /**
     * fixes the two filters to be combined.
     * <p>
     * The order affects only which is asked first, not the answer, since both are always
     * asked.
     *
     * @param filterOne the filter asked first
     * @param filterTwo the filter asked second
     * @since 1.6.5
     */
    public XorFilter(IFilter<T> filterOne, IFilter<T> filterTwo) {
        this.filterOne = filterOne;
        this.filterTwo = filterTwo;
    }

    /**
     * tests a value against both filters, whatever the first one answered.
     * <p>
     * Both are always asked, which is what makes this an exclusive or rather than the short
     * circuiting and or or filters.
     * <p>
     * A script does not call this, and no builder step produces the filter it belongs to.
     * example:
     * <pre>
     * // no builder step produces this filter, so there is no chain to show
     * // here. A Java caller would write new XorFilter(first, second) and
     * // both would be asked on every value. The closest a script gets is an
     * // or step, which skips its second filter once the first has said yes
     * const either = World.getWorldScanner()
     *   .withBlockFilter("getHardness").is(">=", 10)
     *   .orBlockFilter("getSlipperiness").is("==", 1.0)
     *   .build();
     * Chat.log(`${either.scanAroundPlayer(2).size()} hard or fully slippery blocks`);
     * </pre>
     *
     * @param obj the value to test
     * @return whether exactly one of the two filters accepted it
     * @throws NullPointerException if either filter answers {@code null}, since the two
     *         boxed results are unboxed to be combined
     * @since 1.6.5
     */
    @Override
    public Boolean apply(T obj) {
        return filterOne.apply(obj) ^ filterTwo.apply(obj);
    }

    /**
     * the filter asked first.
     * <p>
     * Since no builder step builds this filter, a script has no way to reach this getter
     * either. It exists for a Java caller holding the filter directly.
     * example:
     * <pre>
     * // there is no builder step that builds this filter, so there is no
     * // chain a script writes that ends up here
     * Chat.log("an exclusive or is not reachable from a script");
     * </pre>
     *
     * @return the first of the two filters
     * @since 1.6.5
     */
    public IFilter<T> getFilterOne() {
        return filterOne;
    }

    /**
     * the filter asked second, which is always asked rather than being skipped.
     * <p>
     * Since no builder step builds this filter, a script has no way to reach this getter
     * either. It exists for a Java caller holding the filter directly.
     * example:
     * <pre>
     * // as with the first one, nothing a script writes builds the filter
     * // this would read
     * Chat.log("an exclusive or is not reachable from a script");
     * </pre>
     *
     * @return the second of the two filters
     * @since 1.6.5
     */
    public IFilter<T> getFilterTwo() {
        return filterTwo;
    }

}
