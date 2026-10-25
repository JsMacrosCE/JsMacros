package com.jsmacrosce;

import com.jsmacrosce.jsmacros.client.api.classes.types.*;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.HorseEntityHelper;
import com.jsmacrosce.jsmacros.core.library.ScriptTypeRegistry;

import java.util.Set;

/** Validates every shipped catalog without bootstrapping a Minecraft client. */
public final class ScriptTypeRegistryTest {
    public static void main(String[] args) {
        ScriptTypeRegistry registry = new ScriptTypeRegistry();
        for (Class<?> catalog : new Class<?>[]{EntityHelpers.class, Inventories.class, Widgets.class,
                RenderElements.class, NBTHelpers.class, HitResults.class, ScriptClasses.class}) {
            registry.register(catalog);
        }
        var namespaces = registry.snapshot();
        if (!namespaces.keySet().equals(Set.of("EntityHelpers", "Inventories", "Widgets", "RenderElements",
                "NBTHelpers", "HitResults", "ScriptClasses"))) {
            throw new AssertionError("Unexpected script namespaces: " + namespaces.keySet());
        }
        if (namespaces.get("EntityHelpers").get("Horse") != HorseEntityHelper.class) {
            throw new AssertionError("Horse export lost its actual helper identity");
        }
        if (!namespaces.get("EntityHelpers").containsKey("Enderman")) {
            throw new AssertionError("Stonecutter changed the public Enderman export");
        }
        int count = namespaces.values().stream().mapToInt(java.util.Map::size).sum();
        System.out.println("Validated " + namespaces.size() + " script namespaces and " + count + " type exports");
    }
}
