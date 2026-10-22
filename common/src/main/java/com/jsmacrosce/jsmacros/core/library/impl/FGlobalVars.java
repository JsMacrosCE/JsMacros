package com.jsmacrosce.jsmacros.core.library.impl;

import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.library.BaseLibrary;
import com.jsmacrosce.jsmacros.core.library.Library;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Global" variables for passing to other contexts.
 * <p>
 * An instance of this class is passed to scripts as the {@code GlobalVars} variable.
 * <br>
 * These are shared by every script in the profile rather than by one script, which is what makes
 * them a way for two scripts to talk: one writes a value and another reads it back later, with no
 * reference between the two and neither one holding the other alive. The space outlives the scripts
 * that use it, since it belongs to the profile, so a value written by a macro is still there for
 * the next run.<br>
 * The trade is that the space is not typed. Every entry is a name and an object, and the typed
 * getters below only hand a value back when it is the type they were named for, so a
 * {@code getInt} on a string that happens to look like a number gives {@code null} rather than
 * parsing it. {@link #getType(String) getType} is what tells the two apart.<br>
 * Values written by a script are ordinary Java objects, not script values, so a guest object is
 * refused by {@link #putObject(String, Object) putObject}. Numbers written by
 * {@link #putInt(String, int) putInt} come back through {@link #getObject(String) getObject} as a
 * Java number.
 * example:
 * <pre>
 * // a counter that survives between scripts and between runs, which is the
 * // point of it
 * GlobalVars.putInt("sessions", 0);
 * const next = GlobalVars.incrementAndGetInt("sessions");
 * Chat.log(`this is session ${next}`);
 *
 * // a flag any script can flip and any script can read
 * GlobalVars.putBoolean("myFeatureEnabled", true);
 * if (GlobalVars.getBoolean("myFeatureEnabled")) {
 *   Chat.log("the feature is on");
 * }
 *
 * // and getType is how a value written by one script is read by another safely,
 * // since the typed getters give null on a type mismatch rather than coercing
 * GlobalVars.putDouble("ratio", 0.5);
 * Chat.log(`ratio is a ${GlobalVars.getType("ratio")}`);
 * </pre>
 * @author Wagyourtail
 * @since 1.0.4
 */
@Library("GlobalVars")
@SuppressWarnings("unused")
public class FGlobalVars extends BaseLibrary {
    /**
     * the backing map every call on this library reads and writes, exposed rather than kept
     * private.<br>
     * It is the live map, not a copy, so a script that puts into it is writing the same space the
     * typed calls use, and the typed calls see anything put in this way. Two things come with that.
     * A guest object goes in unchecked, where
     * {@link #putObject(String, Object) putObject} would have refused it and something reading it
     * back later fails instead, and a name can be overwritten with a value of a different type,
     * where {@link #getType(String) getType} reports the new type and the typed getter for the old
     * one silently gives {@code null}. The counter and toggle calls here go through the map's
     * atomic compute, so a value written directly is still seen and counted by them.
     * <br>
     * It is shared by every script in the profile, since this library is built once for the
     * profile rather than per script run, and it is never cleared. A profile that is reloaded from
     * disk starts with a new one, and a profile that is not keeps everything any script has ever
     * put in it. {@link #getRaw()} hands the same map back as a method.
     */
    public Map<String, Object> globalRaw = new ConcurrentHashMap<>();

    public FGlobalVars(Core<?, ?> runner) {
        super(runner);
    }

    /**
     * Put an Integer into the global variable space.
     *
     * @param name
     * @param i
     * @return
     * @since 1.0.4
     */
    public int putInt(String name, int i) {
        globalRaw.put(name, i);
        return i;
    }

    /**
     * put a String into the global variable space.
     *
     * @param name
     * @param str
     * @return
     * @since 1.0.4
     */
    public String putString(String name, String str) {
        globalRaw.put(name, str);
        return str;
    }

    /**
     * put a Double into the global variable space.
     *
     * @param name
     * @param d
     * @return
     * @since 1.0.8
     */
    public double putDouble(String name, double d) {
        globalRaw.put(name, d);
        return d;
    }

    /**
     * put a Boolean into the global variable space.
     *
     * @param name
     * @param b
     * @return
     * @since 1.1.7
     */
    public boolean putBoolean(String name, boolean b) {
        globalRaw.put(name, b);
        return b;
    }

    /**
     * put anything else into the global variable space.
     * <br>
     * The escape hatch for the value types with no call of their own, such as a compiled class, a
     * Java collection or a wrapped helper. What it takes has to be a Java object rather than a
     * script value, since the space is read from other script contexts and a value belonging to
     * one language instance cannot be used from another; a script value is refused with an
     * {@link AssertionError} rather than stored and failed on later.<br>
     * It does no type checking against what was there before, so a name can be given a value of a
     * completely different type, which then reads back as {@code null} from the typed getter for
     * the old type and as {@code "Object"} from {@link #getType(String) getType}.
     * example:
     * <pre>
     * // a java object goes in, and reads back as an object since it is not one of
     * // the four types that have a getter of its own
     * GlobalVars.putObject("lastResult", JavaUtils.createHashMap());
     * print(`that reads as a ${GlobalVars.getType("lastResult")}`);
     *
     * // a name already holding a different type is simply replaced, and the typed
     * // getter for the old type then gives null rather than converting
     * GlobalVars.putInt("shared", 1);
     * GlobalVars.putObject("shared", "now a string");
     * print(`getInt gives ${GlobalVars.getInt("shared")}, getObject gives ${GlobalVars.getObject("shared")}`);
     *
     * // a guest object is refused rather than stored
     * // GlobalVars.putObject("bad", { a: 1 });
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
     * Returns the type of the defined item in the global variable space as a string.
     * <br>
     * One of {@code "Int"}, {@code "String"}, {@code "Double"}, {@code "Boolean"} or
     * {@code "Object"}, and {@code null} for a name that holds nothing at all. {@code "Object"} is
     * the catch-all rather than a distinct type, so it covers a {@link Long}, a {@link Float} or
     * anything else put there through {@link #putObject(String, Object) putObject}, and covers a
     * number written by the wrong typed call. That is also why it cannot be told apart from an
     * object, and why the typed getter for whatever type was meant gives {@code null} in that case:
     * an integer stored as a long is reported as an object, not as a number that happens to be
     * whole.
     * example:
     * <pre>
     * GlobalVars.putInt("count", 1);
     * GlobalVars.putDouble("ratio", 0.5);
     * GlobalVars.putString("name", "notch");
     * GlobalVars.putBoolean("enabled", true);
     * print(`${GlobalVars.getType("count")} and ${GlobalVars.getType("ratio")}`);
     * print(`${GlobalVars.getType("name")} and ${GlobalVars.getType("enabled")}`);
     *
     * // an object put there without a typed call is reported as an object, and the
     * // typed getter for it gives null rather than converting
     * GlobalVars.putObject("raw", JavaUtils.createArrayList());
     * print(`${GlobalVars.getType("raw")}, and getInt gives ${GlobalVars.getInt("raw")}`);
     *
     * // a name that holds nothing at all gives null rather than a type
     * print(`nothing there: ${GlobalVars.getType("neverSet")}`);
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
     * Gets an Integer from the global variable space.
     *
     * @param name
     * @return
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
     * Gets an Integer from the global variable space. and then increment it there.
     * <br>
     * This is the post-increment form: it hands back the value as it was and leaves the incremented
     * one behind, so it is the one to reach for when a counter is being read on the way past. A
     * name that holds nothing is created as {@code 0} and given {@code 1}, so the first call
     * returns zero rather than one. That is the difference from
     * {@link #incrementAndGetInt(String) incrementAndGetInt}, which returns the one it stored.<br>
     * A name holding something that is not an integer is left exactly as it is and this gives
     * {@code null}, so the increment is skipped rather than replacing whatever is there. The read
     * and the write are one atomic step, so two scripts counting the same name cannot both be
     * handed the same value.
     * example:
     * <pre>
     * // a name that held nothing is created as 1 and reported as 0 on the first call
     * print(`first call: ${GlobalVars.getAndIncrementInt("hits")}`);
     * print(`second call: ${GlobalVars.getAndIncrementInt("hits")}`);
     * // which left the incremented value behind
     * print(`stored now: ${GlobalVars.getInt("hits")}`);
     *
     * // a name holding a string is left alone rather than overwritten
     * GlobalVars.putString("hits", "not a number");
     * print(`skipped: ${GlobalVars.getAndIncrementInt("hits")}, still ${GlobalVars.getType("hits")}`);
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
     * Gets an integer from the global variable space. and then decrement it there.
     *
     * @param name
     * @return
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
     * increment an Integer in the global variable space. then return it.
     * <br>
     * The pre-increment form: it hands back the value it stored rather than the one that was
     * there, so a counter read after being moved on starts at one where
     * {@link #getAndIncrementInt(String) getAndIncrementInt} would have started at zero. A name
     * that holds nothing is created as {@code 1} and one is returned, since there is no earlier
     * value to report.<br>
     * A name holding something that is not an integer is left exactly as it is and this gives
     * {@code null}, so nothing is written. The read and the write are one atomic step, so two
     * scripts counting the same name cannot both be handed the same value.
     * example:
     * <pre>
     * // a name that held nothing is created as 1 and that is what comes back
     * print(`first call: ${GlobalVars.incrementAndGetInt("deaths")}`);
     * print(`second call: ${GlobalVars.incrementAndGetInt("deaths")}`);
     *
     * // a name holding a string is left alone rather than overwritten
     * GlobalVars.putString("deaths", "not a number");
     * print(`skipped: ${GlobalVars.incrementAndGetInt("deaths")}, still ${GlobalVars.getType("deaths")}`);
     * </pre>
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
     * decrement an Integer in the global variable space. then return it.
     *
     * @param name
     * @return
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
     * Gets a String from the global variable space
     *
     * @param name
     * @return
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
     * Gets a Double from the global variable space.
     *
     * @param name
     * @return
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
     * Gets a Boolean from the global variable space.
     *
     * @param name
     * @return
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
     * toggles a global boolean and returns its new value
     *
     * @param name
     * @return
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
     * Gets an Object from the global variable space.
     *
     * @param name
     * @return
     * @since 1.1.7
     */
    public Object getObject(String name) {
        return globalRaw.get(name);
    }

    /**
     * removes a key from the global variable space.
     *
     * @param key
     * @since 1.2.0
     */
    public void remove(String key) {
        globalRaw.remove(key);
    }

    /**
     * the backing map itself, the same one {@link #globalRaw} is.
     * <br>
     * It is the live map rather than a copy, so reading it shows the current contents and writing
     * into it is a direct write into the global variable space. Going through it skips the checks
     * the typed calls make, most visibly the refusal of a guest object that
     * {@link #putObject(String, Object) putObject} makes, and it lets a name be given a value of a
     * different type than it had.<br>
     * It is a Java {@link Map}, so it has {@code size()}, {@code get(key)} and the rest of that
     * interface rather than the script side of it.
     * example:
     * <pre>
     * // a read through the map sees what the typed calls wrote
     * GlobalVars.putString("greeting", "hello");
     * print(`the map agrees: ${GlobalVars.getRaw().get("greeting")}`);
     *
     * // and a write through the map is a write into the space, though it skips the
     * // type checks the typed calls make
     * const raw = GlobalVars.getRaw();
     * raw.put("writtenByHand", "text");
     * print(`and the typed getter reads that: ${GlobalVars.getString("writtenByHand")}`);
     *
     * // the map is a Java one, so it is sized with size()
     * print(`there are ${raw.size()} entries in the space`);
     * </pre>
     *
     * @return the live backing map, not a copy of it.
     */
    public Map<String, Object> getRaw() {
        return globalRaw;
    }

}
