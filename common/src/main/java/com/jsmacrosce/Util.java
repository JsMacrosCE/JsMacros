package com.jsmacrosce;

/** Provides numeric-value conversion for reflective calls and proxy argument or return values.
 * <pre>
 * const Util = Java.type("com.jsmacrosce.Util");
 * const Integer = Java.type("java.lang.Integer");
 * const Double = Java.type("java.lang.Double");
 * Chat.log(Util.tryAutoCastNumber(Integer.TYPE, Double.valueOf(3.75)));
 * </pre>
 */
public class Util {

    /** Converts a {@link Number} to the wrapper for a supported primitive or boxed target.
     * Supports {@code int}, {@code float}, {@code double}, {@code short}, {@code long},
     * {@code char}, and {@code byte} targets; matching wrapper values are returned unchanged.
     * Numeric conversions use the corresponding {@link Number} conversion method, so narrowing
     * conversions may discard or truncate value. For {@code char}, the value is converted through
     * {@link Number#intValue()} and cast to {@code char}. Unsupported targets, including
     * {@code null}, return {@code number} unchanged; {@code Boolean} is not converted. For a
     * supported target, {@code null} input or an input that is neither the matching wrapper nor a
     * {@link Number} fails during conversion.
     *
     * @param returnType primitive or boxed target class
     * @param number value to convert
     * @return the converted wrapper, or the original value when no conversion applies
     */
    public static Object tryAutoCastNumber(Class<?> returnType, Object number) {
        if ((returnType == int.class || returnType == Integer.class) && !(number instanceof Integer)) {
            number = ((Number) number).intValue();
        } else if ((returnType == float.class || returnType == Float.class) && !(number instanceof Float)) {
            number = ((Number) number).floatValue();
        } else if ((returnType == double.class || returnType == Double.class) && !(number instanceof Double)) {
            number = ((Number) number).doubleValue();
        } else if ((returnType == short.class || returnType == Short.class) && !(number instanceof Short)) {
            number = ((Number) number).shortValue();
        } else if ((returnType == long.class || returnType == Long.class) && !(number instanceof Long)) {
            number = ((Number) number).longValue();
        } else if ((returnType == char.class || returnType == Character.class) && !(number instanceof Character)) {
            number = (char) ((Number) number).intValue();
        } else if ((returnType == byte.class || returnType == Byte.class) && !(number instanceof Byte)) {
            number = ((Number) number).byteValue();
        }
        return number;
    }

}
