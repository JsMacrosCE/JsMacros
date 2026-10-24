package com.jsmacrosce;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

// Inspect compiled dispatch without loading Minecraft classes or creating a rendering device.
public final class ClientCompatTest {
    private static final String MC = "net/minecraft/client/Minecraft";
    private static final String GUI = "net/minecraft/client/gui/Gui";
    private static final String RENDERER = "net/minecraft/client/renderer/GameRenderer";
    private static final String SCREEN = "net/minecraft/client/gui/screens/Screen";
    private static final String OVERLAY = "net/minecraft/client/gui/screens/Overlay";
    private static final String TOASTS = "net/minecraft/client/gui/components/toasts/ToastManager";
    private static final String CAMERA = "net/minecraft/client/Camera";
    private static final String TARGET = "com/mojang/blaze3d/pipeline/RenderTarget";
    private static int checks;

    public static void main(String[] args) throws IOException {
        ClassNode gui = read("com/jsmacrosce/jsmacros/client/util/GuiCompat");
        ClassNode render = read("com/jsmacrosce/jsmacros/client/util/RenderCompat");
        check(gui.fields.isEmpty() && render.fields.isEmpty(), "adapters must not cache client state");
        check(gui.methods.size() == 6 && render.methods.size() == 3, "only the constructor and adapter methods");

        //? if >=26.2 {
        /*String hud = "net/minecraft/client/gui/Hud";
        dispatch(gui, "screen", getter(SCREEN), field(MC, "gui", GUI), call(GUI, "screen", returns(SCREEN)));
        dispatch(gui, "setScreen", "(L" + MC + ";L" + SCREEN + ";)V", field(MC, "gui", GUI), call(GUI, "setScreen", "(L" + SCREEN + ";)V"));
        dispatch(gui, "overlay", getter(OVERLAY), field(MC, "gui", GUI), call(GUI, "overlay", returns(OVERLAY)));
        dispatch(gui, "hud", getter(hud), field(MC, "gui", GUI), field(GUI, "hud", hud));
        dispatch(gui, "toastManager", getter(TOASTS), field(MC, "gui", GUI), call(GUI, "toastManager", returns(TOASTS)));
        dispatch(render, "mainCamera", getter(CAMERA), field(MC, "gameRenderer", RENDERER), call(RENDERER, "mainCamera", returns(CAMERA)));
        dispatch(render, "mainRenderTarget", getter(TARGET), field(MC, "gameRenderer", RENDERER), call(RENDERER, "mainRenderTarget", returns(TARGET)));
        for (String name : new String[]{"screen", "overlay", "toastManager"}) {
            MethodNode method = method(gui, name);
            boolean nullBranch = false;
            boolean nullValue = false;
            for (var insn : method.instructions) {
                nullBranch |= insn.getOpcode() == Opcodes.IFNONNULL || insn.getOpcode() == Opcodes.IFNULL;
                nullValue |= insn.getOpcode() == Opcodes.ACONST_NULL;
            }
            check(nullBranch && nullValue, name + " handles the GUI's uninitialized state");
        }
        *///?} else {
        dispatch(gui, "screen", getter(SCREEN), field(MC, "screen", SCREEN));
        dispatch(gui, "setScreen", "(L" + MC + ";L" + SCREEN + ";)V", call(MC, "setScreen", "(L" + SCREEN + ";)V"));
        dispatch(gui, "overlay", getter(OVERLAY), call(MC, "getOverlay", returns(OVERLAY)));
        dispatch(gui, "hud", getter(GUI), field(MC, "gui", GUI));
        dispatch(gui, "toastManager", getter(TOASTS), call(MC, "getToastManager", returns(TOASTS)));
        dispatch(render, "mainCamera", getter(CAMERA), field(MC, "gameRenderer", RENDERER), call(RENDERER, "getMainCamera", returns(CAMERA)));
        dispatch(render, "mainRenderTarget", getter(TARGET), call(MC, "getMainRenderTarget", returns(TARGET)));
        //?}

        ClassNode icons = read("com/jsmacrosce/jsmacros/client/gui/containers/MacroContainer");
        int blendCalls = 0;
        for (MethodNode method : icons.methods) {
            for (var insn : method.instructions) {
                if (insn instanceof MethodInsnNode call && call.owner.equals("com/mojang/blaze3d/opengl/GlStateManager")
                        && (call.name.equals("_enableBlend") || call.name.equals("_disableBlend"))) {
                    blendCalls++;
                    //? if >=26.2 {
                    /*check(call.desc.equals("(I)V"), "opt-in blend overload");
                    check(insn.getPrevious().getOpcode() == Opcodes.ICONST_0, "blend adaptation selects color target zero");
                    *///?} else {
                    check(call.desc.equals("()V"), "legacy blend overload");
                    //?}
                }
            }
        }
        //? if <=1.21.5 {
        /*check(blendCalls == 6, "legacy immediate icon rendering retains blend state");
        *///?} else {
        check(blendCalls == 0, "deferred icons use pipeline blend state, not backend-specific GL calls");
        //?}
        System.out.println("ClientCompat: 7 adapters, " + checks + " bytecode checks passed");
    }

    private static void dispatch(ClassNode type, String name, String descriptor, String... expected) {
        MethodNode method = method(type, name);
        check(method.desc.equals(descriptor), name + " descriptor");
        check((method.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)) == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC), name + " is a static adapter");
        Set<String> actual = new HashSet<>();
        for (var insn : method.instructions) {
            if (insn instanceof FieldInsnNode field) {
                check(field.getOpcode() == Opcodes.GETFIELD, name + " only reads client fields");
                actual.add(field.owner + "." + field.name + ":" + field.desc);
            } else if (insn instanceof MethodInsnNode call) {
                check(call.getOpcode() == Opcodes.INVOKEVIRTUAL, name + " directly invokes the target API");
                actual.add(call.owner + "." + call.name + call.desc);
            }
            check(insn.getOpcode() != Opcodes.NEW && insn.getOpcode() != Opcodes.INVOKEDYNAMIC, name + " does not allocate or schedule work");
        }
        check(actual.equals(Set.of(expected)), name + " delegates to the exact target API: " + actual);
    }

    private static MethodNode method(ClassNode type, String name) {
        return type.methods.stream().filter(method -> method.name.equals(name)).findFirst().orElseThrow();
    }

    private static String getter(String result) {
        return "(L" + MC + ";)L" + result + ";";
    }

    private static String returns(String result) {
        return "()L" + result + ";";
    }

    private static String field(String owner, String name, String type) {
        return owner + "." + name + ":L" + type + ";";
    }

    private static String call(String owner, String name, String descriptor) {
        return owner + "." + name + descriptor;
    }

    private static ClassNode read(String name) throws IOException {
        try (var input = ClientCompatTest.class.getResourceAsStream("/" + name + ".class")) {
            if (input == null) throw new AssertionError("Missing compiled class " + name);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
