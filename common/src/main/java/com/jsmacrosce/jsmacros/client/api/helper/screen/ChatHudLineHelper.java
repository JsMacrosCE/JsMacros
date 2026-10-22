package com.jsmacrosce.jsmacros.client.api.helper.screen;

import com.jsmacrosce.doclet.DocletCategory;

import net.minecraft.client.gui.components.ChatComponent;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;
import com.jsmacrosce.doclet.DocletIgnore;

//? if >=26.1 {
/*import net.minecraft.client.multiplayer.chat.GuiMessage;
*///? } else {
import net.minecraft.client.GuiMessage;
//? }

/**
 * one line of the chat HUD, which is the scrollback the client keeps rather than the
 * text sitting on screen: a message the player has scrolled past is still a line here,
 * and a long message is one line here however many rows of text it takes to draw.
 * <p>
 * A helper is a wrapper on one entry in that scrollback, and it is how a line is
 * changed or removed. The readers are on {@code Chat.getHistory()}, which hands back
 * the whole list newest first, and it is the list those readers and
 * {@link #deleteById()} all work on.
 * <p>
 * There is no id on a chat line in this version of the game, so the entry that used to
 * report one is gone rather than reporting something else.
 * example:
 * <pre>
 * const history = Chat.getHistory();
 * const lines = history.getRecvLines();
 * Chat.log(`${lines.size()} line(s) in the scrollback`);
 * // the list is newest first, so index zero is the line that just arrived
 * for (let i = 0; i !== lines.size(); i += 1) {
 *   const line = lines.get(i);
 *   Chat.log(`[${line.getCreationTick()}] ${line.getText().getString()}`);
 * }
 * </pre>
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class ChatHudLineHelper extends BaseHelper<GuiMessage> {
    private ChatComponent hud;

    public ChatHudLineHelper(GuiMessage base, ChatComponent hud) {
        super(base);
        this.hud = hud;
    }

    /**
     * the line's text, as a text wrapper rather than a plain string, so the colouring
     * and any styling the server sent come with it. A line can be more than one row of
     * text on screen, and this is the whole of it in one piece.
     * <p>
     * The text is read at the moment of the call and the wrapper is a snapshot of it, so
     * a line that is still fading in reads the same as one that has finished.
     * example:
     * <pre>
     * const history = Chat.getHistory();
     * if (history.getRecvCount() > 0) {
     *   const newest = history.getRecvLine(0);
     *   // the styling, and the same text with the styling codes taken out
     *   Chat.log(newest.getText());
     *   Chat.log(newest.getText().getStringStripFormatting());
     * }
     * </pre>
     *
     * @return the text of this chat line
     */
    public TextHelper getText() {
        return TextHelper.wrap(base.content());
    }

    /**
     * @deprecated Minecraft no longer exposes an id on {@code GuiMessage}; this always throws.
     */
    @Deprecated
    @DocletIgnore
    public int getId() {
        throw new UnsupportedOperationException("ChatHudLineHelper.getId() is no longer supported: Minecraft no longer exposes a chat message id");
    }

    /**
     * the game tick the line arrived on, which is a tick count rather than a timestamp.
     * The value is fixed when the line arrives and does not change afterwards, so two
     * reads of the same line give the same number, and two lines that arrived on the
     * same tick read the same.
     * <p>
     * The clock is the one the chat HUD keeps, and there is no reader for it on the
     * script side, so this is a number to record rather than one to subtract from the
     * present. It is not the world clock: {@code World.getTime()} counts the world's
     * own ticks since the world started, and the two are separate counters that do not
     * have to agree.
     * <p>
     * The same value can be given to a line the script inserts itself, as the third
     * argument of {@code insertRecvText} on the history.
     * example:
     * <pre>
     * const history = Chat.getHistory();
     * const lines = history.getRecvLines();
     * if (lines.size() > 0) {
     *   // recorded rather than subtracted, since the same clock is not readable
     *   const arrived = lines.get(0).getCreationTick();
     *   Chat.log(`the newest line arrived on tick ${arrived}`);
     * }
     * </pre>
     *
     * @return the game tick this line arrived on
     */
    public int getCreationTick() {
        return base.addedTime();
    }

    /**
     * takes this line off the scrollback, so the client forgets it and it is not drawn
     * again. This is the only way to remove one: there is no id to name it by, so the
     * line itself is the handle.
     * <p>
     * The removal is by value rather than by identity, and the lines in the scrollback
     * are held newest first, so two lines with the same text arriving on the same tick
     * are equal and this takes the first of them rather than necessarily the one it was
     * called on. Removing a line twice is not an error; the second call simply finds
     * nothing to take off.
     * <p>
     * The scrollback is capped at a hundred lines by the game, so a line that has
     * already been pushed off the end is not there to be removed.
     * example:
     * <pre>
     * const history = Chat.getHistory();
     * const lines = history.getRecvLines();
     * let removed = 0;
     * for (let i = 0; i !== lines.size(); i += 1) {
     *   const line = lines.get(i);
     *   if (line.getText().getString().indexOf("noise") >= 0) {
     *     line.deleteById();
     *     removed += 1;
     *   }
     * }
     * Chat.log(`took ${removed} line(s) off, ${history.getRecvCount()} left`);
     * </pre>
     *
     * @return self for chaining.
     */
    public ChatHudLineHelper deleteById() {
        hud.allMessages.remove(base);
        return this;
    }

    @Override
    public String toString() {
        return String.format("ChatHudLineHelper:{\"text\": \"%s\", \"creationTick\": %d}", base.content().getString(), base.addedTime());
    }

}
