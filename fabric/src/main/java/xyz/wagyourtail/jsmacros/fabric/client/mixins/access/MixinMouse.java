package xyz.wagyourtail.jsmacros.fabric.client.mixins.access;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.wagyourtail.jsmacros.client.access.IScreenInternal;

@Mixin(MouseHandler.class)
public class MixinMouse {

    @WrapOperation(method = "onPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(DDI)Z"))
    private boolean onMouseClicked(Screen instance, double x, double y, int button, Operation<Boolean> original) {
        ((IScreenInternal) instance).jsmacros_mouseClicked(x, y, button);
        return original.call(instance, x, y, button);
    }

    @WrapOperation(method = "onPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseReleased(DDI)Z"))
    private boolean onMouseReleased(Screen instance, double x, double y, int button, Operation<Boolean> original) {
        ((IScreenInternal) instance).jsmacros_mouseReleased(x, y, button);
        return original.call(instance, x, y, button);
    }

    @WrapOperation(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseDragged(DDIDD)Z"))
    private boolean onMouseDragged(Screen instance, double x, double y, int button, double dx, double dy, Operation<Boolean> original) {
        ((IScreenInternal) instance).jsmacros_mouseDragged(x, y, button, dx, dy);
        return original.call(instance, x, y, button, dx, dy);
    }

    @WrapOperation(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"))
    private boolean onMouseScrolled(Screen instance, double x, double y, double dx, double dy, Operation<Boolean> original) {
        ((IScreenInternal) instance).jsmacros_mouseScrolled(x, y, dx, dy);
        return original.call(instance, x, y, dx, dy);
    }

}
