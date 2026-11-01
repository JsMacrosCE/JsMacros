package com.jsmacrosce.jsmacros.client.api.helper.screen;

import net.minecraft.client.gui.components.LockIconButton;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.render.IScreen;
import com.jsmacrosce.jsmacros.core.MethodWrapper;

import java.util.concurrent.atomic.AtomicReference;

/**
 * A lock button toggles between locked and unlocked when pressed. Its Minecraft widget
 * is fixed at 20 by 20 pixels; its builder's size settings do not change that.
 * <p>
 * {@code IScreen.lockButtonBuilder()} starts unlocked; its boolean overload sets the
 * initial state.
 * <pre>
 * const screen = Hud.createScreen("a screen", true);
 * screen.setOnInit(JavaWrapper.methodToJava(function (self) {
 *   const lock = self.lockButtonBuilder()
 *      .pos(10, 20)
 *      .action(JavaWrapper.methodToJava(function (btn) {
 *        Chat.log(`locked: ${btn.isLocked()}`);
 *      }))
 *      .build();
 *
 *   // the state can be set outright, without going through a press
 *   lock.setLocked(true);
 *   Chat.log(`locked: ${lock.isLocked()}`);
 * }));
 * Hud.openScreen(screen);
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Screen and UI Elements")
@SuppressWarnings("unused")
public class LockButtonWidgetHelper extends ClickableWidgetHelper<LockButtonWidgetHelper, LockIconButton> {

    /** Wraps the supplied lock button with the default z-index of zero.
     *
     * @param btn the lock button to wrap
     */
    public LockButtonWidgetHelper(LockIconButton btn) {
        super(btn);
    }

    /** Wraps the supplied lock button with the given drawing-order z-index.
     *
     * @param btn the lock button to wrap
     * @param zIndex the helper's z-index for screen drawing order
     */
    public LockButtonWidgetHelper(LockIconButton btn, int zIndex) {
        super(btn, zIndex);
    }

    /**
     * Returns the button's current locked state.
     *
     * @return {@code true} if the button is locked, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isLocked() {
        return base.isLocked();
    }

    /**
     * Sets the locked state directly without pressing the button or invoking its action.
     * The visual state is used on the next render.
     *
     * @param locked whether to lock the button or not
     * @return self for chaining.
     * @since 1.8.4
     */
    public LockButtonWidgetHelper setLocked(boolean locked) {
        base.setLocked(locked);
        return this;
    }

    /** Returns a diagnostic string containing the current message and locked state.
     * The message text is inserted without JSON escaping.
     *
     * @return a formatted diagnostic in the form {@code LockButtonWidgetHelper:{"message": "%s", "locked": %b}}
     */
    @Override
    public String toString() {
        return String.format("LockButtonWidgetHelper:{\"message\": \"%s\", \"locked\": %b}", base.getMessage().getString(), isLocked());
    }

    /**
     * Builder for a lock button bound to its screen. It starts unlocked unless the
     * boolean overload of {@code IScreen.lockButtonBuilder()} specifies otherwise.
     *
     * @author Etheradon
     * @since 1.8.4
     */
    @DocletCategory("Screen and UI Elements")
    public static class LockButtonBuilder extends AbstractWidgetBuilder<LockButtonBuilder, LockIconButton, LockButtonWidgetHelper> {

        private boolean locked = false;
        @Nullable
        private MethodWrapper<LockButtonWidgetHelper, IScreen, Object, ?> action;

        /** Stores the screen to which {@code build()} will add the created widget.
         *
         * @param screen the screen associated with this builder
         */
        public LockButtonBuilder(IScreen screen) {
            super(screen);
        }

        /**
         * Returns the initial locked state configured for this builder. The default is
         * unlocked; this does not track the button after it is built.
         *
         * @return the initial state of the lock button.
         * @since 1.8.4
         */
        public boolean isLocked() {
            return locked;
        }

        /**
         * Sets the button's initial locked state; it does not constrain later changes.
         *
         * @param locked whether to initially lock the button or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public LockButtonBuilder locked(boolean locked) {
            this.locked = locked;
            return this;
        }

        /**
         * @return the configured callback, or {@code null} if none is set.
         * @since 1.8.4
         */
        @Nullable
        public MethodWrapper<LockButtonWidgetHelper, IScreen, Object, ?> getAction() {
            return action;
        }

        /**
         * @param action callback invoked when the button is pressed; {@code null} disables it
         * @return self for chaining.
         * @since 1.8.4
         */
        public LockButtonBuilder action(@Nullable MethodWrapper<LockButtonWidgetHelper, IScreen, Object, ?> action) {
            this.action = action;
            return this;
        }

        /** Creates the lock-button helper using the configured position, z-index, and initial locked state.
         * If an action is configured, it receives this helper and the associated screen; exceptions are logged.
         * The callback then marks the next release to be skipped only for container screens.
         * This method does not add the widget to the screen or apply the builder's label, size, alpha,
         * active, or visible settings; {@code build()} performs the addition and applies alpha, active,
         * and visible settings.
         *
         * @return the helper for the newly created lock button
         */
        @Override
        public LockButtonWidgetHelper createWidget() {
            AtomicReference<LockButtonWidgetHelper> b = new AtomicReference<>(null);
            LockIconButton lockButton = new LockIconButton(getX(), getY(), btn -> {
                try {
                    if (action != null) {
                        action.accept(b.get(), screen);
                    }
                } catch (Exception e) {
                    JsMacrosClient.clientCore.profile.logError(e);
                }
                clickedOn(screen);
            });
            if (locked) {
                lockButton.setLocked(true);
            }
            b.set(new LockButtonWidgetHelper(lockButton, getZIndex()));
            return b.get();
        }

    }

}
