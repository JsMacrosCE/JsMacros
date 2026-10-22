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
 * a push button: the plain game button that is drawn with the game's own look and does
 * whatever it was given to do when it is pressed.
 * <p>
 * Everything about position, size, label, tooltips and the flags is on the shared base
 * class, and this adds only what is particular to a button. The two builders on a
 * screen are where a script makes one: {@code IScreen.buttonBuilder()} for the plain
 * look and {@code IScreen.texturedButtonBuilder()} for a button drawn from a texture.
 * <p>
 * The button's height is fixed at twenty whatever a builder is asked for, which is the
 * height the game draws its own buttons at.
 * example:
 * <pre>
 * const screen = Hud.createScreen("a screen", true);
 * const button = screen.buttonBuilder()
 *    .pos(10, 20)
 *    .size(100, 20)
 *    .message("press me")
 *    .action(JavaWrapper.methodToJava(function (btn) {
 *      // the callback is given the button and the screen it was made on
 *      btn.setLabel("pressed");
 *    }))
 *    .build();
 * Hud.openScreen(screen);
 *
 * // a script can press its own button rather than leaving it to the player
 * button.click();
 * Chat.log(`the label now reads ${button.getLabel().getString()}`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Screen and UI Elements")
public class ButtonWidgetHelper<T extends Button> extends ClickableWidgetHelper<ButtonWidgetHelper<T>, T> {

    public ButtonWidgetHelper(T btn) {
        super(btn);
    }

    public ButtonWidgetHelper(T btn, int zIndex) {
        super(btn, zIndex);
    }

    /**
     * the builder for a button with the game's own look, handed back by
     * {@code IScreen.buttonBuilder()} and already bound to that screen.
     * <p>
     * The height is the one thing this pins: a button is always built twenty tall,
     * whatever it is asked for, which is the height the game draws its own buttons at.
     * Everything else comes from the shared builder.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.buttonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .message("press me")
     *    .action(JavaWrapper.methodToJava(function (btn, onScreen) {
     *       // the callback is given the button and the screen
     *       Chat.log(`pressed on the screen titled ${onScreen.getTitleText()}`);
     *     }))
     *    .build();
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
     * the builder for a button drawn from a texture rather than with the game's own look,
     * handed back by {@code IScreen.texturedButtonBuilder()} and already bound to that
     * screen.
     * <p>
     * The four texture setters are the four states the button is drawn in: enabled,
     * disabled, enabled and focused, disabled and focused. They go into the button's
     * sprite set exactly as given with no defaulting to the enabled one, so a texture
     * has to be given for every state the button can be seen in.
     * <p>
     * The height is pinned at twenty here as it is on the plain button builder.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.texturedButtonBuilder()
     *    .pos(10, 20)
     *    .size(100, 20)
     *    .enabledTexture("minecraft:item/diamond")
     *    .disabledTexture("minecraft:item/emerald")
     *    .enabledFocusedTexture("minecraft:item/diamond")
     *    .disabledFocusedTexture("minecraft:item/emerald")
     *    .build();
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
         * the texture drawn while the button is enabled and not focused. The button can
         * be drawn in this state without ever being focused if the mouse never reaches
         * it, so a texture is needed here even for a button that is only ever clicked by
         * script.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * screen.texturedButtonBuilder()
         *    .pos(10, 20)
         *    .size(100, 20)
         *    .enabledTexture("minecraft:item/diamond")
         *    .build();
         * Hud.openScreen(screen);
         * </pre>
         *
         * @param enabled the texture to draw while enabled and not focused
         * @return self for chaining.
         * @since 1.9.0
         */
        public TexturedButtonBuilder enabledTexture(ResourceLocation enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * the same as the {@link ResourceLocation} form, with the texture named as a
         * string. The namespace may be left off, in which case it is read as
         * {@code minecraft}.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * screen.texturedButtonBuilder()
         *    .pos(10, 20)
         *    .size(100, 20)
         *    .enabledTexture("item/diamond")
         *    .build();
         * Hud.openScreen(screen);
         * </pre>
         *
         * @param enabled the texture to draw while enabled and not focused
         * @return self for chaining.
         * @since 1.9.3
         */
        public TexturedButtonBuilder enabledTexture(String enabled) {
            return enabledTexture(ResourceLocation.parse(enabled));
        }

        /**
         * the texture drawn while the button is disabled, which is the greyed out look.
         * A button that is never made inactive still needs one here, because the button
         * goes to this state whenever its active flag is turned off.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * screen.texturedButtonBuilder()
         *    .pos(10, 20)
         *    .size(100, 20)
         *    .enabledTexture("minecraft:item/diamond")
         *    .disabledTexture("minecraft:item/emerald")
         *    .build();
         * Hud.openScreen(screen);
         * </pre>
         *
         * @param disabled the texture to draw while disabled
         * @return self for chaining.
         * @since 1.9.0
         */
        public TexturedButtonBuilder disabledTexture(ResourceLocation disabled) {
            this.disabled = disabled;
            return this;
        }

        /**
         * the same as the {@link ResourceLocation} form, with the texture named as a
         * string. The namespace may be left off, in which case it is read as
         * {@code minecraft}.
         *
         * @param disabled the texture to draw while disabled
         * @return self for chaining.
         * @since 1.9.3
         */
        public TexturedButtonBuilder disabledTexture(String disabled) {
            return disabledTexture(ResourceLocation.parse(disabled));
        }

        /**
         * the texture drawn while the button is enabled and focused, which is the state
         * it is in under the mouse. This is a separate texture rather than the enabled
         * one with a highlight, so a button that is never focused still needs one.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * screen.texturedButtonBuilder()
         *    .pos(10, 20)
         *    .size(100, 20)
         *    .enabledTexture("minecraft:item/diamond")
         *    .disabledTexture("minecraft:item/emerald")
         *    .enabledFocusedTexture("minecraft:item/emerald")
         *    .disabledFocusedTexture("minecraft:item/coal")
         *    .build();
         * Hud.openScreen(screen);
         * </pre>
         *
         * @param enabledFocused the texture to draw while enabled and focused
         * @return self for chaining.
         * @since 1.9.0
         */
        public TexturedButtonBuilder enabledFocusedTexture(ResourceLocation enabledFocused) {
            this.enabledFocused = enabledFocused;
            return this;
        }

        /**
         * the same as the {@link ResourceLocation} form, with the texture named as a
         * string. The namespace may be left off, in which case it is read as
         * {@code minecraft}.
         *
         * @param enabledFocused the texture to draw while enabled and focused
         * @return self for chaining.
         * @since 1.9.3
         */
        public TexturedButtonBuilder enabledFocusedTexture(String enabledFocused) {
            return enabledFocusedTexture(ResourceLocation.parse(enabledFocused));
        }

        /**
         * the texture drawn while the button is disabled and focused, which is the one
         * state that needs all four textures filled in to be seen at all. A button that
         * is disabled and has the mouse over it is drawn with this.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * const button = screen.texturedButtonBuilder()
         *    .pos(10, 20)
         *    .size(100, 20)
         *    .enabledTexture("minecraft:item/diamond")
         *    .disabledTexture("minecraft:item/emerald")
         *    .enabledFocusedTexture("minecraft:item/emerald")
         *    .disabledFocusedTexture("minecraft:item/coal")
         *    .active(false)
         *    .build();
         * Hud.openScreen(screen);
         * </pre>
         *
         * @param disabledFocused the texture to draw while disabled and focused
         * @return self for chaining.
         * @since 1.9.0
         */
        public TexturedButtonBuilder disabledFocusedTexture(ResourceLocation disabledFocused) {
            this.disabledFocused = disabledFocused;
            return this;
        }

        /**
         * the same as the {@link ResourceLocation} form, with the texture named as a
         * string. The namespace may be left off, in which case it is read as
         * {@code minecraft}.
         *
         * @param disabledFocused the texture to draw while disabled and focused
         * @return self for chaining.
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
