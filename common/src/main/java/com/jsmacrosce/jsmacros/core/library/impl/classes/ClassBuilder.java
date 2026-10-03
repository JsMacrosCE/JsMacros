package com.jsmacrosce.jsmacros.core.library.impl.classes;

import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.*;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.library.impl.classes.proxypackage.Neighbor;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * assembles a Java class at run time, so a script can write one in Java and then use it.<br>
 * A script gets one from {@code Reflection.createClassBuilder}, which names the class, the class it
 * extends and any interfaces it implements, and then members are added until
 * {@link #finishBuildAndFreeze() finishBuildAndFreeze} makes it. What comes back is a real
 * {@link Class}, so an instance is made from it the ordinary way and every method on the class it
 * extends behaves as that class does.<br>
 * The class is generated inside JsMacros' own package, under the name that was given with its dots
 * turned into dollar signs, and it is defined next to an empty placeholder class so that it can be
 * a subclass of anything at all. The name is what {@code Reflection.getClassFromClassBuilderResult}
 * looks it up by afterwards. A class is frozen by the build, so it can only be finished once.
 * <br>
 * There are two ways to add a member, and they mix freely. The source forms,
 * {@link #addField(String) addField(String)}, {@link #addMethod(String) addMethod(String)} and
 * {@link #addConstructor(String) addConstructor(String)}, take one declaration written as Java
 * source and are the shortest way to add something. The nested builders, reached through
 * {@link #addField(java.lang.Class, java.lang.String) addField(Class, String)},
 * {@link #addMethod(java.lang.Class, java.lang.String, java.lang.Class[]) addMethod(Class, String, Class...)}
 * and {@link #addConstructor(java.lang.Class[]) addConstructor(Class...)}, take the types as class
 * objects and give finer control over visibility, exceptions and annotations, and they are the only
 * way to have a generated method call back into the script that built it.<br>
 * That callback is the reason the nested builders are here. A method added with a source body runs
 * its body inside the generated class, where a script has nothing to call. A method added with
 * {@link MethodBuilder#guestBody guestBody} or {@link MethodBuilder#buildBody() buildBody} instead
 * gets a body that looks a handler up in {@link #methodWrappers} and calls it, so the work happens
 * in the script that is still running. The key is worked out from the class name, the method name
 * and the method's JVM descriptor, which is why the types have to be given as class objects rather
 * than written into a source string.<br>
 * Nothing here wraps a compile failure: a declaration that does not compile, or a type that
 * cannot be resolved, is a {@link CannotCompileException} or a {@link NotFoundException} out of
 * the call that caused it, and the half built class is left as it was rather than being built
 * anyway. A handler that returns the wrong kind of value for the method it is standing in for is
 * not caught at all, and fails where the method is called.<br>
 * One thing only shows up in an editor: the type object forms need the types as class objects, and
 * the shipped TypeScript definitions only describe a part of the JDK, so a name under
 * {@code net.minecraft} or a class that is not described comes back from
 * {@code Reflection.getClass} as {@code unknown} rather than as a class. It still resolves at run
 * time, so such a name only has to be cast where it is passed in, which is what the
 * {@link #addAnnotation(java.lang.Class) addAnnotation} example shows.<br>
 * One thing to know about the examples below: each of the ones on a member is shown on a
 * {@code builder} made the way the one at the top of this class is, on a class extending
 * {@code java.util.ArrayList} that already has a {@code String} field named {@code label} on it.
 * That keeps them to the one call being documented.
 * example:
 * <pre class="language-typescript">
 * // a class generated at run time, extending one that already exists
 * const builder = Reflection.createClassBuilder("Counting",
 *   Reflection.getClass("java.util.ArrayList"));
 *
 * // members can be written as java source, one declaration at a time
 * builder.addField("private int total;");
 * builder.addMethod("public int getTotal() { return this.total; }");
 *
 * // or assembled through the nested builder, which takes the types as class
 * // objects and is the only way to have a method call back into this script
 * const add = builder.addMethod(Reflection.getClass("boolean"), "addAndReport",
 *   Reflection.getClass("java.lang.Object"));
 * add.makePublic();
 * add.guestBody(JavaWrapper.methodToJava(function (self, args) {
 *   // `self` is the generated instance and `args` is the arguments as an
 *   // array, with a primitive boxed. the return value is unwrapped to the
 *   // method's own return type
 *   print(`the generated method was handed ${args.length} argument(s)`);
 *   return self.add(args[0]);
 * }));
 *
 * // the mixed form is for a method that needs java around the call. a guest
 * // call is a statement rather than an expression, so it needs somewhere to
 * // put what comes back
 * const bump = builder.addMethod(Reflection.getClass("void"), "bump");
 * bump.makePublic();
 * bump.buildBody()
 *   .appendJavaCode("int before = this.total")
 *   .appendGuestCode(JavaWrapper.methodToJava(function (self, args) {
 *     return 1;
 *   }), "", "Object step = ")
 *   .appendJavaCode("this.total = before + ((java.lang.Integer) step).intValue()")
 *   .finish();
 *
 * // this is what freezes it, and it can only be called once
 * const CountingClass = builder.finishBuildAndFreeze();
 *
 * // the built class is typed as whatever it extends, so a method it added has
 * // to be reached through a cast
 * const counting = Reflection.newInstance(CountingClass) as any;
 * print(counting.getTotal());
 * print(counting.addAndReport("diamond"));
 * counting.bump();
 * print(counting.getTotal());
 * </pre>
 * @param <T> the type of the class being extended, which is what the built class comes back as
 * @since 1.6.5
 */
@SuppressWarnings("unused")
public class ClassBuilder<T> {
    /**
     * the handlers that generated methods call back into, keyed by a name worked out from the
     * class, the method and the method's JVM descriptor.<br>
     * A method added through a {@code guestBody} or a {@code buildBody} gets a body that reads this
     * map and calls whatever it finds, so this is what makes a generated class able to run script
     * code. The key is built here and written into the generated bytecode rather than passed
     * along, which is why the method's types have to be known exactly when it is added. A
     * {@code BodyBuilder} with more than one guest call in it numbers them, so several calls in one
     * method do not collide.<br>
     * Nothing is ever removed from it, and it is shared by every class any script builds, so the
     * handlers behind a generated method stay reachable for as long as the game runs. The field is
     * public and final, but reading it is of no use to a script: the keys are worked out
     * internally and a handler is called by the generated code rather than looked up by hand.
     */
    public static final Map<String, MethodWrapper<Object, Object, Object, ?>> methodWrappers = new ConcurrentHashMap<>();
    ClassPool defaultPool = ClassPool.getDefault();
    /**
     * the class being assembled, as the bytecode library's own handle on it.<br>
     * This is the thing every method on this class adds to, and reading it is the way to reach
     * the parts of the underlying library that are not wrapped here, such as adding an interface
     * after the fact or removing a member. It belongs to the bytecode library rather than to
     * JsMacros, so a script that reaches through it is outside the API this class documents, and
     * it stops being usable once {@link #finishBuildAndFreeze() finishBuildAndFreeze} has frozen
     * the class.
     */
    public final CtClass ctClass;
    private final String className;
    private final AnnotationsAttribute classAnnotations;
    private final AnnotationsAttribute invisibleClassAnnotations;

    /**
     * starts a class, under the name given, extending the class given and implementing whatever
     * interfaces are listed.<br>
     * The name is the name the class is generated under, with its dots turned into dollar signs,
     * inside JsMacros' own package, and it is what
     * {@code Reflection.getClassFromClassBuilderResult} looks the finished class up by. It does
     * not have to match a real Java class and does not have to be a legal identifier, since it
     * never becomes a source name.<br>
     * The class and the interfaces are ordinary class objects, so a script gets them through
     * {@code Reflection.getClass}. The class extended is looked up in the bytecode library's own
     * pool, so a class it cannot find is a {@link NotFoundException} from here rather than
     * something that fails later.
     * example:
     * <pre>
     * // extending a concrete class, with the shipped typings typing the result
     * // as that class rather than as the one being built
     * const builder = Reflection.createClassBuilder("Counting",
     *   Reflection.getClass("java.util.ArrayList"));
     * builder.addField("private int total;");
     * </pre>
     *
     * @param name       the name to generate the class under
     * @param parent     the class to extend
     * @param interfaces any interfaces to implement
     * @throws NotFoundException      if the class or an interface could not be resolved
     * @throws CannotCompileException if the empty class could not be made
     */
    public ClassBuilder(String name, Class<T> parent, Class<?>... interfaces) throws NotFoundException, CannotCompileException {
        className = name.replaceAll("\\.", "\\$");
        ctClass = defaultPool.makeClass("com.jsmacrosce.jsmacros.core.library.impl.classes.proxypackage." + className);
        ctClass.setSuperclass(defaultPool.getCtClass(parent.getName()));
        for (Class<?> i : interfaces) {
            ctClass.addInterface(defaultPool.getCtClass(i.getName()));
        }
        classAnnotations = new AnnotationsAttribute(ctClass.getClassFile().getConstPool(), AnnotationsAttribute.visibleTag);
        invisibleClassAnnotations = new AnnotationsAttribute(ctClass.getClassFile().getConstPool(), AnnotationsAttribute.invisibleTag);
    }

    /**
     * starts a field of the type and name given, to be finished through the returned builder
     * rather than added straight away.<br>
     * This is the type object form, and it is the form to use when a field needs something the
     * source form cannot express: a visibility or modifier set through the builder's
     * {@code makePublic} and {@code toggleStatic} calls, an annotation, or an initial value.
     * Nothing is added to the class until {@link FieldBuilder#end() end} is called on the returned
     * builder, so a field can be set up in steps and then committed.<br>
     * A field added this way carries no generic signature, since the type is given as a class
     * object, and an initial value has to be set through
     * {@link FieldBuilder#initializer() initializer}.
     * example:
     * <pre>
     * builder.addField(Reflection.getClass("java.lang.String"), "label")
     *   .makePrivate()
     *   .toggleFinal()
     *   .initializer().setString("unnamed")
     *   .end();
     * </pre>
     *
     * @param fieldType the type of the field
     * @param name      the name of the field
     * @return a builder for the field, which is added to the class when it is ended
     * @throws NotFoundException if the type could not be resolved
     */
    public FieldBuilder addField(Class<?> fieldType, String name) throws NotFoundException {
        return new FieldBuilder(defaultPool.getCtClass(fieldType.getName()), name);
    }

    /**
     * The code must define the full field, including visibility, type, name and an optional value.
     * Generic types are not supported and must be explicitly cast in the source code when used.
     * Annotations are also not supported.
     * Just like in java, classes from the `java.lang` package don't need a fully qualified name.
     * Examples are:
     * <pre>
     * {@code private String name;}
     * {@code private java.lang.String name;}
     * {@code public java.util.List list = new java.util.ArrayList();}
     * {@code static int value = 10;}
     * </pre>
     *
     * @param code the code for the field
     * @return self for chaining.
     * @throws CannotCompileException
     * @since 1.8.4
     */
    public ClassBuilder<T> addField(String code) throws CannotCompileException {
        CtField field = CtField.make(code, ctClass);
        ctClass.addField(field);
        return this;
    }

    /**
     * starts a method with the return type, name and parameter types given, to be finished
     * through the returned builder rather than added straight away.<br>
     * This is the type object form, and it is the one to reach for when the method has to do
     * something the source form cannot: declare exceptions, carry an annotation, be abstract, be
     * given its body in steps, or call back into the script that is building the class. Nothing is
     * added to the class until one of the builder's finishing calls is made, so a method can be
     * set up in steps and then committed.<br>
     * The body a builder is given is a source string or a handler, not a value, so a method
     * always ends up with a body unless it is ended abstract. There is no way to declare a
     * generic method this way, and no way to declare a vararg, since the parameter types are the
     * ones the method really has.
     * example:
     * <pre>
     * const build = builder.addMethod(Reflection.getClass("java.lang.String"), "describe",
     *   Reflection.getClass("java.lang.Object"));
     * build.makePublic();
     * build.body("return \"an object: \" + $1;");
     *
     * // the same method, but abstract, which is what an interface needs
     * const required = builder.addMethod(Reflection.getClass("void"), "reset");
     * required.makePublic();
     * required.endAbstract();
     * </pre>
     *
     * @param returnType the type the method returns
     * @param name       the name of the method
     * @param params     the parameter types, in order
     * @return a builder for the method, which is added to the class when it is finished
     * @throws NotFoundException if a type could not be resolved
     */
    public MethodBuilder addMethod(Class<?> returnType, String name, Class<?>... params) throws NotFoundException {
        CtClass[] paramCtClasses = new CtClass[params.length];
        for (int i = 0; i < params.length; i++) {
            paramCtClasses[i] = defaultPool.getCtClass(params[i].getName());
        }
        return new MethodBuilder(defaultPool.getCtClass(returnType.getName()), name, paramCtClasses);
    }

    /**
     * The code must define the full method, including visibility, return type, name and parameters.
     * Generic types are not supported as return values or arguments, neither can varargs be used.
     * Annotations are also not supported.
     * Just like in java, classes from the `java.lang` package don't need a fully qualified name.
     * Examples are:
     * <pre>
     * {@code public Object id(Object obj) { return obj; }}
     * {@code private void print(String text) { System.out.println(text); }}
     * {@code private static java.util.Map toMap(Object[] keys, Object[] values) {
     *      java.util.Map map = new java.util.HashMap();
     *      for (int i = 0; i < keys.length; i++) {
     *          map.put(keys[i], values[i]);
     *      }
     *      return map;
     *  }}
     * {@code public String toString() {
     *      System.out.println(super.toString());
     *      return "Hello World!";
     *  }}
     * </pre>
     *
     * @param code the code for the method
     * @return self for chaining.
     * @throws CannotCompileException
     * @since 1.8.4
     */
    public ClassBuilder<T> addMethod(String code) throws CannotCompileException {
        CtMethod method = CtNewMethod.make(code, ctClass);
        ctClass.addMethod(method);
        return this;
    }

    /**
     * starts a constructor with the parameter types given, to be finished through the returned
     * builder rather than added straight away.<br>
     * This is the type object form, and it is the way to give a generated class a constructor that
     * takes arguments, which the source form can also do but only by writing the whole
     * declaration. Nothing is added to the class until one of the builder's finishing calls is
     * made. A body given to a constructor with parameters has to call the superclass itself,
     * since there is nothing to pass on for it automatically, and that is what
     * {@code super($1, $2)} in the body is for.<br>
     * {@link #addClinit() addClinit} is meant to be the static counterpart, a class initialiser
     * taking no parameters that runs when the class is first touched rather than when it is
     * made, but it does not currently work, so a class initialiser is not reachable from here.
     * example:
     * <pre>
     * const init = builder.addConstructor(Reflection.getClass("java.lang.String"));
     * init.makePublic();
     * init.body('super($1); this.label = $1;');
     * </pre>
     *
     * @param params the parameter types, in order
     * @return a builder for the constructor, which is added to the class when it is finished
     * @throws NotFoundException if a type could not be resolved
     */
    public ConstructorBuilder addConstructor(Class<?>... params) throws NotFoundException {
        CtClass[] paramCtClasses = new CtClass[params.length];
        for (int i = 0; i < params.length; i++) {
            paramCtClasses[i] = defaultPool.getCtClass(params[i].getName());
        }
        return new ConstructorBuilder(paramCtClasses, false);
    }

    /**
     * The code must define the full constructor, including visibility and parameters.
     * Generic types are not supported as arguments, neither can varargs be used.
     * Annotations are also not supported.
     * Just like in java, classes from the `java.lang` package don't need a fully qualified name.
     * To make sure the class can be easily instantiated, the visibility of the constructor should be public.
     * Examples are:
     * <pre>
     * {@code public MyClass() { }}
     * {@code public MyClass(String text) { System.out.println(text); }}
     * {@code protected MyClass(String text, int number) { super(text, number, ""); }}
     * {@code public MyClass(String text, int number, String other) {
     *      this(text, number);
     *      this.other = other;
     * }}
     * </pre>
     *
     * @param code the code for the constructor
     * @return self for chaining.
     * @throws CannotCompileException
     * @since 1.8.4
     */
    public ClassBuilder<T> addConstructor(String code) throws CannotCompileException {
        CtConstructor constructor = CtNewConstructor.make(code, ctClass);
        ctClass.addConstructor(constructor);
        return this;
    }

    /**
     * starts a class initialiser, the block of code that runs once when the class is first
     * touched rather than when an instance is made.<br>
     * It does not work, so a script must not call it, and there is no example here for that
     * reason. The builder it returns does name its method {@code &lt;clinit&gt;} and does set
     * the static flag a class initialiser needs, but every body call on it goes through the
     * bytecode library's {@code CtConstructor}, and that type hard-codes the method name to
     * {@code &lt;init&gt;} whatever it was asked for. The class is therefore given a method
     * called {@code &lt;init&gt;} carrying the static flag, and a class file may not have that,
     * so {@link #finishBuildAndFreeze() finishBuildAndFreeze} fails with a
     * {@link java.lang.ClassFormatError ClassFormatError} about illegal modifiers on
     * {@code &lt;init&gt;}. That is an {@code Error} and not one of the two checked exceptions
     * the call declares, so it comes out of the call unwrapped rather than as a
     * {@code CannotCompileException}, and a script that handles a
     * {@code CannotCompileException} does not catch it. No call on the builder produces a
     * real {@code &lt;clinit&gt;}.<br>
     * The body is not the way round it either, and fails before that point: a body on a
     * constructor is the inside of one, so it needs the braces a constructor body needs, and
     * anything unbraced is a {@code CannotCompileException} from
     * {@link ConstructorBuilder#body(java.lang.String) body}.<br>
     * Nothing is lost by it not working. A static field's value is written into the class
     * initialiser by the bytecode library itself, out of the
     * {@link FieldBuilder#initializer() initializer} the field was given, so
     * {@link FieldBuilder#end() ending} the field is all a static field with a value on it needs.
     * A class initialiser that is more than that would have to be made through the bytecode
     * library's own class initialiser and added through its own method call, which this class
     * does not expose, so that is a change to the code rather than something a script has today.
     *
     * @return a builder for a class initialiser, which does not currently produce one
     */
    public ConstructorBuilder addClinit() {
        return new ConstructorBuilder(new CtClass[0], true);
    }

    /**
     * starts an annotation on the class itself, to be filled in and committed through the
     * returned builder.
     * <br>
     * This is the form to use for an annotation whose values have to be worked out rather than
     * written, and the one that works for a type this class cannot otherwise see in source. The
     * annotation type is a class object, and its {@link Retention} is what decides whether it goes
     * on as one the runtime can see or as one only the reflection library can, so the retention on
     * the annotation type is read rather than chosen.<br>
     * Nothing is on the class until {@link AnnotationBuilder#finish() finish} is called, and the
     * member it was reached from is what comes back, so the calls chain.
     * example:
     * <pre class="language-typescript">
     * // an annotation with nothing on it. the cast is for the editor's
     * // benefit: the shipped typings do not describe the annotation types
     * builder.addAnnotation(Reflection.getClass("java.lang.FunctionalInterface") as any)
     *   .finish();
     *
     * // one with a value, which takes two finishes: the first closes the
     * // array, the second puts the annotation on the class
     * builder.addAnnotation(Reflection.getClass("java.lang.SuppressWarnings") as any)
     *   .putArray("value")
     *   .putString("unchecked")
     *   .finish()
     *   .finish();
     * </pre>
     *
     * @param type the annotation type to put on the class
     * @return a builder for the annotation, which is put on the class when it is finished
     * @throws NotFoundException if the annotation type could not be resolved
     */
    public AnnotationBuilder<ClassBuilder<T>> addAnnotation(Class<?> type) throws NotFoundException {
        Annotation annotation = new Annotation(ctClass.getClassFile().getConstPool(), defaultPool.getCtClass(type.getName()));
        return new AnnotationBuilder<>(annotation, ctClass.getClassFile().getConstPool(), this, type.getAnnotation(Retention.class).value() != RetentionPolicy.RUNTIME ? invisibleClassAnnotations : classAnnotations);
    }

    /**
     * one field of the class being built, set up in steps and added when it is ended.<br>
     * A script does not make one of these; it is what
     * {@link #addField(java.lang.Class, java.lang.String) addField(Class, String)} hands back. The
     * type is fixed by the call that started it and the name can be changed with
     * {@link #rename(String) rename}, so the point of this builder is the steps in between: the
     * visibility, the modifiers, an annotation, and an initial value.<br>
     * Nothing is on the class until {@link #end() end}, which is what commits it and hands back
     * the class builder. That ordering is what lets a field be given a value through
     * {@link #initializer() initializer}, since the value is only applied at the end.
     * example:
     * <pre>
     * builder.addField(Reflection.getClass("java.lang.String"), "label")
     *   .makePrivate()
     *   .toggleFinal()
     *   .initializer().setString("unnamed")
     *   .end();
     * </pre>
     */
    public class FieldBuilder {
        private final CtClass fieldType;
        private String fieldName;
        private int fieldMods = 0;
        private final AnnotationsAttribute fieldAnnotations = new AnnotationsAttribute(ctClass.getClassFile().getConstPool(), AnnotationsAttribute.visibleTag);
        private final AnnotationsAttribute invisibleFieldAnnotations = new AnnotationsAttribute(ctClass.getClassFile().getConstPool(), AnnotationsAttribute.invisibleTag);
        /**
         * the value the field starts out with, or {@code null} for a field with no value on it,
         * which is the usual case.<br>
         * It is set through {@link #initializer() initializer} rather than assigned directly, and
         * it is only applied when the field is {@link #end() ended}, so a value set on a builder
         * that is never ended goes nowhere. The underlying bytecode library's own type, so a script
         * has no reason to touch it except to read back what was set.
         */
        public CtField.Initializer fieldInitializer;

        /**
         * starts a field of the type and name given. A script does not call this; it is what
         * {@link #addField(java.lang.Class, java.lang.String) addField(Class, String)} calls.
         *
         * @param fieldType the type of the field
         * @param name      the name of the field
         */
        public FieldBuilder(CtClass fieldType, String name) {
            this.fieldType = fieldType;
            this.fieldName = name;
        }

        /**
         * adds the field to the class straight away, with no modifiers, no annotations and no
         * initial value, and hands back the class builder.
         * <br>
         * This is the shortcut end of this builder, for a field that needs nothing set on it. The
         * argument is not used: the field is added as the type and name this builder was started
         * with, whatever source is passed in. Anything set on the builder before this call, the
         * visibility, the modifiers, an annotation or an initial value, is not applied here, so
         * this and {@link #end() end} are not interchangeable and calling the wrong one silently
         * drops what was set. {@code end} is the one to reach for.
         * example:
         * <pre>
         * // a plain package private field with nothing on it, since the argument
         * // is not read
         * builder.addField(Reflection.getClass("int"), "calls").compile("ignored");
         * </pre>
         *
         * @param code not used, the field is added as this builder was started
         * @return the class builder, so more members can be added
         * @throws CannotCompileException if the field could not be added
         */
        public ClassBuilder<T> compile(String code) throws CannotCompileException {
            ctClass.addField(CtField.make(fieldType.getName() + " " + fieldName + ";", ctClass));
            return ClassBuilder.this;
        }

        /**
         * changes the name the field will be added under. Nothing is added until
         * {@link #end() end}, so this is free to call as often as wanted and the last one wins.
         *
         * @param name the name to add the field under
         * @return self
         */
        public FieldBuilder rename(String name) {
            this.fieldName = name;
            return this;
        }

        /**
         * makes the field private, leaving the static and final flags as they were. Since a
         * field starts with no flags at all, this has to come before
         * {@link #toggleStatic() toggleStatic} and {@link #toggleFinal() toggleFinal} to keep them,
         * and the other three visibility calls do the same thing, so the last one called is the
         * one that applies.
         *
         * @return self
         */
        public FieldBuilder makePrivate() {
            this.fieldMods = (Modifier.PRIVATE) | (this.fieldMods & Modifier.STATIC) | (this.fieldMods & Modifier.FINAL);
            return this;
        }

        /**
         * makes the field public, on the same terms as {@link #makePrivate() makePrivate}: the
         * static and final flags are kept and this replaces any other visibility already set.
         *
         * @return self
         */
        public FieldBuilder makePublic() {
            this.fieldMods = (Modifier.PUBLIC) | (this.fieldMods & Modifier.STATIC) | (this.fieldMods & Modifier.FINAL);
            return this;
        }

        /**
         * makes the field protected, on the same terms as {@link #makePrivate() makePrivate}: the
         * static and final flags are kept and this replaces any other visibility already set.
         *
         * @return self
         */
        public FieldBuilder makeProtected() {
            this.fieldMods = (Modifier.PROTECTED) | (this.fieldMods & Modifier.STATIC) | (this.fieldMods & Modifier.FINAL);
            return this;
        }

        /**
         * makes the field package private, which is what a field with no visibility call is
         * already, so this is the way back to that after one of the other three. The static and
         * final flags are kept.
         *
         * @return self
         */
        public FieldBuilder makePackagePrivate() {
            this.fieldMods = (this.fieldMods & Modifier.STATIC) | (this.fieldMods & Modifier.FINAL);
            return this;
        }

        /**
         * flips the static flag, so calling it twice is the same as never calling it. A static
         * field belongs to the class rather than to an instance, so it has to be given its value
         * through a class initialiser, as {@link #addClinit() addClinit} builds, rather than
         * through a constructor.
         * example:
         * <pre>
         * // one field, not static, then the same builder made static
         * builder.addField(Reflection.getClass("int"), "calls")
         *   .makePrivate()
         *   .toggleStatic()
         *   .end();
         * </pre>
         *
         * @return self
         */
        public FieldBuilder toggleStatic() {
            this.fieldMods ^= Modifier.STATIC;
            return this;
        }

        /**
         * flips the final flag, so calling it twice is the same as never calling it. A final
         * field cannot be assigned after it is initialised, so a value on one has to be given
         * through {@link #initializer() initializer} or it has to be left unset.
         *
         * @return self
         */
        public FieldBuilder toggleFinal() {
            this.fieldMods ^= Modifier.FINAL;
            return this;
        }

        /**
         * the modifiers as the number Java's {@link Modifier} class uses for them, which is what
         * the bytecode holds and what {@link #getModString() getModString} turns into words.
         * <br>
         * A field with nothing set on it answers zero. Reading this is only useful for checking
         * what the other calls on this builder ended up doing, since a number of this kind says
         * nothing readable on its own.
         *
         * @return the modifier bits set on this field
         */
        public int getMods() {
            return fieldMods;
        }

        /**
         * the modifiers as words, such as {@code "public static final"}, which is the readable
         * form of {@link #getMods() getMods}. A field with nothing set on it answers
         * {@code "package-private"}.
         * example:
         * <pre>
         * const field = builder.addField(Reflection.getClass("int"), "calls")
         *   .makePrivate()
         *   .toggleStatic();
         * print(field.getModString());
         * field.end();
         * </pre>
         *
         * @return the modifiers in words
         */
        public String getModString() {
            return Modifier.toString(fieldMods);
        }

        /**
         * starts an annotation on this field, to be filled in and committed through the returned
         * builder. It works the same way as
         * {@link ClassBuilder#addAnnotation(java.lang.Class) addAnnotation} does for the class, and
         * what the annotation's retention says decides whether it goes on as one the runtime can
         * see or as one only the reflection library can.
         * example:
         * <pre class="language-typescript">
         * builder.addField(Reflection.getClass("int"), "calls")
         *   .addAnnotation(Reflection.getClass("java.lang.Deprecated") as any)
         *   .finish()
         *   .end();
         * </pre>
         *
         * @param type the annotation type to put on the field
         * @return a builder for the annotation, which is put on the field when it is finished
         * @throws NotFoundException if the annotation type could not be resolved
         */
        public AnnotationBuilder<FieldBuilder> addAnnotation(Class<?> type) throws NotFoundException {
            Annotation annotation = new Annotation(ctClass.getClassFile().getConstPool(), defaultPool.getCtClass(type.getName()));
            return new AnnotationBuilder<>(annotation, ctClass.getClassFile().getConstPool(), this, type.getAnnotation(Retention.class).value() != RetentionPolicy.RUNTIME ? invisibleFieldAnnotations : fieldAnnotations);
        }

        /**
         * starts an initial value for this field, to be set through the returned builder.
         * <br>
         * The value is only applied when the field is {@link #end() ended}, and only the last one
         * set survives, so a field can be given a value and then have it replaced before it is
         * added. There is one call per kind of value, and one for an expression or a call rather
         * than a constant, which is how a field gets a value that has to be worked out.
         * example:
         * <pre>
         * // a constant of the field's own type
         * builder.addField(Reflection.getClass("int"), "limit").initializer().setInt(10).end();
         *
         * // or an expression, which is java source rather than a value
         * builder.addField(Reflection.getClass("int"), "doubleLimit")
         *   .initializer().compile("3 + 4")
         *   .end();
         *
         * // or a call, with the arguments as java source
         * builder.addField(Reflection.getClass("int"), "parsed")
         *   .initializer().callStaticMethod(
         *     Reflection.getClass("java.lang.Integer"), "parseInt", "\"7\"")
         *   .end();
         * </pre>
         *
         * @return a builder for the value, whose calls hand back the field builder so it can be
         * ended
         */
        public FieldInitializerBuilder initializer() {
            return new FieldInitializerBuilder();
        }

        /**
         * adds the field to the class with everything set on this builder, and hands back the
         * class builder so more members can be added.
         * <br>
         * This is what applies the visibility and the modifiers, the annotations, and the initial
         * value, so it is the call to use whenever any of those were set.
         * {@link #compile(String) compile} also adds the field but applies none of them.
         * example:
         * <pre>
         * builder.addField(Reflection.getClass("java.lang.String"), "label")
         *   .makePrivate()
         *   .toggleFinal()
         *   .initializer().setString("unnamed")
         *   .end()
         *   .addMethod("public String getLabel() { return this.label; }");
         * </pre>
         *
         * @return the class builder, so more members can be added
         * @throws CannotCompileException if the field could not be added
         */
        public ClassBuilder<T> end() throws CannotCompileException {
            CtField field = new CtField(fieldType, fieldName, ctClass);
            field.setModifiers(fieldMods);
            field.getFieldInfo().addAttribute(fieldAnnotations);
            field.getFieldInfo().addAttribute(invisibleFieldAnnotations);
            ctClass.addField(field, fieldInitializer);
            return ClassBuilder.this;
        }

        /**
         * the value a field starts out with, picked from the thirteen calls here, each of which sets
         * it in place of any set before.<br>
         * A script does not make one of these; it is what
         * {@link FieldBuilder#initializer() initializer} hands back. Nine of the calls are for a
         * constant of one kind or another and the last four are for something that has to be
         * worked out: an expression, a new object, a call on another class, or a call on a static
         * method of the class being built. The value is applied when the field is
         * {@link FieldBuilder#end() ended}, so the last call made is the one that counts.
         * example:
         * <pre>
         * builder.addField(Reflection.getClass("java.util.List"), "items")
         *   .initializer().initClass(Reflection.getClass("java.util.ArrayList"))
         *   .end();
         * </pre>
         */
        public class FieldInitializerBuilder {

            /**
             * starts the field off at an int constant.
             *
             * @param value the value to start the field at
             * @return the field builder, so the field can be ended
             */
            public FieldBuilder setInt(int value) {
                FieldBuilder.this.fieldInitializer = CtField.Initializer.constant(value);
                return FieldBuilder.this;
            }

            /**
             * starts the field off at a long constant.
             *
             * @param value the value to start the field at
             * @return the field builder, so the field can be ended
             */
            public FieldBuilder setLong(long value) {
                FieldBuilder.this.fieldInitializer = CtField.Initializer.constant(value);
                return FieldBuilder.this;
            }

            /**
             * starts the field off at a float constant. The value is given as a double, as it is
             * for every script number, and is narrowed to a float here.
             *
             * @param value the value to start the field at, narrowed to a float
             * @return the field builder, so the field can be ended
             */
            public FieldBuilder setFloat(double value) {
                FieldBuilder.this.fieldInitializer = CtField.Initializer.constant((float) value);
                return FieldBuilder.this;
            }

            /**
             * starts the field off at a double constant.
             *
             * @param value the value to start the field at
             * @return the field builder, so the field can be ended
             */
            public FieldBuilder setDouble(double value) {
                FieldBuilder.this.fieldInitializer = CtField.Initializer.constant(value);
                return FieldBuilder.this;
            }

            /**
             * starts the field off at a char constant. A script has no character type of its own,
             * so the value is given as the one character long string Java would take.
             *
             * @param value the character to start the field at
             * @return the field builder, so the field can be ended
             */
            public FieldBuilder setChar(char value) {
                FieldBuilder.this.fieldInitializer = CtField.Initializer.constant(value);
                return FieldBuilder.this;
            }

            /**
             * starts the field off at a string constant, which is the usual value for a field
             * holding text.
             *
             * @param value the value to start the field at
             * @return the field builder, so the field can be ended
             */
            public FieldBuilder setString(String value) {
                FieldBuilder.this.fieldInitializer = CtField.Initializer.constant(value);
                return FieldBuilder.this;
            }

            /**
             * starts the field off at a boolean constant.
             *
             * @param value the value to start the field at
             * @return the field builder, so the field can be ended
             */
            public FieldBuilder setBoolean(boolean value) {
                FieldBuilder.this.fieldInitializer = CtField.Initializer.constant(value);
                return FieldBuilder.this;
            }

            /**
             * starts the field off at a byte constant. A script has no byte type of its own, so
             * this only makes sense for a field whose type is a byte.
             *
             * @param value the value to start the field at
             * @return the field builder, so the field can be ended
             */
            public FieldBuilder setByte(byte value) {
                FieldBuilder.this.fieldInitializer = CtField.Initializer.constant(value);
                return FieldBuilder.this;
            }

            /**
             * starts the field off at a short constant. A script has no short type of its own, so
             * this only makes sense for a field whose type is a short.
             *
             * @param value the value to start the field at
             * @return the field builder, so the field can be ended
             */
            public FieldBuilder setShort(short value) {
                FieldBuilder.this.fieldInitializer = CtField.Initializer.constant(value);
                return FieldBuilder.this;
            }

            /**
             * starts the field off at the value of an expression, which is java source rather than
             * a value.<br>
             * The expression is evaluated once, when the field is initialised, and it runs on its
             * own rather than as part of a constructor body, so there is no instance to refer to
             * and anything it needs has to be a class or a static method. A literal expression of
             * the field's own type is the simple case, and {@link #callStaticMethod(Class, String, String...)
             * callStaticMethod} is the one for anything that has to be worked out.
             * example:
             * <pre>
             * builder.addField(Reflection.getClass("int"), "answer")
             *   .initializer().compile("6 * 7")
             *   .end();
             * </pre>
             *
             * @param code the java source of the expression to evaluate
             * @return the field builder, so the field can be ended
             */
            public FieldBuilder compile(String code) {
                FieldBuilder.this.fieldInitializer = CtField.Initializer.byExpr(code);
                return FieldBuilder.this;
            }

            /**
             * starts the field off at a new object, made by calling the class's constructor.<br>
             * This is how a field gets an empty collection or a fresh object rather than
             * {@code null}. The arguments are java source for the constructor's parameters, and the
             * constructor is looked up from those, so a class with more than one constructor is
             * only reachable through the one whose parameters the given source fits.
             * example:
             * <pre>
             * // a field that starts out holding a new, empty list
             * builder.addField(Reflection.getClass("java.util.List"), "items")
             *   .initializer().initClass(Reflection.getClass("java.util.ArrayList"))
             *   .end();
             *
             * // and one built from an argument
             * builder.addField(Reflection.getClass("java.lang.String"), "greeting")
             *   .initializer().initClass(Reflection.getClass("java.lang.StringBuilder"), "\"hi\"")
             *   .end();
             * </pre>
             *
             * @param clazz    the class to make an instance of
             * @param code_arg java source for the constructor's arguments, none for a no argument
             *                  one
             * @return the field builder, so the field can be ended
             * @throws NotFoundException if the class could not be resolved
             */
            public FieldBuilder initClass(Class<?> clazz, String... code_arg) throws NotFoundException {
                FieldBuilder.this.fieldInitializer = CtField.Initializer.byNewWithParams(defaultPool.getCtClass(clazz.getName()), code_arg);
                return FieldBuilder.this;
            }

            /**
             * starts the field off at what a static method on another class returns.<br>
             * The method is looked up by name and by the arguments given, so it is a method that
             * takes exactly those and returns something the field's type can take. The arguments
             * are java source, not values, so a literal is written as a Java literal.
             * example:
             * <pre>
             * builder.addField(Reflection.getClass("int"), "parsed")
             *   .initializer().callStaticMethod(
             *     Reflection.getClass("java.lang.Integer"), "parseInt", "\"7\"")
             *   .end();
             * </pre>
             *
             * @param clazz      the class declaring the static method
             * @param methodName the name of the static method
             * @param code_arg   java source for the method's arguments
             * @return the field builder, so the field can be ended
             * @throws NotFoundException if the class could not be resolved
             */
            public FieldBuilder callStaticMethod(Class<?> clazz, String methodName, String... code_arg) throws NotFoundException {
                FieldBuilder.this.fieldInitializer = CtField.Initializer.byCallWithParams(defaultPool.getCtClass(clazz.getName()), methodName, code_arg);
                return FieldBuilder.this;
            }

            /**
             * starts the field off at what a static method on the class being built returns.<br>
             * This is {@link #callStaticMethod(Class, String, String...) callStaticMethod} pointed
             * at the generated class rather than at another one, which is how one field is filled
             * in from another. The method has to be static and has to be added before the field,
             * since the class is compiled as a whole.
             * example:
             * <pre>
             * // a static method on the generated class, then a field that reads it
             * const factory = builder.addMethod(Reflection.getClass("int"), "defaultLimit");
             * factory.makePublic();
             * factory.toggleStatic();
             * factory.body("{ return 10; }");
             *
             * builder.addField(Reflection.getClass("int"), "limit")
             *   .initializer().callStaticMethodInThisClass("defaultLimit")
             *   .end();
             * </pre>
             *
             * @param methodName the name of the static method on this class
             * @param code_arg   java source for the method's arguments
             * @return the field builder, so the field can be ended
             */
            public FieldBuilder callStaticMethodInThisClass(String methodName, String... code_arg) {
                FieldBuilder.this.fieldInitializer = CtField.Initializer.byCallWithParams(ctClass, methodName, code_arg);
                return FieldBuilder.this;
            }

        }

    }

    /**
     * one method of the class being built, set up in steps and added when it is finished.
     * <br>
     * A script does not make one of these; it is what
     * {@link #addMethod(java.lang.Class, java.lang.String, java.lang.Class[]) addMethod(Class, String, Class...)}
     * hands back. The return type, the name and the parameter types are fixed by that call, and
     * what is left is everything else: the visibility, whether it is static or abstract, the
     * exceptions it declares, an annotation, and the body.<br>
     * There are six ways to finish it, in three groups, and they are not interchangeable.
     * {@link #body(java.lang.String) body(String)} and {@link #compile(String) compile(String)}
     * add a body written as Java source, which runs inside the generated class where a script has
     * nothing to call. {@link #guestBody(MethodWrapper) guestBody},
     * {@link #buildBody() buildBody()} and
     * {@link #body(com.jsmacrosce.jsmacros.core.MethodWrapper) body(MethodWrapper)} all add a
     * body that calls back into the script that is building the class, the first for a single
     * call, the second for several with Java around them, and the third for a handler that is
     * handed the class and the method itself and so decides what goes in.
     * {@link #endAbstract() endAbstract()} adds no body at all, which is what an
     * interface method or an abstract class needs.
     * example:
     * <pre>
     * // a plain method, with a body written in java
     * const describe = builder.addMethod(Reflection.getClass("java.lang.String"), "describe",
     *   Reflection.getClass("java.lang.Object"));
     * describe.makePublic();
     * describe.body("return \"an object: \" + $1;");
     *
     * // an abstract one, for a class that leaves it to a subclass
     * const reset = builder.addMethod(Reflection.getClass("void"), "reset");
     * reset.makePublic();
     * reset.endAbstract();
     * </pre>
     */
    public class MethodBuilder {
        CtClass methodReturnType;
        CtClass[] params;
        CtClass[] exceptions;
        String methodName;
        final AnnotationsAttribute methodAnnotations = new AnnotationsAttribute(ctClass.getClassFile().getConstPool(), AnnotationsAttribute.visibleTag);
        final AnnotationsAttribute invisibleMethodAnnotations = new AnnotationsAttribute(ctClass.getClassFile().getConstPool(), AnnotationsAttribute.invisibleTag);
        int methodMods = 0;

        /**
         * starts a method with the return type, name and parameter types given. A script does not
         * call this; it is what {@link #addMethod(java.lang.Class, java.lang.String, java.lang.Class[])
         * addMethod(Class, String, Class...)} calls, and what
         * {@link #addConstructor(java.lang.Class[]) addConstructor(Class...)} calls for a
         * constructor.
         *
         * @param methodReturnType the type the method returns
         * @param methodName       the name of the method
         * @param params           the parameter types, in order
         */
        public MethodBuilder(CtClass methodReturnType, String methodName, CtClass... params) {
            this.methodReturnType = methodReturnType;
            this.methodName = methodName;
            this.params = params;
        }

        /**
         * adds the method to the class straight away from a declaration written as Java source,
         * and hands back the class builder.
         * <br>
         * The whole method is written in the argument, so the source is the method rather than
         * just its body, and it says for itself what the return type, the name, the parameters
         * and the visibility are. The types and the name this builder was started with are not
         * used, and neither is anything set on the builder, so this and
         * {@link #body(java.lang.String) body(String)} are two ways of doing the same thing and
         * the source form is the one to use when there is no reason to go through the builder at
         * all. A method with generics or varargs cannot be written this way.
         * example:
         * <pre>
         * builder.addMethod(Reflection.getClass("boolean"), "isEmpty")
         *   .compile("public boolean isEmpty() { return this.size() == 0; }");
         * </pre>
         *
         * @param code the source of the whole method, not just its body
         * @return the class builder, so more members can be added
         * @throws CannotCompileException if the method does not compile
         */
        public ClassBuilder<T> compile(String code) throws CannotCompileException {
            ctClass.addMethod(CtMethod.make(code, ctClass));
            return ClassBuilder.this;
        }

        /**
         * makes the method private, leaving the static flag as it was. Since a method starts with
         * no flags, this has to come before {@link #toggleStatic() toggleStatic} to keep that,
         * and the other three visibility calls do the same thing, so the last one called is the
         * one that applies.
         *
         * @return self
         */
        public MethodBuilder makePrivate() {
            this.methodMods = (Modifier.PRIVATE) | (this.methodMods & Modifier.STATIC);
            return this;
        }

        /**
         * makes the method public, on the same terms as {@link #makePrivate() makePrivate}: the
         * static flag is kept and this replaces any other visibility already set.
         *
         * @return self
         */
        public MethodBuilder makePublic() {
            this.methodMods = (Modifier.PUBLIC) | (this.methodMods & Modifier.STATIC);
            return this;
        }

        /**
         * makes the method protected, on the same terms as {@link #makePrivate() makePrivate}: the
         * static flag is kept and this replaces any other visibility already set.
         *
         * @return self
         */
        public MethodBuilder makeProtected() {
            this.methodMods = (Modifier.PROTECTED) | (this.methodMods & Modifier.STATIC);
            return this;
        }

        /**
         * makes the method package private, which is what a method with no visibility call is
         * already, so this is the way back to that after one of the other three. The static flag
         * is kept.
         *
         * @return self
         */
        public MethodBuilder makePackagePrivate() {
            this.methodMods = (this.methodMods & Modifier.STATIC);
            return this;
        }

        /**
         * flips the static flag, so calling it twice is the same as never calling it. A static
         * method has no instance to work on, so a body written for it cannot use {@code this} and
         * a {@link #guestBody(MethodWrapper) guestBody} handler cannot reach the instance through
         * the reference it is handed.
         *
         * @return self
         */
        public MethodBuilder toggleStatic() {
            this.methodMods ^= Modifier.STATIC;
            return this;
        }

        /**
         * changes the name the method will be added under, for a case where the name on the
         * builder is not the name wanted, such as a name that is a Java keyword and so cannot be
         * written in a source body. Nothing is added until the method is finished, so this is
         * free to call as often as wanted and the last one wins.
         *
         * @param newName the name to add the method under
         * @return self
         */
        public MethodBuilder rename(String newName) {
            this.methodName = newName;
            return this;
        }

        /**
         * declares the exceptions the method is allowed to throw, as a checked exception a caller
         * has to deal with.<br>
         * These are only a declaration: nothing here throws them, and saying so does not make a
         * body that throws something else legal. It is what a method has to say before the Java
         * code calling it is allowed to catch that kind by name rather than by class, and it does
         * not stop an unchecked exception from coming out of it.
         * example:
         * <pre class="language-typescript">
         * const read = builder.addMethod(Reflection.getClass("java.lang.String"), "read");
         * read.makePublic();
         * read.exceptions(Reflection.getClass("java.io.IOException") as any);
         * read.body("throw new java.io.IOException(\"nope\");");
         * </pre>
         *
         * @param exceptions the exception types to declare
         * @return self
         * @throws NotFoundException if a type could not be resolved
         */
        public MethodBuilder exceptions(Class<?>... exceptions) throws NotFoundException {
            this.exceptions = new CtClass[exceptions.length];
            for (int i = 0; i < exceptions.length; i++) {
                this.exceptions[i] = defaultPool.getCtClass(exceptions[i].getName());
            }
            return this;
        }

        /**
         * adds the method to the class with a body written as Java source, and hands back the
         * class builder.<br>
         * The return type, the name, the parameters, the visibility and the declared exceptions
         * come from what was set on this builder, and the argument is just the inside of the
         * method. The body runs inside the generated class when the method is called, so it can
         * use the class and everything it extends, but it has no way to call back into the script
         * that built it: that is what {@link #guestBody(MethodWrapper) guestBody} and
         * {@link #buildBody() buildBody()} are for. A class from the {@code java.lang} package can
         * be named without its package, as in Java, and generics and varargs are not supported
         * here.
         * example:
         * <pre>
         * const add = builder.addMethod(Reflection.getClass("int"), "add",
         *   Reflection.getClass("int"), Reflection.getClass("int"));
         * add.makePublic();
         * add.body("return $1 + $2;");
         * </pre>
         *
         * @param code_src the source of the inside of the method, without the braces
         * @return the class builder, so more members can be added
         * @throws CannotCompileException if the method does not compile
         */
        public ClassBuilder<T> body(String code_src) throws CannotCompileException {
            CtMethod method = CtNewMethod.make(this.methodMods, this.methodReturnType, this.methodName, this.params, this.exceptions, code_src, ctClass);
            method.getMethodInfo().addAttribute(methodAnnotations);
            method.getMethodInfo().addAttribute(invisibleMethodAnnotations);
            ctClass.addMethod(method);
            return ClassBuilder.this;
        }

        /**
         * adds the method to the class with a body that calls a handler in the running script, and
         * hands back the class builder.
         * <br>
         * This is the form that makes a generated class able to run script code. The method that
         * goes into the class does the work of looking the handler up in
         * {@link #methodWrappers} and calling it with the instance as its first argument and the
         * method's arguments as the rest, so what the handler receives is
         * {@code (self, args)}, where {@code self} is the generated instance and {@code args} is
         * an array of the arguments the method was called with. A primitive argument is boxed on
         * the way in and the handler's return value is unwrapped on the way out, so a method
         * declared to return {@code int} wants a number back and one declared {@code void} wants
         * nothing.<br>
         * The handler is a wrapper, so it wants to be made with
         * {@code JavaWrapper.methodToJava} rather than handed as a plain function: a plain
         * function would be called on the Java thread with no script context to join, which is
         * the same reason every other callback in JsMacros is a wrapper. The key the handler is
         * filed under is worked out from the class name, the method name and the method's JVM
         * descriptor, which is why the types have to be fixed before this is called.<br>
         * {@link #buildBody() buildBody()} is the form to use when a method needs more than the
         * one call, or needs some Java around it.
         * example:
         * <pre>
         * const add = builder.addMethod(Reflection.getClass("int"), "addTo",
         *   Reflection.getClass("int"));
         * add.makePublic();
         * add.guestBody(JavaWrapper.methodToJava(function (self, args) {
         *   // `args` is an array, so a primitive argument arrives boxed
         *   return self.size() + Number(args[0]);
         * }));
         * </pre>
         *
         * @param methodBody the handler, made with {@code JavaWrapper.methodToJava}
         * @return the class builder, so more members can be added
         * @throws CannotCompileException if the method could not be added
         * @throws NotFoundException      if a type named while building could not be resolved
         */
        public ClassBuilder<T> guestBody(MethodWrapper<Object, Object, Object, ?> methodBody) throws CannotCompileException, NotFoundException {
            CtMethod method = new CtMethod(this.methodReturnType, this.methodName, this.params, ctClass);
            method.setModifiers(this.methodMods);
            method.setExceptionTypes(this.exceptions);
            boolean voidReturn = methodReturnType.equals(CtClass.voidType);
            String guestName = ClassBuilder.this.className + ";" + methodName + Descriptor.ofMethod(methodReturnType, params);
            StringBuilder body = new StringBuilder();
            body.append("{");
            if (!voidReturn) {
                body.append(" return ");
                if (!methodReturnType.isPrimitive()) {
                    body.append("((").append(methodReturnType.getName()).append(")");
                } else {
                    if (methodReturnType.equals(CtClass.booleanType)) {
                        body.append("((java.lang.Boolean)");
                    } else {
                        body.append("((java.lang.Number)");
                    }
                }
            } else {
                body.append("(");
            }
            body.append("((com.jsmacrosce.jsmacros.core.MethodWrapper)")
                    .append("com.jsmacrosce.jsmacros.core.library.impl.classes.ClassBuilder.methodWrappers.get(\"")
                    .append(guestName)
                    .append("\")).")
                    .append(voidReturn ? "accept" : "apply")
                    .append("(")
                    .append("$0, new Object[]{");
            int i = 0;
            for (CtClass param : params) {
                if (!param.isPrimitive()) {
                    body.append("$").append(++i).append(",");
                } else {
                    if (param.equals(CtClass.booleanType)) {
                        body.append("java.lang.Boolean.valueOf($").append(++i).append("),");
                    } else if (param.equals(CtClass.byteType)) {
                        body.append("java.lang.Byte.valueOf($").append(++i).append("),");
                    } else if (param.equals(CtClass.charType)) {
                        body.append("java.lang.Character.valueOf($").append(++i).append("),");
                    } else if (param.equals(CtClass.shortType)) {
                        body.append("java.lang.Short.valueOf($").append(++i).append("),");
                    } else if (param.equals(CtClass.intType)) {
                        body.append("java.lang.Integer.valueOf($").append(++i).append("),");
                    } else if (param.equals(CtClass.longType)) {
                        body.append("java.lang.Long.valueOf($").append(++i).append("),");
                    } else if (param.equals(CtClass.floatType)) {
                        body.append("java.lang.Float.valueOf($").append(++i).append("),");
                    } else if (param.equals(CtClass.doubleType)) {
                        body.append("java.lang.Double.valueOf($").append(++i).append("),");
                    } else {
                        throw new RuntimeException("Unknown primitive type: " + param.getName());
                    }
                }
            }
            if (params.length > 0) {
                body.deleteCharAt(body.length() - 1);
            }
            body.append("}))");
            if (!voidReturn) {
                if (!methodReturnType.isPrimitive()) {
                    body.append(");");
                } else {
                    if (methodReturnType.equals(CtClass.booleanType)) {
                        body.append(".booleanValue();");
                    } else if (methodReturnType.equals(CtClass.byteType)) {
                        body.append(".byteValue();");
                    } else if (methodReturnType.equals(CtClass.charType)) {
                        body.append(".charValue();");
                    } else if (methodReturnType.equals(CtClass.shortType)) {
                        body.append(".shortValue();");
                    } else if (methodReturnType.equals(CtClass.intType)) {
                        body.append(".intValue();");
                    } else if (methodReturnType.equals(CtClass.longType)) {
                        body.append(".longValue();");
                    } else if (methodReturnType.equals(CtClass.floatType)) {
                        body.append(".floatValue();");
                    } else if (methodReturnType.equals(CtClass.doubleType)) {
                        body.append(".doubleValue();");
                    } else {
                        throw new RuntimeException("Unknown primitive type: " + methodReturnType.getName());
                    }
                }
            } else {
                body.append(";");
            }
            body.append("}");
            method.setBody(body.toString());
            method.getMethodInfo().addAttribute(methodAnnotations);
            method.getMethodInfo().addAttribute(invisibleMethodAnnotations);
            ctClass.addMethod(method);
            methodWrappers.put(guestName, methodBody);
            return ClassBuilder.this;
        }

        /**
         * adds the method to the class with no body yet, and hands back a builder for one that can
         * mix Java and calls into the script.
         * <br>
         * This is the general form of {@link #guestBody(MethodWrapper) guestBody}, and it is the
         * one to use whenever a method needs more than a single call: the returned builder takes
         * lines of Java and lines that call handlers, in any order, and the method is not really
         * there until it is finished. The method is added to the class straight away with an empty
         * body, so the method exists before anything is written into it, which is what lets a
         * later line refer to it.<br>
         * A line that calls a handler is a statement rather than an expression, so the value it
         * gets back has to be put somewhere with the token before it, and a method whose return
         * type is a primitive has to do the unwrapping itself in a line of Java. That is the
         * difference from {@code guestBody}, which writes the cast into the method for you.
         * example:
         * <pre>
         * const bump = builder.addMethod(Reflection.getClass("void"), "bump");
         * bump.makePublic();
         * bump.buildBody()
         *   .appendJavaCode("int before = this.total")
         *   .appendGuestCode(JavaWrapper.methodToJava(function (self, args) {
         *     print("the script is deciding how much to add");
         *     return 1;
         *   }), "", "Object step = ")
         *   .appendJavaCode("this.total = before + ((java.lang.Integer) step).intValue()")
         *   .finish();
         * </pre>
         *
         * @return a builder for the body, whose finish call hands back the class builder
         * @throws CannotCompileException if the method could not be added
         */
        public BodyBuilder buildBody() throws CannotCompileException {
            CtMethod method = new CtMethod(this.methodReturnType, this.methodName, this.params, ctClass);
            method.setModifiers(this.methodMods);
            method.getMethodInfo().addAttribute(methodAnnotations);
            method.getMethodInfo().addAttribute(invisibleMethodAnnotations);
            String guestName = ClassBuilder.this.className + ";" + methodName + Descriptor.ofMethod(methodReturnType, params);
            ctClass.addMethod(method);
            return new BodyBuilder(method, guestName);
        }

        /**
         * adds the method to the class with a body written by a handler in the running script, and
         * hands back the class builder.
         * <br>
         * The handler is given the bytecode library's own handles on the class being built and on
         * the method, both of which it can change, and the method is added once it returns. That
         * makes this the way to write a body that is not just "call a handler and return": a
         * handler here can add fields to the class, set the method's name, or leave the method
         * with no body at all, since it is the one deciding what goes in.<br>
         * It is the same shape as {@link #buildBody() buildBody()} with the body handed over
         * rather than built up, and the handler is called once, on the thread doing the building,
         * so anything slow in it holds that thread up.
         * example:
         * <pre class="language-typescript">
         * const reset = builder.addMethod(Reflection.getClass("void"), "reset");
         * reset.makePublic();
         * reset.body(JavaWrapper.methodToJava(function (clazz, method) {
         *   // the cast is for the editor's benefit: the shipped typings do not
         *   // describe the bytecode library these two are
         *   print("writing the body of reset now");
         *   (method as any).setBody("this.size();");
         * }));
         * </pre>
         *
         * @param buildBody the handler, given the class and the method to finish, made with
         *                  {@code JavaWrapper.methodToJava}
         * @return the class builder, so more members can be added
         * @throws CannotCompileException if the method could not be added
         */
        public ClassBuilder<T> body(MethodWrapper<CtClass, CtBehavior, Object, ?> buildBody) throws CannotCompileException {
            CtMethod method = new CtMethod(this.methodReturnType, this.methodName, this.params, ctClass);
            method.setModifiers(this.methodMods);
            method.getMethodInfo().addAttribute(methodAnnotations);
            method.getMethodInfo().addAttribute(invisibleMethodAnnotations);
            buildBody.apply(ctClass, method);
            ctClass.addMethod(method);
            return ClassBuilder.this;
        }

        /**
         * adds the method to the class with no body at all, and hands back the class builder.
         * <br>
         * This is what an interface method or an abstract class method needs, and it carries the
         * declared exceptions and the annotations set on the builder along with it. The method
         * still needs its visibility set, and on a class rather than an interface it also has to
         * be abstract, since the runtime rejects a non abstract class with a method that has no
         * body.
         * example:
         * <pre>
         * // what an interface method looks like
         * const required = builder.addMethod(Reflection.getClass("void"), "reset");
         * required.makePublic();
         * required.endAbstract();
         * </pre>
         *
         * @return the class builder, so more members can be added
         * @throws NotFoundException      if a type named while building could not be resolved
         * @throws CannotCompileException if the method could not be added
         */
        public ClassBuilder<T> endAbstract() throws NotFoundException, CannotCompileException {
            CtMethod method = CtNewMethod.abstractMethod(this.methodReturnType, this.methodName, this.params, this.exceptions, ctClass);
            method.getMethodInfo().addAttribute(methodAnnotations);
            method.getMethodInfo().addAttribute(invisibleMethodAnnotations);
            ctClass.addMethod(method);
            return ClassBuilder.this;
        }

        /**
         * starts an annotation on this method, to be filled in and committed through the returned
         * builder. It works the same way as
         * {@link ClassBuilder#addAnnotation(java.lang.Class) addAnnotation} does for the class, and
         * what the annotation's retention says decides whether it goes on as one the runtime can
         * see or as one only the reflection library can.
         * example:
         * <pre class="language-typescript">
         * const old = builder.addMethod(Reflection.getClass("void"), "legacy");
         * old.makePublic();
         * old.addAnnotation(Reflection.getClass("java.lang.Deprecated") as any).finish();
         * old.body("return;");
         * </pre>
         *
         * @param type the annotation type to put on the method
         * @return a builder for the annotation, which is put on the method when it is finished
         * @throws NotFoundException if the annotation type could not be resolved
         */
        public AnnotationBuilder<MethodBuilder> addAnnotation(Class<?> type) throws NotFoundException {
            Annotation annotation = new Annotation(ctClass.getClassFile().getConstPool(), defaultPool.getCtClass(type.getName()));
            return new AnnotationBuilder<>(annotation, ctClass.getClassFile().getConstPool(), this, type.getAnnotation(Retention.class).value() != RetentionPolicy.RUNTIME ? invisibleMethodAnnotations : methodAnnotations);
        }

    }

    /**
     * a constructor of the class being built, which is a method builder with the parts that do not
     * apply to it taken out.
     * <br>
     * A script does not make one of these; it is what
     * {@link #addConstructor(java.lang.Class[]) addConstructor(Class...)} and
     * {@link #addClinit() addClinit} hand back. It carries a parameter list rather than a return
     * type and a name, so everything on a {@link MethodBuilder} that is about one of those does
     * not apply: there is no return type to give, no name to change, and nothing to call
     * {@code guestBody}, since a constructor has to call the superclass itself and there is
     * nothing here to do that for it. What is left is the visibility, the declared exceptions, an
     * annotation, and the three ways of giving it a body.
     * example:
     * <pre>
     * const init = builder.addConstructor(Reflection.getClass("java.lang.String"));
     * init.makePublic();
     * init.body('super($1); this.label = $1;');
     * </pre>
     */
    public class ConstructorBuilder extends MethodBuilder {
        /**
         * starts a constructor with the parameter types given, or a class initialiser when
         * {@code clInit} is set. A script does not call this; it is what
         * {@link #addConstructor(java.lang.Class[]) addConstructor(Class...)} and
         * {@link #addClinit() addClinit} call.
         *
         * @param params the parameter types, in order
         * @param clInit whether this is a class initialiser rather than a real constructor, which
         *               also makes it static
         */
        public ConstructorBuilder(CtClass[] params, boolean clInit) {
            super(CtClass.voidType, clInit ? MethodInfo.nameClinit : MethodInfo.nameInit, params);
            if (clInit) {
                this.methodMods |= Modifier.STATIC;
            }
        }

        /**
         * adds the constructor to the class with a body written as Java source, and hands back the
         * class builder.
         * <br>
         * The parameters, the visibility and the declared exceptions come from what was set on this
         * builder, and the argument is just the inside of the constructor. Calling the superclass
         * is the constructor's own job, since there is nothing here to pass on automatically, so
         * a constructor taking arguments writes its own {@code super($1, $2)}.
         * example:
         * <pre>
         * const init = builder.addConstructor(Reflection.getClass("java.lang.String"));
         * init.makePublic();
         * init.body('super($1); this.label = $1;');
         * </pre>
         *
         * @param code_src the source of the inside of the constructor, without the braces
         * @return the class builder, so more members can be added
         * @throws CannotCompileException if the constructor does not compile
         */
        @Override
        public ClassBuilder<T> body(String code_src) throws CannotCompileException {
            CtConstructor constructor = CtNewConstructor.make(this.params, this.exceptions, code_src, ctClass);
            constructor.setModifiers(this.methodMods);
            constructor.getMethodInfo().addAttribute(methodAnnotations);
            constructor.getMethodInfo().addAttribute(invisibleMethodAnnotations);
            ctClass.addConstructor(constructor);
            return ClassBuilder.this;
        }

        /**
         * adds the constructor to the class with a body that calls a handler in the running script,
         * and hands back the class builder.
         * <br>
         * This only works for a constructor with no parameters. One with parameters is refused
         * with an {@link IllegalArgumentException}, because the superclass call cannot be written
         * without knowing what the parameters are, and a handler has no way to supply one; use
         * {@link #body(java.lang.String) body(String)} or {@link #buildBody() buildBody()} for
         * those.<br>
         * The handler is given the instance being built, so it can set the class's own fields up,
         * and the superclass call is written in for it.
         * example:
         * <pre>
         * const init = builder.addConstructor();
         * init.makePublic();
         * init.guestBody(JavaWrapper.methodToJava(function (self, args) {
         *   print(`building a ${self.size()}`);
         * }));
         * </pre>
         *
         * @param methodBody the handler, made with {@code JavaWrapper.methodToJava}
         * @return the class builder, so more members can be added
         * @throws CannotCompileException     if the constructor could not be added
         * @throws NotFoundException          if a type named while building could not be resolved
         * @throws IllegalArgumentException   if the constructor takes parameters
         */
        @Override
        public ClassBuilder<T> guestBody(MethodWrapper<Object, Object, Object, ?> methodBody) throws CannotCompileException, NotFoundException {
            if (params.length != 0) {
                throw new IllegalArgumentException("must use one of the other body methods as this one can't call super...");
            }
            CtConstructor method = new CtConstructor(this.params, ctClass);
            method.setModifiers(this.methodMods);
            method.setExceptionTypes(this.exceptions);
            String guestName = ClassBuilder.this.className + ";" + methodName + Descriptor.ofMethod(methodReturnType, params);
            StringBuilder body = new StringBuilder();
            if (params.length == 0 || Arrays.stream(ClassBuilder.this.ctClass.getSuperclass().getDeclaredConstructors()).anyMatch(c -> {
                try {
                    return Arrays.equals(c.getParameterTypes(), params);
                } catch (NotFoundException e) {
                    throw new RuntimeException(e);
                }
            })) {
                body.append("super(");
                for (int i = 0; i < params.length; i++) {
                    if (i != 0) {
                        body.append(", ");
                    }
                    body.append("$").append(i + 1);
                }
                body.append(");");
            }
            body.append("{(((com.jsmacrosce.jsmacros.core.MethodWrapper)")
                    .append("com.jsmacrosce.jsmacros.core.library.impl.classes.ClassBuilder.methodWrappers.get(\"")
                    .append(guestName)
                    .append("\")).")
                    .append("apply")
                    .append("(")
                    .append("$0, new Object[]{");
            int i = 0;
            for (CtClass param : params) {
                if (!param.isPrimitive()) {
                    body.append("(java.lang.Object) $").append(++i).append(",");
                } else {
                    if (param.equals(CtClass.booleanType)) {
                        body.append("java.lang.Boolean.valueOf($").append(++i).append("),");
                    } else if (param.equals(CtClass.byteType)) {
                        body.append("java.lang.Byte.valueOf($").append(++i).append("),");
                    } else if (param.equals(CtClass.charType)) {
                        body.append("java.lang.Character.valueOf($").append(++i).append("),");
                    } else if (param.equals(CtClass.shortType)) {
                        body.append("java.lang.Short.valueOf($").append(++i).append("),");
                    } else if (param.equals(CtClass.intType)) {
                        body.append("java.lang.Integer.valueOf($").append(++i).append("),");
                    } else if (param.equals(CtClass.longType)) {
                        body.append("java.lang.Long.valueOf($").append(++i).append("),");
                    } else if (param.equals(CtClass.floatType)) {
                        body.append("java.lang.Float.valueOf($").append(++i).append("),");
                    } else if (param.equals(CtClass.doubleType)) {
                        body.append("java.lang.Double.valueOf($").append(++i).append("),");
                    } else {
                        throw new RuntimeException("Unknown primitive type: " + param.getName());
                    }
                }
            }
            if (params.length > 0) {
                body.deleteCharAt(body.length() - 1);
            }
            body.append("}));}");
            method.setBody(body.toString());
            method.getMethodInfo().addAttribute(methodAnnotations);
            method.getMethodInfo().addAttribute(invisibleMethodAnnotations);
            ctClass.addConstructor(method);
            methodWrappers.put(guestName, methodBody);
            return ClassBuilder.this;
        }

        /**
         * adds the constructor to the class with no body yet, and hands back a builder for one that
         * can mix Java and calls into the script.
         * <br>
         * This is {@link MethodBuilder#buildBody() buildBody()} pointed at a constructor, and it
         * is the way to give a constructor a body that calls the script while still writing the
         * superclass call by hand, which {@link #guestBody(MethodWrapper) guestBody} cannot do
         * for a constructor with parameters.
         * example:
         * <pre>
         * const init = builder.addConstructor(Reflection.getClass("java.lang.String"));
         * init.makePublic();
         * init.buildBody()
         *   .appendJavaCode("super($1)")
         *   .appendJavaCode("this.label = $1")
         *   .finish();
         * </pre>
         *
         * @return a builder for the body, whose finish call hands back the class builder
         * @throws CannotCompileException if the constructor could not be added
         */
        @Override
        public BodyBuilder buildBody() throws CannotCompileException {
            CtConstructor method = new CtConstructor(this.params, ctClass);
            method.setModifiers(this.methodMods);
            method.getMethodInfo().addAttribute(methodAnnotations);
            method.getMethodInfo().addAttribute(invisibleMethodAnnotations);
            String guestName = ClassBuilder.this.className + ";" + methodName + Descriptor.ofMethod(methodReturnType, params);
            ctClass.addConstructor(method);
            return new BodyBuilder(method, guestName);
        }

        /**
         * adds the constructor to the class with a body written by a handler in the running script,
         * and hands back the class builder.
         * <br>
         * The handler is given the bytecode library's own handles on the class being built and on
         * the constructor, so it is free to write the superclass call itself and anything else
         * the body needs. This is the form to use when the body is more than a source string, and
         * unlike {@link #guestBody(MethodWrapper) guestBody} it works for a constructor with
         * parameters.
         *
         * @param buildBody the handler, given the class and the constructor to finish, made with
         *                  {@code JavaWrapper.methodToJava}
         * @return the class builder, so more members can be added
         * @throws CannotCompileException if the constructor could not be added
         */
        @Override
        public ClassBuilder<T> body(MethodWrapper<CtClass, CtBehavior, Object, ?> buildBody) throws CannotCompileException {
            CtConstructor constructor = new CtConstructor(this.params, ctClass);
            constructor.setModifiers(this.methodMods);
            constructor.getMethodInfo().addAttribute(methodAnnotations);
            constructor.getMethodInfo().addAttribute(invisibleMethodAnnotations);
            buildBody.apply(ctClass, constructor);
            ctClass.addConstructor(constructor);
            return ClassBuilder.this;
        }

        /**
         * always throws, because a constructor has no abstract form.<br>
         * A constructor is called by the runtime whenever an instance is made, so there is no
         * such thing as one that a subclass is left to fill in, and asking for one is a mistake
         * rather than something to work around. There is no way to make a generated class that
         * cannot be constructed: either give it a constructor or leave it to the superclass's.
         *
         * @return never returns
         * @throws UnsupportedOperationException always, since a constructor cannot be abstract
         */
        @Override
        public ClassBuilder<T> endAbstract() {
            throw new UnsupportedOperationException("Cannot end abstract constructors");
        }

    }

    /**
     * builds the class, defines it, and hands back the {@link Class} for it.
     * <br>
     * This is the call that makes everything added so far into a real class: the annotations put on
     * the class are attached, the bytecode is defined next to a placeholder class so that it can
     * extend anything, and a class object comes back. The class is frozen by it, so this can only
     * be called once, and a second call fails rather than producing a second class. Everything
     * added after it is too late, since the class is already defined.<br>
     * What comes back is typed as the class that was extended rather than as the class that was
     * built, since nothing here knows the members that were added, so a method the build added has
     * to be reached through a cast. {@code Reflection.getClassFromClassBuilderResult} finds the
     * same class again by the name it was built under, and a {@code ClassBuilder} for a library
     * registers the result as well as returning it.
     * example:
     * <pre class="language-typescript">
     * const CountingClass = builder.finishBuildAndFreeze();
     *
     * // typed as what it extends, so an added method needs a cast
     * const counting = Reflection.newInstance(CountingClass) as any;
     * print(counting.getTotal());
     *
     * // and the same class again, by the name it was built under
     * print(Reflection.getClassFromClassBuilderResult("Counting") === CountingClass);
     * </pre>
     *
     * @return the built class, typed as the class it extends
     * @throws CannotCompileException if the assembled class does not compile
     * @throws NotFoundException      if a class named while building could not be resolved
     */
    public Class<? extends T> finishBuildAndFreeze() throws CannotCompileException, NotFoundException {
        ctClass.getClassFile().addAttribute(classAnnotations);
        ctClass.getClassFile().addAttribute(invisibleClassAnnotations);
        return (Class<? extends T>) ctClass.toClass(Neighbor.class);
    }

    /**
     * one annotation on a class, field or method, filled in with the values it takes and put on
     * the member when it is finished.
     * <br>
     * A script does not make one of these; it is what the three {@code addAnnotation} methods
     * hand back, and which member it is for is decided by the call that started it. The
     * annotation type is fixed by that call too, so only the values are left to give, one call per
     * kind of value, each of which takes the name of the annotation's member rather than a
     * position.<br>
     * Nothing is on the member until {@link #finish() finish} is called, and the member it was
     * started from is what that hands back, so the calls chain. A member of the annotation that is
     * never given a value is left with whatever the annotation type declares as its default.
     * example:
     * <pre class="language-typescript">
     * builder.addAnnotation(Reflection.getClass("java.lang.SuppressWarnings") as any)
     *   .putArray("value")
     *   .putString("unchecked")
     *   .putString("rawtypes")
     *   .finish()
     *   .finish();
     * </pre>
     */
    public class AnnotationBuilder<T> {
        final Annotation annotationInstance;
        final AnnotationsAttribute attr;
        final ConstPool constPool;
        private final T member;

        private AnnotationBuilder(Annotation annotationInstance, ConstPool constPool, T member, AnnotationsAttribute attr) {
            this.annotationInstance = annotationInstance;
            this.constPool = constPool;
            this.member = member;
            this.attr = attr;
        }

        /**
         * puts a string on the annotation's member named, replacing anything put there before.
         *
         * @param key   the name of the annotation member
         * @param value the value to put on it
         * @return self
         */
        public AnnotationBuilder<T> putString(String key, String value) {
            annotationInstance.addMemberValue(key, new StringMemberValue(value, constPool));
            return this;
        }

        /**
         * puts a boolean on the annotation's member named.
         *
         * @param key   the name of the annotation member
         * @param value the value to put on it
         * @return self
         */
        public AnnotationBuilder<T> putBoolean(String key, boolean value) {
            annotationInstance.addMemberValue(key, new BooleanMemberValue(value, constPool));
            return this;
        }

        /**
         * puts a byte on the annotation's member named.
         *
         * @param key   the name of the annotation member
         * @param value the value to put on it
         * @return self
         */
        public AnnotationBuilder<T> putByte(String key, byte value) {
            annotationInstance.addMemberValue(key, new ByteMemberValue(value, constPool));
            return this;
        }

        /**
         * puts a character on the annotation's member named. A script has no character type of its
         * own, so this only makes sense for an annotation member that is a {@code char}.
         *
         * @param key   the name of the annotation member
         * @param value the character to put on it
         * @return self
         */
        public AnnotationBuilder<T> putChar(String key, char value) {
            annotationInstance.addMemberValue(key, new CharMemberValue(value, constPool));
            return this;
        }

        /**
         * puts a short on the annotation's member named. A script has no short type of its own, so
         * this only makes sense for an annotation member that is a {@code short}.
         *
         * @param key   the name of the annotation member
         * @param value the value to put on it
         * @return self
         */
        public AnnotationBuilder<T> putShort(String key, short value) {
            annotationInstance.addMemberValue(key, new ShortMemberValue(value, constPool));
            return this;
        }

        /**
         * puts an int on the annotation's member named.
         *
         * @param key   the name of the annotation member
         * @param value the value to put on it
         * @return self
         */
        public AnnotationBuilder<T> putInt(String key, int value) {
            annotationInstance.addMemberValue(key, new IntegerMemberValue(value, constPool));
            return this;
        }

        /**
         * puts a long on the annotation's member named.
         *
         * @param key   the name of the annotation member
         * @param value the value to put on it
         * @return self
         */
        public AnnotationBuilder<T> putLong(String key, long value) {
            annotationInstance.addMemberValue(key, new LongMemberValue(value, constPool));
            return this;
        }

        /**
         * puts a float on the annotation's member named. The value is given as a double, as it is
         * for every script number, and is narrowed to a float here.
         *
         * @param key   the name of the annotation member
         * @param value the value to put on it, narrowed to a float
         * @return self
         */
        public AnnotationBuilder<T> putFloat(String key, double value) {
            annotationInstance.addMemberValue(key, new FloatMemberValue((float) value, constPool));
            return this;
        }

        /**
         * puts a double on the annotation's member named.
         *
         * @param key   the name of the annotation member
         * @param value the value to put on it
         * @return self
         */
        public AnnotationBuilder<T> putDouble(String key, double value) {
            annotationInstance.addMemberValue(key, new DoubleMemberValue(value, constPool));
            return this;
        }

        /**
         * puts a class on the annotation's member named, by giving the class itself rather than
         * its name as text. A member declared {@code Class} is what this is for, and there is no
         * equivalent for one declared as a string, since that is {@link #putString(String, String)
         * putString} and the name would have to be right by hand.
         *
         * @param key   the name of the annotation member
         * @param value the class to put on it
         * @return self
         */
        public AnnotationBuilder<T> putClass(String key, Class<?> value) {
            annotationInstance.addMemberValue(key, new ClassMemberValue(value.getName(), constPool));
            return this;
        }

        /**
         * puts one constant of an enum on the annotation's member named, by giving the constant
         * itself rather than its name as text.<br>
         * The enum is worked out from the constant, so the annotation member has to be declared as
         * that enum or a supertype of it. A member declared {@code Class} is
         * {@link #putClass(String, Class) putClass} instead.<br>
         * Getting hold of the constant is the awkward part from a script: {@code Reflection} has
         * no call for an enum constant by name, so it has to be reached through a field on the
         * enum's own class, which is also not in the shipped typings. This is the least convenient
         * of the value calls for that reason rather than because anything here is unusual.
         *
         * @param key   the name of the annotation member
         * @param value the constant to put on it
         * @return self
         */
        public AnnotationBuilder<T> putEnum(String key, Enum<?> value) {
            EnumMemberValue enumMemberValue = new EnumMemberValue(constPool);
            enumMemberValue.setType(value.getDeclaringClass().getName());
            enumMemberValue.setValue(value.name());
            annotationInstance.addMemberValue(key, enumMemberValue);
            return this;
        }

        /**
         * puts an annotation on the annotation's member named, so that one annotation carries
         * another as a value.<br>
         * What comes back is a builder for the inner one, and finishing it hands back this builder,
         * so the outer annotation still has to be finished after it. An inner annotation given no
         * value of its own is left with its defaults, the same as an outer one. A member declared
         * as an array of annotations is {@link #putArray(String) putArray} and then
         * {@link AnnotationArrayBuilder#putAnnotation(Class) putAnnotation} on the array.
         *
         * @param key            the name of the annotation member
         * @param annotationClass the annotation type to put on it
         * @return a builder for the inner annotation, whose finish hands back this builder
         * @throws NotFoundException if the annotation type could not be resolved
         */
        public AnnotationBuilder<AnnotationBuilder<T>> putAnnotation(String key, Class<?> annotationClass) throws NotFoundException {
            Annotation annotation = new Annotation(constPool, defaultPool.getCtClass(annotationClass.getName()));
            annotationInstance.addMemberValue(key, new AnnotationMemberValue(annotation, constPool));
            return new AnnotationBuilder<>(annotation, constPool, this, null);
        }

        /**
         * starts an array on the annotation's member named, to be filled in through the returned
         * builder.<br>
         * An array member is the one case where the values do not go straight on, since an
         * annotation holds one array rather than several values of the same name. What comes back
         * is a builder for the array, and finishing it hands back this builder, so this builder
         * has to be finished as well: two {@code finish} calls, one for each.
         * example:
         * <pre class="language-typescript">
         * builder.addAnnotation(Reflection.getClass("java.lang.SuppressWarnings") as any)
         *   .putArray("value")
         *   .putString("unchecked")
         *   .putString("rawtypes")
         *   .finish()
         *   .finish();
         * </pre>
         *
         * @param key the name of the annotation member, which is declared as an array
         * @return a builder for the array, whose finish hands back this builder
         */
        public AnnotationArrayBuilder<AnnotationBuilder<T>> putArray(String key) {
            AnnotationArrayBuilder ab = new AnnotationArrayBuilder<>(this, constPool);
            annotationInstance.addMemberValue(key, ab.arrayMemberValue);
            return ab;
        }

        /**
         * puts the annotation on the member it was started from, and hands that member back so
         * the calls carry on.<br>
         * Nothing is on the member until this is called, and the class cannot be built while an
         * annotation is still open, so an annotation that is never finished is one that never
         * makes it onto the class. An annotation type whose retention is not one the runtime reads
         * goes on as one only the reflection library can see, which is worked out from the
         * annotation type rather than chosen here.
         * example:
         * <pre class="language-typescript">
         * // an annotation with nothing on it, which is all a marker needs
         * builder.addAnnotation(Reflection.getClass("java.lang.FunctionalInterface") as any)
         *   .finish();
         * </pre>
         *
         * @return the member the annotation was started from, which is the class builder, a field
         * builder or a method builder
         */
        public T finish() {
            if (attr != null) {
                attr.addAnnotation(annotationInstance);
            }
            return member;
        }

        /**
         * the values of an array on an annotation, put on one at a time and in the order they are
         * given.
         * <br>
         * A script does not make one of these; it is what
         * {@link AnnotationBuilder#putArray(String) putArray} hands back. There is one call per kind
         * of value, the same set as on the annotation builder, but each takes only the value since
         * the name was already given to the array. The array is only closed when
         * {@link #finish() finish} is called, and finishing it hands back the annotation builder,
         * which then has to be finished as well.
         */
        public class AnnotationArrayBuilder<U> {
            ArrayMemberValue arrayMemberValue;
            private final ConstPool constPool;
            private final List<MemberValue> mv = new ArrayList<>();
            private final U parent;

            /**
             * starts an array. A script does not call this; it is what
             * {@link AnnotationBuilder#putArray(String) putArray} calls.
             *
             * @param parent   the annotation builder to hand back when the array is finished
             * @param constPool the pool the values are written into
             */
            public AnnotationArrayBuilder(U parent, ConstPool constPool) {
                this.constPool = constPool;
                this.parent = parent;
                this.arrayMemberValue = new ArrayMemberValue(constPool);
            }

            /**
             * adds a string to the end of the array.
             *
             * @param value the value to add
             * @return self
             */
            public AnnotationArrayBuilder<U> putString(String value) {
                mv.add(new StringMemberValue(value, constPool));
                return this;
            }

            /**
             * adds a boolean to the end of the array.
             *
             * @param value the value to add
             * @return self
             */
            public AnnotationArrayBuilder<U> putBoolean(boolean value) {
                mv.add(new BooleanMemberValue(value, constPool));
                return this;
            }

            /**
             * adds a byte to the end of the array.
             *
             * @param value the value to add
             * @return self
             */
            public AnnotationArrayBuilder<U> putByte(byte value) {
                mv.add(new ByteMemberValue(value, constPool));
                return this;
            }

            /**
             * adds a character to the end of the array.
             *
             * @param value the character to add
             * @return self
             */
            public AnnotationArrayBuilder<U> putChar(char value) {
                mv.add(new CharMemberValue(value, constPool));
                return this;
            }

            /**
             * adds a short to the end of the array.
             *
             * @param value the value to add
             * @return self
             */
            public AnnotationArrayBuilder<U> putShort(short value) {
                mv.add(new ShortMemberValue(value, constPool));
                return this;
            }

            /**
             * adds an int to the end of the array.
             *
             * @param value the value to add
             * @return self
             */
            public AnnotationArrayBuilder<U> putInt(int value) {
                mv.add(new IntegerMemberValue(value, constPool));
                return this;
            }

            /**
             * adds a long to the end of the array.
             *
             * @param value the value to add
             * @return self
             */
            public AnnotationArrayBuilder<U> putLong(long value) {
                mv.add(new LongMemberValue(value, constPool));
                return this;
            }

            /**
             * adds a float to the end of the array, narrowing the value from the double a script
             * has to a float.
             *
             * @param value the value to add, narrowed to a float
             * @return self
             */
            public AnnotationArrayBuilder<U> putFloat(double value) {
                mv.add(new FloatMemberValue((float) value, constPool));
                return this;
            }

            /**
             * adds a double to the end of the array.
             *
             * @param value the value to add
             * @return self
             */
            public AnnotationArrayBuilder<U> putDouble(double value) {
                mv.add(new DoubleMemberValue(value, constPool));
                return this;
            }

            /**
             * adds a class to the end of the array, by giving the class itself rather than its
             * name as text. This is what a member declared {@code Class[]} wants.
             *
             * @param value the class to add
             * @return self
             */
            public AnnotationArrayBuilder<U> putClass(Class<?> value) {
                mv.add(new ClassMemberValue(value.getName(), constPool));
                return this;
            }

            /**
             * adds one constant of an enum to the end of the array, by giving the constant itself
             * rather than its name as text. This is what a member declared as an array of an enum
             * wants, such as {@code ElementType[]} on the target annotation.
             *
             * @param value the constant to add
             * @return self
             */
            public AnnotationArrayBuilder<U> putEnum(Enum<?> value) {
                EnumMemberValue enumMemberValue = new EnumMemberValue(constPool);
                enumMemberValue.setType(value.getDeclaringClass().getName());
                enumMemberValue.setValue(value.name());
                mv.add(enumMemberValue);
                return this;
            }

            /**
             * adds an annotation to the end of the array, so an annotation member declared as an
             * array of annotations can be built one at a time.
             * <br>
             * What comes back is a builder for that annotation, and finishing it hands back this
             * array builder. The annotation is added to the array as soon as this is called, so it
             * is in the array whatever is done with the builder afterwards, and an annotation left
             * without a value of its own is left with its defaults.
             *
             * @param annotationClass the annotation type to add
             * @return a builder for the annotation, whose finish hands back this array builder
             * @throws NotFoundException if the annotation type could not be resolved
             */
            public AnnotationBuilder<AnnotationArrayBuilder<U>> putAnnotation(Class<?> annotationClass) throws NotFoundException {
                Annotation annotation = new Annotation(constPool, defaultPool.getCtClass(annotationClass.getName()));
                mv.add(new AnnotationMemberValue(annotation, constPool));
                return new AnnotationBuilder<>(annotation, constPool, this, null);
            }

            /**
             * adds an array to the end of the array, for a member declared as an array of arrays.
             * <br>
             * The class argument is not read: the array is made with whatever its own
             * {@code put} calls say, and the same is true of the argument here. What comes back is
             * a builder for the inner array, and finishing it hands back this one, so both have to
             * be finished for the value to be complete.
             *
             * @param annotationClass not used, the inner array is described by the put calls
             * @return a builder for the inner array, whose finish hands back this array builder
             */
            public AnnotationArrayBuilder<AnnotationArrayBuilder<U>> putArray(Class<?> annotationClass) {
                AnnotationArrayBuilder ab = new AnnotationArrayBuilder<>(this, constPool);
                mv.add(ab.arrayMemberValue);
                return ab;
            }

            /**
             * closes the array, so the values are written into it, and hands back the annotation
             * builder the array was started from.<br>
             * The annotation itself is not finished by this, so an array member needs this and then
             * one more {@code finish} on the annotation. A builder left unfinished is an array the
             * annotation does not have a value for.
             * example:
             * <pre class="language-typescript">
             * builder.addAnnotation(Reflection.getClass("java.lang.SuppressWarnings") as any)
             *   .putArray("value")
             *   .putString("unchecked")
             *   .putString("rawtypes")
             *   .finish()   // closes the array
             *   .finish();  // and now puts the annotation on
             * </pre>
             *
             * @return the annotation builder the array was started from
             */
            public U finish() {
                this.arrayMemberValue.setValue((MemberValue[])this.mv.toArray(new MemberValue[0]));
                return parent;
            }

        }

    }

    /**
     * the body of a method or a constructor, written one piece at a time, mixing Java with calls
     * back into the script that is building the class.
     * <br>
     * A script does not make one of these; it is what
     * {@link MethodBuilder#buildBody() buildBody()} and {@link ConstructorBuilder#buildBody()
     * buildBody()} hand back. The pieces go in the order they are appended, and the body is only
     * written into the method when it is {@link #finish() finish}ed, so an unfinished builder has
     * a method with an empty body rather than a broken one.<br>
     * The difference from {@link MethodBuilder#guestBody(MethodWrapper) guestBody} is that this
     * allows Java around the calls, and the price is that the value a call gives back is a
     * statement rather than an expression, so it has to be put somewhere with the token before it.
     * example:
     * <pre>
     * const bump = builder.addMethod(Reflection.getClass("void"), "bump");
     * bump.makePublic();
     * bump.buildBody()
     *   .appendJavaCode("int before = this.total")
     *   .appendGuestCode(JavaWrapper.methodToJava(function (self, args) {
     *     return 1;
     *   }), "", "Object step = ")
     *   .appendJavaCode("this.total = before + ((java.lang.Integer) step).intValue()")
     *   .finish();
     * </pre>
     */
    public class BodyBuilder {
        private final CtBehavior ctBehavior;
        private final String guestName;
        private final StringBuilder body = new StringBuilder("{\n");
        private int guestCount = 0;

        private BodyBuilder(CtBehavior ctBehavior, String guestName) {
            this.ctBehavior = ctBehavior;
            this.guestName = guestName;
        }

        /**
         * adds a line of Java to the body, in the order it is called relative to the other pieces.
         * <br>
         * The semicolon is added for you, so the line is the inside of a statement rather than a
         * whole one, and a line that opens a block has no closing brace written for it. Anything a
         * {@link #appendGuestCode(MethodWrapper, String, String) guest call} in the same body
         * refers to has to be named here first, since the guest call is a statement and a
         * statement cannot be assigned from.
         * example:
         * <pre>
         * .appendJavaCode("int before = this.total")
         * .appendJavaCode("this.total = before + 1")
         * .appendJavaCode("if (this.total == 10) { this.total = 0; }")
         * </pre>
         *
         * @param code the inside of a java statement, without the semicolon
         * @return self
         */
        public BodyBuilder appendJavaCode(String code) {
            body.append(code).append(";\n");
            return this;
        }

        /**
         * adds a call into the script to the body, in the order it is called relative to the other
         * pieces.
         * <br>
         * This is what makes a generated method able to run script code. The call it writes out
         * looks the handler up in {@link #methodWrappers} by a key made of the class name, the
         * method name and the method's JVM descriptor, and each one in a body is numbered so that
         * several in one method do not collide. The handler is called with the instance the method
         * was called on and an array of the arguments, and a primitive argument has to be boxed
         * in the source that is handed in, since the array is an object array.<br>
         * The call is a statement, not an expression, so a method with a primitive return has to
         * do its own unwrapping in a line of Java, and a value that is not wanted at all can be
         * discarded by leaving the token out. The handler is a wrapper, so it wants to be made
         * with {@code JavaWrapper.methodToJava} rather than handed as a plain function.
         * example:
         * <pre>
         * // a value put on a local, then used by the java around it
         * .appendGuestCode(JavaWrapper.methodToJava(function (self, args) {
         *   return self.size();
         * }), "", "Object size = ")
         * .appendJavaCode("System.out.println(size)")
         *
         * // a primitive argument boxed, and a value thrown away
         * .appendGuestCode(JavaWrapper.methodToJava(function (self, args) {
         *   print(`a total of ${self.total}`);
         * }), "java.lang.Integer.valueOf($1)", "")
         * </pre>
         *
         * @param code          the handler, made with {@code JavaWrapper.methodToJava}
         * @param argsAsObjects java source for the arguments, comma separated with no brackets,
         *                      boxed if they are primitives
         * @param tokenBefore   ie, "return", "Object wasd = " etc
         * @return self
         */
        public BodyBuilder appendGuestCode(MethodWrapper<Object, Object, Object, ?> code, String argsAsObjects, String tokenBefore) {
            if (tokenBefore != null) {
                body.append(tokenBefore);
            }
            body.append("(((com.jsmacrosce.jsmacros.core.MethodWrapper)")
                    .append("com.jsmacrosce.jsmacros.core.library.impl.classes.ClassBuilder.methodWrappers.get(\"")
                    .append(guestName).append(": ").append(guestCount)
                    .append("\")).").append("apply").append("(")
                    .append("$0, new Object[]{")
                    .append(argsAsObjects)
                    .append("}));\n");
            methodWrappers.put(guestName + ": " + (guestCount++), code);
            return this;
        }

        /**
         * closes the body, writes everything that was appended into the method or constructor, and
         * hands back the class builder so more members can be added.
         * <br>
         * This is the step that turns the pieces into a method: the method already exists in the
         * class with an empty body, and this fills it in. A body that does not compile is a
         * {@link CannotCompileException} from here, and a builder left unfinished leaves the
         * method empty, which the runtime only accepts on an abstract method.
         *
         * @return the class builder, so more members can be added
         * @throws CannotCompileException if the assembled body does not compile
         */
        public ClassBuilder<T> finish() throws CannotCompileException {
            body.append("\n}");
            ctBehavior.setBody(body.toString());
            return ClassBuilder.this;
        }

    }

}
