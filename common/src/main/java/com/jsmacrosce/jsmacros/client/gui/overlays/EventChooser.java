package com.jsmacrosce.jsmacros.client.gui.overlays;

import com.google.common.collect.ImmutableList;
import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.wagyourgui.elements.Button;
import com.jsmacrosce.wagyourgui.elements.Scrollbar;
import com.jsmacrosce.wagyourgui.overlays.IOverlayParent;
import com.jsmacrosce.wagyourgui.overlays.OverlayContainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Displays registered event names in a five-column overlay and lets the caller confirm a selection.
 * Closing or cancelling does not invoke the callback. In this factory the caller supplies an
 * {@code IOverlayParent}, native {@code Font}, non-null initial event name and JavaScript callback:
 * <pre>
 * function showEventChooser(parent, font, selected, onSelected) {
 *   const EventChooser = Java.type('com.jsmacrosce.jsmacros.client.gui.overlays.EventChooser');
 *   const chooser = new EventChooser(10, 10, 300, 180, font, selected, parent, JavaWrapper.methodToJava(onSelected));
 *   parent.openOverlay(chooser);
 *   return chooser;
 * }
 * </pre>
 * The callback runs on confirmation only when both a selection and callback are non-null. An arbitrary non-null
 * selection can be confirmed, whether or not it is in the registered event list. Confirmation closes the
 * overlay only after the callback returns normally; a missing callback leaves the Select button without this action.
 */
@DocletCategory("Screen and UI Elements")
public class EventChooser extends OverlayContainer {
    private String selected;
    private final List<EventObj> events = new ArrayList<>();
    private int topScroll;
    private final Consumer<String> setEvent;
    private final Component eventText;

    /** Stores the overlay geometry, initial selection, parent, and optional confirmation callback; the constructor does not open the overlay or initialize its widgets.
     * @param x left edge in GUI coordinates
     * @param y top edge in GUI coordinates
     * @param width overlay width in GUI coordinates
     * @param height overlay height in GUI coordinates
     * @param textRenderer non-null font used for overlay text and buttons
     * @param selected initial event name; must be non-null if the initialized event list is nonempty, but need not name a registered event
     * @param parent non-null overlay parent
     * @param setEvent nullable callback invoked on confirmation when a selection is non-null
     */
    public EventChooser(int x, int y, int width, int height, Font textRenderer, String selected, IOverlayParent parent, Consumer<String> setEvent) {
        super(x, y, width, height, textRenderer, parent);
        this.selected = selected;
        this.setEvent = setEvent;
        this.eventText = Component.translatable("jsmacrosce.events");
    }

    /** Sets the selection and highlights every row whose event name exactly matches it; does not confirm or close the overlay.
     * @param event non-null event name to select; matching is case-sensitive
     */
    public void selectEvent(String event) {
        this.selected = event;
        for (EventObj e : events) {
            if (event.equals(e.event)) {
                e.btn.setColor(0x7FFFFFFF);
            } else {
                e.btn.setColor(0);
            }
        }
    }

    /** Initializes the overlay controls, copies and sorts the registered event names, and adds rows before applying the current selection.
     * Requires an initialized client event registry and a non-null selection when there are rows.
     * Repeated calls reset the scroll position and container button tracking but retain prior rows, so they append duplicate rows and are not a refresh operation. Previously registered parent widgets are not removed.
     */
    @Override
    public void init() {
        super.init();
        int w = width - 4;
        topScroll = y + 13;
        this.addRenderableWidget(new Button(x + width - 12, y + 2, 10, 10, textRenderer, 0, 0x7FFFFFFF, 0x7FFFFFFF, 0xFFFFFFFF, Component.literal("X"), (btn) -> {
            this.close();
        }));
        scroll = this.addRenderableWidget(new Scrollbar(x + width - 10, y + 13, 8, height - 28, 0, 0xFF000000, 0xFFFFFFFF, 2, this::onScrollbar));
        this.addRenderableWidget(new Button(x + 2, y + height - 14, w / 2, 12, textRenderer, 0, 0, 0x7FFFFFFF, 0xFFFFFFFF, Component.translatable("gui.cancel"), (btn) -> {
            this.close();
        }));
        this.addRenderableWidget(new Button(x + w / 2 + 3, y + height - 14, w / 2, 12, textRenderer, 0, 0, 0x7FFFFFFF, 0xFFFFFFFF, Component.translatable("jsmacrosce.select"), (btn) -> {
            if (this.selected != null && this.setEvent != null) {
                this.setEvent.accept(this.selected);
                this.close();
            }
        }));

        List<String> events = new ArrayList<>(JsMacrosClient.clientCore.eventRegistry.events);
        Collections.sort(events);
        for (String e : events) {
            addEvent(e);
        }
        this.selectEvent(selected);
    }

