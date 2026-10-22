package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter;

import com.jsmacrosce.doclet.DocletCategory;
import com.google.common.collect.ImmutableList;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.compare.NumberCompareFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * a filter that holds a list of other filters and combines all of them at once, rather than
 * being built up pairwise.
 * <p>
 * The pairwise filters, which are the and, or and xor ones, wrap exactly two, so a chain of
 * them ends up as a nested tree that is evaluated from the inside out. This one instead keeps
 * a list that can be added to and removed from after construction, and each subclass decides
 * what "combining the list" means: every one of them, any one of them, none of them, or a
 * count of them compared against a number. The list is mutable and the group is not, in the
 * sense that adding a filter changes what every later test sees rather than producing a new
 * group.
 * <br>
 * <b>This class and all four of its subclasses are currently dead.</b> Nothing in the mod
 * constructs a group filter, the scanner builder only ever combines two filters at a time
 * rather than into a list, and the scanner takes one function per category rather than a list
 * of them, so there is no route to one — from a script or otherwise. The note on
 * {@link IFilter} covers why none of this package is in the shipped TypeScript definitions;
 * this goes further, in that these classes are not reached even from Java. They are
 * documented because they are public and because the four subclass behaviours below are the
 * clearest statement of what the pairwise logical filters are choosing between.
 * example:
 * <pre>
 * // nothing a script writes reaches a group filter. The builder only ever
 * // wraps two filters at a time, and it hands the finished filter straight
 * // to the scanner, so there is no point at which a list of filters exists
 * // that a script could build or inspect
 * Chat.log("group filters are not built by the scanner builder");
 * </pre>
 *
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public abstract class GroupFilter<T> implements IFilter<T> {

    /**
     * the filters this group tests, in the order they were added.
     */
    protected List<IFilter<T>> filters;

    protected GroupFilter() {
        this.filters = new ArrayList<>();
    }

    /**
     * adds one filter to the end of the list.
     * <p>
     * The group is modified in place and returned, so calls chain. The same filter may be
     * added more than once, and it is then tested more than once, which matters for the
     * count based subclass.
     *
     * @param filter the filter to add
     * @return this group, for chaining
     * @since 1.6.5
     */
    public GroupFilter<T> add(IFilter<T> filter) {
        this.filters.add(filter);
        return this;
    }

    /**
     * adds every filter of a list to the end of this list, keeping their order.
     * <p>
     * The group is modified in place and returned, so calls chain. The list is copied entry
     * by entry rather than the list itself being adopted, so later changes to the argument do
     * not reach this group.
     *
     * @param filters the filters to add, in order
     * @return this group, for chaining
     * @throws NullPointerException if {@code filters} is {@code null}
     * @since 1.6.5
     */
    public GroupFilter<T> add(List<IFilter<T>> filters) {
        this.filters.addAll(filters);
        return this;
    }

    /**
     * removes one filter from the list.
     * <p>
     * Only the first entry equal to the argument is removed, and equality here is the default
     * one, since none of the filters in this package override it. A filter that is not the
     * very instance that was added is therefore not removed, and a filter added twice needs
     * two calls to go.
     *
     * @param filter the filter to remove
     * @return this group, for chaining
     * @since 1.6.5
     */
    public GroupFilter<T> remove(IFilter<T> filter) {
        this.filters.remove(filter);
        return this;
    }

    /**
     * removes every filter of a list from this list.
     * <p>
     * Like removing a single one, this matches on the default equality, so only the exact
     * instances that were added are removed. Unlike the single filter version it does not
     * stop at the first match: every occurrence of each one is removed, so a filter that was
     * added twice goes in a single call.
     *
     * @param filters the filters to remove
     * @return this group, for chaining
     * @throws NullPointerException if {@code filters} is {@code null}
     * @since 1.6.5
     */
    public GroupFilter<T> remove(List<IFilter<T>> filters) {
        this.filters.removeAll(filters);
        return this;
    }

    /**
     * a snapshot of the filters currently held.
     * <p>
     * The returned list is immutable, so it cannot be used to change the group; adding and
     * removing goes through the methods here. It is a copy taken at the moment of the call,
     * so it does not track later changes.
     *
     * @return an immutable copy of the current list
     * @since 1.6.5
     */
    public List<IFilter<T>> getFilters() {
        return ImmutableList.copyOf(filters);
    }

    /**
     * a group that keeps a value only when every filter in the list accepts it.
     * <p>
     * The list is tested in order and testing stops at the first rejection, so the filters
     * after a rejecting one are never asked. That is the same short circuiting the pairwise
     * and filter does, generalised to any number of filters, and it differs from the pairwise
     * filters in that the list can grow after the group is built.
     * <p>
     * A group with nothing in it accepts everything, which is the identity for an and: there
     * is no filter left to disagree. That falls out of the stream this is written with rather
     * than being checked for.
     *
     * @since 1.6.5
     */
    @DocletCategory("Filters/Predicates")
    public static class AllMatchFilter<T> extends GroupFilter<T> {

        public AllMatchFilter() {
            super();
        }

        /**
         * tests the value against every filter in the list, stopping at the first rejection.
         *
         * @param t the value to test
         * @return whether every filter in the list accepted it
         * @since 1.6.5
         */
        @Override
        public Boolean apply(T t) {
            return filters.stream().allMatch(filter -> filter.apply(t));
        }

    }

    /**
     * a group that keeps a value when at least one filter in the list accepts it.
     * <p>
     * The list is tested in order and testing stops at the first acceptance, so the filters
     * after an accepting one are never asked. That is the same short circuiting the pairwise
     * or filter does, generalised to any number of filters.
     * <p>
     * A group with nothing in it accepts nothing, which is the identity for an or: with no
     * filter to say yes, there is no acceptance. That falls out of the stream this is written
     * with rather than being checked for.
     *
     * @since 1.6.5
     */
    @DocletCategory("Filters/Predicates")
    public static class AnyMatchFilter<T> extends GroupFilter<T> {

        public AnyMatchFilter() {
            super();
        }

        /**
         * tests the value against the filters in the list, stopping at the first acceptance.
         *
         * @param t the value to test
         * @return whether at least one filter in the list accepted it
         * @since 1.6.5
         */
        @Override
        public Boolean apply(T t) {
            return filters.stream().anyMatch(filter -> filter.apply(t));
        }

    }

    /**
     * a group that keeps a value only when every filter in the list rejects it.
     * <p>
     * The list is tested in order and testing stops at the first acceptance, since that
     * already settles the answer against keeping the value. This is the negation of the any
     * match group, and unlike the not filter it is not a separate filter wrapped around
     * another: the rejection of every filter is what the group tests directly.
     * <p>
     * A group with nothing in it accepts everything, which is the identity for a none: there
     * is no filter left to accept the value. That falls out of the stream this is written
     * with rather than being checked for.
     *
     * @since 1.6.5
     */
    @DocletCategory("Filters/Predicates")
    public static class NoneMatchFilter<T> extends GroupFilter<T> {

        public NoneMatchFilter() {
            super();
        }

        /**
         * tests the value against the filters in the list, stopping at the first acceptance.
         *
         * @param t the value to test
         * @return whether no filter in the list accepted it
         * @since 1.6.5
         */
        @Override
        public Boolean apply(T t) {
            return filters.stream().noneMatch(filter -> filter.apply(t));
        }

    }

    /**
     * a group that keeps a value based on how many of its filters accepted it.
     * <p>
     * Every filter in the list is asked, and the number that accepted is compared against a
     * fixed number by the same numeric comparison a numeric builder step uses. Because every
     * filter is asked, this is the one group that does not short circuit, which is what makes
     * counting possible at all. The count is a whole number and the comparison is made
     * exactly, so there is no tolerance and no fractional threshold: a count of 2 matches
     * only a target of 2.
     * <p>
     * An empty group counts zero, so whether it accepts a value is decided entirely by the
     * comparison of that zero against the fixed number, and the operator alone decides it:
     * an equality test accepts only against a target of zero, an inequality test accepts
     * against any target but zero, a greater test accepts only against a negative target, a
     * greater-or-equal test accepts against zero or below, a less-than test accepts only
     * against a positive target, and a less-than-or-equal test accepts against zero or
     * above. For a target of one or more that leaves an empty group accepting a value
     * everywhere except under an equality or a greater test.
     *
     * @since 1.6.5
     */
    @DocletCategory("Filters/Predicates")
    public static class CountMatchFilter<T> extends GroupFilter<T> {

        private final IFilter<Number> filter;

        /**
         * builds a group that compares the number of accepting filters against a count.
         * <p>
         * The operation is stored rather than checked here, and the count is a
         * {@code long}, so the comparison that is built from the two is the exact one. An
         * operator outside the six is not refused when the group is built: it is refused by
         * the first comparison, which runs when a value is tested.
         *
         * @param operation the comparison to run on the count, one of {@code ">="},
         *                  {@code ">"}, {@code "<="}, {@code "<"}, {@code "=="} or
         *                  {@code "!="}
         * @param compareTo the count to compare the number of accepting filters against
         * @since 1.6.5
         */
        public CountMatchFilter(String operation, long compareTo) {
            super();
            filter = new NumberCompareFilter(operation, compareTo);
        }

        /**
         * asks every filter in the list and compares how many accepted.
         * <p>
         * No filter is skipped, whatever the others answered, because the count has to be
         * complete before it can be compared.
         *
         * @param t the value to test
         * @return whether the number of filters that accepted it satisfies the comparison
         * @since 1.6.5
         */
        @Override
        public Boolean apply(T t) {
            return filter.apply(filters.stream().filter(filter -> filter.apply(t)).count());
        }

    }

}
