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
 * Wraps a chat HUD history entry, exposing its text and creation tick and allowing removal from
 * the HUD scrollback. History access is provided by {@code Chat.getHistory()}.
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

    /**
     * Stores the chat history message and HUD used by {@link #deleteById()}.
     *
     * @param base raw chat history message to wrap
     * @param hud chat HUD used by {@code deleteById()} to remove the captured message
     */
    public ChatHudLineHelper(GuiMessage base, ChatComponent hud) {
        super(base);
        this.hud = hud;
    }

    /**
     * Returns this message's styled content.
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
     * Returns the tick count recorded when the message arrived, not a timestamp.
     *
     * @return the recorded tick count
     */
    public int getCreationTick() {
        return base.addedTime();
    }

    /**
     * Removes this message from the HUD scrollback by value. Removing an absent message has no effect.
     *
     * @return self for chaining.
     */
    public ChatHudLineHelper deleteById() {
        hud.allMessages.remove(base);
        return this;
    }

    /**
     * Returns a diagnostic representation containing the message's plain content and creation tick count.
     *
     * @return diagnostic representation of this chat history entry
     */
    @Override
    public String toString() {
        return String.format("ChatHudLineHelper:{\"text\": \"%s\", \"creationTick\": %d}", base.content().getString(), base.addedTime());
    }

}
