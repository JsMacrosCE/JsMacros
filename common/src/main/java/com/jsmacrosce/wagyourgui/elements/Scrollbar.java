package com.jsmacrosce.wagyourgui.elements;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import com.jsmacrosce.doclet.DocletCategory;

import java.util.function.Consumer;

//? if >1.21.8 {
/*import net.minecraft.client.input.MouseButtonEvent;
*///?}

/**
 * A vertical scroll control whose callback receives the current page offset, not a normalized fraction.
 * The offset is calculated as {@code max(pages - 1, 0) * scrollAmount / scrollDistance}; geometry and page values are not validated.
 * The callback may be {@code null}, and initialization can invoke it when fewer than one page is supplied.
 * Construct the widget, then register it with the desired host separately.
 * <pre>
 * const Scrollbar = Java.type("com.jsmacrosce.wagyourgui.elements.Scrollbar");
 * function createScrollbar(x, y, width, height, onOffset) {
 *   return new Scrollbar(x, y, width, height, 0xff202020, 0xff000000, 0xff808080, 3,
 *     JavaWrapper.methodToJava(onOffset));
 * }
 * </pre>
 */
@DocletCategory("Screen and UI Elements")
public class Scrollbar extends AbstractWidget {
    protected double scrollPages = 1;
    protected double scrollAmount = 0;
    protected double scrollbarHeight;
    protected double scrollDistance;
    protected int color;
    protected int borderColor;
    protected int highlightColor;
    protected Consumer<Double> onChange;

    /**
     * Sets native geometry and raw packed-ARGB colors, stores the optional page-offset callback, then calls the overridable setScrollPages.
     * Its base implementation with fewer than one page resets the position and calls onChange before hiding and deactivating the widget.
     * If that callback throws, the later visibility and activation assignments are not reached. The constructor does not register the widget with a host.
     * @param x left edge in GUI units
     * @param y top edge in GUI units
     * @param width widget width in GUI units
     * @param height widget height in GUI units
     * @param color raw packed-ARGB background color
     * @param borderColor raw packed-ARGB border color
     * @param highlightColor raw packed-ARGB thumb color
     * @param scrollPages total page count used to initialize the control
     * @param onChange optional consumer receiving the current page offset; may be null
     */
    public Scrollbar(int x, int y, int width, int height, int color, int borderColor, int highlightColor, double scrollPages, Consumer<Double> onChange) {
        super(x, y, width, height, Component.literal(""));
        this.color = color;
        this.borderColor = borderColor;
        this.highlightColor = highlightColor;
        this.onChange = onChange;
        this.setScrollPages(scrollPages);
    }

    /** Updates geometry, recalculates thumb height and travel distance, and rescales position by the old and new travel distances.
     * Unlike setScrollPages, this calculation does not round the thumb height up. Position is not explicitly clamped; no callback is invoked and visibility and activation are unchanged.
     * @param x left edge in GUI units
     * @param y top edge in GUI units
     * @param width widget width in GUI units
     * @param height widget height in GUI units
     * @return self for chaining
     */
    public Scrollbar setPos(int x, int y, int width, int height) {
        this.setX(x);
        this.setY(y);
        this.width = width;
        this.height = height;
        this.scrollbarHeight = (height - 2) / (scrollPages + 1);
        double oldDistance = this.scrollDistance;
        this.scrollDistance = Math.max(height - 2 - this.scrollbarHeight, 1);
        this.scrollAmount = this.scrollAmount / oldDistance * scrollDistance;
        return this;
    }

    /** Sets the total page count and recalculates the thumb geometry, rounding the thumb height up.
     * Values below one reset position and call the overridable onChange before hiding and deactivating the widget; if the callback throws, those later assignments are not reached.
     * Values of one or greater show and activate it without rescaling, clamping or resetting position, or notifying the callback.
     * @param scrollPages total page count, not the number of extra pages
     */
    public void setScrollPages(double scrollPages) {
        this.scrollPages = Math.max(scrollPages - 1, 0);
        this.scrollbarHeight = (int) Math.ceil((height - 2) / Math.max(1, scrollPages));
        this.scrollDistance = Math.max(height - 2 - this.scrollbarHeight, 1);
        if (scrollPages < 1) {
            scrollAmount = 0;
            onChange();
            this.active = false;
            this.visible = false;
        } else {
            this.active = true;
            this.visible = true;
        }
    }

    /** Sets the thumb position as a fraction of its travel distance and calls the overridable onChange, even if unchanged, hidden or inactive.
     * Values outside {@code 0..1} are retained without clamping; visibility and activation are unchanged.
     * @param percent travel fraction, normally {@code 0} at the start and {@code 1} at the end
     */
    public void scrollToPercent(double percent) {
        scrollAmount = scrollDistance * percent;
        onChange();
    }

