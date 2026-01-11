package com.jsmacrosce.jsmacros.client.api.helper.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.render.IScreen;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinTextFieldWidget;
import com.jsmacrosce.jsmacros.core.MethodWrapper;

import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Wagyourtail
 * @since 1.0.5
 */
@SuppressWarnings("unused")
public class TextFieldWidgetHelper extends ClickableWidgetHelper<TextFieldWidgetHelper, EditBox> {
    public TextFieldWidgetHelper(EditBox t) {
        super(t);
    }

    public TextFieldWidgetHelper(EditBox t, int zIndex) {
        super(t, zIndex);
    }

    /**
     * @return the currently entered {@link String String}.
     * @since 1.0.5
     */
    public String getText() {
        return base.getValue();
    }

    /**
     * @param text
     * @return self for chaining.
     * @since 1.0.5
     */
    public TextFieldWidgetHelper setText(String text) throws InterruptedException {
        setText(text, true);
        return this;
    }

    /**
     * set the currently entered {@link String String}.
     *
     * @param text
     * @param await
     * @return self for chaining.
     * @throws InterruptedException
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
     * @param color
     * @return self for chaining.
     * @since 1.0.5
     */
    public TextFieldWidgetHelper setEditableColor(int color) {
        base.setTextColor(color);
        return this;
    }

    /**
     * @param edit
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
     * @param color
     * @return self for chaining.
     * @since 1.0.5
     */
    public TextFieldWidgetHelper setUneditableColor(int color) {
        base.setTextColorUneditable(color);
        return this;
    }

    /**
     * @return the selected text.
     * @since 1.8.4
     */
    public String getSelectedText() {
        return base.getHighlighted();
    }

    /**
     * @param suggestion the suggestion to set
     * @return self for chaining.
     * @since 1.8.4
     */
    public TextFieldWidgetHelper setSuggestion(String suggestion) {
        base.setSuggestion(suggestion);
        return this;
    }

    /**
     * @return the maximum length of this text field.
     * @since 1.8.4
     */
    public int getMaxLength() {
        return ((MixinTextFieldWidget) base).getMaxLength();
    }

    /**
     * @param length the new maximum length
     * @return self for chaining.
     * @since 1.8.4
     */
    public TextFieldWidgetHelper setMaxLength(int length) {
        base.setMaxLength(length);
        return this;
    }

    public TextFieldWidgetHelper setSelection(int start, int end) {
        base.setCursorPosition(start);
        base.setHighlightPos(end);
        return this;
    }

    /**
     * @param predicate the text filter
     * @return self for chaining.
     * @since 1.8.4
     */
    public TextFieldWidgetHelper setTextPredicate(MethodWrapper<String, ?, ?, ?> predicate) {
        base.setFilter(predicate);
        return this;
    }

    /**
     * @return self for chaining.
     * @since 1.8.4
     */
    public TextFieldWidgetHelper resetTextPredicate() {
        base.setFilter(Objects::nonNull);
        return this;
    }

    /**
     * @param position the cursor position
     * @return self for chaining.
     * @since 1.8.4
     */
    public TextFieldWidgetHelper setCursorPosition(int position) {
        base.moveCursorTo(position, false);
        return this;
    }

    /**
     * @return the cursor position.
     * @since 1.9.0
     */
    public TextFieldWidgetHelper setCursorPosition(int position, boolean shift) {
        base.moveCursorTo(position, shift);
        return this;
    }

    /**
     * @return self for chaining.
     * @since 1.8.4
     */
    public TextFieldWidgetHelper setCursorToStart() {
        base.moveCursorToStart(false);
        return this;
    }

    /**
     * @return self for chaining.
     * @since 1.9.0
     */
    public TextFieldWidgetHelper setCursorToStart(boolean shift) {
        base.moveCursorToStart(shift);
        return this;
    }

    /**
     * @return self for chaining.
     * @since 1.8.4
     */
    public TextFieldWidgetHelper setCursorToEnd() {
        base.moveCursorToEnd(false);
        return this;
    }

    /**
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
     * @author Etheradon
     * @since 1.8.4
     */
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
         * @return the callback for when the text is changed.
         * @since 1.8.4
         */
        @Nullable
        public MethodWrapper<String, IScreen, Object, ?> getAction() {
            return action;
        }

        /**
         * @param action the callback for when the text is changed
         * @return self for chaining.
         * @since 1.8.4
         */
        public TextFieldBuilder action(@Nullable MethodWrapper<String, IScreen, Object, ?> action) {
            this.action = action;
            return this;
        }

        /**
         * @return the current suggestion.
         * @since 1.8.4
         */
        public String getSuggestion() {
            return suggestion;
        }

        /**
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
            AtomicReference<TextFieldWidgetHelper> b = new AtomicReference<>(null);
            EditBox textField = new EditBox(textRenderer, getX(), getY(), getWidth(), getHeight(), getMessage().getRaw());
            textField.setResponder(text -> {
                try {
                    if (action != null) {
                        action.accept(text, screen);
                    }
                } catch (Throwable e) {
                    JsMacrosClient.clientCore.profile.logError(e);
                }
            });
            textField.setSuggestion(suggestion);
            b.set(new TextFieldWidgetHelper(textField, getZIndex()));
            return b.get();
        }

    }

}
