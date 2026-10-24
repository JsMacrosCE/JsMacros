package com.jsmacrosce.jsmacros.client.api.library.impl;

import com.jsmacrosce.jsmacros.client.util.GuiCompat;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import org.jetbrains.annotations.Nullable;

import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.doclet.DocletDeclareType;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.CustomImage;
import com.jsmacrosce.jsmacros.client.api.classes.render.*;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.library.BaseLibrary;
import com.jsmacrosce.jsmacros.core.library.Library;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Screen, overlay, texture, and window-state APIs exposed to scripts as {@code Hud}.
 * <p>
 * 2D overlay coordinates and mouse positions use GUI-scaled pixels from the window's top left;
 * window width and height are raw pixels. 3D overlays use world coordinates. Overlays render only
 * while registered; use their own {@code register()} and {@code unregister()} methods. The clear
 * methods remove all overlays of their respective dimension, including those registered by other
 * scripts.
 * <pre>
 * // draw 2D text in the overlay and register it
 * const draw = Hud.createDraw2D();
 * draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
 * draw.register();
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.0.5
 */
@Library("Hud")
@SuppressWarnings("unused")
public class FHud extends BaseLibrary {

    private static final Minecraft mc = Minecraft.getInstance();
    /**
     * Set of 2D overlays currently registered for rendering.
     * @since 1.0.5
     */
    public static final Set<IDraw2D<Draw2D>> overlays = ConcurrentHashMap.newKeySet();
    /**
     * Set of 3D overlays currently registered for rendering.
     * @since 1.0.6
     */
    public static final Set<Draw3D> renders = ConcurrentHashMap.newKeySet();

    /**
     * Associates this HUD library instance with its core; constructing it does not reset the shared 2D or 3D overlay registrations.
     *
     * @param runner core associated with this library
     */
    public FHud(Core<?, ?> runner) {
        super(runner);
    }

    // Before 1.20.5 vanilla tiled a dirt texture over the world for this menu background; on the
    // supported versions it is a flat translucent black tile, and the world can also be blurred.
    /**
     * Creates a script screen with the given title and both background effects enabled: the game's
     * menu background overlay is drawn and the world behind it is blurred. The title is drawn by
     * default; set {@code drawTitle} on the returned screen to disable it.
     *
     * @param title the title shown at the top of the screen
     * @param menuBackground whether to draw the game's menu background overlay
     * @return the new screen, not opened automatically
     * @since 1.0.5
     */
    public ScriptScreen createScreen(String title, boolean menuBackground) {
        return createScreen(title, menuBackground, true);
    }

    /**
     * Creates a script screen with the given title and background effects. The title is drawn by
     * default; set {@code drawTitle} on the returned screen to disable it.
     * <p>
     * The two effects are independent. The menu background is vanilla's darkened background: on
     * the supported versions it is a flat translucent black overlay rather than a picture, so it
     * only darkens what is behind the screen. The blurred background applies vanilla's menu blur
     * to the world behind the screen. With both off, the world is shown unmodified.
     *
     * @param title the title shown at the top of the screen
     * @param menuBackground whether to draw the game's menu background overlay
     * @param blurredBackground whether to blur the world behind the screen
     * @return the new screen, not opened automatically
     * @since 2.0.0
     */
    public ScriptScreen createScreen(String title, boolean menuBackground, boolean blurredBackground) {
        return new ScriptScreen(title, menuBackground, blurredBackground);
    }

    /**
     * Opens a screen, or closes the current screen when passed {@code null}. The change is queued
     * on the client thread, so it may not have taken effect when this method returns.
     *
     * @param s the screen to open, or {@code null} to close the current one
     * @since 1.0.5
     */
    public void openScreen(@Nullable IScreen s) {
        net.minecraft.client.gui.screens.Screen screen = (net.minecraft.client.gui.screens.Screen) s;
        mc.execute(() -> GuiCompat.setScreen(mc, screen));
    }

    /**
     * Returns the currently open screen, if any.
     *
     * @return the currently open screen, or {@code null} if none
     * @since 1.2.7
     */
    @Nullable
    public IScreen getOpenScreen() {
        return (IScreen) GuiCompat.screen(mc);
    }

