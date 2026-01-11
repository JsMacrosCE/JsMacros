package com.jsmacrosce.jsmacros.fabric.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.wagyourgui.BaseScreen;

public class ModMenuEntry implements ModMenuApi {
    private final JsMacroScreen jsmacrosscreenfactory = new JsMacroScreen();

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return jsmacrosscreenfactory;
    }

    public static class JsMacroScreen implements ConfigScreenFactory<BaseScreen> {
        @Override
        public BaseScreen create(Screen parent) {
            return JsMacrosClient.prevScreen;
        }

    }

}
