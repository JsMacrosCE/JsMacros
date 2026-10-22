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
 * Base helper for block and fluid states, exposing their named properties. {@link #with(String,
 * String)} returns a new helper without modifying the wrapped state.
 * <pre>
 * const reg = Client.getRegistryManager();
 * const state = reg.getBlockState("minecraft:oak_stairs", "[facing=east]");
 * const northFacing = state.with("facing", "north");
 * Chat.log(northFacing.toMap().facing);
 * </pre>
 *
 * @author Etheradon
 * @param <U> the block or fluid state wrapped by this helper.
 * @since 1.8.4
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public abstract class StateHelper<U extends StateHolder<?, ?>> extends BaseHelper<U> {

    /** Stores the supplied block or fluid state as the value wrapped by this helper.
     * @param base block or fluid state to wrap
     */
    public StateHelper(U base) {
        super(base);
    }

    /**
     * Returns every property of the wrapped state, including properties at their default values,
     * keyed by property name and valued by the property's game-defined value name. These names can
     * be passed to {@link #with(String, String)}.
     *
     * @return the property names and their current values.
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
     * Returns a new helper with the named property set; the original helper and world are unchanged.
     * Throws {@link IllegalArgumentException} if the property is absent or the value is invalid.
     * Property names and value spellings are shown by {@link #toMap()}.
     *
     * @param property property name, as shown by {@link #toMap()}.
     * @param value value spelling, as shown by {@link #toMap()}.
     * @return a new helper of the same kind holding the state with that property set.
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
