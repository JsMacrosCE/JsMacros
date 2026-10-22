package com.jsmacrosce;

/** Stores two mutable values in positional order: {@code T} is first and {@code U} is second.
 * <pre>
 * const Pair = Java.type("com.jsmacrosce.Pair");
 * const pair = new Pair("first", 2);
 * pair.getT(); // "first"
 * pair.getU(); // 2
 * </pre>
 * @param <T> the type of the first value
 * @param <U> the type of the second value
 */
public class Pair<T, U> {
    private T t;
    private U u;

    /** Stores the supplied first and second values, respectively.
     * @param t the first value
     * @param u the second value
     */
    public Pair(T t, U u) {
        this.t = t;
        this.u = u;
    }

    /** Replaces the second stored value.
     * @param u the new second value
     */
    public void setU(U u) {
        this.u = u;
    }

    /** Replaces the first stored value.
     * @param t the new first value
     */
    public void setT(T t) {
        this.t = t;
    }

    /** Returns the second stored value, which may be {@code null} if {@code null} was stored.
     * @return the second stored value
     */
    public U getU() {
        return u;
    }

    /** Returns the first stored value, which may be {@code null} if {@code null} was stored.
     * @return the first stored value
     */
    public T getT() {
        return t;
    }

}
