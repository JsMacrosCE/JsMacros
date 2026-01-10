package xyz.wagyourtail.jsmacros.client.api.library.impl;

import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jetbrains.annotations.Nullable;
import xyz.wagyourtail.doclet.DocletReplaceParams;
import xyz.wagyourtail.doclet.DocletReplaceReturn;
import xyz.wagyourtail.jsmacros.api.helper.ModContainerHelper;
import xyz.wagyourtail.jsmacros.client.JsMacros;
import xyz.wagyourtail.jsmacros.client.api.classes.RegistryHelper;
import xyz.wagyourtail.jsmacros.client.api.helper.OptionsHelper;
import xyz.wagyourtail.jsmacros.client.api.helper.PacketByteBufferHelper;
import xyz.wagyourtail.jsmacros.client.api.helper.inventory.ItemHelper;
import xyz.wagyourtail.jsmacros.client.api.helper.world.BlockHelper;
import xyz.wagyourtail.jsmacros.client.api.helper.world.ServerInfoHelper;
import xyz.wagyourtail.jsmacros.client.tick.TickBasedEvents;
import xyz.wagyourtail.jsmacros.client.tick.TickSync;
import xyz.wagyourtail.jsmacros.core.EventLockWatchdog;
import xyz.wagyourtail.jsmacros.core.MethodWrapper;
import xyz.wagyourtail.jsmacros.core.config.CoreConfigV2;
import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.event.IEventListener;
import xyz.wagyourtail.jsmacros.core.language.BaseScriptContext;
import xyz.wagyourtail.jsmacros.core.language.EventContainer;
import xyz.wagyourtail.jsmacros.core.library.Library;
import xyz.wagyourtail.jsmacros.core.library.PerExecLibrary;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

//? if >1.21.8 {
/*import net.minecraft.ChatFormatting;
import net.minecraft.client.CloudStatus;
import java.util.Locale;
*///?}

/**
 * Functions that interact with minecraft that don't fit into their own module.
 * <p>
 * An instance of this class is passed to scripts as the {@code Client} variable.
 *
 * @author Wagyourtail
 * @since 1.2.9
 */
@Library("Client")
@SuppressWarnings("unused")
public class FClient extends PerExecLibrary {
    private static final Minecraft mc = Minecraft.getInstance();
    /**
     * Don't touch this plz xd.
     */
    public static TickSync tickSynchronizer = new TickSync();

    public FClient(BaseScriptContext<?> context) {
        super(context);
    }

    /**
     * @return the raw minecraft client class, it may be useful to use <a target="_blank" href="https://wagyourtail.xyz/Projects/Minecraft%20Mappings%20Viewer/App">Minecraft Mappings Viewer</a> for this.
     * @since 1.0.0 (was in the {@code jsmacros} library until 1.2.9)
     */
    public Minecraft getMinecraft() {
        return mc;
    }

    /**
     * @return a helper for interacting with minecraft's registry.
     * @since 1.8.4
     */
    public RegistryHelper getRegistryManager() {
        return new RegistryHelper();
    }

    /**
     * @return a helper to modify and send minecraft packets.
     * @since 1.8.4
     */
    public PacketByteBufferHelper createPacketByteBuffer() {
        return new PacketByteBufferHelper();
    }

    /**
     * Run your task on the main minecraft thread
     *
     * @param runnable task to run
     * @since 1.4.0
     */
    public void runOnMainThread(MethodWrapper<Object, Object, Object, ?> runnable) throws InterruptedException {
        runOnMainThread(runnable, false, runner.config.getOptions(CoreConfigV2.class).maxLockTime);
    }

    /**
     *
     * @param runnable
     * @param watchdogMaxTime
     * @throws InterruptedException
     * @since 1.6.5
     */
    public void runOnMainThread(MethodWrapper<Object, Object, Object, ?> runnable, long watchdogMaxTime) throws InterruptedException {
        runOnMainThread(runnable, false, watchdogMaxTime);
    }

