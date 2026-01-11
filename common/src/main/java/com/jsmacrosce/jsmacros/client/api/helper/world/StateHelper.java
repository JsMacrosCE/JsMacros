package com.jsmacrosce.jsmacros.client.api.helper.world;

import net.minecraft.Util;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public abstract class StateHelper<U extends StateHolder<?, ?>> extends BaseHelper<U> {

    public StateHelper(U base) {
        super(base);
    }

    /**
     * @return a map of the state properties with its identifier and value.
     * @since 1.8.4
     */
    public Map<String, String> toMap() {
        return base.getValues().entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().getName(), entry -> Util.getPropertyName(entry.getKey(), entry.getValue())));
    }

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
