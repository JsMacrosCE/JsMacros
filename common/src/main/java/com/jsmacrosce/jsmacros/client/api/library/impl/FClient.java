package com.jsmacrosce.jsmacros.client.api.library.impl;

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

import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.api.helper.ModContainerHelper;
import com.jsmacrosce.jsmacros.client.JsMacros;
import com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper;
import com.jsmacrosce.jsmacros.client.api.helper.OptionsHelper;
import com.jsmacrosce.jsmacros.client.api.helper.PacketByteBufferHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.ServerInfoHelper;
import com.jsmacrosce.jsmacros.client.tick.TickBasedEvents;
import com.jsmacrosce.jsmacros.client.tick.TickSync;
import com.jsmacrosce.jsmacros.core.EventLockWatchdog;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.config.CoreConfigV2;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.IEventListener;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;
import com.jsmacrosce.jsmacros.core.language.EventContainer;
import com.jsmacrosce.jsmacros.core.library.Library;
import com.jsmacrosce.jsmacros.core.library.PerExecLibrary;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

//? if >=1.21.11 {
/*import net.minecraft.server.network.EventLoopGroupHolder;
*///? }

//? if >1.21.8 {
/*import net.minecraft.ChatFormatting;
import net.minecraft.client.CloudStatus;
import java.util.Locale;
*///?}

/**
 * Miscellaneous functions for interacting with Minecraft, exposed to scripts as {@code Client}.
 * <p>
 * Use {@link #runOnMainThread(MethodWrapper)} for client-thread work. Tick waits and synchronous
 * pings reject script threads joined to that thread; tick waits do not independently reject a
 * call made directly on the client thread.
 * <pre>
 * // ask the main thread to do something to the world
 * Client.runOnMainThread(JavaWrapper.methodToJava(function () {
 *   Chat.log("the main thread says hello");
 * }));
 *
 * // and wait for a tick, which is only safe off the main thread
 * Client.waitTick(20);
 *
 * // look a server up without joining it
 * const info = Client.ping("mc.hypixel.net");
 * Chat.log(`${info.getAddress()}: ${info.getPlayerCountLabel()}`);
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.2.9
 */
@Library("Client")
@SuppressWarnings("unused")
public class FClient extends PerExecLibrary {
    private static final Minecraft mc = Minecraft.getInstance();
    /**
     * Shared synchronizer used by client-tick waits.
     */
    @DocletIgnore
    public static TickSync tickSynchronizer = new TickSync();

    /**
     * Initializes this per-execution client library with its script context and runner. The
     * context is retained for tick waits and other operations that use {@code wrapSleep}.
     *
     * @param context non-null script context associated with this library
     */
    public FClient(BaseScriptContext<?> context) {
        super(context);
    }

    /**
     * Returns the raw Minecraft client instance rather than a JsMacros wrapper. Its fields may
     * be unavailable (including {@code null}) outside the relevant game state.
     *
     * @return the client instance
     * @since 1.0.0 (was in the {@code jsmacros} library until 1.2.9)
     */
    public Minecraft getMinecraft() {
        return mc;
    }

    /**
     * Returns a helper for looking up registered game objects by ID.
     *
     * @return the registry helper
     * @since 1.8.4
     */
    public RegistryHelper getRegistryManager() {
        return new RegistryHelper();
    }

    /**
     * Returns a new empty helper buffer. Packet events provide their own packet-backed buffers.
     *
     * @return an empty packet buffer helper
     * @since 1.8.4
     */
    public PacketByteBufferHelper createPacketByteBuffer() {
        return new PacketByteBufferHelper();
    }

    /**
     * Runs a task on the Minecraft client thread without waiting for queued execution to finish.
 * <p>
     * If already on that thread, the task runs immediately. Otherwise it is queued and this
     * method returns without waiting. Exceptions thrown by the task are printed; a thread joined
     * to the client thread is rejected to avoid deadlock.
     * <pre>
     * // change the world on the main thread
     * Client.runOnMainThread(JavaWrapper.methodToJava(function () {
     *   const player = Player.getPlayer();
     *   if (player !== null) {
     *     player.setPos(0.5, 70, 0.5);
     *   }
     * }));
     * </pre>
     *
     * @param runnable task to run
     * @throws InterruptedException if the script is interrupted while waiting
     * @throws IllegalThreadStateException if asked to wait from a thread joined to the main thread
     * @since 1.4.0
     */
    public void runOnMainThread(MethodWrapper<Object, Object, Object, ?> runnable) throws InterruptedException {
        runOnMainThread(runnable, false, runner.config.getOptions(CoreConfigV2.class).maxLockTime);
    }