    /**
     * @param runnable
     * @param await
     * @param watchdogMaxTime max time for the watchdog to wait before killing the script
     * @since 1.9.1
     */
    public void runOnMainThread(MethodWrapper<Object, Object, Object, ?> runnable, boolean await, long watchdogMaxTime) throws InterruptedException {
        if (mc.isSameThread()) {
            runnable.run();
        } else if (runner.profile.checkJoinedThreadStack()) {
            throw new IllegalThreadStateException("Attempted to wait on main thread while currently joined to main!");
        } else {
            Semaphore semaphore = new Semaphore(await ? 0 : 1);
            mc.execute(() -> {
                EventContainer<?> lock = new EventContainer<>(runnable.getCtx());
                lock.setLockThread(Thread.currentThread());
                EventLockWatchdog.startWatchdog(lock, new IEventListener() {
                    @Override
                    public boolean joined() {
                        return false;
                    }

                    @Override
                    public EventContainer<?> trigger(BaseEvent event) {
                        return null;
                    }

                    @Override
                    public String toString() {
                        return "RunOnMainThread{\"called_by\": " + runnable.getCtx().getTriggeringEvent().toString() + "}";
                    }
                }, watchdogMaxTime);
                boolean success = false;
                try {
                    runnable.run();
                } catch (Throwable e) {
                    e.printStackTrace();
                } finally {
                    lock.releaseLock();
                    semaphore.release();
                }
            });

            if (await) {
                ctx.wrapSleep(semaphore::acquire);
            }
        }
    }

    /**
     * @return a helper which gives access to all game options and some other useful features.
     * @since 1.1.7 (was in the {@code jsmacros} library until 1.2.9)
     */
    public OptionsHelper getGameOptions() {
        return new OptionsHelper(mc.options);
    }

    /**
     * @return the current minecraft version as a {@link String String}.
     * @since 1.1.2 (was in the {@code jsmacros} library until 1.2.9)
     */
    public String mcVersion() {
        return mc.getLaunchedVersion();
    }

    /**
     * @return the fps debug string from minecraft.
     * @since 1.2.0 (was in the {@code jsmacros} library until 1.2.9)
     */
    public String getFPS() {
        //? if >1.21.8 {
        /*int framerateLimit = mc.options.framerateLimit().get();
        String string;
        if (mc.getGpuUtilization() > 0.0) {
            string = " GPU: " + (mc.getGpuUtilization() > 100.0 ? ChatFormatting.RED + "100%" : Math.round(mc.getGpuUtilization()) + "%");
        } else {
            string = "";
        }

        return String.format(
                Locale.ROOT,
                "%d fps T: %s%s%s%s B: %d%s",
                mc.getFps(),
                framerateLimit == 260 ? "inf" : framerateLimit,
                mc.options.enableVsync().get() ? " vsync " : " ",
                mc.options.graphicsMode().get(),
                mc.options.cloudStatus().get() == CloudStatus.OFF
                        ? ""
                        : (mc.options.cloudStatus().get() == CloudStatus.FAST ? " fast-clouds" : " fancy-clouds"),
                mc.options.biomeBlendRadius().get(),
                string
        );
        *///?} else {
        return mc.fpsString;
        //?}
    }

    /**
     * Join singleplayer world
     *
     * @param folderName
     * @since 1.6.6
     */
    public void loadWorld(String folderName) throws LevelStorageException {

        LevelStorageSource levelstoragesource = mc.getLevelSource();
        List<LevelStorageSource.LevelDirectory> levels = levelstoragesource.findLevelCandidates().levels();
        if (levels.stream().noneMatch(e -> e.directoryName().equals(folderName))) {
            throw new RuntimeException("Level Not Found!");
        }

        mc.execute(() -> {
            boolean bl = mc.isLocalServer();
            if (mc.level != null) {
                mc.level.disconnect(
                        //? if >1.21.5 {
                        Component.nullToEmpty("")
                        //?}
                );
            }
            if (bl) {
                mc.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")), false);
            } else {
                mc.disconnect(null, false);
            }
            mc.createWorldOpenFlows().openWorld(folderName, () -> mc.setScreen(new TitleScreen()));
        });
    }

    /**
     * @param ip
     * @see #connect(String, int)
     * @since 1.2.3 (was in the {@code jsmacros} library until 1.2.9)
     */
    public void connect(String ip) {
        ServerAddress a = ServerAddress.parseString(ip);
        connect(a.getHost(), a.getPort());
    }

