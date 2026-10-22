package com.jsmacrosce.jsmacros.client.util;

//? if >=26.1 {
/*import net.minecraft.world.inventory.ContainerInput;
*///? } else {
import net.minecraft.world.inventory.ClickType;
//? }

/**
 * Provides helpers for the container click-action enum used by the current Minecraft target.
 * The target-specific overloads expose the action's ordinal and test whether it is
 * {@code THROW}.
 */
public final class ContainerInputCompat {

    private ContainerInputCompat() {
    }

    //? if >=26.1 {
    /*/^*
     * Returns the action's zero-based enum ordinal, which is not guaranteed to be a stable wire or protocol ID.
     * @param action non-null container click action
     * @return the action's zero-based enum ordinal
     ^/
    public static int actionId(ContainerInput action) {
        return action.ordinal();
    }

    /^*
     * Tests whether the action is {@code THROW}; {@code null} is treated as a non-throw action.
     * @param action action to test; may be {@code null}
     * @return {@code true} only for {@code THROW}; {@code false} for other actions or {@code null}
     ^/
    public static boolean isThrow(ContainerInput action) {
        return action == ContainerInput.THROW;
    }
    *///? } else {
    /**
     * Returns the action's zero-based enum ordinal, which is not guaranteed to be a stable wire or protocol ID.
     * @param action non-null container click action
     * @return the action's zero-based enum ordinal
     */
    public static int actionId(ClickType action) {
        return action.ordinal();
    }

    /**
     * Tests whether the action is {@code THROW}; {@code null} is treated as a non-throw action.
     * @param action action to test; may be {@code null}
     * @return {@code true} only for {@code THROW}; {@code false} for other actions or {@code null}
     */
    public static boolean isThrow(ClickType action) {
        return action == ClickType.THROW;
    }
    //? }
}
