package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.access.IChatHud;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.client.api.helper.screen.ChatHudLineHelper;
import com.jsmacrosce.jsmacros.core.MethodWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * the two chat histories: everything that has come in, and everything that was sent.
 * <p>
 * These are different lists. The received side is the scrollback, what
 * {@link #getRecvLines()} and {@link #getRecvCount()} read and what
 * {@link #insertRecvText(int, TextHelper, int, boolean)} and the remove methods change. The
 * sent side is only two things, {@link #getSent()} which hands back the list itself and
 * {@link #clearSent(boolean)} which empties it.
 * <p>
 * The methods that <b>change</b> the history each come in an {@code await} form, and it is worth
 * knowing what it does before reaching for one. Without it a call returns as soon as the work
 * has been <i>queued</i> on the client thread, which is almost always what a script wants; with
 * it the call blocks until its own task has run, which means it also waits out whatever else was
 * queued ahead of it. The readers do not have the choice at all: they
 * always wait, because reading a list the client thread is in the middle of rewriting is the
 * case that actually bites.
 * <p>
 * The one exception is {@link #getSent()}, which hands back the list itself rather than a
 * copy, so a script that writes to it is editing the client history directly.
 * example:
 * <pre>
 * const history = Chat.getHistory();
 * // what has come in
 * Chat.log(`${history.getRecvCount()} lines in the scrollback`);
 * const last = history.getRecvLines();
 * if (last.size() > 0) {
 *   // a line is not a string, it is a wrapper with the text and the tick it arrived on
 *   const newest = last.get(last.size() - 1);
 *   Chat.log(`the newest is ${newest.getText().getString()}, on tick ${newest.getCreationTick()}`);
 * }
 * // and what was sent, which is a list a script can edit in place
 * Chat.log(`sent: ${history.getSent()}`);
 * </pre>
 * @since 1.6.0
 */
public class ChatHistoryManager {
    private static final Minecraft mc = Minecraft.getInstance();
    private final ChatComponent hud;

    public ChatHistoryManager(ChatComponent hud) {
        this.hud = hud;
    }

    /**
     * @param index
     * @return
     * @since 1.6.0
     */
    public ChatHudLineHelper getRecvLine(int index) throws InterruptedException {
        if (JsMacrosClient.clientCore.profile.checkJoinedThreadStack()) {
            return new ChatHudLineHelper(hud.allMessages.get(index), hud);
        }
        ChatHudLineHelper[] helper = {null};
        final Semaphore semaphore = new Semaphore(0);
        mc.execute(() -> {
            helper[0] = new ChatHudLineHelper(hud.allMessages.get(index), hud);
            semaphore.release();
        });
        semaphore.acquire();
        return helper[0];
    }

    /**
     * @return the amount of messages in the chat history.
     * @throws InterruptedException
     * @since 1.8.4
     */
    public int getRecvCount() throws InterruptedException {
        final Semaphore semaphore = new Semaphore(0);
        AtomicInteger count = new AtomicInteger(0);
        mc.execute(() -> {
            count.set(hud.allMessages.size());
            semaphore.release();
        });
        semaphore.acquire();
        return count.get();
    }

    /**
     * @return all received messages in the chat history.
     * @throws InterruptedException
     * @since 1.8.4
     */
    public List<ChatHudLineHelper> getRecvLines() throws InterruptedException {
        List<ChatHudLineHelper> recvLines = new ArrayList<>();
        final Semaphore semaphore = new Semaphore(0);
        mc.execute(() -> {
            hud.allMessages.stream().map(textChatHudLine -> new ChatHudLineHelper(textChatHudLine, hud)).forEach(recvLines::add);
            semaphore.release();
        });
        semaphore.acquire();
        return recvLines;
    }

    /**
     * @param index
     * @param line
     * @since 1.6.0
     */
    public void insertRecvText(int index, TextHelper line) throws InterruptedException {
        insertRecvText(index, line, 0, false);
    }

    /**
     * you should probably run {@link #refreshVisible()} after...
     *
     * @param index
     * @param line
     * @param timeTicks
     * @since 1.6.0
     */
    public void insertRecvText(int index, TextHelper line, int timeTicks) throws InterruptedException {
        insertRecvText(index, line, timeTicks, false);
    }

    /**
     * @param index
     * @param line
     * @param timeTicks
     * @param await
     * @throws InterruptedException
     * @since 1.6.0
     */
    public void insertRecvText(int index, TextHelper line, int timeTicks, boolean await) throws InterruptedException {
        if (JsMacrosClient.clientCore.profile.checkJoinedThreadStack()) {
            ((IChatHud) hud).jsmacros_addMessageAtIndexBypass(line.getRaw(), index, timeTicks);
            return;
        }
        final Semaphore semaphore = new Semaphore(await ? 0 : 1);
        mc.execute(() -> {
            ((IChatHud) hud).jsmacros_addMessageAtIndexBypass(line.getRaw(), index, timeTicks);
            semaphore.release();
        });
        semaphore.acquire();
    }

    /**
     * @param index
     * @since 1.6.0
     */
    public void removeRecvText(int index) throws InterruptedException {
        removeRecvText(index, false);
    }

    /**
     * @param index
     * @param await
     * @throws InterruptedException
     * @since 1.6.0
     */
    public void removeRecvText(int index, boolean await) throws InterruptedException {
        if (JsMacrosClient.clientCore.profile.checkJoinedThreadStack()) {
            hud.allMessages.remove(index);
            return;
        }
        final Semaphore semaphore = new Semaphore(await ? 0 : 1);
        mc.execute(() -> {
            hud.allMessages.remove(index);
            semaphore.release();
        });
        semaphore.acquire();
    }

    /**
     * @param text
     * @since 1.6.0
     */
    public void removeRecvTextMatching(TextHelper text) throws InterruptedException {
        removeRecvTextMatching(text, false);
    }

    /**
     * @param text
     * @param await
     * @throws InterruptedException
     * @since 1.6.0
     */
    public void removeRecvTextMatching(TextHelper text, boolean await) throws InterruptedException {
        if (JsMacrosClient.clientCore.profile.checkJoinedThreadStack()) {
            hud.allMessages.removeIf(c -> c.content().equals(text.getRaw()));
            return;
        }
        final Semaphore semaphore = new Semaphore(await ? 0 : 1);
        mc.execute(() -> {
            hud.allMessages.removeIf(c -> c.content().equals(text.getRaw()));
            semaphore.release();
        });
        semaphore.acquire();
    }

    /**
     * @param filter
     * @since 1.6.0
     */
    public void removeRecvTextMatchingFilter(MethodWrapper<ChatHudLineHelper, Object, Boolean, ?> filter) throws InterruptedException {
        removeRecvTextMatchingFilter(filter, false);
    }

    /**
     * @param filter
     * @param await
     * @throws InterruptedException
     * @since 1.6.0
     */
    public void removeRecvTextMatchingFilter(MethodWrapper<ChatHudLineHelper, Object, Boolean, ?> filter, boolean await) throws InterruptedException {
        if (JsMacrosClient.clientCore.profile.checkJoinedThreadStack()) {
            hud.allMessages.removeIf((c) -> filter.test(new ChatHudLineHelper(c, hud)));
            return;
        }
        final Semaphore semaphore = new Semaphore(await ? 0 : 1);
        Throwable[] ex = new Throwable[]{null};
        mc.execute(() -> {
            try {
                hud.allMessages.removeIf((c) -> filter.test(new ChatHudLineHelper(c, hud)));
            } catch (Throwable e) {
                ex[0] = e;
                if (!await && !(e.getCause() instanceof InterruptedException)) {
                    JsMacrosClient.clientCore.profile.logError(e);
                }
            }
            semaphore.release();
        });
        semaphore.acquire();
        if (ex[0] != null) {
            throw new RuntimeException(ex[0]);
        }
    }

    /**
     * this will reset the view of visible messages
     *
     * @since 1.6.0
     */
    public void refreshVisible() throws InterruptedException {
        refreshVisible(false);
    }

    /**
     * @param await
     * @throws InterruptedException
     * @since 1.6.0
     */
    public void refreshVisible(boolean await) throws InterruptedException {
        if (JsMacrosClient.clientCore.profile.checkJoinedThreadStack()) {
            hud.rescaleChat();
            return;
        }
        final Semaphore semaphore = new Semaphore(await ? 0 : 1);
        mc.execute(() -> {
            hud.rescaleChat();
            semaphore.release();
        });
        semaphore.acquire();

    }

    /**
     * @since 1.6.0
     */
    public void clearRecv() throws InterruptedException {
        clearRecv(false);
    }

    /**
     * @param await
     * @throws InterruptedException
     * @since 1.6.0
     */
    public void clearRecv(boolean await) throws InterruptedException {
        if (JsMacrosClient.clientCore.profile.checkJoinedThreadStack()) {
            hud.clearMessages(false);
            return;
        }
        final Semaphore semaphore = new Semaphore(await ? 0 : 1);
        mc.execute(() -> {
            hud.clearMessages(false);
            semaphore.release();
        });
        semaphore.acquire();
    }

    /**
     * @return direct reference to sent message history list. modifications will affect the list.
     * @since 1.6.0
     */
    public List<String> getSent() {
        return hud.getRecentChat();
    }

    /**
     * @since 1.6.0
     */
    public void clearSent() throws InterruptedException {
        clearSent(false);
    }

    /**
     * @param await
     * @throws InterruptedException
     * @since 1.6.0
     */
    public void clearSent(boolean await) throws InterruptedException {
        if (JsMacrosClient.clientCore.profile.checkJoinedThreadStack()) {
            hud.getRecentChat().clear();
            return;
        }
        final Semaphore semaphore = new Semaphore(await ? 0 : 1);
        mc.execute(() -> {
            hud.getRecentChat().clear();
            semaphore.release();
        });
        semaphore.acquire();
    }

}
