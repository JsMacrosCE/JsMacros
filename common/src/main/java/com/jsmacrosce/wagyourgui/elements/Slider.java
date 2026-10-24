package com.jsmacrosce.wagyourgui.elements;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.platform.InputConstants;
import com.jsmacrosce.doclet.DocletCategory;

import java.util.function.Consumer;

//? if <=1.21.5 {
/*import net.minecraft.client.renderer.RenderType;
import java.util.function.Function;
*///? }

//? if >1.21.8 {
/*import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}

import net.minecraft.client.gui.GuiGraphics;

/**
 * A slider widget with a value normally constrained to {@code [0.0, 1.0]}.
 * Mouse input maps across the track, and left/right keys move by one configured
 * interval. The action runs when setting the value changes it.
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Screen and UI Elements")
public class Slider extends AbstractWidget {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("widget/slider");
    private static final ResourceLocation HIGHLIGHTED_TEXTURE = ResourceLocation.parse("widget/slider_highlighted");
    private static final ResourceLocation HANDLE_TEXTURE = ResourceLocation.parse("widget/slider_handle");
    private static final ResourceLocation HANDLE_HIGHLIGHTED_TEXTURE = ResourceLocation.parse("widget/slider_handle_highlighted");

    private int steps;
    private double value;
    private final Consumer<Slider> action;

    /**
     * Creates a slider with the given number of discrete positions. Values are
     * rounded to those positions; this constructor treats counts below two as two.
     *
     * @param x left edge in screen pixels
     * @param y top edge in screen pixels
     * @param width widget width in screen pixels
     * @param height widget height in screen pixels
     * @param text displayed message
     * @param value initial value, rounded to a configured position without clamping
     * @param action called with this slider when its value changes
     * @param steps number of discrete positions
     */
    public Slider(int x, int y, int width, int height, Component text, double value, Consumer<Slider> action, int steps) {
        super(x, y, width, height, text);
        this.action = action;
        this.steps = (steps > 1 ? steps : 2) - 1;
        this.value = roundValue(value);
    }

    /**
     * Creates a slider with two positions: {@code 0.0} and {@code 1.0}.
     *
     * @param x left edge in screen pixels
     * @param y top edge in screen pixels
     * @param width widget width in screen pixels
     * @param height widget height in screen pixels
     * @param text displayed message
     * @param value initial value, rounded to a configured position without clamping
     * @param action called with this slider when its value changes
     */
    public Slider(int x, int y, int width, int height, Component text, double value, Consumer<Slider> action) {
        this(x, y, width, height, text, value, action, 2);
    }

    /**
     * Moves the value one configured interval left or right for the corresponding key.
     * The key is handled even when clamping leaves the value unchanged; other keys return false without superclass delegation.
     * Calls the overridable setValue; an action failure propagates before returning. There is no local focus, visibility or activation check.
     *
     * @param keyCode native GLFW key code to handle
     * @param scanCode unused platform scan code
     * @param modifiers unused modifier bits
     * @return {@code true} for left or right; {@code false} for other keys
     */
    //? if >1.21.8 {
    /*/^* Moves the value one configured interval left or right for the corresponding key.
     * The key is handled even when clamping leaves the value unchanged; other keys return false without superclass delegation.
     * Calls the overridable setValue; an action failure propagates before returning. There is no local focus, visibility or activation check.
     * @param keyEvent non-null native key event; only its GLFW key code is used, not its scan code or modifiers
     * @return {@code true} for left or right; {@code false} for other keys
     ^/
    *///?}
    @Override
    //? if >1.21.8 {
    /*public boolean keyPressed(KeyEvent keyEvent) {
        int keyCode = keyEvent.key();
    *///?} else {
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?}
        if (keyCode == InputConstants.KEY_LEFT) {
            setValue(value - 1D / steps);
            return true;
        } else if (keyCode == InputConstants.KEY_RIGHT) {
            setValue(value + 1D / steps);
            return true;
        }
        return false;
    }

    /**
     * Rounds a normalized value to the nearest configured interval; does not clamp it.
     *
     * @param value value to round
     * @return the value rounded to the nearest configured interval
     */
    public double roundValue(double value) {
        return (double) Math.round(value * steps) / steps;
    }

    private void setValueFromMouse(double mouseX) {
        setValue((mouseX - (double) (getX() + 4)) / (double) (width - 8));
    }

    private void applyValue() {
        action.accept(this);
    }

    /**
     * Returns the current value. The constructor rounds its initial value without clamping; {@link #setValue(double)} clamps requested values to {@code [0.0, 1.0]}.
     *
     * @return the current normalized value
     */
    public double getValue() {
        return value;
    }

    /**
     * Clamps the requested value to {@code [0.0, 1.0]}, rounds it, and notifies the action if it changed.
     *
     * @param mouseX requested normalized value
     */
    public void setValue(double mouseX) {
        double temp = value;
        value = roundValue(Mth.clamp(mouseX, 0.0D, 1.0D));
        if (temp != value) {
            applyValue();
        }
    }

    /**
     * Returns the configured number of discrete positions.
     *
     * @return the number of discrete positions
     */
    public int getSteps() {
        return steps + 1;
    }

    /**
     * Sets the number of discrete positions without changing or re-rounding the current value; use at least two.
     *
     * @param steps number of discrete positions
     */
    public void setSteps(int steps) {
        this.steps = steps - 1;
    }

    private ResourceLocation getTexture() {
        return (!this.isHovered && !this.isFocused()) ? HIGHLIGHTED_TEXTURE : TEXTURE;
    }

    private ResourceLocation getHandleTexture() {
        return (!this.isHovered && !this.isFocused()) ? HANDLE_TEXTURE : HANDLE_HIGHLIGHTED_TEXTURE;
    }

    @Override
    //? if >=26.1 {
    /*protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    *///?} else {
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
    //?}
        //? if >1.21.5 {
        RenderPipeline renderType = RenderPipelines.GUI_TEXTURED;
        //?} else {
        /*RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        Function<ResourceLocation, RenderType> renderType = RenderType::guiTextured;
        *///?}

        context.blitSprite(renderType, this.getTexture(), this.getX(), this.getY(), this.getWidth(), this.getHeight());
        context.blitSprite(renderType, this.getHandleTexture(), this.getX() + (int)(this.value * (double)(this.width - 8)), this.getY(), 8, this.getHeight());
    }

    /**
     * Sets the value from the horizontal mouse position along the slider track.
     * The position is mapped from the track inset by four GUI units on each side, then clamped and rounded;
     * the action runs only if the value changes, through the overridable setValue.
     * The calculation divides by width minus eight; this method does not validate track width or check bounds, focus, activation or visibility.
     *
     * @param mouseX mouse x-coordinate in GUI units
     * @param mouseY mouse y-coordinate in GUI units; unused
     */
    //? if >1.21.8 {
    /*/^* Sets the value from the horizontal mouse position along the slider track.
     * The position is mapped from the track inset by four GUI units on each side, then clamped and rounded;
     * the action runs only if the value changes, through the overridable setValue.
     * The calculation divides by width minus eight; this method does not validate track width or check bounds, focus, activation or visibility.
     * @param event non-null pointer event; only its x-coordinate in GUI units is used
     * @param isDoubleClick unused double-click indicator
     ^/
    *///?}
    @Override
    //? if >1.21.8 {
    /*public void onClick(MouseButtonEvent event, boolean isDoubleClick)  {
    double mouseX = event.x();
    *///?} else {
    public void onClick(double mouseX, double mouseY) {
    //?}
        setValueFromMouse(mouseX);
    }

    /**
     * Requests the widget's down sound for a release; this does not change the slider value or invoke the action.
     * A direct call does not check activation, visibility or focus; sound audibility depends on native sound handling.
     *
     * @param mouseX mouse x-coordinate in GUI units; unused
     * @param mouseY mouse y-coordinate in GUI units; unused
     */
    //? if >1.21.8 {
    /*/^* Requests the widget's down sound for a release; this does not change the slider value or invoke the action.
     * A direct call does not check activation, visibility or focus; sound audibility depends on native sound handling.
     * @param event unused pointer event
     ^/
    *///?}
    @Override
    //? if >1.21.8 {
    /*public void onRelease(MouseButtonEvent event) {
    *///?} else {
    public void onRelease(double mouseX, double mouseY) {
    //?}
        super.playDownSound(Minecraft.getInstance().getSoundManager());
    }

    @Override
    //? if >1.21.8 {
    /*protected void onDrag(MouseButtonEvent event, double deltaX, double deltaY) {
        setValueFromMouse(event.x());
        super.onDrag(event, deltaX, deltaY);
    }
    *///?} else {
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        setValueFromMouse(mouseX);
        super.onDrag(mouseX, mouseY, deltaX, deltaY);
    }
    //?}

    /**
     * Sets the displayed message as literal text.
     *
     * @param message text to display
     */
    public void setMessage(String message) {
        setMessage(Component.literal(message));
    }

    /**
     * Sets the displayed label to the supplied component without changing the slider value or action.
     * Unlike the string overload, this preserves the supplied component rather than making literal text.
     *
     * @param message non-null native component used as the displayed label
     */
    @Override
    public void setMessage(Component message) {
        super.setMessage(message);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {

    }

}
