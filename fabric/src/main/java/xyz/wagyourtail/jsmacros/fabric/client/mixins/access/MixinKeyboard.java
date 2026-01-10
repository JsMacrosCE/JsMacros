package xyz.wagyourtail.jsmacros.fabric.client.mixins.access;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.wagyourtail.jsmacros.client.access.IScreenInternal;

//? if >1.21.8 {
/*import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
*///?}

@Mixin(KeyboardHandler.class)
public class MixinKeyboard {
    //? if >1.21.8 {
    /*@WrapOperation(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z"))
    private boolean onKeyPressed(Screen instance, KeyEvent keyEvent, Operation<Boolean> original) {
        ((IScreenInternal) instance).jsmacros_keyPressed(keyEvent.key(), keyEvent.scancode(), keyEvent.modifiers());
        return original.call(instance, keyEvent);
    }

    @WrapOperation(method = "charTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;charTyped(Lnet/minecraft/client/input/CharacterEvent;)Z"))
    private boolean onCharTyped1(Screen instance, CharacterEvent characterEvent, Operation<Boolean> original) {
        ((IScreenInternal) instance).jsmacros_charTyped((char) characterEvent.codepoint(), characterEvent.modifiers());
        return original.call(instance, characterEvent);
    }
    *///?} else {
    @WrapOperation(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(III)Z"))
    private boolean onKeyPressed(Screen instance, int keyCode, int scanCode, int modifiers, Operation<Boolean> original) {
        ((IScreenInternal) instance).jsmacros_keyPressed(keyCode, scanCode, modifiers);
        return original.call(instance, keyCode, scanCode, modifiers);
    }

    @WrapOperation(method = "charTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;charTyped(CI)Z"))
    private boolean onCharTyped1(Screen instance, char c, int i, Operation<Boolean> original) {
        ((IScreenInternal) instance).jsmacros_charTyped(c, i);
        return original.call(instance, c, i);
    }
    //?}
}