    /**
     * Queues a task on the Minecraft main thread with a specified watchdog limit.
     * <p>
     * The limit is in milliseconds. Unlike the no-limit overload, this overload does not wait
     * for the task to finish; the watchdog can terminate the task's script if it exceeds the limit.
     *
     * @param runnable         task to run
     * @param watchdogMaxTime watchdog limit in milliseconds
     * @throws InterruptedException if the script is interrupted while waiting
     * @throws IllegalThreadStateException if asked to wait from a thread joined to the main thread
     * @since 1.6.5
     */
    public void runOnMainThread(MethodWrapper<Object, Object, Object, ?> runnable, long watchdogMaxTime) throws InterruptedException {
        runOnMainThread(runnable, false, watchdogMaxTime);
    }

    /**
     * Runs a task on the Minecraft main thread, optionally waiting for it to finish.
     * <p>
     * This is the full form of {@link #runOnMainThread(MethodWrapper)}. When {@code await} is
     * true, the caller waits for the task to finish; otherwise it returns
     * after queuing it. Calls from a thread joined to the main thread throw
     * {@link IllegalThreadStateException} (unless already on the main thread).
     *
     * @param runnable        task to run
     * @param await           whether to block until the task has run
     * @param watchdogMaxTime watchdog limit in milliseconds
     * @throws InterruptedException if the script is interrupted while waiting
     * @throws IllegalThreadStateException if asked to wait from a thread joined to the main thread
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
     * Returns a helper over the game's settings.
     *
     * @return a helper which gives access to all game options and some other useful features.
     * @since 1.1.7 (was in the {@code jsmacros} library until 1.2.9)
     */
    public OptionsHelper getGameOptions() {
        return new OptionsHelper(mc.options);
    }

    /**
     * Returns the version identifier with which the game was launched.
     *
     * @return the current minecraft version as a {@link String String}.
     * @since 1.1.2 (was in the {@code jsmacros} library until 1.2.9)
     */
    public String mcVersion() {
        return mc.getLaunchedVersion();
    }

    /**
     * Returns the game's FPS readout as a formatted string, not a numeric frame rate.
     *
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
                //? if >=1.21.11 {
                /^mc.options.graphicsPreset().get(),
                ^///? } else {
                mc.options.graphicsMode().get(),
                //? }
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
     * Opens a singleplayer world by its save-folder name.
     * <p>
     * This is the saves folder name rather than a path, so for a world at
     * {@code .minecraft/saves/myworld} the argument is {@code myworld}. The name is checked
     * against available saves; if the open succeeds, loading is queued on
     * the main thread and may not have completed when this method returns.
     *
     * @param folderName the name of the save folder under the world's saves directory
     * @throws LevelStorageException if the world storage cannot be read
     * @throws RuntimeException if no save with that name exists
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
     * Connects to a server using an address string, optionally including its port.
     * <p>
     * The address is parsed by Minecraft and passed to {@link #connect(String, int)}.
     *
     * @param ip the server address, with an optional {@code :port}
     * @see #connect(String, int)
     * @since 1.2.3 (was in the {@code jsmacros} library until 1.2.9)
     */
    public void connect(String ip) {
        ServerAddress a = ServerAddress.parseString(ip);
        connect(a.getHost(), a.getPort());
    }

    /**
     * Queues a connection to the given server host and port on the Minecraft main thread.
     *
     * @param ip server host name or address
     * @param port server port
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
     * Disconnects from the current world or server without a callback.
     *
     * @see #disconnect(MethodWrapper)
     * @since 1.2.3 (was in the {@code jsmacros} library until 1.2.9)
     */
    public void disconnect() {
        disconnect(null);
    }

