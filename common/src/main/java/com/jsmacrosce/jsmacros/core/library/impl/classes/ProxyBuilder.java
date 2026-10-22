package com.jsmacrosce.jsmacros.core.library.impl.classes;

import javassist.util.proxy.ProxyFactory;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.Util;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds subclass or interface proxies whose selected methods are handled by script callbacks.
 * Unmatched methods proceed to the superclass implementation. Obtain a builder with
 * {@code Reflection.createClassProxyBuilder}, register handlers, and construct an instance.
 * Handlers receive a {@link ProxyReference} and boxed method arguments; primitive numeric returns
 * are converted to the declared type. Use {@code JavaWrapper.methodToJava} for script callbacks.
 * For {@code void} methods, declared exceptions and runtime exceptions are rethrown; other handler
 * exceptions are suppressed after being logged to the script context, or printed to standard error
 * when no context exists. Exceptions from non-{@code void} handlers propagate through the call. The examples demonstrate interface
 * and superclass proxies, overload fallback, and calling the overridden implementation.
 * <pre>
 * // an interface is proxied too, with Object underneath it
 * const runnable = Reflection.getClass("java.lang.Runnable");
 * const builder = Reflection.createClassProxyBuilder(runnable);
 *
 * // a bare name covers every overload of that name. a full signature covers
 * // just the one, and is what wins when both would match
 * builder.addMethod("run", JavaWrapper.methodToJava(function (ref, args) {
 *   print("the proxy ran");
 * }));
 *
 * const task = builder.buildInstance([]);
 * task.run();
 *
 * // extending a concrete class is the same builder. `self` is the instance
 * // the method was called on and `parent` reaches the real one, so the parts
 * // that were not named here still behave normally
 * const listBuilder = Reflection.createClassProxyBuilder(
 *   Reflection.getClass("java.util.ArrayList"));
 * listBuilder.addMethod("toString", JavaWrapper.methodToJava(function (ref, args) {
 *   return `a proxy over a list of ${ref.self.size()}`;
 * }));
 *
 * const list = listBuilder.buildInstance([]);
 * list.add("diamond");
 * print(list.toString());
 * </pre>
 * @param <T> the type of the class being extended, which is what the built instances come back as
 * @author Wagyourtail
 * @since 1.6.0
 */
public class ProxyBuilder<T> {
    /**
     * Javassist factory configured with this builder's superclass, interfaces, and method filter.
     */
    public final ProxyFactory factory;
    /**
     * Handlers keyed by method name, parameter types, and return type.
     */
    public final Map<MethodSigParts, MethodWrapper<ProxyReference<T>, Object[], ?, ?>> proxiedMethods = new HashMap<>();
    /**
     * Fallback handlers keyed by bare method name; used for matching methods without an exact
     * signature handler.
     */
    public final Map<String, MethodWrapper<ProxyReference<T>, Object[], ?, ?>> proxiedMethodDefaults = new HashMap<>();

    /**
     * Configures proxy generation for a class or interface without constructing a proxy instance.
     * For a class, {@code clazz} is the superclass; for an interface, it is added to
     * {@code interfaces} and {@code Object} is used as the superclass. Register method handlers
     * and construct instances afterward. Usually obtained through
     * {@code Reflection.createClassProxyBuilder}.
     *
     * @param clazz class to extend or interface to add to the proxy
     * @param interfaces additional interfaces implemented by the proxy
     */
    public ProxyBuilder(Class<T> clazz, Class<?>[] interfaces) {
        this.factory = new ProxyFactory();
        if (clazz.isInterface()) {
            factory.setSuperclass(Object.class);
            interfaces = Arrays.copyOf(interfaces, interfaces.length + 1);
            interfaces[interfaces.length - 1] = clazz;
        } else {
            factory.setSuperclass(clazz);
        }
        factory.setInterfaces(interfaces);
        factory.setFilter(m -> getWrapperForMethod(m) != null);
    }

    private MethodWrapper<ProxyReference<T>, Object[], ?, ?> getWrapperForMethod(Method m) {
        MethodSigParts sig = methodToSigParts(m);
        MethodWrapper<ProxyReference<T>, Object[], ?, ?> wrapper = proxiedMethods.get(sig);
        if (wrapper == null) {
            wrapper = proxiedMethodDefaults.get(sig.name);
        }
        return wrapper;
    }