    /**
     * Creates a mutable blank image and registers it under {@code name} when non-null.
     *
     * @param width  the width of the canvas
     * @param height the height of the canvas
     * @param name   the name to register the texture under, or {@code null} for none
     * @return the new mutable image
     * @since 1.8.4
     */
    public CustomImage createTexture(int width, int height, @Nullable String name) {
        return CustomImage.createWidget(width, height, name);
    }

    /**
     * Loads an image from a path relative to the JsMacros config folder. Logs an error and returns
     * {@code null} if reading fails or no image reader recognizes the file.
     *
     * @param path path to an image file, relative to the JsMacros config folder
     * @param name the name to register the texture under, or {@code null} for none
     * @return the loaded image, or {@code null} if reading fails or the format is unsupported
     * @since 1.8.4
     */
    public CustomImage createTexture(String path, @Nullable String name) {
        return CustomImage.createWidget(path, name);
    }

    /**
     * Returns a snapshot of the registered custom textures, keyed by registration name.
     *
     * @return an immutable snapshot of the registered textures
     * @since 1.8.4
     */
    public Map<String, CustomImage> getRegisteredTextures() {
        return ImmutableMap.copyOf(CustomImage.IMAGES);
    }

    /**
     * Returns the configured GUI scale option; automatic scaling is reported as {@code 0}.
     *
     * @return the configured GUI scale option
     * @since 1.8.4
     */
    public int getScaleFactor() {
        return mc.options.guiScale().get();
    }

    /**
     * Returns a name for the open screen: recognized screens use fixed names, other screens use
     * their title (or {@code unknown} when it is empty). Returns {@code null} when no screen is open.
     *
     * @return the screen name, or {@code null} if no screen is open
     * @since 1.0.5, renamed from {@code getOpenScreen} in 1.2.7
     */
    @SuppressWarnings("SpellCheckingInspection")
    @DocletReplaceReturn("ScreenName | null")
    @DocletDeclareType(name = "HandledScreenName", type =
            """
            | `${ 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 } Row Chest`
            | '3x3 Container'
            | 'Anvil'
            | 'Beacon'
            | 'Blast Furnace'
            | 'Brewing Stand'
            | 'Crafting Table'
            | 'Enchanting Table'
            | 'Furnace'
            | 'Grindstone'
            | 'Hopper'
            | 'Loom'
            | 'Villager'
            | 'Shulker Box'
            | 'Smithing Table'
            | 'Smoker'
            | 'Cartography Table'
            | 'Stonecutter'
            | 'Survival Inventory'
            | 'Horse'
            | 'Creative Inventory'
            | 'Chat'
            | string & {}
            | 'unknown'
            """
    )
    @Nullable
    public String getOpenScreenName() {
        return JsMacrosClient.getScreenName(GuiCompat.screen(mc));
    }

    /**
     * Returns whether the current screen is a container screen.
     *
     * @return {@code true} if the current screen is a container screen
     * @since 1.1.2
     */
    public boolean isContainer() {
        return GuiCompat.screen(mc) instanceof AbstractContainerScreen;
    }

    /**
     * Creates an unregistered 2D overlay. It renders while registered; registering runs its init
     * function. Coordinates use GUI-scaled pixels from the top left, as do {@link #getMouseX()} and
     * {@link #getMouseY()}.
     *
     * @return a new, unregistered overlay
     * @see IDraw2D
     * @since 1.0.5
     */
    public Draw2D createDraw2D() {
        return new Draw2D();
    }

    /**
     * Initializes and registers a 2D overlay. Prefer {@code register()} on the overlay itself.
     *
     * @param overlay the overlay to start rendering
     * @see IDraw2D
     * @since 1.0.5
     * @deprecated since 1.6.5, use {@link Draw2D#register()} instead.
     */
    @Deprecated
    @DocletIgnore
    public void registerDraw2D(IDraw2D<Draw2D> overlay) {
        ((Draw2D) overlay).init();
        overlays.add(overlay);
    }

