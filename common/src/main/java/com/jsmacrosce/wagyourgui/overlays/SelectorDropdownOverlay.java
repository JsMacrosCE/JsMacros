package com.jsmacrosce.wagyourgui.overlays;

import com.jsmacrosce.doclet.DocletCategory;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import com.mojang.blaze3d.platform.InputConstants;

import com.jsmacrosce.wagyourgui.elements.Button;
import com.jsmacrosce.wagyourgui.elements.Scrollbar;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphics;

//? if >1.21.8 {
/*import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}

/** A component-choice overlay with optional scrolling and a callback reporting the chosen zero-based index.
 * Keyboard confirmation can report -1 when nothing is selected. Construct it with a font, component collection and host; opening registers its controls.
 * <pre>
 * function openChoices(parent, font, choices, onChoice) {
 *   const Dropdown = Java.type("com.jsmacrosce.wagyourgui.overlays.SelectorDropdownOverlay");
 *   const overlay = new Dropdown(10, 10, 160, 100, choices, font, parent,
 *     JavaWrapper.methodToJava(onChoice));
 *   parent.openOverlay(overlay);
 *   return overlay;
 * }
 * </pre>
 */
@DocletCategory("Screen and UI Elements")
public class SelectorDropdownOverlay extends OverlayContainer {
    private final int lineHeight;
    private final Collection<Component> choices;
    private final List<Button> scrollChoices = new LinkedList<>();
    private final Consumer<Integer> onChoice;
    protected int selected = -1;
    private final double pages;

    /** Retains the choices collection and caches row height and page count from the font, collection size and geometry.
     * Does not initialize or open the overlay, register controls, or invoke the callback. Later collection changes do not refresh the cached metrics.
     * @param x left edge in GUI coordinates
     * @param y top edge in GUI coordinates
     * @param width overlay width in GUI units
     * @param height overlay height in GUI units; values greater than four are needed for meaningful page calculation
     * @param choices non-null collection of non-null native components, displayed in iteration order
     * @param textRenderer non-null font used to size and render choices
     * @param parent non-null overlay host
     * @param onChoice optional callback receiving the zero-based choice index, or -1 on keyboard confirmation without a selection; may be null
     */
    public SelectorDropdownOverlay(int x, int y, int width, int height, Collection<Component> choices, Font textRenderer, IOverlayParent parent, Consumer<Integer> onChoice) {
        super(x, y, width, height, textRenderer, parent);
        this.choices = choices;
        this.lineHeight = textRenderer.lineHeight + 1;
        this.onChoice = onChoice;
        this.pages = lineHeight * choices.size() / (height - 4F);
    }

    /** Registers a scrollbar when the cached page count exceeds one, then buttons for the current choices in iteration order.
     * A choice button invokes the optional callback before closing; if it throws, closing is not reached.
     * Only the base local widget list is cleared. Reinitialization appends to the retained choice-button list without removing earlier host registrations or resetting selection; cached metrics and an old scroll reference are not reset.
     * When scrolling is enabled, this pass finishes by laying out the retained choice buttons at offset zero.
     */
    @Override
    public void init() {
        super.init();
        if (pages > 1) {
            this.scroll = addRenderableWidget(new Scrollbar(x + width - 8, y, 8, height, 0, 0xFF000000, 0xFFFFFFFF, pages, this::onScroll));
        }
        int pos = 0;
        int scrollwidth = pages <= 1 ? width - 4 : width - 10;
        for (Component choice : choices) {
            final int finalPos = pos;
            Button ch = this.addRenderableWidget(new Button(x + 2, y + pos * lineHeight + 2, scrollwidth, lineHeight, textRenderer, 0, 0xFF000000, 0x4FFFFFFF, 0xFFFFFFFF, choice, (b) -> {
                if (onChoice != null) {
                    onChoice.accept(finalPos);
                }
                close();
            }));
            ch.horizCenter = false;
            scrollChoices.add(ch);
            pos += 1;
        }

        if (pages > 1) {
            onScroll(0);
        }
    }

    /** Positions all retained choice buttons for a page offset, hiding rows unless both edges lie strictly inside the overlay's inner bounds.
     * Sets row width to overlay width minus ten. The offset is not clamped, and this does not change selection or invoke the choice callback.
     * @param page page offset, not a fraction or percentage; displacement is page multiplied by overlay height in GUI units
     */
    public void onScroll(double page) {
        int pos = 0;
        for (Button btn : scrollChoices) {
            int height = (int) (y + pos * lineHeight - page * this.height + 2);
            btn.visible = height > y + 1 && height + lineHeight < y + this.height - 1;
            btn.setPos(x + 2, height, width - 10, lineHeight);
            pos += 1;
        }
    }

