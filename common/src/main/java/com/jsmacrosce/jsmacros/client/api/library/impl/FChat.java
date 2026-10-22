package com.jsmacrosce.jsmacros.client.api.library.impl;

import com.google.gson.JsonParseException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtGrammar;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jsmacrosce.jsmacros.client.JsMacros;
import com.jsmacrosce.jsmacros.client.access.IChatHud;
import com.jsmacrosce.jsmacros.client.api.classes.TextBuilder;
import com.jsmacrosce.jsmacros.client.api.classes.inventory.ChatHistoryManager;
import com.jsmacrosce.jsmacros.client.api.classes.inventory.CommandBuilder;
import com.jsmacrosce.jsmacros.client.api.classes.inventory.CommandManager;
import com.jsmacrosce.jsmacros.client.api.helper.CommandNodeHelper;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.library.BaseLibrary;
import com.jsmacrosce.jsmacros.core.library.Library;

import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Functions for interacting with chat.
 * <p>
 * An instance of this class is passed to scripts as the {@code Chat} variable.
 * <br>
 * The name covers more than the chat box. It is the one place that talks to the player rather
 * than to the server or to the world, and it splits into four fairly separate groups.
 * {@link #log(Object)} and its {@code logf} and {@code logColor} siblings put a line in the
 * client's own chat history, which the player sees and which no server ever hears;
 * {@link #say(String)} and {@code sayf} send a line to the server, and a line beginning with a
 * slash is sent as a command rather than as chat. The overlays are {@link #title(Object, Object,
 * int, int, int)}, {@link #actionbar(Object)} and {@link #toast(Object, Object)}, which are the
 * three things the game can put on screen without a screen being open. Then there is the text
 * itself: {@link #createTextHelperFromString(String)}, {@link #createTextBuilder()} and
 * {@link #createTextHelperFromTranslationKey(String, Object...)} build the rich text object that
 * the display calls above take, and {@link #sectionSymbolToAmpersand(String)},
 * {@link #ampersandToSectionSymbol(String)} and {@link #stripFormatting(String)} are about the
 * formatting codes inside a plain string.
 * <br>
 * The distinction between logging and saying is the one that matters most. {@code log} is
 * purely local and always safe; {@code say} goes out to everyone else on the server, and it does
 * nothing at all if nothing is connected, so it wants a check that a world is loaded first.
 * <br>
 * The display calls take an {@code Object} rather than a string, and the type is what decides
 * how the value is rendered: a {@code TextHelper} or a {@code TextBuilder} is used as the rich
 * text it is, and anything else is converted with {@code toString()}, which for a number or a
 * boolean means the plain text of it. A {@code null} is handled per call rather than uniformly:
 * {@code log} ignores it, {@code title} treats it as "leave that part alone".
 * example:
 * <pre>
 * // logging is local, so it needs nothing checked first
 * Chat.log("a note to myself");
 * Chat.logf("%d blocks walked", 12);
 *
 * // a coloured line: logColor takes the ampersand codes and converts them.
 * // the ampersand is written as the escape \x26 here so this documentation renders
 * // it literally, in a script you would just write an ampersand followed by an a
 * Chat.logColor("\x26agreen line");
 *
 * // saying goes to the server, so only do it in a world
 * if (World.isWorldLoaded()) {
 *   Chat.say("hello everyone");
 * }
 *
 * // the three screen overlays
 * Chat.title("Title", "subtitle", 10, 70, 20);
 * Chat.actionbar("a line above the hotbar");
 * Chat.toast("Notice", "something worth reading", 4000);
 * </pre>
 *
 * @author Wagyourtail
 */
@Library("Chat")
@SuppressWarnings("unused")
public class FChat extends BaseLibrary {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final Grammar<Tag> nbtParser = SnbtGrammar.createParser(NbtOps.INSTANCE);

    public FChat(Core<?, ?> runner) {
        super(runner);
    }

    /**
     * Log to player chat.
     * <p>
     * This adds a line to the client's own chat, which the player sees and which is never sent
     * to the server. A {@code null} is silently ignored rather than logged, and the type of what
     * is passed decides how it renders: a {@code TextHelper} keeps its colours and styling, a
     * {@code TextBuilder} is built and then treated that way, and anything else goes through
     * {@code toString()}.
     * <p>
     * The line is put on the main thread, so this returns once the message is queued rather than
     * once it is on screen. Pass {@code true} as the second argument if the next line of the
     * script depends on the message being there.
     * example:
     * <pre>
     * // a plain string, a number, and a value whose toString is what you want
     * Chat.log("a note to myself");
     * Chat.log(42);
     * </pre>
     *
     * @param message the message to log, or {@code null} to do nothing
     * @throws InterruptedException if the script is interrupted while waiting for the message
     * @since 1.1.3
     */
    public void log(@Nullable Object message) throws InterruptedException {
        log(message, false);
    }

    /**
     * puts a line in the client's own chat and optionally waits for it to be queued.
     * <p>
     * A {@code null} is silently ignored. A {@code TextHelper} is used as the rich text it is,
     * a {@code TextBuilder} is built first, and anything else is converted with
     * {@code toString()}. When {@code await} is {@code true} this blocks until the line has
     * actually been handed to the chat, which is what a script wants when the ordering of two
     * lines matters.
     * example:
     * <pre>
     * // wait for each of these to be in the chat before the next one goes in
     * Chat.log("first", true);
     * Chat.log("second", true);
     * </pre>
     *
     * @param message the message to log, or {@code null} to do nothing
     * @param await   should wait for message to actually be sent to chat to continue.
     * @throws InterruptedException if the script is interrupted while waiting
     * @since 1.1.3
     */
    public void log(@Nullable Object message, boolean await) throws InterruptedException {
        if (message == null) {
            return;
        }
        final Object message2 = message instanceof TextHelper ? message :
                message instanceof TextBuilder ? ((TextBuilder) message).build() :
                        message.toString();

        if (runner.profile.checkJoinedThreadStack()) {
            if (message2 instanceof TextHelper) {
                logInternal((TextHelper) message2);
            } else {
                logInternal((String) message2);
            }
        } else {
            final Semaphore semaphore = new Semaphore(await ? 0 : 1);
            mc.execute(() -> {
                if (message2 instanceof TextHelper) {
                    logInternal((TextHelper) message2);
                } else {
                    logInternal((String) message2);
                }
                semaphore.release();
            });
            semaphore.acquire();
        }
    }

    /**
     * Logs the formatted message to the player's chat. The message is formatted using the default
     * java {@link String#format(String, Object...)} syntax.
     * <p>
     * The formatting happens here, before anything is queued, so a format code that does not
     * match the arguments raises here rather than quietly printing something odd. The result is
     * logged the way {@link #log(Object)} logs, so a line containing section symbol codes comes
     * out coloured.
     * example:
     * <pre>
     * // %s takes anything, %d wants a whole number
     * Chat.logf("%s is at %d, %d, %d", "the player", 12, 64, -30);
     * </pre>
     *
     * @param message the message to format and log
     * @param args    the arguments used to format the message
     * @throws InterruptedException if the script is interrupted while waiting for the message
     * @since 1.8.4
     */
    public void logf(String message, Object... args) throws InterruptedException {
        log(String.format(message, args), false);
    }

    /**
     * Logs the formatted message to the player's chat. The message is formatted using the default
     * java {@link String#format(String, Object...)} syntax.
     * <p>
     * The formatting happens here, before anything is queued. Pass {@code true} for
     * {@code await} to block until the formatted line has been handed to the chat.
     * example:
     * <pre>
     * Chat.logf("%d of %d", 3, 10, true);
     * </pre>
     *
     * @param message the message to format and log
     * @param await   whether to wait for message to be sent to chat before continuing
     * @param args    the arguments used to format the message
     * @throws InterruptedException if the script is interrupted while waiting
     * @since 1.8.4
     */
    public void logf(String message, boolean await, Object... args) throws InterruptedException {
        log(String.format(message, args), await);
    }

    /**
     * log with auto wrapping with {@link #ampersandToSectionSymbol(String)}
     * <p>
     * This is {@link #log(Object)} on a string whose ampersand codes have first been turned into
     * the section symbol codes the game itself understands, so {@code &a} comes out green.
     * The conversion is done by {@link #ampersandToSectionSymbol(String)}, which means a literal
     * ampersand in ordinary text is read as the start of a code and has to be doubled to
     * survive; see that method for what it does with each form.
     * example:
     * <pre>
     * // the usual ampersand codes, converted on the way in. the ampersand is written
     * // as the escape \x26 here so this documentation renders it literally
     * Chat.logColor("\x26l bold, \x26r back to normal");
     * </pre>
     *
     * @since 1.9.0
     * @param message the message to log, with ampersand codes
     * @throws InterruptedException if the script is interrupted while waiting for the message
     */
    public void logColor(String message) throws InterruptedException {
        log(ampersandToSectionSymbol(message), false);
    }

    /**
     * log with auto wrapping with {@link #ampersandToSectionSymbol(String)}
     * <p>
     * As the single argument form, but this one can wait for the converted line to be handed to
     * the chat. The ampersand is written as the escape {@code \x26} here so this documentation
     * renders it literally.
     * example:
     * <pre>
     * Chat.logColor("\x26c red", true);
     * </pre>
     *
     * @since 1.9.0
     * @param message the message to log, with ampersand codes
     * @param await   whether to wait for the message to be sent to chat before continuing
     * @throws InterruptedException if the script is interrupted while waiting
     */
    public void logColor(String message, boolean await) throws InterruptedException {
        log(ampersandToSectionSymbol(message), await);
    }

    private static void logInternal(String message) {
        if (message != null) {
            Component text = Component.literal(message);
            ((IChatHud) mc.gui.getChat()).jsmacros_addMessageBypass(text);
        }
    }

    private static void logInternal(TextHelper text) {
        Minecraft mc = Minecraft.getInstance();
        ((IChatHud) mc.gui.getChat()).jsmacros_addMessageBypass(text.getRaw());
    }

    /**
     * Say to server as player.
     * <p>
     * This is the message other players see, so unlike {@link #log(Object)} it goes out to
     * everyone. A line beginning with a slash is sent as a command rather than as chat, with the
     * slash taken off, which is the same thing typing it into the chat box would do. A
     * {@code null} is ignored, and nothing is sent at all when nothing is connected, so this
     * wants a check that a world is loaded.
     * example:
     * <pre>
     * if (World.isWorldLoaded()) {
     *   Chat.say("hello everyone");
     *   // a leading slash runs a command instead
     *   Chat.say("/list");
     * }
     * </pre>
     *
     * @param message the line to send, with a leading slash meaning a command
     * @since 1.0.0
     */
    public void say(@Nullable String message) throws InterruptedException {
        say(message, false);
    }

    /**
     * Say to server as player.
     * <p>
     * A {@code null} is ignored. A line beginning with a slash is sent as a command. When
     * {@code await} is {@code true} this blocks until the line has been handed to the connection
     * rather than returning as soon as it is queued.
     * example:
     * <pre>
     * if (World.isWorldLoaded()) {
     *   Chat.say("/tp 0 64 0", true);
     * }
     * </pre>
     *
     * @param message the line to send, with a leading slash meaning a command
     * @param await   whether to wait for the message to be sent to the server before continuing
     * @throws InterruptedException if the script is interrupted while waiting
     * @since 1.3.1
     */
    public void say(@Nullable String message, boolean await) throws InterruptedException {
        if (message == null) {
            return;
        }
        if (runner.profile.checkJoinedThreadStack()) {
            assert mc.player != null;
            sayInternal(message);
        } else {
            final Semaphore semaphore = new Semaphore(await ? 0 : 1);
            mc.execute(() -> {
                assert mc.player != null;
                sayInternal(message);
                semaphore.release();
            });
            semaphore.acquire();
        }
    }

    private void sayInternal(String message) {
        if (message.startsWith("/")) {
            mc.getConnection().sendCommand(message.substring(1));
        } else {
            mc.getConnection().sendChat(message);
        }
    }

    /**
     * Sends the formatted message to the server. The message is formatted using the default java
     * {@link String#format(String, Object...)} syntax.
     *
     * @param message the message to format and send to the server
     * @param args    the arguments used to format the message
     * @throws InterruptedException if the script is interrupted while waiting
     * @since 1.8.4
     */
    public void sayf(String message, Object... args) throws InterruptedException {
        say(String.format(message, args), false);
    }

    /**
     * Sends the formatted message to the server. The message is formatted using the default java
     * {@link String#format(String, Object...)} syntax.
     *
     * @param message the message to format and send to the server
     * @param await   whether to wait for message to be sent to chat before continuing
     * @param args    the arguments used to format the message
     * @throws InterruptedException if the script is interrupted while waiting
     * @since 1.8.4
     */
    public void sayf(String message, boolean await, Object... args) throws InterruptedException {
        say(String.format(message, args), await);
    }

    /**
     * open the chat input box with specific text already typed.
     * <p>
     * This puts the real chat screen up with the text already in it, so the player is left to
     * press enter, and the cursor is placed where a slash would be for a line starting with one.
     * A {@code null} opens it empty. This cannot be called from a thread joined to the main one,
     * because opening a screen is what the main thread is not expecting to be asked for.
     * example:
     * <pre>
     * // open the chat box with a command ready to go
     * Chat.open("/say ");
     * </pre>
     *
     * @param message the message to start the chat screen with
     * @throws InterruptedException if the script is interrupted while waiting
     * @since 1.6.4
     */
    public void open(@Nullable String message) throws InterruptedException {
        open(message, false);
    }

    /**
     * open the chat input box with specific text already typed.
     * hint: you can combine with {@link com.jsmacrosce.jsmacros.core.library.impl.FJsMacros#waitForEvent(String)} or
     * {@link com.jsmacrosce.jsmacros.core.library.impl.FJsMacros#once(String, MethodWrapper)} to wait for the chat screen
     * to close and/or the to wait for the sent message
     * <p>
     * A {@code null} opens the box empty. Pass {@code true} for {@code await} to block until the
     * screen is actually up, which is what to do before anything that depends on it. This
     * raises {@link UnsupportedOperationException} when called from a thread joined to the main
     * one, since the main thread cannot wait for itself to open a screen.
     * example:
     * <pre>
     * // open it and wait for it to be up, then wait for it to be closed again
     * Chat.open("/summon ", true);
     * JsMacros.waitForEvent("OpenScreen");
     * </pre>
     *
     * @param message the message to start the chat screen with
     * @param await   whether to wait for the screen to be open before continuing
     * @throws InterruptedException if the script is interrupted while waiting
     * @throws UnsupportedOperationException if called from a thread joined to the main thread
     * @since 1.6.4
     */
    public void open(@Nullable String message, boolean await) throws InterruptedException {
        if (message == null) {
            message = "";
        }
        if (runner.profile.checkJoinedThreadStack()) {
            throw new UnsupportedOperationException("Cannot open a screen while joined to the main thread");
        } else {
            String finalMessage = message;
            final Semaphore semaphore = new Semaphore(await ? 0 : 1);
            mc.execute(() -> {
                //? if >1.21.8 {
                /*mc.setScreen(new ChatScreen(finalMessage, true));
                *///?} else {
                mc.setScreen(new ChatScreen(finalMessage));
                //?}
                semaphore.release();
            });
            semaphore.acquire();
        }
    }

    /**
     * Display a Title to the player.
     * <p>
     * This is the large text in the middle of the screen with a smaller line under it, the same
     * one a boss or an advancement uses. A {@code TextHelper} is used as the rich text it is and
     * anything else is converted with {@code toString()}.
     * <p>
     * A {@code null} is not an error but it is not symmetric either: passing {@code null} for
     * one of the two leaves that one alone, while passing {@code null} for <em>both</em> clears
     * the title and the subtitle. The three timings are in ticks, twenty to the second, and they
     * are applied every time even when a part was left alone.
     * example:
     * <pre>
     * // fade in for half a second, hold for three and a half, fade out over a second
     * Chat.title("Title", "and a subtitle", 10, 70, 20);
     *
     * // and clear it again
     * Chat.title(null, null, 0, 0, 0);
     * </pre>
     *
     * @param title    the main line, or {@code null} to leave the current one
     * @param subtitle the smaller line under it, or {@code null} to leave the current one
     * @param fadeIn   ticks spent fading the text in
     * @param remain   ticks the text is held fully visible for
     * @param fadeOut  ticks spent fading the text out
     * @since 1.2.1
     */
    public void title(Object title, Object subtitle, int fadeIn, int remain, int fadeOut) {
        Component titlee = null;
        Component subtitlee = null;
        if (title instanceof TextHelper) {
            titlee = ((TextHelper) title).getRaw();
        } else if (title != null) {
            titlee = Component.literal(title.toString());
        }
        if (subtitle instanceof TextHelper) {
            subtitlee = ((TextHelper) subtitle).getRaw();
        } else if (subtitle != null) {
            subtitlee = Component.literal(subtitle.toString());
        }
        if (title != null) {
            mc.gui.setTitle(titlee);
        }
        if (subtitle != null) {
            mc.gui.setSubtitle(subtitlee);
        }
        if (title == null && subtitle == null) {
            mc.gui.setTitle(null);
            mc.gui.setSubtitle(null);
        }
        mc.gui.setTimes(fadeIn, remain, fadeOut);
    }

    /**
     * Display a line just above the hotbar.
     * <p>
     * This is the small text in the middle of the screen that a {@link #title(Object, Object,
     * int, int, int)} subtitle sits above, the one a block break progress or a waypoint message
     * uses. A {@code TextHelper} is used as the rich text it is and anything else is converted
     * with {@code toString()}.
     * example:
     * <pre>
     * const under = World.getBlock(0, 63, 0);
     * if (under !== null) {
     *   Chat.actionbar("standing on " + under.getName());
     * }
     * </pre>
     *
     * @param text the line to show, or {@code null} to clear it
     * @since 1.8.1
     */
    public void actionbar(Object text) {
        actionbar(text, false);
    }

    /**
     * Display the smaller title that's above the actionbar.
     * <p>
     * A {@code TextHelper} is used as the rich text it is and anything else is converted with
     * {@code toString()}. The {@code tinted} argument decides whether the text is drawn in the
     * game's background colour, which keeps it readable over a bright sky, or left alone.
     * example:
     * <pre>
     * Chat.actionbar("tinted for readability", true);
     * </pre>
     *
     * @param text   the line to show, or {@code null} to clear it
     * @param tinted whether to draw the line over a background tint
     * @since 1.2.1
     */
    public void actionbar(Object text, boolean tinted) {
        assert mc.gui != null;
        Component textt = null;
        if (text instanceof TextHelper) {
            textt = ((TextHelper) text).getRaw();
        } else if (text != null) {
            textt = Component.literal(text.toString());
        }
        mc.gui.setOverlayMessage(textt, tinted);
    }

    /**
     * Display a toast.
     * <p>
     * A toast is the small box in the top left corner that the game uses for things like
     * recipe unlocks. This is the game's own periodic notification toast, so how long it stays
     * up is whatever that one is set to and not something chosen here; the three argument
     * {@link #toast(Object, Object, long)} is the one to reach for when the duration matters.
     * <p>
     * A {@code TextHelper} is used as the rich text it is and anything else is converted with
     * {@code toString()}. A {@code null} title means nothing is shown at all, so this is the
     * natural way to skip the call.
     * example:
     * <pre>
     * Chat.toast("Unlocked", "a new recipe");
     * </pre>
     *
     * @param title the bold line of the toast, or {@code null} to show nothing
     * @param desc  the smaller description under it, or {@code null} for none
     * @since 1.2.5
     */
    public void toast(Object title, Object desc) {
        ToastManager t = mc.getToastManager();
        if (t != null) {
            Component titlee = (title instanceof TextHelper) ? ((TextHelper) title).getRaw() : title != null ? Component.literal(title.toString()) : null;
            Component descc = (desc instanceof TextHelper) ? ((TextHelper) desc).getRaw() : desc != null ? Component.literal(desc.toString()) : null;
            // There doesn't seem to be a difference in the appearance or the functionality except for the UNSECURE_SERVER_WARNING with a longer duration
            if (titlee != null) {
                t.addToast(SystemToast.multiline(mc, SystemToast.SystemToastId.PERIODIC_NOTIFICATION, titlee, descc));
            }
        }
    }

    /**
     * Display a toast.
     * <p>
     * This is the form to use when the duration matters: the toast is given its own lifetime in
     * milliseconds and comes down on its own. A {@code TextHelper} is used as the rich text it
     * is and anything else is converted with {@code toString()}. A {@code null} title means
     * nothing is shown, and if the game has no toast manager up yet this does nothing rather
     * than raising anything.
     * example:
     * <pre>
     * // something to read for four seconds
     * Chat.toast("Heads up", "the mob farm is done", 4000);
     * </pre>
     *
     * @param title the bold line of the toast, or {@code null} to show nothing
     * @param desc  the smaller description under it, or {@code null} for none
     * @param displayTimeMs The amount of time in milliseconds to display the toast
     * @since 1.2.5
     */
    public void toast(Object title, Object desc, long displayTimeMs) {
        ToastManager t = mc.getToastManager();
        // TODO: Are scripts run before Minecraft is properly instantiated anyway? Even if not wouldn't the line above
        //  throw an error because mc is null?
        if (t == null) {
            // TODO: Throw?
            return;
        }

        Component titleComp = (title instanceof TextHelper) ? ((TextHelper) title).getRaw() : title != null ? Component.literal(title.toString()) : null;
        Component descComp = (desc instanceof TextHelper) ? ((TextHelper) desc).getRaw() : desc != null ? Component.literal(desc.toString()) : null;
        // There doesn't seem to be a difference in the appearance or the functionality except for the UNSECURE_SERVER_WARNING with a longer duration
        if (titleComp != null) {
            t.addToast(SystemToast.multiline(
                    mc,
                    new SystemToast.SystemToastId(displayTimeMs),
                    titleComp,
                    descComp
            ));
        }
    }

    /**
     * Creates a {@link TextHelper TextHelper} for use where you need one and not a string.
     * <p>
     * A {@code TextHelper} is the game's own text object rather than a plain string, so it can
     * carry colours, styles, hover text and click actions, and it is what the display calls such
     * as {@link #title(Object, Object, int, int, int)} and {@link #actionbar(Object)} take
     * instead of a string. This makes one from a plain literal, and
     * {@link #createTextBuilder()} makes one that can be styled piece by piece.
     * <p>
     * Any section symbol codes in the string are interpreted, so a string containing them comes
     * out coloured rather than showing the codes.
     * example:
     * <pre>
     * const text = Chat.createTextHelperFromString("a styled line");
     * Chat.title(text, null, 5, 40, 10);
     * </pre>
     *
     * @param content the literal text, with section symbol codes interpreted
     * @return a new {@link TextHelper TextHelper}
     * @see TextHelper
     * @since 1.1.3
     */
    public TextHelper createTextHelperFromString(String content) {
        return TextHelper.wrap(Component.literal(content));
    }

    /**
     * a {@link TextHelper} built from a translation key and its arguments.
     * <p>
     * The text is the game's own translated string for the key, with the arguments filled in,
     * so it follows whatever language the game is set to rather than being fixed text. This is
     * how the game's own messages are built, and it is what a script wants when its output
     * should be readable in the player's language.<br>
     * A key the game does not know is not an error, it simply renders as the key itself, so a
     * mistyped key shows up in the output rather than coming out blank.
     * example:
     * <pre>
     * // the game's own wording for a block, in the player's language
     * Chat.actionbar(Chat.createTextHelperFromTranslationKey("block", "minecraft", "stone"));
     * </pre>
     *
     * @param key     the translation key to look up
     * @param content the arguments to fill into the translated string
     * @return a new {@link TextHelper TextHelper}
     * @since 1.9.0
     */
    public TextHelper createTextHelperFromTranslationKey(String key, Object... content) {
        return TextHelper.wrap(Component.translatable(key, content));
    }

    /**
     * JsMacros' own logger, for the game log and the JsMacros log file.
     * <p>
     * This writes wherever the game itself writes its log, so it ends up in the log file and in
     * the console and nowhere the player can see. It is the right one for a script debugging
     * itself; {@link #log(Object)} is the right one for anything meant to be read.
     * example:
     * <pre>
     * Chat.getLogger().info("the macro reached this point");
     * </pre>
     *
     * @return the slf4j logger JsMacros itself uses
     * @since 1.5.2
     */
    public Logger getLogger() {
        return JsMacros.LOGGER;
    }

    /**
     * returns a log4j logger, for logging to console only.
     * <p>
     * This is a logger under a name of the script's choosing, so several scripts can keep their
     * output apart in the game log. It goes to the same places as the game's own log and is
     * never shown to the player.
     * example:
     * <pre>
     * const log = Chat.getLogger("my macro");
     * log.info("started");
     * log.warn("nothing loaded yet");
     * </pre>
     *
     * @param name the name to log under, which is how the entries are told apart
     * @return an slf4j logger under that name
     * @since 1.5.2
     */
    public Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }

    /**
     * Create a  {@link TextHelper TextHelper} for use where you need one and not a string.
     * <p>
     * The argument is SNBT, the named binary text form the command line uses, in the shape of a
     * text component, so this is the round trip partner of
     * {@link TextHelper#getJson() getJson()}. Reading it back is a strict parse and is not
     * forgiving: anything malformed raises rather than producing a partial text, which includes
     * a string that is valid JSON but not a component.
     * <p>
     * Note that despite the name this is not the game's JSON chat format, and it never returns
     * {@code null}: it either hands back the text it read or raises.
     * example:
     * <pre>
     * // rebuild text from the SNBT form, which is what a saved config file holds
     * const text = Chat.createTextHelperFromJSON('{text:"hello",color:"red"}');
     * Chat.actionbar(text);
     * </pre>
     *
     * @param json the SNBT form of a text component
     * @return a new {@link TextHelper TextHelper}
     * @throws RuntimeException if the string is not a well formed text component
     * @see TextHelper
     * @since 1.1.3
     */
    @Nullable
    public TextHelper createTextHelperFromJSON(String json) {
        try {
            var nbt = nbtParser.parseForCommands(new StringReader(json));
            return TextHelper.wrap(ComponentSerialization.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow());
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * a builder for rich text, built a piece at a time.
     * <p>
     * This is the way to make a {@code TextHelper} that has more than one style in it: a run of
     * one colour, then a run of another, each part added and styled separately. The builder is
     * finished with {@code build()}, and what it produces is what the display calls such as
     * {@link #title(Object, Object, int, int, int)} and {@link #actionbar(Object)} take.
     * <p>
     * {@link #createTextHelperFromString(String)} is the shortcut for a single run of text with
     * no styling to do.
     * example:
     * <pre>
     * // a line with two colours in it: the styling applies to what comes after it
     * const text = Chat.createTextBuilder()
     *   .append("normal then ")
     *   .withColor(0xFFFF5555)
     *   .append("red")
     *   .build();
     * Chat.actionbar(text);
     * </pre>
     *
     * @return a new empty builder
     * @see TextBuilder
     * @since 1.3.0
     */
    public TextBuilder createTextBuilder() {
        return new TextBuilder();
    }

    /**
     * a builder for a command the script registers.
     * <p>
     * The name is the command's name as typed, without a slash. This is the same builder
     * {@link #getCommandManager()} hands out, and it is the older way to reach it.
     * example:
     * <pre>
     * const command = Chat.createCommandBuilder("hello")
     *   .literalArg("world")
     *   .executes(JavaWrapper.methodToJava(function (context) {
     *     Chat.log("hello world");
     *   }));
     * command.register();
     * </pre>
     *
     * @param name name of command
     * @return a builder for a command with that name
     * @see #getCommandManager()
     * @since 1.4.2
     */
    @Deprecated
    public CommandBuilder createCommandBuilder(String name) {
        return CommandManager.instance.createCommandBuilder(name);
    }

    /**
     * takes a command the script registered back out.
     * <p>
     * This returns the node it was built from, which is what {@link #reRegisterCommand} takes
     * again, so a script can unregister and put the same command back later. This is the older
     * way to reach it.
     * example:
     * <pre>
     * const node = Chat.unregisterCommand("hello");
     * Chat.reRegisterCommand(node);
     * </pre>
     *
     * @param name the name of the command to take out, without a slash
     * @return the node the command was built from
     * @throws IllegalAccessException if the command cannot be reached for removal
     * @see #getCommandManager()
     * @since 1.6.5
     */
    @Deprecated
    public CommandNodeHelper unregisterCommand(String name) throws IllegalAccessException {
        return CommandManager.instance.unregisterCommand(name);
    }

    /**
     * puts a command back after {@link #unregisterCommand(String)} has taken it out.
     * <p>
     * This is the older way to reach it.
     *
     * @param node the node {@link #unregisterCommand(String)} handed back
     * @see #getCommandManager()
     * @since 1.6.5
     */
    @Deprecated
    public void reRegisterCommand(CommandNodeHelper node) {
        CommandManager.instance.reRegisterCommand(node);
    }

    /**
     * the manager for commands the script registers.
     * <p>
     * This is the modern way to reach what {@link #createCommandBuilder(String)},
     * {@link #unregisterCommand(String)} and {@link #reRegisterCommand(CommandNodeHelper)} do,
     * and it also lists the commands the server has and asks for their autocompletion, which
     * the older calls do not.
     * example:
     * <pre>
     * // what the server says we can run
     * for (const command of Chat.getCommandManager().getValidCommands()) {
     *   Chat.log(`/${command}`);
     * }
     * </pre>
     *
     * @return the shared command manager
     * @since 1.7.0
     */
    public CommandManager getCommandManager() {
        return CommandManager.instance;
    }

    /**
     * the chat lines currently on screen, and what has been sent.
     * <p>
     * The manager is made fresh on every call, so two calls hand back two objects that both
     * look at the same chat. The received side is what the client has been shown, which is not
     * quite the same as what the server sent: JsMacros' own {@link #log(Object)} lines are in
     * there too, and a line that has scrolled off is not. Every read and write on it waits for
     * the main thread, so it is safe to call from a listener but it does block.
     * <p>
     * This is a script editing what is on screen, not a log of the whole session; the sent side
     * is the client's own history of what the player typed.
     * example:
     * <pre>
     * const history = Chat.getHistory();
     * // how many lines are up right now
     * Chat.log(`${history.getRecvCount()} lines in chat`);
     * // and take one away again
     * history.removeRecvText(0, true);
     * </pre>
     *
     * @return a manager over the chat lines currently on screen
     * @since 1.7.0
     */
    public ChatHistoryManager getHistory() {
        return new ChatHistoryManager(mc.gui.getChat());
    }

    /**
     * how wide a line of text would draw.
     * <p>
     * This is the width in the same scaled screen pixels a 2D overlay uses, and it accounts for
     * the game's own font, so a string with section symbol codes measures the width of the text
     * and not of the codes. A {@code null} measures {@code 0}. This is what a script needs
     * before deciding where to put something, and it is the honest way to centre text.
     * example:
     * <pre>
     * // centre a line of text on screen
     * const label = "centred";
     * const draw = Hud.createDraw2D();
     * draw.addText(label, Math.round((Hud.getWindowWidth() - Chat.getTextWidth(label)) / 2), 20, 0xFFFFFFFF, true);
     * draw.register();
     * </pre>
     *
     * @param text the text to get the width of, or {@code null} for zero
     * @return the width of the given text in pixels.
     * @since 1.8.4
     */
    public int getTextWidth(@Nullable String text) {
        return mc.font.width(text);
    }

    private static final Pattern SECTION_SYMBOL_PATTERN = Pattern.compile("[§&]");

    /**
     * escapes {@code &} to {@code &&} since 1.9.0
     * <p>
     * This is the opposite of {@link #ampersandToSectionSymbol(String)}: it takes text the game
     * has already rendered and turns it back into the ampersand form a config file would hold.
     * A section symbol becomes a single ampersand and a literal ampersand becomes a doubled
     * one, so the result can go through
     * {@link #ampersandToSectionSymbol(String)} again and come back where it started.<br>
     * Only the two character codes are handled. A hex colour, which is a longer run beginning
     * with a section symbol and a hash, is left alone rather than being converted.<br>
     * The ampersand is written as the escape {@code \x26} in the example below so this
     * documentation renders it literally; in a script it would just be written out.
     * example:
     * <pre>
     * // the ampersand form of a coloured string, for writing to a config file
     * const asFile = Chat.sectionSymbolToAmpersand(Chat.ampersandToSectionSymbol("\x26c red"));
     * </pre>
     *
     * @param string the string to convert
     * @return § -> {@code &}
     * @since 1.6.5
     */
    public String sectionSymbolToAmpersand(String string) {
        StringBuilder sb = new StringBuilder();
        Matcher m = SECTION_SYMBOL_PATTERN.matcher(string);
        while (m.find()) {
            if (m.group().equals("§"))
                m.appendReplacement(sb, "&");
            else
                m.appendReplacement(sb, "&&");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static final Pattern AMPERSAND_PATTERN = Pattern.compile("&(.)");

    /**
     * escapes {@code &&} to {@code &} since 1.9.0
     * <p>
     * This is what turns the config file form of a colour code into the one the game reads. An
     * ampersand followed by a character becomes a section symbol followed by that same
     * character, so the code means the same thing afterwards.<br>
     * A doubled ampersand is the escape, and it collapses to a single literal ampersand that
     * survives the conversion. This matters more than it sounds: the match is an ampersand and
     * <em>any</em> character after it, so an ampersand in ordinary text is read as the start of
     * a code and the letter after it is swallowed into a code the game may not recognise. A
     * literal ampersand therefore has to be doubled, and an ampersand at the very end of the
     * string has nothing after it to match and is left alone.<br>
     * {@link #logColor(String)} is this conversion applied to a logged line.<br>
     * The ampersands in the example below are written as the escape {@code \x26} so this
     * documentation renders them literally.
     * example:
     * <pre>
     * // a doubled ampersand is a literal one, a single one starts a code
     * const plain = Chat.ampersandToSectionSymbol("R\x26\x26D is a company, R\x26cD is not");
     * Chat.log(plain);
     * </pre>
     *
     * @param string the string to convert
     * @return {@code &} -> §
     * @since 1.6.5
     */
    public String ampersandToSectionSymbol(String string) {
        StringBuilder sb = new StringBuilder();
        Matcher m = AMPERSAND_PATTERN.matcher(string);
        while (m.find()) {
            if (m.group().equals("&&"))
                m.appendReplacement(sb, "&");
            else
                m.appendReplacement(sb, "§$1");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * takes the colour and style codes out of a string, leaving the plain text.
     * <p>
     * This works on a plain string and is the cheap way to get readable text out of something
     * that is full of codes. For a {@code TextHelper},
     * {@link TextHelper#getStringStripFormatting() getStringStripFormatting()} does the same
     * thing and walks the actual styled text rather than a pattern.<br>
     * It matches the two character codes, which is every code the game has had up to and
     * including hex colours, so all of them go. It does <em>not</em> match the longer hex
     * colour form, so a colour written that way is left in the output, codes and all. A
     * {@code null} is not handled and raises.<br>
     * The ampersands below are written as the escape {@code \x26} so this documentation
     * renders them literally.
     * example:
     * <pre>
     * // write to a log file without the codes in it
     * const plain = Chat.stripFormatting(Chat.ampersandToSectionSymbol("\x26c red \x26r and normal"));
     * Chat.getLogger("my macro").info(plain);
     * </pre>
     *
     * @param string the string to strip, which must not be {@code null}
     * @return the same string with the formatting codes removed
     * @since 1.6.5
     */
    public String stripFormatting(String string) {
        // on 1.15 and lower switch to comment
//        return string.replaceAll("§#\\d{6}|§.", "");
        return TextHelper.STRIP_FORMATTING_PATTERN.matcher(string).replaceAll("");
    }

}
