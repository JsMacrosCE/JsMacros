package com.jsmacrosce.jsmacros.graal.language.impl;

import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.strings.TruffleString;
import org.graalvm.polyglot.PolyglotException;
import com.jsmacrosce.jsmacros.graal.ValueAccessor;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Simplifies selected Graal guest arity and overload errors into diagnostic messages. */
public final class GuestExceptionSimplifier {

    private static final String QUALIFIER = "[\\w.$]+";
    private static final Pattern TYPE_ERROR_PATTERN = Pattern.compile("TypeError: invokeMember \\((?<function>[\\w_$]+)\\) on (?<class>" + QUALIFIER + ")(?:@[\\w]+)? failed due to: (?<error>.*)");

    private GuestExceptionSimplifier() {
    }

    /**
     * Simplifies guest arity and overload errors when their Graal exception details and message
     * format are recognized. Other exceptions and failures during simplification return the
     * original exception message.
     *
     * @param ex the exception to simplify
     * @return the simplified diagnostic when recognized, or the original message otherwise
     */
    public static String simplifyException(Throwable ex) {
        try {
            if (ex instanceof PolyglotException) {
                PolyglotException p = (PolyglotException) ex;
                if (p.isGuestException()) {
                    Object guest = ValueAccessor.getReceiver(((PolyglotException) ex).getGuestObject());
                    if (guest instanceof AbstractTruffleException) {
                        Throwable cause = ((AbstractTruffleException) guest).getCause();
                        if (cause instanceof ArityException) {
                            return simplifyArityError(ex.getMessage(), (ArityException) cause);
                        } else if (cause instanceof UnsupportedTypeException) {
                            return simplifyOverloadError(ex.getMessage(), (UnsupportedTypeException) cause);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("Failed to simplify exception: " + t.getMessage());
            t.printStackTrace();
        }
        return ex.getMessage();
    }

    private static String simplifyArityError(String originalMessage, ArityException arityException) throws ClassNotFoundException {
        Matcher matcher = TYPE_ERROR_PATTERN.matcher(originalMessage);
        if (matcher.matches()) {
            String functionName = matcher.group("function");
            String className = matcher.group("class");
            String error = matcher.group("error");
            Class<?> clazz = Class.forName(className);
            StringBuilder builder = new StringBuilder();
            builder.append("ArityError on method ").append(className).append(".").append(functionName).append("\n");
            // Only count public methods, including static ones, defined in that class
            int[] possibleArities = Arrays.stream(clazz.getMethods())
                    .filter(m -> m.getName().equals(functionName))
                    .mapToInt(Method::getParameterCount)
                    .sorted()
                    .distinct()
                    .toArray();
            builder.append("  Expected amount of arguments: ")
                    .append(possibleArities.length == 1 ? possibleArities[0] : Arrays.toString(possibleArities))
                    .append(", but got ")
                    .append(arityException.getActualArity())
                    .append(" arguments.");
            return builder.toString();
        }
        return originalMessage;
    }

    private static String simplifyOverloadError(String originalMessage, UnsupportedTypeException exception) throws ClassNotFoundException {
        Matcher matcher = TYPE_ERROR_PATTERN.matcher(originalMessage);
        if (matcher.matches()) {
            String functionName = matcher.group("function");
            String className = matcher.group("class");
            Class<?> clazz = Class.forName(className);
            return formatOverloadError(clazz, functionName, exception.getSuppliedValues());
        }
        return originalMessage;
    }

    /**
     * Formats an overload failure with the declared parameter names and the values the guest
     * supplied. Classes compiled without {@code -parameters} fall back to {@code arg0},
     * {@code arg1}, and so on.
     *
     * @param clazz class that declares the called methods
     * @param functionName called method name
     * @param suppliedValues values the guest passed to the call
     * @return the formatted diagnostic
     */
    static String formatOverloadError(Class<?> clazz, String functionName, Object[] suppliedValues) {
        List<Method> methods = Arrays.stream(clazz.getMethods())
                .filter(method -> method.getName().equals(functionName))
                .filter(method -> !method.isBridge() && !method.isSynthetic())
                .sorted(GuestExceptionSimplifier::compareMethods)
                .toList();
        String[] names = suppliedArgumentNames(methods, suppliedValues.length);
        List<ArgumentMismatch> mismatches = findMismatches(methods, suppliedValues, names);

        List<String> lines = new ArrayList<>();
        lines.add("OverloadError on method " + simpleClassName(clazz) + "." + functionName);
        lines.add("  Expected arguments (declared types):");
        for (Method method : methods) {
            lines.add("    " + formatSignature(method));
        }

        lines.add("  Given arguments (runtime types):");
        lines.add("    " + formatGivenArguments(suppliedValues, names, mismatches));
        if (!mismatches.isEmpty()) {
            lines.add("  Mismatched arguments:");
            for (ArgumentMismatch mismatch : mismatches) {
                lines.add("    " + highlight(mismatch.name() + ": expected "
                        + simpleTypeName(mismatch.expected()) + ", got " + formatValue(mismatch.actual())));
            }
        }
        return String.join("\n", lines);
    }

    private static int compareMethods(Method first, Method second) {
        if (first.getParameterCount() != second.getParameterCount()) {
            return Integer.compare(first.getParameterCount(), second.getParameterCount());
        }
        Class<?>[] firstParams = first.getParameterTypes();
        Class<?>[] secondParams = second.getParameterTypes();
        for (int i = 0; i < firstParams.length; i++) {
            if (firstParams[i] != secondParams[i]) {
                return firstParams[i].getName().compareTo(secondParams[i].getName());
            }
        }
        return 0;
    }

    private static String formatSignature(Method method) {
        StringBuilder builder = new StringBuilder("(");
        Class<?>[] types = method.getParameterTypes();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < types.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(simpleTypeName(types[i]))
                    .append(' ')
                    .append(parameterName(parameters[i], i));
        }
        return builder.append(')').toString();
    }

    private static String formatGivenArguments(Object[] suppliedValues, String[] names, List<ArgumentMismatch> mismatches) {
        StringBuilder builder = new StringBuilder("(");
        for (int i = 0; i < suppliedValues.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            String value = names[i] + ": " + formatValue(suppliedValues[i]);
            builder.append(isMismatched(mismatches, i) ? highlight(value) : value);
        }
        return builder.append(')').toString();
    }

    /**
     * Finds supplied arguments that cannot match the declared type shared by every overload with
     * the same argument count. Overloads that disagree on the type are skipped because the value
     * may still be valid for one of them.
     */
    private static List<ArgumentMismatch> findMismatches(List<Method> methods, Object[] suppliedValues, String[] names) {
        List<Method> sameArity = methods.stream()
                .filter(method -> method.getParameterCount() == suppliedValues.length)
                .toList();
        if (sameArity.isEmpty()) {
            return List.of();
        }

        List<ArgumentMismatch> mismatches = new ArrayList<>();
        for (int i = 0; i < suppliedValues.length; i++) {
            Class<?> expected = sameArity.get(0).getParameterTypes()[i];
            boolean consistent = true;
            for (int j = 1; j < sameArity.size(); j++) {
                if (sameArity.get(j).getParameterTypes()[i] != expected) {
                    consistent = false;
                    break;
                }
            }
            if (consistent && isDefinitelyIncompatible(suppliedValues[i], expected)) {
                mismatches.add(new ArgumentMismatch(i, names[i], expected, suppliedValues[i]));
            }
        }
        return mismatches;
    }

    private static boolean isMismatched(List<ArgumentMismatch> mismatches, int index) {
        for (ArgumentMismatch mismatch : mismatches) {
            if (mismatch.index() == index) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reports only conversions that are clearly invalid, so the diagnostic does not blame an
     * argument that GraalVM may still be able to convert. Reference types other than strings and
     * numbers are left alone because a host wrapper may implement them.
     */
    private static boolean isDefinitelyIncompatible(Object value, Class<?> expected) {
        if (value == null) {
            return expected.isPrimitive();
        }
        if (expected.isInstance(value) || expected == Object.class) {
            return false;
        }
        if (expected.isPrimitive()) {
            if (expected == boolean.class) {
                return !(value instanceof Boolean);
            }
            if (expected == char.class) {
                return !(value instanceof Character) && !(value instanceof TruffleString);
            }
            if (expected == byte.class || expected == short.class || expected == int.class || expected == long.class) {
                if (value instanceof Double || value instanceof Float) {
                    return true;
                }
                return !(value instanceof Number);
            }
            if (expected == float.class || expected == double.class) {
                return !(value instanceof Number);
            }
            return false;
        }
        if (expected == String.class || expected == CharSequence.class) {
            return !(value instanceof String) && !(value instanceof TruffleString);
        }
        if (expected == Number.class) {
            return !(value instanceof Number);
        }
        return false;
    }

    /**
     * Marks a segment with the legacy yellow formatting code. Minecraft's chat renderer applies
     * these codes inside literal components; other consumers see the codes as plain characters.
     */
    private static String highlight(String text) {
        return "\u00A7e" + text + "\u00A7r";
    }

    /**
     * Finds a stable name for each supplied argument. Names are only used when every overload
     * with the supplied argument count agrees on the name, so the list never mislabels a value
     * by borrowing a name from an unrelated signature.
     */
    private static String[] suppliedArgumentNames(List<Method> methods, int count) {
        String[] names = new String[count];
        List<Method> sameArity = methods.stream()
                .filter(method -> method.getParameterCount() == count)
                .toList();
        for (int i = 0; i < count; i++) {
            String commonName = null;
            boolean agrees = true;
            for (Method method : sameArity) {
                Parameter parameter = method.getParameters()[i];
                if (!parameter.isNamePresent()) {
                    agrees = false;
                    break;
                }
                if (commonName == null) {
                    commonName = parameter.getName();
                } else if (!commonName.equals(parameter.getName())) {
                    agrees = false;
                    break;
                }
            }
            names[i] = agrees && commonName != null ? commonName : "arg" + i;
        }
        return names;
    }

    private static String parameterName(Parameter parameter, int index) {
        return parameter.isNamePresent() ? parameter.getName() : "arg" + index;
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof TruffleString) {
            return "String";
        }
        String typeName = simpleTypeName(value.getClass());
        if (value instanceof Number || value instanceof Boolean || value instanceof Character) {
            return typeName + " = " + value;
        }
        return typeName;
    }

    private static String simpleClassName(Class<?> clazz) {
        String name = clazz.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot == -1 ? name : name.substring(lastDot + 1);
    }

    private static String simpleTypeName(Class<?> type) {
        if (type.isArray()) {
            return simpleTypeName(type.getComponentType()) + "[]";
        }
        String name = type.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot == -1 ? name : name.substring(lastDot + 1);
    }

    private record ArgumentMismatch(int index, String name, Class<?> expected, Object actual) {
    }

}
