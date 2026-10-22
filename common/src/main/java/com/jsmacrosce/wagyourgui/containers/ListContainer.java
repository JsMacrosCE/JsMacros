package com.jsmacrosce.wagyourgui.containers;

import com.jsmacrosce.doclet.DocletCategory;

import com.google.common.collect.ImmutableList;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import com.jsmacrosce.wagyourgui.elements.Button;
import com.jsmacrosce.wagyourgui.elements.Scrollbar;
import com.jsmacrosce.wagyourgui.overlays.IOverlayParent;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphics;

/** A five-column list of selectable component buttons registered with a supplied widget host.
 * Call {@code init()} after construction to register the scrollbar and buttons; arrange any popup-hook invocation separately.
 * Buttons report their captured zero-based item index to the callback without closing the container. The factory
 * takes a caller-supplied {@code IOverlayParent}, native {@code Font}, {@code java.util.List} of native
 * {@code Component} choices and JavaScript callback:
 * <pre>
 * function makeList(parent, font, choices, onSelected) {
 *   const ListContainer = Java.type('com.jsmacrosce.wagyourgui.containers.ListContainer');
 *   const list = new ListContainer(10, 10, 300, 120, font, choices, parent, JavaWrapper.methodToJava(onSelected));
 *   list.init();
 *   return list;
 * }
 * </pre>
 */
@DocletCategory("Screen and UI Elements")
public class ListContainer extends MultiElementContainer<IContainerParent> {
    private final List<Component> list;
    private final List<Button> listItems = new LinkedList<>();
    private Scrollbar scroll;
    private int topScroll;
    private int selected = -1;
    /** Consumer invoked with the zero-based item index after the stored selection and hover flags are updated,
     * including when reselecting the same item. Keep it non-null before selection. Reassignment affects future calls;
     * callback failure does not roll back the updated selection or highlights.
     */
    public Consumer<Integer> onSelect;

    /** Stores the list, geometry, rendering font, widget host, and selection callback; call {@code init()} separately to register widgets.
     * The supplied component list is retained by reference; changes are read on a later {@code init()}, not applied to existing rows automatically.
     * @param x left GUI coordinate
     * @param y top GUI coordinate
     * @param width container width in GUI units
     * @param height container height in GUI units
     * @param textRenderer non-null font used to render item labels
     * @param list non-null native list of non-null component labels to create rows from
     * @param parent non-null widget host that receives registered widgets
     * @param onSelect non-null callback used when an item is selected
     */
    public ListContainer(int x, int y, int width, int height, Font textRenderer, List<Component> list, IOverlayParent parent, Consumer<Integer> onSelect) {
        super(x, y, width, height, textRenderer, parent);
        this.list = list;
        this.onSelect = onSelect;
    }

    /** Clears local widget tracking, resets the row origin to {@code y + 13} and registers a scrollbar configured for two pages,
     * then appends buttons for the retained input list. The retained rows and selected index are not cleared, and earlier
     * parent widget registrations are not removed, so repeated calls append duplicate rows rather than refresh them.
     * The scrollbar range is not adjusted to item count, and new rows are not initially visibility-filtered.
     */
    @Override
    public void init() {
        super.init();
        int w = width - 4;
        topScroll = y + 13;

        scroll = this.addRenderableWidget(new Scrollbar(x + width - 10, y + 13, 8, height - 28, 0, 0xFF000000, 0xFFFFFFFF, 2, this::onScrollbar));

        for (Component element : list) {
            addItem(element);
        }
    }

    /** Creates and registers a button at the next zero-based item index, in a five-column grid with 12-unit row heights.
     * Uses the current stored scroll origin, even before {@code init}; clicking selects the captured index.
     * This does not modify the constructor's input list, update the scrollbar range, filter visibility or select the item.
     * @param name non-null native component label for the button
     */
    public void addItem(Component name) {
        int index = listItems.size();
        listItems.add(this.addRenderableWidget(new Button(x + 3 + (index % 5 * (width - 12) / 5), topScroll + (index / 5 * 12), (width - 12) / 5, 12, textRenderer, 0, 0, 0x7FFFFFFF, 0xFFFFFFFF, name, (btn) -> setSelected(index))));
    }

