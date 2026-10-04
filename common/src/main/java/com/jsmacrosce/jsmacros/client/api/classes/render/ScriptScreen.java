package com.jsmacrosce.jsmacros.client.api.classes.render;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.access.IScreenInternal;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.wagyourgui.BaseScreen;

/**
 * just go look at {@link IScreen IScreen}
 * since all the methods are done through a mixin...
 * <p>
 * That is the interface a script actually uses: this class is what
 * {@code Hud.createScreen(String, boolean)} hands back, and almost everything on it
 * comes from the mixin that makes any screen an {@code IScreen}. Read that for the
 * widgets, the element methods, the input callbacks and the builders, and read this for
 * the handful of things a screen has that an overlay does not.
 * <p>
 * What a screen has and a 2D overlay does not is that it is <em>opened</em> rather than
 * registered, and that it is the game's own screen class, so opening one pauses the game
 * in singleplayer and takes the mouse. Its elements and widgets are in the same screen
 * space as an overlay's and mean the same thing.
 * <p>
 * The three public fields here are the settings a script screen has of its own. The
 * {@code dirt} argument to the constructor decides the background rather than being a
 * field of its own: {@code true} asks for the game's own background and {@code false}
 * leaves it transparent, so the world shows straight through. The fields are read by the
 * game on each frame, so changing one takes effect on the next one.
 * example:
 * <pre>
 * const screen = Hud.createScreen("my screen", true);
 * // a screen with no title bar drawn on it
 * screen.drawTitle = false;
 * Hud.openScreen(screen);
 * </pre>
 *
 * @author Wagyourtail
 * @see IScreen
 * @since 1.0.5
 */
@DocletCategory("Screen and UI Elements")
public class ScriptScreen extends BaseScreen {
    /**
     * whether the screen's title is drawn at the top.
     * <p>
     * On, the title is drawn centred near the top of the screen in white. Turning it off
     * hides it without changing the title itself, so
     * {@link IScreen#getTitleText()} still reports it and a script can still put it
     * somewhere else as an ordinary text element.
     * <p>
     * This is a field rather than a setter, and it is {@code true} on a new screen.
     *
     * @since 1.0.5
     */
    public boolean drawTitle;
    /**
     * WARNING: this can break the game if you set it false and don't have a way to close the screen.
     * @since 1.8.4
     */
    public boolean shouldCloseOnEsc = true;
    /**
     * @since 1.8.4
     */
    public boolean shouldPause = true;
    private final int bgStyle;
    @Nullable
    private MethodWrapper<Pos3D, GuiGraphics, Object, ?> onRender;

    public ScriptScreen(String title, boolean dirt) {
        super(Component.literal(title), null);
        this.bgStyle = dirt ? 0 : 1;
        this.drawTitle = true;
    }

    @Override
    protected void init() {
        BaseScreen prev = JsMacrosClient.prevScreen;
        super.init();
        JsMacrosClient.prevScreen = prev;
    }

    /**
     * Closing this screen opens that one instead of whatever was open before, which is
     * what a chain of screens is built from: a settings screen leading back to a menu
     * screen, say. With no parent set the screen goes back to the game itself, which is
     * the same as opening nothing.
     * <p>
     * This replaces the parent rather than adding one, so a screen has exactly one place
     * to return to. It is read when the screen closes, so changing it while the screen is
     * up is enough.
     * example:
     * <pre>
     * const menu = Hud.createScreen("menu", true);
     * const settings = Hud.createScreen("settings", true);
     * // closing settings goes back to the menu, not to the game
     * settings.setParent(menu);
     * Hud.openScreen(settings);
     * </pre>
     *
     * @param parent parent screen to go to when this one exits.
     * @since 1.4.0
     */
    public void setParent(IScreen parent) {
        this.parent = (net.minecraft.client.gui.screens.Screen) parent;
    }

    /**
     * add custom stuff to the render function on the main thread.
     * <p>
     * The function runs at the end of every frame the screen is drawn, after the
     * screen's own elements have been rendered, so anything it adds sits over them. It is
     * given the mouse position and the current tick delta as a three component position,
     * and the game's own graphics object for drawing with.
     * <p>
     * A function that throws is called once more and then discarded: the failure is
     * logged to the current profile and the callback is dropped, so the screen keeps
     * rendering and the rest of the game is unaffected. That is a one-shot rather than a
     * per-frame log, which is worth knowing before relying on it.
     * example:
     * <pre>
     * const screen = Hud.createScreen("a screen", true);
     * screen.setOnRender(JavaWrapper.methodToJava(function (mouse, graphics) {
     *   // mouse is a Pos3D: the x and y are the mouse and the z is the tick delta
     *   const where = `at ${mouse.getX()}, ${mouse.getY()}`;
     *   screen.addText(where, 0, 0, 0xFFFFFFFF, true);
     * }));
     * Hud.openScreen(screen);
     * </pre>
     *
     * @param onRender pos3d elements are mousex, mousey, tickDelta
     * @since 1.4.0
     */
    public void setOnRender(@Nullable MethodWrapper<Pos3D, GuiGraphics, Object, ?> onRender) {
        this.onRender = onRender;
    }

