package com.jsmacrosce.jsmacros.client.api.helper.screen;

import net.minecraft.client.gui.components.LockIconButton;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.render.IScreen;
import com.jsmacrosce.jsmacros.core.MethodWrapper;

import java.util.concurrent.atomic.AtomicReference;

/**
 * the padlock a real screen uses to hold a setting still: a small button that is either
 * locked or unlocked, and that the player clicks to change which.
 * <p>
 * The padlock is drawn at a fixed size of its own, so a script building one only sets
 * where it goes. The size on the shared builder is not what decides how big the padlock
 * looks.
 * <p>
 * A script makes one through {@code IScreen.lockButtonBuilder()}, which starts unlocked,
 * or the form of that taking a boolean, which starts in the state given.
 * example:
 * <pre>
 * const screen = Hud.createScreen("a screen", true);
 * const lock = screen.lockButtonBuilder()
 *    .pos(10, 20)
 *    .action(JavaWrapper.methodToJava(function (btn) {
 *      Chat.log(`locked: ${btn.isLocked()}`);
 *    }))
 *    .build();
 * Hud.openScreen(screen);
 *
 * // the state can be set outright, without going through a press
 * lock.setLocked(true);
 * Chat.log(`locked: ${lock.isLocked()}`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Screen and UI Elements")
@SuppressWarnings("unused")
public class LockButtonWidgetHelper extends ClickableWidgetHelper<LockButtonWidgetHelper, LockIconButton> {

    public LockButtonWidgetHelper(LockIconButton btn) {
        super(btn);
    }

    public LockButtonWidgetHelper(LockIconButton btn, int zIndex) {
        super(btn, zIndex);
    }

    /**
     * whether the padlock is currently locked, read at the moment of the call. A lock
     * the player has just clicked does not agree with what a script last set, so this is
     * the one to read.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const lock = screen.lockButtonBuilder().pos(10, 20).build();
     * Hud.openScreen(screen);
     * // a player can have clicked it, so read the lock rather than a value kept aside
     * Chat.log(lock.isLocked() ? "locked" : "unlocked");
     * </pre>
     *
     * @return {@code true} if the button is locked, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isLocked() {
        return base.isLocked();
    }

    /**
     * locks or unlocks the padlock outright. This writes the state rather than pressing
     * the button, so the action given to the builder does not run; a script that wants
     * the action to see the change has to press the button instead.
     * <p>
     * The new state takes on the next draw rather than immediately, and it is the state
     * {@link #isLocked()} reads back.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const lock = screen.lockButtonBuilder().pos(10, 20).build();
     * Hud.openScreen(screen);
     * // written straight to the widget, so no action runs
     * lock.setLocked(true);
     * Chat.log(`locked: ${lock.isLocked()}`);
     * </pre>
     *
     * @param locked whether to lock the button or not
     * @return self for chaining.
     * @since 1.8.4
     */
    public LockButtonWidgetHelper setLocked(boolean locked) {
        base.setLocked(locked);
        return this;
    }

    @Override
    public String toString() {
        return String.format("LockButtonWidgetHelper:{\"message\": \"%s\", \"locked\": %b}", base.getMessage().getString(), isLocked());
    }

    /**
     * the builder for a padlock, handed back by {@code IScreen.lockButtonBuilder()} and
     * already bound to that screen. It starts unlocked, and the form of that taking a
     * boolean starts in the state given.
     * <p>
     * The padlock is drawn at a size of its own, so a builder for one usually only sets
     * where it goes. The state set here is the starting state, and a lock on screen can
     * be changed afterwards through the helper.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * const builder = screen.lockButtonBuilder(true)
     *    .pos(10, 20);
     * // the starting state is readable on the builder before anything is built
     * Chat.log(`starts locked: ${builder.isLocked()}`);
     * const lock = builder.build();
     * Hud.openScreen(screen);
     * Chat.log(`built locked: ${lock.isLocked()}`);
     * </pre>
     *
     * @author Etheradon
     * @since 1.8.4
     */
    @DocletCategory("Screen and UI Elements")
    public static class LockButtonBuilder extends AbstractWidgetBuilder<LockButtonBuilder, LockIconButton, LockButtonWidgetHelper> {

        private boolean locked = false;
        @Nullable
        private MethodWrapper<LockButtonWidgetHelper, IScreen, Object, ?> action;

        public LockButtonBuilder(IScreen screen) {
            super(screen);
        }

        /**
         * whether the padlock will be built locked, which is only the starting state and
         * says nothing about the lock after it has been on screen. It starts unlocked.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * const builder = screen.lockButtonBuilder();
         * Chat.log(`starts locked: ${builder.isLocked()}`);
         * builder.locked(true);
         * Chat.log(`now: ${builder.isLocked()}`);
         * </pre>
         *
         * @return the initial state of the lock button.
         * @since 1.8.4
         */
        public boolean isLocked() {
            return locked;
        }

        /**
         * sets the state the padlock is built locked in. This is only where it starts:
         * the lock can be changed afterwards through the helper, and a player can click
         * it, so this number is not what it will go on being.
         * example:
         * <pre>
         * const screen = Hud.createScreen("a screen", true);
         * screen.lockButtonBuilder()
         *    .pos(10, 20)
         *    .locked(true)
         *    .build();
         * Hud.openScreen(screen);
         * </pre>
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
         * @return the action to run when the button is pressed.
         * @since 1.8.4
         */
        @Nullable
        public MethodWrapper<LockButtonWidgetHelper, IScreen, Object, ?> getAction() {
            return action;
        }

        /**
         * @param action the action to run when the button is pressed
         * @return self for chaining.
         * @since 1.8.4
         */
        public LockButtonBuilder action(@Nullable MethodWrapper<LockButtonWidgetHelper, IScreen, Object, ?> action) {
            this.action = action;
            return this;
        }

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