    /**
     * Disconnects from the current world or server and optionally invokes a callback.
     * <p>
     * The callback is queued on the main thread and receives whether a world was open when this
     * method was called. Exceptions thrown by the callback are caught and printed.
     *
     * @param callback callback to invoke with whether a world was open, or {@code null} for none
     * @since 1.2.3 (was in the {@code jsmacros} library until 1.2.9)
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
     * Queues a client stop. A caller not joined to the main thread then sleeps indefinitely,
     * ignoring interruption; a joined caller returns after queuing the stop.
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
     * Waits until the client tick counter advances, using the shared {@link TickSync} synchronizer.
     * Throws if the calling script thread is joined to the main thread; the method does not
     * independently check whether the caller is the main thread.
     * <pre>
     * // three steps, a tick apart
     * for (const step of ["one", "two", "three"]) {
     *   Chat.log(step);
     *   Client.waitTick();
     * }
     * </pre>
     *
     * @throws InterruptedException if the script is interrupted while waiting
     * @throws IllegalThreadStateException if called on or from a thread joined to the main thread
     * @since 1.2.4
     */
    public void waitTick() throws InterruptedException {
        if (runner.profile.checkJoinedThreadStack()) {
            throw new IllegalThreadStateException("Attempted to wait on a thread that is currently joined to main!");
        }
        ctx.wrapSleep(tickSynchronizer::waitTick);
    }

    /**
     * Waits for the specified number of client ticks; for callers not joined to the main thread,
     * zero or negative counts return without waiting. Throws if the calling script thread is
     * joined to the main thread, even for a nonpositive count.
     * <pre>
     * // hold a key down for about half a second of game time
     * KeyBind.pressKeyBind("key.forward");
     * Client.waitTick(10);
     * KeyBind.releaseKeyBind("key.forward");
     * </pre>
     *
     * @param i how many client ticks to wait for
     * @throws InterruptedException if the script is interrupted while waiting
     * @throws IllegalThreadStateException if called on or from a thread joined to the main thread
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
     * Starts a server status ping and waits for its completion signal, then returns the server
     * data helper. The signal is issued after the status response's pong; it does not indicate
     * that status data was successfully received. Throws if the calling script thread is joined
     * to the main thread.
     * <pre>
     * // what a server says about itself
     * const info = Client.ping("mc.hypixel.net");
     * Chat.log(`${info.getAddress()}: ${info.getPlayerCountLabel()} in ${info.getVersion()}`);
     * </pre>
     *
     * @param ip the server address to look up
     * @return the server data helper, which may contain failure or incomplete status data
     * @throws UnknownHostException if the address cannot be resolved
     * @throws InterruptedException if the script is interrupted while waiting
     * @throws IllegalThreadStateException if called on or from a thread joined to the main thread
     * @since 1.6.5
     */
    public ServerInfoHelper ping(String ip) throws UnknownHostException, InterruptedException {
        ServerData info = new ServerData("", ip, ServerData.Type.OTHER);
        if (runner.profile.checkJoinedThreadStack()) {
            throw new IllegalThreadStateException("pinging from main thread is not supported!");
        }
        Semaphore semaphore = new Semaphore(0);
        TickBasedEvents.serverListPinger.pingServer(
                info,
                () -> {},
                semaphore::release
                //? if >=1.21.11 {
                /*, EventLoopGroupHolder.remote(true)
                *///? }
        );
        semaphore.acquire();
        return new ServerInfoHelper(info);
    }

    /**
     * Starts a server status ping on an asynchronous task. The callback receives the helper after
     * ping completion, or {@code null} and the {@link IOException} if starting the ping fails. The
     * callback runs on the ping completion thread or, for a startup failure, the asynchronous task
     * thread. Startup failures, including {@link UnknownHostException}, are delivered to the
     * callback rather than thrown from this method.
     * <pre>
     * // look several servers up at once, without waiting for any of them
     * for (const address of ["mc.hypixel.net", "play.cubecraft.net"]) {
     *   Client.pingAsync(address, JavaWrapper.methodToJava(function (info, error) {
     *     if (info !== null) {
     *       Chat.log(`${address}: ${info.getPlayerCountLabel()}`);
     *     } else {
     *       Chat.log(`${address} did not answer`);
     *     }
     *   }));
     * }
     * </pre>
     *
     * @param ip the server address to look up
     * @param callback called with what the server reported, or with {@code null} and the error
     * @since 1.6.5
     */
    @DocletReplaceParams("ip: string, callback: MethodWrapper<ServerInfoHelper | null, java.io.IOException | null>")
    public void pingAsync(String ip, MethodWrapper<ServerInfoHelper, IOException, Object, ?> callback) throws UnknownHostException {
        CompletableFuture.runAsync(() -> {
            ServerData info = new ServerData("", ip, ServerData.Type.OTHER);
            try {
                TickBasedEvents.serverListPinger.pingServer(
                        info,
                        () -> {},
                        () -> callback.accept(new ServerInfoHelper(info), null)
                        //? if >=1.21.11 {
                        /*, EventLoopGroupHolder.remote(true)
                        *///? }
                );
            } catch (IOException e) {
                callback.accept(null, e);
            }
        });
    }

