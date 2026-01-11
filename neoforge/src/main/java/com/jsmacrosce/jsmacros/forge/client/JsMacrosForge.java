package com.jsmacrosce.jsmacros.forge.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jsmacrosce.jsmacros.client.JsMacros;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.inventory.CommandManager;
import com.jsmacrosce.jsmacros.forge.client.api.classes.CommandManagerForge;
import com.jsmacrosce.jsmacros.forge.client.forgeevents.ForgeEvents;

@Mod(JsMacros.MOD_ID)
public class JsMacrosForge {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsMacrosForge.class);

    public JsMacrosForge(IEventBus modBus, ModContainer modContainer) {
        LOGGER.error("JsMacrosForge constructor");
        System.setProperty("jnr.ffi.provider", "cause.class.not.found.please");

        modBus.addListener(this::onInitialize);
        modBus.addListener(this::onInitializeClient);
        modBus.addListener(this::onRegisterKeyMappings);
        modBus.addListener(ForgeEvents::onRegisterGuiOverlays);
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () -> (mc, parent) -> {
            JsMacrosClient.prevScreen.setParent(parent);
            return JsMacrosClient.prevScreen;
        });
        JsMacros.onInitialize();
    }

    @SubscribeEvent
    public void onInitialize(FMLCommonSetupEvent event) {

        // initialize loader-specific stuff
        CommandManager.instance = new CommandManagerForge();
        ForgeEvents.init();
    }

    @SubscribeEvent
    public void onInitializeClient(FMLClientSetupEvent event) {
        JsMacrosClient.onInitializeClient();
    }

    @SubscribeEvent
    public void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(JsMacrosClient.keyBinding);
    }

}
