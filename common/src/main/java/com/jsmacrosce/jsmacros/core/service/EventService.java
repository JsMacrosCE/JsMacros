package com.jsmacrosce.jsmacros.core.service;

import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.classes.Registrable;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event payload for a running service, including shared arguments and stop callbacks.
 * @since 1.6.4
 */
@SuppressWarnings("unused")
@DocletCategory("Core")
@Event("Service")
public class EventService extends BaseEvent {
    /** Immutable service name supplied at construction. */
    public final String serviceName;

    /**
     * Callback run before event listeners and registered resources are removed when the service stops.
     */
    @Nullable
    public MethodWrapper<Object, Object, Object, ?> stopListener;
    /**
     * Callback run after event listeners and registered resources are removed when the service stops.
     * @since 1.9.1
     */
    @Nullable
    public MethodWrapper<Object, Object, Object, ?> postStopListener;

    boolean offEventsOnStop = false;
    @Nullable
    Registrable<?>[] registrableList = null;
    @Nullable
    BaseScriptContext<?> ctx = null;

    /**
     * Initializes this event with its service runner and name; this does not start or register the service.
     * @param runner core associated with this event
     * @param name service name
     */
    public EventService(Core<?, ?> runner, String name) {
        super(runner);
        this.serviceName = name;
    }

    /**
     * Configures cleanup performed when the service stops; each call replaces the previous configuration. Cleanup runs between {@link #stopListener} and {@link #postStopListener}.
     * @param offEvents whether to remove every listener registered by the service, rather than only listeners owned by its script context
     * @param list resources to unregister; an empty list disables resource cleanup
     * @since 1.9.1
     */
    public void unregisterOnStop(boolean offEvents, Registrable<?> ...list) {
        offEventsOnStop = offEvents;
        registrableList = list.length > 0 ? list : null;

        if (ctx != null) {
            ServiceManager.setAutoUnregisterKeepAlive(ctx, offEvents || registrableList != null);
        }
    }

    /**
     * Returns a diagnostic representation containing the service name.
     * @return diagnostic service-name text
     */
    @Override
    public String toString() {
        return "EventService{" +
                "serviceName:\"" + serviceName + '\"' +
                '}';
    }

    protected Map<String, Object> args = new ConcurrentHashMap<>();

    /**
     * Stores an integer in this service event's shared argument map.
     *
     * @param name map key
     * @param i value to store
     * @return the same value stored in the map
     * @since 1.6.5
     */
    public int putInt(String name, int i) {
        args.put(name, i);
        return i;
    }

    /**
     * Stores a string in this service event's shared argument map.
     *
     * @param name map key
     * @param str value to store
     * @return the same value stored in the map
     * @since 1.6.5
     */
    public String putString(String name, String str) {
        args.put(name, str);
        return str;
    }

    /**
     * Stores a double in this service event's shared argument map.
     *
     * @param name map key
     * @param d value to store
     * @return the same value stored in the map
     * @since 1.6.5
     */
    public double putDouble(String name, double d) {
        args.put(name, d);
        return d;
    }

    /**
     * Stores a boolean in this service event's shared argument map.
     *
     * @param name map key
     * @param b value to store
     * @return the same value stored in the map
     * @since 1.6.5
     */
    public boolean putBoolean(String name, boolean b) {
        args.put(name, b);
        return b;
    }

    /**
     * Stores an object in this service event's shared argument map. Guest-language objects are rejected.
     *
     * @param name map key
     * @param o value to store
     * @return the same value stored in the map
     * @since 1.6.5
     */
    public Object putObject(String name, Object o) {
        if (runner.extensions.isGuestObject(o)) {
            throw new AssertionError("Cannot put a guest object into event");
        }
        args.put(name, o);
        return o;
    }

