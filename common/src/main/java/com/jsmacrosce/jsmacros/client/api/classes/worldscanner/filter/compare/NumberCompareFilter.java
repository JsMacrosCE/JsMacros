package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.compare;

import com.google.common.math.DoubleMath;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;

import java.util.Locale;

/**
 * a filter that compares a number against a fixed one with an ordering operator.
 * <p>
 * This is the comparison behind a numeric builder step: the step supplies an operator and a
 * number, and each value the named method returns is tested against that number. The six
 * operators are {@code ">="}, {@code ">"}, {@code "<="}, {@code "<"}, {@code "=="} and
 * {@code "!="}.
 * <p>
 * The operator itself is not checked when the filter is built. The constructor stores it as
 * it was given and reads only the class of the number, so a name outside that set is first
 * met when a value is compared, and it is that comparison, during a scan, which throws.
 * This is the other way round from the string comparisons, which resolve a comparison name
 * in their constructor and refuse a name that is not one of their five there and then.
 * <p>
 * Two things about the comparison are worth knowing before relying on an equality test. The
 * first is that the width used comes from the <i>number the filter was given</i>, not from
 * the method being filtered. A number given as a decimal decides an equality test with a
 * small tolerance, so two values that differ only by rounding still count as equal; a number
 * given as a whole number is compared exactly, and a value of the same magnitude but a
 * different width is truncated to that width before the comparison, so a whole number target
 * truncates the candidate as well.
 * <p>
 * The second is that this is decided once, when the filter is built, from the class of the
 * number supplied. A number of some other numeric class, one that is not one of the six
 * widths above or their boxed forms, is accepted by the constructor and then has no
 * comparison to run, and testing such a filter throws.
 * <br>
 * <b>This class is not in the shipped TypeScript definitions and a script never names it.</b>
 * The builder constructs it whenever a named method returns one of the number types, and
 * supplies both arguments from the {@code is} step.
 * example:
 * <pre>
 * // getHardness is a declared public method of the block helper taking no
 * // parameters and returning a float, so the step takes an operator and a
 * // number. The ordering operators are exact at every width, so this is an
 * // exact greater-or-equal test whichever class the 10 arrives as
 * const hard = World.getWorldScanner()
 *   .withBlockFilter("getHardness").is(">=", 10)
 *   .build();
 * Chat.log(`${hard.scanAroundPlayer(2).size()} blocks with a hardness of 10 or more`);
 *
 * // getBlastResistance returns a float and the target 1200.5 is a decimal,
 * // so this runs as a comparison of doubles. The ordering operators are
 * // exact even so: only == and != get the tolerance, so the > here is a
 * // plain greater-than on the two values as they are
 * const strong = World.getWorldScanner()
 *   .withBlockFilter("getBlastResistance").is(">", 1200.5)
 *   .build();
 * Chat.log(`${strong.scanAroundPlayer(2).size()} very blast resistant blocks`);
 * </pre>
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
     * fixes the operator and the number candidates are compared against.
     * <p>
     * The class of {@code compareTo} is inspected here, once, and decides which comparison
     * runs later — including whether an equality test is exact or tolerant, and how wide a
     * candidate is truncated to before the comparison. Passing a number as a string does not
     * work: it is the class of the object, not its text, that is inspected.
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
     * names the width of a number, from its class.
     * <p>
     * A primitive and its boxed form give the same name, so the width does not depend on
     * whether the value arrived boxed. A number of some other numeric class gives an empty
     * name, which is not a width anything can be compared with, and the constructor does not
     * object to it — the failure comes later, when the filter is tested.
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
     * tests a number against the fixed one.
     * <p>
     * This is a plain dispatch to whichever comparison matches the width of the fixed number,
     * and it adds nothing of its own.
     * <p>
     * A script does not call this. It is what a builder step naming a method with a numeric
     * return type ends up testing each value with.
     * example:
     * <pre>
     * // getLuminance is a declared public method of the state helper taking
     * // no parameters and returning an int, so the step takes an operator and
     * // a number
     * const bright = World.getWorldScanner()
     *   .withStateFilter("getLuminance").is("==", 15)
     *   .build();
     * Chat.log(`${bright.scanAroundPlayer(2).size()} states emitting full light`);
     * </pre>
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
     * runs the comparison that matches the width of the fixed number.
     * <p>
     * The name decided in the constructor selects one of six private comparisons, but the
     * name is switched on again on every test, and the comparison it picks switches on the
     * operator in turn, so an operator outside the six is refused by the first test rather
     * than when the filter is built. An empty name, which is what a number of an
     * unrecognised numeric class gives, matches none of the six cases here and is refused
     * instead.
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
     * compares as doubles, with a tolerance on an equality test.
     * <p>
     * The ordering comparisons are exact. The equality comparison is not: the two values are
     * considered equal when the absolute difference between them is at most
     * {@code 0.000001}, which is what lets a rounded decimal stand in for the value it was
     * rounded from.
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
     * compares as floats, with a tolerance on an equality test.
     * <p>
     * The ordering comparisons are exact. The equality comparison treats the two values as
     * equal when the absolute difference between them is at most {@code 0.000001}.
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
     * compares as longs, exactly.
     * <p>
     * Both values are narrowed to a long before the comparison, so a value of a wider type
     * than the fixed number is truncated. An equality test here is exact, with no tolerance.
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
     * compares as ints, exactly.
     * <p>
     * Both values are narrowed to an int before the comparison, so a value of a wider type
     * than the fixed number is truncated — a whole number that does not fit in an int wraps
     * rather than saturating. An equality test here is exact, with no tolerance.
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
     * compares as shorts, exactly.
     * <p>
     * Both values are narrowed to a short before the comparison, so a value of a wider type
     * than the fixed number is truncated and may wrap. An equality test here is exact, with
     * no tolerance.
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
     * compares as bytes, exactly.
     * <p>
     * Both values are narrowed to a byte before the comparison, so a value of a wider type
     * than the fixed number is truncated and very likely wraps, since a byte holds a much
     * smaller range than any of the others. An equality test here is exact, with no
     * tolerance.
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