    /**
     * Removes a 2D overlay from rendering. Prefer {@code unregister()} on the overlay itself.
     *
     * @param overlay the overlay to stop rendering
     * @see IDraw2D
     * @since 1.0.5
     * @deprecated since 1.6.5, use {@link Draw2D#unregister()} instead.
     */
    @Deprecated
    @DocletIgnore
    public void unregisterDraw2D(IDraw2D<Draw2D> overlay) {
        overlays.remove(overlay);
    }

    /**
     * Returns a snapshot of all currently registered 2D overlays, including those registered by other scripts.
     *
     * @return the currently registered overlays
     * @see IDraw2D
     * @since 1.0.5
     */
    public List<IDraw2D<Draw2D>> listDraw2Ds() {
        return ImmutableList.copyOf(overlays);
    }

    /**
     * Unregisters all 2D overlays, including those registered by other scripts; the overlay objects and their elements remain available.
     *
     * @see IDraw2D
     * @since 1.0.5
     */
    public void clearDraw2Ds() {
        overlays.clear();
    }

    /**
     * Creates an unregistered 3D overlay. Its elements use world-space block coordinates and
     * render relative to the camera only while registered; unlike a 2D overlay, it has no init
     * function.
     * <pre>
     * // a wireframe box around a single block
     * const draw = Hud.createDraw3D();
     * draw.addBox(0, 64, 0, 1, 65, 1, 0xFFFF0000, 0x00000000, false);
     * draw.register();
     * </pre>
     *
     * @return a new unregistered {@link Draw3D}
     * @since 1.0.6
     */
    public Draw3D createDraw3D() {
        return new Draw3D();
    }

    /**
     * Registers an overlay for rendering. Prefer {@link Draw3D#register()}.
     *
     * @param draw the overlay to start rendering
     * @see Draw3D
     * @since 1.0.6
     * @deprecated since 1.6.5 use {@link Draw3D#register()} instead.
     */
    @Deprecated
    @DocletIgnore
    public void registerDraw3D(Draw3D draw) {
        renders.add(draw);
    }

    /**
     * Stops rendering an overlay without removing its elements. Prefer {@link Draw3D#unregister()}.
     *
     * @param draw the overlay to stop rendering
     * @see Draw3D
     * @since 1.0.6
     * @deprecated since 1.6.5 use {@link Draw3D#unregister()} instead.
     */
    @Deprecated
    @DocletIgnore
    public void unregisterDraw3D(Draw3D draw) {
        renders.remove(draw);
    }

    /**
     * Returns a snapshot of the 3D overlays currently registered for rendering, including those
     * registered by other scripts.
     *
     * @return the currently registered {@link Draw3D} overlays
     * @since 1.0.6
     */
    public List<Draw3D> listDraw3Ds() {
        return ImmutableList.copyOf(renders);
    }

    /**
     * Unregisters all 3D overlays, including those registered by other scripts. This also removes
     * prediction markers; {@link FPlayer#setDrawPredictions(boolean)} can register them again.
     *
     * @since 1.0.6
     */
    public void clearDraw3Ds() {
        renders.clear();
    }

    /**
     * Returns the mouse's horizontal position in GUI-scaled pixels, matching 2D overlay coordinates.
     * The scaled value is not necessarily an integer.
     *
     * @return the current X coordinate of the mouse
     * @since 1.1.3
     */
    public double getMouseX() {
        return mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
    }

    /**
     * Returns the mouse's vertical position in GUI-scaled pixels, matching 2D overlay coordinates.
     * The scaled value is not necessarily an integer.
     *
     * @return the current Y coordinate of the mouse
     * @since 1.1.3
     */
    public double getMouseY() {
        return mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();
    }

    /**
     * Returns the game window width in raw (unscaled) pixels.
     *
     * @return the current window width.
     * @since 1.8.4
     */
    public int getWindowWidth() {
        return mc.getWindow().getScreenWidth();
    }

    /**
     * Returns the game window height in raw (unscaled) pixels.
     *
     * @return the current window height.
     * @since 1.8.4
     */
    public int getWindowHeight() {
        return mc.getWindow().getScreenHeight();
    }

}
