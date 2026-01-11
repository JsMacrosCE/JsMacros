package com.jsmacrosce.jsmacros.client.api.helper.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.render.IScreen;
import com.jsmacrosce.jsmacros.core.MethodWrapper;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Etheradon
 * @since 1.8.4
 */
public class ButtonWidgetHelper<T extends Button> extends ClickableWidgetHelper<ButtonWidgetHelper<T>, T> {

    public ButtonWidgetHelper(T btn) {
        super(btn);
    }

    public ButtonWidgetHelper(T btn, int zIndex) {
        super(btn, zIndex);
    }

    /**
     * @author Etheradon
     * @since 1.8.4
     */
    public static class ButtonBuilder extends AbstractWidgetBuilder<ButtonBuilder, Button, ButtonWidgetHelper<Button>> {

        @Nullable
        private MethodWrapper<ButtonWidgetHelper<Button>, IScreen, Object, ?> action;

        public ButtonBuilder(IScreen screen) {
            super(screen);
        }

        /**
         * @param height this argument is ignored and will always be set to 20
         * @return self for chaining.
         * @since 1.8.4
         */
        @Override
        public ButtonBuilder height(int height) {
            super.height(20);
            return this;
        }

        /**
         * @param width  the width of the button
         * @param height this argument is ignored and will always be set to 20
         * @return self for chaining.
         * @since 1.8.4
         */
        @Override
        public ButtonBuilder size(int width, int height) {
            super.size(width, 20);
            return this;
        }

        /**
         * @return the action to run when the button is pressed.
         * @since 1.8.4
         */
        @Nullable
        public MethodWrapper<ButtonWidgetHelper<Button>, IScreen, Object, ?> getAction() {
            return action;
        }

        /**
         * @param action the action to run when the button is pressed
         * @return self for chaining.
         * @since 1.8.4
         */
        public ButtonBuilder action(@Nullable MethodWrapper<ButtonWidgetHelper<Button>, IScreen, Object, ?> action) {
            this.action = action;
            return this;
        }

        @Override
        public ButtonWidgetHelper<Button> createWidget() {
            AtomicReference<ButtonWidgetHelper<Button>> b = new AtomicReference<>(null);
            Button button = Button.builder(getMessage().getRaw(), btn -> {
                try {
                    if (action != null) {
                        action.accept(b.get(), screen);
                    }
                } catch (Throwable e) {
                    JsMacrosClient.clientCore.profile.logError(e);
                }
                clickedOn(screen);
            }).pos(getX(), getY()).size(getWidth(), 20).build();
            b.set(new ButtonWidgetHelper<>(button, getZIndex()));
            return b.get();
        }

    }

    /**
     * @author Etheradon
     * @since 1.8.4
     */
    public static class TexturedButtonBuilder extends AbstractWidgetBuilder<TexturedButtonBuilder, ImageButton, ButtonWidgetHelper<ImageButton>> {

        @Nullable
        private MethodWrapper<ButtonWidgetHelper<ImageButton>, IScreen, Object, ?> action;

        private ResourceLocation enabled;
        private ResourceLocation disabled;
        private ResourceLocation enabledFocused;
        private ResourceLocation disabledFocused;

        public TexturedButtonBuilder(IScreen screen) {
            super(screen);
        }

        /**
         * @param height this argument is ignored and will always be set to 20
         * @return self for chaining.
         * @since 1.8.4
         */
        @Override
        public TexturedButtonBuilder height(int height) {
            super.height(20);
            return this;
        }

        /**
         * @param width  the width of the button
         * @param height this argument is ignored and will always be set to 20
         * @return self for chaining.
         * @since 1.8.4
         */
        @Override
        public TexturedButtonBuilder size(int width, int height) {
            super.size(width, 20);
            return this;
        }

        /**
         * @return the action to run when the button is pressed.
         * @since 1.8.4
         */
        @Nullable
        public MethodWrapper<ButtonWidgetHelper<ImageButton>, IScreen, Object, ?> getAction() {
            return action;
        }

        /**
         * @param action the action to run when the button is pressed
         * @return self for chaining.
         * @since 1.8.4
         */
        public TexturedButtonBuilder action(@Nullable MethodWrapper<ButtonWidgetHelper<ImageButton>, IScreen, Object, ?> action) {
            this.action = action;
            return this;
        }

        /**
         * @since 1.9.0
         * @return self for chaining.
         */
        public TexturedButtonBuilder enabledTexture(ResourceLocation enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * @since 1.9.3
         */
        public TexturedButtonBuilder enabledTexture(String enabled) {
            return enabledTexture(ResourceLocation.parse(enabled));
        }

        /**
         * @since 1.9.0
         * @return self for chaining.
         */
        public TexturedButtonBuilder disabledTexture(ResourceLocation disabled) {
            this.disabled = disabled;
            return this;
        }

        /**
         * @since 1.9.3
         */
        public TexturedButtonBuilder disabledTexture(String disabled) {
            return disabledTexture(ResourceLocation.parse(disabled));
        }

        /**
         * @since 1.9.0
         * @return self for chaining.
         */
        public TexturedButtonBuilder enabledFocusedTexture(ResourceLocation enabledFocused) {
            this.enabledFocused = enabledFocused;
            return this;
        }

        /**
         * @since 1.9.3
         */
        public TexturedButtonBuilder enabledFocusedTexture(String enabledFocused) {
            return enabledFocusedTexture(ResourceLocation.parse(enabledFocused));
        }

        /**
         * @since 1.9.0
         * @return self for chaining.
         */
        public TexturedButtonBuilder disabledFocusedTexture(ResourceLocation disabledFocused) {
            this.disabledFocused = disabledFocused;
            return this;
        }

        /**
         * @since 1.9.3
         */
        public TexturedButtonBuilder disabledFocusedTexture(String disabledFocused) {
            return disabledFocusedTexture(ResourceLocation.parse(disabledFocused));
        }

        @Override
        public ButtonWidgetHelper<ImageButton> createWidget() {
            AtomicReference<ButtonWidgetHelper<ImageButton>> b = new AtomicReference<>(null);
            ImageButton button = new ImageButton(getX(), getY(), getWidth(), getHeight(), new WidgetSprites(enabled, disabled, enabledFocused, disabledFocused), btn -> {
                try {
                    if (getAction() != null) {
                        getAction().accept(b.get(), screen);
                    }
                } catch (Throwable e) {
                    JsMacrosClient.clientCore.profile.logError(e);
                }
                clickedOn(screen);
            }, getMessage().getRaw());
            b.set(new ButtonWidgetHelper<>(button, getZIndex()));
            return b.get();
        }

    }

}
