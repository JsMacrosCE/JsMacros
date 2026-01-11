package com.jsmacrosce.jsmacros.client.api.helper.screen;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

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

//    public int getId() {
//        return base.getId();
//    }

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
