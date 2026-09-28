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
 * Functions for getting and using raw java classes, methods and functions.
 * <p>
 * An instance of this class is passed to scripts as the {@code Reflection} variable.
 * <br>
 * This is the escape hatch out of the wrapped API and into plain Java reflection. Most of the
 * time a script does not need it, because the client libraries already wrap the vanilla classes
 * with the parts that are useful. It earns its keep where there is no wrapper, or where a field is
 * private and the wrapper does not expose it.
 * <br>
 * The two things worth knowing before using it are that the class names here are
 * intermediary-mapped names, so they are the {@code net.minecraft} ones and not the Mojang ones,
 * and that a name is looked up in a class loader that also holds whatever
 * {@link #loadJarFile(String) loadJarFile} added, which is why a class from a jar next to a script
 * can be found without being named by path. Names beginning with the old {@code xyz.wagyourtail.}
 * package are rewritten to the current one on the way in.<br>
 * One thing that only shows up in an editor: the shipped TypeScript definitions only describe the
 * JDK, so a name under {@code net.minecraft} has no type in them and comes back typed as
 * {@code unknown} rather than as a class. It still resolves at run time, so such a name only has
 * to be cast where it is passed on as a class, as the {@link #getClass(String) getClass} example
 * shows.
 * <br>
 * Nothing here wraps what it finds. A method comes back as a {@link java.lang.reflect.Method} and
 * a field as a {@link java.lang.reflect.Field}, so calling one is
 * {@link #invokeMethod(Method, Object, Object...) invokeMethod} rather than a direct call, and a
 * private field has to be made accessible first.
 * example:
 * <pre>
 * // a class by its fully qualified name
 * const ArrayList = Reflection.getClass("java.util.ArrayList");
 * const list = Reflection.newInstance(ArrayList);
 * list.add("diamond");
 *
 * // a method is found by name, and calling it is a separate step rather than a
 * // plain call, because a java Method is not something a script can call
 * const size = Reflection.getDeclaredMethod(ArrayList, "size");
 * print(`the list holds ${Reflection.invokeMethod(size, list)}`);
 *
 * // a field that is not public has to be made accessible before it can be
 * // read, which is the reason to reach for the declared form rather than
 * // the plain one. it is reached on a Minecraft class, which is named by its
 * // intermediary name, so the net.minecraft one
 * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond");
 * const count = Reflection.getDeclaredField(
 *   Reflection.getClass("net.minecraft.world.item.ItemStack") as any, "count");
 * count.setAccessible(true);
 * print(`the private field says ${count.get(stack.getRaw())}, the wrapper says ${stack.getCount()}`);
 * </pre>
 * @author Wagyourtail
 * @since 1.2.3
 */
@Library("Reflection")
@SuppressWarnings("unused")
public class FReflection extends PerExecLibrary {
    private static final Map<String, List<Class<?>>> JAVA_CLASS_CACHE = new HashMap<>();
    public static final CombinedVariableClassLoader classLoader = new CombinedVariableClassLoader(FReflection.class.getClassLoader());

    public FReflection(BaseScriptContext<?> context) {
        super(context);
    }

    /**
     * Resolves a class by its fully qualified name.<br>
     * The nine Java primitive names in lower case are answered directly, so
     * {@code Reflection.getClass("int")} gives the {@code int} class rather than looking for a
     * class actually called {@code int}. Anything else is loaded through this library's own class
     * loader, which is also where {@link #loadJarFile(String) loadJarFile} adds jars, and a name
     * starting with the old {@code xyz.wagyourtail.} package is rewritten to the current one first.
     * <br>
     * The class is initialised as it is found, since that is what {@code Class.forName} does with
     * initialisation on. Nothing is wrapped: what comes back is a plain
     * {@link java.lang.Class Class}, so it is a building block for the calls below rather than
     * something a script calls methods on.<br>
     * There is a second name form for the case where the first name is not found on this build,
     * which is {@link #getClass(String, String) getClass(name, name2)}.
     * example:
     * <pre>
     * // a JDK class, resolved by name
     * const ArrayList = Reflection.getClass("java.util.ArrayList");
     * // the primitive names are answered without a lookup
     * const IntegerClass = Reflection.getClass("int");
     * print(`${IntegerClass.getName()} is a primitive`);
     *
     * // a Minecraft class is named by its intermediary mapped name, so the
     * // net.minecraft one rather than the Mojang one. the shipped typings do
     * // not describe that package, so the cast is for the editor's benefit
     * const BlockState = Reflection.getClass("net.minecraft.world.level.block.state.BlockState") as any;
     * print(`that resolved to ${Reflection.getClassName(BlockState)}`);
     * </pre>
     *
     * @param name name of class like {@code path.to.class}
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
     * Use this to specify a class with intermediary and yarn names of classes for cleaner code. also has support for
     * java primitives by using their name in lower case.
     * <br>
     * The first name is tried first and the second is only used if the first throws
     * {@link ClassNotFoundException}, so this is how one call is written to survive a change of
     * mappings: put the name this build uses first and the other one second. Nothing is gained by
     * putting the older name first, since a name that resolves is used as it is. The second name
     * goes through the plain default class loader rather than this library's own, so it cannot see
     * a jar added by {@link #loadJarFile(String) loadJarFile}, and the old package prefix rewrite
     * applies to it as well.
     * <br>
     * Note that the fallback only covers the first name. If the first name is a Java primitive name
     * it is answered directly and never falls through, and if the first name resolves but the class
     * turns out to be the wrong thing, no second attempt is made.
     * example:
     * <pre>
     * // the first name is tried, and the second is only reached if it is not found,
     * // so this is how one call covers a change of mappings
     * const Block = Reflection.getClass("net.minecraft.world.level.block.Block",
     *   "net.minecraft.block.Block");
     * print(`resolved to ${Reflection.getClassName(Block)}`);
     * // a name that is neither throws rather than returning null
     * </pre>
     *
     * @param name  first try
     * @param name2 second try
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
     * Finds a method the class itself declares, by name and by the exact classes of its parameters.
     * <br>
     * Declared rather than inherited, so a method a parent class has is not found here and the
     * {@link #getMethod(Class, String, Class...) getMethod} form is the one for that. The parameter
     * types have to be given, and they have to be exact: there is no widening and no boxing, so a
     * method taking a {@code double} is not found by passing {@code int.class}. A class with two
     * overloads is told apart by the list, and a method with none is found by passing none.
     * <br>
     * What comes back is a {@link java.lang.reflect.Method Method}, not something callable from a
     * script. {@link #invokeMethod(Method, Object, Object...) invokeMethod} is how it is called.
     * A method that is not public comes back but cannot be called until
     * {@code setAccessible(true)} has been called on it.
     * example:
     * <pre>
     * // the parameter types have to be the exact ones, and a method with no
     * // parameters is found by passing no classes at all
     * const ArrayList = Reflection.getClass("java.util.ArrayList");
     * const size = Reflection.getDeclaredMethod(ArrayList, "size");
     * const list = Reflection.newInstance(ArrayList);
     * print(`the list holds ${Reflection.invokeMethod(size, list)}`);
     * </pre>
     *
     * @param c the class to look the method up on
     * @param name the method's name
     * @param parameterTypes the exact classes of the method's parameters, none for no parameters
     * @return the method, which has to be invoked separately
     * @throws NoSuchMethodException if the class declares no such method
     * @throws SecurityException
     * @see FReflection#getDeclaredMethod(Class, String, String, Class...)
     * @since 1.2.3
     */
    public Method getDeclaredMethod(Class<?> c, String name, Class<?>... parameterTypes) throws NoSuchMethodException, SecurityException {
        return c.getDeclaredMethod(name, parameterTypes);
    }

    /**
     * Use this to specify a method with intermediary and yarn names of classes for cleaner code.
     *
     * @param c
     * @param name
     * @param name2
     * @param parameterTypes
     * @return a {@link java.lang.reflect.Method Method} reference.
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
     * @param c
     * @param name
     * @param name2
     * @param parameterTypes
     * @return
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
     * @param c
     * @param name
     * @param parameterTypes
     * @return
     * @throws NoSuchMethodException
     * @since 1.6.0
     */
    public Method getMethod(Class<?> c, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        return c.getMethod(name, parameterTypes);
    }

    /**
     * @param c
     * @param name
     * @return
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @see FReflection#getDeclaredField(Class, String, String)
     * @since 1.2.3
     */
    public Field getDeclaredField(Class<?> c, String name) throws NoSuchFieldException, SecurityException {
        return c.getDeclaredField(name);
    }

    /**
     * Use this to specify a field with intermediary and yarn names of classes for cleaner code.
     *
     * @param c
     * @param name
     * @param name2
     * @return a {@link java.lang.reflect.Field Field} reference.
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
     * @param c
     * @param name
     * @return
     * @throws NoSuchFieldException
     * @since 1.6.0
     */
    public Field getField(Class<?> c, String name) throws NoSuchFieldException {
        return c.getField(name);
    }

    /**
     * @param c
     * @param name
     * @param name2
     * @return
     * @throws NoSuchFieldException
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
     * Invoke a method on an object with auto type coercion for numbers.
     * <br>
     * The coercion is the reason to use this rather than calling the {@link java.lang.reflect.Method}
     * directly: a number coming out of a script is a double, and a method taking an {@code int},
     * a {@code float}, a {@code long} or any of the other numeric types would reject it. Each
     * argument is converted to whatever the matching parameter type is before the call, so
     * {@code Math.max(1, 2)} and {@code Math.max(1.0, 2.0)} both work. Only numbers are touched,
     * and a method whose parameters are not numeric is called with the arguments as they are.
     * <br>
     * The second argument is the receiver, and a static method is called by passing {@code null}
     * there. This is declared to hand back a plain {@link Object}, so a method returning a
     * primitive comes back boxed on the Java side. In a script that is not a separate thing to
     * handle: a boxed number arrives as an ordinary number, so it can be used as one.
     * <br>
     * A method that is not public cannot be called until {@code setAccessible(true)} has been
     * called on it, and one that throws throws {@link InvocationTargetException} rather than the
     * exception it threw.
     * example:
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
     * @param m       method
     * @param c       object (can be {@code null} for statics)
     * @param objects the arguments, each a number coerced to the matching parameter's type
     * @return whatever the method returns, boxed on the Java side if it returned a primitive
     * @throws IllegalAccessException if the method is not accessible, which it is not until
     *         {@code setAccessible(true)} has been called on it
     * @throws IllegalArgumentException if an argument does not fit the parameter it is passed for
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
     * Attempts to create a new instance of a class. You probably don't have to use this one and can just call {@code
     * new} on a {@link java.lang.Class Class} unless you're in LUA, but then you also have the (kinda poorly
     * documented, can someone find a better docs link for me)
     * <a target="_blank" href= "http://luaj.sourceforge.net/api/3.2/org/luaj/vm2/lib/jse/LuajavaLib.html">LuaJava Library</a>.
     *
     * @param c
     * @param objects
     * @return
     * @since 1.2.7
     */
    public <T> T newInstance(Class<T> c, Object... objects) {
        return newInstance0(c, objects);
    }

    /**
     * the body of {@link #newInstance(Class, Object...) newInstance}, as a static call, so that Java
     * code can reach the same behaviour without a script context to hand it an instance of this
     * library.<br>
     * It is what that method does, exactly: the arguments are first matched to a constructor by
     * their own runtime types, and if that finds nothing, every constructor taking that many
     * arguments is tried in turn with the numbers coerced to the parameter types, and the first one
     * that accepts them is used. That is what lets a script pass the doubles it has for a
     * constructor that takes three {@code double}s.<br>
     * A class whose constructors do not fit the arguments ends in a {@link RuntimeException} saying
     * the parameters were bad, with the original failure as its cause.
     *
     * @param c       the class to construct
     * @param objects the constructor arguments, numbers coerced to the parameter types
     * @return the new instance, already of the class that was asked for
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
     * proxy for extending java classes in the guest language with proper threading support.
     *
     * @param clazz
     * @param interfaces
     * @param <T>
     * @return
     * @since 1.6.0
     */
    public <T> ProxyBuilder<T> createClassProxyBuilder(Class<T> clazz, Class<?>... interfaces) {
        return new ProxyBuilder<>(clazz, interfaces);
    }

    /**
     * @param cName
     * @param clazz
     * @param interfaces
     * @param <T>
     * @return
     * @throws NotFoundException
     * @throws CannotCompileException
     * @since 1.6.5
     */
    public <T> ClassBuilder<T> createClassBuilder(String cName, Class<T> clazz, Class<?>... interfaces) throws NotFoundException, CannotCompileException {
        cName = redirectWagYourTail(cName);

        return new ClassBuilder<>(cName, clazz, interfaces);
    }

    /**
     * @param cName
     * @return
     * @throws ClassNotFoundException
     * @since 1.6.5
     */
    public Class<?> getClassFromClassBuilderResult(String cName) throws ClassNotFoundException {
        return Class.forName("com.jsmacrosce.jsmacros.core.library.impl.classes.proxypackage." + cName.replaceAll("\\.", "\\$"), true, Neighbor.class.getClassLoader());
    }

    /**
     * builds a library class in Java, at run time, and registers it so scripts get a new global
     * variable for it once it is finished.<br>
     * The builder is a {@link ClassBuilder}, so a field and a method are added the same way it adds
     * them, and every step returns the builder or a nested builder for the step. The constructor is
     * the exception: {@link LibraryBuilder#addConstructor()} takes no arguments and works the
     * parameter list out from the {@code perExec} and language settings, so it should not be given
     * any. The name is the name of the global rather than the name of the class, and it is also
     * written onto the generated class as the {@link Library} value by the builder itself.<br>
     * A per-exec library is built once per script run and so is the right shape for one that holds
     * on to the run's context; a once-only library is built once for the profile and shared by
     * every script. The accepted languages narrow which guest languages the library is available
     * to, and none at all means all of them.
     * <br>
     * Nothing exists for a script until {@link LibraryBuilder#finishBuildAndFreeze()} has been
     * called, and that call can only be made once, since it freezes the class. The class it freezes
     * extends {@link BaseLibrary}, or one of the per exec and per language forms of it, which is
     * the same rule {@link #createLibrary(String, String) createLibrary} states.
     *
     * @param name         the name of the global to create, also used as the generated class's name
     * @param perExec      whether the library is built per script run rather than once per profile
     * @param acceptedLangs the languages to make it available to, none for all of them
     * @return a builder the class is assembled on
     * @throws NotFoundException if a name could not be resolved while building
     * @throws CannotCompileException if the assembled class does not compile
     */
    public LibraryBuilder createLibraryBuilder(String name, boolean perExec, String... acceptedLangs) throws NotFoundException, CannotCompileException {
        return new LibraryBuilder(runner, name, perExec, acceptedLangs);
    }

    /**
     * A library class always has a {@link Library} annotation containing the name of the library,
     * which may differ from the actual class name. A library class must also extend
     * {@link BaseLibrary} in some way, either directly or through
     * {@link PerExecLibrary PerExecLibrary},
     * {@link com.jsmacrosce.jsmacros.core.library.PerExecLanguageLibrary PerExecLanguageLibrary}
     * or {@link com.jsmacrosce.jsmacros.core.library.PerLanguageLibrary PerLanguageLibrary}.
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
     * A Java Development Kit (JDK) must be installed (and potentially used to start Minecraft) in
     * order to compile whole classes.
     * <p>
     * Compiled classes can't be accessed from any guest language, but must be either stored through
     * {@link FGlobalVars#putObject(String, Object)} or retrieved from this library. Unlike normal
     * hot swapping, already created instances of the class will not be updated. Thus, it's
     * important to know which version of the class you're using when instantiating it.<br>
     * Compiling the same name twice does not replace anything: the second call is a second,
     * separate class of the same name, and both are kept, oldest first. Which one a script has in
     * hand is therefore the only thing that decides what it is, and that is why the two getters
     * exist at all rather than the class just being looked up by name.
     * example:
     * <pre>
     * // compiling the same name again gives a second, separate class rather than
     * // replacing the first one
     * const first = Reflection.compileJavaClass("com.example.Counter",
     *   "package com.example; public class Counter { public int n = 0; }");
     * const second = Reflection.compileJavaClass("com.example.Counter",
     *   "package com.example; public class Counter { public int n = 100; }");
     * const versions = Reflection.getAllCompiledJavaClassVersions("com.example.Counter");
     * print(`that name now has ${versions.size()} compiled versions`);
     *
     * // the compiled class cannot be looked up by name, so keep the reference and
     * // hand instances to anything that takes a raw java object
     * const instance = Reflection.newInstance(first);
     * GlobalVars.putObject("counter", instance);
     * // getCompiledJavaClass gives the most recent compilation of that name
     * print(`and the newest is the second one: ${Reflection.getCompiledJavaClass("com.example.Counter") === second}`);
     * </pre>
     *
     * @param className the fully qualified name of the class, including the package
     * @param code      the java code to compile
     * @return the compiled class, which is a new class even if the name was compiled before
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
     * @param className the fully qualified name of the class, including the package
     * @return the latest compiled class or {@code null} if it doesn't exist.
     * @since 1.8.4
     */
    public Class<?> getCompiledJavaClass(String className) {
        className = redirectWagYourTail(className);

        List<Class<?>> versions = JAVA_CLASS_CACHE.get(className);
        return versions == null ? null : versions.get(versions.size() - 1);
    }

    /**
     * @param className the fully qualified name of the class, including the package
     * @return all compiled versions of the class, in order of compilation.
     * @since 1.8.4
     */
    public List<Class<?>> getAllCompiledJavaClassVersions(String className) {
        className = redirectWagYourTail(className);
        List<Class<?>> versions = JAVA_CLASS_CACHE.get(className);
        return versions == null ? Collections.emptyList() : ImmutableList.copyOf(versions);
    }

    /**
     * See <a href="https://github.com/jOOQ/jOOR">jOOR Github</a> for more information.
     * <br>
     * The wrapper it returns is a different style from the rest of this library rather than a
     * different capability: the calls chain off each other and the last one in a chain gives the
     * value back, so a field is reached with {@code field("name")} and read with
     * {@code get()}, and a method with {@code call("name", args)} and read the same way. A field
     * reached this way is made accessible as it is found, so there is no {@code setAccessible} step
     * to remember.<br>
     * It is worth reaching for when a chain of two or three calls reads better than the equivalent
     * sequence of {@link #getDeclaredMethod(Class, String, Class...) getDeclaredMethod} and
     * {@link #invokeMethod(Method, Object, Object...) invokeMethod} calls. Note that the coercion
     * this library does for numbers is not part of it, so a method called through the wrapper
     * still has to be given arguments of the right type.
     * example:
     * <pre>
     * // the wrapper chains, and the last call in a chain gives the value back
     * const stack = Client.getRegistryManager().getItemStack("minecraft:diamond");
     * const reflected = Reflection.getReflect(stack.getRaw());
     * print(`calling through it: ${reflected.call("getCount")}`);
     * // a private field is reachable without a setAccessible call first
     * print(`the private count field says ${reflected.field("count").get()}`);
     * </pre>
     *
     * @param obj the object to wrap
     * @return a wrapper for the passed object to do help with java reflection.
     * @since 1.8.4
     */
    public Reflect getReflect(Object obj) {
        return Reflect.on(obj);
    }

    /**
     * Loads a jar file to be accessible with this library.
     * <br>
     * The jar's classes then resolve through {@link #getClass(String) getClass} and
     * {@link #compileJavaClass(String, String) compileJavaClass} without anything else, since they
     * all go through this library's own class loader. A jar that has already been loaded is not
     * loaded twice, and the return value says so: {@code true} for a jar that was added and also
     * {@code true} for one that was already there, so it cannot be used to tell the two apart.
     * A path that does not exist is a {@link java.io.FileNotFoundException FileNotFoundException}
     * rather than a {@code false}.
     * example:
     * <pre>
     * // the path is relative to the script's folder, not to the game's working
     * // directory, and loading the same jar twice is not an error
     * const first = Reflection.loadJarFile("libs/mylib.jar");
     * const second = Reflection.loadJarFile("libs/mylib.jar");
     * print(`loaded ${first}, and again ${second}`);
     *
     * // the classes in it are now resolvable by name like any other
     * const MyThing = Reflection.getClass("com.example.MyThing");
     * print(`that is ${Reflection.getClassName(MyThing)}`);
     * </pre>
     *
     * @param file relative to the script's folder.
     * @return success value, which is also true for a jar that was already loaded
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
     * the fully qualified name of a class, or of the class an object actually is.
     * <br>
     * A {@link java.lang.Class Class} passed in names itself, and anything else names the class
     * its runtime type is, so a subclass instance reports the subclass rather than the type it was
     * declared as. The name is the canonical one, with dots rather than slashes and with nested
     * classes joined by a dot, and it comes back {@code null} for the two cases a canonical name
     * has none for: an anonymous class and a local class. An array is not one of those and comes
     * back in the array form, such as {@code java.lang.String[]}, and a primitive comes back as the
     * primitive's own name.
     * example:
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
     * @param o class you want the name of
     * @return the fully qualified class name (with "."'s not "/"'s)
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
     * Replaces the package prefix of a given string from "xyz.wagyourtail." to "com.jsmacrosce.".
     *
     * @param name the fully qualified name or a string containing "xyz.wagyourtail." to be replaced
     * @return a new string with the package prefix "xyz.wagyourtail." replaced by "com.jsmacrosce."
     */
    @DocletIgnore
    public static String redirectWagYourTail(String name) {
        return name.replace("xyz.wagyourtail.", "com.jsmacrosce.");
    }

    /**
     * I know this is probably bad practice, but lets be real, this whole library is bad practice, So I can make it
     * worse, right? at least this should work better than {@code try/catch}'ing using
     * {@link ClassLoader#loadClass(String)} to search through every {@link URLClassLoader} that
     * {@link FReflection#loadJarFile(String)} would make, or how I was previously doing it by pre-loading and caching
     * all the classes to a {@link Map}
     * <p>
     * This class is a modification to
     * <a target="_blank" href="https://www.source-code.biz/snippets/java/12.htm">Christian d'Heureuse's JoinClassLoader</a>, under the
     * <a target="_blank" href="https://www.apache.org/licenses/LICENSE-2.0">Apache-2.0 license</a> to change it from a Class array to a
     * {@link Set}, to allow for modifications to the {@link ClassLoader ClassLoaders} contained in the classLoader.
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
