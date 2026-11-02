import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Refuses Chrome DevTools Protocol evaluations that ask for side-effect-free behaviour.
 *
 * Chrome DevTools sends `throwOnSideEffect: true` for the grey inline previews it shows
 * while the user is still typing. The Chrome Inspector version JsMacrosCE embeds records
 * that flag but never checks it, so a preview can run arbitrary script code, including
 * code the user never submitted. Checking the request at the CDP dispatch boundary stops
 * the preview before it reaches the language runtime. An explicit Enter in the Console
 * sends the flag as `false`, so ordinary evaluation keeps working.
 *
 * The patch is applied to the embedded tool jar at build time instead of forking Graal so
 * the change stays small and disappears when a future inspector implements the flag.
 * `InspectServerSession.doProcessCommand` is the single dispatch point for both
 * `Debugger.evaluateOnCallFrame` and `Runtime.evaluate`.
 */
abstract class PatchChromeInspectorTask : DefaultTask() {

    /** The stock `chromeinspector-tool` jar resolved by Gradle. */
    @get:InputFile
    abstract val originalJar: RegularFileProperty

    /** The rewritten jar that is embedded in the Graal extension. */
    @get:OutputFile
    abstract val patchedJar: RegularFileProperty

    @TaskAction
    fun patch() {
        val input = originalJar.get().asFile
        val output = patchedJar.get().asFile
        output.parentFile.mkdirs()
        var classFound = false
        ZipFile(input).use { zip ->
            ZipOutputStream(output.outputStream().buffered()).use { out ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.isDirectory) {
                        continue
                    }
                    val bytes = zip.getInputStream(entry).use { it.readBytes() }
                    out.putNextEntry(ZipEntry(entry.name).apply { time = entry.time })
                    if (entry.name == TARGET_CLASS) {
                        classFound = true
                        out.write(patchClass(bytes))
                    } else {
                        out.write(bytes)
                    }
                    out.closeEntry()
                }
            }
        }
        check(classFound) { "Chrome Inspector jar does not contain $TARGET_CLASS" }
    }

    /**
     * Adds the guard at the start of [TARGET_METHOD] and leaves the original switch
     * untouched. The injected branch only reads public CDP parameter objects.
     */
    private fun patchClass(bytes: ByteArray): ByteArray {
        val reader = ClassReader(bytes)
        val writer = ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
        var patched = false
        reader.accept(object : ClassVisitor(Opcodes.ASM9, writer) {
            override fun visitMethod(
                access: Int,
                name: String,
                descriptor: String,
                signature: String?,
                exceptions: Array<out String>?
            ): MethodVisitor? {
                val delegate = super.visitMethod(access, name, descriptor, signature, exceptions)
                if (name != TARGET_METHOD || descriptor != TARGET_DESCRIPTOR) {
                    return delegate
                }
                patched = true
                return object : MethodVisitor(Opcodes.ASM9, delegate) {
                    override fun visitCode() {
                        super.visitCode()
                        val dispatch = Label()
                        emitGuard(this, dispatch)
                        visitLabel(dispatch)
                        // The original method starts with no locals beyond the parameters
                        // and an empty operand stack, so the fall-through frame is unchanged.
                        visitFrame(Opcodes.F_SAME, 0, null, 0, null)
                    }
                }
            }
        }, 0)
        check(patched) {
            "Chrome Inspector method $TARGET_METHOD$TARGET_DESCRIPTOR was not found; " +
                "update PatchChromeInspectorTask for this Graal version"
        }
        return writer.toByteArray()
    }

    private fun emitGuard(mv: MethodVisitor, dispatch: Label) {
        // if (cmd.getParams() == null) goto dispatch;
        mv.visitVarInsn(Opcodes.ALOAD, 1)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, COMMAND, "getParams", "()L$PARAMS;", false)
        mv.visitJumpInsn(Opcodes.IFNULL, dispatch)

        // JSONObject params = cmd.getParams().getJSONObject();
        // if (params == null) goto dispatch;
        mv.visitVarInsn(Opcodes.ALOAD, 1)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, COMMAND, "getParams", "()L$PARAMS;", false)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, PARAMS, "getJSONObject", "()L$JSON_OBJECT;", false)
        mv.visitJumpInsn(Opcodes.IFNULL, dispatch)

        // if (!params.optBoolean("throwOnSideEffect", false)) goto dispatch;
        mv.visitVarInsn(Opcodes.ALOAD, 1)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, COMMAND, "getParams", "()L$PARAMS;", false)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, PARAMS, "getJSONObject", "()L$JSON_OBJECT;", false)
        mv.visitLdcInsn("throwOnSideEffect")
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, JSON_OBJECT, "optBoolean", "(Ljava/lang/String;Z)Z", false)
        mv.visitJumpInsn(Opcodes.IFEQ, dispatch)

        // throw new CommandProcessException(REJECTION_MESSAGE);
        mv.visitTypeInsn(Opcodes.NEW, COMMAND_PROCESS_EXCEPTION)
        mv.visitInsn(Opcodes.DUP)
        mv.visitLdcInsn(REJECTION_MESSAGE)
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            COMMAND_PROCESS_EXCEPTION,
            "<init>",
            "(Ljava/lang/String;)V",
            false
        )
        mv.visitInsn(Opcodes.ATHROW)
    }

    private companion object {
        const val TARGET_CLASS =
            "com/oracle/truffle/tools/chromeinspector/server/InspectServerSession.class"
        const val TARGET_METHOD = "doProcessCommand"
        const val TARGET_DESCRIPTOR =
            "(Lcom/oracle/truffle/tools/chromeinspector/commands/Command;" +
                "Lcom/oracle/truffle/tools/chromeinspector/server/InspectServerSession\$CommandPostProcessor;)" +
                "Lcom/oracle/truffle/tools/chromeinspector/commands/Params;"

        const val COMMAND = "com/oracle/truffle/tools/chromeinspector/commands/Command"
        const val PARAMS = "com/oracle/truffle/tools/chromeinspector/commands/Params"
        const val COMMAND_PROCESS_EXCEPTION =
            "com/oracle/truffle/tools/chromeinspector/server/CommandProcessException"
        const val JSON_OBJECT = "org/graalvm/shadowed/org/json/JSONObject"

        const val REJECTION_MESSAGE =
            "JsMacrosCE refuses throwOnSideEffect evaluation because the inspector cannot " +
                "prove the expression is free of side effects"
    }
}