    /**
     * Removes currently connected server-status ping connections and disconnects them. This does
     * not establish that pending or in-flight ping callbacks are cancelled.
     *
     * @since 1.6.5
     */
    public void cancelAllPings() {
        TickBasedEvents.serverListPinger.removeAll();
    }

    /**
     * Returns the mod loader's loaded-mod collection.
     *
     * @return the loaded mods reported by the mod loader
     * @since 1.8.4
     */
    public List<? extends ModContainerHelper<?>> getLoadedMods() {
        return JsMacros.getModLoader().getLoadedMods();
    }

    /**
     * Returns whether the mod loader reports the specified mod ID as loaded.
     *
     * @param modId the mod ID to check
     * @return {@code true} if the mod with the given modId is loaded, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isModLoaded(String modId) {
        return JsMacros.getModLoader().isModLoaded(modId);
    }

    /**
     * Returns the mod container for the specified mod ID, or {@code null} if none is found.
     *
     * @param modId the mod ID to look up
     * @return the mod container for the given modId or {@code null} if the mod is not loaded.
     * @since 1.8.4
     */
    @Nullable
    public ModContainerHelper<?> getMod(String modId) {
        return JsMacros.getModLoader().getMod(modId);
    }

    /**
     * Disables pause-on-lost-focus and grabs the mouse. On Minecraft versions before 26.1, it
     * also marks the window active. The option change is not restored by this method.
     *
     * @since 1.8.4
     */
    public void grabMouse() {
        mc.options.pauseOnLostFocus = false;
        // TODO(26.1): setWindowActive was removed in 26.1, This needs to be tested at runtime.
        //? if <26.1 {
        mc.setWindowActive(true);
        //? }
        mc.mouseHandler.grabMouse();
    }

    /**
     * Returns whether the mod loader considers the current environment a development environment.
     *
     * @return {@code true} when the mod loader reports a development environment
     * @since 1.8.4
     */
    public boolean isDevEnv() {
        return JsMacros.getModLoader().isDevEnv();
    }

    /**
     * Returns the mod loader's name.
     *
     * @return the name of the mod loader.
     * @since 1.8.4
     */
    public String getModLoader() {
        return JsMacros.getModLoader().getName();
    }

    /**
     * Returns helpers for all entries in the block registry, independent of the current world.
     *
     * @return a list of all loaded blocks as {@link BlockHelper BlockHelper} objects.
     * @since 1.8.4
     */
    public List<BlockHelper> getRegisteredBlocks() {
        return BuiltInRegistries.BLOCK.stream().map(BlockHelper::new).collect(Collectors.toList());
    }

    /**
     * Returns helpers for all entries in the item registry, independent of the current inventory.
     *
     * @return a list of all loaded items as {@link ItemHelper ItemHelper} objects.
     * @since 1.8.4
     */
    public List<ItemHelper> getRegisteredItems() {
        return BuiltInRegistries.ITEM.stream().map(ItemHelper::new).collect(Collectors.toList());
    }

    /**
     * Requests a normal game shutdown without waiting. In contrast, {@link #shutdown()} queues
     * shutdown and then blocks callers not joined to the main thread indefinitely.
     *
     * @since 1.8.4
     */
    public void exitGamePeacefully() {
        mc.stop();
    }

    /**
     * Terminates the JVM with exit status {@code 0}.
     *
     * @since 1.8.4
     */
    @DocletReplaceReturn("never")
    public void exitGameForcefully() {
        System.exit(0);
    }

    /**
     * Sends a packet through the current server connection, if one exists.
     *
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
     * Invokes a clientbound packet's handler directly, passing the current connection.
     *
     * @param packet the clientbound packet to handle as if it arrived from the server
     * @see #createPacketByteBuffer()
     * @since 1.8.4
     */
    public void receivePacket(Packet<ClientGamePacketListener> packet) {
        packet.handle(mc.getConnection());
    }

    /**
     * Returns the clipboard text through the client's keyboard handler.
     *
     * @return clipboard text, or an empty string if the underlying clipboard read returns no text
     * @since 2.0.0
     */
    public String getClipboard() {
        return mc.keyboardHandler.getClipboard();
    }

    /**
     * Sets the clipboard text through the client's keyboard handler. Empty strings are ignored,
     * so passing {@code ""} does not clear the clipboard.
     *
     * @param text the text to set
     * @since 2.0.0
     */
    public void setClipboard(String text) {
        mc.keyboardHandler.setClipboard(text);
    }

}
