package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Function;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(CycleButton.class)
public interface MixinCyclingButton<T> {

    @Invoker("cycleValue")
    void invokeCycle(int amount);

    @Invoker("createLabelForValue")
    Component invokeComposeText(T value);

    @Accessor("valueStringifier")
    Function<T, Component> getValueToText();

}
