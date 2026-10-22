package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.compare;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.ICompare;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;

/**
 * a filter that keeps a string when it satisfies one of five named comparisons against a
 * fixed text.
 * <p>
 * The five comparisons are the ones a scanner builder step can reach, and each is named by an
 * upper case constant rather than a symbol: {@code EQUALS}, {@code CONTAINS},
 * {@code STARTS_WITH}, {@code ENDS_WITH} and {@code MATCHES}. The name is looked up when the
 * filter is built, so it has to be spelled exactly as the constant — lower case or a mixed
 * case spelling is refused there and then, before a scan starts.
 * <p>
 * None of the five adds any behaviour of its own. Each delegates straight to the same named
 * method on {@link String}, which means each carries that method's behaviour exactly, and in
 * one case that is worth being explicit about: {@code MATCHES} is a whole string match, not
 * a search. A pattern has to account for the entire text or it will not match, so a pattern
 * that names part of a value has to allow for whatever surrounds it. The other four are the
 * substring and equality tests a name suggests they are.
 * <p>
 * A script reaches this by naming a method that returns a string in a scanner builder step.
 * A string method takes two arguments, the name of the comparison and the text, which is why
 * the names are written out rather than being symbols.
 * <br>
 * <b>This class is not in the shipped TypeScript definitions and a script never names it.</b>
 * The builder constructs it from the return type of the method named in the step, and the
 * same five comparisons are what a string filter step on the builder reaches — the two
 * differ in what they apply the comparison to, not in how the comparison behaves.
 * example:
 * <pre>
 * // getId is a declared public method of the block helper taking no
 * // parameters and returning a string, so the step takes the name of a
 * // comparison and the text. ENDS_WITH reads the id, not the stringified
 * // helper, which is what makes a partial match possible at all
 * const scanner = World.getWorldScanner()
 *   .withBlockFilter("getId").is("ENDS_WITH", "ore")
 *   .build();
 * const found = scanner.scanAroundPlayer(2);
 * Chat.log(`${found.size()} blocks whose id ends in ore`);
 *
 * // EQUALS is a whole string equality, so the text has to be the entire id
 * const exact = World.getWorldScanner()
 *   .withBlockFilter("getId").is("EQUALS", "minecraft:iron_ore")
 *   .build();
 * Chat.log(`${exact.scanAroundPlayer(2).size()} iron ore blocks`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public class StringCompareFilter implements IFilter<String> {

    private final String compareTo;

    protected final ICompare<String> filter;

    /**
     * fixes the text candidates are compared against, along with the comparison to use.
     * <p>
     * The comparison name is resolved to one of the five here rather than per test, and a
     * name that is not one of them is refused here.
     *
     * @param operation the comparison to run, spelled exactly as its constant:
     *                  {@code EQUALS}, {@code CONTAINS}, {@code STARTS_WITH},
     *                  {@code ENDS_WITH} or {@code MATCHES}
     * @param compareTo the text candidates are compared against
     * @throws IllegalArgumentException if {@code operation} is not one of those five, which
     *         includes the lower case and mixed case spellings of them
     * @since 1.6.5
     */
    public StringCompareFilter(String operation, String compareTo) {
        this.compareTo = compareTo;
        filter = FilterMethod.valueOf(operation).getMethod();
    }

    /**
     * tests a string against the fixed text.
     * <p>
     * This is the chosen comparison and nothing else, so what it does with a value that
     * differs in length, case, or everything at once is whatever the named method on
     * {@link String} does with it.
     * <p>
     * A script does not call this. It is what a builder step naming a method with a string
     * return type ends up testing each value with.
     * example:
     * <pre>
     * // getPistonBehaviour is a declared public method of the state helper
     * // returning a string, so the step takes a comparison name and the text
     * const scanner = World.getWorldScanner()
     *   .withStateFilter("getPistonBehaviour").is("EQUALS", "DESTROY")
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} states that destroy on push`);
     * </pre>
     *
     * @param val the string to test
     * @return whether the comparison holds for {@code val}
     * @since 1.6.5
     */
    @Override
    public Boolean apply(String val) {
        return filter.compare(val, compareTo);
    }

    /**
     * the five string comparisons, each holding the method that implements it.
     * <p>
     * A constant per operation rather than a switch, so adding an operation is a constant
     * and a method reference and nothing else. The names are the ones a builder step takes,
     * which is why they are upper case and spelled out rather than being symbols.
     * <p>
     * {@code MATCHES} deserves the note again here, because it is the one that surprises: it
     * is {@link String#matches(String)}, which is a whole string match, so a pattern that
     * names only part of a value does not match it.
     *
     * @since 1.6.5
     */
    @DocletCategory("Filters/Predicates")
    public enum FilterMethod {
        /**
         * keeps a value containing the fixed text anywhere within it.
         */
        CONTAINS(String::contains),
        /**
         * keeps a value equal to the fixed text, in full and case sensitively.
         */
        EQUALS(String::equals),
        /**
         * keeps a value beginning with the fixed text.
         */
        STARTS_WITH(String::startsWith),
        /**
         * keeps a value ending with the fixed text.
         */
        ENDS_WITH(String::endsWith),
        /**
         * keeps a value the fixed pattern matches in full, rather than a value it matches
         * somewhere within.
         */
        MATCHES(String::matches);

        private final ICompare<String> method;

        FilterMethod(ICompare<String> method) {
            this.method = method;
        }

        /**
         * the comparison this constant stands for.
         * <p>
         * A script does not call this. The string filters call it once each, when they are
         * built, to pick the comparison they will use for every candidate.
         * example:
         * <pre>
         * // each builder step picks one of these by name and the filter
         * // built from it then uses that comparison on every value. This is
         * // the whole of what a builder step choosing between the five buys
         * const scanner = World.getWorldScanner()
         *   .withBlockFilter("getId").is("STARTS_WITH", "minecraft:")
         *   .build();
         * Chat.log(`${scanner.scanAroundPlayer(2).size()} namespaced blocks`);
         * </pre>
         *
         * @return the comparison this constant names
         * @since 1.6.5
         */
        public ICompare<String> getMethod() {
            return method;
        }
    }

}
