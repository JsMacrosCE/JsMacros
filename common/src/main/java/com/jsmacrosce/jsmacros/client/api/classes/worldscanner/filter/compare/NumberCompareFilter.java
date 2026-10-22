package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.compare;

import com.google.common.math.DoubleMath;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;

import java.util.Locale;

/**
 * Compares numeric candidates with a fixed target using {@code >=}, {@code >}, {@code <=},
 * {@code <}, {@code ==} or {@code !=}. The target's runtime class selects the comparison
 * width: byte, short, int and long comparisons narrow both operands to that width, while float
 * and double comparisons use those respective conversions. Floating-point equality uses a
 * tolerance of {@code 0.000001}; ordering comparisons are exact. An unsupported {@link Number}
 * subclass is accepted at construction but causes an {@link IllegalArgumentException} when
 * tested. An unsupported operator likewise causes that exception when a candidate is tested.
 *
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public class NumberCompareFilter implements IFilter<Number> {

    private final static double EPSILON = 0.000001d;

    private final String operation;
    private final Number compareTo;
    private final String numberType;

    /**
     * Sets the operator and comparison target. The target's runtime class selects the
     * comparison width; the operator is not validated until a candidate is tested.
     *
     * @param operation the comparison to run, one of {@code ">="}, {@code ">"},
     *                  {@code "<="}, {@code "<"}, {@code "=="} or {@code "!="}
     * @param compareTo the number candidates are compared against, whose class decides the
     *                  width of the comparison
     * @throws ClassCastException if {@code compareTo} is not a {@link Number}
     * @throws NullPointerException if {@code compareTo} is {@code null}, since its class is
     *         read to decide the comparison
     * @since 1.6.5
     */
    public NumberCompareFilter(String operation, Object compareTo) {
        this.operation = operation;
        this.compareTo = (Number) compareTo;
        numberType = getNumberType(compareTo);
    }

    /**
     * Returns the supported comparison width for the target's runtime class.
     *
     * @param compareTo the number to classify
     * @return the name of the width, one of {@code int}, {@code long}, {@code byte},
     *         {@code float}, {@code double} or {@code short}, or an empty string for a
     *         number of another class
     * @throws NullPointerException if {@code compareTo} is {@code null}
     * @since 1.6.5
     */
    private String getNumberType(Object compareTo) {
        Class<?> clazz = compareTo.getClass();
        if (clazz == int.class || clazz == Integer.class) {
            return "int";
        } else if (clazz == long.class || clazz == Long.class) {
            return "long";
        } else if (clazz == byte.class || clazz == Byte.class) {
            return "byte";
        } else if (clazz == float.class || clazz == Float.class) {
            return "float";
        } else if (clazz == double.class || clazz == Double.class) {
            return "double";
        } else if (clazz == short.class || clazz == Short.class) {
            return "short";
        }
        return "";
    }

    /**
     * Tests a numeric candidate using the comparison width selected by the target.
     *
     * @param t the number to test
     * @return whether the comparison holds for {@code t}
     * @throws IllegalArgumentException if the fixed number was of a class this has no
     *         comparison for
     * @since 1.6.5
     */
    @Override
    public Boolean apply(Number t) {
        return applyOperation(t);
    }

    // I know this is not ideal, but there is no easy way to compare dynamic numbers during runtime
    /**
     * Dispatches to the comparison selected by the fixed target's width.
     *
     * @param num the number to test
     * @return whether the comparison holds for {@code num}
     * @throws IllegalArgumentException if the fixed number was of a class with no
     *         comparison, with a message saying the type of the number does not exist
     * @since 1.6.5
     */
    private boolean applyOperation(Number num) {
        switch (numberType.toLowerCase(Locale.ROOT)) {
            case "byte":
                return compareByte(num, compareTo);
            case "short":
                return compareShort(num, compareTo);
            case "int":
                return compareInt(num, compareTo);
            case "long":
                return compareLong(num, compareTo);
            case "float":
                return compareFloat(num, compareTo);
            case "double":
                return compareDouble(num, compareTo);
            default:
                throw new IllegalArgumentException("The type of the number doesn't exist");
        }
    }

    /**
     * Compares {@link Number#doubleValue()} values; ordering is exact, while equality and
     * inequality use a tolerance of {@code 0.000001}.
     *
     * @param num the number to test
     * @param compareTo the fixed number
     * @return whether the comparison holds
     * @throws IllegalArgumentException if the operator is not one of the six
     * @since 1.6.5
     */
    private boolean compareDouble(Number num, Number compareTo) {
        switch (operation) {
            case ">":
                return num.doubleValue() > compareTo.doubleValue();
            case ">=":
                return num.doubleValue() >= compareTo.doubleValue();
            case "<":
                return num.doubleValue() < compareTo.doubleValue();
            case "<=":
                return num.doubleValue() <= compareTo.doubleValue();
            case "==":
                return DoubleMath.fuzzyEquals(num.doubleValue(), compareTo.doubleValue(), EPSILON);
            case "!=":
                return !DoubleMath.fuzzyEquals(num.doubleValue(), compareTo.doubleValue(), EPSILON);
            default:
                throw new IllegalArgumentException("Unknown operation, try < > <= >= == != instead of " + operation);
        }
    }

    /**
     * Compares {@link Number#floatValue()} values; ordering is exact, while equality and
     * inequality use a tolerance of {@code 0.000001}.
     *
     * @param num the number to test
     * @param compareTo the fixed number
     * @return whether the comparison holds
     * @throws IllegalArgumentException if the operator is not one of the six
     * @since 1.6.5
     */
    private boolean compareFloat(Number num, Number compareTo) {
        switch (operation) {
            case ">":
                return num.floatValue() > compareTo.floatValue();
            case ">=":
                return num.floatValue() >= compareTo.floatValue();
            case "<":
                return num.floatValue() < compareTo.floatValue();
            case "<=":
                return num.floatValue() <= compareTo.floatValue();
            case "==":
                return DoubleMath.fuzzyEquals(num.floatValue(), compareTo.floatValue(), EPSILON);
            case "!=":
                return !DoubleMath.fuzzyEquals(num.floatValue(), compareTo.floatValue(), EPSILON);
            default:
                throw new IllegalArgumentException("Unknown operation, try < > <= >= == != instead of " + operation);
        }
    }

    /**
     * Compares {@link Number#longValue()} values using exact ordering and equality.
     *
     * @param num the number to test
     * @param compareTo the fixed number
     * @return whether the comparison holds
     * @throws IllegalArgumentException if the operator is not one of the six
     * @since 1.6.5
     */
    private boolean compareLong(Number num, Number compareTo) {
        switch (operation) {
            case ">":
                return num.longValue() > compareTo.longValue();
            case ">=":
                return num.longValue() >= compareTo.longValue();
            case "<":
                return num.longValue() < compareTo.longValue();
            case "<=":
                return num.longValue() <= compareTo.longValue();
            case "==":
                return num.longValue() == compareTo.longValue();
            case "!=":
                return num.longValue() != compareTo.longValue();
            default:
                throw new IllegalArgumentException("Unknown operation, try < > <= >= == != instead of " + operation);
        }
    }

    /**
     * Compares {@link Number#intValue()} values using exact ordering and equality.
     *
     * @param num the number to test
     * @param compareTo the fixed number
     * @return whether the comparison holds
     * @throws IllegalArgumentException if the operator is not one of the six
     * @since 1.6.5
     */
    private boolean compareInt(Number num, Number compareTo) {
        switch (operation) {
            case ">":
                return num.intValue() > compareTo.intValue();
            case ">=":
                return num.intValue() >= compareTo.intValue();
            case "<":
                return num.intValue() < compareTo.intValue();
            case "<=":
                return num.intValue() <= compareTo.intValue();
            case "==":
                return num.intValue() == compareTo.intValue();
            case "!=":
                return num.intValue() != compareTo.intValue();
            default:
                throw new IllegalArgumentException("Unknown operation, try < > <= >= == != instead of " + operation);
        }
    }

    /**
     * Compares {@link Number#shortValue()} values using exact ordering and equality.
     *
     * @param num the number to test
     * @param compareTo the fixed number
     * @return whether the comparison holds
     * @throws IllegalArgumentException if the operator is not one of the six
     * @since 1.6.5
     */
    private boolean compareShort(Number num, Number compareTo) {
        switch (operation) {
            case ">":
                return num.shortValue() > compareTo.shortValue();
            case ">=":
                return num.shortValue() >= compareTo.shortValue();
            case "<":
                return num.shortValue() < compareTo.shortValue();
            case "<=":
                return num.shortValue() <= compareTo.shortValue();
            case "==":
                return num.shortValue() == compareTo.shortValue();
            case "!=":
                return num.shortValue() != compareTo.shortValue();
            default:
                throw new IllegalArgumentException("Unknown operation, try < > <= >= == != instead of " + operation);
        }
    }

    /**
     * Compares the operands' {@link Number#byteValue()} values using exact ordering and
     * equality. Values outside the byte range are narrowed before comparison.
     *
     * @param num the number to test
     * @param compareTo the fixed number
     * @return whether the comparison holds
     * @throws IllegalArgumentException if the operator is not one of the six
     * @since 1.6.5
     */
    private boolean compareByte(Number num, Number compareTo) {
        switch (operation) {
            case ">":
                return num.byteValue() > compareTo.byteValue();
            case ">=":
                return num.byteValue() >= compareTo.byteValue();
            case "<":
                return num.byteValue() < compareTo.byteValue();
            case "<=":
                return num.byteValue() <= compareTo.byteValue();
            case "==":
                return num.byteValue() == compareTo.byteValue();
            case "!=":
                return num.byteValue() != compareTo.byteValue();
            default:
                throw new IllegalArgumentException("Unknown operation, try < > <= >= == != instead of " + operation);
        }
    }

}
