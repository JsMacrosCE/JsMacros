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
 * Manages received chat scrollback and sent-message history. Reads and list mutations run inline on
 * the client thread or from a thread joined to it; otherwise they queue on the client thread.
 * Value-returning reads and most mutations wait for their queued work, but a joined caller cannot
 * wait without deadlocking. Inserting a received line and rebuilding the visible chat touch the
 * font, so they run inline only on the client thread and are queued without waiting elsewhere.
 * {@link #getSent()} returns the live sent-history list, so changes to it affect the chat
 * component directly.
 * <pre>
 * const history = Chat.getHistory();
 * Chat.log(`${history.getRecvCount()} lines in the scrollback`);
 * const last = history.getRecvLines();
 * if (last.size() > 0) {
 *   const newest = last.get(last.size() - 1);
 *   Chat.log(`the newest is ${newest.getText().getString()}, on tick ${newest.getCreationTick()}`);
 * }
 * </pre>
 * @since 1.6.0
 */
public class ChatHistoryManager {
    private static final Minecraft mc = Minecraft.getInstance();
    private final ChatComponent hud;

    /**
     * Manages the received and sent history of the supplied chat component.
     * {@link com.jsmacrosce.jsmacros.client.api.library.impl.FChat#getHistory()} obtains a manager
     * for Minecraft's chat component; this constructor stores the supplied component without
     * copying or changing its history.
     *
     * @param hud chat component whose history is managed
     */
    public ChatHistoryManager(ChatComponent hud) {
        this.hud = hud;
    }

    /**
     * @param index zero-based index in the received-message history
     * @return the received message at that index
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
     * @return the number of received messages in the chat history
     * @throws InterruptedException
     * @since 1.8.4
     */
    public int getRecvCount() throws InterruptedException {
        if (JsMacrosClient.clientCore.profile.checkJoinedThreadStack()) {
            return hud.allMessages.size();
        }
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
     * @return a new list of wrappers for all received messages
     * @throws InterruptedException
     * @since 1.8.4
     */
    public List<ChatHudLineHelper> getRecvLines() throws InterruptedException {
        if (JsMacrosClient.clientCore.profile.checkJoinedThreadStack()) {
            List<ChatHudLineHelper> recvLines = new ArrayList<>();
            hud.allMessages.stream().map(textChatHudLine -> new ChatHudLineHelper(textChatHudLine, hud)).forEach(recvLines::add);
            return recvLines;
        }
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
     * Inserts a chat line at the specified history index. On the client thread the insertion
     * happens immediately; other calls queue it on that thread without waiting.
     *
     * @param index history position at which to insert the line
     * @param line the chat line to insert
     * @throws InterruptedException if the calling thread is interrupted
     * @since 1.6.0
     */
    public void insertRecvText(int index, TextHelper line) throws InterruptedException {
        insertRecvText(index, line, 0, false);
    }

    /**
     * Inserts a chat line at the specified history index. On the client thread the insertion
     * happens immediately; other calls queue it on that thread without waiting. The
     * {@code timeTicks} argument is currently unused. Call {@link #refreshVisible()} to update
     * the visible chat after the insertion.
     *
     * @param index history position at which to insert the line
     * @param line the chat line to insert
     * @param timeTicks currently unused
     * @throws InterruptedException if the calling thread is interrupted
     * @since 1.6.0
     */
    public void insertRecvText(int index, TextHelper line, int timeTicks) throws InterruptedException {
        insertRecvText(index, line, timeTicks, false);
    }

    /**
     * Inserts a chat line at the specified history index. On the client thread the insertion
     * happens immediately. Other calls queue the insertion on the client thread and wait only
     * when {@code await} is {@code true}; a thread that the client is already waiting on, such as
     * a joined callback, cannot wait without deadlocking, so the insertion is queued and this
     * call returns. The {@code timeTicks} argument is currently unused.
     *
     * @param index history position at which to insert the line
     * @param line the chat line to insert
     * @param timeTicks currently unused
     * @param await whether to wait for a queued insertion to finish
     * @throws InterruptedException if waiting is interrupted
     * @since 1.6.0
     */
    public void insertRecvText(int index, TextHelper line, int timeTicks, boolean await) throws InterruptedException {
        if (mc.isSameThread()) {
            ((IChatHud) hud).jsmacros_addMessageAtIndexBypass(line.getRaw(), index, timeTicks);
            return;
        }
        final boolean wait = await && !JsMacrosClient.clientCore.profile.checkJoinedThreadStack();
        final Semaphore semaphore = new Semaphore(wait ? 0 : 1);
        mc.execute(() -> {
            try {
                ((IChatHud) hud).jsmacros_addMessageAtIndexBypass(line.getRaw(), index, timeTicks);
            } finally {
                semaphore.release();
            }
        });
        if (wait) {
            semaphore.acquire();
        }
    }

    /**
     * @param index zero-based received-message history index to remove
     * @since 1.6.0
     */
    public void removeRecvText(int index) throws InterruptedException {
        removeRecvText(index, false);
    }

    /**
     * Removes the received message at the specified history index. Off-thread removal is queued;
     * set {@code await} to {@code true} to wait for it to finish.
     *
     * @param index zero-based received-message history index to remove
     * @param await whether to wait for queued removal to finish
     * @throws InterruptedException if waiting is interrupted
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
     * @param text text whose exact raw value is matched against received messages
     * @since 1.6.0
     */
    public void removeRecvTextMatching(TextHelper text) throws InterruptedException {
        removeRecvTextMatching(text, false);
    }

    /**
     * Removes received messages whose raw content equals {@code text}. Off-thread removal is
     * queued; set {@code await} to {@code true} to wait for it to finish.
     *
     * @param text text whose exact raw value is matched against received messages
     * @param await whether to wait for queued removal to finish
     * @throws InterruptedException if waiting is interrupted
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
     * Removes received messages for which {@code filter} returns {@code true}. Off-thread removal
     * is queued without waiting for it to finish.
     *
     * @param filter predicate applied to each received message
     * @since 1.6.0
     */
    public void removeRecvTextMatchingFilter(MethodWrapper<ChatHudLineHelper, Object, Boolean, ?> filter) throws InterruptedException {
        removeRecvTextMatchingFilter(filter, false);
    }

    /**
     * Removes received messages for which {@code filter} returns {@code true}. Off-thread removal
     * is queued; set {@code await} to {@code true} to wait for it to finish.
     *
     * @param filter predicate applied to each received message
     * @param await whether to wait for queued removal to finish
     * @throws InterruptedException if waiting is interrupted
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
     * Rebuilds the visible chat view from the received-message history. On the client thread the
     * rebuild happens immediately; other calls queue it without waiting.
     *
     * @since 1.6.0
     */
    public void refreshVisible() throws InterruptedException {
        refreshVisible(false);
    }

    /**
     * Rebuilds the visible chat view from the received-message history. On the client thread the
     * rebuild happens immediately. Other calls queue it and wait only when {@code await} is
     * {@code true}; a thread that the client is already waiting on, such as a joined callback,
     * cannot wait without deadlocking, so the rebuild is queued and this call returns.
     *
     * @param await whether to wait for queued work to finish
     * @throws InterruptedException if waiting is interrupted
     * @since 1.6.0
     */
    public void refreshVisible(boolean await) throws InterruptedException {
        if (mc.isSameThread()) {
            hud.rescaleChat();
            return;
        }
        final boolean wait = await && !JsMacrosClient.clientCore.profile.checkJoinedThreadStack();
        final Semaphore semaphore = new Semaphore(wait ? 0 : 1);
        mc.execute(() -> {
            try {
                hud.rescaleChat();
            } finally {
                semaphore.release();
            }
        });
        if (wait) {
            semaphore.acquire();
        }
    }

    /**
     * Clears the received-message history without waiting for queued off-thread work to finish.
     *
     * @since 1.6.0
     */
    public void clearRecv() throws InterruptedException {
        clearRecv(false);
    }

    /**
     * Clears the received-message history. Off-thread work is queued; set {@code await} to
     * {@code true} to wait for it to finish.
     *
     * @param await whether to wait for queued work to finish
     * @throws InterruptedException if waiting is interrupted
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
     * @return the live sent-message history list; modifying it changes the chat component's list
     * @since 1.6.0
     */
    public List<String> getSent() {
        return hud.getRecentChat();
    }

    /**
     * Clears the sent-message history without waiting for queued off-thread work to finish.
     *
     * @since 1.6.0
     */
    public void clearSent() throws InterruptedException {
        clearSent(false);
    }

    /**
     * Clears the sent-message history. Off-thread work is queued; set {@code await} to
     * {@code true} to wait for it to finish.
     *
     * @param await whether to wait for queued work to finish
     * @throws InterruptedException if waiting is interrupted
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