    //? if >1.21.8 {
    /*/^* Moves the thumb toward the clicked track position when active.
     * Each qualifying side of the thumb calls onChange after its own one-sided position clamp.
     * The two tests are separate, not mutually exclusive; a click within the thumb makes no change.
     * This method does not check visibility or whether the coordinates lie within the widget.
     * @param buttonEvent non-null pointer event; its y-coordinate in GUI units is read even when inactive, and its other fields are unused
     * @param doubleClick unused double-click indicator
     ^/
    @Override
    public void onClick(MouseButtonEvent buttonEvent, boolean doubleClick) {
        double mouseY = buttonEvent.y();
    *///?} else {
    /** Moves the thumb toward the clicked track position when active; the mouse X coordinate is unused.
     * Each qualifying side of the thumb calls onChange after its own one-sided position clamp. The two tests are separate, not mutually exclusive; a click within the thumb makes no change.
     * This method does not check visibility or whether the coordinates lie within the widget.
     * @param mouseX mouse X coordinate in GUI units (unused)
     * @param mouseY mouse Y coordinate in GUI units
     */
    @Override
    public void onClick(double mouseX, double mouseY) {
    //?}
        if (this.active) {
            double mpos = mouseY - getY() - 1;
            if (mpos < scrollAmount) {
                scrollAmount = Math.max(mpos - (scrollbarHeight / 2), 0);
                onChange();
            }
            if (mpos > (scrollAmount + scrollbarHeight)) {
                scrollAmount = Math.min(mpos - (scrollbarHeight / 2), scrollDistance);
                onChange();
            }
        }
    }

    /** Invokes the optional callback with the current page offset: extra pages multiplied by position and divided by travel distance.
     * A direct call does not change position and may notify even when unchanged, hidden or inactive. No finite-value or range check is performed.
     */
    public void onChange() {
        if (onChange != null) {
            onChange.accept(scrollPages * scrollAmount / scrollDistance);
        }
    }

    //? if >1.21.8 {
    /*/^* Adds the vertical drag delta to position, clamps first to the upper travel distance and then to zero, calls onChange, then delegates to the superclass.
     * There is no local activation, visibility, focus, button or bounds check.
     * The callback may run even if position is unchanged; if it throws, superclass delegation is not reached.
     * The superclass result does not report whether this method changed position.
     * @param buttonEvent non-null native pointer event; its y-coordinate is read before changing position but is unused in the local adjustment, and the event is forwarded unchanged to the superclass
     * @param deltaX horizontal drag delta, forwarded to the superclass but unused in the local adjustment
     * @param deltaY vertical drag delta in GUI units; positive values increase the position
     * @return the result returned by the superclass
     ^/
    @Override
    public boolean mouseDragged(MouseButtonEvent buttonEvent, double deltaX, double deltaY) {
        double mouseY = buttonEvent.y();
    *///?} else {
    /** Adds the vertical drag delta to position, clamps first to the upper travel distance and then to zero, calls onChange, then delegates to the superclass.
     * There is no local activation, visibility, focus, button or bounds check. The callback may run even if position is unchanged; if it throws, superclass delegation is not reached.
     * The superclass result does not report whether this method changed position.
     * @param mouseX current mouse X coordinate in GUI units, forwarded to the superclass
     * @param mouseY current mouse Y coordinate in GUI units, forwarded to the superclass
     * @param button mouse button, forwarded to the superclass
     * @param deltaX horizontal drag delta, forwarded to the superclass but unused in the local adjustment
     * @param deltaY vertical drag delta in GUI units; positive values increase the position
     * @return the result returned by the superclass
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
    //?}
        scrollAmount += deltaY;
        if (scrollAmount > scrollDistance) {
            scrollAmount = scrollDistance;
        }
        if (scrollAmount < 0) {
            scrollAmount = 0;
        }
        onChange();
        //? if >1.21.8 {
        /*return super.mouseDragged(buttonEvent, deltaX, deltaY);
        *///?} else {
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        //?}
    }

    @Override
    //? if >=26.1 {
    /*protected void extractWidgetRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
    *///?} else {
    protected void renderWidget(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
    //?}
        // mainpart
        drawContext.fill(getX() + 1, (int) (getY() + 1 + scrollAmount), getX() + width - 1, (int) (getY() + 1 + scrollAmount + scrollbarHeight), highlightColor);

        // outline and back
        drawContext.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, color);
        drawContext.fill(getX(), getY(), getX() + 1, getY() + height, borderColor);
        drawContext.fill(getX() + width - 1, getY(), getX() + width, getY() + height, borderColor);
        drawContext.fill(getX() + 1, getY(), getX() + width - 1, getY() + 1, borderColor);
        drawContext.fill(getX() + 1, getY() + height - 1, getX() + width - 1, getY() + height, borderColor);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {

    }

}
