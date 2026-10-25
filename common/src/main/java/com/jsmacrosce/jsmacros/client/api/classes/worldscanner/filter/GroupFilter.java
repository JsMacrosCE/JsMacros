package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
import com.google.common.collect.ImmutableList;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.compare.NumberCompareFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds a mutable list of filters and combines their results according to the concrete
 * subclass. Adding or removing entries changes subsequent evaluations of the same group.
 * <p>
 * These public Java filter implementations are not constructed by the world-scanner builder,
 * which composes filters pairwise instead.
 *
 * @param <T> the value type tested by the group's filters
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
@DocletIgnore
public abstract class GroupFilter<T> implements IFilter<T> {

    /**
     * Filters tested by this group, in insertion order.
     */
    protected List<IFilter<T>> filters;

    protected GroupFilter() {
        this.filters = new ArrayList<>();
    }

    /**
     * Appends a filter; repeated entries are tested repeatedly.
     *
     * @param filter the filter to add
     * @return this group
     * @since 1.6.5
     */
    public GroupFilter<T> add(IFilter<T> filter) {
        this.filters.add(filter);
        return this;
    }

    /**
     * Appends the entries of a list in iteration order.
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
     * Removes the first entry equal to the argument, if present.
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
     * Removes all entries equal to any entry in the supplied list.
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
     * Returns an immutable snapshot of the current entries; later group changes are not
     * reflected in it.
     *
     * @return an immutable copy of the current list
     * @since 1.6.5
     */
    public List<IFilter<T>> getFilters() {
        return ImmutableList.copyOf(filters);
    }

    /**
     * Accepts a value only if every entry accepts it, stopping at the first rejection. An
     * empty group accepts every value.
     *
     * @param <T> the value type tested by the group's filters
     * @since 1.6.5
     */
    @DocletCategory("Filters/Predicates")
    public static class AllMatchFilter<T> extends GroupFilter<T> {

        /**
         * Initializes an empty group, which accepts every value until filters are added.
         */
        public AllMatchFilter() {
            super();
        }

        /**
         * Tests entries in order, stopping at the first rejection.
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
     * Accepts a value if at least one entry accepts it, stopping at the first acceptance. An
     * empty group rejects every value.
     *
     * @param <T> the value type tested by the group's filters
     * @since 1.6.5
     */
    @DocletCategory("Filters/Predicates")
    public static class AnyMatchFilter<T> extends GroupFilter<T> {

        /**
         * Initializes an empty group, which rejects every value until filters are added.
         */
        public AnyMatchFilter() {
            super();
        }

        /**
         * Tests entries in order, stopping at the first acceptance.
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
     * Accepts a value if no entry accepts it, stopping at the first acceptance. An empty
     * group accepts every value.
     *
     * @param <T> the value type tested by the group's filters
     * @since 1.6.5
     */
    @DocletCategory("Filters/Predicates")
    public static class NoneMatchFilter<T> extends GroupFilter<T> {

        /**
         * Initializes an empty group, which accepts every value until filters are added.
         */
        public NoneMatchFilter() {
            super();
        }

        /**
         * Tests entries in order, stopping at the first acceptance.
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
     * Accepts a value according to a numeric comparison of the number of entries that accept
     * it. Every entry is tested; an empty group therefore compares a count of zero.
     *
     * @param <T> the value type tested by the group's filters
     * @since 1.6.5
     */
    @DocletCategory("Filters/Predicates")
    public static class CountMatchFilter<T> extends GroupFilter<T> {

        private final IFilter<Number> filter;

        /**
         * Creates a group comparing the number of accepting entries with {@code compareTo}.
         * The operation is validated when a value is tested, not by this constructor.
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
         * Tests every entry and compares the count that accepted.
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