    /** Appends an event row after existing rows and updates the scrollbar range; this does not sort, deduplicate, or register the name.
     * Requires the scrollbar created by {@code init}. Uses an available translation for
     * {@code "jsmacrosce.event."} plus the name lowercased with {@code Locale.ROOT}, otherwise the literal name.
     * Adding a row does not select or highlight it, even if its name matches the current selection.
     * @param eventName non-null event name represented by the row and selected when its button is clicked
     */
    public void addEvent(String eventName) {
        String lowerCaseName = eventName.toLowerCase(Locale.ROOT);
        Component translatedName;
        if (Language.getInstance().has("jsmacrosce.event." + lowerCaseName)) {
            translatedName = Component.translatable("jsmacrosce.event." + lowerCaseName);
        } else {
            translatedName = Component.literal(eventName);
        }

        EventObj e = new EventObj(eventName, new Button(x + 3 + (events.size() % 5 * (width - 12) / 5), topScroll + (events.size() / 5 * 12), (width - 12) / 5, 12, textRenderer, 0, 0, 0x7FFFFFFF, 0xFFFFFFFF, translatedName, (btn) -> {
            selectEvent(eventName);
        }));

        e.btn.visible = topScroll + (events.size() / 5 * 12) >= y + 13 && topScroll + (events.size() / 5 * 12) <= y + height - 27;
        events.add(e);
        this.addRenderableWidget(e.btn);
        scroll.setScrollPages((Math.ceil(events.size() / 5D) * 12) / (double) Math.max(1, height - 39));
    }

    /** Repositions retained rows from the current overlay geometry and scroll offset, updating visibility from each row's top edge.
     * Rows use five columns with 12-unit row heights. Visibility checks whether each row's top is within
     * the inclusive interval from {@code y + 13} to {@code y + height - 27}, not whether its full bounds fit.
     * This does not change the scroll offset or recompute the scrollbar range.
     */
    public void updateEventPos() {
        for (int i = 0; i < events.size(); ++i) {
            EventObj e = events.get(i);
            e.btn.visible = topScroll + (i / 5 * 12) >= y + 13 && topScroll + (i / 5 * 12) <= y + height - 27;
            e.btn.setPos(x + 3 + (i % 5 * (width - 12) / 5), topScroll + (i / 5 * 12), (width - 12) / 5, 12);
        }
    }

    /** Applies an unclamped scrollbar offset and repositions rows; visibility uses row top edges, not full-row clipping.
     * Sets the row origin to {@code y + 13 - (int) (page * (height - 27))}.
     * @param page scroll offset in units of {@code height - 27} GUI coordinates, not a normalized percentage
     */
    public void onScrollbar(double page) {
        topScroll = y + 13 - (int) (page * (height - 27));
        int i = 0;
        for (EventObj fi : events) {
            fi.btn.visible = topScroll + (i / 5 * 12) >= y + 13 && topScroll + (i / 5 * 12) <= y + height - 27;
            fi.btn.setPos(x + 3 + (i % 5 * (width - 12) / 5), topScroll + (i / 5 * 12), (width - 12) / 5, 12);
            ++i;
        }
    }

    /** Submits the background, caption and separators, forwards the hook to widgets and a child overlay,
     * then submits full-label popups for tracked buttons with the stored hovering flag and truncated text.
     * Neither the inherited visibility flag nor the popup button's visibility or active flag is checked here.
     * @param drawContext context receiving the overlay and widget drawing operations
     * @param mouseX mouse x-coordinate in GUI coordinates, forwarded to widgets and used to position label popups
     * @param mouseY mouse y-coordinate in GUI coordinates, forwarded to widgets and used to position label popups
     * @param delta frame value forwarded to widgets
     */
    @Override
    //? if >=26.1 {
    /*public void extractRenderState(final GuiGraphicsExtractor drawContext, int mouseX, int mouseY, final float delta) {
    *///? } else {
    public void render(final GuiGraphics drawContext, int mouseX, int mouseY, final float delta) {
    //? }
        renderBackground(drawContext);

        //? if >=26.1 {
        /*drawContext.textWithWordWrap(textRenderer, eventText, x + 3, y + 3, width - 14, 0xFFFFFFFF, false);
        *///? } else {
        drawContext.drawWordWrap(textRenderer, eventText, x + 3, y + 3, width - 14, 0xFFFFFFFF, false);
        //? }

        drawContext.fill(x + 2, y + 12, x + width - 2, y + 13, 0xFFFFFFFF);
        drawContext.fill(x + 2, y + height - 15, x + width - 2, y + height - 14, 0xFFFFFFFF);
//        textRenderer.draw(, mouseX, mouseY, color, shadow, matrix, vertexConsumers, seeThrough, backgroundColor, light)
        //? if >=26.1 {
        /*super.extractRenderState(drawContext, mouseX, mouseY, delta);
        *///? } else {
        super.render(drawContext, mouseX, mouseY, delta);
        //? }

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
                *///? } else {
                drawContext.drawString(textRenderer, b.getMessage(), mouseX, mouseY - textRenderer.lineHeight - 1, 0xFFFFFFFF);
                //? }
            }
        }
    }

    /** Holds an event name and its associated button as a row value; it does not register the button itself.
     * This supporting type is not a global library. The caller supplies a name and WagYourGUI button:
     * <pre>
     * function makeEventRow(name, button) {
     *   const EventObj = Java.type('com.jsmacrosce.jsmacros.client.gui.overlays.EventChooser$EventObj');
     *   return new EventObj(name, button);
     * }
     * </pre>
     */
    public static class EventObj {
        String event;
        Button btn;

        /** Stores the supplied row name and button references.
         * @param event event name represented by the row
         * @param btn associated WagYourGUI button
         */
        public EventObj(String event, Button btn) {
            this.event = event;
            this.btn = btn;
        }

    }

}
