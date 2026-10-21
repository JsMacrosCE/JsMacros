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
 * Builds a Java class at runtime from source declarations or member builders. Scripts obtain one
 * through {@code Reflection.createClassBuilder}, add members, and define it with
 * {@link #finishBuildAndFreeze()}. The generated class is defined in JsMacros' proxy package;
 * {@code Reflection.getClassFromClassBuilderResult} looks it up by its generated name.
 *
 * Source declarations provide Java code directly. The typed member builders additionally support
 * configuring modifiers, exceptions, and annotations; their guest-body forms can call back into
 * the running script, unlike source method bodies. The callback body uses a key derived from the
 * generated class name, method name, and JVM descriptor.
 * Use {@link MethodBuilder#buildBody()} to mix Java statements with guest calls;
 * {@link BodyBuilder} shows how to capture a guest call's result in a Java variable.
 *
 * The returned {@link Class} is typed as its superclass, so script code may need a cast to access
 * generated members. Defining the class freezes it; it cannot then be modified or built again.
 * Compilation and resolution failures are reported by the operation that encounters them.
 *
 * <pre>
 * // Build and define a generated subclass with one Java-source method and
 * // one method that calls back into this script.
 * const builder = Reflection.createClassBuilder("Counting",
 *   Reflection.getClass("java.util.ArrayList"));
 *
 * // members can be written as java source, one declaration at a time
 * builder.addField("private int total;");
 * builder.addMethod("public int getTotal() { return this.total; }");
 *
 * // Typed member builders accept Class objects and guestBody calls back into
 * // this script; its handler receives the instance and boxed arguments.
 * const check = builder.addMethod(Reflection.getClass("boolean"), "hasItems");
 * check.makePublic();
 * check.guestBody(JavaWrapper.methodToJava(function (self, args) {
 *   return self.size() > 0;
 * }));
 *
 * // this is what freezes it, and it can only be called once
 * const CountingClass = builder.finishBuildAndFreeze();
 *
 * // The returned Class is exposed as its superclass type; JavaScript can still
 * // invoke generated members on the constructed instance.
 * const counting = Reflection.newInstance(CountingClass);
 * print(counting.getTotal());
 * print(counting.hasItems());
 * </pre>
 * @param <T> superclass type, used as the generated class's exposed type
 * @since 1.6.5
 */
@SuppressWarnings("unused")
public class ClassBuilder<T> {
    /**
     * Handlers called by generated guest-body code, indexed by generated class name, method name,
     * and JVM descriptor. {@code BodyBuilder} adds a call-specific suffix when a method contains
     * multiple guest calls. Handlers are registered as {@link MethodBuilder#guestBody(MethodWrapper)
     * guestBody} or {@link BodyBuilder#appendGuestCode(MethodWrapper, String, String) appendGuestCode}
     * builds the guest-call code.
     */
    public static final Map<String, MethodWrapper<Object, Object, Object, ?>> methodWrappers = new ConcurrentHashMap<>();
    ClassPool defaultPool = ClassPool.getDefault();
    /**
     * Javassist handle for the class being assembled. It exposes Javassist operations beyond this
     * builder's methods; the class is frozen when {@link #finishBuildAndFreeze()} defines it.
     */
    public final CtClass ctClass;
    private final String className;
    private final AnnotationsAttribute classAnnotations;
    private final AnnotationsAttribute invisibleClassAnnotations;

    /**
     * Starts a generated class extending {@code parent} and implementing the listed interfaces.
     * Dots in {@code name} become dollar signs; the generated class is placed in JsMacros' proxy
     * package, and this name is used by {@code Reflection.getClassFromClassBuilderResult} to look
     * it up. The parent and interfaces must resolve in the bytecode library's class pool or this
     * constructor throws {@link NotFoundException}.
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
     * Starts a field builder; the field is added only when its {@link FieldBuilder#end() end}
     * method is called. Use this form to configure modifiers, annotations, or an initializer.
     * Since the field type is supplied as a {@link Class}, this form does not specify a generic
     * signature; set any initial value through {@link FieldBuilder#initializer() initializer}.
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
     * Adds a field from its complete Java source declaration. The declaration may include an
     * initializer, but not annotations or generic types. Types in {@code java.lang} may be
     * unqualified, as in Java.
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
     * Starts a method builder; the method is added when one of its finishing methods is called.
     * Use this form to configure exceptions or annotations, create an abstract method, or provide
     * a body in steps or through a script handler. A method body is required unless the method is
     * ended abstract. This form does not declare generic methods or varargs.
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
     * Adds a method from its complete Java source declaration. Generic types, varargs, and
     * annotations are not supported. Types in {@code java.lang} may be unqualified, as in Java.
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
     * Starts a constructor builder; the constructor is added when one of its finishing methods is
     * called. A constructor body must call the superclass itself when needed; superclass
     * arguments are not forwarded automatically. Use {@link #addClinit()} for a static class
     * initializer, which runs when the JVM initializes the class.
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
     * Adds a constructor from its complete Java source declaration. Generic types, varargs, and
     * annotations are not supported. Types in {@code java.lang} may be unqualified, as in Java.
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
     * Starts a static class initializer, which runs when the JVM initializes the class. It has no
     * instance receiver or superclass-constructor call. Finish it with a braced Java body, a guest
     * body, a {@link BodyBuilder}, or a bytecode-building callback. A guest handler receives a
     * null receiver and an empty argument array.
     *
     * @return a builder for a static class initialiser
     */
    public ConstructorBuilder addClinit() {
        return new ConstructorBuilder(new CtClass[0], true);
    }

    /**
     * Starts building an annotation for the class. Its retention policy determines whether the
     * annotation is stored as runtime-visible or runtime-invisible metadata; it is added when
     * {@link AnnotationBuilder#finish() finish} is called, which returns this builder.
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
     * Builds a field of the class being built. The field is added only when {@link #end() end} is
     * called; use this builder to configure its name, modifiers, annotations, and initializer.
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
         * The configured field initializer, or {@code null} if none is set. It is applied when
         * {@link #end()} adds the field.
         */
        public CtField.Initializer fieldInitializer;

        /**
         * Starts a field builder for the given type and name; normally obtained through
         * {@link #addField(java.lang.Class, java.lang.String)}.
         *
         * @param fieldType the type of the field
         * @param name      the name of the field
         */
        public FieldBuilder(CtClass fieldType, String name) {
            this.fieldType = fieldType;
            this.fieldName = name;
        }

        /**
         * Adds this field to the class without modifiers, annotations, or an initializer, ignoring
         * {@code code}. Unlike {@link #end()}, this does not apply settings configured on this
         * builder.
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
         * Sets the name under which the field will be added when {@link #end()} is called.
         *
         * @param name the name to add the field under
         * @return self for chaining
         */
        public FieldBuilder rename(String name) {
            this.fieldName = name;
            return this;
        }

        /**
         * Sets private visibility while preserving the static and final flags. The visibility
         * methods replace one another; call this after toggling static or final to preserve those
         * flags, since the builder starts with no modifiers.
         *
         * @return self for chaining
         */
        public FieldBuilder makePrivate() {
            this.fieldMods = (Modifier.PRIVATE) | (this.fieldMods & Modifier.STATIC) | (this.fieldMods & Modifier.FINAL);
            return this;
        }

        /**
         * Sets public visibility while preserving the static and final flags. This replaces any
         * visibility set by another visibility method; see {@link #makePrivate()} for ordering.
         *
         * @return self for chaining
         */
        public FieldBuilder makePublic() {
            this.fieldMods = (Modifier.PUBLIC) | (this.fieldMods & Modifier.STATIC) | (this.fieldMods & Modifier.FINAL);
            return this;
        }

        /**
         * Sets protected visibility while preserving the static and final flags. This replaces any
         * visibility set by another visibility method; see {@link #makePrivate()} for ordering.
         *
         * @return self for chaining
         */
        public FieldBuilder makeProtected() {
            this.fieldMods = (Modifier.PROTECTED) | (this.fieldMods & Modifier.STATIC) | (this.fieldMods & Modifier.FINAL);
            return this;
        }

        /**
         * Resets the field to package-private visibility while keeping its static and final flags.
         *
         * @return self for chaining
         */
        public FieldBuilder makePackagePrivate() {
            this.fieldMods = (this.fieldMods & Modifier.STATIC) | (this.fieldMods & Modifier.FINAL);
            return this;
        }

        /**
         * Toggles the static modifier; calling this twice restores its original state.
         *
         * @return self for chaining
         */
        public FieldBuilder toggleStatic() {
            this.fieldMods ^= Modifier.STATIC;
            return this;
        }

        /**
         * Toggles the final modifier; calling this twice restores its original state.
         *
         * @return self for chaining
         */
        public FieldBuilder toggleFinal() {
            this.fieldMods ^= Modifier.FINAL;
            return this;
        }

        /**
         * Returns the Java modifier bit mask currently configured for this field, or zero when
         * no modifiers are set.
         *
         * @return the modifier bits set on this field
         */
        public int getMods() {
            return fieldMods;
        }

        /**
         * Returns the configured modifiers as words, or the empty string when none are set.
         *
         * @return the modifiers in words
         */
        public String getModString() {
            return Modifier.toString(fieldMods);
        }

        /**
         * Starts building an annotation for this field. Its retention policy determines whether
         * the annotation is stored as runtime-visible or runtime-invisible metadata.
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
         * Starts configuring this field's initializer. The last initializer operation performed
         * before {@link #end()} is applied when the field is added.
         *
         * @return a builder for the value, whose calls hand back the field builder so it can be
         * ended
         */
        public FieldInitializerBuilder initializer() {
            return new FieldInitializerBuilder();
        }

        /**
         * Adds the field with its configured modifiers, annotations, and initializer, then returns
         * the class builder. Unlike {@link #compile(String)}, this applies the builder settings.
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
         * Configures the value assigned to a field when it is added. Each operation replaces the
         * previous initializer; this builder is returned by {@link FieldBuilder#initializer()}.
         */
        public class FieldInitializerBuilder {

            /**
             * Sets the field initializer to an integer constant.
             *
             * @param value the value to start the field at
             * @return the field builder, so the field can be ended
             */
            public FieldBuilder setInt(int value) {
                FieldBuilder.this.fieldInitializer = CtField.Initializer.constant(value);
                return FieldBuilder.this;
            }

            /**
             * Sets the field initializer to a long constant.
             *
             * @param value the value to start the field at
             * @return the field builder, so the field can be ended
             */
            public FieldBuilder setLong(long value) {
                FieldBuilder.this.fieldInitializer = CtField.Initializer.constant(value);
                return FieldBuilder.this;
            }

            /**
             * Sets the field initializer to a float constant, narrowing the input double to float.
             *
             * @param value the value to convert to float
             * @return the field builder, so the field can be ended
             */
            public FieldBuilder setFloat(double value) {
                FieldBuilder.this.fieldInitializer = CtField.Initializer.constant((float) value);
                return FieldBuilder.this;
            }

            /**
             * Sets the field initializer to a double constant.
             *
             * @param value the value to start the field at
             * @return the field builder, so the field can be ended
             */
            public FieldBuilder setDouble(double value) {
                FieldBuilder.this.fieldInitializer = CtField.Initializer.constant(value);
                return FieldBuilder.this;
            }

            /**
             * Sets the field initializer to a char constant.
             *
             * @param value the character to start the field at
             * @return the field builder, so the field can be ended
             */
            public FieldBuilder setChar(char value) {
                FieldBuilder.this.fieldInitializer = CtField.Initializer.constant(value);
                return FieldBuilder.this;
            }

            /**
             * Sets the field initializer to a string constant.
             *
             * @param value the value to start the field at
             * @return the field builder, so the field can be ended
             */
            public FieldBuilder setString(String value) {
                FieldBuilder.this.fieldInitializer = CtField.Initializer.constant(value);
                return FieldBuilder.this;
            }

            /**
             * Sets the field initializer to a boolean constant.
             *
             * @param value the value to start the field at
             * @return the field builder, so the field can be ended
             */
            public FieldBuilder setBoolean(boolean value) {
                FieldBuilder.this.fieldInitializer = CtField.Initializer.constant(value);
                return FieldBuilder.this;
            }

            /**
             * Sets the field initializer to a byte constant.
             *
             * @param value the value to start the field at
             * @return the field builder, so the field can be ended
             */
            public FieldBuilder setByte(byte value) {
                FieldBuilder.this.fieldInitializer = CtField.Initializer.constant(value);
                return FieldBuilder.this;
            }

            /**
             * Sets the field initializer to a short constant.
             *
             * @param value the value to start the field at
             * @return the field builder, so the field can be ended
             */
            public FieldBuilder setShort(short value) {
                FieldBuilder.this.fieldInitializer = CtField.Initializer.constant(value);
                return FieldBuilder.this;
            }

            /**
             * Sets the initializer to a Java source expression, evaluated once when the field is
             * initialized. The expression is not in a constructor body and has no instance
             * available, so it cannot reference instance members.
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
             * Sets the initializer to a constructor call. Each argument is Java source for a
             * constructor parameter; the argument forms determine which overloaded constructor
             * is selected.
             *
             * @param clazz    the class to make an instance of
             * @param code_arg Java source expressions for the constructor arguments
             * @return the field builder, so the field can be ended
             * @throws NotFoundException if the class could not be resolved
             */
            public FieldBuilder initClass(Class<?> clazz, String... code_arg) throws NotFoundException {
                FieldBuilder.this.fieldInitializer = CtField.Initializer.byNewWithParams(defaultPool.getCtClass(clazz.getName()), code_arg);
                return FieldBuilder.this;
            }

            /**
             * Sets the field initializer to a call to a static method on another class. Arguments
             * are Java source expressions, not values.
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
             * Sets the field initializer to a call to a static method on the generated class.
             * Arguments are Java source expressions, not values.
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
     * Builds a method of the generated class. A script obtains one from
     * {@link #addMethod(java.lang.Class, java.lang.String, java.lang.Class[]) addMethod(Class, String, Class...)}
     * or {@link #addConstructor(java.lang.Class[]) addConstructor(Class...)}. These methods
     * configure its signature, modifiers, exceptions, annotations, and how it is added. Use
     * {@link #body(String) body(String)} for Java body source,
     * {@link #guestBody(MethodWrapper) guestBody} for a script handler,
     * {@link #buildBody() buildBody()} to combine Java statements and handler calls,
     * {@link #body(MethodWrapper) body(MethodWrapper)} to customize the Javassist representations,
     * {@link #compile(String) compile(String)} to add a complete Java method declaration, or
     * {@link #endAbstract() endAbstract()} to add an abstract method.
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
         * Creates the internal builder initialized with a method return type, name, and parameter
         * types. The class builder uses it for methods and constructors.
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
         * Adds a complete method declaration written in Java source and returns the class
         * builder. The declaration supplies the method signature and modifiers; this builder's
         * configured signature and modifiers are not used.
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
         * Makes the method private, preserving its static flag. Visibility calls replace any
         * previously selected visibility.
         *
         * @return self for chaining
         */
        public MethodBuilder makePrivate() {
            this.methodMods = (Modifier.PRIVATE) | (this.methodMods & Modifier.STATIC);
            return this;
        }

        /**
         * Makes the method public, preserving its static flag and replacing any previous
         * visibility.
         *
         * @return self for chaining
         */
        public MethodBuilder makePublic() {
            this.methodMods = (Modifier.PUBLIC) | (this.methodMods & Modifier.STATIC);
            return this;
        }

        /**
         * Makes the method protected, preserving its static flag and replacing any previous
         * visibility.
         *
         * @return self for chaining
         */
        public MethodBuilder makeProtected() {
            this.methodMods = (Modifier.PROTECTED) | (this.methodMods & Modifier.STATIC);
            return this;
        }

        /**
         * Makes the method package-private, preserving its static flag and clearing any previous
         * visibility.
         *
         * @return self for chaining
         */
        public MethodBuilder makePackagePrivate() {
            this.methodMods = (this.methodMods & Modifier.STATIC);
            return this;
        }

        /**
         * Toggles the static flag. A static method has no instance; its Java body cannot use
         * {@code this}, and a {@link #guestBody(MethodWrapper) guestBody} handler receives no
         * instance reference.
         *
         * @return self for chaining
         */
        public MethodBuilder toggleStatic() {
            this.methodMods ^= Modifier.STATIC;
            return this;
        }

        /**
         * Sets the name used when this method is added. If called more than once, the last name
         * is used.
         *
         * @param newName the name to add the method under
         * @return self for chaining
         */
        public MethodBuilder rename(String newName) {
            this.methodName = newName;
            return this;
        }

        /**
         * Sets the method's declared exception types. This records the declaration; it does not
         * throw an exception or change what the method body can throw at runtime.
         *
         * @param exceptions the exception types to declare
         * @return self for chaining
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
         * Adds the configured method with a body written as Java source and returns the class
         * builder. The argument is the method body without braces; its signature and modifiers
         * come from this builder. This form does not provide a callback into the building script;
         * use {@link #guestBody(MethodWrapper) guestBody} or {@link #buildBody() buildBody()} for
         * script callbacks. Generic methods and varargs are not supported in the body source.
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
         * Adds the configured method with a body that calls the supplied script handler. The
         * handler receives the instance followed by an array of method arguments; primitive
         * arguments are boxed, and a primitive return value is unboxed. Supply a
         * {@code JavaWrapper.methodToJava} wrapper. Use {@link #buildBody() buildBody()} to mix
         * Java statements with handler calls.
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
         * Adds the configured method with an initially empty body and returns a builder for
         * composing Java statements and script-handler calls. Handler calls are statements; use
         * the token arguments to {@code appendGuestCode} to place their result in Java source.
         * Unlike {@link #guestBody(MethodWrapper) guestBody}, this form does not automatically
         * convert a handler result to the method's return type.
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
         * Lets a script handler edit the class and method representations before the method is
         * added, then returns the class builder. The handler runs synchronously on the calling
         * thread.
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
         * Adds an abstract method with the configured parameter types, declared exceptions, and
         * method annotations. For a class, ensure the class is abstract; the method visibility
         * must also be configured as needed.
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
         * Starts an annotation for this method. The returned builder commits it when finished;
         * annotations with runtime retention are stored separately from other retention types.
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
     * Builder for a constructor or static initializer of the class being built. A script does not
     * instantiate this builder directly; it is returned by
     * {@link #addConstructor(java.lang.Class[]) addConstructor(Class...)} and
     * {@link #addClinit() addClinit}. Unlike a method, a constructor has a parameter list but no
     * return type or method name. Instance constructors may need an explicit superclass call;
     * static initializers have no
     * receiver or superclass call. The finishing calls support Java source, guest handlers, a
     * {@link BodyBuilder}, and bytecode callbacks.
     * <pre>
     * const init = builder.addConstructor(Reflection.getClass("java.lang.String"));
     * init.makePublic();
     * init.body('super($1); this.label = $1;');
     * </pre>
     */
    public class ConstructorBuilder extends MethodBuilder {
        /**
         * Creates a constructor builder for the parameter types, or a static-initializer builder
         * when {@code clInit} is true. It is called by
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
         * Adds the constructor with a Java-source body and returns the class builder. Constructor
         * parameters, visibility, and declared exceptions come from this builder; the body must
         * write any required superclass call itself.
         * <pre>
         * const init = builder.addConstructor(Reflection.getClass("java.lang.String"));
         * init.makePublic();
         * init.body('super($1); this.label = $1;');
         * </pre>
         *
         * For a static initializer, {@code code_src} is a braced body with no {@code this} or
         * superclass call.
         *
         * @param code_src the braced Java body
         * @return the class builder, so more members can be added
         * @throws CannotCompileException if the constructor does not compile
         */
        @Override
        public ClassBuilder<T> body(String code_src) throws CannotCompileException {
            if (MethodInfo.nameClinit.equals(methodName)) {
                ctClass.makeClassInitializer().setBody(code_src);
                return ClassBuilder.this;
            }
            CtConstructor constructor = CtNewConstructor.make(this.params, this.exceptions, code_src, ctClass);
            constructor.setModifiers(this.methodMods);
            constructor.getMethodInfo().addAttribute(methodAnnotations);
            constructor.getMethodInfo().addAttribute(invisibleMethodAnnotations);
            ctClass.addConstructor(constructor);
            return ClassBuilder.this;
        }

        /**
         * Adds a constructor that calls a script handler. Instance constructors must have no
         * parameters; parameterized constructors throw {@link IllegalArgumentException} and can
         * instead use {@link #body(java.lang.String) body(String)} or {@link #buildBody()}.
         * For an instance constructor, the handler receives the new instance and the generated
         * body calls a matching superclass constructor when available. A static initializer calls
         * the handler with a null receiver and empty argument array.
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
            if (MethodInfo.nameClinit.equals(methodName)) {
                String guestName = ClassBuilder.this.className + ";" + methodName + Descriptor.ofMethod(methodReturnType, params);
                ctClass.makeClassInitializer().setBody("{ ((com.jsmacrosce.jsmacros.core.MethodWrapper) "
                        + "com.jsmacrosce.jsmacros.core.library.impl.classes.ClassBuilder.methodWrappers.get(\""
                        + guestName + "\")).apply(null, new Object[]{}); }");
                methodWrappers.put(guestName, methodBody);
                return ClassBuilder.this;
            }
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
         * Adds a constructor and returns a {@link BodyBuilder} for composing Java code and script
         * calls. Unlike {@link #guestBody(MethodWrapper) guestBody}, this supports parameterized
         * constructors and lets the body write its own superclass call. Static initializer bodies
         * have no superclass call; guest code in them receives a null receiver.
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
            if (MethodInfo.nameClinit.equals(methodName)) {
                CtConstructor initializer = ctClass.makeClassInitializer();
                return new BodyBuilder(initializer, ClassBuilder.this.className + ";" + methodName + Descriptor.ofMethod(methodReturnType, params));
            }
            CtConstructor method = new CtConstructor(this.params, ctClass);
            method.setModifiers(this.methodMods);
            method.getMethodInfo().addAttribute(methodAnnotations);
            method.getMethodInfo().addAttribute(invisibleMethodAnnotations);
            String guestName = ClassBuilder.this.className + ";" + methodName + Descriptor.ofMethod(methodReturnType, params);
            ctClass.addConstructor(method);
            return new BodyBuilder(method, guestName);
        }

        /**
         * Adds a constructor whose body is supplied by a callback receiving the generated class
         * and constructor representation. The callback runs while building; it can write the
         * superclass call itself and supports parameterized constructors. For a static initializer,
         * it receives the class and initializer handles at build time.
         *
         * @param buildBody the handler, given the class and the constructor to finish, made with
         *                  {@code JavaWrapper.methodToJava}
         * @return the class builder, so more members can be added
         * @throws CannotCompileException if the constructor could not be added
         */
        @Override
        public ClassBuilder<T> body(MethodWrapper<CtClass, CtBehavior, Object, ?> buildBody) throws CannotCompileException {
            if (MethodInfo.nameClinit.equals(methodName)) {
                buildBody.apply(ctClass, ctClass.makeClassInitializer());
                return ClassBuilder.this;
            }
            CtConstructor constructor = new CtConstructor(this.params, ctClass);
            constructor.setModifiers(this.methodMods);
            constructor.getMethodInfo().addAttribute(methodAnnotations);
            constructor.getMethodInfo().addAttribute(invisibleMethodAnnotations);
            buildBody.apply(ctClass, constructor);
            ctClass.addConstructor(constructor);
            return ClassBuilder.this;
        }

        /**
         * Cannot make a constructor abstract; always throws {@link UnsupportedOperationException}.
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
     * Defines the generated class and returns its {@link Class}, typed as its superclass.
     * The class-level annotations are attached before definition. The returned type does not
     * expose generated members, so accessing them may require a cast. This freezes the builder;
     * call it once, after adding all members, since the class cannot then be modified or built again.
     * <pre class="language-typescript">
     * const CountingClass = builder.finishBuildAndFreeze();
     *
     * // typed as what it extends, so an added method needs a cast
     * const counting = Reflection.newInstance(CountingClass) as any;
     * print(counting.getTotal());
     *
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
     * Builds values for an annotation created by an {@code addAnnotation} method. Each value is
     * set by annotation-member name; {@link #finish()} attaches the annotation and returns the
     * member builder that created this one.
     * <pre>
     * builder.addAnnotation(Reflection.getClass("java.lang.SuppressWarnings"))
     *   .putArray("value")
     *   .putString("unchecked")
     *   .putString("rawtypes")
     *   .finish()
     *   .finish();
     * </pre>
     * @param <T> the builder returned by {@link #finish()}
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
         * Sets a string-valued annotation member, replacing any previous value for that name.
         *
         * @param key   the name of the annotation member
         * @param value the value to put on it
         * @return self for chaining
         */
        public AnnotationBuilder<T> putString(String key, String value) {
            annotationInstance.addMemberValue(key, new StringMemberValue(value, constPool));
            return this;
        }

        /**
         * Sets a boolean-valued annotation member, replacing any previous value for that name.
         *
         * @param key   the name of the annotation member
         * @param value the value to put on it
         * @return self for chaining
         */
        public AnnotationBuilder<T> putBoolean(String key, boolean value) {
            annotationInstance.addMemberValue(key, new BooleanMemberValue(value, constPool));
            return this;
        }

        /**
         * Sets a byte-valued annotation member, replacing any previous value for that name.
         *
         * @param key   the name of the annotation member
         * @param value the value to put on it
         * @return self for chaining
         */
        public AnnotationBuilder<T> putByte(String key, byte value) {
            annotationInstance.addMemberValue(key, new ByteMemberValue(value, constPool));
            return this;
        }

        /**
         * Sets a {@code char}-valued annotation member, replacing any previous value for that name.
         *
         * @param key   the name of the annotation member
         * @param value the character to put on it
         * @return self for chaining
         */
        public AnnotationBuilder<T> putChar(String key, char value) {
            annotationInstance.addMemberValue(key, new CharMemberValue(value, constPool));
            return this;
        }

        /**
         * Sets a {@code short}-valued annotation member, replacing any previous value for that name.
         *
         * @param key   the name of the annotation member
         * @param value the value to put on it
         * @return self for chaining
         */
        public AnnotationBuilder<T> putShort(String key, short value) {
            annotationInstance.addMemberValue(key, new ShortMemberValue(value, constPool));
            return this;
        }

        /**
         * Sets an {@code int}-valued annotation member, replacing any previous value for that name.
         *
         * @param key   the name of the annotation member
         * @param value the value to put on it
         * @return self for chaining
         */
        public AnnotationBuilder<T> putInt(String key, int value) {
            annotationInstance.addMemberValue(key, new IntegerMemberValue(value, constPool));
            return this;
        }

        /**
         * Sets a {@code long}-valued annotation member, replacing any previous value for that name.
         *
         * @param key   the name of the annotation member
         * @param value the value to put on it
         * @return self for chaining
         */
        public AnnotationBuilder<T> putLong(String key, long value) {
            annotationInstance.addMemberValue(key, new LongMemberValue(value, constPool));
            return this;
        }

        /**
         * Sets a {@code float}-valued annotation member, narrowing {@code value} to a float.
         *
         * @param key   the name of the annotation member
         * @param value the value to put on it, narrowed to a float
         * @return self for chaining
         */
        public AnnotationBuilder<T> putFloat(String key, double value) {
            annotationInstance.addMemberValue(key, new FloatMemberValue((float) value, constPool));
            return this;
        }

        /**
         * Sets a {@code double} annotation member.
         *
         * @param key   the name of the annotation member
         * @param value the value to put on it
         * @return self for chaining
         */
        public AnnotationBuilder<T> putDouble(String key, double value) {
            annotationInstance.addMemberValue(key, new DoubleMemberValue(value, constPool));
            return this;
        }

        /**
         * Sets a {@code Class}-valued annotation member using the class value rather than a class
         * name string. For a string-valued member, use {@link #putString(String, String)}.
         *
         * @param key   the name of the annotation member
         * @param value the class to put on it
         * @return self for chaining
         */
        public AnnotationBuilder<T> putClass(String key, Class<?> value) {
            annotationInstance.addMemberValue(key, new ClassMemberValue(value.getName(), constPool));
            return this;
        }

        /**
         * Sets an enum-valued annotation member from an enum constant. The member must accept that
         * enum type; class-valued members use {@link #putClass(String, Class)} instead.
         *
         * @param key   the name of the annotation member
         * @param value the constant to put on it
         * @return self for chaining
         */
        public AnnotationBuilder<T> putEnum(String key, Enum<?> value) {
            EnumMemberValue enumMemberValue = new EnumMemberValue(constPool);
            enumMemberValue.setType(value.getDeclaringClass().getName());
            enumMemberValue.setValue(value.name());
            annotationInstance.addMemberValue(key, enumMemberValue);
            return this;
        }

        /**
         * Starts building a nested annotation value. Finish the returned builder, then this builder
         * to commit the outer annotation. For an array-valued member, use
         * {@link #putArray(String)} followed by {@link AnnotationArrayBuilder#putAnnotation(Class)}.
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
         * Starts an array-valued annotation member. Finish the returned array builder, then this
         * annotation builder to commit the annotation.
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
         * Adds this annotation to its target and returns the builder for that target.
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
         * Builds an annotation array in insertion order. {@link AnnotationBuilder#putArray(String)}
         * returns this builder; call {@link #finish()} to set the accumulated values and return to
         * the parent builder.
         * @param <U> the parent builder returned by {@link #finish()}
         */
        public class AnnotationArrayBuilder<U> {
            ArrayMemberValue arrayMemberValue;
            private final ConstPool constPool;
            private final List<MemberValue> mv = new ArrayList<>();
            private final U parent;

            /**
             * Creates an empty annotation-array builder.
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
             * Appends a string value to the array.
             *
             * @param value the value to add
             * @return self for chaining
             */
            public AnnotationArrayBuilder<U> putString(String value) {
                mv.add(new StringMemberValue(value, constPool));
                return this;
            }

            /**
             * Appends a boolean value to the array.
             *
             * @param value the value to add
             * @return self for chaining
             */
            public AnnotationArrayBuilder<U> putBoolean(boolean value) {
                mv.add(new BooleanMemberValue(value, constPool));
                return this;
            }

            /**
             * Appends a byte value to the array.
             *
             * @param value the byte value to append
             * @return this array builder
             */
            public AnnotationArrayBuilder<U> putByte(byte value) {
                mv.add(new ByteMemberValue(value, constPool));
                return this;
            }

            /**
             * Appends a character value to the array.
             *
             * @param value the character value to append
             * @return this array builder
             */
            public AnnotationArrayBuilder<U> putChar(char value) {
                mv.add(new CharMemberValue(value, constPool));
                return this;
            }

            /**
             * Appends a short value to the array.
             *
             * @param value the short value to append
             * @return this array builder
             */
            public AnnotationArrayBuilder<U> putShort(short value) {
                mv.add(new ShortMemberValue(value, constPool));
                return this;
            }

            /**
             * Appends an integer value to the array.
             *
             * @param value the integer value to append
             * @return this array builder
             */
            public AnnotationArrayBuilder<U> putInt(int value) {
                mv.add(new IntegerMemberValue(value, constPool));
                return this;
            }

            /**
             * Appends a long value to the array.
             *
             * @param value the long value to append
             * @return this array builder
             */
            public AnnotationArrayBuilder<U> putLong(long value) {
                mv.add(new LongMemberValue(value, constPool));
                return this;
            }

            /**
             * Appends a float value, converting the input from {@code double} to {@code float}.
             *
             * @param value the value to convert to a float
             * @return this array builder
             */
            public AnnotationArrayBuilder<U> putFloat(double value) {
                mv.add(new FloatMemberValue((float) value, constPool));
                return this;
            }

            /**
             * Appends a double value to the array.
             *
             * @param value the double value to append
             * @return this array builder
             */
            public AnnotationArrayBuilder<U> putDouble(double value) {
                mv.add(new DoubleMemberValue(value, constPool));
                return this;
            }

            /**
             * Appends a class value to the array.
             *
             * @param value the class to add
             * @return this array builder
             */
            public AnnotationArrayBuilder<U> putClass(Class<?> value) {
                mv.add(new ClassMemberValue(value.getName(), constPool));
                return this;
            }

            /**
             * Appends an enum constant to the array.
             *
             * @param value the constant to add
             * @return this array builder
             */
            public AnnotationArrayBuilder<U> putEnum(Enum<?> value) {
                EnumMemberValue enumMemberValue = new EnumMemberValue(constPool);
                enumMemberValue.setType(value.getDeclaringClass().getName());
                enumMemberValue.setValue(value.name());
                mv.add(enumMemberValue);
                return this;
            }

            /**
             * Starts building an annotation value and appends it to the array. The returned
             * annotation builder's {@code finish} returns this array builder.
             *
             * @param annotationClass the annotation type to add
             * @return a builder for the annotation; finishing it returns this array builder
             * @throws NotFoundException if the annotation type could not be resolved
             */
            public AnnotationBuilder<AnnotationArrayBuilder<U>> putAnnotation(Class<?> annotationClass) throws NotFoundException {
                Annotation annotation = new Annotation(constPool, defaultPool.getCtClass(annotationClass.getName()));
                mv.add(new AnnotationMemberValue(annotation, constPool));
                return new AnnotationBuilder<>(annotation, constPool, this, null);
            }

            /**
             * Starts a nested annotation-value array and adds it to this array. The argument is
             * ignored; the inner array's element values are determined by its {@code put} calls.
             * Finish the inner builder, then this builder, to commit both arrays.
             *
             * @param annotationClass ignored
             * @return a builder for the inner array, whose finish hands back this array builder
             */
            public AnnotationArrayBuilder<AnnotationArrayBuilder<U>> putArray(Class<?> annotationClass) {
                AnnotationArrayBuilder ab = new AnnotationArrayBuilder<>(this, constPool);
                mv.add(ab.arrayMemberValue);
                return ab;
            }

            /**
             * Writes the accumulated values into this array and returns its parent builder. For
             * an annotation member, finish the returned annotation builder separately to commit
             * the annotation.
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
     * Builds a method or constructor body from ordered Java statements and calls back into the
     * script. {@link MethodBuilder#buildBody() buildBody()} and
     * {@link ConstructorBuilder#buildBody() buildBody()} return this builder; the body is written
     * only when {@link #finish() finish} is called. Unlike {@link MethodBuilder#guestBody(MethodWrapper)
     * guestBody}, this permits Java statements around guest calls. A guest call is emitted as a
     * statement, so use its token to place the result, such as in a local variable.
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
         * Appends Java source to the body and adds a semicolon and newline. Supply the statement
         * content without a trailing semicolon; any variables used by a later
         * {@link #appendGuestCode(MethodWrapper, String, String) guest call} must be declared here.
         * <pre>
         * .appendJavaCode("int before = this.total")
         * .appendJavaCode("this.total = before + 1")
         * .appendJavaCode("if (this.total == 10) { this.total = 0; }")
         * </pre>
         *
         * @param code the inside of a java statement, without the semicolon
         * @return self for chaining
         */
        public BodyBuilder appendJavaCode(String code) {
            body.append(code).append(";\n");
            return this;
        }

        /**
         * Appends a call to a script handler. The handler receives the instance (or {@code null}
         * for a static method or class initializer) and an object array of arguments; box primitive
         * arguments in {@code argsAsObjects}. The generated call is a statement, not an expression:
         * use {@code tokenBefore} to place its result, or leave the result unused. Pass a handler
         * wrapped with {@code JavaWrapper.methodToJava}.
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
         * @param code          the handler wrapped with {@code JavaWrapper.methodToJava}
         * @param argsAsObjects comma-separated Java source for the handler arguments, without brackets
         * @param tokenBefore   Java source to prepend to the handler call, or {@code null} for none
         * @return self for chaining
         */
        public BodyBuilder appendGuestCode(MethodWrapper<Object, Object, Object, ?> code, String argsAsObjects, String tokenBefore) {
            if (tokenBefore != null) {
                body.append(tokenBefore);
            }
            body.append("(((com.jsmacrosce.jsmacros.core.MethodWrapper)")
                    .append("com.jsmacrosce.jsmacros.core.library.impl.classes.ClassBuilder.methodWrappers.get(\"")
                    .append(guestName).append(": ").append(guestCount)
                    .append("\")).").append("apply").append("(")
                    .append(ctBehavior instanceof CtConstructor && ((CtConstructor) ctBehavior).isClassInitializer() ? "null, new Object[]{" : "$0, new Object[]{")
                    .append(argsAsObjects)
                    .append("}));\n");
            methodWrappers.put(guestName + ": " + (guestCount++), code);
            return this;
        }

        /**
         * Writes the assembled body into its method or constructor and returns the class builder.
         * Compilation failures are reported here as {@link CannotCompileException}; without this
         * call, the method or constructor retains its existing empty body.
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
