package com.jsmacrosce.jsmacros.client.api.helper.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.render.IScreen;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinTextFieldWidget;
import com.jsmacrosce.jsmacros.core.MethodWrapper;

//? if <26.1 {
import java.util.Objects;
//? }
import java.util.concurrent.Semaphore;
import java.util.function.Predicate;

/**
 * a text field: a box the player types into, which is the game's own edit box wrapped up
 * with a change callback.
 * <p>
 * The two things to know about the callback are that it takes the text rather than the
 * field, and that it is wired up when the field is built rather than when the action is
 * set on the builder. On this version of the game the field itself no longer has a
 * filter, so the callback also enforces whatever predicate a script has set through
 * {@link #setTextPredicate(MethodWrapper)}: text the predicate turns down is put back to
 * the last value that was accepted, along with the cursor and the selection.
 * <p>
 * Setting the text from a script runs the callback too, because the game fires it for
 * any change to the field rather than only for a keystroke.
 * example:
 * <pre>
 * const screen = Hud.createScreen("a screen", true);
 * const field = screen.textFieldBuilder()
 *    .pos(10, 20)
 *    .size(120, 14)
 *    .message("type a name")
 *    .action(JavaWrapper.methodToJava(function (text) {
 *      // the callback is given the text, not the field
 *      Chat.log(`so far: ${text}`);
 *    }))
 *    .build();
 * Hud.openScreen(screen);
 *
 * // setting the text from a script runs the callback as well
 * field.setText("hello");
 * // and a predicate puts back anything it turns down
 * field.setTextPredicate(JavaWrapper.methodToJava(function (text) {
 *   return 8 >= String(text).length;
 * }));
 * field.setText("far too long to be allowed");
 * Chat.log(`the field still says ${field.getText()}`);
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.0.5
 */
@DocletCategory("Screen and UI Elements")
@SuppressWarnings("unused")
public class TextFieldWidgetHelper extends ClickableWidgetHelper<TextFieldWidgetHelper, EditBox> {
    @Nullable
    private Predicate<String> textFilter;
    @Nullable
    private MethodWrapper<String, IScreen, Object, ?> action;
    @Nullable
    private IScreen actionScreen;
    private String lastAcceptedText = "";
    private int lastAcceptedCursor;
    private int lastAcceptedHighlight;
    private boolean restoringText;

    public TextFieldWidgetHelper(EditBox t) {
        super(t);
    }

    public TextFieldWidgetHelper(EditBox t, int zIndex) {
        super(t, zIndex);
    }

    /**
     * whatever is in the field right now, as a plain string with no styling on it. A
     * field nothing has been typed into gives an empty string rather than {@code null}.
     * <p>
     * This is the field's own value, so it is what a player has typed; the greyed out
     * hint the field is drawn with when it is empty is a separate thing and is not in
     * this.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   for (const field of screen.getTextFields()) {
     *     if (String(field.getText()).length > 0) {
     *       Chat.log(`a field says "${field.getText()}"`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the currently entered {@link String String}.
     * @since 1.0.5
     */
    public String getText() {
        return base.getValue();
    }

    /**
     * replaces what is in the field, which is the same as calling the other form with
     * {@code true}. This waits for the change to be applied, so reading the field
     * afterwards gives the new text.
     * <p>
     * The change runs the game's own responder, so the callback given to the builder
     * fires here as well as on a keystroke, and a predicate set through
     * {@link #setTextPredicate(MethodWrapper)} is applied to the new text: text the
     * predicate turns down is put back to the last value it accepted.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const field = screen.textFieldBuilder()
     *    .pos(10, 20)
     *    .size(120, 14)
     *    .build();
     * Hud.openScreen(screen);
     * // typing into it from a script runs the callback as well
     * field.setText("hello");
     * Chat.log(`the field says ${field.getText()}`);
     * </pre>
     *
     * @param text the text to put in the field
     * @return self for chaining.
     * @throws InterruptedException if the thread is interrupted while waiting for the
     * change to be applied
     * @since 1.0.5
     */
    public TextFieldWidgetHelper setText(String text) throws InterruptedException {
        setText(text, true);
        return this;
    }

