package com.jsmacrosce.jsmacros.client.api.helper;

import com.google.common.collect.ImmutableList;

import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.*;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.PlayerModelPart;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import org.lwjgl.glfw.GLFW;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletDeclareType;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.access.IResourcePackManager;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinSimpleOption;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//? if >1.21.8 {
/*import net.minecraft.resources.ResourceLocation;
*///?}

/**
 * Provides access to the client's game settings.
 * <p>
 * Wraps the supplied options object; changes made through this helper affect that object.
 * <p>
 * Settings are grouped in {@link #skin}, {@link #video}, {@link #music}, {@link #control},
 * {@link #chat}, and {@link #accessibility}; each field and its corresponding getter return
 * the same group object.
 * <p>
 * Setting changes are not necessarily saved to {@code options.txt} until {@link #saveOptions()}
 * is called; some methods save as part of their own behavior.
 * <pre>
 * const options = Client.getGameOptions();
 * options.video.setRenderDistance(12);
 * options.saveOptions();
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Configuration/Profiles")
@SuppressWarnings("unused")
public class OptionsHelper extends BaseHelper<Options> {

    private static final Map<String, SoundSource> SOUND_CATEGORY_MAP = Arrays.stream(SoundSource.values()).collect(Collectors.toMap(SoundSource::getName, Function.identity()));
    private final Minecraft mc = Minecraft.getInstance();
    private final PackRepository rpm = mc.getResourcePackRepository();

    /**
 * Player skin model-part visibility and main-hand settings.
     * @since 1.8.4
     */
    public final SkinOptionsHelper skin = new SkinOptionsHelper(this);
    /**
 * Graphics and video settings.
     * @since 1.8.4
     */
    public final VideoOptionsHelper video = new VideoOptionsHelper(this);
    /**
 * Sound volumes, audio device selection, and subtitles. Per-category volumes are available by
 * category name or through individual methods.
     * @since 1.8.4
     */
    public final MusicOptionsHelper music = new MusicOptionsHelper(this);
    /**
 * Input settings and key bindings.
     * @since 1.8.4
     */
    public final ControlOptionsHelper control = new ControlOptionsHelper(this);
    /**
 * Chat display and behavior settings.
     * @since 1.8.4
     */
    public final ChatOptionsHelper chat = new ChatOptionsHelper(this);
    /**
 * Accessibility settings. Some settings are also exposed by other groups and operate on the same
 * underlying option.
     * @since 1.8.4
     */
    public final AccessibilityOptionsHelper accessibility = new AccessibilityOptionsHelper(this);

    /** Wraps the supplied game options object, retaining its reference.
     * @param options game options to wrap
     */
    public OptionsHelper(Options options) {
        super(options);
    }

    private float getSoundSourceVolume(SoundSource source) {
        //? if >1.21.5 {
        return base.getFinalSoundSourceVolume(source);
         //?} else {
        /*return base.getSoundSourceVolume(source);
        *///?}
    }

    /**
     * @return the skin options group.
     * @since 1.8.4
     */
    public SkinOptionsHelper getSkinOptions() {
        return skin;
    }

    /**
     * @return the video options group.
     * @since 1.8.4
     */
    public VideoOptionsHelper getVideoOptions() {
        return video;
    }

    /**
     * @return the music options group.
     * @since 1.8.4
     */
    public MusicOptionsHelper getMusicOptions() {
        return music;
    }

    /**
     * @return the control options group.
     * @since 1.8.4
     */
    public ControlOptionsHelper getControlOptions() {
        return control;
    }

    /**
     * @return the chat options group.
     * @since 1.8.4
     */
    public ChatOptionsHelper getChatOptions() {
        return chat;
    }

    /**
     * @return the accessibility options group.
     * @since 1.8.4
     */
    public AccessibilityOptionsHelper getAccessibilityOptions() {
        return accessibility;
    }

    /**
     * @return this helper for chaining.
     * @since 1.8.4
     */
    public OptionsHelper saveOptions() {
        base.save();
        return this;
    }

    /**
     * @return the available resource-pack IDs.
     * @since 1.1.7
     */
    public List<String> getResourcePacks() {
        return new ArrayList<>(rpm.getAvailableIds());
    }

    /**
     * @return the selected resource-pack IDs.
     * @since 1.2.0
     */
    public List<String> getEnabledResourcePacks() {
        return new ArrayList<>(rpm.getSelectedIds());
    }

    /**
     * Selects resource packs, saves the options, and reloads packs if the saved selection changes.
     *
     * @param enabled resource-pack IDs to select; duplicate IDs are ignored
     * @return this helper for chaining
     * @since 1.2.0
     */
    public OptionsHelper setEnabledResourcePacks(String[] enabled) {
        Collection<String> en = Arrays.stream(enabled).distinct().collect(Collectors.toList());
        List<String> currentRP = ImmutableList.copyOf(base.resourcePacks);
        rpm.setSelected(en);
        base.resourcePacks.clear();
        base.incompatibleResourcePacks.clear();
        for (Pack p : rpm.getSelectedPacks()) {
            if (!p.isFixedPosition()) {
                base.resourcePacks.add(p.getId());
                if (!p.getCompatibility().isCompatible()) {
                    base.incompatibleResourcePacks.add(p.getId());
                }
            }
        }
        base.save();
        List<String> newRP = ImmutableList.copyOf(base.resourcePacks);
        if (!currentRP.equals(newRP)) {
            mc.reloadResourcePacks();
        }
        return this;
    }

    /**
     * Disables or allows server-provided resource packs. Reloads packs only when the disabled state changes.
     * @param state {@code true} to disable server-provided resource packs; {@code false} to allow them
     * @return self for chaining
     * @since 1.8.3
     */
    public OptionsHelper removeServerResourcePack(boolean state) {
        if (state != ((IResourcePackManager) rpm).jsmacros_isServerPacksDisabled()) {
            ((IResourcePackManager) rpm).jsmacros_disableServerPacks(state);
            mc.reloadResourcePacks();
        }
        return this;
    }

    /**
     * @return the configured language code.
     * @since 1.8.4
     */
    @DocletReplaceReturn("Locale")
    public String getLanguage() {
        return base.languageCode;
    }

    /**
     * Selects the language if its code is recognized, then always reloads resource packs and saves options.
     *
     * @param languageCode language code to select
     * @return this helper for chaining
     * @since 1.8.4
     */
    @DocletReplaceParams("languageCode: Locale")
    public OptionsHelper setLanguage(String languageCode) {
        LanguageManager manager = Minecraft.getInstance().getLanguageManager();
        LanguageInfo language = manager.getLanguage(languageCode);
        if (language != null) {
            manager.setSelected(languageCode);
            base.languageCode = languageCode;
            base.save();
            mc.reloadResourcePacks();
        }
        Minecraft.getInstance().reloadResourcePacks();
        base.save();
        return this;
    }

    /**
     * @return the active difficulty's name: {@code peaceful}, {@code easy}, {@code normal}, or {@code hard}.
     * @since 1.8.4
     */
    @DocletReplaceReturn("Difficulty")
    public String getDifficulty() {
        //? if >=26.1 {
        /*return mc.level.getDifficulty().getSerializedName();
        *///?} else {
        return mc.level.getDifficulty().getKey();
        //?}
    }

    /**
     * Sets the singleplayer world's difficulty; multiplayer clients do not apply this change.
     *
     * @param name {@code peaceful}, {@code easy}, {@code normal}, or {@code hard}
     * @return self for chaining.
     * @since 1.8.4
     */
    @DocletReplaceParams("name: Difficulty")
    @DocletDeclareType(name = "Difficulty", type = "'peaceful' | 'easy' | 'normal' | 'hard'")
    public OptionsHelper setDifficulty(String name) {
        if (mc.hasSingleplayerServer()) {
            mc.getSingleplayerServer().setDifficulty(Difficulty.byName(name), true);
        }
        return this;
    }

    /**
     * @return {@code true} if the current world's difficulty is locked
     * @since 1.8.4
     */
    public boolean isDifficultyLocked() {
        return Minecraft.getInstance().level.getLevelData().isDifficultyLocked();
    }

    /**
     * Sends a request to lock the world's difficulty.
     *
     * @return this helper for chaining
     * @since 1.8.4
     */
    public OptionsHelper lockDifficulty() {
        Minecraft.getInstance().getConnection().send(new ServerboundLockDifficultyPacket(true));
        return this;
    }

    /**
     * Sends a request to unlock the world's difficulty; the server may reject it if the player lacks permission.
     *
     * @return this helper for chaining
     * @since 1.8.4
     */
    public OptionsHelper unlockDifficulty() {
        Minecraft.getInstance().getConnection().send(new ServerboundLockDifficultyPacket(false));
        return this;
    }

    /**
     * @return the configured field-of-view value
     * @since 1.1.7
     */
    public int getFov() {
        return base.fov().get();
    }

    /**
     * @param fov the new field-of-view value
     * @return this helper for chaining
     * @since 1.1.7
     */
    public OptionsHelper setFov(int fov) {
        getBase(base.fov()).forceSetValue(fov);
        return this;
    }

    /**
     * @return {@code 0} for first person, {@code 1} for third person from behind, or {@code 2} for third person from the front
     * @since 1.5.0
     */
    @DocletReplaceReturn("Trit")
    public int getCameraMode() {
        return base.getCameraType().ordinal();
    }

    /**
     * @param mode {@code 0} for first person, {@code 1} for third person from behind, or {@code 2} for third person from the front
     * @return self for chaining
     * @since 1.5.0
     */
    @DocletReplaceParams("mode: Trit")
    public OptionsHelper setCameraMode(int mode) {
        base.setCameraType(CameraType.values()[mode]);
        return this;
    }

    /**
     * @return whether smooth camera is enabled
     * @since 1.5.0
     */
    public boolean getSmoothCamera() {
        return base.smoothCamera;
    }

    /**
     * Sets whether smooth camera is enabled.
     * @param val {@code true} to enable smooth camera; {@code false} to disable it
     * @return self for chaining
     * @since 1.5.0
     */
    public OptionsHelper setSmoothCamera(boolean val) {
        base.smoothCamera = val;
        return this;
    }

    /**
     * @return the window's screen width in pixels
     * @since 1.2.6
     */
    public int getWidth() {
        return mc.getWindow().getScreenWidth();
    }

    /**
     * @return the window's screen height in pixels
     * @since 1.2.6
     */
    public int getHeight() {
        return mc.getWindow().getScreenHeight();
    }

    private long getWindowHandle() {
        Window win = mc.getWindow();
        //? if >1.21.8 {
         /*long handle = win.handle(); 
        *///?} else {
        long handle = win.getWindow();
        //?}
        return handle;
    }

    /**
     * Sets the window's screen width, preserving its current screen height.
     * @param w the width in pixels
     * @return self for chaining
     * @since 1.2.6
     */
    public OptionsHelper setWidth(int w) {
        Window win = mc.getWindow();
        GLFW.glfwSetWindowSize(getWindowHandle(), w, win.getScreenHeight());
        return this;
    }

    /**
     * Sets the window's screen height, preserving its current screen width.
     * @param h the height in pixels
     * @return self for chaining
     * @since 1.2.6
     */
    public OptionsHelper setHeight(int h) {
        Window win = mc.getWindow();
        GLFW.glfwSetWindowSize(getWindowHandle(), win.getScreenWidth(), h);
        return this;
    }

    /**
     * Sets the window's screen size.
     * @param w the width in pixels
     * @param h the height in pixels
     * @return self for chaining
     * @since 1.2.6
     */
    public OptionsHelper setSize(int w, int h) {
        Window win = mc.getWindow();
        GLFW.glfwSetWindowSize(getWindowHandle(), w, h);
        return this;
    }


    /**
     * Sends the current synchronized options to the server. Setting changes do not
     * automatically call this method; call it manually when those changes need to be
     * broadcast.
     *
     * @since 2.0.0
     */
    public void sendSyncedOptions() {
        base.broadcastOptions();
    }

    private MixinSimpleOption getBase(OptionInstance<?> option) {
        return (MixinSimpleOption) (Object) option;
    }

    /** Skin model-part visibility and main-hand settings.
     * <pre>
     * const options = Client.getGameOptions();
     * options.skin.isCapeActivated();
     * </pre>
     */
    @DocletCategory("Configuration/Profiles")
    public class SkinOptionsHelper {

        /**
         * The options helper that owns this settings group.
         * @since 1.8.4
         */
        public final OptionsHelper parent;

        /** Stores the supplied parent reference; settings methods use the enclosing {@code OptionsHelper} instance, which may differ.
         * @param OptionsHelper parent helper reference to retain
         */
        public SkinOptionsHelper(OptionsHelper OptionsHelper) {
            parent = OptionsHelper;
        }

        /**
         * @return the parent options helper.
         * @since 1.8.4
         */
        public OptionsHelper getParent() {
            return parent;
        }

        /**
         * @return {@code true} if the player's cape should be shown, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isCapeActivated() {
            return base.isModelPartEnabled(PlayerModelPart.CAPE);
        }

        /**
         * @return {@code true} if the player's jacket should be shown, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isJacketActivated() {
            return base.isModelPartEnabled(PlayerModelPart.JACKET);
        }

        /**
         * @return {@code true} if the player's left sleeve should be shown, {@code false}
         * otherwise.
         * @since 1.8.4
         */
        public boolean isLeftSleeveActivated() {
            return base.isModelPartEnabled(PlayerModelPart.LEFT_SLEEVE);
        }

        /**
         * @return {@code true} if the player's right sleeve should be shown, {@code false}
         * otherwise.
         * @since 1.8.4
         */
        public boolean isRightSleeveActivated() {
            return base.isModelPartEnabled(PlayerModelPart.RIGHT_SLEEVE);
        }

        /**
         * @return {@code true} if the player's left pants should be shown, {@code false}
         * otherwise.
         * @since 1.8.4
         */
        public boolean isLeftPantsActivated() {
            return base.isModelPartEnabled(PlayerModelPart.LEFT_PANTS_LEG);
        }

        /**
         * @return {@code true} if the player's right pants should be shown, {@code false}
         * otherwise.
         * @since 1.8.4
         */
        public boolean isRightPantsActivated() {
            return base.isModelPartEnabled(PlayerModelPart.RIGHT_PANTS_LEG);
        }

        /**
         * @return {@code true} if the player's hat should be shown, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isHatActivated() {
            return base.isModelPartEnabled(PlayerModelPart.HAT);
        }

        /**
         * @return {@code true} if the player's main hand is the right one, {@code false}
         * otherwise.
         * @since 1.8.4
         */
        public boolean isRightHanded() {
            return base.mainHand().get() == HumanoidArm.RIGHT;
        }

        /**
         * @return {@code true} if the player's main hand is the left one, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isLeftHanded() {
            return base.mainHand().get() == HumanoidArm.LEFT;
        }

        /**
         * @param val whether the cape should be shown or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public SkinOptionsHelper toggleCape(boolean val) {
            base.setModelPart(PlayerModelPart.CAPE, val);
            return this;
        }

        /**
         * @param val whether the jacket should be shown or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public SkinOptionsHelper toggleJacket(boolean val) {
            base.setModelPart(PlayerModelPart.JACKET, val);
            return this;
        }

        /**
         * Sets whether the left sleeve is shown.
         * @param val {@code true} to show the sleeve; {@code false} to hide it
         * @return self for chaining.
         * @since 1.8.4
         */
        public SkinOptionsHelper toggleLeftSleeve(boolean val) {
            base.setModelPart(PlayerModelPart.LEFT_SLEEVE, val);
            return this;
        }

        /**
         * Sets whether the right sleeve is shown.
         * @param val {@code true} to show the sleeve; {@code false} to hide it
         * @return self for chaining.
         * @since 1.8.4
         */
        public SkinOptionsHelper toggleRightSleeve(boolean val) {
            base.setModelPart(PlayerModelPart.RIGHT_SLEEVE, val);
            return this;
        }

        /**
         * Sets whether the left pants leg is shown.
         * @param val {@code true} to show the pants leg; {@code false} to hide it
         * @return self for chaining.
         * @since 1.8.4
         */
        public SkinOptionsHelper toggleLeftPants(boolean val) {
            base.setModelPart(PlayerModelPart.LEFT_PANTS_LEG, val);
            return this;
        }

        /**
         * Sets whether the right pants leg is shown.
         * @param val {@code true} to show the pants leg; {@code false} to hide it
         * @return self for chaining.
         * @since 1.8.4
         */
        public SkinOptionsHelper toggleRightPants(boolean val) {
            base.setModelPart(PlayerModelPart.RIGHT_PANTS_LEG, val);
            return this;
        }

        /**
         * Sets whether the hat is shown.
         * @param val {@code true} to show the hat; {@code false} to hide it
         * @return self for chaining.
         * @since 1.8.4
         */
        public SkinOptionsHelper toggleHat(boolean val) {
            base.setModelPart(PlayerModelPart.HAT, val);
            return this;
        }

        /**
         * Sets the main hand. The value is case-insensitive; only {@code "left"} selects the left hand,
         * and any other value selects the right hand.
         *
         * @param hand the requested hand
         * @return self for chaining.
         * @since 1.8.4
         */
        public SkinOptionsHelper toggleMainHand(String hand) {
            base.mainHand().set(hand.toLowerCase(Locale.ROOT).equals("left") ? HumanoidArm.LEFT : HumanoidArm.RIGHT);
            return this;
        }

    }

    /** Graphics and display settings.
     * <pre>
     * const options = Client.getGameOptions();
     * options.video.getGraphicsMode();
     * </pre>
     */
    @DocletCategory("Configuration/Profiles")
    public class VideoOptionsHelper {

        /**
         * The {@link OptionsHelper} that owns this settings group; this is the same object returned by
         * {@link #getParent()}.
         * @since 1.8.4
         */
        public final OptionsHelper parent;

        /** Stores the supplied parent reference; settings methods use the enclosing {@code OptionsHelper} instance, which may differ.
         * @param OptionsHelper parent helper reference to retain
         */
        public VideoOptionsHelper(OptionsHelper OptionsHelper) {
            parent = OptionsHelper;
        }

        /**
         * @return the parent options helper.
         * @since 1.8.4
         */
        public OptionsHelper getParent() {
            return parent;
        }

        /**
         * @return the configured fullscreen video mode string, such as {@code 1920x1080@60:24}
         * (width x height @ refresh rate : total color bits), or {@code null} if none is configured;
         * this may not match the active display mode
         * @since 1.8.4
         */
        @Nullable
        public String getFullscreenResolution() {
            return base.fullscreenVideoModeString;
        }

        /**
         * @return the current biome blend radius, in blocks.
         * @since 1.8.4
         */
        public int getBiomeBlendRadius() {
            return base.biomeBlendRadius().get();
        }

        /**
         * @param radius the new biome blend radius, in blocks
         * @return self for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper setBiomeBlendRadius(int radius) {
            getBase(base.biomeBlendRadius()).forceSetValue(radius);
            return this;
        }

        //? if >=1.21.11 {
        /*private OptionInstance<GraphicsPreset> graphicsOption() {
            return base.graphicsPreset();
        }

        private GraphicsPreset parseGraphicsPreset(String mode) {
            switch (mode.toUpperCase(Locale.ROOT)) {
                case "FAST":      return GraphicsPreset.FAST;
                case "FANCY":     return GraphicsPreset.FANCY;
                case "FABULOUS":  return GraphicsPreset.FABULOUS;
                default:          return graphicsOption().get();
            }
        }
        *///?} else {
        private OptionInstance<GraphicsStatus> graphicsOption() {
            return base.graphicsMode();
        }

        private GraphicsStatus parseGraphicsPreset(String mode) {
            switch (mode.toUpperCase(Locale.ROOT)) {
                case "FAST":      return GraphicsStatus.FAST;
                case "FANCY":     return GraphicsStatus.FANCY;
                case "FABULOUS":  return GraphicsStatus.FABULOUS;
                default:          return graphicsOption().get();
            }
        }
        //?}

        /** Returns the selected graphics mode name.
         * @return {@code "fast"}, {@code "fancy"}, or {@code "fabulous"}; an unrecognized stored mode throws {@code IllegalArgumentException}.
         */
        @DocletReplaceReturn("GraphicsMode")
        public String getGraphicsMode() {
            switch (graphicsOption().get()) {
                case FAST:     return "fast";
                case FANCY:    return "fancy";
                case FABULOUS: return "fabulous";
                default:       throw new IllegalArgumentException();
            }
        }

        /** Sets the graphics mode. Matching is case-insensitive; an unrecognized value retains the current selection but still calls the option setter.
         * @param mode non-null mode name; recognized values are {@code "fast"}, {@code "fancy"}, and {@code "fabulous"}
         * @return self for chaining
         */
        @DocletReplaceParams("mode: GraphicsMode")
        @DocletDeclareType(name = "GraphicsMode", type = "'fast' | 'fancy' | 'fabulous'")
        public VideoOptionsHelper setGraphicsMode(String mode) {
            graphicsOption().set(parseGraphicsPreset(mode));
            return this;
        }

        /**
         * @return the selected chunk builder mode.
         * @since 1.8.4
         */
        @DocletReplaceReturn("ChunkBuilderMode")
        public String getChunkBuilderMode() {
            switch (base.prioritizeChunkUpdates().get()) {
                case NONE:
                    return "none";
                case NEARBY:
                    return "nearby";
                case PLAYER_AFFECTED:
                    return "player_affected";
                default:
                    throw new IllegalArgumentException();
            }
        }

        /**
         * @param mode the chunk builder mode; {@code "none"}, {@code "nearby"}, or
         *             {@code "player_affected"}, case-insensitively. Other values leave the
         *             current mode unchanged.
         * @return self for chaining.
         * @since 1.8.4
         */
        @DocletReplaceParams("mode: ChunkBuilderMode")
        @DocletDeclareType(name = "ChunkBuilderMode", type = "'none' | 'nearby' | 'player_affected'")
        public VideoOptionsHelper setChunkBuilderMode(String mode) {
            PrioritizeChunkUpdates newMode;
            switch (mode.toUpperCase(Locale.ROOT)) {
                case "NONE":
                    newMode = PrioritizeChunkUpdates.NONE;
                    break;
                case "NEARBY":
                    newMode = PrioritizeChunkUpdates.NEARBY;
                    break;
                case "PLAYER_AFFECTED":
                    newMode = PrioritizeChunkUpdates.PLAYER_AFFECTED;
                    break;
                default:
                    newMode = base.prioritizeChunkUpdates().get();
                    break;
            }
            base.prioritizeChunkUpdates().set(newMode);
            return this;
        }

        /**
         * @return whether ambient occlusion is enabled.
         * @since 1.8.4
         */
        public boolean getSmoothLightningMode() {
            return base.ambientOcclusion().get();
        }

        /**
         * @param mode {@code true} to enable ambient occlusion; {@code false} to disable it
         * @return self for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper setSmoothLightningMode(boolean mode) {
            base.ambientOcclusion().set(mode);
            return this;
        }

        /**
         * @return the current render distance in chunks.
         * @since 1.8.4
         */
        public int getRenderDistance() {
            return base.renderDistance().get();
        }

        /**
         * @param radius the new render distance, in chunks
         * @return self for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper setRenderDistance(int radius) {
            base.renderDistance().set(radius);
            return this;
        }

        /**
         * @return the current simulation distance in chunks.
         * @since 1.8.4
         */
        public int getSimulationDistance() {
            return base.simulationDistance().get();
        }

        /**
         * @param radius the new simulation distance, in chunks
         * @return self for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper setSimulationDistance(int radius) {
            base.simulationDistance().set(radius);
            return this;
        }

        /**
         * @return the current upper fps limit.
         * @since 1.8.4
         */
        public int getMaxFps() {
            return base.framerateLimit().get();
        }

        /**
         * @param maxFps the maximum frame-rate limit to set
         * @return self for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper setMaxFps(int maxFps) {
            base.framerateLimit().set(maxFps);
            return this;
        }

        /**
         * @return {@code true} if vsync is enabled, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isVsyncEnabled() {
            return base.enableVsync().get();
        }

        /**
         * @param val {@code true} to enable vsync; {@code false} to disable it
         * @return self for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper enableVsync(boolean val) {
            base.enableVsync().set(val);
            return this;
        }

        /**
         * @return {@code true} if view bobbing is enabled, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isViewBobbingEnabled() {
            return base.bobView().get();
        }

        /**
         * @param val {@code true} to enable view bobbing; {@code false} to disable it
         * @return self for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper enableViewBobbing(boolean val) {
            base.bobView().set(val);
            return this;
        }

        /**
         * @return the configured GUI scale factor; Minecraft may calculate a different effective scale
         *         to fit the display.
         * @since 1.8.4
         */
        public int getGuiScale() {
            return base.guiScale().get();
        }

        /**
         * @param scale the GUI scale factor to set; Minecraft may adjust the effective scale to fit
         *              the display
         * @return self for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper setGuiScale(int scale) {
            base.guiScale().set(scale);
            //? if >=26.1 {
            /*mc.execute(mc::resizeGui);
            *///? } else {
            mc.execute(mc::resizeDisplay);
            //? }
            return this;
        }

        /**
         * @return {@code "off"}, {@code "crosshair"}, or {@code "hotbar"}, according to the selected
         *         attack indicator
         * @since 1.8.4
         */
        @DocletReplaceReturn("AttackIndicatorType")
        public String getAttackIndicatorType() {
            return switch (base.attackIndicator().get()) {
                case OFF -> "off";
                case CROSSHAIR -> "crosshair";
                case HOTBAR -> "hotbar";
                default -> throw new IllegalArgumentException();
            };
        }

        /**
         * @param type the attack indicator type: {@code "off"}, {@code "crosshair"}, or {@code "hotbar"};
         *             other values leave the current setting unchanged
         * @return self for chaining.
         * @since 1.8.4
         */
        @DocletReplaceParams("type: AttackIndicatorType")
        @DocletDeclareType(name = "AttackIndicatorType", type = "'off' | 'crosshair' | 'hotbar'")
        public VideoOptionsHelper setAttackIndicatorType(String type) {
            AttackIndicatorStatus newType;
            switch (type.toUpperCase(Locale.ROOT)) {
                case "OFF":
                    newType = AttackIndicatorStatus.OFF;
                    break;
                case "CROSSHAIR":
                    newType = AttackIndicatorStatus.CROSSHAIR;
                    break;
                case "HOTBAR":
                    newType = AttackIndicatorStatus.HOTBAR;
                    break;
                default:
                    newType = base.attackIndicator().get();
                    break;
            }
            base.attackIndicator().set(newType);
            return this;
        }

        /**
         * @return the current brightness (gamma) value.
         * @since 1.8.4
         */
        public double getGamma() {
            return getBrightness();
        }

        /**
         * @param gamma the gamma (brightness) value to set
         * @return self for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper setGamma(double gamma) {
            return setBrightness(gamma);
        }

        /**
         * @return the current brightness value.
         * @since 1.8.4
         */
        public double getBrightness() {
            return base.gamma().get();
        }

        /**
         * @param gamma the brightness (gamma) value to set
         * @return self for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper setBrightness(double gamma) {
            getBase(base.gamma()).forceSetValue(gamma);
            return this;
        }

        /**
         * @return the current cloud rendering mode.
         * @since 1.8.4
         */
        @DocletReplaceReturn("CloudsMode")
        public String getCloudsMode() {
            switch (base.cloudStatus().get()) {
                case OFF:
                    return "off";
                case FAST:
                    return "fast";
                case FANCY:
                    return "fancy";
                default:
                    throw new IllegalArgumentException();
            }
        }

        /**
         * @param mode the cloud rendering mode; accepts "off", "fast", or "fancy" (case-insensitive). Other values leave the setting unchanged
         * @return self for chaining.
         * @since 1.8.4
         */
        @DocletReplaceParams("mode: CloudsMode")
        @DocletDeclareType(name = "CloudsMode", type = "'off' | 'fast' | 'fancy'")
        public VideoOptionsHelper setCloudsMode(String mode) {
            CloudStatus newMode;
            switch (mode.toUpperCase(Locale.ROOT)) {
                case "OFF":
                    newMode = CloudStatus.OFF;
                    break;
                case "FAST":
                    newMode = CloudStatus.FAST;
                    break;
                case "FANCY":
                    newMode = CloudStatus.FANCY;
                    break;
                default:
                    newMode = base.cloudStatus().get();
                    break;
            }
            base.cloudStatus().set(newMode);
            return this;
        }

        /**
         * @return {@code true} if the game is running in fullscreen mode, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isFullscreen() {
            return base.fullscreen().get();
        }

        /**
         * @param fullscreen whether to enable fullscreen mode
         * @return self for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper setFullScreen(boolean fullscreen) {
            base.fullscreen().set(fullscreen);
            return this;
        }

        /**
         * @return the current particle rendering mode.
         * @since 1.8.4
         */
        @DocletReplaceReturn("ParticleMode")
        public String getParticleMode() {
            switch (base.particles().get()) {
                case MINIMAL:
                    return "minimal";
                case DECREASED:
                    return "decreased";
                case ALL:
                    return "all";
                default:
                    throw new IllegalArgumentException();
            }
        }

        /**
         * @param mode the particle rendering mode; accepts "minimal", "decreased", or "all" (case-insensitive). Other values leave the setting unchanged
         * @return self for chaining.
         * @since 1.8.4
         */
        @DocletReplaceParams("mode: ParticleMode")
        @DocletDeclareType(name = "ParticleMode", type = "'minimal' | 'decreased' | 'all'")
        public VideoOptionsHelper setParticleMode(String mode) {
            ParticleStatus newMode;
            switch (mode.toUpperCase(Locale.ROOT)) {
                case "MINIMAL":
                    newMode = ParticleStatus.MINIMAL;
                    break;
                case "DECREASED":
                    newMode = ParticleStatus.DECREASED;
                    break;
                case "ALL":
                    newMode = ParticleStatus.ALL;
                    break;
                default:
                    newMode = base.particles().get();
                    break;
            }
            base.particles().set(newMode);
            return this;
        }

        /**
         * @return the current mip map level.
         * @since 1.8.4
         */
        public int getMipMapLevels() {
            return base.mipmapLevels().get();
        }

        /**
         * Sets the mipmap level.
         * @param val the mipmap level to set
         * @return this helper for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper setMipMapLevels(int val) {
            base.mipmapLevels().set(val);
            return this;
        }

        /**
         * @return {@code true} if entity shadows are enabled; {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean areEntityShadowsEnabled() {
            return base.entityShadows().get();
        }

        /**
         * Enables or disables entity shadows.
         * @param val whether to enable entity shadows
         * @return this helper for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper enableEntityShadows(boolean val) {
            base.entityShadows().set(val);
            return this;
        }

        /**
         * @return the current screen-effect scale.
         * @since 1.8.4
         */
        public double getDistortionEffect() {
            return base.screenEffectScale().get();
        }

        /**
         * Sets the screen-effect scale.
         * @param val the screen-effect scale to set
         * @return this helper for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper setDistortionEffects(double val) {
            base.screenEffectScale().set(val);
            return this;
        }

        /**
         * @return the current entity-distance scaling value.
         * @since 1.8.4
         */
        public double getEntityDistance() {
            return base.entityDistanceScaling().get();
        }

        /**
         * Sets the entity-distance scaling value.
         * @param val the scaling value to set
         * @return this helper for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper setEntityDistance(double val) {
            base.entityDistanceScaling().set(val);
            return this;
        }

        /**
         * @return the current field-of-view effect scale.
         * @since 1.8.4
         */
        public double getFovEffects() {
            return base.fovEffectScale().get();
        }

        /**
         * Sets the field-of-view effect scale.
         * @param val the scale to set
         * @return this helper for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper setFovEffects(double val) {
            getBase(base.fovEffectScale()).forceSetValue(val);
            return this;
        }

        /**
         * @return {@code true} if the autosave indicator is enabled; {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isAutosaveIndicatorEnabled() {
            return base.showAutosaveIndicator().get();
        }

        /**
         * @param val whether to show the autosave indicator
         * @return this helper for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper enableAutosaveIndicator(boolean val) {
            base.showAutosaveIndicator().set(val);
            return this;
        }

    }

    /** Sound volumes, audio-device selection, and subtitles.
     * <pre>
     * const options = Client.getGameOptions();
     * options.music.getMasterVolume();
     * </pre>
     */
    @DocletCategory("Configuration/Profiles")
    public class MusicOptionsHelper {

        /**
         * The options helper that owns this group.
         * @since 1.8.4
         */
        public final OptionsHelper parent;

        /** Stores the supplied parent reference; settings methods use the enclosing {@code OptionsHelper} instance, which may differ.
         * @param OptionsHelper parent helper reference to retain
         */
        public MusicOptionsHelper(OptionsHelper OptionsHelper) {
            parent = OptionsHelper;
        }

        /**
         * @return the options helper that owns this group.
         * @since 1.8.4
         */
        public OptionsHelper getParent() {
            return parent;
        }

        /**
         * @return the current master sound volume.
         * @since 1.8.4
         */
        public float getMasterVolume() {
            return getSoundSourceVolume(SoundSource.MASTER);
        }

        /**
         * @param volume the new master sound volume
         * @return this helper for chaining.
         * @since 1.8.4
         */
        public MusicOptionsHelper setMasterVolume(double volume) {
            base.getSoundSourceOptionInstance(SoundSource.MASTER).set(volume);
            return this;
        }

        /**
         * @return the current music sound volume.
         * @since 1.8.4
         */
        public float getMusicVolume() {
            return getSoundSourceVolume(SoundSource.MUSIC);
        }

        /**
         * @param volume the new music sound volume
         * @return this helper for chaining.
         * @since 1.8.4
         */
        public MusicOptionsHelper setMusicVolume(double volume) {
            base.getSoundSourceOptionInstance(SoundSource.MUSIC).set(volume);
            return this;
        }

        /**
         * @return the current records sound volume.
         * @since 1.8.4
         */
        public float getRecordsVolume() {
            return getSoundSourceVolume(SoundSource.RECORDS);
        }

        /**
         * @param volume the new records sound volume
         * @return this helper for chaining.
         * @since 1.8.4
         */
        public MusicOptionsHelper setRecordsVolume(double volume) {
            base.getSoundSourceOptionInstance(SoundSource.RECORDS).set(volume);
            return this;
        }

        /**
         * @return the current weather sound volume.
         * @since 1.8.4
         */
        public float getWeatherVolume() {
            return getSoundSourceVolume(SoundSource.WEATHER);
        }

        /**
         * @param volume the new volume for the weather
         * @return self for chaining.
         * @since 1.8.4
         */
        public MusicOptionsHelper setWeatherVolume(double volume) {
            base.getSoundSourceOptionInstance(SoundSource.WEATHER).set(volume);
            return this;
        }

        /**
         * @return the current volume of block related sounds.
         * @since 1.8.4
         */
        public float getBlocksVolume() {
            return getSoundSourceVolume(SoundSource.BLOCKS);
        }

        /**
         * @param volume the new volume for block sounds
         * @return self for chaining.
         * @since 1.8.4
         */
        public MusicOptionsHelper setBlocksVolume(double volume) {
            base.getSoundSourceOptionInstance(SoundSource.BLOCKS).set(volume);
            return this;
        }

        /**
         * @return the current volume of hostile mobs.
         * @since 1.8.4
         */
        public float getHostileVolume() {
            return getSoundSourceVolume(SoundSource.HOSTILE);
        }

        /**
         * @param volume the new volume for hostile mobs
         * @return self for chaining.
         * @since 1.8.4
         */
        public MusicOptionsHelper setHostileVolume(double volume) {
            base.getSoundSourceOptionInstance(SoundSource.HOSTILE).set(volume);
            return this;
        }

        /**
         * @return the current volume of neutral mobs.
         * @since 1.8.4
         */
        public float getNeutralVolume() {
            return getSoundSourceVolume(SoundSource.NEUTRAL);
        }

        /**
         * @param volume the new volume for neutral mobs
         * @return self for chaining.
         * @since 1.8.4
         */
        public MusicOptionsHelper setNeutralVolume(double volume) {
            base.getSoundSourceOptionInstance(SoundSource.NEUTRAL).set(volume);
            return this;
        }

        /**
         * @return the current player volume.
         * @since 1.8.4
         */
        public float getPlayerVolume() {
            return getSoundSourceVolume(SoundSource.PLAYERS);
        }

        /**
         * @param volume the new player volume
         * @return self for chaining.
         * @since 1.8.4
         */
        public MusicOptionsHelper setPlayerVolume(double volume) {
            base.getSoundSourceOptionInstance(SoundSource.PLAYERS).set(volume);
            return this;
        }

        /**
         * @return the current ambient volume.
         * @since 1.8.4
         */
        public float getAmbientVolume() {
            return getSoundSourceVolume(SoundSource.AMBIENT);
        }

        /**
         * Sets the ambient sound volume.
         * @param volume the new ambient volume
         * @return self for chaining.
         * @since 1.8.4
         */
        public MusicOptionsHelper setAmbientVolume(double volume) {
            base.getSoundSourceOptionInstance(SoundSource.AMBIENT).set(volume);
            return this;
        }

        /**
         * Returns the voice sound volume.
         * @return the current voice volume.
         * @since 1.8.4
         */
        public float getVoiceVolume() {
            return getSoundSourceVolume(SoundSource.VOICE);
        }

        /**
         * Sets the voice sound volume.
         * @param volume the new voice volume
         * @return self for chaining.
         * @since 1.8.4
         */
        public MusicOptionsHelper setVoiceVolume(double volume) {
            base.getSoundSourceOptionInstance(SoundSource.VOICE).set(volume);
            return this;
        }

        /**
         * Returns the volume for a sound category, looked up by its category name.
         * @param category the category to get the volume of
         * @return the volume of the given sound category.
         * @since 1.8.4
         */
        @DocletReplaceParams("category: SoundCategory")
        public float getVolume(String category) {
            return getSoundSourceVolume(SOUND_CATEGORY_MAP.get(category));
        }

        /**
         * Returns the volume of each sound category, keyed by category name.
         * @return a map of all sound categories and their volumes.
         * @since 1.8.4
         */
        public Map<String, Float> getVolumes() {
            Map<String, Float> volumes = new HashMap<>();
            for (SoundSource category : SoundSource.values()) {
                volumes.put(category.getName(), getSoundSourceVolume(category));
            }
            return volumes;
        }

        /**
         * Sets the volume for a sound category, looked up by its category name.
         * @param category the category to set the volume for
         * @param volume   the new volume
         * @return self for chaining.
         * @since 1.8.4
         */
        @DocletReplaceParams("category: SoundCategory, volume: double")
        public MusicOptionsHelper setVolume(String category, double volume) {
            base.getSoundSourceOptionInstance(SOUND_CATEGORY_MAP.get(category)).set(volume);
            return this;
        }

        /**
         * Returns the selected audio device identifier.
         * @return the currently selected sound device.
         * @since 1.8.4
         */
        public String getSoundDevice() {
            return base.soundDevice().get();
        }

        /**
         * Selects an audio device and reloads the sound manager. An identifier not present in
         * {@link #getAudioDevices()} selects the default device.
         * @param audioDevice the audio device to use
         * @return self for chaining.
         * @since 1.8.4
         */
        public MusicOptionsHelper setSoundDevice(String audioDevice) {
            List<String> audioDevices = getAudioDevices();
            if (!audioDevices.contains(audioDevice)) {
                audioDevice = "";
            }
            base.soundDevice().set(audioDevice);
            SoundManager soundManager = Minecraft.getInstance().getSoundManager();
            soundManager.reload();
            return this;
        }

        /**
         * Returns available audio device identifiers, including the empty identifier for the
         * default device.
         * @return the available audio device identifiers.
         * @since 1.8.4
         */
        public List<String> getAudioDevices() {
            return Stream.concat(Stream.of(""), Minecraft.getInstance().getSoundManager().getAvailableSoundDevices().stream()).collect(Collectors.toList());
        }

        /**
         * Returns whether subtitles are enabled.
         * @return {@code true} if subtitles should be shown, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean areSubtitlesShown() {
            return base.showSubtitles().get();
        }

        /**
         * @param val whether subtitles should be shown or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public MusicOptionsHelper showSubtitles(boolean val) {
            base.showSubtitles().set(val);
            return this;
        }

    }

    /** Input and mouse settings, including key bindings.
     * <pre>
     * const options = Client.getGameOptions();
     * options.control.getMouseSensitivity();
     * </pre>
     */
    @DocletCategory("Configuration/Profiles")
    public class ControlOptionsHelper {

        /**
         * the whole options helper this group belongs to.
         * <p>
         * Each group is a view onto one part of the settings, and this is the way back to the
         * rest of them, so a script that starts in a group and needs a setting from another
         * does not have to keep the outer handle around separately. It is the same object the
         * field was created with, and the same one the group's {@code getParent()} returns.
         * <br>
         * The six group fields hold the same thing, so the example on
         * {@code SkinOptionsHelper.parent}, the first of them, covers this one too.
         * @since 1.8.4
         */
        public final OptionsHelper parent;

        /** Stores the supplied parent reference; settings methods use the enclosing {@code OptionsHelper} instance, which may differ.
         * @param OptionsHelper parent helper reference to retain
         */
        public ControlOptionsHelper(OptionsHelper OptionsHelper) {
            parent = OptionsHelper;
        }

        /**
         * @return the parent options helper.
         * @since 1.8.4
         */
        public OptionsHelper getParent() {
            return parent;
        }

        /**
         * @return the current mouse sensitivity.
         * @since 1.8.4
         */
        public double getMouseSensitivity() {
            return base.sensitivity().get();
        }

        /**
         * @param val the new mouse sensitivity
         * @return self for chaining.
         * @since 1.8.4
         */
        public ControlOptionsHelper setMouseSensitivity(double val) {
            getBase(base.sensitivity()).forceSetValue(val);
            return this;
        }

        /**
         * @return {@code true} if the mouse direction should be inverted.
         * @since 1.8.4
         */
        public boolean isMouseInverted() {
            //? if >1.21.8 {
            /*return base.invertMouseY().get();
            *///?} else {
            return base.invertYMouse().get();
            //?}
        }

        /**
         * @param val whether to invert the mouse direction or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public ControlOptionsHelper invertMouse(boolean val) {
            //? if >1.21.8 {
            /*base.invertMouseY().set(val);
            *///?} else {
            base.invertYMouse().set(val);
            //?}
            return this;
        }

        /**
         * @return the current mouse wheel sensitivity.
         * @since 1.8.4
         */
        public double getMouseWheelSensitivity() {
            return base.mouseWheelSensitivity().get();
        }

        /**
         * @param val the new mouse wheel sensitivity
         * @return self for chaining.
         * @since 1.8.4
         */
        public ControlOptionsHelper setMouseWheelSensitivity(double val) {
            getBase(base.mouseWheelSensitivity()).forceSetValue(val);
            return this;
        }

        /**
         * This option was introduced due to a bug on some systems where the mouse wheel would
         * scroll too fast.
         *
         * @return {@code true} if discrete scrolling is enabled, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isDiscreteScrollingEnabled() {
            return base.discreteMouseScroll().get();
        }

        /**
         * Sets whether discrete mouse scrolling is enabled.
         * @param val {@code true} to enable discrete scrolling
         * @return this helper for chaining.
         * @since 1.8.4
         */
        public ControlOptionsHelper enableDiscreteScrolling(boolean val) {
            base.discreteMouseScroll().set(val);
            return this;
        }

        /**
         * Returns whether touchscreen mode is enabled.
         * @return {@code true} if enabled; {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isTouchscreenEnabled() {
            return base.touchscreen().get();
        }

        /**
         * Sets whether touchscreen mode is enabled.
         * @param val {@code true} to enable touchscreen mode
         * @return this helper for chaining.
         * @since 1.8.4
         */
        public ControlOptionsHelper enableTouchscreen(boolean val) {
            base.touchscreen().set(val);
            return this;
        }

        /**
         * Returns whether raw mouse input is enabled.
         * @return {@code true} if enabled; {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isRawMouseInputEnabled() {
            return base.rawMouseInput().get();
        }

        /**
         * Sets whether raw mouse input is enabled.
         * @param val {@code true} to enable raw mouse input
         * @return this helper for chaining.
         * @since 1.8.4
         */
        public ControlOptionsHelper enableRawMouseInput(boolean val) {
            base.rawMouseInput().set(val);
            return this;
        }

        /**
         * Returns whether auto-jump is enabled.
         * @return {@code true} if enabled; {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isAutoJumpEnabled() {
            return base.autoJump().get();
        }

        /**
         * Sets whether auto-jump is enabled.
         * @param val {@code true} to enable auto-jump
         * @return this helper for chaining.
         * @since 1.8.4
         */
        public ControlOptionsHelper enableAutoJump(boolean val) {
            base.autoJump().set(val);
            return this;
        }

        /**
         * Returns whether sneak toggling is enabled.
         * @return {@code true} if enabled; {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isSneakTogglingEnabled() {
            return base.toggleCrouch().get();
        }

        /**
         * Sets whether sneak toggling is enabled.
         * @param val {@code true} to enable sneak toggling
         * @return this helper for chaining.
         * @since 1.8.4
         */
        public ControlOptionsHelper toggleSneak(boolean val) {
            base.toggleCrouch().set(val);
            return this;
        }

        /**
         * Returns whether sprint toggling is enabled.
         * @return {@code true} if enabled; {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isSprintTogglingEnabled() {
            return base.toggleSprint().get();
        }

        /**
         * @param val whether sprint toggling is enabled
         * @return this helper, for chaining.
         * @since 1.8.4
         */
        public ControlOptionsHelper toggleSprint(boolean val) {
            base.toggleSprint().set(val);
            return this;
        }

        /**
         * @return a shallow clone of Minecraft's key-mapping array; the mappings themselves are not cloned.
         * @since 1.8.4
         */
        public KeyMapping[] getRawKeys() {
            return ArrayUtils.clone(base.keyMappings);
        }

        /**
         * @return distinct category identifiers used by the key mappings.
         * @since 1.8.4
         */
        @DocletReplaceReturn("JavaList<KeyCategory>")
        public List<String> getCategories() {
            return Arrays.stream(base.keyMappings)
                    .map(KeyMapping::getCategory).distinct()
                    //? if >1.21.8 {
                    /*.map(c -> c.id().toLanguageKey())
                    *///?}
                    .collect(Collectors.toList());
        }

        /**
         * @return the key-mapping names, in mapping-array order.
         * @since 1.8.4
         */
        @DocletReplaceReturn("JavaList<Key>")
        public List<String> getKeys() {
            return Arrays.stream(base.keyMappings).map(KeyMapping::getName).collect(Collectors.toList());
        }

        /**
         * @return translated key-mapping names mapped to their translated bound-key labels.
         * @since 1.8.4
         */
        @DocletReplaceReturn("JavaMap<Bind, Key>")
        public Map<String, String> getKeyBinds() {
            Map<String, String> keyBinds = new HashMap<>(base.keyMappings.length);

            for (KeyMapping key : base.keyMappings) {
                keyBinds.put(Component.translatable(key.getName()).getString(), key.getTranslatedKeyMessage().getString());
            }
            return keyBinds;
        }

        /**
         * @param category the category identifier
         * @return translated key-mapping names and bound-key labels for the category, or {@code null} if absent.
         * @since 1.8.4
         */
        @DocletReplaceParams("category: KeyCategory")
        public Map<String, String> getKeyBindsByCategory(String category) {
            return getKeyBindsByCategory().get(category);
        }

        /**
         * @return category identifiers mapped to translated key-mapping names and bound-key labels.
         * @since 1.8.4
         */
        public Map<String, Map<String, String>> getKeyBindsByCategory() {
            Map<String, Map<String, String>> entries = new HashMap<>(Minecraft.getInstance().options.keyMappings.length);

            for (KeyMapping key : Minecraft.getInstance().options.keyMappings) {
                Map<String, String> categoryMap;
                //? >1.21.8 {
                /*String category = key.getCategory().id().toLanguageKey();
                *///?} else {
                String category = key.getCategory();
                //?}
                if (!entries.containsKey(category)) {
                    categoryMap = new HashMap<>();
                    entries.put(category, categoryMap);
                } else {
                    categoryMap = entries.get(category);
                }
                categoryMap.put(Component.translatable(key.getName()).getString(), key.getTranslatedKeyMessage().getString());
            }
            return entries;
        }

    }

    /** Chat visibility and layout settings.
     * <pre>
     * const options = Client.getGameOptions();
     * Chat.log(options.chat.getChatVisibility());
     * </pre>
     */
    @DocletCategory("Configuration/Profiles")
    public class ChatOptionsHelper {

        /**
         * The options helper that owns this settings group; the same instance is returned by
         * {@link #getParent()}.
         * @since 1.8.4
         */
        public final OptionsHelper parent;

        /** Stores the supplied parent reference; this does not replace the options object bound to this inner group.
         * @param OptionsHelper optionshelper retained as the parent
         */
        public ChatOptionsHelper(OptionsHelper OptionsHelper) {
            parent = OptionsHelper;
        }

        /**
         * @return the options helper that owns this settings group.
         * @since 1.8.4
         */
        public OptionsHelper getParent() {
            return parent;
        }

        /**
         * @return the final segment of the chat-visibility translation key, including its leading dot (for example, {@code .full}).
         * @since 1.8.4
         */
        @DocletReplaceReturn("ChatVisibility")
        public String getChatVisibility() {
            String chatVisibilityKey = base.chatVisibility().get()
                    //? if >=1.21.11 {
                    /*.name();
                    *///? } else {
                    .getKey();
                    //? }
            return chatVisibilityKey.substring(chatVisibilityKey.lastIndexOf('.'));
        }

        /**
         * Sets chat visibility to {@code FULL}, {@code SYSTEM}, or {@code HIDDEN}; matching is case-insensitive.
         * Unrecognized values leave the current setting unchanged.
         * @param mode the visibility mode
         * @return this helper
         * @since 1.8.4
         */
        @DocletReplaceParams("mode: ChatVisibility")
        @DocletDeclareType(name = "ChatVisibility", type = "'FULL' | 'SYSTEM' | 'HIDDEN'")
        public ChatOptionsHelper setChatVisibility(String mode) {
            ChatVisiblity newMode;
            switch (mode.toUpperCase(Locale.ROOT)) {
                case "FULL":
                    newMode = ChatVisiblity.FULL;
                    break;
                case "SYSTEM":
                    newMode = ChatVisiblity.SYSTEM;
                    break;
                case "HIDDEN":
                    newMode = ChatVisiblity.HIDDEN;
                    break;
                default:
                    newMode = base.chatVisibility().get();
                    break;
            }
            base.chatVisibility().set(newMode);
            return this;
        }

        /**
         * @return whether chat colors are enabled
         * @since 1.8.4
         */
        public boolean areColorsShown() {
            return base.chatColors().get();
        }

        /**
         * Sets whether chat colors are enabled.
         * @param val whether chat colors are enabled
         * @return this helper
         * @since 1.8.4
         */
        public ChatOptionsHelper setShowColors(boolean val) {
            base.chatColors().set(val);
            return this;
        }

        /**
         * @return whether opening web links from chat is enabled
         * @since 1.8.4
         */
        public boolean areWebLinksEnabled() {
            return base.chatLinks().get();
        }

        /**
         * Sets whether opening web links from chat is enabled.
         * @param val whether opening chat links is enabled
         * @return this helper
         * @since 1.8.4
         */
        public ChatOptionsHelper enableWebLinks(boolean val) {
            base.chatLinks().set(val);
            return this;
        }

        /**
         * @return whether a warning prompt is enabled before opening chat links
         * @since 1.8.4
         */
        public boolean isWebLinkPromptEnabled() {
            return base.chatLinksPrompt().get();
        }

        /**
         * Sets whether to show a warning prompt before opening chat links.
         * @param val whether the warning prompt is enabled
         * @return this helper
         * @since 1.8.4
         */
        public ChatOptionsHelper enableWebLinkPrompt(boolean val) {
            base.chatLinksPrompt().set(val);
            return this;
        }

        /**
         * @return the configured chat opacity
         * @since 1.8.4
         */
        public double getChatOpacity() {
            return base.chatOpacity().get();
        }

        /**
         * Sets the chat opacity.
         * @param val the new opacity
         * @return this helper
         * @since 1.8.4
         */
        public ChatOptionsHelper setChatOpacity(double val) {
            base.chatOpacity().set(val);
            return this;
        }

        /**
         * Sets the opacity of text backgrounds.
         * @param val the new text background opacity
         * @return this helper
         * @since 1.8.4
         */
        public ChatOptionsHelper setTextBackgroundOpacity(double val) {
            getBase(base.textBackgroundOpacity()).forceSetValue(val);
            return this;
        }

        /**
         * Returns the configured opacity for text backgrounds.
         * @return the current text background opacity.
         * @since 1.8.4
         */
        public double getTextBackgroundOpacity() {
            return base.textBackgroundOpacity().get();
        }

        /**
         * Returns the configured chat text scale.
         * @return the current text scale.
         * @since 1.8.4
         */
        public double getTextSize() {
            return base.chatScale().get();
        }

        /**
         * Sets the chat text scale.
         * @param val the new text scale
         * @return self for chaining.
         * @since 1.8.4
         */
        public ChatOptionsHelper setTextSize(double val) {
            getBase(base.chatScale()).forceSetValue(val);
            return this;
        }

        /**
         * Returns the configured spacing between chat lines.
         * @return the current chat line spacing.
         * @since 1.8.4
         */
        public double getChatLineSpacing() {
            return base.chatLineSpacing().get();
        }

        /**
         * Sets the spacing between chat lines.
         * @param val the new chat line spacing
         * @return self for chaining.
         * @since 1.8.4
         */
        public ChatOptionsHelper setChatLineSpacing(double val) {
            getBase(base.chatLineSpacing()).forceSetValue(val);
            return this;
        }

        /**
         * Returns the configured delay between chat messages.
         * @return the current chat delay in seconds.
         * @since 1.8.4
         */
        public double getChatDelay() {
            return base.chatDelay().get();
        }

        /**
         * Sets the delay between chat messages.
         * @param val the new chat delay in seconds
         * @return self for chaining.
         * @since 1.8.4
         */
        public ChatOptionsHelper setChatDelay(double val) {
            base.chatDelay().set(val);
            return this;
        }

        /**
         * Returns the configured chat width scale.
         * @return the current chat width scale.
         * @since 1.8.4
         */
        public double getChatWidth() {
            return base.chatWidth().get();
        }

        /**
         * Sets the chat width scale.
         * @param val the new chat width scale
         * @return self for chaining.
         * @since 1.8.4
         */
        public ChatOptionsHelper setChatWidth(double val) {
            getBase(base.chatWidth()).forceSetValue(val);
            return this;
        }

        /**
         * Returns the configured chat height scale while chat is focused.
         * @return the focused chat height scale.
         * @since 1.8.4
         */
        public double getChatFocusedHeight() {
            return base.chatHeightFocused().get();
        }

        /**
         * @param val the chat height scale while chat is focused
         * @return self for chaining.
         * @since 1.8.4
         */
        public ChatOptionsHelper setChatFocusedHeight(double val) {
            getBase(base.chatHeightFocused()).forceSetValue(val);
            return this;
        }

        /**
         * @return the chat height scale while chat is unfocused.
         * @since 1.8.4
         */
        public double getChatUnfocusedHeight() {
            return base.chatHeightUnfocused().get();
        }

        /**
         * @param val the chat height scale while chat is unfocused
         * @return self for chaining.
         * @since 1.8.4
         */
        public ChatOptionsHelper setChatUnfocusedHeight(double val) {
            getBase(base.chatHeightUnfocused()).forceSetValue(val);
            return this;
        }

        /**
         * @return the final segment of the narrator translation key, including its leading dot (for example, {@code .all}).
         * @since 1.8.4
         */
        @DocletReplaceReturn("NarratorMode")
        public String getNarratorMode() {
            String narratorKey = ((TranslatableContents) (base.narrator().get().getName().getContents())).getKey();
            return narratorKey.substring(narratorKey.lastIndexOf('.'));
        }

        /**
         * @param mode the narrator mode to set; accepts {@code OFF}, {@code ALL}, {@code CHAT}, or {@code SYSTEM}, case-insensitively. Other values leave the current mode unchanged.
         * @return self for chaining.
         * @since 1.8.4
         */
        @DocletReplaceParams("mode: NarratorMode")
        @DocletDeclareType(name = "NarratorMode", type = "'OFF' | 'ALL' | 'CHAT' | 'SYSTEM'")
        public ChatOptionsHelper setNarratorMode(String mode) {
            NarratorStatus newMode;
            switch (mode.toUpperCase(Locale.ROOT)) {
                case "OFF":
                    newMode = NarratorStatus.OFF;
                    break;
                case "ALL":
                    newMode = NarratorStatus.ALL;
                    break;
                case "CHAT":
                    newMode = NarratorStatus.CHAT;
                    break;
                case "SYSTEM":
                    newMode = NarratorStatus.SYSTEM;
                    break;
                default:
                    newMode = base.narrator().get();
                    break;
            }
            base.narrator().set(newMode);
            return this;
        }

        /**
         * @return {@code true} if command suggestions are enabled, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean areCommandSuggestionsEnabled() {
            return base.autoSuggestions().get();
        }

        /**
         * @param val whether to enable command suggestions
         * @return self for chaining.
         * @since 1.8.4
         */
        public ChatOptionsHelper enableCommandSuggestions(boolean val) {
            base.autoSuggestions().set(val);
            return this;
        }

        /**
         * @return {@code true} if messages from blocked players are hidden, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean areMatchedNamesHidden() {
            return base.hideMatchedNames().get();
        }

        /**
         * @param val whether to hide messages from blocked players
         * @return self for chaining.
         * @since 1.8.4
         */
        public ChatOptionsHelper enableHideMatchedNames(boolean val) {
            base.hideMatchedNames().set(val);
            return this;
        }

        /**
         * @return {@code true} if reduced debug information is enabled, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isDebugInfoReduced() {
            return base.reducedDebugInfo().get();
        }

        /**
         * @param val whether to enable reduced debug info or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public ChatOptionsHelper reduceDebugInfo(boolean val) {
            base.reducedDebugInfo().set(val);
            return this;
        }

    }

    /** Provides accessibility settings, including narrator, subtitle, and chat-display options, on the enclosing options object.
     * <pre>
     * const options = Client.getGameOptions();
     * const accessibility = options.accessibility;
     * Chat.log(accessibility.getNarratorMode());
     * </pre>
     */
    @DocletCategory("Configuration/Profiles")
    public class AccessibilityOptionsHelper {

        /**
         * the whole options helper this group belongs to.
         * <p>
         * Each group is a view onto one part of the settings, and this is the way back to the
         * rest of them, so a script that starts in a group and needs a setting from another
         * does not have to keep the outer handle around separately. It is the same object the
         * field was created with, and the same one the group's {@code getParent()} returns.
         * <br>
         * The six group fields hold the same thing, so the example on
         * {@code SkinOptionsHelper.parent}, the first of them, covers this one too.
         * @since 1.8.4
         */
        public final OptionsHelper parent;

        /** Stores the supplied parent reference; this does not replace the options object bound to this inner group.
         * @param OptionsHelper optionshelper retained as the parent
         */
        public AccessibilityOptionsHelper(OptionsHelper OptionsHelper) {
            parent = OptionsHelper;
        }

        /**
         * @return the parent options helper.
         * @since 1.8.4
         */
        public OptionsHelper getParent() {
            return parent;
        }

        /**
         * @return the current narrator mode.
         * @since 1.8.4
         */
        public String getNarratorMode() {
            String narratorKey = ((TranslatableContents) (base.narrator().get().getName().getContents())).getKey();
            return narratorKey.substring(narratorKey.lastIndexOf('.'));
        }

        /**
         * @param mode the narrator mode: {@code "OFF"}, {@code "ALL"}, {@code "CHAT"}, or
         *             {@code "SYSTEM"} (case-insensitive); other values leave the current mode unchanged.
         * @return self for chaining.
         * @since 1.8.4
         */
        public AccessibilityOptionsHelper setNarratorMode(String mode) {
            NarratorStatus newMode;
            switch (mode.toUpperCase(Locale.ROOT)) {
                case "OFF":
                    newMode = NarratorStatus.OFF;
                    break;
                case "ALL":
                    newMode = NarratorStatus.ALL;
                    break;
                case "CHAT":
                    newMode = NarratorStatus.CHAT;
                    break;
                case "SYSTEM":
                    newMode = NarratorStatus.SYSTEM;
                    break;
                default:
                    newMode = base.narrator().get();
                    break;
            }
            base.narrator().set(newMode);
            return this;
        }

        /**
         * @return {@code true} if subtitles are enabled.
         * @since 1.8.4
         */
        public boolean areSubtitlesShown() {
            return base.showSubtitles().get();
        }

        /**
         * @param val whether to show subtitles or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public AccessibilityOptionsHelper showSubtitles(boolean val) {
            base.showSubtitles().set(val);
            return this;
        }

        /**
         * @param val the new opacity for the text background
         * @return self for chaining.
         * @since 1.8.4
         */
        public AccessibilityOptionsHelper setTextBackgroundOpacity(double val) {
            base.textBackgroundOpacity().set(val);
            return this;
        }

        /**
         * @return the opacity of the text background.
         * @since 1.8.4
         */
        public double getTextBackgroundOpacity() {
            return base.textBackgroundOpacity().get();
        }

        /**
         * @return {@code true} if the text background is shown only behind chat.
         * @since 1.8.4
         */
        public boolean isBackgroundForChatOnly() {
            return base.backgroundForChatOnly().get();
        }

        /**
         * @param val whether to show the text background only behind chat
         * @return this helper for chaining.
         * @since 1.8.4
         */
        public AccessibilityOptionsHelper enableBackgroundForChatOnly(boolean val) {
            base.backgroundForChatOnly().set(val);
            return this;
        }

        /**
         * @return the current chat opacity.
         * @since 1.8.4
         */
        public double getChatOpacity() {
            return base.chatOpacity().get();
        }

        /**
         * @param val the new chat opacity
         * @return self for chaining.
         * @since 1.8.4
         */
        public AccessibilityOptionsHelper setChatOpacity(double val) {
            base.chatOpacity().set(val);
            return this;
        }

        /**
         * @return the current chat line spacing.
         * @since 1.8.4
         */
        public double getChatLineSpacing() {
            return base.chatLineSpacing().get();
        }

        /**
         * @param val the new chat line spacing
         * @return self for chaining.
         * @since 1.8.4
         */
        public AccessibilityOptionsHelper setChatLineSpacing(double val) {
            getBase(base.chatLineSpacing()).forceSetValue(val);
            return this;
        }

        /**
         * @return the current chat delay in seconds.
         * @since 1.8.4
         */
        public double getChatDelay() {
            return base.chatDelay().get();
        }

        /**
         * @param val the new chat delay in seconds
         * @return self for chaining.
         * @since 1.8.4
         */
        public AccessibilityOptionsHelper setChatDelay(double val) {
            getBase(base.chatDelay()).forceSetValue(val);
            return this;
        }

        /**
         * @return {@code true} if auto jump is enabled, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isAutoJumpEnabled() {
            return base.autoJump().get();
        }

        /**
         * @param val whether to enable auto jump
         * @return self for chaining.
         * @since 1.8.4
         */
        public AccessibilityOptionsHelper enableAutoJump(boolean val) {
            base.autoJump().set(val);
            return this;
        }

        /**
         * @return {@code true} if the toggle functionality for sneaking is enabled, {@code false}
         * otherwise.
         * @since 1.8.4
         */
        public boolean isSneakTogglingEnabled() {
            return base.toggleCrouch().get();
        }

        /**
         * @param val whether sneaking uses toggle behavior
         * @return self for chaining.
         * @since 1.8.4
         */
        public AccessibilityOptionsHelper toggleSneak(boolean val) {
            base.toggleCrouch().set(val);
            return this;
        }

        /**
         * @return {@code true} if sprinting uses toggle behavior; {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isSprintTogglingEnabled() {
            return base.toggleSprint().get();
        }

        /**
         * @param val whether sprinting uses toggle behavior
         * @return self for chaining.
         * @since 1.8.4
         */
        public AccessibilityOptionsHelper toggleSprint(boolean val) {
            base.toggleSprint().set(val);
            return this;
        }

        /**
         * @return the current screen distortion effect scale.
         * @since 1.8.4
         */
        public double getDistortionEffect() {
            return base.screenEffectScale().get();
        }

        /**
         * @param val the new screen distortion effect scale
         * @return self for chaining.
         * @since 1.8.4
         */
        public AccessibilityOptionsHelper setDistortionEffect(double val) {
            getBase(base.screenEffectScale()).forceSetValue(val);
            return this;
        }

        /**
         * @return the current FOV effect scale.
         * @since 1.8.4
         */
        public double getFovEffect() {
            return base.fovEffectScale().get();
        }

        /**
         * @param val the new FOV effect scale
         * @return self for chaining.
         * @since 1.8.4
         */
        public AccessibilityOptionsHelper setFovEffect(double val) {
            base.fovEffectScale().set(val);
            return this;
        }

        /**
         * @return {@code true} if the dark Mojang Studios background is enabled; {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isMonochromeLogoEnabled() {
            return base.darkMojangStudiosBackground().get();
        }

        /**
         * @param val whether to enable the dark Mojang Studios background
         * @return the current helper instance for chaining.
         * @since 1.8.4
         */
        public AccessibilityOptionsHelper enableMonochromeLogo(boolean val) {
            base.darkMojangStudiosBackground().set(val);
            return this;
        }

        /**
         * @return {@code true} if lightning flashes are hidden; {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean areLightningFlashesHidden() {
            return base.hideLightningFlash().get();
        }

        /**
         * Sets the FOV effect scale to {@code 1.0} when enabled or {@code 0.0} when disabled.
         * Use {@link #setFovEffect(double)} to set an intermediate scale.
         * @param val whether to enable FOV effects at full scale
         * @return self for chaining.
         * @since 1.8.4
         */
        public AccessibilityOptionsHelper setFovEffect(boolean val) {
            getBase(base.fovEffectScale()).forceSetValue(val ? 1.0 : 0.0);
            return this;
        }

    }

    /**
     * @return {@code 0} when off, {@code 1} for fast, or {@code 2} for fancy clouds
     * @since 1.1.7
     * @deprecated use {@link VideoOptionsHelper#getCloudsMode()} instead.
     */
    @Deprecated
    @DocletIgnore
    @DocletReplaceReturn("Trit")
    public int getCloudMode() {
        switch (base.cloudStatus().get()) {
            case FANCY:
                return 2;
            case FAST:
                return 1;
            default:
                return 0;
        }
    }

    /**
     * @param mode {@code 0} for off, {@code 1} for fast, or {@code 2} for fancy; other values turn clouds off
     * @return this helper for chaining
     * @since 1.1.7
     * @deprecated use {@link VideoOptionsHelper#setCloudsMode(String)} instead.
     */
    @Deprecated
    @DocletIgnore
    @DocletReplaceParams("mode: Trit")
    public OptionsHelper setCloudMode(int mode) {
        switch (mode) {
            case 2:
                base.cloudStatus().set(CloudStatus.FANCY);
                return this;
            case 1:
                base.cloudStatus().set(CloudStatus.FAST);
                return this;
            default:
                base.cloudStatus().set(CloudStatus.OFF);
                return this;
        }
    }

    /**
     * @return {@code 0} for fast, {@code 1} for fancy, {@code 2} for fabulous, or {@code -1} for an unrecognized mode
     * @since 1.1.7
     * @deprecated use {@link VideoOptionsHelper#getGraphicsMode()} instead.
     */
    @Deprecated
    @DocletIgnore
    public int getGraphicsMode() {
        switch (video.getGraphicsMode()) {
            case "fabulous":
                return 2;
            case "fancy":
                return 1;
            case "fast":
                return 0;
            default:
                return -1;
        }
    }

    /**
     * @param mode {@code 0} for fast, {@code 1} for fancy, {@code 2} for fabulous; other values select fast
     * @return this helper for chaining
     * @since 1.1.7
     * @deprecated use {@link VideoOptionsHelper#setGraphicsMode(String)} instead.
     */
    @Deprecated
    @DocletIgnore
    public OptionsHelper setGraphicsMode(int mode) {
        switch (mode) {
            case 2:
                this.getVideoOptions().setGraphicsMode("fabulous");
                return this;
            case 1:
                this.getVideoOptions().setGraphicsMode("fancy");
                return this;
            default:
                this.getVideoOptions().setGraphicsMode("fast");
                return this;
        }
    }

    /**
     * @return {@code true} when the main hand is right, otherwise {@code false}
     * @since 1.1.7
     * @deprecated use {@link SkinOptionsHelper#isRightHanded()} instead.
     */
    @Deprecated
    @DocletIgnore
    public boolean isRightHanded() {
        return base.mainHand().get() == HumanoidArm.RIGHT;
    }

    /**
     * @param val {@code true} to set the main hand to right, {@code false} to set it to left
     * @since 1.1.7
     * @deprecated use {@link SkinOptionsHelper#toggleMainHand(String)} instead.
     */
    @Deprecated
    @DocletIgnore
    public OptionsHelper setRightHanded(boolean val) {
        if (val) {
            base.mainHand().set(HumanoidArm.RIGHT);
        } else {
            base.mainHand().set(HumanoidArm.LEFT);
        }
        return this;
    }

    /**
     * @return the configured render distance in chunks
     * @since 1.1.7
     * @deprecated use {@link VideoOptionsHelper#getRenderDistance()} instead.
     */
    @Deprecated
    @DocletIgnore
    public int getRenderDistance() {
        return base.renderDistance().get();
    }

    /**
     * @param d the render distance in chunks
     * @return this helper for chaining
     * @since 1.1.7
     * @deprecated use {@link VideoOptionsHelper#setRenderDistance(int)} instead.
     */
    @Deprecated
    @DocletIgnore
    public OptionsHelper setRenderDistance(int d) {
        ((MixinSimpleOption) (Object) base.renderDistance()).forceSetValue(d);
        return this;
    }

    /**
     * @return the configured gamma value
     * @since 1.3.0 normal values for gamma are between {@code 0} and {@code 1}
     * @deprecated use {@link VideoOptionsHelper#getGamma()} instead.
     */
    @Deprecated
    @DocletIgnore
    public double getGamma() {
        return base.gamma().get();
    }

    /**
     * Sets the gamma value.
     *
     * @param gamma the gamma value; normal values are between {@code 0} and {@code 1}
     * @return this helper for chaining
     * @since 1.3.0 normal values for gamma are between {@code 0} and {@code 1}
     * @deprecated use {@link VideoOptionsHelper#setGamma(double)} instead.
     */
    @Deprecated
    @DocletIgnore
    public OptionsHelper setGamma(double gamma) {
        ((MixinSimpleOption) (Object) base.gamma()).forceSetValue(gamma);
        return this;
    }

    /**
     * Sets the master volume.
     *
     * @param vol the volume value
     * @return this helper for chaining
     * @since 1.3.1
     * @deprecated use {@link MusicOptionsHelper#setMasterVolume(double)} instead.
     */
    @Deprecated
    @DocletIgnore
    public OptionsHelper setVolume(double vol) {
        base.getSoundSourceOptionInstance(SoundSource.MASTER).set(vol);
        return this;
    }

    /**
     * Sets the volume for a sound category.
     *
     * @param category the sound category name
     * @param volume the volume value
     * @return this helper for chaining
     * @since 1.3.1
     * @deprecated use {@link MusicOptionsHelper#setVolume(String, double)} instead.
     */
    @Deprecated
    @DocletReplaceParams("category: SoundCategory, volume: double")
    @DocletIgnore
    public OptionsHelper setVolume(String category, double volume) {
        base.getSoundSourceOptionInstance(Arrays.stream(SoundSource.values()).filter(e -> e.getName().equals(category)).findFirst().orElseThrow(() -> new IllegalArgumentException("unknown sound category"))).set(volume);
        return this;
    }

    /**
     * Returns the configured volume for every sound category, keyed by category name.
     * @return a map from sound-category names to their configured volumes
     * @since 1.3.1
     * @deprecated use {@link MusicOptionsHelper#getVolumes()} instead.
     */
    @Deprecated
    @DocletReplaceReturn("JavaMap<SoundCategory, float>")
    @DocletIgnore
    public Map<String, Float> getVolumes() {
        Map<String, Float> volumes = new HashMap<>();
        for (SoundSource category : SoundSource.values()) {
            volumes.put(category.getName(), getSoundSourceVolume(category));
        }
        return volumes;
    }

    /**
     * Sets the GUI scale; {@code 0} selects automatic scaling.
     *
     * @param scale the GUI scale, or {@code 0} for automatic scaling
     * @return this helper for chaining
     * @since 1.3.1
     * @deprecated use {@link VideoOptionsHelper#setGuiScale(int)} instead.
     */
    @Deprecated
    @DocletIgnore
    public OptionsHelper setGuiScale(int scale) {
        base.guiScale().set(scale);
        //? if >=26.1 {
        /*mc.execute(mc::resizeGui);
        *///? } else {
        mc.execute(mc::resizeDisplay);
        //? }
        return this;
    }

    /**
     * @return the configured GUI scale; {@code 0} means automatic scaling
     * @since 1.3.1
     * @deprecated use {@link VideoOptionsHelper#getGuiScale()} instead.
     */
    @Deprecated
    @DocletIgnore
    public int getGuiScale() {
        return base.guiScale().get();
    }

    /**
     * Returns the configured volume for a sound category. The name must be one of {@code master},
     * {@code music}, {@code record}, {@code weather}, {@code block}, {@code hostile},
     * {@code neutral}, {@code player}, {@code ambient}, {@code voice}, or {@code ui}.
     *
     * @param category a sound category name
     * @return the configured volume
     * @since 1.3.1
     * @deprecated use {@link MusicOptionsHelper#getVolume(String)} instead.
     */
    @Deprecated
    @DocletReplaceParams("category: SoundCategory")
    @DocletIgnore
    public float getVolume(String category) {
        return getSoundSourceVolume(SOUND_CATEGORY_MAP.get(category));
    }

}
