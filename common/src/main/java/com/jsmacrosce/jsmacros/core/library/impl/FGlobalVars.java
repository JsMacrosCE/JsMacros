package com.jsmacrosce.jsmacros.core.library.impl;

import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.library.BaseLibrary;
import com.jsmacrosce.jsmacros.core.library.Library;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores named Java values shared by scripts in the current profile. Values remain until removed
 * or the profile is reloaded. Typed getters return {@code null} for a missing value or a value of
 * another type; they do not coerce.
 * <pre>
 * GlobalVars.putInt("sessions", 0);
 * const next = GlobalVars.incrementAndGetInt("sessions");
 * print(`session ${next}`);
 * </pre>
 * @author Wagyourtail
 * @since 1.0.4
 */
@Library("GlobalVars")
@SuppressWarnings("unused")
public class FGlobalVars extends BaseLibrary {
    /**
     * Live backing map. Direct writes bypass the guest-object check in
     * {@link #putObject(String, Object)}; typed getters return {@code null} for values of other
     * types. Counter and toggle operations use atomic map computations.
     */
    public Map<String, Object> globalRaw = new ConcurrentHashMap<>();

    /**
     * Associates this library with the supplied core and initializes its backing map.
     *
     * @param runner core associated with this event library
     */
    public FGlobalVars(Core<?, ?> runner) {
        super(runner);
    }

    /**
     * Stores an integer under {@code name}.
     *
     * @param name key to store under
     * @param i value to store
     * @return the stored value
     * @since 1.0.4
     */
    public int putInt(String name, int i) {
        globalRaw.put(name, i);
        return i;
    }

    /**
     * Stores a string under {@code name}.
     *
     * @param name key to store under
     * @param str value to store
     * @return the stored value
     * @since 1.0.4
     */
    public String putString(String name, String str) {
        globalRaw.put(name, str);
        return str;
    }

    /**
     * Stores a double under {@code name}.
     *
     * @param name key to store under
     * @param d value to store
     * @return the stored value
     * @since 1.0.8
     */
    public double putDouble(String name, double d) {
        globalRaw.put(name, d);
        return d;
    }

    /**
     * Stores a boolean under {@code name}.
     *
     * @param name key to store under
     * @param b value to store
     * @return the stored value
     * @since 1.1.7
     */
    public boolean putBoolean(String name, boolean b) {
        globalRaw.put(name, b);
        return b;
    }

    /**
     * Stores a Java object under {@code name}. Guest objects are rejected with an
     * {@link AssertionError}; any existing value is replaced regardless of type.
     * <pre>
     * GlobalVars.putObject("lastResult", JavaUtils.createHashMap());
     * print(GlobalVars.getType("lastResult")); // "Object"
     * </pre>
     *
     * @param name the name to store under
     * @param o    the Java object to store
     * @return {@code o}, the same object that was stored
     * @throws AssertionError if {@code o} is a guest object rather than a Java one
     * @since 1.1.7
     */
    public Object putObject(String name, Object o) {
        if (runner.extensions.isGuestObject(o)) {
            throw new AssertionError("Cannot put a guest object into global variables");
        }
        globalRaw.put(name, o);
        return o;
    }