    /**
     * set the currently entered {@link String String}, with a choice about whether to
     * wait for it.
     * <p>
     * When called from the client thread there is nothing to hand off and nothing to
     * wait for, so the {@code await} flag makes no difference there. Off the client
     * thread the change is queued onto it, and the flag decides whether this call blocks
     * until its own task has run. Blocking also waits out whatever else was queued
     * ahead of it.
     * <p>
     * The change runs the game's own responder, so the callback given to the builder
     * fires for a scripted change as well as for a keystroke, and a predicate set
     * through {@link #setTextPredicate(MethodWrapper)} is applied to the new text.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const field = screen.textFieldBuilder()
     *    .pos(10, 20)
     *    .size(120, 14)
     *    .build();
     * Hud.openScreen(screen);
     * // queue the change and carry on without waiting for it
     * field.setText("hello", false);
     * Chat.log("the change has been queued");
     * </pre>
     *
     * @param text the text to put in the field
     * @param await whether to wait for the change to be applied
     * @return self for chaining.
     * @throws InterruptedException if the thread is interrupted while waiting for the
     * change to be applied
     * @since 1.3.1
     */
    public TextFieldWidgetHelper setText(String text, boolean await) throws InterruptedException {
        if (JsMacrosClient.clientCore.profile.checkJoinedThreadStack()) {
            base.setValue(text);
        } else {
            final Semaphore waiter = new Semaphore(await ? 0 : 1);
            Minecraft.getInstance().execute(() -> {
                base.setValue(text);
                waiter.release();
            });
            waiter.acquire();
        }
        return this;
    }

    /**
     * sets the colour the text is drawn in while the field can be edited. The colour is
     * a packed rgb number rather than a name, and it is the field's own editable colour
     * rather than the one it falls back to when it cannot.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const field = screen.textFieldBuilder()
     *    .pos(10, 20)
     *    .size(120, 14)
     *    .build();
     * Hud.openScreen(screen);
     * // bright red while it can be typed into
     * field.setEditableColor(0xFFFF0000);
     * </pre>
     *
     * @param color the packed rgb colour to draw editable text in
     * @return self for chaining.
     * @since 1.0.5
     */
    public TextFieldWidgetHelper setEditableColor(int color) {
        base.setTextColor(color);
        return this;
    }

    /**
     * sets whether the field can be typed into. A field that cannot is drawn in its
     * uneditable colour and ignores keys, so a script that wants to hold a field still
     * while something is in progress turns this off and turns it back on afterwards.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const field = screen.textFieldBuilder()
     *    .pos(10, 20)
     *    .size(120, 14)
     *    .build();
     * Hud.openScreen(screen);
     * // hold it still
     * field.setEditable(false);
     * Chat.log(`editable: ${field.isEditable()}`);
     * // and let go again
     * field.setEditable(true);
     * </pre>
     *
     * @param edit whether the field should be editable
     * @return self for chaining.
     * @since 1.0.5
     */
    public TextFieldWidgetHelper setEditable(boolean edit) {
        base.setEditable(edit);
        return this;
    }

    /**
     * @return {@code true} if the text field is editable, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isEditable() {
        return ((MixinTextFieldWidget) base).getEditable();
    }

    /**
     * sets the colour the text is drawn in when the field cannot be edited, which is the
     * other half of the pair this and the editable colour make. A packed rgb number,
     * like the editable one.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const field = screen.textFieldBuilder()
     *    .pos(10, 20)
     *    .size(120, 14)
     *    .build();
     * Hud.openScreen(screen);
     * // dark grey once it has been held still
     * field.setUneditableColor(0xFF555555);
     * field.setEditable(false);
     * </pre>
     *
     * @param color the packed rgb colour to draw uneditable text in
     * @return self for chaining.
     * @since 1.0.5
     */
    public TextFieldWidgetHelper setUneditableColor(int color) {
        base.setTextColorUneditable(color);
        return this;
    }

    /**
     * the text currently highlighted in the field, which is the part between the cursor
     * and the other end of the selection. A field with nothing selected gives an empty
     * string rather than the whole of what is in it, and a field with no text at all
     * gives an empty string too.
     * <p>
     * There is no setter for the selection on this class beyond
     * {@link #setSelection(int, int)}, so a script reads the highlight and sets it
     * rather than the other way round.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen !== null) {
     *   for (const field of screen.getTextFields()) {
     *     const selected = field.getSelectedText();
     *     if (String(selected).length > 0) {
     *       Chat.log(`"${field.getText()}" has "${selected}" selected`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the selected text.
     * @since 1.8.4
     */
    public String getSelectedText() {
        return base.getHighlighted();
    }

