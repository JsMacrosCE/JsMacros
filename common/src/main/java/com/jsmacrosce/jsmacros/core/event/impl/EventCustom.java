package com.jsmacrosce.jsmacros.core.event.impl;

import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A named event with key-value arguments. Call {@link #registerEvent()} before subscribing; dispatch
 * also reaches eligible {@code ANYTHING} listeners.
 *
 * @author Wagyourtail
 * @since 1.2.8
 */
@SuppressWarnings("unused")
@DocletCategory("Core")
@Event("Custom")
public class EventCustom extends BaseEvent {
    protected Map<String, Object> args = new ConcurrentHashMap<>();
    /** Name used as this custom event's dispatch and subscription-registration key. Changing it does not register the new name; call {@link #registerEvent()} separately. */
    public String eventName;
    /** Explicit permission for joined dispatch; defaults to {@code false}. */
    public boolean joinable;
    /** Whether this event permits cancellation; defaults to {@code false}. */
    public boolean cancelable;

    /**
     * Whether this event permits joining, based on its current {@code joinable} or {@code cancelable} flag.
     * @return {@code true} if either flag is true; this does not guarantee every listener is joined.
     */
    @Override
    public boolean joinable() {
        return joinable || cancelable;
    }

    /**
     * Whether this event currently permits cancellation.
     * @return the current value of {@code cancelable}; calling {@link #cancel()} marks this instance cancelled when permitted.
     */
    @Override
    public boolean cancellable() {
        return cancelable;
    }

    /**
     * @param runner core that supplies this event's runtime context
     * @param eventName name used to dispatch this event
     */
    public EventCustom(Core<?, ?> runner, String eventName) {
        super(runner);
        this.eventName = eventName;
    }

    /**
     * Dispatches this event to listeners registered under its name and eligible {@code ANYTHING}
     * listeners.
     *
     * @since 1.2.8
     */
    public void trigger() {
        runner.profile.triggerEvent(this);
    }

    /**
     * Dispatches the event on a thread-pool task, then runs {@code callback} after dispatch returns.
     *
     * @param callback callback to run after dispatch
     * @since 1.9.0
     */
    public void triggerAsync(MethodWrapper<Object, Object, Object, ?> callback) {
        runner.threadPool.runTask(() -> {
            runner.profile.triggerEvent(this);
            try {
                callback.run();
            } catch (Throwable e) {
                runner.profile.logError(e);
            }
        });
    }

    /**
     * Stores an integer argument.
     *
     * @param name argument key
     * @param i value to store
     * @return the stored integer value
     * @since 1.2.8
     */
    public int putInt(String name, int i) {
        args.put(name, i);
        return i;
    }

    /**
     * Stores a string argument.
     *
     * @param name argument key
     * @param str value to store
     * @return the stored string value
     * @since 1.2.8
     */
    public String putString(String name, String str) {
        args.put(name, str);
        return str;
    }

    /**
     * Stores a double argument.
     *
     * @param name argument key
     * @param d value to store
     * @return the stored double value
     * @since 1.2.8
     */
    public double putDouble(String name, double d) {
        args.put(name, d);
        return d;
    }

    /**
     * Stores a boolean argument.
     *
     * @param name argument key
     * @param b value to store
     * @return the stored boolean value
     * @since 1.2.8
     */
    public boolean putBoolean(String name, boolean b) {
        args.put(name, b);
        return b;
    }

    /**
     * Stores an object argument. Guest-language objects cause an {@link AssertionError}.
     *
     * @param name argument key
     * @param o value to store
     * @return the stored object
     * @since 1.2.8
     */
    public Object putObject(String name, Object o) {
        if (runner.extensions.isGuestObject(o)) {
            throw new AssertionError("Cannot put a guest object into an event");
        }
        args.put(name, o);
        return o;
    }

    /**
     * Returns the stored argument's type label, or {@code null} when the key is absent.
     *
     * @param name argument key
     * @return {@code Int}, {@code String}, {@code Double}, {@code Boolean}, or {@code Object}; {@code null} if absent
     * @since 1.2.8
     */
    @DocletReplaceReturn("'Int' | 'String' | 'Double' | 'Boolean' | 'Object' | null")
    @Nullable
    public String getType(String name) {
        Object i = args.get(name);
        if (i == null) {
            return null;
        } else if (i instanceof Integer) {
            return "Int";
        } else if (i instanceof String) {
            return "String";
        } else if (i instanceof Double) {
            return "Double";
        } else if (i instanceof Boolean) {
            return "Boolean";
        } else {
            return "Object";
        }
    }

    /**
     * Returns the argument if it is an integer; otherwise returns {@code null}.
     *
     * @param name argument key
     * @return the value, or {@code null} if absent or not an integer
     * @since 1.2.8
     */
    @Nullable
    public Integer getInt(String name) {
        Object i = args.get(name);
        if (i instanceof Integer) {
            return (Integer) i;
        } else {
            return null;
        }
    }

    /**
     * Returns the argument if it is a string; otherwise returns {@code null}.
     *
     * @param name argument key
     * @return the value, or {@code null} if absent or not a string
     * @since 1.2.8
     */
    @Nullable
    public String getString(String name) {
        Object i = args.get(name);
        if (i instanceof String) {
            return (String) i;
        } else {
            return null;
        }
    }

    /**
     * Returns the argument if it is a double; otherwise returns {@code null}.
     *
     * @param name argument key
     * @return the value, or {@code null} if absent or not a double
     * @since 1.2.8
     */
    @Nullable
    public Double getDouble(String name) {
        Object i = args.get(name);
        if (i instanceof Double) {
            return (Double) i;
        } else {
            return null;
        }
    }

    /**
     * Returns the argument if it is a boolean; otherwise returns {@code null}.
     *
     * @param name argument key
     * @return the value, or {@code null} if absent or not a boolean
     * @since 1.2.8
     */
    @Nullable
    public Boolean getBoolean(String name) {
        Object i = args.get(name);
        if (i instanceof Boolean) {
            return (Boolean) i;
        } else {
            return null;
        }
    }

    /**
     * Returns the argument value, or {@code null} if absent.
     *
     * @param name argument key
     * @return the value, or {@code null} if absent
     * @since 1.2.8
     */
    @Nullable
    public Object getObject(String name) {
        return args.get(name);
    }

    /**
     * @return the live map backing this event; changes to it change the event's arguments
     * @since 1.6.4
     */
    public Map<String, Object> getUnderlyingMap() {
        return args;
    }

    /**
     * Registers this event's name so it can be subscribed to and shown in the GUI.
     *
     * @since 1.3.0
     */
    public void registerEvent() {
        runner.eventRegistry.addEvent(eventName);
    }

}
