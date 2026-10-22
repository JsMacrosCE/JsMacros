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
 * The screen implementation returned by {@code Hud.createScreen}, exposed through {@link IScreen}.
 * <p>
 * <pre>
 * const screen = Hud.createScreen("my screen", true);
 * // Hide the rendered title text without changing the screen's title.
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
     * Whether the screen title is drawn. Changing this field affects subsequent renders;
     * it does not change the title returned by {@link IScreen#getTitleText()}.
     *
     * @since 1.0.5
     */
    public boolean drawTitle;
    /**
     * Whether this screen permits closing with Escape; an open overlay can still prevent
     * closing.
     * @since 1.8.4
     */
    public boolean shouldCloseOnEsc = true;
    /** Whether the game pauses while this screen is open.
     * @since 1.8.4
     */
    public boolean shouldPause = true;
    private final boolean menuBackground;
    private final boolean blurredBackground;
    @Nullable
    private MethodWrapper<Pos3D, GuiGraphics, Object, ?> onRender;

    /**
     * Sets up a screen with the supplied literal title and both background effects enabled: the
     * game's menu background overlay is drawn and the world behind it is blurred.
     * The title must be non-null; an empty title is allowed. This constructor does not open the screen.
     * Passing {@code null} for {@code title} causes a {@code NullPointerException}.
     *
     * @param title non-null literal screen title, not a translation key
     * @param menuBackground whether to draw the game's menu background overlay
     */
    public ScriptScreen(String title, boolean menuBackground) {
        this(title, menuBackground, true);
    }

    /**
     * Sets up a screen with the supplied literal title and background effects.
     * The title must be non-null; an empty title is allowed. This constructor does not open the screen.
     * Passing {@code null} for {@code title} causes a {@code NullPointerException}.
     * <p>
     * The two effects are independent. The menu background is vanilla's darkened background: on
     * the supported versions it is a flat translucent black overlay rather than a picture, so it
     * only darkens what is behind the screen. The blurred background applies vanilla's menu blur
     * to the world behind the screen. With both off, the world is shown unmodified.
     *
     * @param title non-null literal screen title, not a translation key
     * @param menuBackground whether to draw the game's menu background overlay
     * @param blurredBackground whether to blur the world behind the screen
     * @since 2.0.0
     */
    public ScriptScreen(String title, boolean menuBackground, boolean blurredBackground) {
        super(Component.literal(title), null);
        this.menuBackground = menuBackground;
        this.blurredBackground = blurredBackground;
        this.drawTitle = true;
    }

    //? if >=26.1 {
    /*@Override
    protected void extractMenuBackground(GuiGraphicsExtractor drawContext) {
        if (!menuBackground) {
            return;
        }
        super.extractMenuBackground(drawContext);
    }
    *///?} else {
    @Override
    protected void renderMenuBackground(GuiGraphics drawContext) {
        if (!menuBackground) {
            return;
        }
        super.renderMenuBackground(drawContext);
    }
    //?}

    //? if >=26.1 {
    /*@Override
    protected void extractBlurredBackground(GuiGraphicsExtractor drawContext) {
        if (!blurredBackground) {
            return;
        }
        super.extractBlurredBackground(drawContext);
    }
    *///? } else if >1.21.5 {
    @Override
    protected void renderBlurredBackground(GuiGraphics drawContext) {
        if (!blurredBackground) {
            return;
        }
        super.renderBlurredBackground(drawContext);
    }
    //?} else {
    /*@Override
    protected void renderBlurredBackground() {
        if (!blurredBackground) {
            return;
        }
        super.renderBlurredBackground();
    }
    *///?}

    @Override
    protected void init() {
        BaseScreen prev = JsMacrosClient.prevScreen;
        super.init();
        JsMacrosClient.prevScreen = prev;
    }

    /**
     * Sets the screen opened when this screen closes; without a parent, close behavior
     * returns to the game if a world is loaded, or to no screen otherwise.
     *
     * @param parent screen to open on close
     * @since 1.4.0
     */
    public void setParent(IScreen parent) {
        this.parent = (net.minecraft.client.gui.screens.Screen) parent;
    }

    /**
     * Sets a callback that draws after the screen's widgets and overlay elements.
     * <p>
     * It runs on the render path with mouse coordinates and tick delta in a
     * {@link Pos3D}, plus the graphics object.
     * <p>
     * If the callback throws, the error is logged and the callback is cleared.
     *
     * @param onRender callback receiving mouse x, mouse y, and tick delta in a {@link Pos3D}, then the graphics object
     * @since 1.4.0
     */
    public void setOnRender(@Nullable MethodWrapper<Pos3D, GuiGraphics, Object, ?> onRender) {
        this.onRender = onRender;
    }

    /**
     * Renders this screen.
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
     * @param delta frame delta in game ticks; not limited to 0–1
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
        if (menuBackground) {
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
     * Closes this screen, opening its parent if set; otherwise uses the superclass close behavior.
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
     * Whether the game pauses while this screen is open.
     *
     * @return whether the game pauses
     * @author Wagyourtail
     * @since 1.0.5
     */
    @Override
    public boolean isPauseScreen() {
        return shouldPause;
    }

    /**
     * Whether Escape closes this screen. The result also depends on the superclass,
     * which prevents closing while an overlay is open.
     *
     * @return whether this screen and its superclass permit Escape closing
     * @author Wagyourtail
     * @since 1.0.5
     */
    @Override
    public boolean shouldCloseOnEsc() {
        return shouldCloseOnEsc && super.shouldCloseOnEsc();
    }

}
