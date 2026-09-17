package com.jsmacrosce.jsmacros.client.api.helper.screen;

import net.minecraft.client.gui.components.ChatComponent;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;
import com.jsmacrosce.doclet.DocletIgnore;

//? if >=26.1 {
/*import net.minecraft.client.multiplayer.chat.GuiMessage;
*///? } else {
import net.minecraft.client.GuiMessage;
//? }

@SuppressWarnings("unused")
public class ChatHudLineHelper extends BaseHelper<GuiMessage> {
    private ChatComponent hud;

    public ChatHudLineHelper(GuiMessage base, ChatComponent hud) {
        super(base);
        this.hud = hud;
    }

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

    public int getCreationTick() {
        return base.addedTime();
    }

    public ChatHudLineHelper deleteById() {
        hud.allMessages.remove(base);
        return this;
    }

    @Override
    public String toString() {
        return String.format("ChatHudLineHelper:{\"text\": \"%s\", \"creationTick\": %d}", base.content().getString(), base.addedTime());
    }

}
