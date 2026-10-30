package com.jsmacrosce.jsmacros.client.api.helper.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.render.IScreen;
import com.jsmacrosce.jsmacros.core.MethodWrapper;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Wraps a game button and exposes its button-specific behavior to scripts. Use
 * {@code IScreen.buttonBuilder()} for the standard appearance or
 * {@code IScreen.texturedButtonBuilder()} for a texture-based button.
 * <pre>
 * const screen = Hud.createScreen("a screen", true);
 * screen.setOnInit(JavaWrapper.methodToJava(function (self) {
 *   const button = self.buttonBuilder()
 *      .pos(10, 20)
 *      .size(100, 20)
 *      .message("press me")
 *      .action(JavaWrapper.methodToJava(function (btn) {
 *        btn.setLabel("pressed");
 *      }))
 *      .build();
 * }));
 * Hud.openScreen(screen);
 *
 * </pre>
 *
 * @param <T> the game button type wrapped by this helper
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Screen and UI Elements")
public class ButtonWidgetHelper<T extends Button> extends ClickableWidgetHelper<ButtonWidgetHelper<T>, T> {

    /**
     * Wraps the supplied button with helper z-index {@code 0}. Empty helper tooltips are set during initialization.
     * @param btn button to wrap
     */
    public ButtonWidgetHelper(T btn) {
        super(btn);
    }

    /** Wraps the supplied button using the given helper draw-order value. Empty helper tooltips are set during initialization.
     * @param btn button to wrap
     * @param zIndex helper draw-order value
     */
    public ButtonWidgetHelper(T btn, int zIndex) {
        super(btn, zIndex);
    }

    /**
     * Builds a standard-appearance button bound to the screen that supplied this builder.
     * Its height is fixed at 20; other properties are configured through the shared builder.
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.setOnInit(JavaWrapper.methodToJava(function (self) {
     *   self.buttonBuilder()
     *      .pos(10, 20)
     *      .size(100, 20)
     *      .message("press me")
     *      .action(JavaWrapper.methodToJava(function (btn, onScreen) {
     *        // the callback is given the button and the screen
     *        Chat.log(`pressed on the screen titled ${onScreen.getTitleText()}`);
     *      }))
     *      .build();
     * }));
     * Hud.openScreen(screen);
     * </pre>
     *
     * @author Etheradon
     * @since 1.8.4
     */
    @DocletCategory("Screen and UI Elements")
    public static class ButtonBuilder extends AbstractWidgetBuilder<ButtonBuilder, Button, ButtonWidgetHelper<Button>> {

        @Nullable
        private MethodWrapper<ButtonWidgetHelper<Button>, IScreen, Object, ?> action;

        /** Retains the screen associated with this builder for later widget creation.
         * @param screen screen associated with the builder
         */
        public ButtonBuilder(IScreen screen) {
            super(screen);
        }

        /**
         * @param height ignored; the button height remains 20
         * @return this builder
         * @since 1.8.4
         */
        @Override
        public ButtonBuilder height(int height) {
            super.height(20);
            return this;
        }

        /**
         * @param width the button width
         * @param height ignored; the button height remains 20
         * @return this builder
         * @since 1.8.4
         */
        @Override
        public ButtonBuilder size(int width, int height) {
            super.size(width, 20);
            return this;
        }

        /**
         * @return the configured action, or {@code null} if none is set
         * @since 1.8.4
         */
        @Nullable
        public MethodWrapper<ButtonWidgetHelper<Button>, IScreen, Object, ?> getAction() {
            return action;
        }

        /**
         * @param action the action to run on activation, or {@code null} for no action
         * @return this builder
         * @since 1.8.4
         */
        public ButtonBuilder action(@Nullable MethodWrapper<ButtonWidgetHelper<Button>, IScreen, Object, ?> action) {
            this.action = action;
            return this;
        }

        /** Creates and wraps a new standard-appearance button from this builder's current configuration without adding it to the screen or applying shared post-creation styles. Use {@code build()} to create, configure, and add the button.
         * @return the newly wrapped standard-appearance button
         */
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
     * Builds a texture-based button bound to the screen that supplied this builder.
     * Provide textures for the enabled, disabled, enabled-focused, and disabled-focused
     * states; none falls back to another. Its height is fixed at 20.
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.setOnInit(JavaWrapper.methodToJava(function (self) {
     *   self.texturedButtonBuilder()
     *      .pos(10, 20)
     *      .size(100, 20)
     *      .enabledTexture("minecraft:item/diamond")
     *      .disabledTexture("minecraft:item/emerald")
     *      .enabledFocusedTexture("minecraft:item/diamond")
     *      .disabledFocusedTexture("minecraft:item/emerald")
     *      .build();
     * }));
     * Hud.openScreen(screen);
     * </pre>
     *
     * @author Etheradon
     * @since 1.8.4
     */
    @DocletCategory("Screen and UI Elements")
    public static class TexturedButtonBuilder extends AbstractWidgetBuilder<TexturedButtonBuilder, ImageButton, ButtonWidgetHelper<ImageButton>> {

