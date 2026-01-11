package com.jsmacrosce.jsmacros.client.gui.screens;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.Screen;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.event.impl.EventKey;
import com.jsmacrosce.jsmacros.client.config.ClientConfigV2;
import com.jsmacrosce.jsmacros.client.gui.containers.MacroContainer;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.event.BaseListener;
import com.jsmacrosce.jsmacros.core.event.Event;
import com.jsmacrosce.jsmacros.core.event.IEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//? if >1.21.8 {
/*import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}

public class KeyMacrosScreen extends MacroScreen {

    public KeyMacrosScreen(Screen parent) {
        super(parent);
    }

    @Override
    public void init() {
        super.init();
        keyScreen.setColor(0x4FFFFFFF);

        Set<IEventListener> listeners = JsMacrosClient.clientCore.eventRegistry.getListeners().get(EventKey.class.getAnnotation(Event.class).value());
        List<ScriptTrigger> macros = new ArrayList<>();

        if (listeners != null) {
            for (IEventListener event : ImmutableList.copyOf(listeners)) {
                if (event instanceof BaseListener && ((BaseListener) event).getRawTrigger().triggerType != ScriptTrigger.TriggerType.EVENT) {
                    macros.add(((BaseListener) event).getRawTrigger());
                }
            }
        }

        macros.sort(JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).getSortComparator());

        for (ScriptTrigger macro : macros) {
            addMacro(macro);
        }
    }

    @Override
    //? if >1.21.8 {
    /*public boolean keyReleased(KeyEvent keyEvent) {
    int modifiers = keyEvent.modifiers();
    *///?} else {
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
    //?}
        String translationKey = EventKey.getKeyModifiers(modifiers);
        if (!translationKey.equals("")) {
            translationKey += "+";
        }
        //? if >1.21.8 {
        /*translationKey += InputConstants.getKey(keyEvent).getName();
        *///?} else {
        translationKey += InputConstants.getKey(keyCode, scanCode).getName();
        //?}
        for (MacroContainer macro : (List<MacroContainer>) (List) macros) {
            if (!macro.onKey(translationKey)) {
                return false;
            }
        }

        //? if >1.21.8 {
        /*return super.keyReleased(keyEvent);
         *///?} else {
        return super.keyReleased(keyCode, scanCode, modifiers);
        //?}
    }

    @Override
    //? if >1.21.8 {
    /*public boolean mouseReleased(MouseButtonEvent buttonEvent) {
        boolean hasShift = buttonEvent.hasShiftDown();
        boolean hasCtrl = buttonEvent.hasControlDown();
        boolean hasAlt = buttonEvent.hasAltDown();
        int button = buttonEvent.button();
    *///?} else {
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean hasShift = hasShiftDown();
        boolean hasCtrl = hasControlDown();
        boolean hasAlt = hasAltDown();
    //?}
        int mods = 0;
        if (hasShift) {
            mods += 1;
        }
        if (hasCtrl) {
            mods += 2;
        }
        if (hasAlt) {
            mods += 4;
        }
        String translationKey = EventKey.getKeyModifiers(mods);
        if (!translationKey.equals("")) {
            translationKey += "+";
        }
        translationKey += InputConstants.Type.MOUSE.getOrCreate(button).getName();
        for (MacroContainer macro : (List<MacroContainer>) (List) macros) {
            if (!macro.onKey(translationKey)) {
                return false;
            }
        }
        //? if >1.21.8 {
        /*return super.mouseReleased(buttonEvent);
        *///?} else {
        return super.mouseReleased(mouseX, mouseY, button);
        //?}
    }

}
