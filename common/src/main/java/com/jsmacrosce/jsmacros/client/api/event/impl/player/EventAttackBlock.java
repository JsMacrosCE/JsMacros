package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockDataHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

@Event("AttackBlock")
public class EventAttackBlock extends BaseEvent {
    public final BlockDataHelper block;
    @DocletReplaceReturn("Side")
    public final int side;

    public EventAttackBlock(BlockDataHelper block, int side) {
        super(JsMacrosClient.clientCore);
        this.block = block;
        this.side = side;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"block\": %s}", this.getEventName(), block);
    }

}