    /**
     * sets the greyed out hint the field is drawn with when it is empty, which is what
     * tells a player what to type there. It is a hint and not a value: it is not what
     * {@link #getText()} gives back, and it is not in the field once anything is typed
     * into it.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const field = screen.textFieldBuilder()
     *    .pos(10, 20)
     *    .size(120, 14)
     *    .message("type a name")
     *    .build();
     * Hud.openScreen(screen);
     * // the hint, which the field is drawn with while it is empty
     * field.setSuggestion("a name is required");
     * // a hint is not a value
     * Chat.log(`the field still says "${field.getText()}"`);
     * </pre>
     *
     * @param suggestion the suggestion to set
     * @return self for chaining.
     * @since 1.8.4
     */
    public TextFieldWidgetHelper setSuggestion(String suggestion) {
        base.setSuggestion(suggestion);
        return this;
    }

    /**
     * the longest the field will let itself be, counted in characters. The game caps
     * what can be typed at this, so a script that sets a longer text gets it cut to
     * this many characters.
     * <p>
     * A length of zero is the game's own way of saying the field has no cap, so a
     * script that sets zero is asking for an unlimited field rather than for one nothing
     * can be typed into.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const field = screen.textFieldBuilder()
     *    .pos(10, 20)
     *    .size(120, 14)
     *    .build();
     * Hud.openScreen(screen);
     * Chat.log(`the field takes up to ${field.getMaxLength()} characters`);
     * </pre>
     *
     * @return the maximum length of this text field.
     * @since 1.8.4
     */
    public int getMaxLength() {
        return ((MixinTextFieldWidget) base).getMaxLength();
    }

    /**
     * sets the longest the field will let itself be, counted in characters. Anything
     * already in the field that is over the new length is cut back to it, so a shorter
     * limit takes effect on what is already there rather than waiting for the next
     * keystroke.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const field = screen.textFieldBuilder()
     *    .pos(10, 20)
     *    .size(120, 14)
     *    .build();
     * Hud.openScreen(screen);
     * field.setMaxLength(16);
     * // anything longer than this is cut back by the game
     * field.setText("this is much longer than sixteen characters");
     * Chat.log(`the field now says "${field.getText()}"`);
     * </pre>
     *
     * @param length the new maximum length
     * @return self for chaining.
     * @since 1.8.4
     */
    public TextFieldWidgetHelper setMaxLength(int length) {
        base.setMaxLength(length);
        return this;
    }

    /**
     * selects a run of the field's text, by character offset from the start. The two
     * numbers are the cursor and the other end of the selection, so passing them in the
     * other order selects the same run with the cursor at the other end.
     * <p>
     * This is the pair of calls the game uses to set a selection, so it works whatever
     * the field holds. Nothing is checked here, and an offset past the end of the text
     * is left to the game.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const field = screen.textFieldBuilder()
     *    .pos(10, 20)
     *    .size(120, 14)
     *    .build();
     * Hud.openScreen(screen);
     * field.setText("hello world");
     * // select "world", with the cursor at the end of it
     * field.setSelection(6, 11);
     * Chat.log(`selected "${field.getSelectedText()}"`);
     * </pre>
     *
     * @param start the offset of the cursor
     * @param end   the offset of the other end of the selection
     * @return self for chaining.
     * @since 1.8.4
     */
    public TextFieldWidgetHelper setSelection(int start, int end) {
        base.setCursorPosition(start);
        base.setHighlightPos(end);
        return this;
    }

    /**
     * Wires the text-change callback. On 26.1+ the same responder also enforces
     * {@link #setTextPredicate(MethodWrapper)}, because {@code EditBox} no longer exposes a filter.
     * <p>
     * A builder calls this as it makes a field, so the callback is in place before the
     * field is ever shown rather than being attached when an action is set. There is
     * nothing for a script to call here.
     *
     * @param action the callback to run on a change, which may be {@code null}
     * @param screen the screen to hand the callback as its second argument
     */
    void bindAction(@Nullable MethodWrapper<String, IScreen, Object, ?> action, @Nullable IScreen screen) {
        this.action = action;
        this.actionScreen = screen;
        installResponder();
    }

    private void installResponder() {
        base.setResponder(this::onTextChanged);
        snapshotAcceptedState();
    }

