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
 *
 * @author Wagyourtail
 * @since 1.0.5
 */
@Library("Hud")
@SuppressWarnings("unused")
public class FHud extends BaseLibrary {

    private static final Minecraft mc = Minecraft.getInstance();
    /**
     * Don't touch this here
     */
    public static final Set<IDraw2D<Draw2D>> overlays = ConcurrentHashMap.newKeySet();
    /**
     * Don't touch this here
     */
    public static final Set<Draw3D> renders = ConcurrentHashMap.newKeySet();

    public FHud(Core<?, ?> runner) {
        super(runner);
    }

    /**
     * @param title
     * @param dirtBG boolean of whether to use a dirt background or not.
     * @return a new {@link IScreen IScreen} Object.
     * @see IScreen
     * @since 1.0.5
     */
    public ScriptScreen createScreen(String title, boolean dirtBG) {
        return new ScriptScreen(title, dirtBG);
    }

    /**
     * Opens a {@link IScreen IScreen} Object.
     *
     * @param s
     * @see IScreen
     * @since 1.0.5
     */
    public void openScreen(@Nullable IScreen s) {
        net.minecraft.client.gui.screens.Screen screen = (net.minecraft.client.gui.screens.Screen) s;
        mc.execute(() -> mc.setScreen(screen));
    }

    /**
     * @return the currently open Screen as an {@link IScreen IScreen}
     * @see IScreen
     * @since 1.2.7
     */
    @Nullable
    public IScreen getOpenScreen() {
        return (IScreen) mc.screen;
    }

    /**
     * @param width  the width of the canvas
     * @param height the height of the canvas
     * @return a {@link CustomImage} that can be used as a texture for screen backgrounds, rendering
     * images, etc.
     * @since 1.8.4
     */
    public CustomImage createTexture(int width, int height, @Nullable String name) {
        return CustomImage.createWidget(width, height, name);
    }

    /**
     * @param path absolute path to an image file
     * @return a {@link CustomImage} that can be used as a texture for screen backgrounds, rendering
     * images, etc.
     * @since 1.8.4
     */
    public CustomImage createTexture(String path, @Nullable String name) {
        return CustomImage.createWidget(path, name);
    }

    /**
     * @return an immutable Map of all registered custom textures.
     * @since 1.8.4
     */
    public Map<String, CustomImage> getRegisteredTextures() {
        return ImmutableMap.copyOf(CustomImage.IMAGES);
    }

    /**
     * @return the current gui scale factor of minecraft.
     * @since 1.8.4
     */
    public int getScaleFactor() {
        return mc.options.guiScale().get();
    }

    /**
     * @return The name of the currently open screen.
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
     * @return a {@link Boolean boolean} denoting if the currently open screen is a container.
     * @since 1.1.2
     */
    public boolean isContainer() {
        return mc.screen instanceof AbstractContainerScreen;
    }

    /**
     * @return
     * @see IDraw2D
     * @since 1.0.5
     */
    public Draw2D createDraw2D() {
        return new Draw2D();
    }

    /**
     * @param overlay
     * @see IDraw2D
     * @since 1.0.5
     * <p>
     * Registers an {@link IDraw2D IDraw2D} to be rendered.
     * @deprecated since 1.6.5, use {@link Draw2D#register()} instead.
     */
    @Deprecated
    public void registerDraw2D(IDraw2D<Draw2D> overlay) {
        ((Draw2D) overlay).init();
        overlays.add(overlay);
    }

    /**
     * @param overlay
     * @see IDraw2D
     * @since 1.0.5
     * <p>
     * Unregisters an {@link IDraw2D IDraw2D} to stop it being rendered.
     * @deprecated since 1.6.5, use {@link Draw2D#unregister()} instead.
     */
    @Deprecated
    public void unregisterDraw2D(IDraw2D<Draw2D> overlay) {
        overlays.remove(overlay);
    }

    /**
     * @return A list of current {@link IDraw2D IDraw2Ds}.
     * @see IDraw2D
     * @since 1.0.5
     */
    public List<IDraw2D<Draw2D>> listDraw2Ds() {
        return ImmutableList.copyOf(overlays);
    }

    /**
     * @see IDraw2D
     * @since 1.0.5
     * <p>
     * clears the Draw2D render list.
     */
    public void clearDraw2Ds() {
        overlays.clear();
    }

    /**
     * @return a new {@link Draw3D Draw3D}.
     * @see Draw3D
     * @since 1.0.6
     */
    public Draw3D createDraw3D() {
        return new Draw3D();
    }

    /**
     * @param draw
     * @see Draw3D
     * @since 1.0.6
     * <p>
     * Registers an {@link Draw3D Draw3D} to be rendered.
     * @deprecated since 1.6.5 use {@link Draw3D#register()} instead.
     */
    @Deprecated
    public void registerDraw3D(Draw3D draw) {
        renders.add(draw);
    }

    /**
     * @param draw
     * @see Draw3D
     * @since 1.0.6
     * <p>
     * Unregisters an {@link Draw3D Draw3D} to stop it being rendered.
     * @since 1.6.5 use {@link Draw3D#unregister()} instead.
     */
    @Deprecated
    public void unregisterDraw3D(Draw3D draw) {
        renders.remove(draw);
    }

    /**
     * @return A list of current {@link Draw3D Draw3D}.
     * @see Draw3D
     * @since 1.0.6
     */
    public List<Draw3D> listDraw3Ds() {
        return ImmutableList.copyOf(renders);
    }

    /**
     * @see Draw3D
     * @since 1.0.6
     * <p>
     * clears the Draw3D render list.
     */
    public void clearDraw3Ds() {
        renders.clear();
    }

    /**
     * @return the current X coordinate of the mouse
     * @since 1.1.3
     */
    public double getMouseX() {
        return mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
    }

    /**
     * @return the current Y coordinate of the mouse
     * @since 1.1.3
     */
    public double getMouseY() {
        return mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();
    }

    /**
     * @return the current window width.
     * @since 1.8.4
     */
    public int getWindowWidth() {
        return mc.getWindow().getScreenWidth();
    }

    /**
     * @return the current window height.
     * @since 1.8.4
     */
    public int getWindowHeight() {
        return mc.getWindow().getScreenHeight();
    }

}