    /** Selects the indexed row, updates its highlight, and invokes {@code onSelect} even when it was already selected.
     * The index is not validated before state changes; an invalid index may fail after clearing the old highlight
     * and storing the new index. The callback is not null-checked and runs after the selection and highlight updates;
     * callback failure does not roll them back. This does not close the container or scroll to the item.
     * @param index valid zero-based index into the retained item list; {@code -1} is not a deselection operation
     */
    public void setSelected(int index) {
        if (selected > -1) {
            listItems.get(selected).forceHover = false;
        }
        listItems.get(selected = index).forceHover = true;
        onSelect.accept(selected);
    }

    /** Repositions every retained row in five columns with 12-unit row heights from the origin
     * {@code y + 13 - (int) (page * (height - 27))}. Visibility checks each row's top against the inclusive
     * interval from {@code y + 13} to {@code y + height - 27}, not whether its full bounds fit.
     * This does not recompute the scrollbar range or change the selected index.
     * @param page unclamped offset in units of {@code height - 27} GUI coordinates, not a normalized percentage
     */
    public void onScrollbar(double page) {
        topScroll = y + 13 - (int) (page * (height - 27));
        int i = 0;
        for (Button btn : listItems) {
            btn.visible = topScroll + (i / 5 * 12) >= y + 13 && topScroll + (i / 5 * 12) <= y + height - 27;
            btn.setPos(x + 3 + (i % 5 * (width - 12) / 5), topScroll + (i / 5 * 12), (width - 12) / 5, 12);
            ++i;
        }
    }

    /** Submits full-label popups for tracked buttons whose stored {@code hovering} flag is set and whose label does not fit.
     * It does not submit the buttons or a background, or forward the hook to the widget host or children.
     * Neither the inherited visibility flag nor button visibility or active state is checked here;
     * {@code forceHover} alone is not the popup predicate.
     * @param drawContext graphics receiving the popup
     * @param mouseX GUI x-coordinate used to position the popup
     * @param mouseY GUI y-coordinate used to position the popup
     * @param delta unused frame value
     */
    @Override
    //? if >=26.1 {
    /*public void extractRenderState(final GuiGraphicsExtractor drawContext, int mouseX, int mouseY, final float delta) {
    *///?} else {
    public void render(final GuiGraphics drawContext, int mouseX, int mouseY, final float delta) {
    //?}
        for (AbstractWidget b : ImmutableList.copyOf(this.buttons)) {
            if (b instanceof Button && ((Button) b).hovering && ((Button) b).cantRenderAllText()) {
                // border
                int width = textRenderer.width(b.getMessage());
                drawContext.fill(mouseX - 3, mouseY, mouseX + width + 3, mouseY + 1, 0x7F7F7F7F);
                drawContext.fill(mouseX + width + 2, mouseY - textRenderer.lineHeight - 3, mouseX + width + 3, mouseY, 0x7F7F7F7F);
                drawContext.fill(mouseX - 3, mouseY - textRenderer.lineHeight - 3, mouseX - 2, mouseY, 0x7F7F7F7F);
                drawContext.fill(mouseX - 3, mouseY - textRenderer.lineHeight - 4, mouseX + width + 3, mouseY - textRenderer.lineHeight - 3, 0x7F7F7F7F);

                // fill
                drawContext.fill(mouseX - 2, mouseY - textRenderer.lineHeight - 3, mouseX + width + 2, mouseY, 0xFF000000);
                //? if >=26.1 {
                /*drawContext.text(textRenderer, b.getMessage(), mouseX, mouseY - textRenderer.lineHeight - 1, 0xFFFFFFFF);
                *///?} else {
                drawContext.drawString(textRenderer, b.getMessage(), mouseX, mouseY - textRenderer.lineHeight - 1, 0xFFFFFFFF);
                //?}
            }
        }
    }

}