    /**
     * draws the screen: its background, its title, its widgets, its elements, and then
     * whatever the render callback wants to add.
     * <p>
     * The order is the background first if the screen asked for one, then the title if
     * {@code drawTitle} is on, then the widgets the game put on it, then the elements
     * added through this screen's 2D overlay side, and then the function set with
     * {@link #setOnRender(MethodWrapper)}. So the render callback is drawn over
     * everything else and is the right place for something that should sit on top.
     * <p>
     * Nothing here is called by a script; the game calls this once a frame while the
     * screen is open. A callback that throws is logged and dropped, so the rest of the
     * screen still draws.
     *
     * @param drawContext the game's own graphics object
     * @param mouseX the current mouse position, in the same space as the screen's
     *               elements
     * @param mouseY the current mouse position, in the same space as the screen's
     *               elements
     * @param delta how far into the current tick this frame is, from 0 to 1
     * @author Wagyourtail
     * @since 1.0.5
     */
    @Override
    //? if >=26.1 {
    /*public void extractRenderState(final GuiGraphicsExtractor drawContext, int mouseX, int mouseY, final float delta) {
    *///?} else {
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
    //?}
        if (drawContext == null) {
            return;
        }
        if (bgStyle == 0) {
            //? if >=26.1 {
            /*this.extractMenuBackground(drawContext);
            *///?} else {
            this.renderMenuBackground(drawContext);
            //?}
        }

        if (drawTitle) {
            //? if >=26.1 {
            /*drawContext.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
            *///?} else {
            drawContext.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
            //?}
        }

        //? if >=26.1 {
        /*super.extractRenderState(drawContext, mouseX, mouseY, delta);
        *///?} else {
        super.render(drawContext, mouseX, mouseY, delta);
        //?}

        for (GuiEventListener button : ImmutableList.copyOf(this.children())) {
            if (!(button instanceof Renderable)) {
                continue;
            }
            //? if >=26.1 {
            /*((Renderable) button).extractRenderState(drawContext, mouseX, mouseY, delta);
            *///?} else {
            ((Renderable) button).render(drawContext, mouseX, mouseY, delta);
            //?}
        }

        ((IScreenInternal) this).jsmacros_render(drawContext, mouseX, mouseY, delta);
        try {
            if (onRender != null) {
                onRender.accept(new Pos3D(mouseX, mouseY, delta), drawContext);
            }
        } catch (Throwable e) {
            JsMacrosClient.clientCore.profile.logError(e);
            onRender = null;
        }
    }

    /**
     * closes this screen and goes back to whatever it was opened from.
     * <p>
     * This is the same as the player closing it, and it is what
     * {@link IScreen#close()} ends up calling. It opens the parent screen rather than
     * closing to the game directly, so a screen with no parent set goes back to the
     * game itself; see {@link #setParent(IScreen)} for changing that.
     * <p>
     * With a parent set this opens that parent; otherwise it follows the superclass's normal
     * close behavior. A parentless screen does not require a custom close action to leave it.
     *
     * @author Wagyourtail
     * @since 1.0.5
     */
    @Override
    public void onClose() {
        if (parent != null) {
            openParent();
        } else {
            super.onClose();
        }
    }

    /**
     * whether the game is paused while this screen is open.
     * <p>
     * This is the field {@code shouldPause} read at the moment the game asks, so
     * changing that field takes effect the next time a frame goes by. In singleplayer
     * {@code true} is what stops the world moving behind the screen.
     * <p>
     * A screen that pauses cannot be closed by the world going on, so anything that has
     * to keep running while one is open has to come from somewhere else.
     *
     * @return the value of the {@code shouldPause} field
     * @author Wagyourtail
     * @since 1.0.5
     */
    @Override
    public boolean isPauseScreen() {
        return shouldPause;
    }

    /**
     * whether escape closes this screen.
     * <p>
     * Two things have to agree: the {@code shouldCloseOnEsc} field on this screen, and
     * whatever the underlying screen says, which is false while an overlay is open on
     * top of it. Both being true is what lets escape through, so setting the field
     * {@code false} is enough to keep the screen up no matter what else is going on.
     * <p>
     * The game reads this as the key is handled, so a screen that sets it
     * {@code false} needs its own way out, such as a close button.
     *
     * @return the {@code shouldCloseOnEsc} field, anded with what the underlying screen
     *         says
     * @author Wagyourtail
     * @since 1.0.5
     */
    @Override
    public boolean shouldCloseOnEsc() {
        return shouldCloseOnEsc && super.shouldCloseOnEsc();
    }

}
