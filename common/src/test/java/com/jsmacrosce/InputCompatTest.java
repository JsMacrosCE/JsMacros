package com.jsmacrosce;

import com.jsmacrosce.jsmacros.client.util.InputCompat;
import com.mojang.blaze3d.platform.InputConstants;

//? if >1.21.8 {
/*import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
*///?}

public final class InputCompatTest {
    public static void main(String[] args) throws Exception {
        for (int code = 0; code <= 348; code++) InputCompat.legacyKey(code);
        check(InputCompat.mouseButton(InputConstants.MOUSE_BUTTON_LEFT) == 0, "left mouse button");
        check(InputCompat.mouseButton(InputConstants.MOUSE_BUTTON_RIGHT) == 1, "right mouse button");
        check(InputCompat.mouseButton(InputConstants.MOUSE_BUTTON_MIDDLE) == 2, "middle mouse button");
        check(InputCompat.legacyKey(65).getName().equals("key.keyboard.a"), "public legacy A code");
        check(InputCompat.legacyKey(0).getName().equals("key.mouse.left"), "public legacy left button");
        check(InputCompat.legacyKey(1).getName().equals("key.mouse.right"), "public legacy right button");
        check(InputCompat.legacyKey(2).getName().equals("key.mouse.middle"), "public legacy middle button");
        check(InputCompat.legacyKey(7).getName().equals("key.mouse.8"), "public legacy eighth button");
        check(InputCompat.legacyKey(340).getName().equals("key.keyboard.left.shift"), "public legacy modifier key");
        //? if >1.21.8 {
        /*unknownKeyGuard();
        *///?}
        //? if >=26.3 {
        /*check(InputCompat.mouseButton(8) == 7, "extra mouse buttons remain zero-based");
        check(InputCompat.legacyKey(330).getValue() == 99, "legacy keypad decimal maps to SDL period");
        check(InputCompat.legacyKey(314) == InputConstants.UNKNOWN, "GLFW F25 has no SDL equivalent");
        for (int nativeMods : new int[]{1, 2, 3}) check(InputCompat.modifiers(nativeMods) == 1, "both shift sides");
        for (int nativeMods : new int[]{64, 128, 192}) check(InputCompat.modifiers(nativeMods) == 2, "both control sides");
        for (int nativeMods : new int[]{256, 512, 768}) check(InputCompat.modifiers(nativeMods) == 4, "both alt sides");
        check(InputCompat.modifiers(3 | 192 | 768 | 3072 | 8192) == 15, "combined modifiers exclude lock bits");
        check(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_A).getName().equals("key.keyboard.a"), "low SDL key is a keyboard input");
        check(InputConstants.Type.MOUSE.getOrCreate(InputConstants.MOUSE_BUTTON_4).getName().equals("key.mouse.4"), "same numeric value can identify a mouse input");
        *///?} else {
        check(InputCompat.modifiers(15) == 15, "legacy modifiers remain unchanged");
        //?}
        System.out.println("InputCompat: native mouse and modifier checks passed");
    }

    // Check the callback's raw-key filter without loading Minecraft or applying Mixins.
    //? if >1.21.8 {
    /*private static void unknownKeyGuard() throws Exception {
        try (var input = InputCompatTest.class.getResourceAsStream(
                "/com/jsmacrosce/jsmacros/client/mixin/events/MixinKeyboard.class")) {
            check(input != null, "compiled keyboard mixin is available");
            ClassNode mixin = new ClassNode();
            new ClassReader(input).accept(mixin, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            var handler = mixin.methods.stream().filter(method -> method.name.equals("keyPress")).findFirst().orElseThrow();
            for (var instruction : handler.instructions) {
                if (!(instruction instanceof MethodInsnNode call)) continue;
                check(!call.name.equals("getKey"), "reject raw -1 before converting the key");
                if (!call.owner.equals("net/minecraft/client/input/KeyEvent") || !call.name.equals("key")) continue;
                var constant = nextInstruction(call);
                var branch = nextInstruction(constant);
                check(constant.getOpcode() == Opcodes.ICONST_M1
                                && branch instanceof JumpInsnNode jump && jump.getOpcode() == Opcodes.IF_ICMPEQ
                                && nextInstruction(jump.label).getOpcode() == Opcodes.RETURN,
                        "raw -1 exits before key dispatch, including when a scancode is present");
                return;
            }
            throw new AssertionError("Keyboard callback has no raw-key guard");
        }
    }

    private static AbstractInsnNode nextInstruction(AbstractInsnNode instruction) {
        do {
            instruction = instruction.getNext();
        } while (instruction != null && instruction.getOpcode() < 0);
        if (instruction == null) throw new AssertionError("Missing keyboard guard instruction");
        return instruction;
    }
    *///?}

    private static void check(boolean success, String message) {
        if (!success) throw new AssertionError(message);
    }
}
