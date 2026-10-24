package com.jsmacrosce.jsmacros.client.api.library.impl;

import com.jsmacrosce.jsmacros.client.util.GuiCompat;
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
import com.jsmacrosce.doclet.DocletIgnore;
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
 * Provides client chat, server messaging, overlay display, and text-formatting functions to
 * scripts as the {@code Chat} variable. {@link #log(Object)} writes locally; {@link #say(String)}
 * sends a message or command to the server.
 * <pre>
 * Chat.log("a note to myself");
 *
 * Chat.logColor("\x26agreen line");
 *
 * if (World.isWorldLoaded()) {
 *   Chat.say("hello everyone");
 * }
 *
 * </pre>
 *
 * @author Wagyourtail
 */
@Library("Chat")
@SuppressWarnings("unused")
public class FChat extends BaseLibrary {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final Grammar<Tag> nbtParser = SnbtGrammar.createParser(NbtOps.INSTANCE);

    /**
     * Associates this Chat library with the core whose profile is consulted for joined-thread checks by some chat operations.
     *
     * @param runner the core associated with this library
     */
    public FChat(Core<?, ?> runner) {
        super(runner);
    }

    /**
     * Adds a message to the client's chat without sending it to the server. A {@code null} is
     * ignored; text helpers preserve rich formatting and other values are converted with
     * {@code toString()}. This overload does not wait for the chat task to run when called outside
     * a thread joined to Minecraft's main thread.
     *
     * @param message the message to log, or {@code null} to do nothing
     * @throws InterruptedException if the script is interrupted while waiting for the message
     * @since 1.1.3
     */
    public void log(@Nullable Object message) throws InterruptedException {
        log(message, false);
    }

    /**
     * Adds a message to the client's chat without sending it to the server. A {@code null} is
     * ignored; text helpers preserve rich formatting and other values are converted with
     * {@code toString()}. Outside a thread joined to Minecraft's main thread, {@code await=true}
     * waits for the scheduled task to finish; {@code await=false} returns without waiting for
     * that task to run.
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
     * Formatting occurs before the message is queued; the resulting string is passed to
     * {@link #log(Object)}.
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
     * <pre>
     * Chat.logf("%d of %d", true, 3, 10);
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
     * Logs text after converting ampersand codes to section-sign codes with
     * {@link #ampersandToSectionSymbol(String)}.
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
     * Logs converted text, with waiting behavior controlled by {@code await}.
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
            ((IChatHud) GuiCompat.hud(mc).getChat()).jsmacros_addMessageBypass(text);
        }
    }

    private static void logInternal(TextHelper text) {
        Minecraft mc = Minecraft.getInstance();
        ((IChatHud) GuiCompat.hud(mc).getChat()).jsmacros_addMessageBypass(text.getRaw());
    }

    /**
     * Sends a chat message or command as the player; a leading slash selects command sending.
     * A {@code null} message is ignored.
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
     * Sends a chat message or command as the player; a leading slash selects command sending.
     * A {@code null} message is ignored. Outside a thread joined to the main thread, sending is
     * scheduled on Minecraft's thread; {@code await} controls whether this call waits for that
     * task to run. On a joined thread, sending happens immediately regardless of {@code await}.
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
     * Formats a message with {@link String#format(String, Object...)} and sends it without
     * waiting for Minecraft's scheduled send task to run.
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
     * Formats a message with {@link String#format(String, Object...)} and sends it using the
     * waiting behavior of {@link #say(String, boolean)}.
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
     * Opens the chat screen with the supplied text, or empty when {@code message} is {@code null}.
     * The player must submit the text; outside a thread joined to the main thread, this overload
     * does not wait for the screen-opening task to run.
     *
     * @param message the message to start the chat screen with
     * @throws InterruptedException if the script is interrupted while waiting
     * @since 1.6.4
     */
    public void open(@Nullable String message) throws InterruptedException {
        open(message, false);
    }

    /**
     * Opens the chat screen with the supplied text, or empty when {@code message} is {@code null}.
     * When called outside a thread joined to the main thread, {@code await} determines whether
     * this call waits for the screen-opening task to run. A joined thread causes
     * {@link UnsupportedOperationException}.
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
                /*GuiCompat.setScreen(mc, new ChatScreen(finalMessage, true));
                *///?} else {
                GuiCompat.setScreen(mc, new ChatScreen(finalMessage));
                //?}
                semaphore.release();
            });
            semaphore.acquire();
        }
    }

    /**
     * Displays title and subtitle text, updating either independently.
     * <p>
     * Non-null values are used as rich text when they are {@code TextHelper}s and otherwise
     * converted to literal text. A null value leaves that component unchanged; passing null for
     * both clears both. Timing values are ticks and are always updated.
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
            GuiCompat.hud(mc).setTitle(titlee);
        }
        if (subtitle != null) {
            GuiCompat.hud(mc).setSubtitle(subtitlee);
        }
        if (title == null && subtitle == null) {
            GuiCompat.hud(mc).setTitle(null);
            GuiCompat.hud(mc).setSubtitle(null);
        }
        GuiCompat.hud(mc).setTimes(fadeIn, remain, fadeOut);
    }

    /**
     * Displays a message above the hotbar. A {@code TextHelper} is used as rich text; other
     * non-null values are converted to literal text. {@code null} clears the message.
     *
     * @param text the line to show, or {@code null} to clear it
     * @since 1.8.1
     */
    public void actionbar(Object text) {
        actionbar(text, false);
    }

    /**
     * Displays a message above the hotbar. A {@code TextHelper} is used as rich text; other
     * non-null values are converted to literal text. {@code null} clears the message; {@code tinted}
     * controls the overlay's color animation.
     *
     * @param text   the line to show, or {@code null} to clear it
     * @param tinted whether to animate the overlay message's color
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
        GuiCompat.hud(mc).setOverlayMessage(textt, tinted);
    }

    /**
     * Displays a periodic notification toast.
     * <p>
     * A {@code TextHelper} is used as rich text; other non-null values are converted to literal
     * text. A {@code null} title displays nothing. The game controls this toast's duration.
     *
     * @param title the bold line of the toast, or {@code null} to show nothing
     * @param desc  the smaller description under it, or {@code null} for none
     * @since 1.2.5
     */
    public void toast(Object title, Object desc) {
        ToastManager t = GuiCompat.toastManager(mc);
        if (t != null) {
            Component titlee = (title instanceof TextHelper) ? ((TextHelper) title).getRaw() : title != null ? Component.literal(title.toString()) : null;
            Component descc = (desc instanceof TextHelper) ? ((TextHelper) desc).getRaw() : desc != null ? Component.literal(desc.toString()) : null;
            // There doesn't seem to be a difference in the appearance or the functionality except for the UNSECURE_SERVER_WARNING with a longer duration
            if (titlee != null) {
                showSystemToast(t, SystemToast.SystemToastId.PERIODIC_NOTIFICATION, titlee, descc);
            }
        }
    }

    /**
     * Displays a toast with the supplied duration.
     * <p>
     * {@code displayTimeMs} is in milliseconds. A {@code TextHelper} is used as rich text; other
     * non-null values are converted to literal text. A {@code null} title displays nothing. If
     * no toast manager is available, this does nothing.
     *
     * @param title the bold line of the toast, or {@code null} to show nothing
     * @param desc  the smaller description under it, or {@code null} for none
     * @param displayTimeMs duration assigned to the toast, in milliseconds
     * @since 1.2.5
     */
    public void toast(Object title, Object desc, long displayTimeMs) {
        ToastManager tm = GuiCompat.toastManager(mc);
        // TODO: Are scripts run before Minecraft is properly instantiated anyway? Even if not wouldn't the line above
        //  throw an error because mc is null?
        if (tm == null) {
            // TODO: Throw?
            return;
        }

        Component titleComp = (title instanceof TextHelper) ? ((TextHelper) title).getRaw() : title != null ? Component.literal(title.toString()) : null;
        Component descComp = (desc instanceof TextHelper) ? ((TextHelper) desc).getRaw() : desc != null ? Component.literal(desc.toString()) : null;
        // There doesn't seem to be a difference in the appearance or the functionality except for the UNSECURE_SERVER_WARNING with a longer duration
        if (titleComp != null) {
            showSystemToast(tm, new SystemToast.SystemToastId(displayTimeMs), titleComp, descComp);
        }
    }

    /**
     * Queues a system toast, keeping long descriptions multi-line on versions that still support
     * that layout.
     */
    private static void showSystemToast(ToastManager manager, SystemToast.SystemToastId id,
                                        Component title, @Nullable Component desc) {
        // 26.2 removed SystemToast.multiline; its constructor now splits the title and description itself.
        //? if >=26.2 {
        SystemToast.add(manager, id, title, desc);
        //?} else {
        if (desc == null) {
            SystemToast.add(manager, id, title, null);
        } else {
            manager.addToast(SystemToast.multiline(mc, id, title, desc));
        }
        //?}
    }

    /**
     * Wraps the supplied text as a literal {@link TextHelper}; formatting codes are not parsed.
     *
     * @param content literal text
     * @return a new {@link TextHelper TextHelper}
     * @see TextHelper
     * @since 1.1.3
     */
    public TextHelper createTextHelperFromString(String content) {
        return TextHelper.wrap(Component.literal(content));
    }

    /**
     * Wraps a translatable text component using the key and supplied arguments.
     *
     * @param key     the translation key to look up
     * @param content arguments supplied to the translation
     * @return a new {@link TextHelper TextHelper}
     * @since 1.9.0
     */
    public TextHelper createTextHelperFromTranslationKey(String key, Object... content) {
        return TextHelper.wrap(Component.translatable(key, content));
    }

    /**
     * Returns the SLF4J logger used by JsMacros.
     *
     * @return the slf4j logger JsMacros itself uses
     * @since 1.5.2
     */
    public Logger getLogger() {
        return JsMacros.LOGGER;
    }

    /**
     * Returns the SLF4J logger associated with the supplied name.
     *
     * @param name logger name
     * @return the logger associated with {@code name}
     * @since 1.5.2
     */
    public Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }

    /**
     * Parses an SNBT text-component representation and wraps the decoded component.
     *
     * @param json the SNBT form of a text component
     * @return a new {@link TextHelper TextHelper}
     * @throws RuntimeException if parsing or component decoding fails
     * @see TextHelper
     * @since 1.1.3
     */
    public TextHelper createTextHelperFromJSON(String json) {
        try {
            var nbt = nbtParser.parseForCommands(new StringReader(json));
            return TextHelper.wrap(ComponentSerialization.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow());
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns an empty {@link TextBuilder} for constructing styled text in sections.
     *
     * @return a new empty builder
     * @see TextBuilder
     * @since 1.3.0
     */
    public TextBuilder createTextBuilder() {
        return new TextBuilder();
    }

    /**
     * Returns a command builder; use {@link #getCommandManager()} for the non-deprecated access path.
     *
     * @param name command name
     * @return a builder for a command with that name
     * @see #getCommandManager()
     * @since 1.4.2
     */
    @Deprecated
    @DocletIgnore
    public CommandBuilder createCommandBuilder(String name) {
        return CommandManager.instance.createCommandBuilder(name);
    }

    /**
     * Removes a registered command and returns its node for possible re-registration.
     *
     * @param name the name of the command to take out, without a slash
     * @return the node the command was built from
     * @throws IllegalAccessException if the command cannot be reached for removal
     * @see #getCommandManager()
     * @since 1.6.5
     */
    @Deprecated
    @DocletIgnore
    public CommandNodeHelper unregisterCommand(String name) throws IllegalAccessException {
        return CommandManager.instance.unregisterCommand(name);
    }

    /**
     * Re-registers a node returned by {@link #unregisterCommand(String)}.
     *
     * @param node the node {@link #unregisterCommand(String)} handed back
     * @see #getCommandManager()
     * @since 1.6.5
     */
    @Deprecated
    @DocletIgnore
    public void reRegisterCommand(CommandNodeHelper node) {
        CommandManager.instance.reRegisterCommand(node);
    }

    /**
     * Returns the shared manager for command builders, client command-tree access, and autocomplete.
     *
     * @return the shared command manager
     * @since 1.7.0
     */
    public CommandManager getCommandManager() {
        return CommandManager.instance;
    }

    /**
     * Returns a manager for received chat history and sent-message history. A new manager is
     * returned on each call, but each refers to the same chat component. Received history can
     * include JsMacros log lines and excludes lines no longer in the history. Off-thread
     * operations may be queued; whether they wait depends on the operation and its {@code await}
     * argument.
     *
     * @return a manager for the chat histories
     * @since 1.7.0
     */
    public ChatHistoryManager getHistory() {
        return new ChatHistoryManager(GuiCompat.hud(mc).getChat());
    }

    /**
     * Returns the rendered width of the text using the game's font.
     *
     * @param text the text to get the width of, or {@code null} for zero
     * @return the rendered width in GUI-scaled screen pixels
     * @since 1.8.4
     */
    public int getTextWidth(@Nullable String text) {
        return mc.font.width(text);
    }

    private static final Pattern SECTION_SYMBOL_PATTERN = Pattern.compile("[§&]");

    /**
     * Converts section symbols to {@code &} and doubles literal ampersands, reversing
     * {@link #ampersandToSectionSymbol(String)} character by character. For example, a section
     * symbol in a hex colour sequence becomes {@code &}, while its {@code #} remains unchanged.
     * The inverse conversion restores these characters.
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
     * Converts each {@code &} followed by a non-line-terminator character to {@code §} and
     * that character, except that {@code &&} becomes a literal {@code &}.
     * <p>
     * An unmatched trailing ampersand, or one immediately before a line terminator, is
     * unchanged. This conversion is also used by {@link #logColor(String)}.
     * It does not validate formatting codes, so use single ampersands only before characters
     * intended as formatting codes.
     * <pre>
     * const converted = Chat.ampersandToSectionSymbol("\x26lbold\x26r normal");
     * Chat.log(converted);
     * </pre>
     *
     * @param string the string to convert
     * @return the converted text
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
     * removes matching legacy colour and style codes from a string.
     * <p>
     * This works on a plain string and is the cheap way to get readable text out of something
     * that is full of codes. For a {@code TextHelper},
     * {@link TextHelper#getStringStripFormatting() getStringStripFormatting()} does the same
     * thing and walks the actual styled text rather than a pattern.<br>
     * The pattern removes {@code §} followed by one legacy formatting character. It does not
     * parse a full hex-colour sequence such as {@code §x§1§2§3§4§5§6}; that input becomes
     * {@code §x123456}, because {@code x} is not matched while each following section-sign
     * and digit pair is. A
     * {@code null} is not handled and raises.<br>
     * The ampersands below are written as the escape {@code \x26} so this documentation
     * renders them literally.
     * <pre>
     * // write to a log file without the codes in it
     * const plain = Chat.stripFormatting(Chat.ampersandToSectionSymbol("\x26c red \x26r and normal"));
     * Chat.getLogger("my macro").info(plain);
     * </pre>
     *
     * @param string the string to strip, which must not be {@code null}
     * @return the string with matching formatting codes removed
     * @since 1.6.5
     */
    public String stripFormatting(String string) {
        // on 1.15 and lower switch to comment
//        return string.replaceAll("§#\\d{6}|§.", "");
        return TextHelper.STRIP_FORMATTING_PATTERN.matcher(string).replaceAll("");
    }

}
