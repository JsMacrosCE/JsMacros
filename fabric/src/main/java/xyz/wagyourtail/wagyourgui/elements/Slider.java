package xyz.wagyourtail.wagyourgui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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
        //context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        context.blitSprite(RenderPipelines.GUI_TEXTURED, this.getTexture(), this.getX(), this.getY(), this.getWidth(), this.getHeight());
        context.blitSprite(RenderPipelines.GUI_TEXTURED, this.getHandleTexture(), this.getX() + (int)(this.value * (double)(this.width - 8)), this.getY(), 8, this.getHeight());
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        setValueFromMouse(mouseX);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        super.playDownSound(Minecraft.getInstance().getSoundManager());
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        setValueFromMouse(mouseX);
        super.onDrag(mouseX, mouseY, deltaX, deltaY);
    }

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
