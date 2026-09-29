package com.jsmacrosce.jsmacros.client.api.helper.world;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import net.minecraft.Util;

/**
 * The shared base for the two helpers that wrap a state: a block state and a fluid state. Both of
 * the things they wrap are a state holder, and a state holder is nothing more than a set of named
 * properties and the value each one is currently set to, so both kinds get the same two methods
 * here.<br>
 * A script does not construct this class. {@link BlockStateHelper} and {@link FluidStateHelper}
 * extend it, and one of those is what comes back from a world lookup, which makes this class a
 * useful thing to test with {@code instanceof} when a call can hand back either kind.
 * <p>
 * The one thing worth knowing before calling anything here is that {@link #with(String, String)}
 * builds a <b>new</b> state and hands it back wrapped: it leaves the helper you called it on
 * untouched, so setting a property this way never changes the world.
 * example:
 * <pre>
 * const reg = Client.getRegistryManager();
 * const state = reg.getBlockState("minecraft:oak_stairs", "[facing=east]");
 *
 * // every property, keyed by the name with() takes and valued by what the game
 * // calls the value it currently holds
 * for (const [name, value] of state.toMap()) {
 *   Chat.log(`${name} = ${value}`);
 * }
 *
 * // with() gives back a new helper, it does not change the one you asked
 * const turned = state.with("facing", "north");
 * Chat.log(state.toMap().facing);       // still east
 * Chat.log(turned.toMap().facing);      // now north
 *
 * // a fluid state is a state helper too
 * const water = state.getFluidState();
 * const StateHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.StateHelper");
 * Chat.log(water instanceof StateHelper);   // true
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public abstract class StateHelper<U extends StateHolder<?, ?>> extends BaseHelper<U> {

    public StateHelper(U base) {
        super(base);
    }

    /**
     * Every property the wrapped state has, keyed by the property's name. The value is the name the
     * game uses for whatever that property is currently set to, so a state sitting on its defaults
     * still reports those defaults rather than nothing.<br>
     * The keys are exactly the strings {@link #with(String, String)} accepts, which makes this the
     * call to make first when a script has to name a property it was not written knowing.
     * example:
     * <pre>
     * const reg = Client.getRegistryManager();
     * const wire = reg.getBlockState("minecraft:redstone_wire");
     * const props = wire.toMap();
     *
     * // read one property by name
     * Chat.log(`this wire carries power ${props.power}`);
     *
     * // and walk the rest of them
     * for (const [name, value] of props) {
     *   if (name !== "power") {
     *     Chat.log(`${name} is ${value}`);
     *   }
     * }
     * </pre>
     *
     * @return a map of the state properties with its identifier and value.
     * @since 1.8.4
     */
    public Map<String, String> toMap() {
        //? if >=26.1 {
        /*return base.getValues().collect(Collectors.toMap(
                entry -> entry.property().getName(),
                entry -> Util.getPropertyName(entry.property(), entry.value())
        ));
        *///?} else {
        return base.getValues().entrySet().stream().collect(Collectors.toMap(
            entry -> entry.getKey().getName(),
            entry -> Util.getPropertyName(entry.getKey(), entry.getValue()))
        );
        //?}
    }

    /**
     * Sets one property and hands the resulting state back as a new helper. The helper this was
     * called on is left exactly as it was, so nothing about the world changes: the new state is for
     * reading, or for handing to something that takes a block state.<br>
     * A state that has no property of that name is an error rather than a quiet no-op, and so is a
     * value the property will not accept. Both spellings come from {@link #toMap()}, which is the
     * reliable way to find out what a given state actually calls them.
     * example:
     * <pre>
     * const reg = Client.getRegistryManager();
     * const log = reg.getBlockState("minecraft:oak_log");
     *
     * // set a property the log really has, and read the result back
     * const before = log.with("axis", "x");
     * Chat.log(`a log on the x axis is ${before.toMap().axis}, and the original is still ${log.toMap().axis}`);
     *
     * // a property this state does not have is an error
     * try {
     *   log.with("not_a_property", "true");
     * } catch (e) {
     *   Chat.log("a log has no such property");
     * }
     *
     * // and so is a value the property will not take
     * try {
     *   log.with("axis", "sideways");
     * } catch (e) {
     *   Chat.log("that is not a value an axis can hold");
     * }
     * </pre>
     *
     * @param property the name of the property to set, as {@link #toMap()} spells it.
     * @param value    the value to set that property to, as {@link #toMap()} spells it.
     * @return a new helper of the same kind holding the state with that property set.
     * @throws IllegalArgumentException if the state has no property of that name, or if the value
     *                                  is not one that property accepts.
     * @since 1.8.4
     */
    public <T extends Comparable<?>> StateHelper<U> with(String property, String value) {
        Optional<Property<?>> prop = base.getProperties().stream().filter(p -> p.getName().equals(property)).findFirst();
        if (prop.isEmpty()) {
            throw new IllegalArgumentException("Property " + property + " does not exist for this state");
        }
        return with(prop.get(), value);
    }

    private <T extends Comparable<T>> StateHelper<U> with(Property<T> property, String value) {
        Optional<T> arg = property.getValue(value);
        if (arg.isEmpty()) {
            throw new IllegalArgumentException("Value " + value + " is not valid for the property " + property);
        }
        return create((U) base.setValue(property, arg.get()));
    }

    protected abstract StateHelper<U> create(U base);

}
