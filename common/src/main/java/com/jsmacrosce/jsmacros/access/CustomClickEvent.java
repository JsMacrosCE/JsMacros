package com.jsmacrosce.jsmacros.access;

import net.minecraft.network.chat.ClickEvent;
import org.jetbrains.annotations.NotNull;

public record CustomClickEvent(Runnable event) implements ClickEvent {

    @NotNull
    @Override
    public Action action() {
        //? if >1.21.5 {
        return Action.CUSTOM;
        //?} else {
            /*return Action.RUN_COMMAND;
        *///?}
    }
}
