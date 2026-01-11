package com.jsmacrosce.jsmacros.core.event;

import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.language.EventContainer;

public class EventListener extends BaseListener {

    public EventListener(ScriptTrigger macro, Core runner) {
        super(macro, runner);
    }

    @Override
    public EventContainer<?> trigger(BaseEvent event) {
        return runScript(event);
    }

}
