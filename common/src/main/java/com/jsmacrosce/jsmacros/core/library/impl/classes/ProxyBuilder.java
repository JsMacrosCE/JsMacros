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
 * a way for a script to supply the body of a Java class without writing one in Java, by naming
 * the methods it wants to answer for and handing each one a function.<br>
 * A script gets one from {@code Reflection.createClassProxyBuilder}, passing the class to extend
 * and any interfaces to implement, and then adds a handler per method before asking for an
 * instance. The result is a real instance of that class: the methods named are the script's, and
 * every other one, including the ones inherited, runs the class's own code. That is the difference
 * from {@code Reflection.createClassBuilder}, which generates a class from source, and from a
 * {@code java.lang.reflect.Proxy}, which only works for interfaces.<br>
 * The interesting part is the argument handling, which is where a script's types stop lining up
 * with the Java method's. Each handler is a wrapper over a
 * {@link com.jsmacrosce.jsmacros.core.MethodWrapper MethodWrapper} taking
 * {@link ProxyReference} and an array of arguments, and the array is filled in as the Java
 * signature declares it: a primitive is boxed, so a method declared {@code int} hands the handler
 * a {@code Number}, not a number. A handler can be given a plain function rather than a wrapper,
 * in which case it runs on the calling thread with no script context to join, so
 * {@code JavaWrapper.methodToJava} is the form to use for anything a script is running.<br>
 * A number on the way back is coerced the same way in the other direction, so a handler returning
 * a plain number satisfies a method declared {@code int} without the script having to narrow it.<br>
 * What happens to an exception a handler throws depends on whether the Java method returns
 * anything. For a method declared {@code void}, it is rethrown into the Java caller when the
 * method declares that kind of thing, is rethrown as is when it is already a
 * {@link RuntimeException}, and is otherwise reported to the script's log and swallowed, so such a
 * handler cannot be relied on to fail the call it was standing in for. Every other return type
 * gets none of that: the handler's result goes straight back through the number coercion, so
 * whatever the handler threw comes out of the call as it is, unchecked if it was not declared,
 * and the script's log never sees it. The examples below are all on methods that return
 * something, so a handler that throws there throws out of the call.<br>
 * The examples below each build their own builder, apart from the ones on the three
 * {@code buildInstance} calls and the one on {@link ProxyReference}, which are shown on the
 * {@code builder} the first of them makes.
 * example:
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
     * the client library's factory that does the actual work, holding the class and the interfaces
     * this builder was made with and the filter that decides which methods are the script's. It is
     * here rather than private, but nothing in a script has a reason to reach past the builder
     * and use it.
     */
    public final ProxyFactory factory;
    /**
     * the handlers that were added with a full signature, keyed by the method's name, parameter
     * types and return type together. Two methods of the same name and different parameters are
     * therefore handled separately, which is what a signature is for.
     */
    public final Map<MethodSigParts, MethodWrapper<ProxyReference<T>, Object[], ?, ?>> proxiedMethods = new HashMap<>();
    /**
     * the handlers that were added with a bare method name, keyed by that name alone. One of
     * these covers every overload of the name, so it is what a name with no signature resolves to
     * and it is used whenever no exact signature matched. Adding a bare name for a method that
     * already has a signature is the way to give one overload its own behaviour and leave the
     * rest to a fallback.
     */
    public final Map<String, MethodWrapper<ProxyReference<T>, Object[], ?, ?>> proxiedMethodDefaults = new HashMap<>();

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
     * names a method the script wants to answer for, and the function that answers it.
     * <br>
     * What goes in as the first argument decides how wide the handler is. A bare method name
     * covers every overload of that name, and is what a call resolves to when no exact signature
     * matched. A full signature covers only that one method, and is looked for first, so a
     * signature and a bare name for the same method is a way to single out one overload and leave
     * the rest to the fallback. The signature is the JVM's own form, the one a class file holds,
     * with a {@code L} and a {@code ;} around a class name and a single letter for a primitive,
     * as in {@code size} against {@code indexOf(Ljava/lang/Object;)I}. A name with no parenthesis
     * in it is always taken as a bare name, since that is what tells the two apart.<br>
     * The handler is a wrapper over {@link ProxyReference} and the method's arguments, so it
     * wants to be made with {@code JavaWrapper.methodToJava} rather than handed as a plain
     * function. Primitive arguments arrive boxed, and a number returned for a primitive return is
     * narrowed on the way out.
     * example:
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
     * @param methodNameOrSig name of method or sig (the usual format)
     * @param proxyMethod     the handler, made with {@code JavaWrapper.methodToJava}
     * @return self for chaining
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
     * builds an instance, working out which constructor to call from the arguments given.
     * <br>
     * The superclass's constructors are looked at for one that takes the arguments, and the
     * lookup starts from the exact runtime types of what was passed. A constructor that takes a
     * supertype of one of them, or any {@link Number} where another {@link Number} is wanted, is
     * accepted as a fallback, and a number is narrowed to the parameter's own type on the way in,
     * so a value the script had as a single number type can still satisfy an {@code int} or a
     * {@code double} parameter. A proxy over an interface has {@code Object} underneath it, so it
     * is built through {@code Object}'s no argument constructor.<br>
     * None of this is checked before the instance is made, so an argument the constructor does not
     * take is a {@link NoSuchMethodException} rather than a failed call.
     * example:
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
     * @param constructorArgs args for the super constructor
     * @return new instance of the constructor
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
     * builds an instance through a constructor named by signature rather than by the arguments.
     * <br>
     * This is {@link #buildInstance(java.lang.Object[]) buildInstance} with the parameter types
     * written out instead of inferred, which is the form to use when the argument alone is not
     * enough to pick a constructor out, such as a {@code null} whose type the signature settles.
     * The name is the JVM's own form, with a {@code L} and a {@code ;} around a class name and a
     * single letter for a primitive, as in {@code (Ljava/lang/String;I)V}. The
     * {@code &lt;init&gt;} part may be left off, so {@code "()V"} and
     * {@code "&lt;init&gt;()V"} are the same thing.<br>
     * A class named in the signature that cannot be resolved fails here rather than being skipped.
     * example:
     * <pre>
     * // a constructor taking a String and an int, named rather than inferred
     * const started = builder.buildInstance("(Ljava/lang/String;I)V", ["diamond", 3]);
     * </pre>
     *
     * @param constructorSig  string signature (you can skip the &lt;init&gt; part)
     * @param constructorArgs args for the super constructor
     * @return new instance of the constructor
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
     * builds an instance, taking the parameter types and arguments as two arrays that line up.
     * <br>
     * This is the form with both halves given, so it is the one that does not have to work out
     * what a constructor takes. Each argument is narrowed to the type at the same position, so a
     * number the script holds as one type can still be passed for an {@code int} or a
     * {@code double}, and a position the signature has no type for, or a value that is not a
     * number where one is wanted, fails here rather than at the call.<br>
     * The return value of the signature is not used for anything, but it still has to be there,
     * since that is what makes the string parse.
     * example:
     * <pre>
     * // the types and the values, side by side. this is the way to reach a
     * // constructor whose parameters the arguments alone would not pick out
     * const started = builder.buildInstance(
     *   [Reflection.getClass("java.lang.String"), Reflection.getClass("int")],
     *   ["diamond", 3]);
     * </pre>
     *
     * @param constructorSig  the parameter types, as classes, with a return type at the end since
     *                        that is the shape the parser expects
     * @param constructorArgs args for the super constructor
     * @return new instance of the constructor
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
     * what a handler is handed as its first argument: the instance the method was called on, and
     * a way to reach the real method underneath the proxy.
     * <br>
     * A script does not make one of these; it is what the builder fills in and passes to whatever
     * was added through {@link ProxyBuilder#addMethod(String, MethodWrapper) addMethod}.
     * example:
     * <pre class="language-typescript">
     * // `self` is the instance the method was called on, so the methods that
     * // were not named still work on it
     * builder.addMethod("get", JavaWrapper.methodToJava(function (ref, args) {
     *   // and `parent` is the real one, for a method that is
     *   return `proxied at ${ref.self.size()}, which really holds ${ref.parent.apply([0] as any)}`;
     * }));
     * </pre>
     */
    public static class ProxyReference<T> {
        /**
         * "this" value, but like python because "this" is a keyword in java...
         * <br>
         * The proxy itself, which is an instance of the class that was extended, so every method
         * that was not named through {@link ProxyBuilder#addMethod(String, MethodWrapper) addMethod}
         * runs normally on it. Reading a field or calling a real method through this is how a
         * handler reaches the state the class is actually in.
         */
        public final T self;
        /**
         * "super" value, but that's also a keyword so...
         * <br>
         * Calls the method the proxy took over, with the arguments given, and gives back what it
         * returned. This is {@code null} when there is nothing to call, which is the case for a
         * method the superclass does not have at all, such as one of the interface's when the
         * builder was given a concrete class that does not declare it. A handler that calls it
         * when it is {@code null} fails there.
         */
        public final Function<Object[], Object> parent;

        /**
         * makes one of these for a handler to be called with. A script does not call this: the
         * builder builds it, filling in the instance the method was called on and the call
         * through to the real method beside it.
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