    /**
     * Returns the stored value's type label, or {@code null} if the key is absent.
     *
     * @param name key to inspect
     * @return {@code Int}, {@code String}, {@code Double}, {@code Boolean}, or {@code Object}; {@code null} if absent
     * @since 1.6.5
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
     * Returns the value if it is an integer; otherwise returns {@code null}.
     *
     * @param name key to read
     * @return the integer value, or {@code null}
     * @since 1.6.5
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
     * Increments the integer value and returns its previous value; returns {@code null} without changing the map if it is absent or not an integer.
     *
     * @param name key to read and increment
     * @return the value before increment, or {@code null}
     * @since 1.6.5
     */
    @Nullable
    public Integer getAndIncrementInt(String name) {
        Object i = args.get(name);
        if (i instanceof Integer) {
            args.put(name, ((Integer) i) + 1);
            return (Integer) i;
        } else {
            return null;
        }
    }

    /**
     * Decrements the integer value and returns its previous value; returns {@code null} without changing the map if it is absent or not an integer.
     *
     * @param name key to read and decrement
     * @return the value before decrement, or {@code null}
     * @since 1.6.5
     */
    @Nullable
    public Integer getAndDecrementInt(String name) {
        Object i = args.get(name);
        if (i instanceof Integer) {
            args.put(name, ((Integer) i) - 1);
            return (Integer) i;
        } else {
            return null;
        }
    }

    /**
     * Increments the integer value and returns the result; returns {@code null} without changing the map if it is absent or not an integer.
     *
     * @param name key to increment
     * @return the value after increment, or {@code null}
     * @since 1.6.5
     */
    @Nullable
    public Integer incrementAndGetInt(String name) {
        Object i = args.get(name);
        if (i instanceof Integer) {
            args.put(name, i = ((Integer) i) + 1);
            return (Integer) i;
        } else {
            return null;
        }
    }

    /**
     * Decrements the integer value and returns the result; returns {@code null} without changing the map if it is absent or not an integer.
     *
     * @param name key to decrement
     * @return the value after decrement, or {@code null}
     * @since 1.6.5
     */
    @Nullable
    public Integer decrementAndGetInt(String name) {
        Object i = args.get(name);
        if (i instanceof Integer) {
            args.put(name, i = ((Integer) i) - 1);
            return (Integer) i;
        } else {
            return null;
        }
    }

    /**
     * Returns the value if it is a string; otherwise returns {@code null}.
     *
     * @param name key to read
     * @return the string value, or {@code null}
     * @since 1.6.5
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
     * Returns the value if it is a double; otherwise returns {@code null}.
     *
     * @param name key to read
     * @return the double value, or {@code null}
     * @since 1.6.5
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
     * Returns the value if it is a boolean; otherwise returns {@code null}.
     *
     * @param name key to read
     * @return the boolean value, or {@code null}
     * @since 1.6.5
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
     * Toggles the boolean value and returns the result; returns {@code null} without changing the map if it is absent or not a boolean.
     *
     * @param name key to toggle
     * @return the value after toggling, or {@code null}
     * @since 1.6.5
     */
    @Nullable
    public Boolean toggleBoolean(String name) {
        Object i = args.get(name);
        if (i instanceof Boolean) {
            args.put(name, !(Boolean) i);
            return !(Boolean) i;
        } else {
            return null;
        }
    }

    /**
     * Returns the value stored for this key, or {@code null} if absent.
     *
     * @param name key to read
     * @return the stored value, or {@code null}
     * @since 1.6.5
     */
    @Nullable
    public Object getObject(String name) {
        return args.get(name);
    }

    /**
     * Removes a key from this service event's shared argument map.
     *
     * @param key key to remove
     * @since 1.6.5
     */
    public void remove(String key) {
        args.remove(key);
    }

    /**
     * Returns the live argument map; direct mutations bypass {@link #putObject(String, Object)}'s guest-object check.
     * @return live shared argument map
     */
    public Map<String, Object> getRaw() {
        return args;
    }

}