    private void onTextChanged(String text) {
        if (restoringText) {
            return;
        }
        if (textFilter != null && !textFilter.test(text)) {
            restoringText = true;
            try {
                base.setValue(lastAcceptedText);
                base.setCursorPosition(lastAcceptedCursor);
                base.setHighlightPos(lastAcceptedHighlight);
            } finally {
                restoringText = false;
            }
            return;
        }
        snapshotAcceptedState();
        if (action != null) {
            try {
                action.accept(text, actionScreen);
            } catch (Throwable e) {
                JsMacrosClient.clientCore.profile.logError(e);
            }
        }
    }

    private void snapshotAcceptedState() {
        lastAcceptedText = base.getValue();
        lastAcceptedCursor = base.getCursorPosition();
        lastAcceptedHighlight = ((MixinTextFieldWidget) base).getHighlightPos();
    }

    /**
     * sets a filter the text has to pass to be allowed in the field. The predicate is
     * given the whole of the proposed text and answers whether it is acceptable;
     * anything it turns down is refused, and the field is put back to the last value it
     * accepted along with the cursor and the selection.
     * <p>
     * On this version of the game the field itself has no filter of its own, so this is
     * enforced by the same responder that runs the change callback. That has two
     * consequences worth knowing. A refused change does not run the callback, because
     * the field never really changed. And the field keeps the last text it accepted
     * rather than keeping what the player typed and hiding it, so what is in the field
     * and what the player just pressed are not the same thing.
     * <p>
     * Calling this also re-wires the responder, which is what puts the filter in place.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const field = screen.textFieldBuilder()
     *    .pos(10, 20)
     *    .size(120, 14)
     *    .action(JavaWrapper.methodToJava(function (text) {
     *       Chat.log(`accepted: ${text}`);
     *     }))
     *    .build();
     * Hud.openScreen(screen);
     * // eight characters at most
     * field.setTextPredicate(JavaWrapper.methodToJava(function (text) {
     *   return 8 >= String(text).length;
     * }));
     * field.setText("short");
     * // this one is refused, so the callback does not run
     * field.setText("far too long to be allowed");
     * Chat.log(`the field still says "${field.getText()}"`);
     * </pre>
     *
     * @param predicate the text filter
     * @return self for chaining.
     * @since 1.8.4
     */
    public TextFieldWidgetHelper setTextPredicate(MethodWrapper<String, ?, ?, ?> predicate) {
        //? if >=26.1 {
        /*this.textFilter = predicate;
        installResponder();
        *///? } else {
        base.setFilter(predicate);
        //? }
        return this;
    }