        @Nullable
        private MethodWrapper<ButtonWidgetHelper<ImageButton>, IScreen, Object, ?> action;

        private ResourceLocation enabled;
        private ResourceLocation disabled;
        private ResourceLocation enabledFocused;
        private ResourceLocation disabledFocused;

        /** Retains the screen associated with this builder for later widget creation.
         * @param screen screen associated with the builder
         */
        public TexturedButtonBuilder(IScreen screen) {
            super(screen);
        }

        /**
         * @param height ignored; the button height remains 20
         * @return this builder
         * @since 1.8.4
         */
        @Override
        public TexturedButtonBuilder height(int height) {
            super.height(20);
            return this;
        }

        /**
         * @param width the button width
         * @param height ignored; the button height remains 20
         * @return this builder
         * @since 1.8.4
         */
        @Override
        public TexturedButtonBuilder size(int width, int height) {
            super.size(width, 20);
            return this;
        }

        /**
         * @return the configured action, or {@code null} if none is set
         * @since 1.8.4
         */
        @Nullable
        public MethodWrapper<ButtonWidgetHelper<ImageButton>, IScreen, Object, ?> getAction() {
            return action;
        }

        /**
         * Sets the action run when the button is activated; pass {@code null} to clear it.
         * @param action the action to run, or {@code null} for no action
         * @return this builder
         * @since 1.8.4
         */
        public TexturedButtonBuilder action(@Nullable MethodWrapper<ButtonWidgetHelper<ImageButton>, IScreen, Object, ?> action) {
            this.action = action;
            return this;
        }

        /**
         * Sets the texture used when the button is active and neither hovered nor focused.
         *
         * @param enabled the texture to draw while enabled and not focused
         * @return this builder
         * @since 1.9.0
         */
        public TexturedButtonBuilder enabledTexture(ResourceLocation enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * Sets the enabled, unfocused texture from its resource-location string; an omitted namespace defaults to {@code minecraft}.
         *
         * @param enabled the texture to draw while enabled and not focused
         * @return this builder
         * @since 1.9.3
         */
        public TexturedButtonBuilder enabledTexture(String enabled) {
            return enabledTexture(ResourceLocation.parse(enabled));
        }

        /**
         * Sets the texture used when the button is inactive and neither hovered nor focused.
         *
         * @param disabled the texture to draw while disabled
         * @return this builder
         * @since 1.9.0
         */
        public TexturedButtonBuilder disabledTexture(ResourceLocation disabled) {
            this.disabled = disabled;
            return this;
        }

        /**
         * Sets the disabled, unfocused texture from its resource-location string; an omitted namespace defaults to {@code minecraft}.
         *
         * @param disabled the texture to draw while disabled
         * @return this builder
         * @since 1.9.3
         */
        public TexturedButtonBuilder disabledTexture(String disabled) {
            return disabledTexture(ResourceLocation.parse(disabled));
        }

        /**
         * Sets the texture used when the button is active and hovered or focused.
         *
         * @param enabledFocused the texture to draw while enabled and focused
         * @return this builder
         * @since 1.9.0
         */
        public TexturedButtonBuilder enabledFocusedTexture(ResourceLocation enabledFocused) {
            this.enabledFocused = enabledFocused;
            return this;
        }

        /**
         * Sets the enabled, focused texture from its resource-location string; an omitted namespace defaults to {@code minecraft}.
         *
         * @param enabledFocused the texture to draw while enabled and focused
         * @return this builder
         * @since 1.9.3
         */
        public TexturedButtonBuilder enabledFocusedTexture(String enabledFocused) {
            return enabledFocusedTexture(ResourceLocation.parse(enabledFocused));
        }

        /**
         * Sets the texture used when the button is inactive and hovered or focused.
         *
         * @param disabledFocused the texture to draw while disabled and focused
         * @return this builder
         * @since 1.9.0
         */
        public TexturedButtonBuilder disabledFocusedTexture(ResourceLocation disabledFocused) {
            this.disabledFocused = disabledFocused;
            return this;
        }

        /**
         * Sets the disabled, focused texture from its resource-location string; an omitted namespace defaults to {@code minecraft}.
         *
         * @param disabledFocused the texture to draw while disabled and focused
         * @return this builder
         * @since 1.9.3
         */
        public TexturedButtonBuilder disabledFocusedTexture(String disabledFocused) {
            return disabledFocusedTexture(ResourceLocation.parse(disabledFocused));
        }

        /** Creates and wraps a new texture-based button from this builder's current configuration without adding it to the screen or applying shared post-creation styles. Use {@code build()} to create, configure, and add the button.
         * @return the newly wrapped texture-based button
         */
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
