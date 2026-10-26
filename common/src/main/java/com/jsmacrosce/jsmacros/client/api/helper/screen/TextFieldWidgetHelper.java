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
 * Wraps the game's text field with a text-change callback.
 * <p>
 * The callback receives accepted new text and is attached when the field is built.
 * Scripted {@link #setText(String)} changes invoke it when accepted.
 * <pre>
 * const screen = Hud.createScreen("a screen", true);
 * screen.setOnInit(JavaWrapper.methodToJava(function (self) {
 *   const field = self.textFieldBuilder()
 *      .pos(10, 20)
 *      .size(120, 14)
 *      .message("type a name")
 *      .action(JavaWrapper.methodToJava(function (text) {
 *        // the callback is given the text, not the field
 *        Chat.log(`so far: ${text}`);
 *      }))
 *      .build();
 *
 *   // Accepted scripted changes invoke the callback too.
 *   field.setText("hello");
 *   // On 26.1+, rejected text restores the last accepted value, cursor, and selection.
 *   field.setTextPredicate(JavaWrapper.methodToJava(function (text) {
 *     return 8 >= String(text).length;
 *   }));
 *   field.setText("far too long to be allowed");
 *   Chat.log(`the field still says ${field.getText()}`);
 * }));
 * Hud.openScreen(screen);
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

    /** Wraps an existing text field with a helper whose drawing-order index is zero.
     *
     * @param t the text field to wrap
     */
    public TextFieldWidgetHelper(EditBox t) {
        super(t);
    }

    /** Wraps an existing text field with the specified helper drawing-order index.
     *
     * @param t the text field to wrap
     * @param zIndex the helper's drawing-order index
     */
    public TextFieldWidgetHelper(EditBox t, int zIndex) {
        super(t, zIndex);
    }

    /**
     * Returns the entered text, excluding the field's suggestion.
     *
     * @return the current text; empty when no text is entered.
     * @since 1.0.5
     */
    public String getText() {
        return base.getValue();
    }

    /**
     * Sets the field's text and waits for the change to be applied. Any configured text
     * predicate applies, and the callback runs if the text is accepted.
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
     * Sets the field's text, optionally waiting for the change to be applied.
     * <p>
     * When called from the client thread there is nothing to hand off and nothing to
     * wait for, so the {@code await} flag makes no difference there. Off the client
     * thread the change is queued onto it, and the flag decides whether this call blocks
     * until its own task has run. Blocking also waits out whatever else was queued
     * ahead of it.
     * <p>
     * Any configured text predicate applies to scripted changes; the callback runs for
     * accepted changes.
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
     * Sets the color used to draw text while the field is editable.
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
     * Sets whether the field accepts text input. When false, it uses the uneditable
     * text color.
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
     * Sets the color used to draw text while the field is not editable.
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
     * Returns the selected portion of the text, or an empty string when nothing is
     * selected.
     *
     * @return the selected text.
     * @since 1.8.4
     */
    public String getSelectedText() {
        return base.getHighlighted();
    }

    /**
     * Sets the hint drawn in the field while it is empty; this does not change the text
     * returned by {@link #getText()}.
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
     * Returns the maximum number of UTF-16 code units allowed in the field; a limit of zero
     * allows no text.
     *
     * @return the maximum number of UTF-16 code units allowed in this text field.
     * @since 1.8.4
     */
    public int getMaxLength() {
        return ((MixinTextFieldWidget) base).getMaxLength();
    }

    /**
     * Sets the maximum number of UTF-16 code units allowed. If the current text exceeds the
     * new limit, it is truncated immediately; a limit of zero allows no text.
     *
     * @param length the new maximum number of UTF-16 code units
     * @return self for chaining.
     * @since 1.8.4
     */
    public TextFieldWidgetHelper setMaxLength(int length) {
        base.setMaxLength(length);
        return this;
    }

    /**
     * Selects text between two character offsets: {@code start} is the cursor position
     * and {@code end} is the other selection endpoint. Offsets are passed to the game
     * without validation.
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
     * Installs the text-change callback. On 26.1+, the responder also enforces any predicate
     * configured by {@link #setTextPredicate(MethodWrapper)} because {@code EditBox} has no native filter.
     * Called by the builder when creating the field; scripts do not call this method.
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
     * Sets a predicate over the complete proposed text. On 26.1+, rejected input restores
     * the last accepted text, cursor, and selection without running the change callback;
     * this is enforced by the responder because {@code EditBox} has no filter. Earlier
     * versions use the field's native filter.
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
     * Removes the text filter, allowing all non-null text. Existing text is retained.
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
     * Moves the cursor to a character offset and drops any selection; equivalent to
     * calling {@link #setCursorPosition(int, boolean)} with {@code false}.
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
     * Moves the cursor to a character offset. With {@code shift} enabled, the selection
     * is extended or shrunk to that position; otherwise it is dropped.
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

    /** Moves the cursor to the start of the text and drops the selection.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public TextFieldWidgetHelper setCursorToStart() {
        base.moveCursorToStart(false);
        return this;
    }

    /** Moves the cursor to the start of the text.
     * With {@code shift} enabled, the existing selection is extended or shrunk to the start;
     * otherwise it is dropped.
     *
     * @param shift whether to keep the selection and extend it to the new position
     * @return self for chaining.
     * @since 1.9.0
     */
    public TextFieldWidgetHelper setCursorToStart(boolean shift) {
        base.moveCursorToStart(shift);
        return this;
    }

    /** Moves the cursor to the end of the text and drops the selection.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public TextFieldWidgetHelper setCursorToEnd() {
        base.moveCursorToEnd(false);
        return this;
    }

    /** Moves the cursor to the end of the text.
     * With {@code shift} enabled, the existing selection is extended or shrunk to the end;
     * otherwise it is dropped.
     *
     * @param shift whether to keep the selection and extend it to the new position
     * @return self for chaining.
     * @since 1.9.0
     */
    public TextFieldWidgetHelper setCursorToEnd(boolean shift) {
        base.moveCursorToEnd(shift);
        return this;
    }

    /** Returns a diagnostic string containing the current text, without JSON escaping.
     *
     * @return the diagnostic string, formatted as {@code TextFieldWidgetHelper:{"text": "%s"}}
     */
    @Override
    public String toString() {
        return String.format("TextFieldWidgetHelper:{\"text\": \"%s\"}", base.getValue());
    }

    /** Builds text-field widgets using the screen's font and configured dimensions.
     * The configured callback is attached when the widget is built.
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

        /** Stores the screen used for widget addition and the font used to create text fields.
         *
         * @param screen the screen associated with this builder
         * @param textRenderer the font used to create text fields
         */
        public TextFieldBuilder(IScreen screen, Font textRenderer) {
            super(screen);
            this.textRenderer = textRenderer;
        }

        /** Returns the configured text-change callback, or {@code null} if none is set.
         *
         * @return the callback for when the text is changed.
         * @since 1.8.4
         */
        @Nullable
        public MethodWrapper<String, IScreen, Object, ?> getAction() {
            return action;
        }

        /** Sets the callback for accepted text changes. It receives the new text and
         * the screen used to create this builder; it is attached when the field is built.
         * Scripted {@link TextFieldWidgetHelper#setText(String)} changes also invoke it.
         *
         * @param action the callback for when the text is changed
         * @return self for chaining.
         * @since 1.8.4
         */
        public TextFieldBuilder action(@Nullable MethodWrapper<String, IScreen, Object, ?> action) {
            this.action = action;
            return this;
        }

        /** Returns the configured hint, initially an empty string.
         *
         * @return the current suggestion.
         * @since 1.8.4
         */
        public String getSuggestion() {
            return suggestion;
        }

        /** Sets the hint drawn while the field is empty; it is not part of the field's
         * value returned by {@link TextFieldWidgetHelper#getText()}.
         *
         * @param suggestion the suggestion to use
         * @return self for chaining.
         * @since 1.8.4
         */
        public TextFieldBuilder suggestion(String suggestion) {
            this.suggestion = suggestion;
            return this;
        }

        /** Creates a configured text field and helper, attaching the configured callback.
         * The field is not added to the screen and build-time alpha, active, and visible
         * settings are not applied until {@code build()} is called.
         *
         * @return the helper for the newly created text field
         */
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