    /**
     * Registers a handler for a method name or JVM method signature. A bare name is a fallback
     * for all overloads; an exact signature takes precedence. Signatures use JVM descriptors, such
     * as {@code size()I} or {@code indexOf(Ljava/lang/Object;)I}. Handlers receive a
     * {@link ProxyReference} and boxed arguments; numeric results are converted to primitive return
     * types. Use {@code JavaWrapper.methodToJava} for script callbacks.
     * <pre class="language-typescript">
     * const builder = Reflection.createClassProxyBuilder(
     *   Reflection.getClass("java.util.ArrayList"));
     *
     * // every `add`, whatever it is declared as
     * builder.addMethod("add", JavaWrapper.methodToJava(function (ref, args) {
     *   print(`adding ${args.length} argument(s)`);
     *   return ref.parent.apply(args as any);
     * }));
     *
     * // the same method given both ways. a signature is looked for first, so
     * // `size()I` is the handler that runs, and the bare name is only a
     * // fallback for any other `size` the class turns out to have
     * builder.addMethod("size", JavaWrapper.methodToJava(function (ref, args) {
     *   return 0;
     * }));
     * builder.addMethod("size()I", JavaWrapper.methodToJava(function (ref, args) {
     *   print(`the real size, which is ${ref.parent.apply([] as any)}`);
     *   return ref.parent.apply([] as any);
     * }));
     * </pre>
     *
     * @param methodNameOrSig bare method name or JVM method signature
     * @param proxyMethod handler for matching method calls
     * @return this builder
     * @throws ClassNotFoundException if a signature was given and a class named in it could not
     *                                be resolved
     * @since 1.6.0
     */
    public ProxyBuilder<T> addMethod(String methodNameOrSig, MethodWrapper<ProxyReference<T>, Object[], ?, ?> proxyMethod) throws ClassNotFoundException {
        String[] parts = methodNameOrSig.split("\\(");
        if (parts.length > 1) {
            proxiedMethods.put(mapMethodSig(methodNameOrSig), proxyMethod);
        } else {
            proxiedMethodDefaults.put(methodNameOrSig, proxyMethod);
        }
        return this;
    }

