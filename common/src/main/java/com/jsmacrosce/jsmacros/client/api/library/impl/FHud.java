package com.jsmacrosce.jsmacros.client.api.library.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import org.jetbrains.annotations.Nullable;

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
 * Functions for displaying stuff in 2 to 3 dimensions
 * <p>
 * An instance of this class is passed to scripts as the {@code Hud} variable.
 * <br>
 * There are three fairly separate things in here, and it helps to know which one a call belongs
 * to. {@link #createScreen(String, boolean)} and {@link #openScreen(IScreen)} are about whole
 * screens, the game's own GUI class, which is what stops the player moving until it is closed.
 * {@link #createDraw2D()} and {@link #createDraw3D()} are overlays: a 2D one is a list of shapes
 * and text in screen space, a 3D one is a list of boxes, lines and surfaces in the world, and
 * both keep rendering every frame until they are unregistered. {@link #createTexture(int, int,
 * String)} and its sibling make the images a 2D overlay can draw. And a handful of readers,
 * {@link #getOpenScreen()}, {@link #getOpenScreenName()}, {@link #isContainer()},
 * {@link #getMouseX()}, {@link #getMouseY()}, {@link #getWindowWidth()} and
 * {@link #getWindowHeight()}, report where the player currently is in all of that.
 * <br>
 * A {@code Draw2D} and a {@code Draw3D} only render once they are registered, and building one
 * does not register it, so a script that creates elements and returns draws nothing. Register it
 * and take it off again with {@code register()} and {@code unregister()} on the object itself;
 * the older {@code Hud.registerDraw2D} and friends do the same thing and are deprecated.
 * {@link #clearDraw2Ds()} and {@link #clearDraw3Ds()} empty the whole list, which takes
 * <em>every</em> overlay down including ones another script put there.
 * <br>
 * The coordinates here are screen coordinates, not world ones. A 2D overlay measures from the
 * top left of the window in pixels after the GUI scale has been applied, which is the same space
 * the mouse readings are in and the same space {@link #getWindowWidth()} and
 * {@link #getWindowHeight()} report, so a mouse position can be used as a drawing position
 * directly. A 3D overlay is in world space and is anchored to the camera, so its positions are
 * block coordinates in the dimension the player is in.
 * example:
 * <pre>
 * // a 2D overlay that says something, registered so it actually renders
 * const draw = Hud.createDraw2D();
 * draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
 * draw.register();
 *
 * // and taken off again when it is not wanted, which is per object
 * draw.unregister();
 *
 * // a 3D overlay marking a block out in the world
 * const draw3d = Hud.createDraw3D();
 * draw3d.addBox(0, 64, 0, 1, 65, 1, 0xFFFF0000, 0x88000000, true);
 * draw3d.register();
 *
 * // where is the player looking from, in the same space a 2D overlay draws in
 * Chat.log(`mouse at ${Hud.getMouseX()}, ${Hud.getMouseY()} in ${Hud.getWindowWidth()}x${Hud.getWindowHeight()}`);
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
     * the set of 2D overlays that are currently being rendered.
     * <p>
     * This is the list the renderer walks, and a {@code Draw2D} adds itself to it when it is
     * registered. An entry here is what makes an overlay visible, so a {@code Draw2D} that was
     * created but never registered is not in it and does not render.
     * <p>
     * Don't touch this here
     * @since 1.0.5
     */
    public static final Set<IDraw2D<Draw2D>> overlays = ConcurrentHashMap.newKeySet();
    /**
     * the set of 3D overlays that are currently being rendered.
     * <p>
     * This is the list the world renderer walks, and a {@code Draw3D} adds itself to it when it
     * is registered. As with the 2D list, an entry here is what makes an overlay visible.
     * <p>
     * Don't touch this here
     * @since 1.0.6
     */
    public static final Set<Draw3D> renders = ConcurrentHashMap.newKeySet();

    public FHud(Core<?, ?> runner) {
        super(runner);
    }

    // Before 1.20.5 vanilla used a dirt texture for this menu background; modern
    // versions blur the world behind the screen instead.
    /**
     * a blank screen with a title, ready to be opened.
     * <p>
     * This is the game's own screen class rather than an overlay: opening one pauses the game in
     * singleplayer and takes the mouse, and nothing on it draws until it is opened. The
     * {@code showBackground} argument decides what is drawn behind it. Before 1.20.5 vanilla used
     * a dirt texture for that; on modern versions it is the blurred world behind the screen, so
     * {@code true} asks for the game's own background and {@code false} leaves it transparent
     * and shows the world straight through.
     * <p>
     * The title is drawn centred near the top by default, and turning that off is a field on the
     * screen rather than an argument here.
     * example:
     * <pre>
     * // a screen with a title and the usual background
     * const screen = Hud.createScreen("my screen", true);
     * screen.addText("something to read", 10, 40, 0xFFFFFFFF, true);
     * Hud.openScreen(screen);
     *
     * // take it back down again, which is the same as opening nothing
     * Hud.openScreen(null);
     * </pre>
     *
     * @param title the title shown at the top of the screen
     * @param showBackground boolean of whether to use the blurred menu background or not.
     * @return a new {@link IScreen IScreen} Object.
     * @see IScreen
     * @since 1.0.5
     */
    public ScriptScreen createScreen(String title, boolean showBackground) {
        return new ScriptScreen(title, showBackground);
    }

    /**
     * Opens a {@link IScreen IScreen} Object.
     * <p>
     * The swap is done on the main thread, so this returns before the screen is actually up and a
     * script that opens one and then measures it has to wait a tick. Passing {@code null} closes
     * whatever is open and hands control back to the game.
     * <p>
     * The screen the game returns to when this one closes is whatever was open before it, unless
     * the screen itself names a different parent.
     * example:
     * <pre>
     * // open a screen, then take it down again on a click
     * Hud.openScreen(Hud.createScreen("hello", true));
     * Client.waitTick(1);
     * Hud.openScreen(null);
     * </pre>
     *
     * @param s the screen to open, or {@code null} to close the current one
     * @see IScreen
     * @since 1.0.5
     */
    public void openScreen(@Nullable IScreen s) {
        net.minecraft.client.gui.screens.Screen screen = (net.minecraft.client.gui.screens.Screen) s;
        mc.execute(() -> mc.setScreen(screen));
    }

    /**
     * what is open right now, if anything.
     * <p>
     * A {@code null} here means the player is in the world with no screen up, which is the
     * normal case. A non-null one can be a script screen, one of the game's own menus, or a
     * container, and {@link #getOpenScreenName()} is usually the easier thing to print.
     * example:
     * <pre>
     * const screen = Hud.getOpenScreen();
     * if (screen === null) {
     *   Chat.log("no screen, in the world");
     * } else {
     *   Chat.log(`open: ${Hud.getOpenScreenName()}`);
     * }
     * </pre>
     *
     * @return the currently open Screen as an {@link IScreen IScreen}, or {@code null} if none is
     * @see IScreen
     * @since 1.2.7
     */
    @Nullable
    public IScreen getOpenScreen() {
        return (IScreen) mc.screen;
    }

    /**
     * a blank texture, ready to be drawn on.
     * <p>
     * This is a mutable image rather than a file, so the point of it is to make a canvas: draw
     * into it with the {@code CustomImage} methods, and then hand the result to a 2D overlay as
     * a texture. The name is what identifies it later, and a {@code null} name is allowed, in
     * which case it is still usable but is harder to find again.
     * <p>
     * A texture made this way is registered the moment it exists, so it turns up in
     * {@link #getRegisteredTextures()} whether or not anything has drawn it.
     * example:
     * <pre>
     * // a 16 by 16 canvas, painted red, then drawn onto an overlay
     * const image = Hud.createTexture(16, 16, "my icon");
     * image.setGraphicsColor(0xFFFF0000);
     * image.fillRect(0, 0, 16, 16);
     *
     * const draw = Hud.createDraw2D();
     * draw.addImage(10, 10, 16, 16, "my icon", 0, 0, 16, 16, 16, 16);
     * draw.register();
     * </pre>
     *
     * @param width  the width of the canvas
     * @param height the height of the canvas
     * @param name   the name to register the texture under, or {@code null} for none
     * @return a {@link CustomImage} that can be used as a texture for screen backgrounds, rendering
     * images, etc.
     * @since 1.8.4
     */
    public CustomImage createTexture(int width, int height, @Nullable String name) {
        return CustomImage.createWidget(width, height, name);
    }

    /**
     * a texture loaded from an image file.
     * <p>
     * The path is resolved against JsMacros' own config folder, not against the game's working
     * directory and not against the folder of the script that is running, so a texture shipped
     * next to a macro is not found this way.<br>
     * The two ways this can go wrong are not the same, and only one of them is the {@code null}
     * the check below is for. A file that is not there, or that cannot be read, gives
     * {@code null} here and an error in the log. A file that is there and readable but is not an
     * image the Java imaging library understands is worse: the reader gives up on it silently,
     * that {@code null} goes straight into the texture's constructor, and the call throws a
     * {@link NullPointerException} rather than returning anything.
     * example:
     * <pre>
     * const image = Hud.createTexture("textures/logo.png", "logo");
     * if (image === null) {
     *   // only a missing or unreadable file gets here; a file that is not an image throws instead
     *   Chat.log("no readable image at textures/logo.png in the config folder");
     * } else {
     *   Chat.log(`loaded ${image.getWidth()}x${image.getHeight()}`);
     * }
     * </pre>
     *
     * @param path path to an image file, relative to the JsMacros config folder
     * @param name the name to register the texture under, or {@code null} for none
     * @return a {@link CustomImage} that can be used as a texture for screen backgrounds, rendering
     * images, etc, or {@code null} if the file could not be read
     * @since 1.8.4
     */
    public CustomImage createTexture(String path, @Nullable String name) {
        return CustomImage.createWidget(path, name);
    }

    /**
     * every texture JsMacros has registered, by the name it was created under.
     * <p>
     * A {@code null} name never reaches this map, and re-creating a texture under a name that is
     * already taken replaces what is there. The map is a snapshot, so it is safe to walk while
     * creating more.
     * example:
     * <pre>
     * for (const name of Object.keys(Hud.getRegisteredTextures())) {
     *   Chat.log(`texture: ${name}`);
     * }
     * </pre>
     *
     * @return an immutable Map of all registered custom textures.
     * @since 1.8.4
     */
    public Map<String, CustomImage> getRegisteredTextures() {
        return ImmutableMap.copyOf(CustomImage.IMAGES);
    }

    /**
     * the GUI scale the game is rendering at, as a plain number.
     * <p>
     * This is the {@code guiScale} option, so a player who has set it to auto reports
     * {@code 0} here rather than whatever the game worked out. It is worth knowing because every
     * coordinate a 2D overlay draws in, and every reading {@link #getMouseX()} and
     * {@link #getMouseY()} give, is in the scaled space this produces.
     * example:
     * <pre>
     * Chat.log(`gui scale ${Hud.getScaleFactor()}, window ${Hud.getWindowWidth()}x${Hud.getWindowHeight()}`);
     * </pre>
     *
     * @return the current gui scale factor of minecraft.
     * @since 1.8.4
     */
    public int getScaleFactor() {
        return mc.options.guiScale().get();
    }

    /**
     * the name of whatever screen is open, as a short readable string.
     * <p>
     * This is a friendlier answer than {@link #getOpenScreen()} for a log line or a branch. A
     * container with rows reports how many it has, as in {@code 3 Row Chest}, the well known
     * screens have their own names such as {@code Crafting Table} or {@code Survival Inventory},
     * and anything else falls back to its title, or to {@code unknown} if it has no title at
     * all. It is {@code null} when nothing is open.
     * example:
     * <pre>
     * const name = Hud.getOpenScreenName();
     * Chat.log(name === null ? "in the world" : `screen is ${name}`);
     * </pre>
     *
     * @return The name of the currently open screen, or {@code null} if none is.
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
        return JsMacrosClient.getScreenName(mc.screen);
    }

    /**
     * whether the open screen is a container, meaning something with slots in it.
     * <p>
     * This covers the chest and shulker style containers as well as the player's own inventory.
     * It is the cheap way to ask whether there is anything for
     * {@link FPlayer#openInventory()} to work with.
     * example:
     * <pre>
     * if (Hud.isContainer()) {
     *   Chat.log("a container is open");
     * }
     * </pre>
     *
     * @return a {@link Boolean boolean} denoting if the currently open screen is a container.
     * @since 1.1.2
     */
    public boolean isContainer() {
        return mc.screen instanceof AbstractContainerScreen;
    }

    /**
     * a new 2D overlay, in screen space.
     * <p>
     * Elements are added to it and it renders every frame, but only once it has been registered
     * with {@code register()} on the object itself. Registering is also what calls its init
     * function, so an overlay that builds its elements in {@code setOnInit} should be registered
     * rather than filled in directly. Coordinates are pixels from the top left of the window
     * after the GUI scale has been applied, which is the same space
     * {@link #getMouseX()} and {@link #getMouseY()} report.
     * example:
     * <pre>
     * // an overlay that labels itself, and takes itself back down after five seconds
     * const draw = Hud.createDraw2D();
     * draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
     * draw.register();
     * let ticks = 0;
     * const listener = JsMacros.on("Tick", JavaWrapper.methodToJava(function (event) {
     *   if (++ticks > 100) {
     *     draw.unregister();
     *     JsMacros.off(listener);
     *   }
     * }));
     * </pre>
     *
     * @return a new unregistered {@link Draw2D}, nothing on it yet
     * @see IDraw2D
     * @since 1.0.5
     */
    public Draw2D createDraw2D() {
        return new Draw2D();
    }

    /**
     * Registers an {@link IDraw2D IDraw2D} to be rendered.
     * <p>
     * This initialises the overlay and then registers it, so the init function runs before the
     * first frame. The same thing is available as {@code register()} on the overlay itself,
     * which is what should be preferred.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
     * Hud.registerDraw2D(draw);
     * </pre>
     *
     * @param overlay the overlay to start rendering
     * @see IDraw2D
     * @since 1.0.5
     * @deprecated since 1.6.5, use {@link Draw2D#register()} instead.
     */
    @Deprecated
    public void registerDraw2D(IDraw2D<Draw2D> overlay) {
        ((Draw2D) overlay).init();
        overlays.add(overlay);
    }

    /**
     * Unregisters an {@link IDraw2D IDraw2D} to stop it being rendered.
     * <p>
     * The elements are left on the overlay, so the same object can be registered again later and
     * comes back as it was. The same thing is available as {@code unregister()} on the overlay
     * itself, which is what should be preferred.
     *
     * @param overlay the overlay to stop rendering
     * @see IDraw2D
     * @since 1.0.5
     * @deprecated since 1.6.5, use {@link Draw2D#unregister()} instead.
     */
    @Deprecated
    public void unregisterDraw2D(IDraw2D<Draw2D> overlay) {
        overlays.remove(overlay);
    }

    /**
     * the 2D overlays that are currently rendering.
     * <p>
     * This is a snapshot of the list, taken when it is called. It includes overlays other
     * scripts registered, so it shows the whole current state rather than only what this script
     * owns.
     * example:
     * <pre>
     * Chat.log(`${Hud.listDraw2Ds().size()} 2D overlays up`);
     * </pre>
     *
     * @return A list of current {@link IDraw2D IDraw2Ds}.
     * @see IDraw2D
     * @since 1.0.5
     */
    public List<IDraw2D<Draw2D>> listDraw2Ds() {
        return ImmutableList.copyOf(overlays);
    }

    /**
     * clears the Draw2D render list.
     * <p>
     * This takes down every 2D overlay at once, including ones registered by other scripts and
     * ones this script is still holding a reference to. An overlay cleared this way keeps its
     * elements and can be registered again; it is the <em>registration</em> that is gone, not the
     * overlay.
     * example:
     * <pre>
     * // take down every 2D overlay, this script's and anyone else's
     * Hud.clearDraw2Ds();
     * </pre>
     *
     * @see IDraw2D
     * @since 1.0.5
     */
    public void clearDraw2Ds() {
        overlays.clear();
    }

    /**
     * a new 3D overlay, in world space.
     * <p>
     * Elements are added to it and it renders every frame, but only once it has been registered
     * with {@code register()} on the object itself. Unlike a 2D overlay it has no init function
     * to wait for, so building the elements before registering is fine. Coordinates are block
     * positions in the dimension the player is currently in, and the overlay is drawn relative
     * to the camera, so it moves with the player.
     * example:
     * <pre>
     * // a wireframe box around a single block
     * const draw = Hud.createDraw3D();
     * draw.addBox(0, 64, 0, 1, 65, 1, 0xFFFF0000, 0x00000000, false);
     * draw.register();
     * </pre>
     *
     * @return a new unregistered {@link Draw3D Draw3D}.
     * @see Draw3D
     * @since 1.0.6
     */
    public Draw3D createDraw3D() {
        return new Draw3D();
    }

    /**
     * Registers an {@link Draw3D Draw3D} to be rendered.
     * <p>
     * The same thing is available as {@code register()} on the overlay itself, which is what
     * should be preferred.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * draw.addBox(0, 64, 0, 1, 65, 1, 0xFFFF0000, 0x00000000, false);
     * Hud.registerDraw3D(draw);
     * </pre>
     *
     * @param draw the overlay to start rendering
     * @see Draw3D
     * @since 1.0.6
     * @deprecated since 1.6.5 use {@link Draw3D#register()} instead.
     */
    @Deprecated
    public void registerDraw3D(Draw3D draw) {
        renders.add(draw);
    }

    /**
     * Unregisters an {@link Draw3D Draw3D} to stop it being rendered.
     * <p>
     * The elements are left on the overlay, so the same object can be registered again later and
     * comes back as it was. The same thing is available as {@code unregister()} on the overlay
     * itself, which is what should be preferred.
     *
     * @param draw the overlay to stop rendering
     * @see Draw3D
     * @since 1.0.6
     * @deprecated since 1.6.5 use {@link Draw3D#unregister()} instead.
     */
    @Deprecated
    public void unregisterDraw3D(Draw3D draw) {
        renders.remove(draw);
    }

    /**
     * the 3D overlays that are currently rendering.
     * <p>
     * This is a snapshot of the list, taken when it is called, and it includes overlays other
     * scripts registered.
     * example:
     * <pre>
     * Chat.log(`${Hud.listDraw3Ds().size()} 3D overlays up`);
     * </pre>
     *
     * @return A list of current {@link Draw3D Draw3D}.
     * @see Draw3D
     * @since 1.0.6
     */
    public List<Draw3D> listDraw3Ds() {
        return ImmutableList.copyOf(renders);
    }

    /**
     * clears the Draw3D render list.
     * <p>
     * This takes down every 3D overlay at once, including ones registered by other scripts. The
     * prediction markers that {@link FPlayer#setDrawPredictions(boolean)} puts up live on this
     * same list, so clearing it takes those down too, and calling
     * {@link FPlayer#setDrawPredictions(boolean)} with {@code true} again puts them back.
     * example:
     * <pre>
     * Hud.clearDraw3Ds();
     * </pre>
     *
     * @see Draw3D
     * @since 1.0.6
     */
    public void clearDraw3Ds() {
        renders.clear();
    }

    /**
     * where the mouse is, horizontally.
     * <p>
     * This is in the scaled screen space a 2D overlay draws in, not raw window pixels, so it can
     * be used as a drawing position directly. It is a {@code double} because the reading is
     * scaled rather than rounded, so it is not necessarily a whole pixel.
     * example:
     * <pre>
     * // a label pinned to the mouse
     * const draw = Hud.createDraw2D();
     * draw.addText("here", Hud.getMouseX(), Hud.getMouseY(), 0xFFFFFFFF, true);
     * draw.register();
     * </pre>
     *
     * @return the current X coordinate of the mouse
     * @since 1.1.3
     */
    public double getMouseX() {
        return mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
    }

    /**
     * where the mouse is, vertically.
     * <p>
     * This is in the scaled screen space a 2D overlay draws in, not raw window pixels, so it can
     * be used as a drawing position directly. It is a {@code double} because the reading is
     * scaled rather than rounded.
     * example:
     * <pre>
     * // a crosshair at the mouse
     * const draw = Hud.createDraw2D();
     * draw.addLine(Hud.getMouseX() - 4, Hud.getMouseY(), Hud.getMouseX() + 4, Hud.getMouseY(), 0xFFFFFFFF);
     * draw.addLine(Hud.getMouseX(), Hud.getMouseY() - 4, Hud.getMouseX(), Hud.getMouseY() + 4, 0xFFFFFFFF);
     * draw.register();
     * </pre>
     *
     * @return the current Y coordinate of the mouse
     * @since 1.1.3
     */
    public double getMouseY() {
        return mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();
    }

    /**
     * the width of the game window, in raw window pixels.
     * <p>
     * This is the unscaled width, so it is larger than the space a 2D overlay draws in whenever
     * the GUI scale is above one. The scaled space is what the mouse readings are in.
     * example:
     * <pre>
     * Chat.log(`window ${Hud.getWindowWidth()}x${Hud.getWindowHeight()}`);
     * </pre>
     *
     * @return the current window width.
     * @since 1.8.4
     */
    public int getWindowWidth() {
        return mc.getWindow().getScreenWidth();
    }

    /**
     * the height of the game window, in raw window pixels.
     * <p>
     * This is the unscaled height, so it is larger than the space a 2D overlay draws in whenever
     * the GUI scale is above one. The scaled space is what the mouse readings are in.
     *
     * @return the current window height.
     * @since 1.8.4
     */
    public int getWindowHeight() {
        return mc.getWindow().getScreenHeight();
    }

}
