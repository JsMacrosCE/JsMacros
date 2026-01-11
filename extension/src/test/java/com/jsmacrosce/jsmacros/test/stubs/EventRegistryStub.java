package com.jsmacrosce.jsmacros.test.stubs;

import com.google.common.collect.ImmutableList;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.event.BaseEventRegistry;

import java.util.List;

public class EventRegistryStub extends BaseEventRegistry {
    public EventRegistryStub(Core runner) {
        super(runner);
    }

    @Override
    public void addScriptTrigger(ScriptTrigger rawmacro) {
        // no-op
    }

    @Override
    public boolean removeScriptTrigger(ScriptTrigger rawmacro) {
        throw new AssertionError("not implemented");
    }

    @Override
    public List<ScriptTrigger> getScriptTriggers() {
        return ImmutableList.of();
    }

}
