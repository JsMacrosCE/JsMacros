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
 * Functions that interact with minecraft that don't fit into their own module.
 * <p>
 * An instance of this class is passed to scripts as the {@code Client} variable.
 * <br>
 * This is the odds and ends library, and it is worth knowing the three threads of the main
 * thread come through here before anything else, because several of the calls care about which
 * one a script is on. A script runs on its own thread by default, so it must ask for the main
 * thread with {@link #runOnMainThread(MethodWrapper)} to touch the world directly. The reverse
 * case is a listener running on the main thread, which cannot then wait on anything that needs
 * the main thread to make progress, so {@link #waitTick()} and its numbered form raise there
 * rather than deadlocking. The third case is a thread the script has joined to the main one, and
 * that is the one most of the guards here are looking for.<br>
 * The rest is grouped by what it does. {@link #connect(String)}, {@link #disconnect()} and
 * {@link #loadWorld(String)} get in and out of somewhere. {@link #ping(String)} and
 * {@link #pingAsync(String, MethodWrapper)} look a server up without joining it, and
 * {@link #getGameOptions()} hands out the settings object. {@link #getLoadedMods()} and the
 * three mod calls answer questions about what is installed, and {@link #getRegisteredBlocks()}
 * and {@link #getRegisteredItems()} list what the registries hold. {@link #shutdown()},
 * {@link #exitGamePeacefully()} and {@link #exitGameForcefully()} are the three ways out, and
 * they are not equivalent: only the first two let the game save and close cleanly.<br>
 * {@link #getMinecraft()} is the escape hatch to the raw client object, for the things nothing
 * here wraps. It is typed as untyped in the shipped definitions, since the class it returns has
 * no TypeScript definition of its own, so a value from it has to be cast before it can be passed
 * anywhere.
 * example:
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
     * the shared clock the tick waiters are released by.
     * <p>
     * Every call to {@link #waitTick()} and {@link #waitTick(int)} blocks on this, and the
     * game's tick loop is what lets it go. It is the object that makes a waitTick a wait for a
     * real client tick rather than for a sleep, and a script should not touch it: it is
     * reassigned nowhere, so replacing it would leave every existing waiter blocked forever.
     * <p>
     * Don't touch this plz xd.
     */
    @DocletIgnore
    public static TickSync tickSynchronizer = new TickSync();

    public FClient(BaseScriptContext<?> context) {
        super(context);
    }

    /**
     * the raw client object, for the things nothing else here wraps.
     * <p>
     * This is the game's own client class rather than a wrapper, so it is where the things the
     * rest of the API does not cover are reached. That makes it a sharp tool: the names are the
     * intermediary mapped ones rather than the Mojang ones, and the
     * <a target="_blank" href="https://wagyourtail.xyz/Projects/Minecraft%20Mappings%20Viewer/App">Minecraft Mappings Viewer</a>
     * is the quickest way to look one up. Most of the state on it is {@code null} outside a
     * world, so a field read has to be checked rather than assumed.<br>
     * One thing to know in an editor: the shipped TypeScript definitions describe the JDK and
     * not the game, so this comes back typed as untyped and a value taken from it has to be
     * cast before it is passed anywhere.
     * {@link com.jsmacrosce.jsmacros.core.library.impl.FReflection#getClass(String)} is the way to
     * reach a game class by name when a type is needed.
     * example:
     * <pre>
     * // reach something nothing wraps, through the raw client
     * const minecraft = Client.getMinecraft();
     * const window = minecraft.getWindow();
     * Chat.log(`framerate limit ${window.getGuiScaledWidth()} by ${window.getGuiScaledHeight()}`);
     * </pre>
     *
     * @return the raw minecraft client class, it may be useful to use <a target="_blank" href="https://wagyourtail.xyz/Projects/Minecraft%20Mappings%20Viewer/App">Minecraft Mappings Viewer</a> for this.
     * @since 1.0.0 (was in the {@code jsmacros} library until 1.2.9)
     */
    public Minecraft getMinecraft() {
        return mc;
    }

    /**
     * a handle on the game's registries, for looking ids up in both directions.
     * <p>
     * This is how a block, item, entity or sound name becomes the object behind it and back
     * again, and it is what {@code World.getEntities} and the other id-taking calls use
     * internally, so it is the place to go for an id that those calls will not accept. A fresh
     * handle is made on each call, and there is nothing to keep alive.
     * example:
     * <pre>
     * const registry = Client.getRegistryManager();
     * // an id string, and the block behind it
     * const block = registry.getBlock("minecraft:stone");
     * if (block !== null) {
     *   Chat.log(`${block.getName()} is a ${block.getId()}`);
     * }
     * </pre>
     *
     * @return a helper for interacting with minecraft's registry.
     * @since 1.8.4
     */
    public RegistryHelper getRegistryManager() {
        return new RegistryHelper();
    }

    /**
     * a reader and writer over a packet's raw bytes.
     * <p>
     * This is the reader and writer over a packet's raw bytes, and it is the type the packet
     * events hand out their buffer as. The two directions are the same object: what is written
     * is what a reader then sees, and every read moves the position along, so
     * {@link PacketByteBufferHelper#reset()} is how to get back to the start and read the same
     * bytes twice. A fresh empty buffer is made on each call, and the constructors on the
     * helper itself are the way to wrap a packet's own bytes.
     * <p>
     * Treat a buffer as read only for now: the game is handed the packet rather than the buffer,
     * so writing into it does not reach the game, and turning it back into a packet with
     * {@code toPacket()} does not currently work.
     * example:
     * <pre>
     * // read the fields of a packet that has just arrived, in the order it writes them
     * JsMacros.on("RecvPacket", JavaWrapper.methodToJava(function (event) {
     *   const buf = event.getPacketBuffer();
     *   Chat.log(`${event.type} carries ${buf.readVarInt()}`);
     * }));
     * </pre>
     *
     * @return a helper to modify and send minecraft packets.
     * @since 1.8.4
     */
    public PacketByteBufferHelper createPacketByteBuffer() {
        return new PacketByteBufferHelper();
    }

    /**
     * Run your task on the main minecraft thread
     * <p>
     * Almost everything that touches the world has to happen on the main thread, and this is
     * how a script gets there. The task is handed over and this returns straight away, so a
     * script that changes the world and then reads it back has to wait: either
     * {@link #waitTick()} afterwards, or the {@code await} form of this call.<br>
     * Two things follow from what this is. It does nothing on the main thread, it just runs
     * the task there and then, so calling it from a listener that is already on the main
     * thread is harmless. And it refuses to wait when the calling thread is joined to the main
     * one, because the main thread would then be waiting for the thing it is meant to be
     * running; the joined form runs the task and returns rather than deadlocking.
     * <p>
     * An exception thrown inside the task is printed and the script carries on, so a task that
     * fails does not stop the rest of the run. A task that blocks for longer than the
     * watchdog allows is killed.
     * example:
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
     * Run your task on the main minecraft thread, with a watchdog time of your own.
     * <p>
     * This is {@link #runOnMainThread(MethodWrapper)} with the limit on how long the task may
     * hold the main thread given explicitly, in milliseconds. A task that runs longer than that
     * is killed along with the script waiting on it. The default comes from the profile's
     * settings and is what the shorter form uses.
     * example:
     * <pre>
     * // a task allowed a full second to finish
     * Client.runOnMainThread(JavaWrapper.methodToJava(function () {
     *   Chat.log("still on the main thread");
     * }), 1000);
     * </pre>
     *
     * @param runnable         task to run
     * @param watchdogMaxTime  how long the task may hold the main thread, in milliseconds
     * @throws InterruptedException if the script is interrupted while waiting
     * @throws IllegalThreadStateException if asked to wait from a thread joined to the main thread
     * @since 1.6.5
     */
    public void runOnMainThread(MethodWrapper<Object, Object, Object, ?> runnable, long watchdogMaxTime) throws InterruptedException {
        runOnMainThread(runnable, false, watchdogMaxTime);
    }

    /**
     * Run your task on the main minecraft thread, optionally waiting for it to finish.
     * <p>
     * This is the full form of {@link #runOnMainThread(MethodWrapper)}. With {@code await} set
     * to {@code true} the call blocks until the task has actually run, which is what a script
     * needs when the next line depends on the world having changed. With {@code false} it
     * returns as soon as the task is queued, and {@link #waitTick()} is the way to give it a
     * chance to happen.<br>
     * Asking to wait from a thread joined to the main thread raises rather than hanging, since
     * the main thread cannot make progress until the thing it is waiting on returns.
     * example:
     * <pre>
     * // make a change and be sure it has landed before carrying on
     * Client.runOnMainThread(JavaWrapper.methodToJava(function () {
     *   const player = Player.getPlayer();
     *   if (player !== null) {
     *     player.setPos(0.5, 70, 0.5);
     *   }
     * }), true, 5000);
     * Chat.log("the move has landed");
     * </pre>
     *
     * @param runnable        task to run
     * @param await           whether to block until the task has run
     * @param watchdogMaxTime max time for the watchdog to wait before killing the script
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
     * a handle on the game's settings.
     * <p>
     * This is the options screen's own object, so it reads and writes the real settings: a
     * change here is what the game uses from the next time it looks, and it is the same object
     * a change made in the options screen would go through. A fresh handle is made on each
     * call, and they all wrap the same settings, so a value written through one is visible
     * through another.
     * example:
     * <pre>
     * const options = Client.getGameOptions();
     * // turn the gui scale to a fixed value
     * options.setGuiScale(2);
     * Chat.log(`gui scale is now ${options.getGuiScale()}`);
     * </pre>
     *
     * @return a helper which gives access to all game options and some other useful features.
     * @since 1.1.7 (was in the {@code jsmacros} library until 1.2.9)
     */
    public OptionsHelper getGameOptions() {
        return new OptionsHelper(mc.options);
    }

    /**
     * the version string the game was launched as, which is what the launcher called it.
     * <p>
     * This is not the modpack or profile name, it is the version id, so it looks like
     * {@code 1.21.8} or {@code 1.21.8-forge}. A script that needs to know which target it is on
     * should compare against this.
     * example:
     * <pre>
     * Chat.log(`running on ${Client.mcVersion()}`);
     * </pre>
     *
     * @return the current minecraft version as a {@link String String}.
     * @since 1.1.2 (was in the {@code jsmacros} library until 1.2.9)
     */
    public String mcVersion() {
        return mc.getLaunchedVersion();
    }

    /**
     * the frame rate readout the game shows on the debug screen, as a whole line of text.
     * <p>
     * This is the game's own formatted string rather than a number, so it carries the frame
     * limit, whether vsync is on, the graphics preset, the cloud setting, the biome blend
     * distance and, where the game can measure it, the GPU load. A script that wants a plain
     * number has to read it out of the string, and what is in the string differs between game
     * versions.
     * example:
     * <pre>
     * // the same line the debug screen shows in the top left
     * Chat.log(Client.getFPS());
     * </pre>
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
     * Join singleplayer world
     * <p>
     * This is the saves folder name rather than a path, so for a world at
     * {@code .minecraft/saves/myworld} the argument is {@code myworld}. The name is checked
     * against the saves folder before anything happens, and one that is not there raises rather
     * than silently doing nothing. If a world is already open it is closed and saved first, so
     * this is also a way to switch worlds.
     * <p>
     * The load itself happens on the main thread after this returns, so a script that carries
     * on immediately will be running against the old world for a moment.
     * example:
     * <pre>
     * // open a singleplayer world by its saves folder name
     * Client.loadWorld("myworld");
     * </pre>
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
     * connect to a server, address and port together.
     * <p>
     * The address is the one typed into the multiplayer screen, so a host name works and a
     * {@code host:port} works too. When there is no port in it the standard one is used. This is
     * the same as {@link #connect(String, int)} with the port taken from the string, so use that
     * form when the port is a number of its own.
     * <p>
     * The connection is made on the main thread after this returns, so a script that reads the
     * world straight afterwards will still see the old one. Anything already open is closed
     * first, saving a singleplayer world on the way out.
     * example:
     * <pre>
     * Client.connect("mc.hypixel.net");
     * </pre>
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
     * Connect to a server
     * <p>
     * The address is a host name or an ip with no port on it; the port is separate here. A local
     * world is closed and saved first, so this is also a way out of singleplayer.<br>
     * The connection is made on the main thread after this returns, so a script that reads the
     * world straight afterwards will still see the old one.
     * example:
     * <pre>
     * Client.connect("localhost", 25565);
     * </pre>
     *
     * @param ip the server host name or address, with no port
     * @param port the port to connect on
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
     * leave whatever is open and go back to the title screen.
     * <p>
     * This is the no callback form, so it is the same as passing {@code null}.
     * example:
     * <pre>
     * Client.disconnect();
     * </pre>
     *
     * @see #disconnect(MethodWrapper)
     * @since 1.2.3 (was in the {@code jsmacros} library until 1.2.9)
     */
    public void disconnect() {
        disconnect(null);
    }

    /**
     * Disconnect from a server with callback.
     * <p>
     * This saves and closes whatever is open and puts the title screen up, or the realms or
     * multiplayer screen when that is where the client came from. The callback runs on the main
     * thread once that has happened, and it is told whether a world was actually open, so a
     * script can tell leaving a server from already being at the title screen. An exception
     * thrown inside the callback is logged and does not reach the script.<br>
     * The callback is optional and defaults to {@code null}. A {@code null} here means the
     * script does not care.
     * example:
     * <pre>
     * // leave, and be told afterwards whether there was anything to leave
     * Client.disconnect(JavaWrapper.methodToJava(function (wasInWorld) {
     *   Chat.log(wasInWorld ? "left the world" : "was already at the title screen");
     * }));
     * </pre>
     *
     * @param callback calls your method as a {@link java.util.function.Consumer Consumer}{@code <}{@link Boolean Boolean}{@code >},
     *                 told whether a world was open, or {@code null} for none
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
     * block until the client has ticked once.
     * <p>
     * This waits for a real game tick rather than for a length of time, so it is the right way
     * to let something that was queued on the main thread actually happen. It blocks for about
     * a twentieth of a second, and it is the standard way to space the steps of a macro out.
     * <br>
     * It cannot be called from a listener running on the main thread, or from a thread joined
     * to the main one, because the main thread would be waiting for the tick it is meant to be
     * producing. In both of those cases it raises rather than hanging.
     * example:
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
     * <p>
     * A tick is a twentieth of a second, so twenty of them is about a second, and this is the
     * way a macro paces itself. It is a tick count rather than a wall clock time, so it slows
     * down with the game instead of firing into a lagging world, which is what a movement macro
     * wants.<br>
     * It cannot be called from a listener running on the main thread, or from a thread joined
     * to the main one, and raises there rather than hanging. A count of zero or less returns
     * without waiting at all.
     * example:
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
     * look a server up without joining it, and wait for the answer.
     * <p>
     * This is the same lookup the multiplayer screen does, so the result is the server's reported
     * version, player count, ping and icon. It blocks until the answer arrives, which for an
     * address that does not answer is however long the underlying timeout is, so a script should
     * not call it in a tight loop. A server that cannot be reached raises
     * {@link UnknownHostException} rather than handing back an empty helper.
     * <p>
     * It cannot be called from the main thread or from a thread joined to it, since the answer
     * needs the network thread and blocking the main thread waiting for it would stall the game;
     * in both of those cases it raises {@link IllegalThreadStateException}. Use
     * {@link #pingAsync(String, MethodWrapper)} where the script may be on the main thread.
     * example:
     * <pre>
     * // what a server says about itself
     * const info = Client.ping("mc.hypixel.net");
     * Chat.log(`${info.getAddress()}: ${info.getPlayerCountLabel()} in ${info.getVersion()}`);
     * </pre>
     *
     * @param ip the server address to look up
     * @return what the server reported about itself
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
     * look a server up without joining it, and get told the answer later.
     * <p>
     * This is the non blocking form of {@link #ping(String)}: it returns straight away and the
     * callback runs when the server answers, with either the helper or {@code null} and the
     * error that stopped it. The callback runs on a background thread, not the main one, so
     * anything that touches the world has to be handed to
     * {@link #runOnMainThread(MethodWrapper)} from inside it.<br>
     * This is also the form to use from the main thread, which {@link #ping(String)} refuses.
     * An address that resolves to nothing comes back through the callback as an error rather
     * than raising here, so the one thing that raises is a resolver failure that happens
     * synchronously.
     * example:
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
     * @throws UnknownHostException if the address cannot be resolved
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
     * abandon every ping that is in flight, whether queued or waiting on an answer.
     * <p>
     * This is what a script wants before it leaves, since a callback that runs after the script
     * has finished is wasted work and can log into a context that is gone. A ping cancelled this
     * way never calls its callback at all, so a script waiting on one has to check whether it is
     * still running rather than assuming an answer is coming.
     * example:
     * <pre>
     * // look up a server, and give up if it has not answered in time
     * Client.pingAsync("example.invalid", JavaWrapper.methodToJava(function (info) {
     *   Chat.log("answered");
     * }));
     * Client.waitTick(100);
     * Client.cancelAllPings();
     * </pre>
     *
     * @since 1.6.5
     */
    public void cancelAllPings() {
        TickBasedEvents.serverListPinger.removeAll();
    }

    /**
     * every mod that is loaded, whatever mod loader brought it in.
     * <p>
     * This is a snapshot, and a fresh handle is made on each call. Each entry says which loader
     * it came from and gives the mod's own metadata, so a script that wants a list of what is
     * installed goes here rather than at the mod loader by name.
     * example:
     * <pre>
     * for (const mod of Client.getLoadedMods()) {
     *   Chat.log(`${mod.getId()} ${mod.getVersion()}`);
     * }
     * </pre>
     *
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
     * <p>
     * Two things happen here and the second is the one with a lasting effect. The game is told
     * the window is active, and the game option to pause when the window loses focus is turned
     * off and left off, so a player who tabs away while a macro runs does not come back to a
     * paused game. The mouse is then grabbed, so it stops being visible and the game takes it
     * as its own. Nothing releases either of those, so a script that wants the window to behave
     * normally again has to put the option back itself.
     * example:
     * <pre>
     * // take the mouse and stop the game pausing when the window loses focus
     * Client.grabMouse();
     * </pre>
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
     * whether this is a development copy rather than a released one.
     * <p>
     * This asks the mod loader, so it is really asking whether the game is running from a
     * development environment. It is what a macro uses to decide whether to be chatty, and a
     * released game reports {@code false}.
     * example:
     * <pre>
     * if (Client.isDevEnv()) {
     *   Chat.getLogger("my macro").info("running in a development environment");
     * }
     * </pre>
     *
     * @return {@code true} if the mod is loaded inside a development environment, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isDevEnv() {
        return JsMacros.getModLoader().isDevEnv();
    }

    /**
     * the name of the mod loader in use, which is how a script tells the loaders apart.
     * <p>
     * This is a short name rather than a whole object, and it is the thing to branch on when
     * two loaders behave differently.
     * example:
     * <pre>
     * const loader = Client.getModLoader();
     * Chat.log(loader === "fabric" ? "on fabric" : `on ${loader}`);
     * </pre>
     *
     * @return the name of the mod loader.
     * @since 1.8.4
     */
    public String getModLoader() {
        return JsMacros.getModLoader().getName();
    }

    /**
     * every block the game knows about, not only the ones in a world.
     * <p>
     * This walks the block registry, so it includes every block any mod has added, whether or
     * not the world being played has any of them. A lookup by id is usually what a script wants
     * instead, and {@link #getRegistryManager()} is the way to do it.
     * example:
     * <pre>
     * // how many kinds of block are installed
     * Chat.log(`${Client.getRegisteredBlocks().size()} blocks registered`);
     * </pre>
     *
     * @return a list of all loaded blocks as {@link BlockHelper BlockHelper} objects.
     * @since 1.8.4
     */
    public List<BlockHelper> getRegisteredBlocks() {
        return BuiltInRegistries.BLOCK.stream().map(BlockHelper::new).collect(Collectors.toList());
    }

    /**
     * every item the game knows about, not only the ones in an inventory.
     * <p>
     * This walks the item registry, so it includes every item any mod has added. A lookup by id
     * is usually what a script wants instead, and {@link #getRegistryManager()} is the way to
     * do it.
     * example:
     * <pre>
     * // how many kinds of item are installed
     * Chat.log(`${Client.getRegisteredItems().size()} items registered`);
     * </pre>
     *
     * @return a list of all loaded items as {@link ItemHelper ItemHelper} objects.
     * @since 1.8.4
     */
    public List<ItemHelper> getRegisteredItems() {
        return BuiltInRegistries.ITEM.stream().map(ItemHelper::new).collect(Collectors.toList());
    }

    /**
     * Tries to peacefully close the game.
     * <p>
     * This asks the game to stop, which is the same thing closing the window does: the game
     * saves, closes cleanly and shuts down. The script itself keeps running until the process
     * goes, so a line after this is reached in a game that is on its way out and should not be
     * relied on. {@link #shutdown()} is the form that waits for the game to actually be gone.
     * example:
     * <pre>
     * Chat.log("closing the game");
     * Client.exitGamePeacefully();
     * </pre>
     *
     * @since 1.8.4
     */
    public void exitGamePeacefully() {
        mc.stop();
    }

    /**
     * Will close the game forcefully.
     * <p>
     * This kills the process outright rather than asking the game to stop, so the game gets no
     * chance to save and anything not written to disk is lost. That makes it the wrong tool for
     * closing after a session, and {@link #exitGamePeacefully()} or {@link #shutdown()} is what
     * a script normally wants.<br>
     * Nothing after this line runs.
     *
     * @since 1.8.4
     */
    @DocletReplaceReturn("never")
    public void exitGameForcefully() {
        System.exit(0);
    }

    /**
     * <p>
     * A packet sent with nothing connected is dropped without complaint rather than raising, so
     * this is safe to call from a script that runs anywhere. A packet the server rejects is
     * still the server's business and is not something this reports on.
     *
     * @param packet the packet to send, which is dropped if nothing is connected
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
     * hand a packet to the client as if it had arrived from the server.
     * <p>
     * This runs the packet's handler rather than going through the network, so it is the way to
     * make the client act on something a script invented. It expects a packet that travels from
     * the server to the client, the kind with a client side handler, and it does nothing when
     * nothing is connected, since there is no connection for the handler to be given.
     * example:
     * <pre>
     * // on a RecvPacket listener, replay a copy of a chat message that just arrived
     * JsMacros.on("RecvPacket", JavaWrapper.methodToJava(function (event) {
     *   const packet = event.packet;
     *   if (event.type === "ChatMessageS2CPacket") {
     *     if (packet !== null) {
     *       Client.receivePacket(packet);
     *     }
     *   }
     * }));
     * </pre>
     *
     * @param packet the serverbound packet to hand to the client
     * @see #createPacketByteBuffer()
     * @since 1.8.4
     */
    public void receivePacket(Packet<ClientGamePacketListener> packet) {
        packet.handle(mc.getConnection());
    }

    /**
     * what is on the system clipboard.
     * <p>
     * This reads the real clipboard of the window the game is in, so it is shared with
     * everything else on the desktop rather than being a private copy. The GLFW backend holds
     * the contents itself and hands them over on request, so this is a real read rather than a
     * cached value.
     * example:
     * <pre>
     * const pasted = Client.getClipboard();
     * if (pasted !== "") {
     *   Chat.log(`clipboard holds ${pasted.length} characters`);
     * }
     * </pre>
     *
     * @return the current system clipboard contents
     * @since 2.0.0
     */
    public String getClipboard() {
        return mc.keyboardHandler.getClipboard();
    }

    /**
     * put a string on the system clipboard.
     * <p>
     * This replaces the clipboard of the whole desktop, so anything the player had copied is
     * gone afterwards, and it is the same clipboard {@link #getClipboard()} reads.
     * example:
     * <pre>
     * // copy a coordinate to the clipboard so it can be pasted elsewhere
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   Client.setClipboard(`${player.getX()}, ${player.getY()}, ${player.getZ()}`);
     * }
     * </pre>
     *
     * @param text the string to put on the clipboard, replacing whatever was there
     * @since 2.0.0
     */
    public void setClipboard(String text) {
        mc.keyboardHandler.setClipboard(text);
    }

}
