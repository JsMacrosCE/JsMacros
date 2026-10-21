package com.jsmacrosce.jsmacros.core.library.impl;

import com.google.common.collect.ImmutableList;
import com.jsmacrosce.doclet.DocletIgnore;
import javassist.CannotCompileException;
import javassist.NotFoundException;
import org.joor.Reflect;
import com.jsmacrosce.Util;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.doclet.DocletReplaceTypeParams;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;
import com.jsmacrosce.jsmacros.core.library.BaseLibrary;
import com.jsmacrosce.jsmacros.core.library.Library;
import com.jsmacrosce.jsmacros.core.library.PerExecLibrary;
import com.jsmacrosce.jsmacros.core.library.impl.classes.ClassBuilder;
import com.jsmacrosce.jsmacros.core.library.impl.classes.LibraryBuilder;
import com.jsmacrosce.jsmacros.core.library.impl.classes.ProxyBuilder;
import com.jsmacrosce.jsmacros.core.library.impl.classes.proxypackage.Neighbor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Loads classes and uses Java reflection through the script global {@code Reflection}.
 * Minecraft class and member names depend on the loader, runtime mappings, and Minecraft target;
 * they are not universally intermediary names. Primary class lookup uses this library's loader,
 * including jars added with {@link #loadJarFile(String)}. The two-name {@link #getClass(String, String)}
 * overload uses the default loader for its fallback. Legacy {@code xyz.wagyourtail.} names are
 * rewritten before lookup.
 * <p>
 * Returned classes, methods and fields are ordinary Java reflection objects. Names absent from
 * the shipped TypeScript package model are typed as {@code unknown}; raw Minecraft members are
 * not modeled by those typings. Resolve methods separately from invoking them, for example with
 * {@link #invokeMethod(Method, Object, Object...)}.
 * <pre>
 * // Resolve and construct a JDK class, then look up and invoke a method separately.
 * const ArrayList = Reflection.getClass("java.util.ArrayList");
 * const list = Reflection.newInstance(ArrayList);
 * list.add("diamond");
 * const size = Reflection.getDeclaredMethod(ArrayList, "size");
 * print(`the list holds ${Reflection.invokeMethod(size, list)}`);
 * </pre>
 * A declared-field lookup can find non-public fields, but reading one requires access and may be
 * denied by Java access restrictions.
 * @author Wagyourtail
 * @since 1.2.3
 */
@Library("Reflection")
@SuppressWarnings("unused")
public class FReflection extends PerExecLibrary {
    private static final Map<String, List<Class<?>>> JAVA_CLASS_CACHE = new HashMap<>();
    /** Shared loader used by {@link #getClass(String)} and {@link #loadJarFile(String)}. */
    public static final CombinedVariableClassLoader classLoader = new CombinedVariableClassLoader(FReflection.class.getClassLoader());

    /** Associates this per-execution library with its script context.
     * @param context script context associated with this library
     */
    public FReflection(BaseScriptContext<?> context) {
        super(context);
    }

    /**
     * Resolves a fully qualified class name through this library's loader, including jars added
     * by {@link #loadJarFile(String)}, and initializes the class. The nine lower-case Java
     * primitive names, including {@code void}, resolve directly to their primitive classes.
     * The legacy {@code xyz.wagyourtail.} package prefix is rewritten before loading.
     * Returns an ordinary {@link Class}; use {@link #getClass(String, String)} for an alternative
     * name when the first is absent. Minecraft class names depend on the runtime mappings.
     *
     * @param name name of class like {@code path.to.class}
     * @param <T> type represented by the resolved class
     * @return resolved class
     * @throws ClassNotFoundException
     * @since 1.2.3
     */
    @DocletReplaceTypeParams("C extends string")
    @DocletReplaceParams(
            """
            name: C): GetJava.Type$Reflection<C>;
            getClass<C extends JavaTypeList | keyof GetJava.Primitives>(name: C"""
    )
    @DocletReplaceReturn("GetJava.Type$Reflection<C>")
    public <T> Class<T> getClass(String name) throws ClassNotFoundException {
        switch (name) {
            case "boolean":
                return (Class<T>) boolean.class;
            case "byte":
                return (Class<T>) byte.class;
            case "short":
                return (Class<T>) short.class;
            case "int":
                return (Class<T>) int.class;
            case "long":
                return (Class<T>) long.class;
            case "float":
                return (Class<T>) float.class;
            case "double":
                return (Class<T>) double.class;
            case "char":
                return (Class<T>) char.class;
            case "void":
                return (Class<T>) void.class;
            default:
                name = redirectWagYourTail(name);
                return (Class<T>) Class.forName(name, true, classLoader);
        }
    }

    /**
     * Tries the first class name, then the second only if the first lookup throws
     * {@link ClassNotFoundException}. This can accommodate differing runtime mappings when the
     * supplied names match them; it does not guarantee that either name exists on every version.
     * The second lookup uses the default class loader, not this library's loader, so it does not
     * use jars registered only through {@link #loadJarFile(String)}. A primitive first name resolves directly and
     * does not fall back; a successfully resolved but unintended class also prevents fallback.
     * <pre>
     * // Put the runtime's expected name first; the alternative is tried only if it is absent.
     * const List = Reflection.getClass("example.Missing", "java.util.ArrayList");
     * const list = Reflection.newInstance(List);
     * list.add("item");
     * </pre>
     *
     * @param name  first try
     * @param name2 second try
     * @param <T> type represented by the resolved class
     * @return a {@link java.lang.Class Class} reference.
     * @throws ClassNotFoundException if the first name is not found and the second one is not
     *         either
     * @since 1.2.3
     */
    @DocletReplaceTypeParams("C extends string")
    @DocletReplaceParams(
            """
            name: C, name2: string): GetJava.Type$Reflection<C>;
            getClass<C extends JavaTypeList | keyof GetJava.Primitives>(name: C, name2: JavaTypeList | keyof GetJava.Primitives"""
    )
    @DocletReplaceReturn("GetJava.Type$Reflection<C>")
    public <T> Class<T> getClass(String name, String name2) throws ClassNotFoundException {
        name = redirectWagYourTail(name);
        name2 = redirectWagYourTail(name2);

        try {
            return getClass(name);
        } catch (ClassNotFoundException e) {
            return (Class<T>) Class.forName(name2);
        }
    }

    /**
     * Finds a method declared directly by the class, matching its exact parameter classes; it
     * does not find inherited methods. Use {@link #getMethod(Class, String, Class...)} for public
     * methods including inherited ones. The two-name {@link #getDeclaredMethod(Class, String, String, Class...)}
     * overload retries with another method name. The returned {@link java.lang.reflect.Method} must be
     * invoked separately with {@link #invokeMethod(Method, Object, Object...)}. Non-public
     * methods may require {@code setAccessible(true)}, subject to Java access restrictions.
     * <pre>
     * const StringClass = Reflection.getClass("java.lang.String");
     * const startsWith = Reflection.getDeclaredMethod(
     *   StringClass, "startsWith", StringClass);
     * print(Reflection.invokeMethod(startsWith, "hello", "he"));
     * </pre>
     *
     * @param c the class to look the method up on
     * @param name the method's name
     * @param parameterTypes the exact classes of the method's parameters, none for no parameters
     * @return the method, which has to be invoked separately
     * @throws NoSuchMethodException if the class declares no such method
     * @throws SecurityException
     * @since 1.2.3
     */
    public Method getDeclaredMethod(Class<?> c, String name, Class<?>... parameterTypes) throws NoSuchMethodException, SecurityException {
        return c.getDeclaredMethod(name, parameterTypes);
    }

    /**
     * Finds a declared method by trying {@code name}, then {@code name2} if lookup by the first
     * name throws {@link NoSuchMethodException} or {@link SecurityException}. Both names use the
     * same exact parameter types.
     *
     * @param c class declaring the method
     * @param name primary method name
     * @param name2 fallback method name
     * @param parameterTypes exact parameter types
     * @return the matching method
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @since 1.2.3
     */
    public Method getDeclaredMethod(Class<?> c, String name, String name2, Class<?>... parameterTypes) throws NoSuchMethodException, SecurityException {
        try {
            return c.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException | SecurityException e) {
            return c.getDeclaredMethod(name2, parameterTypes);
        }
    }

    /**
     * Finds a public method by trying {@code name}, then {@code name2} if the first lookup throws
     * {@link NoSuchMethodException} or {@link SecurityException}.
     * @param c class to search
     * @param name primary method name
     * @param name2 fallback method name
     * @param parameterTypes exact parameter types
     * @return the matching public method
     * @throws NoSuchMethodException
     * @since 1.6.0
     */
    public Method getMethod(Class<?> c, String name, String name2, Class<?>... parameterTypes) throws NoSuchMethodException {
        try {
            return c.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException | SecurityException e) {
            return c.getMethod(name2, parameterTypes);
        }
    }

    /**
     * Finds a public method by name and exact parameter types.
     * @param c class to search
     * @param name method name
     * @param parameterTypes exact parameter types
     * @return the matching public method
     * @throws NoSuchMethodException
     * @since 1.6.0
     */
    public Method getMethod(Class<?> c, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        return c.getMethod(name, parameterTypes);
    }

    /**
     * Finds a field declared directly by the class, including non-public fields.
     * @param c class to search
     * @param name field name
     * @return the declared field
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @see FReflection#getDeclaredField(Class, String, String)
     * @since 1.2.3
     */
    public Field getDeclaredField(Class<?> c, String name) throws NoSuchFieldException, SecurityException {
        return c.getDeclaredField(name);
    }

    /**
     * Finds a declared field by trying {@code name}, then {@code name2} if the first lookup throws
     * {@link NoSuchFieldException} or {@link SecurityException}.
     *
     * @param c class declaring the field
     * @param name primary field name
     * @param name2 fallback field name
     * @return the matching field
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @since 1.2.3
     */
    public Field getDeclaredField(Class<?> c, String name, String name2) throws NoSuchFieldException, SecurityException {
        try {
            return c.getDeclaredField(name);
        } catch (NoSuchFieldException | SecurityException e) {
            return c.getDeclaredField(name2);
        }
    }

    /**
     * Finds a public field by name.
     * @param c class to search
     * @param name field name
     * @return the matching public field
     * @throws NoSuchFieldException
     * @since 1.6.0
     */
    public Field getField(Class<?> c, String name) throws NoSuchFieldException {
        return c.getField(name);
    }

    /**
     * Returns a public field, trying {@code name2} if lookup by {@code name} throws
     * {@link NoSuchFieldException} or {@link SecurityException}.
     * @param c class to search
     * @param name primary field name
     * @param name2 fallback field name
     * @return the matching public field
     * @throws NoSuchFieldException if neither lookup succeeds
     * @since 1.6.0
     */
    public Field getField(Class<?> c, String name, String name2) throws NoSuchFieldException {
        try {
            return c.getField(name);
        } catch (NoSuchFieldException | SecurityException e) {
            return c.getField(name2);
        }
    }

    /**
     * Invokes a method, coercing each numeric argument to the corresponding parameter type.
     * Non-numeric arguments are passed through. Pass {@code null} as the receiver for a static
     * method. The underlying reflective invocation enforces access and wraps target exceptions in
     * {@link InvocationTargetException}.
     * <pre>
     * // a number out of a script is a double, and the coercion makes it fit an int
     * // parameter anyway. a null receiver means a static method
     * const Integer = Reflection.getClass("java.lang.Integer");
     * const bitCount = Reflection.getDeclaredMethod(Integer, "bitCount",
     *   Reflection.getClass("int"));
     * print(`the bit count of 3 is ${Reflection.invokeMethod(bitCount, null, 3)}`);
     *
     * // anything that is not a number is passed through untouched
     * const ArrayList = Reflection.getClass("java.util.ArrayList");
     * const list = Reflection.newInstance(ArrayList);
     * const add = Reflection.getDeclaredMethod(ArrayList, "add",
     *   Reflection.getClass("java.lang.Object"));
     * Reflection.invokeMethod(add, list, "diamond");
     * print(`the list holds ${list.size()}`);
     * </pre>
     *
     * @param m method to invoke
     * @param c receiver, or {@code null} for a static method
     * @param objects invocation arguments
     * @return the method result, or {@code null} for a {@code void} method
     * @throws IllegalAccessException if the method is inaccessible
     * @throws IllegalArgumentException if the arguments do not match the method parameters
     * @throws InvocationTargetException if the method itself threw
     * @since 1.2.3
     */
    public Object invokeMethod(Method m, Object c, Object... objects) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class<?>[] params = m.getParameterTypes();
        for (int i = 0; i < objects.length; ++i) {
            objects[i] = Util.tryAutoCastNumber(params[i], objects[i]);
        }
        return m.invoke(c, objects);
    }

    /**
     * Creates an instance using a matching public constructor, coercing numeric arguments if needed.
     * A {@code null} argument causes a {@link NullPointerException} before constructor lookup.
     *
     * @param c class to instantiate
     * @param objects constructor arguments
     * @param <T> type of instance to construct
     * @return the constructed instance
     * @since 1.2.7
     */
    public <T> T newInstance(Class<T> c, Object... objects) {
        return newInstance0(c, objects);
    }

    /**
     * Constructs an instance using a public constructor with exactly matching runtime parameter
     * types, or tries public constructors of the same arity with numeric arguments coerced to their
     * parameter types. If no attempt succeeds, throws a {@link RuntimeException}.
     *
     * @param c       the class to construct
     * @param objects constructor arguments
     * @param <T> type of instance to construct
     * @return the constructed instance
     * @throws RuntimeException if no constructor of the class takes those arguments
     */
    public static <T> T newInstance0(Class<T> c, Object... objects) {
        Class<?>[] params = new Class<?>[objects.length];
        for (int i = 0; i < objects.length; ++i) {
            params[i] = objects[i].getClass();
        }
        try {
            Constructor<T> con = c.getConstructor(params);
            return con.newInstance(objects);
        } catch (Exception e) {
            for (Constructor<?> con : c.getConstructors()) {
                if (con.getParameterTypes().length != objects.length) {
                    continue;
                }
                params = con.getParameterTypes();
                Object[] tempObjects = new Object[objects.length];
                try {
                    for (int i = 0; i < objects.length; ++i) {
                        tempObjects[i] = Util.tryAutoCastNumber(params[i], objects[i]);
                    }
                    return (T) con.newInstance(tempObjects);
                } catch (Exception ignored) {
                }
            }
            throw new RuntimeException("Failed to create new instance, bad parameters?", e);
        }
    }

    /**
     * Creates a builder for a guest-language proxy implementing the requested interfaces.
     *
     * @param clazz superclass to proxy
     * @param interfaces interfaces implemented by the proxy
     * @param <T> superclass type
     * @return the proxy builder
     * @since 1.6.0
     */
    public <T> ProxyBuilder<T> createClassProxyBuilder(Class<T> clazz, Class<?>... interfaces) {
        return new ProxyBuilder<>(clazz, interfaces);
    }

    /**
     * @param cName generated class name
     * @param clazz superclass
     * @param interfaces interfaces implemented by the generated class
     * @param <T> superclass type
     * @return the class builder
     * @throws NotFoundException if a referenced class cannot be found
     * @throws CannotCompileException if the class cannot be compiled
     * @since 1.6.5
     */
    public <T> ClassBuilder<T> createClassBuilder(String cName, Class<T> clazz, Class<?>... interfaces) throws NotFoundException, CannotCompileException {
        cName = redirectWagYourTail(cName);

        return new ClassBuilder<>(cName, clazz, interfaces);
    }

    /**
     * @param cName generated class name used to create the class
     * @return the generated class
     * @throws ClassNotFoundException if the generated class cannot be found
     * @since 1.6.5
     */
    public Class<?> getClassFromClassBuilderResult(String cName) throws ClassNotFoundException {
        return Class.forName("com.jsmacrosce.jsmacros.core.library.impl.classes.proxypackage." + cName.replaceAll("\\.", "\\$"), true, Neighbor.class.getClassLoader());
    }

    /**
     * Creates a builder for a runtime-generated library class, configured with its script global,
     * lifecycle, and supported languages. The generated class is registered for scripts only when
     * {@link LibraryBuilder#finishBuildAndFreeze()} is called.
     * {@link LibraryBuilder#addConstructor()} adds a public constructor whose parameters are
     * determined by this builder's lifecycle and language configuration; if omitted, the builder
     * supplies a forwarding constructor automatically.
     *
     * @param name name of the script global
     * @param perExec whether to create a library per script run rather than per profile
     * @param acceptedLangs guest languages allowed to use the library; empty means all
     * @return a builder for assembling the library class
     * @throws NotFoundException if a name could not be resolved while building
     * @throws CannotCompileException if the assembled class does not compile
     */
    public LibraryBuilder createLibraryBuilder(String name, boolean perExec, String... acceptedLangs) throws NotFoundException, CannotCompileException {
        return new LibraryBuilder(runner, name, perExec, acceptedLangs);
    }

    /**
     * Compiles Java source as a class and registers it as a library. The class must carry a
     * {@link Library} annotation and extend {@link BaseLibrary} (directly or through a supported
     * per-exec or per-language base class).
     *
     * @param className the fully qualified name of the class, including the package
     * @param javaCode  the source code of the library
     * @since 1.8.4
     */
    public void createLibrary(String className, String javaCode) {
        // TODO: Should we dynamically redirect xyz.wagyourtail to com.jsmacrosce in here?
        runner.libraryRegistry.addLibrary((Class<? extends BaseLibrary>) compileJavaClass(className, javaCode));
    }

    /**
     * Compiles source into a runtime class and keeps each compilation under the requested name.
     * Each call creates a distinct class; previously returned classes and their instances are
     * unaffected. Use the returned {@link Class} reference to construct instances or store them
     * with {@link FGlobalVars#putObject(String, Object)}. A JDK is required to compile classes.
     * <pre>
     * // Recompiling the same name creates a separate class.
     * const first = Reflection.compileJavaClass("com.example.Counter",
     *   "package com.example; public class Counter { public int n = 0; }");
     * const second = Reflection.compileJavaClass("com.example.Counter",
     *   "package com.example; public class Counter { public int n = 100; }");
     * const versions = Reflection.getAllCompiledJavaClassVersions("com.example.Counter");
     * print(`that name now has ${versions.size()} compiled versions`);
     *
     * // Keep the class reference to choose which version to instantiate.
     * const instance = Reflection.newInstance(first);
     * GlobalVars.putObject("counter", instance);
     * // getCompiledJavaClass gives the most recent compilation of that name
     * print(`and the newest is the second one: ${Reflection.getCompiledJavaClass("com.example.Counter") === second}`);
     * </pre>
     *
     * @param className the fully qualified name of the class, including the package
     * @param code      the java code to compile
     * @return the newly compiled class
     * @since 1.8.4
     */
    public Class<?> compileJavaClass(String className, String code) {
        // TODO: Should we dynamically redirect xyz.wagyourtail to com.jsmacrosce in here?
        Class<?> clazz = Reflect.compile(className, code).type();
        JAVA_CLASS_CACHE.putIfAbsent(className, new ArrayList<>());
        JAVA_CLASS_CACHE.get(className).add(clazz);
        return clazz;
    }

    /**
     * Returns the most recently compiled class cached under the requested name.
     *
     * @param className the fully qualified class name
     * @return the most recently compiled class, or {@code null} if none is cached
     * @since 1.8.4
     */
    public Class<?> getCompiledJavaClass(String className) {
        className = redirectWagYourTail(className);

        List<Class<?>> versions = JAVA_CLASS_CACHE.get(className);
        return versions == null ? null : versions.get(versions.size() - 1);
    }

    /**
     * Returns the compiled classes cached under the requested name, in compilation order.
     *
     * @param className the fully qualified class name
     * @return an immutable snapshot of compiled classes in compilation order, or an empty list if none are cached
     * @since 1.8.4
     */
    public List<Class<?>> getAllCompiledJavaClassVersions(String className) {
        className = redirectWagYourTail(className);
        List<Class<?>> versions = JAVA_CLASS_CACHE.get(className);
        return versions == null ? Collections.emptyList() : ImmutableList.copyOf(versions);
    }

    /**
     * Returns a jOOR reflection wrapper for the object. Wrapper operations can be chained; fields
     * are made accessible when reached. Unlike this library's invocation helpers, the wrapper does
     * not coerce numeric arguments.
     * <pre>
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond");
     * const reflected = Reflection.getReflect(stack.getRaw());
     * print(`calling through it: ${reflected.call("getCount")}`);
     * print(`the private count field says ${reflected.field("count").get()}`);
     * </pre>
     *
     * @param obj the object to wrap
     * @return a reflection wrapper for the object
     * @since 1.8.4
     */
    public Reflect getReflect(Object obj) {
        return Reflect.on(obj);
    }

    /**
     * Adds a jar from the script's folder to this library's class loader. Its classes can then be
     * resolved through {@link #getClass(String)} and compilation using that loader. Loading an
     * already-added jar returns {@code true}; a missing path throws {@link java.io.FileNotFoundException}.
     * <pre>
     * const first = Reflection.loadJarFile("libs/mylib.jar");
     * const second = Reflection.loadJarFile("libs/mylib.jar");
     * print(`loaded ${first}, and again ${second}`);
     *
     * const MyThing = Reflection.getClass("com.example.MyThing");
     * print(`that is ${Reflection.getClassName(MyThing)}`);
     * </pre>
     *
     * @param file path to the jar, resolved relative to the script's folder
     * @return {@code true} if the jar was added or was already loaded
     * @throws java.io.FileNotFoundException if there is no file at that path
     * @throws IOException if the jar cannot be added as a class loader
     * @since 1.2.6
     */
    public boolean loadJarFile(String file) throws IOException {
        File jarFile = ctx.getContainedFolder().toPath().resolve(file).toFile();
        if (classLoader.hasJar(jarFile)) {
            return true;
        }
        if (!jarFile.exists()) {
            throw new FileNotFoundException("Jar File Not Found");
        }
        return classLoader.addClassLoader(jarFile, new URLClassLoader(new URL[]{new URL("jar:file:" + jarFile.getCanonicalPath() + "!/")}));
    }

    /**
     * Returns the canonical name of a class or an object's runtime class. Canonical names use dots
     * for nested classes; the result is {@code null} for local and anonymous classes. Arrays and
     * primitive classes use their canonical names.
     * <pre>
     * const ArrayList = Reflection.getClass("java.util.ArrayList");
     * // a Class names itself, whatever the instance it came from was
     * print(`the class is ${Reflection.getClassName(ArrayList)}`);
     * // an instance names its runtime type, which is the subclass
     * print(`the instance is ${Reflection.getClassName(Reflection.newInstance(ArrayList))}`);
     * // a primitive class names the primitive
     * print(`and a primitive is ${Reflection.getClassName(Reflection.getClass("int"))}`);
     * </pre>
     *
     * @param o a {@link Class} or an object whose runtime class should be named
     * @return the canonical class name, or {@code null} if it has none
     * @since 1.3.1
     */
    public String getClassName(Object o) {
        if (o instanceof Class) {
            return ((Class<?>) o).getCanonicalName();
        } else {
            return o.getClass().getCanonicalName();
        }
    }

    /**
     * Replaces every occurrence of "xyz.wagyourtail." in the given string with "com.jsmacrosce.".
     *
     * @param name the string to rewrite
     * @return a new string with every occurrence of "xyz.wagyourtail." replaced by "com.jsmacrosce."
     */
    @DocletIgnore
    public static String redirectWagYourTail(String name) {
        return name.replace("xyz.wagyourtail.", "com.jsmacrosce.");
    }

    /**
     * Class loader that searches registered sibling class loaders for classes and resources.
     * Based on Christian d'Heureuse's JoinClassLoader, modified to store delegates by canonical
     * jar path.
     *
     * @author Wagyourtail, Christian d'Heureuse
     * @since 1.2.8
     */
    protected static class CombinedVariableClassLoader extends ClassLoader {
        private final Map<File, ClassLoader> siblingDelegates = new LinkedHashMap<>();

        public CombinedVariableClassLoader(ClassLoader parent) {
            super(parent);
        }

        public boolean addClassLoader(File jarPath, ClassLoader loader) throws IOException {
            return siblingDelegates.putIfAbsent(jarPath.getCanonicalFile(), loader) == null;
        }

        public boolean hasJar(File path) throws IOException {
            return siblingDelegates.containsKey(path.getCanonicalFile());
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            String path = name.replace('.', '/') + ".class";
            URL url = findResource(path);
            if (url == null) {
                throw new ClassNotFoundException(name);
            }
            ByteBuffer byteCode;
            try {
                byteCode = loadResource(url);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
            return defineClass(name, byteCode, null);
        }

        private ByteBuffer loadResource(URL url) throws IOException {
            try (InputStream stream = url.openStream()) {
                int initialBufferCapacity = Math.min(0x40000, stream.available() + 1);
                if (initialBufferCapacity <= 2) {
                    initialBufferCapacity = 0x10000;
                } else {
                    initialBufferCapacity = Math.max(initialBufferCapacity, 0x200);
                }
                ByteBuffer buf = ByteBuffer.allocate(initialBufferCapacity);
                while (true) {
                    if (!buf.hasRemaining()) {
                        ByteBuffer newBuf = ByteBuffer.allocate(2 * buf.capacity());
                        buf.flip();
                        newBuf.put(buf);
                        buf = newBuf;
                    }
                    int len = stream.read(buf.array(), buf.position(), buf.remaining());
                    if (len <= 0) {
                        break;
                    }
                    buf.position(buf.position() + len);
                }
                buf.flip();
                return buf;
            }
        }

        @Override
        protected URL findResource(String name) {
            for (ClassLoader delegate : siblingDelegates.values()) {
                URL resource = delegate.getResource(name);
                if (resource != null) {
                    return resource;
                }
            }
            return null;
        }

        @Override
        protected Enumeration<URL> findResources(String name) throws IOException {
            Vector<URL> vector = new Vector<>();
            for (ClassLoader delegate : siblingDelegates.values()) {
                Enumeration<URL> enumeration = delegate.getResources(name);
                while (enumeration.hasMoreElements()) {
                    vector.add(enumeration.nextElement());
                }
            }
            return vector.elements();
        }

    }

}