    /**
     * Connect to a server
     *
     * @param ip
     * @param port
     * @since 1.2.3 (was in the {@code jsmacros} library until 1.2.9)
     */
    public void connect(String ip, int port) {
        mc.execute(() -> {
            boolean localServer = mc.isLocalServer();
            if (mc.level != null) {
                mc.level.disconnect(
                        //? if >1.21.5 {
                        Component.nullToEmpty("")
                         //?}
                );
            }
            if (localServer) {
                mc.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")), true);
            } else {
                mc.disconnect(new GenericMessageScreen(Component.nullToEmpty("")),false);
            }
            ConnectScreen.startConnecting(null, mc, new ServerAddress(ip, port), new ServerData("server", new ServerAddress(ip, port).toString(), ServerData.Type.OTHER), false, null);
        });
    }

    /**
     * @see #disconnect(MethodWrapper)
     * @since 1.2.3 (was in the {@code jsmacros} library until 1.2.9)
     */
    public void disconnect() {
        disconnect(null);
    }

    /**
     * Disconnect from a server with callback.
     *
     * @param callback calls your method as a {@link java.util.function.Consumer Consumer}&lt;{@link Boolean Boolean}&gt;
     * @since 1.2.3 (was in the {@code jsmacros} library until 1.2.9)
     * <p>
     * {@code callback} defaults to {@code null}
     */
    public void disconnect(@Nullable MethodWrapper<Boolean, Object, Object, ?> callback) {
        mc.execute(() -> {
            boolean isWorld = mc.level != null;
            boolean isInSingleplayer = mc.isLocalServer();
            if (isWorld) {
                // logic in death screen disconnect button
                if (mc.level != null) {
                    mc.level.disconnect(
                            //? if >1.21.5 {
                            Component.nullToEmpty("")
                             //?}
                    );
                }
                mc.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")), false);
                mc.setScreen(new TitleScreen());
            }
            if (isInSingleplayer) {
                mc.setScreen(new TitleScreen());
            } else if (mc.getCurrentServer() != null) {
                if (mc.getCurrentServer().isRealm()) {
                    mc.setScreen(new RealmsMainScreen(new TitleScreen()));
                } else {
                    mc.setScreen(new JoinMultiplayerScreen(new TitleScreen()));
                }
            }
            try {
                if (callback != null) {
                    callback.accept(isWorld);
                }
            } catch (Throwable e) {
                runner.profile.logError(e);
            }
        });
    }

