package com.jsmacrosce.jsmacros.client.access;

//? if >=26.1 {
/*import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import com.jsmacrosce.doclet.DocletIgnore;

@DocletIgnore
public interface IScreenInternal {
    //? if >=26.1 {
    /*void jsmacros_render(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta);
    *///?} else {
    void jsmacros_render(GuiGraphics drawContext, int mouseX, int mouseY, float delta);
    //?}

    void jsmacros_mouseClicked(double mouseX, double mouseY, int button);

    void jsmacros_mouseReleased(double mouseX, double mouseY, int button);

    void jsmacros_mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY);

    void jsmacros_mouseScrolled(double mouseX, double mouseY, double horiz, double vert);

    void jsmacros_keyPressed(int keyCode, int scanCode, int modifiers);

    void jsmacros_charTyped(char chr, int modifiers);

    //? if >=26.1 {
    /*// 26.1's CharacterEvent no longer carries modifiers, so derive them from the current key
    // state (shift=1, ctrl=2, alt=4, super=8).
    static int currentModifiers() {
        Minecraft mc = Minecraft.getInstance();
        int modifiers = 0;
        if (mc.hasShiftDown()) modifiers |= 1;
        if (mc.hasControlDown()) modifiers |= 2;
        if (mc.hasAltDown()) modifiers |= 4;
        if (InputConstants.isKeyDown(mc.getWindow(), 343) || InputConstants.isKeyDown(mc.getWindow(), 347)) modifiers |= 8;
        return modifiers;
    }
    *///? }

}