    /**
     * Returns the stored value's type name: {@code "Int"}, {@code "String"}, {@code "Double"},
     * {@code "Boolean"}, or {@code "Object"}; returns {@code null} if the name holds nothing.
     * {@code "Object"} covers all other stored Java types, including
     * numbers stored under a different numeric type; typed getters do not coerce them.
     * <pre>
     * GlobalVars.putInt("count", 1);
     * GlobalVars.putDouble("ratio", 0.5);
     * GlobalVars.putString("name", "notch");
     * GlobalVars.putBoolean("enabled", true);
     * print(`${GlobalVars.getType("count")} and ${GlobalVars.getType("ratio")}`);
     * print(`${GlobalVars.getType("name")} and ${GlobalVars.getType("enabled")}`);
     *
     * GlobalVars.putObject("raw", JavaUtils.createArrayList());
     * print(GlobalVars.getType("raw")); // "Object"
     * </pre>
     *
     * @param name the name to look up
     * @return one of the five type names above, or {@code null} if the name holds nothing
     * @since 1.0.4
     */
    @Nullable
    public String getType(String name) {
        Object i = globalRaw.get(name);
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
     * Returns the value under {@code name} only if it is an {@link Integer}.
     *
     * @param name key to look up
     * @return the integer value, or {@code null} if absent or of another type
     * @since 1.0.4
     */
    @Nullable
    public Integer getInt(String name) {
        Object i = globalRaw.get(name);
        if (i instanceof Integer) {
            return (Integer) i;
        } else {
            return null;
        }
    }

    /**
     * Atomically increments the integer under {@code name} and returns its previous value. An
     * absent key is set to {@code 1} and returns {@code 0}; a non-integer value is unchanged and
     * returns {@code null}. Unlike {@link #incrementAndGetInt(String)}, this returns the value
     * before incrementing.
     * <pre>
     * print(`first call: ${GlobalVars.getAndIncrementInt("hits")}`);
     * print(`second call: ${GlobalVars.getAndIncrementInt("hits")}`);
     * </pre>
     *
     * @param name the name to read and increment
     * @return the value before the increment, {@code 0} for a name that held nothing, or
     *         {@code null} if the name holds something that is not an integer
     * @since 1.6.5
     */
    @Nullable
    public Integer getAndIncrementInt(String name) {
        Integer[] value = new Integer[1];
        globalRaw.compute(name, (k, v) -> {
            if (v == null) {
                value[0] = 0;
                return 1;
            } else if (v instanceof Integer) {
                value[0] = (Integer) v;
                return ((Integer) v) + 1;
            } else {
                return v;
            }
        });
        return value[0];
    }

    /**
     * Decrements the integer under {@code name} and returns its previous value. A missing key is
     * set to {@code -1} and returns {@code 0}; a non-integer value is unchanged and returns
     * {@code null}. The read and update are atomic.
     *
     * @param name key to read and decrement
     * @return the value before decrement, {@code 0} if absent, or {@code null} if not an integer
     * @since 1.6.5
     */
    @Nullable
    public Integer getAndDecrementInt(String name) {
        Integer[] value = new Integer[1];
        globalRaw.compute(name, (k, v) -> {
            if (v == null) {
                value[0] = 0;
                return -1;
            } else if (v instanceof Integer) {
                value[0] = (Integer) v;
                return ((Integer) v) - 1;
            } else {
                return v;
            }
        });
        return value[0];

    }

    /**
     * Increments the integer under {@code name} and returns the new value. A missing key is set to
     * {@code 1} and returns {@code 1}; a non-integer value is unchanged and returns {@code null}. The read
     * and update are atomic. Unlike {@link #getAndIncrementInt(String)}, this returns the value
     * after incrementing.
     *
     * @param name the name to increment
     * @return the value after the increment, {@code 1} for a name that held nothing, or
     *         {@code null} if the name holds something that is not an integer
     * @since 1.6.5
     */
    @Nullable
    public Integer incrementAndGetInt(String name) {
        Object o = globalRaw.compute(name, (k, v) -> {
            if (v == null) {
                return 1;
            } else if (v instanceof Integer) {
                return ((Integer) v) + 1;
            } else {
                return v;
            }
        });
        if (o instanceof Integer) {
            return (Integer) o;
        }
        return null;
    }

    /**
     * Decrements the integer under {@code name} and returns the new value. A missing key is set to
     * and returns {@code -1}; a non-integer value is unchanged and returns {@code null}. The read
     * and update are atomic.
     *
     * @param name key to decrement
     * @return the value after decrement, {@code -1} if absent, or {@code null} if not an integer
     * @since 1.6.5
     */
    @Nullable
    public Integer decrementAndGetInt(String name) {
        Object o = globalRaw.compute(name, (k, v) -> {
            if (v == null) {
                return -1;
            } else if (v instanceof Integer) {
                return ((Integer) v) - 1;
            } else {
                return v;
            }
        });
        if (o instanceof Integer) {
            return (Integer) o;
        }
        return null;
    }

    /**
     * Returns the value under {@code name} only if it is a string.
     *
     * @param name key to look up
     * @return the string value, or {@code null} if absent or of another type
     * @since 1.0.4
     */
    @Nullable
    public String getString(String name) {
        Object i = globalRaw.get(name);
        if (i instanceof String) {
            return (String) i;
        } else {
            return null;
        }
    }

    /**
     * Returns the value under {@code name} only if it is a {@link Double}.
     *
     * @param name key to look up
     * @return the double value, or {@code null} if absent or of another type
     * @since 1.0.8
     */
    @Nullable
    public Double getDouble(String name) {
        Object i = globalRaw.get(name);
        if (i instanceof Double) {
            return (Double) i;
        } else {
            return null;
        }
    }

    /**
     * Returns the value under {@code name} only if it is a {@link Boolean}.
     *
     * @param name key to look up
     * @return the boolean value, or {@code null} if absent or of another type
     * @since 1.1.7
     */
    @Nullable
    public Boolean getBoolean(String name) {
        Object i = globalRaw.get(name);
        if (i instanceof Boolean) {
            return (Boolean) i;
        } else {
            return null;
        }
    }

    /**
     * Atomically toggles the boolean under {@code name} and returns the resulting value.
     * A missing key is set to {@code true}; a non-boolean value is left unchanged and yields
     * {@code null}.
     *
     * @param name key to toggle
     * @return the new value, or {@code null} if the existing value is not boolean
     * @since 1.6.5
     */
    @Nullable
    public Boolean toggleBoolean(String name) {
        Object o = globalRaw.compute(name, (k, v) -> {
            if (v == null) {
                return true;
            } else if (v instanceof Boolean) {
                return !((Boolean) v);
            } else {
                return v;
            }
        });
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        return null;
    }

    /**
     * Returns the value stored under {@code name}, regardless of its type.
     *
     * @param name key to look up
     * @return the stored value, or {@code null} if absent
     * @since 1.1.7
     */
    public Object getObject(String name) {
        return globalRaw.get(name);
    }

    /**
     * Removes a key from the global variable space.
     *
     * @param key key to remove
     * @since 1.2.0
     */
    public void remove(String key) {
        globalRaw.remove(key);
    }

    /**
     * Returns the live backing map, not a copy. Direct writes bypass the guest-object check in
     * {@link #putObject(String, Object)}.
     *
     * @return the live backing map
     */
    public Map<String, Object> getRaw() {
        return globalRaw;
    }

}