    /**
     * Builds an instance by selecting a superclass constructor from the arguments' runtime types.
     * It first tries an exact type match, then accepts assignable supertypes or other numeric
     * types; numeric arguments are converted to the selected parameter types. Arguments cannot be
     * {@code null}: inspecting their runtime types throws {@link NullPointerException}. For an
     * interface proxy, the superclass is {@code Object} and its no-argument constructor is used.
     * If no compatible constructor is found, this throws {@link NoSuchMethodException}.
     * <pre>
     * // the constructor is worked out from what is passed, so this is
     * // java.util.ArrayList's no argument one
     * const list = builder.buildInstance([]);
     * list.add("diamond");
     *
     * // and this is the one taking a single element
     * const started = builder.buildInstance(["diamond"]);
     * print(`${started.size()} from the start`);
     * </pre>
     *
     * @param constructorArgs arguments passed to the selected superclass constructor
     * @return the constructed proxy instance
     * @throws InvocationTargetException if the constructor itself threw
     * @throws NoSuchMethodException    if no constructor of the superclass takes those arguments
     * @throws InstantiationException   if the superclass could not be instantiated
     * @throws IllegalAccessException   if the superclass or its constructor is not accessible
     * @since 1.6.0
     */
    public T buildInstance(Object[] constructorArgs) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Class<?>[] params = new Class<?>[constructorArgs.length];
        for (int i = 0; i < constructorArgs.length; ++i) {
            params[i] = constructorArgs[i].getClass();
        }
        try {
            Constructor<?> con = factory.getSuperclass().getDeclaredConstructor(params);
            return buildInstance(con.getParameterTypes(), constructorArgs);
        } catch (NoSuchMethodException | SecurityException ignored) {
            for (Constructor<?> constructor : factory.getSuperclass().getDeclaredConstructors()) {
                if (areParamsCompatible(params, constructor.getParameterTypes())) {
                    return buildInstance(constructor.getParameterTypes(), constructorArgs);
                }
            }
            throw new NoSuchMethodException("Constructor for supplied types doesn't exist");
        }
    }

    /**
     * Builds an instance using parameter types parsed from a JVM constructor signature. Include
     * the return descriptor, normally {@code V}; the {@code <init>} name may be omitted. Unlike
     * {@link #buildInstance(java.lang.Object[])}, this selects the constructor by declared
     * parameter types, which can disambiguate arguments such as {@code null}. A referenced class
     * that cannot be resolved causes {@link ClassNotFoundException}.
     * <pre>
     * // a constructor taking a String and an int, named rather than inferred
     * const started = builder.buildInstance("(Ljava/lang/String;I)V", ["diamond", 3]);
     * </pre>
     *
     * @param constructorSig JVM constructor signature
     * @param constructorArgs arguments passed to the selected superclass constructor
     * @return the constructed proxy instance
     * @throws InvocationTargetException if the constructor itself threw
     * @throws NoSuchMethodException    if the superclass has no constructor with that signature
     * @throws InstantiationException   if the superclass could not be instantiated
     * @throws IllegalAccessException   if the superclass or its constructor is not accessible
     * @throws ClassNotFoundException   if a class named in the signature could not be resolved
     * @since 1.6.0
     */
    public T buildInstance(String constructorSig, Object[] constructorArgs) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return buildInstance(mapMethodSig(constructorSig).params, constructorArgs);
    }

    /**
     * Builds an instance using the supplied parameter types and corresponding arguments. Numeric
     * arguments are converted when {@link Util#tryAutoCastNumber(Class, Object)} supports the
     * declared parameter type. The parameter types must describe a superclass constructor; its
     * return type is not used.
     * <pre>
     * // the types and the values, side by side. this is the way to reach a
     * // constructor whose parameters the arguments alone would not pick out
     * const started = builder.buildInstance(
     *   [Reflection.getClass("java.lang.String"), Reflection.getClass("int")],
     *   ["diamond", 3]);
     * </pre>
     *
     * @param constructorSig parameter types of the superclass constructor
     * @param constructorArgs arguments passed to that constructor
     * @return the constructed proxy instance
     * @throws NoSuchMethodException  if the superclass has no constructor taking those types
     * @throws IllegalArgumentException if the signature is not one the parser can read
     * @throws InstantiationException if the superclass could not be instantiated
     * @throws IllegalAccessException if the superclass or its constructor is not accessible
     * @throws InvocationTargetException if the constructor itself threw
     * @since 1.6.0
     */
    public T buildInstance(Class<?>[] constructorSig, Object[] constructorArgs) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        for (int i = 0; i < constructorArgs.length; ++i) {
            constructorArgs[i] = Util.tryAutoCastNumber(constructorSig[i], constructorArgs[i]);
        }
        return (T) factory.create(constructorSig, constructorArgs, this::invoke);
    }

    private Object invoke(Object self, Method thisMethod, @Nullable Method proceed, Object[] args) throws Throwable {
        MethodWrapper<ProxyReference<T>, Object[], ?, ?> wrapper = getWrapperForMethod(thisMethod);
        if (wrapper == null) {
            return proceed.invoke(self, args);
        }
        if (thisMethod.getReturnType().equals(void.class)) {
            try {
                wrapper.accept(new ProxyReference<>((T) self, proceed != null ? (arg) -> {
                    try {
                        return proceed.invoke(self, arg);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                } : null), args);
            } catch (Throwable e) {
                if (Arrays.stream(thisMethod.getExceptionTypes()).anyMatch(f -> f.isAssignableFrom(e.getCause().getClass()))) {
                    throw e.getCause();
                } else if (e.getCause() instanceof RuntimeException) {
                    throw e.getCause();
                } else {
                    BaseScriptContext<?> ctx = wrapper.getCtx();
                    if (ctx != null) {
                        ctx.runner.profile.logError(e.getCause());
                    } else {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
        return Util.tryAutoCastNumber(thisMethod.getReturnType(), wrapper.apply(new ProxyReference<>((T) self, proceed != null ? (arg) -> {
            try {
                return proceed.invoke(self, arg);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } : null), args));
    }

    private static final Pattern sigPart = Pattern.compile("[ZBCSIJFDV]|L(.+?);");

    private MethodSigParts mapMethodSig(String methodSig) throws ClassNotFoundException {
        String[] parts = methodSig.split("[()]", 3);
        List<Class<?>> params = new ArrayList<>();
        Matcher m = sigPart.matcher(parts[1]);
        while (m.find()) {
            String clazz = m.group(1);
            if (clazz == null) {
                params.add(getPrimitive(m.group().charAt(0)));
            } else {
                params.add(Class.forName(clazz.replace("/", ".")));
            }
        }
        Class<?> retval;
        Matcher r = sigPart.matcher(parts[2]);
        if (r.find()) {
            String clazz = r.group(1);
            if (clazz == null) {
                retval = getPrimitive(r.group().charAt(0));
            } else {
                retval = Class.forName(clazz.replace("/", "."));
            }
        } else {
            throw new IllegalArgumentException("Signature return value invalid.");
        }

        return new MethodSigParts(parts[0], params.toArray(new Class[0]), retval);
    }

    private MethodSigParts methodToSigParts(Method mthd) {
        return new MethodSigParts(mthd.getName(), mthd.getParameterTypes(), mthd.getReturnType());
    }

    private static Class<?> getPrimitive(char c) {
        return switch (c) {
            case 'Z' -> boolean.class;
            case 'B' -> byte.class;
            case 'C' -> char.class;
            case 'S' -> short.class;
            case 'I' -> int.class;
            case 'J' -> long.class;
            case 'F' -> float.class;
            case 'D' -> double.class;
            case 'V' -> void.class;
            default -> throw new NullPointerException("Unknown Primitive: " + c);
        };
    }

    private static Class<?> boxPrimitive(Class<?> primitive) {
        if (!primitive.isPrimitive()) return primitive;
        if (primitive == boolean.class) return Boolean.class;
        if (primitive == byte.class) return Byte.class;
        if (primitive == char.class) return Character.class;
        if (primitive == short.class) return Short.class;
        if (primitive == int.class) return Integer.class;
        if (primitive == long.class) return Long.class;
        if (primitive == float.class) return Float.class;
        if (primitive == double.class) return Double.class;
        if (primitive == void.class) return Void.class;
        throw new NullPointerException("Unknown Primitive: " + primitive);
    }

    private static boolean areParamsCompatible(Class<?>[] fuzzable, Class<?>[] target) {
        if (fuzzable.length != target.length) {
            return false;
        }
        for (int i = 0; i < fuzzable.length; ++i) {
            Class<?> targetArg = boxPrimitive(target[i]);
            Class<?> fuzzableArg = boxPrimitive(fuzzable[i]);
            if (targetArg.isAssignableFrom(fuzzableArg) || (Number.class.isAssignableFrom(targetArg) && Number.class.isAssignableFrom(fuzzableArg))) {
                continue;
            }
            return false;
        }
        return true;
    }

    /**
     * Holds the proxy instance and a callable for the overridden method, and is passed to handlers
     * registered with {@link ProxyBuilder#addMethod(String, MethodWrapper) addMethod}.
     * @param <T> the proxy instance type
     */
    public static class ProxyReference<T> {
        /**
         * The proxy instance on which the intercepted method was called.
         */
        public final T self;
        /**
         * Calls the overridden method with the supplied arguments and returns its result, or is
         * {@code null} when there is no superclass method to invoke.
         */
        public final Function<Object[], Object> parent;

        /**
         * Creates the handler reference for a proxy instance and its overridden method.
         *
         * @param self   the instance the method was called on
         * @param parent calls the method the proxy took over, or {@code null} when the superclass
         *               has no such method
         */
        public ProxyReference(T self, Function<Object[], Object> parent) {
            this.self = self;
            this.parent = parent;
        }

    }

    private static class MethodSigParts {
        public final String name;
        public final Class<?>[] params;
        public final Class<?> returnType;

        MethodSigParts(String name, Class<?>[] params, Class<?> returnType) {
            this.name = name;
            this.params = params;
            this.returnType = returnType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MethodSigParts)) {
                return false;
            }
            MethodSigParts that = (MethodSigParts) o;
            return name.equals(that.name) && Arrays.equals(params, that.params) && returnType.equals(that.returnType);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(name, returnType);
            result = 31 * result + Arrays.hashCode(params);
            return result;
        }

    }

}
