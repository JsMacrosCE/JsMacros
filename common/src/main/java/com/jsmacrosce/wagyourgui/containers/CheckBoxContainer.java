package com.jsmacrosce.wagyourgui.containers;

import com.jsmacrosce.doclet.DocletCategory;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import com.jsmacrosce.wagyourgui.elements.Button;

import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphics;

/** A checkbox-and-label container for WagYourGUI; construction registers its checkbox with the supplied parent,
 * while the host must arrange this container's label hook separately. Activation toggles the stored state,
 * calls the optional consumer with the new boolean, then updates the checkmark after a normal callback return.
 * If the callback throws, the state has changed but the checkmark update is not reached; without a callback both updates proceed.
 * The factory takes a caller-supplied {@code IContainerParent}, native {@code Font}, native {@code Component} label
 * and JavaScript callback:
 * <pre>
 * function makeCheckBox(parent, font, label, onChanged) {
 *   const CheckBoxContainer = Java.type('com.jsmacrosce.wagyourgui.containers.CheckBoxContainer');
 *   return new CheckBoxContainer(10, 10, 140, 14, font, false, label, parent, JavaWrapper.methodToJava(onChanged));
 * }
 * </pre>
 */
@DocletCategory("Screen and UI Elements")
public class CheckBoxContainer extends MultiElementContainer<IContainerParent> {
    private boolean state;
    private Button checkBox;
    private final Consumer<Boolean> setState;
    /** Non-null native label reference read by each visible label hook; replacing it changes later label submissions,
     * not checkbox state or its checkmark. The constructor stores the supplied reference without copying. */
    public Component message;

    /** Stores the checkbox configuration and immediately initializes and registers its button with {@code parent}; no callback is invoked during construction.
     * @param x left coordinate in the GUI
     * @param y top coordinate in the GUI
     * @param width total container width, including the label region
     * @param height checkbox side length and label offset
     * @param textRenderer non-null font used for the checkbox and label
     * @param defaultState initial checked state
     * @param message non-null native component used as the label
     * @param parent non-null widget host that registers the checkbox
     * @param setState optional consumer called with the new checked state on button activation
     */
    public CheckBoxContainer(int x, int y, int width, int height, Font textRenderer, boolean defaultState, Component message, IContainerParent parent, Consumer<Boolean> setState) {
        super(x, y, width, height, textRenderer, parent);
        this.state = defaultState;
        this.message = message;
        this.setState = setState;
        this.init();
    }

    /** Clears local button tracking and registers a new checkbox at the stored position with the stored height as its side length.
     * Retains the current checked state, replaces the checkbox reference and does not invoke the state consumer.
     * Prior parent registrations are not removed; the constructor already calls this method. */
    @Override
    public void init() {
        super.init();

        checkBox = this.addRenderableWidget(new Button(x, y, height, height, textRenderer, 0, 0xFF000000, 0x7FFFFFFF, 0xFFFFFFFF, Component.literal(state ? "\u2713" : ""), btn -> {
            state = !state;
            if (setState != null) {
                setState.accept(state);
            }
            btn.setMessage(Component.literal(state ? "\u2713" : ""));
        }));
    }

    /** Moves and resizes only the checkbox to {@code (x + 1, y + 1, height - 2, height - 2)}.
     * Stored container geometry and label placement are unchanged, and {@code width} is unused.
     * Reinitialization uses the stored container geometry again.
     * @param x supplied outer left coordinate
     * @param y supplied outer top coordinate
     * @param width unused
     * @param height supplied outer height, determining the inset checkbox side
     */
    @Override
    public void setPos(int x, int y, int width, int height) {
        checkBox.setPos(x + 1, y + 1, height - 2, height - 2);
    }

    /** Submits only the current label when this container is visible, using word wrapping at
     * {@code (x + height, y + 2)} with width {@code width - height - 2} from the stored container geometry.
     * Does not submit the checkbox or forward the hook to children; the parent handles the separately registered checkbox.
     * Mouse and frame parameters are unused.
     * @param drawContext graphics context receiving the label
     * @param mouseX unused GUI mouse coordinate
     * @param mouseY unused GUI mouse coordinate
     * @param delta unused frame value
     */
    @Override
    //? if >=26.1 {
    /*public void extractRenderState(final GuiGraphicsExtractor drawContext, int mouseX, int mouseY, final float delta) {
    *///?} else {
    public void render(final GuiGraphics drawContext, int mouseX, int mouseY, final float delta) {
    //?}
        if (this.visible) {
            //? if >=26.1 {
            /*drawContext.textWithWordWrap(textRenderer, message, x + height, y + 2, width - height - 2, 0xFFFFFFFF, false);
            *///?} else {
            drawContext.drawWordWrap(textRenderer, message, x + height, y + 2, width - height - 2, 0xFFFFFFFF, false);
            //?}
        }
    }

}
