package com.jsmacrosce.jsmacros.core.helpers;


import com.jsmacrosce.doclet.DocletCategory;

/**
 * Base class for helpers that wrap a raw value; it is abstract and is not a standalone script global.
 * <pre>
 * const text = Chat.createTextHelperFromString("hello");
 * const component = text.getRaw();
 * </pre>
 * @param <T> type of the wrapped value
 */
@DocletCategory("Misc Helpers")
public abstract class BaseHelper<T> {
    protected T base;

    /** Stores the supplied value without validation; {@code null} is retained as-is.
     * @param base value to wrap
     */
    public BaseHelper(T base) {
        this.base = base;
    }

    /** Returns the wrapped value, which may be {@code null}.
     * @return the stored value
     */
    public T getRaw() {
        return base;
    }

    /** Returns the wrapped value's hash code; throws {@link NullPointerException} if it is {@code null}.
     * @return the raw value's hash code
     */
    @Override
    public int hashCode() {
        return base.hashCode();
    }

    /** Compares wrapped values when {@code obj} is a {@code BaseHelper}; otherwise compares the wrapped value directly with {@code obj}.
     * This delegates to the stored value's {@code equals} and throws {@link NullPointerException} if that value is {@code null}.
     * @param obj object to compare
     * @return whether the delegated comparison reports equality
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BaseHelper) {
            return base.equals(((BaseHelper<?>) obj).base);
        }
        return base.equals(obj);
    }

}
