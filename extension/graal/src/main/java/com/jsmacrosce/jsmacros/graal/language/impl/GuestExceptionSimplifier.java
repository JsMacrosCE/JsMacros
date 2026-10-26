package com.jsmacrosce.jsmacros.graal.language.impl;

import com.jsmacrosce.jsmacros.core.library.Library;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.strings.TruffleString;
import org.graalvm.polyglot.PolyglotException;
import com.jsmacrosce.jsmacros.graal.ValueAccessor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
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
    private static final Pattern METHOD_ERROR_PATTERN = Pattern.compile("TypeError: invokeMember \\((?<function>[\\w_$]+)\\) on (?<class>" + QUALIFIER + ")(?:@[\\w]+)? failed due to: (?<error>.*)");
    private static final Pattern CONSTRUCTOR_ERROR_PATTERN = Pattern.compile("TypeError: instantiate on (?<class>" + QUALIFIER + ")(?:@[\\w]+)? failed due to: (?<error>.*)");

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

    static String simplifyArityError(String originalMessage, ArityException arityException) throws ClassNotFoundException {
        if (originalMessage == null) {
            return null;
        }
        Matcher methodMatcher = METHOD_ERROR_PATTERN.matcher(originalMessage);
        if (methodMatcher.matches()) {
            Class<?> clazz = Class.forName(methodMatcher.group("class"));
            List<Method> methods = methods(clazz, methodMatcher.group("function"));
            if (methods.isEmpty()) {
                return originalMessage;
            }
            return formatArityError("method " + scriptClassName(clazz) + "." + methodMatcher.group("function"),
                    methods, arityException.getActualArity(), false);
        }

        Matcher constructorMatcher = CONSTRUCTOR_ERROR_PATTERN.matcher(originalMessage);
        if (constructorMatcher.matches()) {
            Class<?> clazz = Class.forName(constructorMatcher.group("class"));
            List<Constructor<?>> constructors = constructors(clazz);
            if (constructors.isEmpty()) {
                return originalMessage;
            }
            return formatArityError("constructor " + scriptClassName(clazz), constructors,
                    arityException.getActualArity(), true);
        }
        return originalMessage;
    }

    static String simplifyOverloadError(String originalMessage, UnsupportedTypeException exception) throws ClassNotFoundException {
        if (originalMessage == null) {
            return null;
        }
        Matcher methodMatcher = METHOD_ERROR_PATTERN.matcher(originalMessage);
        if (methodMatcher.matches()) {
            Class<?> clazz = Class.forName(methodMatcher.group("class"));
            List<Method> methods = methods(clazz, methodMatcher.group("function"));
            if (methods.isEmpty()) {
                return originalMessage;
            }
            return formatOverloadError("method " + scriptClassName(clazz) + "." + methodMatcher.group("function"),
                    methods, exception.getSuppliedValues(), false);
        }

        Matcher constructorMatcher = CONSTRUCTOR_ERROR_PATTERN.matcher(originalMessage);
        if (constructorMatcher.matches()) {
            Class<?> clazz = Class.forName(constructorMatcher.group("class"));
            List<Constructor<?>> constructors = constructors(clazz);
            if (constructors.isEmpty()) {
                return originalMessage;
            }
            return formatOverloadError("constructor " + scriptClassName(clazz), constructors,
                    exception.getSuppliedValues(), true);
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
        return formatOverloadError("method " + scriptClassName(clazz) + "." + functionName,
                methods(clazz, functionName), suppliedValues, false);
    }

    /**
     * Formats a constructor overload failure using the same shape as a method overload failure.
     *
     * @param clazz class whose constructors were called
     * @param suppliedValues values the guest passed to the constructor
     * @return the formatted diagnostic
     */
    static String formatConstructorOverloadError(Class<?> clazz, Object[] suppliedValues) {
        return formatOverloadError("constructor " + scriptClassName(clazz),
                constructors(clazz), suppliedValues, true);
    }

    /**
     * Formats an arity failure for a method, including varargs methods and their accepted ranges.
     *
     * @param clazz class that declares the called methods
     * @param functionName called method name
     * @param actualArity number of arguments the guest supplied
     * @return the formatted diagnostic
     */
    static String formatMethodArityError(Class<?> clazz, String functionName, int actualArity) {
        return formatArityError("method " + scriptClassName(clazz) + "." + functionName,
                methods(clazz, functionName), actualArity, false);
    }

    /**
     * Formats an arity failure for a constructor, including varargs constructors and their
     * accepted ranges.
     *
     * @param clazz class whose constructors were called
     * @param actualArity number of arguments the guest supplied
     * @return the formatted diagnostic
     */
    static String formatConstructorArityError(Class<?> clazz, int actualArity) {
        return formatArityError("constructor " + scriptClassName(clazz),
                constructors(clazz), actualArity, true);
    }

    private static String formatOverloadError(String target, List<? extends Executable> executables,
                                              Object[] suppliedValues, boolean constructor) {
        String[] names = suppliedArgumentNames(executables, suppliedValues.length);
        List<ArgumentMismatch> mismatches = findMismatches(executables, suppliedValues, names);

        List<String> lines = new ArrayList<>();
        lines.add("OverloadError on " + target);
        lines.add(constructor ? "  Expected constructors (declared types):" : "  Expected arguments (declared types):");
        for (Executable executable : executables) {
            lines.add("    " + formatSignature(executable, constructor));
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

    private static String formatArityError(String target, List<? extends Executable> executables,
                                           int actualArity, boolean constructor) {
        List<String> lines = new ArrayList<>();
        lines.add("ArityError on " + target);
        lines.add(constructor ? "  Expected constructors:" : "  Expected arguments:");
        for (Executable executable : executables) {
            lines.add("    " + formatSignature(executable, constructor) + " - " + describeArity(executable));
        }
        lines.add("  Given: " + actualArity + " argument" + (actualArity == 1 ? "" : "s") + ".");
        return String.join("\n", lines);
    }

    private static String describeArity(Executable executable) {
        if (!executable.isVarArgs()) {
            int count = executable.getParameterCount();
            return count + " argument" + (count == 1 ? "" : "s");
        }
        int fixed = executable.getParameterCount() - 1;
        if (fixed == 0) {
            return "0 or more arguments";
        }
        return "at least " + fixed + " argument" + (fixed == 1 ? "" : "s");
    }

    private static List<Method> methods(Class<?> clazz, String functionName) {
        return Arrays.stream(clazz.getMethods())
                .filter(method -> method.getName().equals(functionName))
                .filter(method -> !method.isBridge() && !method.isSynthetic())
                .sorted(GuestExceptionSimplifier::compareExecutables)
                .toList();
    }

    private static List<Constructor<?>> constructors(Class<?> clazz) {
        return Arrays.stream(clazz.getConstructors())
                .filter(constructor -> !constructor.isSynthetic())
                .sorted(GuestExceptionSimplifier::compareExecutables)
                .toList();
    }

    private static int compareExecutables(Executable first, Executable second) {
        int arity = Integer.compare(first.getParameterCount(), second.getParameterCount());
        if (arity != 0) {
            return arity;
        }
        Class<?>[] firstParams = first.getParameterTypes();
        Class<?>[] secondParams = second.getParameterTypes();
        for (int i = 0; i < firstParams.length; i++) {
            if (firstParams[i] != secondParams[i]) {
                return firstParams[i].getName().compareTo(secondParams[i].getName());
            }
        }
        return first.toGenericString().compareTo(second.toGenericString());
    }

    private static String formatSignature(Executable executable, boolean constructor) {
        StringBuilder builder = new StringBuilder();
        if (constructor) {
            builder.append("new ").append(scriptClassName(executable.getDeclaringClass()));
        }
        builder.append('(');
        Class<?>[] types = executable.getParameterTypes();
        Parameter[] parameters = executable.getParameters();
        for (int i = 0; i < types.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            if (executable.isVarArgs() && i == types.length - 1 && types[i].isArray()) {
                builder.append(simpleTypeName(types[i].getComponentType())).append("...");
            } else {
                builder.append(simpleTypeName(types[i]));
            }
            builder.append(' ').append(parameterName(parameters[i], i));
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
     * Finds supplied arguments that cannot match the declared type shared by every applicable
     * overload. Overloads that disagree on the type are skipped because the value may still be
     * valid for one of them. Varargs parameters map every extra supplied value to the component
     * type, so a bad type inside the varargs tail is still reported.
     */
    private static List<ArgumentMismatch> findMismatches(List<? extends Executable> executables,
                                                         Object[] suppliedValues, String[] names) {
        List<? extends Executable> applicable = applicable(executables, suppliedValues.length);
        if (applicable.isEmpty()) {
            return List.of();
        }

        List<ArgumentMismatch> mismatches = new ArrayList<>();
        for (int i = 0; i < suppliedValues.length; i++) {
            Class<?> expected = null;
            boolean consistent = true;
            for (Executable executable : applicable) {
                Class<?> candidate = expectedTypeAt(executable, i, suppliedValues[i]);
                if (candidate == null) {
                    consistent = false;
                    break;
                }
                if (expected == null) {
                    expected = candidate;
                } else if (!expected.equals(candidate)) {
                    consistent = false;
                    break;
                }
            }
            if (consistent && expected != null && isDefinitelyIncompatible(suppliedValues[i], expected)) {
                mismatches.add(new ArgumentMismatch(i, names[i], expected, suppliedValues[i]));
            }
        }
        return mismatches;
    }

    private static List<? extends Executable> applicable(List<? extends Executable> executables, int argumentCount) {
        return executables.stream()
                .filter(executable -> acceptsArity(executable, argumentCount))
                .toList();
    }

    private static boolean acceptsArity(Executable executable, int argumentCount) {
        if (!executable.isVarArgs()) {
            return executable.getParameterCount() == argumentCount;
        }
        return argumentCount >= executable.getParameterCount() - 1;
    }

    private static Class<?> expectedTypeAt(Executable executable, int argumentIndex, Object value) {
        Class<?>[] types = executable.getParameterTypes();
        if (!executable.isVarArgs()) {
            return argumentIndex < types.length ? types[argumentIndex] : null;
        }
        int fixed = types.length - 1;
        if (argumentIndex < fixed) {
            return types[argumentIndex];
        }
        Class<?> varargsArray = types[fixed];
        if (argumentIndex == fixed && value != null && varargsArray.isInstance(value)) {
            return varargsArray;
        }
        return varargsArray.getComponentType();
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
                    double number = ((Number) value).doubleValue();
                    return number != Math.rint(number) || !fitsIntegralRange(expected, number);
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

    private static boolean fitsIntegralRange(Class<?> expected, double value) {
        if (expected == byte.class) {
            return value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE;
        }
        if (expected == short.class) {
            return value >= Short.MIN_VALUE && value <= Short.MAX_VALUE;
        }
        if (expected == int.class) {
            return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
        }
        return value >= Long.MIN_VALUE && value <= Long.MAX_VALUE;
    }

    /**
     * Marks a segment with the legacy yellow formatting code. Minecraft's chat renderer applies
     * these codes inside literal components; other consumers see the codes as plain characters.
     */
    private static String highlight(String text) {
        return "\u00A7e" + text + "\u00A7r";
    }

    /**
     * Finds a stable name for each supplied argument. Names are only used when every applicable
     * overload agrees on the name, so the list never mislabels a value by borrowing a name from
     * an unrelated signature. Extra varargs values reuse the varargs parameter's name.
     */
    private static String[] suppliedArgumentNames(List<? extends Executable> executables, int count) {
        String[] names = new String[count];
        List<? extends Executable> applicable = applicable(executables, count);
        for (int i = 0; i < count; i++) {
            String commonName = null;
            boolean agrees = true;
            for (Executable executable : applicable) {
                String name = parameterNameAt(executable, i);
                if (name == null) {
                    agrees = false;
                    break;
                }
                if (commonName == null) {
                    commonName = name;
                } else if (!commonName.equals(name)) {
                    agrees = false;
                    break;
                }
            }
            names[i] = agrees && commonName != null ? commonName : "arg" + i;
        }
        return names;
    }

    private static String parameterNameAt(Executable executable, int argumentIndex) {
        if (executable.getParameterCount() == 0) {
            return null;
        }
        int parameterIndex = executable.isVarArgs()
                ? Math.min(argumentIndex, executable.getParameterCount() - 1)
                : argumentIndex;
        if (parameterIndex < 0 || parameterIndex >= executable.getParameterCount()) {
            return null;
        }
        Parameter parameter = executable.getParameters()[parameterIndex];
        return parameter.isNamePresent() ? parameter.getName() : null;
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

    /**
     * Uses the script-facing library name when the class is a {@code @Library} implementation,
     * so errors refer to {@code World} rather than the Java class {@code FWorld}.
     */
    private static String scriptClassName(Class<?> clazz) {
        try {
            Library library = clazz.getAnnotation(Library.class);
            if (library != null && !library.value().isBlank()) {
                return library.value();
            }
        } catch (RuntimeException | LinkageError ignored) {
            // Fall through to the Java name if the annotation cannot be read.
        }
        return simpleClassName(clazz);
    }

    private static String simpleClassName(Class<?> clazz) {
        String simpleName = clazz.getSimpleName();
        if (!simpleName.isEmpty()) {
            return simpleName;
        }
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
