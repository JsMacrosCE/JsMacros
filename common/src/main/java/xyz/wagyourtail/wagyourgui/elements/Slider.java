package xyz.wagyourtail.wagyourgui.elements;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Function;

//? if >1.21.8 {
/*import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}

/**
 * @author Etheradon
 * @since 1.8.4
 */
public class Slider extends AbstractWidget {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("widget/slider");
    private static final ResourceLocation HIGHLIGHTED_TEXTURE = ResourceLocation.parse("widget/slider_highlighted");
    private static final ResourceLocation HANDLE_TEXTURE = ResourceLocation.parse("widget/slider_handle");
    private static final ResourceLocation HANDLE_HIGHLIGHTED_TEXTURE = ResourceLocation.parse("widget/slider_handle_highlighted");

    private int steps;
    private double value;
    private final Consumer<Slider> action;

    public Slider(int x, int y, int width, int height, Component text, double value, Consumer<Slider> action, int steps) {
        super(x, y, width, height, text);
        this.action = action;
        this.steps = (steps > 1 ? steps : 2) - 1;
        this.value = roundValue(value);
    }

    public Slider(int x, int y, int width, int height, Component text, double value, Consumer<Slider> action) {
        this(x, y, width, height, text, value, action, 2);
    }

    @Override
    //? if >1.21.8 {
    /*public boolean keyPressed(KeyEvent keyEvent) {
        int keyCode = keyEvent.key();
    *///?} else {
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?}
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            setValue(value + (double) (1 / steps));
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            setValue(value - (double) (1 / steps));
        }
        return false;
    }

    public double roundValue(double value) {
        return (double) Math.round(value * steps) / steps;
    }

    private void setValueFromMouse(double mouseX) {
        setValue((mouseX - (double) (getX() + 4)) / (double) (width - 8));
    }

    private void applyValue() {
        action.accept(this);
    }

    public double getValue() {
        return value;
    }

    public void setValue(double mouseX) {
        double temp = value;
        value = roundValue(Mth.clamp(mouseX, 0.0D, 1.0D));
        if (temp != value) {
            applyValue();
        }
    }

    public int getSteps() {
        return steps + 1;
    }

    public void setSteps(int steps) {
        this.steps = steps - 1;
    }

    private ResourceLocation getTexture() {
        return this.isFocused() && !this.isFocused() ? HIGHLIGHTED_TEXTURE : TEXTURE;
    }

    private ResourceLocation getHandleTexture() {
        return !this.isHovered && !this.isFocused() ? HANDLE_TEXTURE : HANDLE_HIGHLIGHTED_TEXTURE;
    }

    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        //? if >1.21.5 {
        RenderPipeline renderType = RenderPipelines.GUI_TEXTURED;
        //?} else {
        /*RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        Function<ResourceLocation, RenderType> renderType = RenderType::guiTextured;
        *///?}

        context.blitSprite(renderType, this.getTexture(), this.getX(), this.getY(), this.getWidth(), this.getHeight());
        context.blitSprite(renderType, this.getHandleTexture(), this.getX() + (int)(this.value * (double)(this.width - 8)), this.getY(), 8, this.getHeight());
    }

    @Override
    //? if >1.21.8 {
    /*public void onClick(MouseButtonEvent event, boolean isDoubleClick)  {
    double mouseX = event.x();
    *///?} else {
    public void onClick(double mouseX, double mouseY) {
    //?}
        setValueFromMouse(mouseX);
    }

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

    public void setMessage(String message) {
        setMessage(Component.literal(message));
    }

    @Override
    public void setMessage(Component message) {
        super.setMessage(message);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {

    }

}