    /**
     * Closes the client (stops the game).
     * Waits until the game has stopped, meaning no further code is executed (for obvious reasons).
     * Warning: this does not wait on joined threads, so your script may stop at an undefined point.
     *
     * @since 1.6.0
     */
    @DocletReplaceReturn("never")
    public void shutdown() {
        mc.execute(mc::stop);

        if (!runner.profile.checkJoinedThreadStack()) {
            // Wait until the game stops
            while (true) {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException ignore) {
                }
            }
        }
    }

    /**
     * @throws InterruptedException
     * @see #waitTick(int)
     * @since 1.2.4
     */
    public void waitTick() throws InterruptedException {
        if (runner.profile.checkJoinedThreadStack()) {
            throw new IllegalThreadStateException("Attempted to wait on a thread that is currently joined to main!");
        }
        ctx.wrapSleep(tickSynchronizer::waitTick);
    }

    /**
     * waits the specified number of client ticks.
     * don't use this on an event that the main thread waits on (joins)... that'll cause circular waiting.
     *
     * @param i
     * @throws InterruptedException
     * @since 1.2.6
     */
    public void waitTick(int i) throws InterruptedException {
        if (runner.profile.checkJoinedThreadStack()) {
            throw new IllegalThreadStateException("Attempted to wait on a thread that is currently joined to main!");
        }
        ctx.wrapSleep(() -> {
            tickSynchronizer.waitTicks(i);
        });
    }

    /**
     * @param ip
     * @return
     * @throws UnknownHostException
     * @throws InterruptedException
     * @since 1.6.5
     */
    public ServerInfoHelper ping(String ip) throws UnknownHostException, InterruptedException {
        ServerData info = new ServerData("", ip, ServerData.Type.OTHER);
        if (runner.profile.checkJoinedThreadStack()) {
            throw new IllegalThreadStateException("pinging from main thread is not supported!");
        }
        Semaphore semaphore = new Semaphore(0);
        TickBasedEvents.serverListPinger.pingServer(info, () -> {}, semaphore::release);
        semaphore.acquire();
        return new ServerInfoHelper(info);
    }

    /**
     * @param ip
     * @param callback
     * @throws UnknownHostException
     * @since 1.6.5
     */
    @DocletReplaceParams("ip: string, callback: MethodWrapper<ServerInfoHelper | null, java.io.IOException | null>")
    public void pingAsync(String ip, MethodWrapper<ServerInfoHelper, IOException, Object, ?> callback) throws UnknownHostException {
        CompletableFuture.runAsync(() -> {
            ServerData info = new ServerData("", ip, ServerData.Type.OTHER);
            try {
                TickBasedEvents.serverListPinger.pingServer(info, () -> {}, () -> callback.accept(new ServerInfoHelper(info), null));
            } catch (IOException e) {
                callback.accept(null, e);
            }
        });
    }

    /**
     * @since 1.6.5
     */
    public void cancelAllPings() {
        TickBasedEvents.serverListPinger.removeAll();
    }

    /**
     * @return a list of all loaded mods.
     * @since 1.8.4
     */
    public List<? extends ModContainerHelper<?>> getLoadedMods() {
        return JsMacros.getModLoader().getLoadedMods();
    }

    /**
     * @param modId the mod modId
     * @return {@code true} if the mod with the given modId is loaded, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isModLoaded(String modId) {
        return JsMacros.getModLoader().isModLoaded(modId);
    }

    /**
     * @param modId the mod modId
     * @return the mod container for the given modId or {@code null} if the mod is not loaded.
     * @since 1.8.4
     */
    @Nullable
    public ModContainerHelper<?> getMod(String modId) {
        return JsMacros.getModLoader().getMod(modId);
    }

    /**
     * Makes minecraft believe that the mouse is currently inside the window.
     * This will automatically set pause on lost focus to false.
     *
     * @since 1.8.4
     */
    public void grabMouse() {
        mc.options.pauseOnLostFocus = false;
        mc.setWindowActive(true);
        mc.mouseHandler.grabMouse();
    }

    /**
     * @return {@code true} if the mod is loaded inside a development environment, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isDevEnv() {
        return JsMacros.getModLoader().isDevEnv();
    }

    /**
     * @return the name of the mod loader.
     * @since 1.8.4
     */
    public String getModLoader() {
        return JsMacros.getModLoader().getName();
    }

    /**
     * @return a list of all loaded blocks as {@link BlockHelper BlockHelper} objects.
     * @since 1.8.4
     */
    public List<BlockHelper> getRegisteredBlocks() {
        return BuiltInRegistries.BLOCK.stream().map(BlockHelper::new).collect(Collectors.toList());
    }

    /**
     * @return a list of all loaded items as {@link ItemHelper ItemHelper} objects.
     * @since 1.8.4
     */
    public List<ItemHelper> getRegisteredItems() {
        return BuiltInRegistries.ITEM.stream().map(ItemHelper::new).collect(Collectors.toList());
    }

    /**
     * Tries to peacefully close the game.
     *
     * @since 1.8.4
     */
    public void exitGamePeacefully() {
        mc.stop();
    }

    /**
     * Will close the game forcefully.
     *
     * @since 1.8.4
     */
    @DocletReplaceReturn("never")
    public void exitGameForcefully() {
        System.exit(0);
    }

    /**
     * @param packet the packet to send
     * @see #createPacketByteBuffer()
     * @since 1.8.4
     */
    public void sendPacket(Packet<?> packet) {
        ClientPacketListener network = mc.getConnection();
        if (network != null) {
            network.send(packet);
        }
    }

    /**
     * @param packet the packet to receive
     * @see #createPacketByteBuffer()
     * @since 1.8.4
     */
    public void receivePacket(Packet<ClientGamePacketListener> packet) {
        packet.handle(mc.getConnection());
    }

    /**
     * moved from FUtils
     * @since 2.0.0
     */
    public String getClipboard() {
        return mc.keyboardHandler.getClipboard();
    }

    /**
     * moved from FUtils
     * @since 2.0.0
     */
    public void setClipboard(String text) {
        mc.keyboardHandler.setClipboard(text);
    }

}
