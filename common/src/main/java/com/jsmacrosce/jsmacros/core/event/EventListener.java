package com.jsmacrosce.jsmacros.core.event;

import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.language.EventContainer;

/**
 * the listener behind a macro trigger: a script file saved against an event in the profile editor,
 * run whenever that event fires. It is a {@link BaseListener}, so everything about how it is
 * configured lives on the {@link com.jsmacrosce.jsmacros.core.config.ScriptTrigger ScriptTrigger}
 * behind it rather than on the listener, which is why a script has no way to make one of these.
 * <p>
 * It reaches the event the same way a script listener does, and so is caught by the same catch:
 * {@link #trigger(BaseEvent) trigger} hands the event to
 * {@link BaseListener#runScript(com.jsmacrosce.jsmacros.core.event.BaseEvent) runScript}, which
 * runs the file through
 * {@link com.jsmacrosce.jsmacros.core.Core#exec(com.jsmacrosce.jsmacros.core.config.ScriptTrigger, com.jsmacrosce.jsmacros.core.event.BaseEvent) Core.exec}
 * on a {@link com.jsmacrosce.jsmacros.core.threads.JsMacrosThreadPool JsMacrosThreadPool} thread
 * and comes straight back. What decides whether the profile waits for that is
 * {@link BaseListener#joined() joined}, which reads the Joined box on the trigger, so a macro
 * trigger has to be joined before its {@code cancel} can be relied on, exactly as a script
 * listener has to pass {@code joined = true}. Read {@link BaseEvent} for the whole of that rule.
 * <p>
 * Two things it does that a script listener does not. It gives {@code null} back when the trigger
 * is switched off, which the profile reads as the listener declining the event and skips past,
 * where a script listener that throws takes itself off the event instead. So a macro whose script
 * throws is logged by the language on every occurrence and stays registered, while the equivalent
 * failure in a script listener is a one off.
 */
public class EventListener extends BaseListener {

    /**
     * makes the listener for a trigger that has already been saved in a profile. Registering it is
     * the registry's job, not this constructor's.
     *
     * @param macro the trigger to run. Its event name is what this listener is filed under, and
     * its enabled and joined flags are read on every occurrence, so toggling either in the
     * profile editor takes effect without reloading the profile.
     * @param runner the core to run the file on.
     */
    public EventListener(ScriptTrigger macro, Core runner) {
        super(macro, runner);
    }

    /**
     * runs the script file this trigger points at, with the event as its input, and gives back the
     * container the profile will park on if this trigger is joined.
     *
     * @param event the event that was raised, passed to the script as the {@code event} global.
     * @return the container the file is running on, or {@code null} if the trigger is switched
     * off, which the profile reads as the listener declining the event. A script that throws once
     * it is running does not give {@code null} here: the file is already on its own thread by
     * then, so the error is logged by the language and this comes back as normal.
     */
    @Override
    public EventContainer<?> trigger(BaseEvent event) {
        return runScript(event);
    }

}
