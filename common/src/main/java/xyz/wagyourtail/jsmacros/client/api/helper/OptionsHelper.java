package xyz.wagyourtail.jsmacros.client.api.helper;

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
import org.lwjgl.glfw.GLFW;
import xyz.wagyourtail.doclet.DocletDeclareType;
import xyz.wagyourtail.doclet.DocletReplaceParams;
import xyz.wagyourtail.doclet.DocletReplaceReturn;
import xyz.wagyourtail.jsmacros.client.access.IResourcePackManager;
import xyz.wagyourtail.jsmacros.client.mixin.access.MixinSimpleOption;
import xyz.wagyourtail.jsmacros.core.helpers.BaseHelper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//? if >1.21.8 {
/*import net.minecraft.resources.ResourceLocation;
*///?}

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class OptionsHelper extends BaseHelper<Options> {

    private static final Map<String, SoundSource> SOUND_CATEGORY_MAP = Arrays.stream(SoundSource.values()).collect(Collectors.toMap(SoundSource::getName, Function.identity()));
    private final Minecraft mc = Minecraft.getInstance();
    private final PackRepository rpm = mc.getResourcePackRepository();

    public final SkinOptionsHelper skin = new SkinOptionsHelper(this);
    public final VideoOptionsHelper video = new VideoOptionsHelper(this);
    public final MusicOptionsHelper music = new MusicOptionsHelper(this);
    public final ControlOptionsHelper control = new ControlOptionsHelper(this);
    public final ChatOptionsHelper chat = new ChatOptionsHelper(this);
    public final AccessibilityOptionsHelper accessibility = new AccessibilityOptionsHelper(this);

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
     * @return a helper for the skin options.
     * @since 1.8.4
     */
    public SkinOptionsHelper getSkinOptions() {
        return skin;
    }

    /**
     * @return a helper for the video options.
     * @since 1.8.4
     */
    public VideoOptionsHelper getVideoOptions() {
        return video;
    }

    /**
     * @return a helper for the music options.
     * @since 1.8.4
     */
    public MusicOptionsHelper getMusicOptions() {
        return music;
    }

    /**
     * @return a helper for the control options.
     * @since 1.8.4
     */
    public ControlOptionsHelper getControlOptions() {
        return control;
    }

    /**
     * @return a helper for the chat options.
     * @since 1.8.4
     */
    public ChatOptionsHelper getChatOptions() {
        return chat;
    }

    /**
     * @return a helper for the accessibility options.
     * @since 1.8.4
     */
    public AccessibilityOptionsHelper getAccessibilityOptions() {
        return accessibility;
    }

    /**
     * @return self for chaining.
     * @since 1.8.4
     */
    public OptionsHelper saveOptions() {
        base.save();
        return this;
    }

    /**
     * @return list of names of resource packs.
     * @since 1.1.7
     */
    public List<String> getResourcePacks() {
        return new ArrayList<>(rpm.getAvailableIds());
    }

    /**
     * @return list of names of enabled resource packs.
     * @since 1.2.0
     */
    public List<String> getEnabledResourcePacks() {
        return new ArrayList<>(rpm.getSelectedIds());
    }

    /**
     * Set the enabled resource packs to the provided list.
     *
     * @param enabled
     * @return self for chaining.
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
     * @param state false to put it back
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
     * @return the active language.
     * @since 1.8.4
     */
    @DocletReplaceReturn("Locale")
    public String getLanguage() {
        return base.languageCode;
    }

    /**
     * @param languageCode the language to change to
     * @return self for chaining.
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
     * @return the active difficulty.
     * @since 1.8.4
     */
    @DocletReplaceReturn("Difficulty")
    public String getDifficulty() {
        return mc.level.getDifficulty().getKey();
    }

    /**
     * The name be either "peaceful", "easy", "normal", or "hard".
     *
     * @param name the name of the difficulty to change to
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
     * @return {@code true} if the difficulty is locked, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isDifficultyLocked() {
        return Minecraft.getInstance().level.getLevelData().isDifficultyLocked();
    }

    /**
     * @return self for chaining.
     * @since 1.8.4
     */
    public OptionsHelper lockDifficulty() {
        Minecraft.getInstance().getConnection().send(new ServerboundLockDifficultyPacket(true));
        return this;
    }

    /**
     * Unlocks the difficulty of the world. This can't be done in an unmodified client.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public OptionsHelper unlockDifficulty() {
        Minecraft.getInstance().getConnection().send(new ServerboundLockDifficultyPacket(false));
        return this;
    }

    /**
     * @return the current fov value.
     * @since 1.1.7
     */
    public int getFov() {
        return base.fov().get();
    }

    /**
     * @param fov the new fov value
     * @return self for chaining.
     * @since 1.1.7
     */
    public OptionsHelper setFov(int fov) {
        getBase(base.fov()).forceSetValue(fov);
        return this;
    }

    /**
     * @return 0 for 1st person, 2 for in front.
     * @since 1.5.0
     */
    @DocletReplaceReturn("Trit")
    public int getCameraMode() {
        return base.getCameraType().ordinal();
    }

    /**
     * @param mode 0: first, 2: front
     * @since 1.5.0
     */
    @DocletReplaceParams("mode: Trit")
    public OptionsHelper setCameraMode(int mode) {
        base.setCameraType(CameraType.values()[mode]);
        return this;
    }

    /**
     * @return
     * @since 1.5.0
     */
    public boolean getSmoothCamera() {
        return base.smoothCamera;
    }

    /**
     * @param val
     * @since 1.5.0
     */
    public OptionsHelper setSmoothCamera(boolean val) {
        base.smoothCamera = val;
        return this;
    }

    /**
     * @return
     * @since 1.2.6
     */
    public int getWidth() {
        return mc.getWindow().getScreenWidth();
    }

    /**
     * @return
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
     * @param w
     * @since 1.2.6
     */
    public OptionsHelper setWidth(int w) {
        Window win = mc.getWindow();
        GLFW.glfwSetWindowSize(getWindowHandle(), w, win.getScreenHeight());
        return this;
    }

    /**
     * @param h
     * @since 1.2.6
     */
    public OptionsHelper setHeight(int h) {
        Window win = mc.getWindow();
        GLFW.glfwSetWindowSize(getWindowHandle(), win.getScreenWidth(), h);
        return this;
    }

    /**
     * @param w
     * @param h
     * @since 1.2.6
     */
    public OptionsHelper setSize(int w, int h) {
        Window win = mc.getWindow();
        GLFW.glfwSetWindowSize(getWindowHandle(), w, h);
        return this;
    }


    /**
     * sends the synced options to the server.
     * this may be necessary for certain things like the skin layers and (serverside) language to update.
     * <br>
     * normally this is called when the options screen is closed, but obviously, we don't open a screen, thus
     * this must get called manually.
     *
     * @since 2.0.0
     */
    public void sendSyncedOptions() {
        base.broadcastOptions();
    }

    private MixinSimpleOption getBase(OptionInstance<?> option) {
        return (MixinSimpleOption) (Object) option;
    }

    public class SkinOptionsHelper {

        public final OptionsHelper parent;

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
         * @param val whether the left sleeve should be shown or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public SkinOptionsHelper toggleLeftSleeve(boolean val) {
            base.setModelPart(PlayerModelPart.LEFT_SLEEVE, val);
            return this;
        }

        /**
         * @param val whether the right sleeve should be shown or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public SkinOptionsHelper toggleRightSleeve(boolean val) {
            base.setModelPart(PlayerModelPart.RIGHT_SLEEVE, val);
            return this;
        }

        /**
         * @param val whether the left pants should be shown or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public SkinOptionsHelper toggleLeftPants(boolean val) {
            base.setModelPart(PlayerModelPart.LEFT_PANTS_LEG, val);
            return this;
        }

        /**
         * @param val whether the right pants should be shown or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public SkinOptionsHelper toggleRightPants(boolean val) {
            base.setModelPart(PlayerModelPart.RIGHT_PANTS_LEG, val);
            return this;
        }

        /**
         * @param val whether the hat should be shown or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public SkinOptionsHelper toggleHat(boolean val) {
            base.setModelPart(PlayerModelPart.HAT, val);
            return this;
        }

        /**
         * The hand must be either {@code "left"} or {@code "right"}.
         *
         * @param hand the hand to set as main hand
         * @return self for chaining.
         * @since 1.8.4
         */
        public SkinOptionsHelper toggleMainHand(String hand) {
            base.mainHand().set(hand.toLowerCase(Locale.ROOT).equals("left") ? HumanoidArm.LEFT : HumanoidArm.RIGHT);
            return this;
        }

    }

    public class VideoOptionsHelper {

        public final OptionsHelper parent;

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
         * @return the full screen resolution as a string.
         * @since 1.8.4
         */
        public String getFullscreenResolution() {
            return base.fullscreenVideoModeString;
        }

        /**
         * @return the current biome blend radius.
         * @since 1.8.4
         */
        public int getBiomeBlendRadius() {
            return base.biomeBlendRadius().get();
        }

        /**
         * @param radius the new biome blend radius
         * @return self for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper setBiomeBlendRadius(int radius) {
            getBase(base.biomeBlendRadius()).forceSetValue(radius);
            return this;
        }

        /**
         * @return the selected graphics mode.
         * @since 1.8.4
         */
        @DocletReplaceReturn("GraphicsMode")
        public String getGraphicsMode() {
            switch (base.graphicsMode().get()) {
                case FAST:
                    return "fast";
                case FANCY:
                    return "fancy";
                case FABULOUS:
                    return "fabulous";
                default:
                    throw new IllegalArgumentException();
            }
        }

        /**
         * @param mode the graphics mode to select. Must be either "fast", "fancy" or "fabulous"
         * @return self for chaining.
         * @since 1.8.4
         */
        @DocletReplaceParams("mode: GraphicsMode")
        @DocletDeclareType(name = "GraphicsMode", type = "'fast' | 'fancy' | 'fabulous'")
        public VideoOptionsHelper setGraphicsMode(String mode) {
            GraphicsStatus newMode;
            switch (mode.toUpperCase(Locale.ROOT)) {
                case "FAST":
                    newMode = GraphicsStatus.FAST;
                    break;
                case "FANCY":
                    newMode = GraphicsStatus.FANCY;
                    break;
                case "FABULOUS":
                    newMode = GraphicsStatus.FABULOUS;
                    break;
                default:
                    newMode = base.graphicsMode().get();
                    break;
            }
            base.graphicsMode().set(newMode);
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
         * @param mode the chunk builder mode to select. Must be either "none", "nearby" or
         *             "player_affected"
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
         * @return the selected smooth lightning mode.
         * @since 1.8.4
         */
        public boolean getSmoothLightningMode() {
            return base.ambientOcclusion().get();
        }

        /**
         * @param mode the smooth lightning mode to select. boolean value
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
         * @param radius the new render distance in chunks
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
         * @param radius the new simulation distance in chunks
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
         * @param maxFps the new maximum fps limit
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
         * @param val whether to enable vsync or not
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
         * @param val whether to enable view bobbing or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper enableViewBobbing(boolean val) {
            base.bobView().set(val);
            return this;
        }

        /**
         * @return the current gui scale.
         * @since 1.8.4
         */
        public int getGuiScale() {
            return base.guiScale().get();
        }

        /**
         * @param scale the gui scale to set. Must be 1, 2, 3 or 4
         * @return self for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper setGuiScale(int scale) {
            base.guiScale().set(scale);
            mc.execute(mc::resizeDisplay);
            return this;
        }

        /**
         * @return the current attack indicator type.
         * @since 1.8.4
         */
        @DocletReplaceReturn("AttackIndicatorType")
        public String getAttackIndicatorType() {
            switch (base.attackIndicator().get()) {
                case OFF:
                    return "off";
                case CROSSHAIR:
                    return "crosshair";
                case HOTBAR:
                    return "hotbar";
                default:
                    throw new IllegalArgumentException();
            }
        }

        /**
         * @param type the attack indicator type. Must be either "off", "crosshair", or "hotbar"
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
         * @return the current gamma value.
         * @since 1.8.4
         */
        public double getGamma() {
            return getBrightness();
        }

        /**
         * @param gamma the new gamma value
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
         * @param gamma the new brightness value
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
         * @param mode the cloud rendering mode to select. Must be either "off", "fast" or "fancy"
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
         * @param fullscreen whether to enable fullscreen mode or not
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
         * @param mode the particle rendering mode to select. Must be either "minimal", "decreased"
         *             or "all"
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
         * @param val the new mip map level
         * @return self for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper setMipMapLevels(int val) {
            base.mipmapLevels().set(val);
            return this;
        }

        /**
         * @return {@code true} if entity shadows should be rendered, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean areEntityShadowsEnabled() {
            return base.entityShadows().get();
        }

        /**
         * @param val whether to enable entity shadows or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper enableEntityShadows(boolean val) {
            base.entityShadows().set(val);
            return this;
        }

        /**
         * @return the current distortion effect scale.
         * @since 1.8.4
         */
        public double getDistortionEffect() {
            return base.screenEffectScale().get();
        }

        /**
         * @param val the new distortion effect scale
         * @return self for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper setDistortionEffects(double val) {
            base.screenEffectScale().set(val);
            return this;
        }

        /**
         * @return the current entity render distance.
         * @since 1.8.4
         */
        public double getEntityDistance() {
            return base.entityDistanceScaling().get();
        }

        /**
         * @param val the new entity render distance
         * @return self for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper setEntityDistance(double val) {
            base.entityDistanceScaling().set(val);
            return this;
        }

        /**
         * @return the current fov value.
         * @since 1.8.4
         */
        public double getFovEffects() {
            return base.fovEffectScale().get();
        }

        /**
         * @param val the new fov value
         * @return self for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper setFovEffects(double val) {
            getBase(base.fovEffectScale()).forceSetValue(val);
            return this;
        }

        /**
         * @return {@code true} if the autosave indicator is enabled, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isAutosaveIndicatorEnabled() {
            return base.showAutosaveIndicator().get();
        }

        /**
         * @return self for chaining.
         * @since 1.8.4
         */
        public VideoOptionsHelper enableAutosaveIndicator(boolean val) {
            base.showAutosaveIndicator().set(val);
            return this;
        }

    }

    public class MusicOptionsHelper {

        public final OptionsHelper parent;

        public MusicOptionsHelper(OptionsHelper OptionsHelper) {
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
         * @return the current master volume.
         * @since 1.8.4
         */
        public float getMasterVolume() {
            return getSoundSourceVolume(SoundSource.MASTER);
        }

        /**
         * @param volume the new master volume
         * @return self for chaining.
         * @since 1.8.4
         */
        public MusicOptionsHelper setMasterVolume(double volume) {
            base.getSoundSourceOptionInstance(SoundSource.MASTER).set(volume);
            return this;
        }

        /**
         * @return the current music volume.
         * @since 1.8.4
         */
        public float getMusicVolume() {
            return getSoundSourceVolume(SoundSource.MUSIC);
        }

        /**
         * @param volume the new music volume
         * @return self for chaining.
         * @since 1.8.4
         */
        public MusicOptionsHelper setMusicVolume(double volume) {
            base.getSoundSourceOptionInstance(SoundSource.MUSIC).set(volume);
            return this;
        }

        /**
         * @return the current value of played recods.
         * @since 1.8.4
         */
        public float getRecordsVolume() {
            return getSoundSourceVolume(SoundSource.RECORDS);
        }

        /**
         * @param volume the new volume for playing records
         * @return self for chaining.
         * @since 1.8.4
         */
        public MusicOptionsHelper setRecordsVolume(double volume) {
            base.getSoundSourceOptionInstance(SoundSource.RECORDS).set(volume);
            return this;
        }

        /**
         * @return the current volume of the weather.
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
         * @param volume the new ambient volume
         * @return self for chaining.
         * @since 1.8.4
         */
        public MusicOptionsHelper setAmbientVolume(double volume) {
            base.getSoundSourceOptionInstance(SoundSource.AMBIENT).set(volume);
            return this;
        }

        /**
         * @return the current voice volume.
         * @since 1.8.4
         */
        public float getVoiceVolume() {
            return getSoundSourceVolume(SoundSource.VOICE);
        }

        /**
         * @return self for chaining.
         * @since 1.8.4
         */
        public MusicOptionsHelper setVoiceVolume(double volume) {
            base.getSoundSourceOptionInstance(SoundSource.VOICE).set(volume);
            return this;
        }

        /**
         * @param category the category to get the volume of
         * @return the volume of the given sound category.
         * @since 1.8.4
         */
        @DocletReplaceParams("category: SoundCategory")
        public float getVolume(String category) {
            return getSoundSourceVolume(SOUND_CATEGORY_MAP.get(category));
        }

        /**
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
         * @return the currently selected sound device.
         * @since 1.8.4
         */
        public String getSoundDevice() {
            return base.soundDevice().get();
        }

        /**
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
         * @return a list of all connected audio devices.
         * @since 1.8.4
         */
        public List<String> getAudioDevices() {
            return Stream.concat(Stream.of(""), Minecraft.getInstance().getSoundManager().getAvailableSoundDevices().stream()).collect(Collectors.toList());
        }

        /**
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

    public class ControlOptionsHelper {

        public final OptionsHelper parent;

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
         * @param val whether to enable discrete scrolling or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public ControlOptionsHelper enableDiscreteScrolling(boolean val) {
            base.discreteMouseScroll().set(val);
            return this;
        }

        /**
         * @return {@code true} if touchscreen mode is enabled, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isTouchscreenEnabled() {
            return base.touchscreen().get();
        }

        /**
         * @param val whether to enable touchscreen mode or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public ControlOptionsHelper enableTouchscreen(boolean val) {
            base.touchscreen().set(val);
            return this;
        }

        /**
         * Raw input is directly reading the mouse data, without any adjustments due to other
         * programs or the operating system.
         *
         * @return {@code true} if raw mouse input is enabled, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isRawMouseInputEnabled() {
            return base.rawMouseInput().get();
        }

        /**
         * @param val whether to enable raw mouse input or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public ControlOptionsHelper enableRawMouseInput(boolean val) {
            base.rawMouseInput().set(val);
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
         * @param val whether to enable auto jump or not or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public ControlOptionsHelper enableAutoJump(boolean val) {
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
         * @param val whether to enable or disable the toggle functionality for sneaking
         * @return self for chaining.
         * @since 1.8.4
         */
        public ControlOptionsHelper toggleSneak(boolean val) {
            base.toggleCrouch().set(val);
            return this;
        }

        /**
         * @return {@code true} if the toggle functionality for sprinting is enabled, {@code false}
         * otherwise.
         * @since 1.8.4
         */
        public boolean isSprintTogglingEnabled() {
            return base.toggleSprint().get();
        }

        /**
         * @param val whether to enable or disable the toggle functionality for sprinting
         * @return self for chaining.
         * @since 1.8.4
         */
        public ControlOptionsHelper toggleSprint(boolean val) {
            base.toggleSprint().set(val);
            return this;
        }

        /**
         * @return an array of all raw minecraft keybindings.
         * @since 1.8.4
         */
        public KeyMapping[] getRawKeys() {
            return ArrayUtils.clone(base.keyMappings);
        }

        /**
         * @return a list of all keybinding categories.
         * @since 1.8.4
         */
        @DocletReplaceReturn("JavaList<KeyCategory>")
        public List<String> getCategories() {
            return Arrays.stream(base.keyMappings)
                    .map(KeyMapping::getCategory)
                    //? if >1.21.8 {
                    /*.map(KeyMapping.Category::id)
                    .map(ResourceLocation::toString)
                    *///?}
                    .distinct()
                    .collect(Collectors.toList());
        }

        /**
         * @return a list of all key names.
         * @since 1.8.4
         */
        @DocletReplaceReturn("JavaList<Key>")
        public List<String> getKeys() {
            return Arrays.stream(base.keyMappings).map(KeyMapping::getName).collect(Collectors.toList());
        }

        /**
         * @return a map of all keybindings and their bound key.
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
         * @param category the category to get keybindings from
         * @return a map of all keybindings and their bound key in the specified category.
         * @since 1.8.4
         */
        @DocletReplaceParams("category: KeyCategory")
        public Map<String, String> getKeyBindsByCategory(String category) {
            return getKeyBindsByCategory().get(category);
        }

        /**
         * @return a map of all keybinding categories, containing a map of all keybindings in that
         * category and their bound key.
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

    public class ChatOptionsHelper {

        public final OptionsHelper parent;

        public ChatOptionsHelper(OptionsHelper OptionsHelper) {
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
         * @return the current chat visibility mode.
         * @since 1.8.4
         */
        @DocletReplaceReturn("ChatVisibility")
        public String getChatVisibility() {
            String chatVisibilityKey = base.chatVisibility().get().getKey();
            return chatVisibilityKey.substring(chatVisibilityKey.lastIndexOf('.'));
        }

        /**
         * @param mode the new chat visibility mode. Must be "FULL", "SYSTEM" or "HIDDEN
         * @return self for chaining.
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
         * @return {@code true} if messages can use color codes, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean areColorsShown() {
            return base.chatColors().get();
        }

        /**
         * @param val whether to allow color codes in messages or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public ChatOptionsHelper setShowColors(boolean val) {
            base.chatColors().set(val);
            return this;
        }

        /**
         * @return {@code true} if it's allowed to open web links from chat, {@code false}
         * otherwise.
         * @since 1.8.4
         */
        public boolean areWebLinksEnabled() {
            return base.chatLinks().get();
        }

        /**
         * @param val whether to allow opening web links from chat or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public ChatOptionsHelper enableWebLinks(boolean val) {
            base.chatLinks().set(val);
            return this;
        }

        /**
         * @return {@code true} if a warning prompt before opening links should be shown,
         * {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isWebLinkPromptEnabled() {
            return base.chatLinksPrompt().get();
        }

        /**
         * @param val whether to show warning prompts before opening links or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public ChatOptionsHelper enableWebLinkPrompt(boolean val) {
            base.chatLinksPrompt().set(val);
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
        public ChatOptionsHelper setChatOpacity(double val) {
            base.chatOpacity().set(val);
            return this;
        }

        /**
         * @param val the new background opacity for text
         * @return self for chaining.
         * @since 1.8.4
         */
        public ChatOptionsHelper setTextBackgroundOpacity(double val) {
            getBase(base.textBackgroundOpacity()).forceSetValue(val);
            return this;
        }

        /**
         * @return the current background opacity of text.
         * @since 1.8.4
         */
        public double getTextBackgroundOpacity() {
            return base.textBackgroundOpacity().get();
        }

        /**
         * @return the current text size.
         * @since 1.8.4
         */
        public double getTextSize() {
            return base.chatScale().get();
        }

        /**
         * @param val the new text size
         * @return self for chaining.
         * @since 1.8.4
         */
        public ChatOptionsHelper setTextSize(double val) {
            getBase(base.chatScale()).forceSetValue(val);
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
        public ChatOptionsHelper setChatLineSpacing(double val) {
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
        public ChatOptionsHelper setChatDelay(double val) {
            base.chatDelay().set(val);
            return this;
        }

        /**
         * @return the current chat width.
         * @since 1.8.4
         */
        public double getChatWidth() {
            return base.chatWidth().get();
        }

        /**
         * @param val the new chat width
         * @return self for chaining.
         * @since 1.8.4
         */
        public ChatOptionsHelper setChatWidth(double val) {
            getBase(base.chatWidth()).forceSetValue(val);
            return this;
        }

        /**
         * @return the focused chat height.
         * @since 1.8.4
         */
        public double getChatFocusedHeight() {
            return base.chatHeightFocused().get();
        }

        /**
         * @param val the new focused chat height
         * @return self for chaining.
         * @since 1.8.4
         */
        public ChatOptionsHelper setChatFocusedHeight(double val) {
            getBase(base.chatHeightFocused()).forceSetValue(val);
            return this;
        }

        /**
         * @return the unfocused chat height.
         * @since 1.8.4
         */
        public double getChatUnfocusedHeight() {
            return base.chatHeightUnfocused().get();
        }

        /**
         * @param val the new unfocused chat height
         * @return self for chaining.
         * @since 1.8.4
         */
        public ChatOptionsHelper setChatUnfocusedHeight(double val) {
            getBase(base.chatHeightUnfocused()).forceSetValue(val);
            return this;
        }

        /**
         * @return the current narrator mode.
         * @since 1.8.4
         */
        @DocletReplaceReturn("NarratorMode")
        public String getNarratorMode() {
            String narratorKey = ((TranslatableContents) (base.narrator().get().getName().getContents())).getKey();
            return narratorKey.substring(narratorKey.lastIndexOf('.'));
        }

        /**
         * @param mode the mode to set the narrator to. Must be either "OFF", "ALL", "CHAT", or
         *             "SYSTEM"
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
         * @return {@code true} if command suggestions are enabled
         * @since 1.8.4
         */
        public boolean areCommandSuggestionsEnabled() {
            return base.autoSuggestions().get();
        }

        /**
         * @param val whether to enable command suggestions or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public ChatOptionsHelper enableCommandSuggestions(boolean val) {
            base.autoSuggestions().set(val);
            return this;
        }

        /**
         * @return {@code true} if messages from blocked users are hidden.
         * @since 1.8.4
         */
        public boolean areMatchedNamesHidden() {
            return base.hideMatchedNames().get();
        }

        /**
         * @param val whether to hide messages of blocked users or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public ChatOptionsHelper enableHideMatchedNames(boolean val) {
            base.hideMatchedNames().set(val);
            return this;
        }

        /**
         * @return {@code true} if reduced debug info is enabled, {@code false} otherwise.
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

    public class AccessibilityOptionsHelper {

        public final OptionsHelper parent;

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
         * @param mode the mode to set the narrator to. Must be either "OFF", "ALL", "CHAT", or
         *             "SYSTEM"
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
         * @return
         * @since 1.8.4
         */
        public boolean isBackgroundForChatOnly() {
            return base.backgroundForChatOnly().get();
        }

        /**
         * @param val
         * @return self for chaining.
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
         * @param val whether to enable auto jump or not or not
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
         * @param val whether to enable or disable the toggle functionality for sneaking
         * @return self for chaining.
         * @since 1.8.4
         */
        public AccessibilityOptionsHelper toggleSneak(boolean val) {
            base.toggleCrouch().set(val);
            return this;
        }

        /**
         * @return {@code true} if the toggle functionality for sprinting is enabled, {@code false}
         * otherwise.
         * @since 1.8.4
         */
        public boolean isSprintTogglingEnabled() {
            return base.toggleSprint().get();
        }

        /**
         * @param val whether to enable or disable the toggle functionality for sprinting
         * @return self for chaining.
         * @since 1.8.4
         */
        public AccessibilityOptionsHelper toggleSprint(boolean val) {
            base.toggleSprint().set(val);
            return this;
        }

        /**
         * @return the current distortion effect scale.
         * @since 1.8.4
         */
        public double getDistortionEffect() {
            return base.screenEffectScale().get();
        }

        /**
         * @param val the new distortion effect scale
         * @return self for chaining.
         * @since 1.8.4
         */
        public AccessibilityOptionsHelper setDistortionEffect(double val) {
            getBase(base.screenEffectScale()).forceSetValue(val);
            return this;
        }

        /**
         * @return the current fov effect scale.
         * @since 1.8.4
         */
        public double getFovEffect() {
            return base.fovEffectScale().get();
        }

        /**
         * @param val the new fov effect scale
         * @return self for chaining.
         * @since 1.8.4
         */
        public AccessibilityOptionsHelper setFovEffect(double val) {
            base.fovEffectScale().set(val);
            return this;
        }

        /**
         * @return {@code true} if the monochrome logo is enabled, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isMonochromeLogoEnabled() {
            return base.darkMojangStudiosBackground().get();
        }

        /**
         * @param val whether to enable the monochrome logo or not
         * @return the current helper instance for chaining.
         * @since 1.8.4
         */
        public AccessibilityOptionsHelper enableMonochromeLogo(boolean val) {
            base.darkMojangStudiosBackground().set(val);
            return this;
        }

        /**
         * @return {@code true} if lighting flashes are hidden, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean areLightningFlashesHidden() {
            return base.hideLightningFlash().get();
        }

        /**
         * @param val the new fov value
         * @return self for chaining.
         * @since 1.8.4
         */
        public AccessibilityOptionsHelper setFovEffect(boolean val) {
            getBase(base.hideLightningFlash()).forceSetValue(val);
            return this;
        }

    }

    /**
     * @return 0: off, 2: fancy
     * @since 1.1.7
     * @deprecated use {@link VideoOptionsHelper#getCloudsMode()} instead.
     */
    @Deprecated
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
     * @param mode 0: off, 2: fancy
     * @return
     * @since 1.1.7
     * @deprecated use {@link VideoOptionsHelper#setCloudsMode(String)} instead.
     */
    @Deprecated
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
     * @return
     * @since 1.1.7
     * @deprecated use {@link VideoOptionsHelper#getGraphicsMode()} instead.
     */
    @Deprecated
    public int getGraphicsMode() {
        switch (base.graphicsMode().get()) {
            case FABULOUS:
                return 2;
            case FANCY:
                return 1;
            default:
                return 0;
        }
    }

    /**
     * @param mode 0: fast, 2: fabulous
     * @return
     * @since 1.1.7
     * @deprecated use {@link VideoOptionsHelper#setGraphicsMode(String)} instead.
     */
    @Deprecated
    public OptionsHelper setGraphicsMode(int mode) {
        switch (mode) {
            case 2:
                base.graphicsMode().set(GraphicsStatus.FABULOUS);
                return this;
            case 1:
                base.graphicsMode().set(GraphicsStatus.FANCY);
                return this;
            default:
                base.graphicsMode().set(GraphicsStatus.FAST);
                return this;
        }
    }

    /**
     * @return
     * @since 1.1.7
     * @deprecated use {@link SkinOptionsHelper#isRightHanded()} instead.
     */
    @Deprecated
    public boolean isRightHanded() {
        return base.mainHand().get() == HumanoidArm.RIGHT;
    }

    /**
     * @param val
     * @since 1.1.7
     * @deprecated use {@link SkinOptionsHelper#toggleMainHand(String)} instead.
     */
    @Deprecated
    public OptionsHelper setRightHanded(boolean val) {
        if (val) {
            base.mainHand().set(HumanoidArm.RIGHT);
        } else {
            base.mainHand().set(HumanoidArm.LEFT);
        }
        return this;
    }

    /**
     * @return
     * @since 1.1.7
     * @deprecated use {@link VideoOptionsHelper#getRenderDistance()} instead.
     */
    @Deprecated
    public int getRenderDistance() {
        return base.renderDistance().get();
    }

    /**
     * @param d
     * @since 1.1.7
     * @deprecated use {@link VideoOptionsHelper#setRenderDistance(int)} instead.
     */
    @Deprecated
    public OptionsHelper setRenderDistance(int d) {
        ((MixinSimpleOption) (Object) base.renderDistance()).forceSetValue(d);
        return this;
    }

    /**
     * @since 1.3.0 normal values for gamma are between {@code 0} and {@code 1}
     * @deprecated use {@link VideoOptionsHelper#getGamma()} instead.
     */
    @Deprecated
    public double getGamma() {
        return base.gamma().get();
    }

    /**
     * @since 1.3.0 normal values for gamma are between {@code 0} and {@code 1}
     * @deprecated use {@link VideoOptionsHelper#setGamma(double)} instead.
     */
    @Deprecated
    public OptionsHelper setGamma(double gamma) {
        ((MixinSimpleOption) (Object) base.gamma()).forceSetValue(gamma);
        return this;
    }

    /**
     * @param vol
     * @since 1.3.1
     * @deprecated use {@link MusicOptionsHelper#setMasterVolume(double)} instead.
     */
    @Deprecated
    public OptionsHelper setVolume(double vol) {
        base.getSoundSourceOptionInstance(SoundSource.MASTER).set(vol);
        return this;
    }

    /**
     * set volume by category.
     *
     * @param category
     * @param volume
     * @since 1.3.1
     * @deprecated use {@link MusicOptionsHelper#setVolume(String, double)} instead.
     */
    @Deprecated
    @DocletReplaceParams("category: SoundCategory, volume: double")
    public OptionsHelper setVolume(String category, double volume) {
        base.getSoundSourceOptionInstance(Arrays.stream(SoundSource.values()).filter(e -> e.getName().equals(category)).findFirst().orElseThrow(() -> new IllegalArgumentException("unknown sound category"))).set(volume);
        return this;
    }

    /**
     * @return
     * @since 1.3.1
     * @deprecated use {@link MusicOptionsHelper#getVolumes()} instead.
     */
    @Deprecated
    @DocletReplaceReturn("JavaMap<SoundCategory, float>")
    public Map<String, Float> getVolumes() {
        Map<String, Float> volumes = new HashMap<>();
        for (SoundSource category : SoundSource.values()) {
            volumes.put(category.getName(), getSoundSourceVolume(category));
        }
        return volumes;
    }

    /**
     * sets gui scale, {@code 0} for auto.
     *
     * @param scale
     * @since 1.3.1
     * @deprecated use {@link VideoOptionsHelper#setGuiScale(int)} instead.
     */
    @Deprecated
    public OptionsHelper setGuiScale(int scale) {
        base.guiScale().set(scale);
        mc.execute(mc::resizeDisplay);
        return this;
    }

    /**
     * @return gui scale, {@code 0} for auto.
     * @since 1.3.1
     * @deprecated use {@link VideoOptionsHelper#getGuiScale()} instead.
     */
    @Deprecated
    public int getGuiScale() {
        return base.guiScale().get();
    }

    /**
     * @param category
     * @return
     * @since 1.3.1
     * @deprecated use {@link MusicOptionsHelper#getVolume(String)} instead.
     */
    @Deprecated
    @DocletReplaceParams("category: SoundCategory")
    public float getVolume(String category) {
        return getSoundSourceVolume(SOUND_CATEGORY_MAP.get(category));
    }

}
