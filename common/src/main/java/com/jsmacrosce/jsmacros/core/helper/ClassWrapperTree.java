package com.jsmacrosce.jsmacros.core.helper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Registry that dispatches values to wrappers according to their runtime class.
 * Core's helper registry uses this tree to select helpers; scripts can also request
 * wrapping through {@code JavaUtils.getHelperFromRaw(raw)}.
 * <pre>
 * const raw = Chat.createTextHelperFromString("hello").getRaw();
 * const helper = JavaUtils.getHelperFromRaw(raw);
 * if (helper !== null) print(helper);
 * </pre>
 * @param <K> the input type accepted by registered wrappers
 * @param <V> the wrapper result type
 */
public class ClassWrapperTree<K, V> {
    final List<ClassWrapperTree<? super K, ? super V>> subTypes = new ArrayList<>();
    final Class<K> type;
    Function<K, V> wrapper;

    /** Initializes the fallback wrapper used when no registered child type matches.
     * @param baseType type handled by this tree's fallback wrapper
     * @param baseTypeWrapper fallback wrapper function
     */
    public ClassWrapperTree(Class<K> baseType, Function<K, V> baseTypeWrapper) {
        type = baseType;
        wrapper = baseTypeWrapper;
    }

    /** Registers or replaces a wrapper and places assignable registered types beneath it.
     * @param <T> registered input type
     * @param <U> result type produced by the wrapper
     * @param type input class to register
     * @param wrapper function applied to matching values
     */
    public <T extends K, U extends V> void registerType(Class<T> type, Function<T, U> wrapper) {
        if (type.equals(this.type)) {
            this.wrapper = (Function) wrapper;
            return;
        }
        for (ClassWrapperTree<? super K, ? super V> wrap : subTypes) {
            if (wrap.type.equals(type)) {
                wrap.wrapper = (Function) wrapper;
                return;
            }
            if (wrap.type.isAssignableFrom(type)) {
                wrap.registerType(type, wrapper);
                return;
            }
        }
        ClassWrapperTree<T, U> newWrapper = new ClassWrapperTree<>(type, wrapper);
        Iterator<ClassWrapperTree<? super K, ? super V>> iter = subTypes.iterator();
        while (iter.hasNext()) {
            ClassWrapperTree<? super K, ? super V> wrap = iter.next();
            if (type.isAssignableFrom(wrap.type)) {
                newWrapper.subTypes.add(wrap);
                iter.remove();
            }
        }
        this.subTypes.add((ClassWrapperTree) newWrapper);
    }

    /** Applies the first matching registered wrapper to the input's runtime class.
     * @param <T> input type
     * @param inputType value to wrap; must be non-null
     * @return the selected wrapper's result, which may be {@code null}
     */
    public <T extends K> V wrap(T inputType) {
        Class<T> inputClass = (Class) inputType.getClass();
        return getSubtypeWrapper(inputClass).wrapper.apply(inputType);
    }

    /** Searches the first assignable child recursively, or returns this node if none matches.
     * The fallback also applies when {@code type} is unrelated to this node's registered type.
     * @param <T> queried type
     * @param <U> wrapper result type
     * @param type class to look up
     * @return the first matching descendant, or this node as the fallback
     */
    public <T extends K, U extends V> ClassWrapperTree<T, U> getSubtypeWrapper(Class<T> type) {
        for (ClassWrapperTree<? super K, ? super V> wrap : subTypes) {
            if (wrap.type.isAssignableFrom(type)) {
                return wrap.getSubtypeWrapper(type);
            }
        }
        return (ClassWrapperTree<T, U>) this;
    }

}