    /**
     * drops the filter, so anything can be typed into the field again. A field that
     * never had a filter does not need this, and one whose predicate is turned off here
     * keeps whatever text it had rather than being emptied.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const field = screen.textFieldBuilder()
     *    .pos(10, 20)
     *    .size(120, 14)
     *    .build();
     * Hud.openScreen(screen);
     * field.setTextPredicate(JavaWrapper.methodToJava(function (text) {
     *   return 8 >= String(text).length;
     * }));
     * // let anything through again
     * field.resetTextPredicate();
     * field.setText("this is much longer than eight characters");
     * Chat.log(`the field now says "${field.getText()}"`);
     * </pre>
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public TextFieldWidgetHelper resetTextPredicate() {
        //? if >=26.1 {
        /*this.textFilter = null;
        *///? } else {
        base.setFilter(Objects::nonNull);
        //? }
        return this;
    }

    /**
     * moves the cursor to a character offset in the field and drops any selection, which
     * is the form without a shift held. This is the same as calling the other form with
     * {@code false}.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const field = screen.textFieldBuilder()
     *    .pos(10, 20)
     *    .size(120, 14)
     *    .build();
     * Hud.openScreen(screen);
     * field.setText("hello world");
     * field.setCursorPosition(5);
     * // moving the cursor drops the selection
     * Chat.log(`selected "${field.getSelectedText()}"`);
     * </pre>
     *
     * @param position the cursor position
     * @return self for chaining.
     * @since 1.8.4
     */
    public TextFieldWidgetHelper setCursorPosition(int position) {
        base.moveCursorTo(position, false);
        return this;
    }

    /**
     * moves the cursor to a character offset, with a choice about what happens to the
     * selection. With {@code shift} off the old selection is dropped; with it on the
     * cursor moves and the selection grows or shrinks to reach it, which is what holding
     * shift while clicking does.
     * <p>
     * This is the two-argument form of the cursor move. The other one, taking only a
     * position, is the same as calling this with {@code false}.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const field = screen.textFieldBuilder()
     *    .pos(10, 20)
     *    .size(120, 14)
     *    .build();
     * Hud.openScreen(screen);
     * field.setText("hello world");
     * // select the first five characters
     * field.setCursorPosition(0);
     * field.setCursorPosition(5, true);
     * Chat.log(`selected "${field.getSelectedText()}"`);
     * </pre>
     *
     * @param position the cursor position
     * @param shift    whether to keep the selection and extend it to the new position
     * @return self for chaining.
     * @since 1.9.0
     */
    public TextFieldWidgetHelper setCursorPosition(int position, boolean shift) {
        base.moveCursorTo(position, shift);
        return this;
    }

    /**
     * puts the cursor at the very start of the field and drops the selection, which is
     * the form without a shift held.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const field = screen.textFieldBuilder()
     *    .pos(10, 20)
     *    .size(120, 14)
     *    .build();
     * Hud.openScreen(screen);
     * field.setText("hello world");
     * field.setCursorToStart();
     * Chat.log(`selected "${field.getSelectedText()}"`);
     * </pre>
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public TextFieldWidgetHelper setCursorToStart() {
        base.moveCursorToStart(false);
        return this;
    }

    /**
     * puts the cursor at the very start of the field, with a choice about the
     * selection. With {@code shift} on, the selection runs from wherever the cursor was
     * to the start rather than being dropped.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const field = screen.textFieldBuilder()
     *    .pos(10, 20)
     *    .size(120, 14)
     *    .build();
     * Hud.openScreen(screen);
     * field.setText("hello world");
     * field.setCursorToEnd();
     * // shift back to the start, selecting the lot
     * field.setCursorToStart(true);
     * Chat.log(`selected "${field.getSelectedText()}"`);
     * </pre>
     *
     * @param shift whether to keep the selection and extend it to the new position
     * @return self for chaining.
     * @since 1.9.0
     */
    public TextFieldWidgetHelper setCursorToStart(boolean shift) {
        base.moveCursorToStart(shift);
        return this;
    }

    /**
     * puts the cursor at the very end of the field and drops the selection, which is
     * the form without a shift held.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const field = screen.textFieldBuilder()
     *    .pos(10, 20)
     *    .size(120, 14)
     *    .build();
     * Hud.openScreen(screen);
     * field.setText("hello world");
     * field.setCursorToEnd();
     * Chat.log(`selected "${field.getSelectedText()}"`);
     * </pre>
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public TextFieldWidgetHelper setCursorToEnd() {
        base.moveCursorToEnd(false);
        return this;
    }

    /**
     * puts the cursor at the very end of the field, with a choice about the selection.
     * With {@code shift} on, the selection runs from wherever the cursor was to the end
     * rather than being dropped, which is the way to select a whole field's contents
     * from script.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const field = screen.textFieldBuilder()
     *    .pos(10, 20)
     *    .size(120, 14)
     *    .build();
     * Hud.openScreen(screen);
     * field.setText("hello world");
     * field.setCursorToStart();
     * // shift forward to the end, selecting the lot
     * field.setCursorToEnd(true);
     * Chat.log(`selected "${field.getSelectedText()}"`);
     * </pre>
     *
     * @param shift whether to keep the selection and extend it to the new position
     * @return self for chaining.
     * @since 1.9.0
     */
    public TextFieldWidgetHelper setCursorToEnd(boolean shift) {
        base.moveCursorToEnd(shift);
        return this;
    }

    @Override
    public String toString() {
        return String.format("TextFieldWidgetHelper:{\"text\": \"%s\"}", base.getValue());
    }

    /**
     * the builder for a text field, handed back by {@code IScreen.textFieldBuilder()}
     * and already bound to that screen and to its font. The font is what the field draws
     * its text with, which is why the builder takes the screen's rather than one of its
     * own.
     * <p>
     * The callback is wired up as the field is built rather than when the action is set,
     * so it is in place before the field is ever shown. The height is taken from the
     * shared builder, so a field can be built at whatever height is asked for.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const builder = screen.textFieldBuilder()
     *    .pos(10, 20)
     *    .size(120, 14)
     *    .message("type a name")
     *    .suggestion("a name is required")
     *    .action(JavaWrapper.methodToJava(function (text) {
     *       Chat.log(`so far: ${text}`);
     *     }));
     * // both are readable on the builder before anything is built
     * Chat.log(`has a callback: ${builder.getAction() !== null}`);
     * Chat.log(`suggestion: ${builder.getSuggestion()}`);
     * const field = builder.build();
     * Hud.openScreen(screen);
     * </pre>
     *
     * @author Etheradon
     * @since 1.8.4
     */
    @DocletCategory("Screen and UI Elements")
    public static class TextFieldBuilder extends AbstractWidgetBuilder<TextFieldBuilder, EditBox, TextFieldWidgetHelper> {

        private String suggestion = "";
        @Nullable
        private MethodWrapper<String, IScreen, Object, ?> action;
        private final Font textRenderer;

        public TextFieldBuilder(IScreen screen, Font textRenderer) {
            super(screen);
            this.textRenderer = textRenderer;
        }

        /**
         * the callback to run when the text changes, which is {@code null} until one is
         * set. It is given the text and the screen rather than the field, so a callback
         * that wants the field has to reach it some other way.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * const builder = screen.textFieldBuilder().pos(10, 20).size(120, 14);
         * Chat.log(`has a callback: ${builder.getAction() !== null}`);
         * builder.action(JavaWrapper.methodToJava(function (text) {
         *   Chat.log(`so far: ${text}`);
         * }));
         * Chat.log(`now: ${builder.getAction() !== null}`);
         * </pre>
         *
         * @return the callback for when the text is changed.
         * @since 1.8.4
         */
        @Nullable
        public MethodWrapper<String, IScreen, Object, ?> getAction() {
            return action;
        }

        /**
         * sets the callback to run when the text changes, given the text and the screen
         * the builder was made for. It is wired into the field as the field is built,
         * so it is in place before the field is ever shown.
         * <p>
         * It runs for any change to the text rather than only for a keystroke, so a
         * scripted {@link TextFieldWidgetHelper#setText(String)} runs it too.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * screen.textFieldBuilder()
         *    .pos(10, 20)
         *    .size(120, 14)
         *    .message("type a name")
         *    .action(JavaWrapper.methodToJava(function (text) {
         *       Chat.log(`so far: ${text}`);
         *     }))
         *    .build();
         * Hud.openScreen(screen);
         * </pre>
         *
         * @param action the callback for when the text is changed
         * @return self for chaining.
         * @since 1.8.4
         */
        public TextFieldBuilder action(@Nullable MethodWrapper<String, IScreen, Object, ?> action) {
            this.action = action;
            return this;
        }

        /**
         * the hint set so far, which starts as an empty string rather than as
         * {@code null}. It is the text the field is drawn with while it is empty.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * const builder = screen.textFieldBuilder().pos(10, 20).size(120, 14);
         * Chat.log(`starts as "${builder.getSuggestion()}"`);
         * builder.suggestion("a name is required");
         * Chat.log(`now "${builder.getSuggestion()}"`);
         * </pre>
         *
         * @return the current suggestion.
         * @since 1.8.4
         */
        public String getSuggestion() {
            return suggestion;
        }

        /**
         * sets the greyed out hint the field is drawn with while it is empty, which is
         * what tells a player what belongs there. It is a hint rather than a value: the
         * field does not contain it, and {@link TextFieldWidgetHelper#getText()} does
         * not give it back.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * screen.textFieldBuilder()
         *    .pos(10, 20)
         *    .size(120, 14)
         *    .message("type a name")
         *    .suggestion("a name is required")
         *    .build();
         * Hud.openScreen(screen);
         * </pre>
         *
         * @param suggestion the suggestion to use
         * @return self for chaining.
         * @since 1.8.4
         */
        public TextFieldBuilder suggestion(String suggestion) {
            this.suggestion = suggestion;
            return this;
        }

        @Override
        public TextFieldWidgetHelper createWidget() {
            EditBox textField = new EditBox(textRenderer, getX(), getY(), getWidth(), getHeight(), getMessage().getRaw());
            textField.setSuggestion(suggestion);
            TextFieldWidgetHelper helper = new TextFieldWidgetHelper(textField, getZIndex());
            helper.bindAction(action, screen);
            return helper;
        }

    }

}