    /** Requests closing when the pointer is strictly outside the overlay bounds; points on the border do not close it.
     * Does not forward the click to a child overlay or invoke the choice callback itself.
     * @param mouseX pointer x-coordinate in GUI coordinates
     * @param mouseY pointer y-coordinate in GUI coordinates
     * @param button unused mouse-button value
     */
    //? if >1.21.8 {
    /*/^* Requests closing when the pointer is strictly outside the overlay bounds; points on the border do not close it.
     * Does not forward the click to a child overlay or invoke the choice callback itself.
     * @param buttonEvent non-null pointer event supplying x/y in GUI coordinates; its button value is unused
     * @param doubleClick unused double-click indicator
     ^/
    *///?}
    @Override
    //? if >1.21.8 {
    /*public void onClick(MouseButtonEvent buttonEvent, boolean doubleClick) {
        double mouseX = buttonEvent.x();
        double mouseY = buttonEvent.y();
    *///?} else {
    public void onClick(double mouseX, double mouseY, int button) {
    //?}
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            close();
        }
    }

    /** Moves forced-hover highlighting from the old retained button to the clamped new selection, or clears it for -1.
     * Hidden and previously retained buttons can be selected; this does not invoke the callback, close the overlay, change native focus, or scroll the selection into view.
     * @param sel zero-based retained-button index, or -1 to clear; values are clamped to -1 through the retained list's last index
     */
    public void setSelected(int sel) {
        if (selected != -1) {
            scrollChoices.get(selected).forceHover = false;
        }
        selected = Mth.clamp(sel, -1, scrollChoices.size() - 1);
        if (selected != -1) {
            scrollChoices.get(selected).forceHover = true;
        }
    }

    /** Handles up/down selection and enter/tab confirmation; confirmation invokes the optional callback with the current selection, including -1, then closes.
     * If the callback throws, closing and the return are not reached. Down is handled even when selection cannot change; up is not handled when nothing is selected.
     * Other keys return false without superclass or child delegation. There is no local focus, activation or visibility check, and changing selection does not scroll it into view.
     * @param keyCode native GLFW key code
     * @param scanCode unused scan code
     * @param modifiers unused modifier flags
     * @return true for down, enter or tab, and for up when a selection exists; false otherwise
     */
    //? if >1.21.8 {
    /*/^* Handles up/down selection and enter/tab confirmation; confirmation invokes the optional callback with the current selection, including -1, then requests closing.
     * If the callback throws, closing and the return are not reached. Down is handled even when selection cannot change; up is not handled when nothing is selected.
     * Other keys return false without superclass or child delegation. There is no local focus, activation or visibility check, and changing selection does not scroll it into view.
     * @param keyEvent non-null native key event; only its GLFW key code is used, not its scan code or modifiers
     * @return true for down, enter or tab, and for up when a selection exists; false otherwise
     ^/
    *///?}
    @Override
    //? if >1.21.8 {
    /*public boolean keyPressed(KeyEvent keyEvent) {
        switch (keyEvent.key()) {
    *///?} else {
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
    //?}
            case InputConstants.KEY_UP:
                if (selected == -1) {
                    return false;
                }
                setSelected(selected - 1);
                return true;
            case InputConstants.KEY_DOWN:
                setSelected(selected + 1);
                return true;
            case InputConstants.KEY_RETURN:
            case InputConstants.KEY_TAB:
                if (onChoice != null) {
                    onChoice.accept(selected);
                }
                close();
                return true;
            default:
                return false;
        }
    }

    /** Submits the overlay background, then delegates rendering of current tracked widgets and the direct child overlay.
     * Does not check the stored container visibility flag or recalculate layout; widgets apply their own rendering behavior.
     * @param drawContext non-null native graphics context
     * @param mouseX pointer x-coordinate in GUI coordinates
     * @param mouseY pointer y-coordinate in GUI coordinates
     * @param delta frame value forwarded to the superclass
     */
    @Override
    //? if >=26.1 {
    /*public void extractRenderState(final GuiGraphicsExtractor drawContext, int mouseX, int mouseY, final float delta) {
    *///? } else {
    public void render(final GuiGraphics drawContext, int mouseX, int mouseY, final float delta) {
    //? }
        renderBackground(drawContext);
        //? if >=26.1 {
        /*super.extractRenderState(drawContext, mouseX, mouseY, delta);
        *///? } else {
        super.render(drawContext, mouseX, mouseY, delta);
        //? }
    }

}
