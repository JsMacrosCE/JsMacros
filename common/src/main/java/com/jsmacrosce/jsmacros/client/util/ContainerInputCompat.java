package com.jsmacrosce.jsmacros.client.util;

//? if >=26.1 {
/*import net.minecraft.world.inventory.ContainerInput;
*///? } else {
import net.minecraft.world.inventory.ClickType;
//? }

/**
 * Bridges the container click-action enum rename ({@code ClickType} -> {@code ContainerInput}).
 * <p>
 * The two enums are declared in the same order and carry the same ids in every
 * supported version, so {@code ordinal()} stays a stable action id across the rename and
 * the script-facing {@code EventClickSlot} action value does not change.
 * <p>
 * Overloads are declared per version rather than accepting {@link Object} so that call
 * sites need no version gate: the compiler picks the right overload from the static type
 * of the action.
 */
public final class ContainerInputCompat {

    private ContainerInputCompat() {
    }

    //? if >=26.1 {
    /*public static int actionId(ContainerInput action) {
        return action.ordinal();
    }

    public static boolean isThrow(ContainerInput action) {
        return action == ContainerInput.THROW;
    }
    *///? } else {
    public static int actionId(ClickType action) {
        return action.ordinal();
    }

    public static boolean isThrow(ClickType action) {
        return action == ClickType.THROW;
    }
    //? }
}
