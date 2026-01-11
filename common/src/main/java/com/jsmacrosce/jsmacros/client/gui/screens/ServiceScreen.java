package com.jsmacrosce.jsmacros.client.gui.screens;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.config.ClientConfigV2;
import com.jsmacrosce.jsmacros.client.gui.containers.ServiceContainer;
import com.jsmacrosce.jsmacros.client.gui.containers.ServiceListTopbar;
import com.jsmacrosce.jsmacros.client.gui.overlays.FileChooser;
import com.jsmacrosce.jsmacros.core.service.ServiceTrigger;
import com.jsmacrosce.wagyourgui.containers.MultiElementContainer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ServiceScreen extends MacroScreen {

    public ServiceScreen(Screen parent) {
        super(parent);
    }

    @Override
    protected void init() {
        super.init();
        serviceScreen.setColor(0x4FFFFFFF);
        List<String> services = new ArrayList<>(JsMacrosClient.clientCore.services.getServices());

        services.sort(JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).getServiceSortComparator());

        for (String service : services) {
            addService(service);
        }
    }

    public void addService(String service) {
        macros.add(new ServiceContainer(this.width / 12, topScroll + macros.size() * 16, this.width * 5 / 6, 14, this.font, this, service));
        macroScroll.setScrollPages(((macros.size() + 1) * 16) / (double) Math.max(1, this.height - 40));
    }

    @Override
    public void removeMacro(MultiElementContainer<MacroScreen> macro) {
        for (AbstractWidget b : macro.getButtons()) {
            removeWidget(b);
        }
        JsMacrosClient.clientCore.services.unregisterService(((ServiceContainer) macro).service);
        macros.remove(macro);
        setMacroPos();
    }

    @Override
    public void setFile(MultiElementContainer<MacroScreen> macro) {
        ServiceTrigger m = ((ServiceContainer) macro).getTrigger();
        final File file;
        if (m.file.isAbsolute()) {
            file = m.file.toFile();
        } else {
            file = JsMacrosClient.clientCore.config.macroFolder.toPath().resolve(m.file).toFile();
        }
        File dir = JsMacrosClient.clientCore.config.macroFolder;
        if (!file.equals(JsMacrosClient.clientCore.config.macroFolder)) {
            dir = file.getParentFile();
        }
        openOverlay(new FileChooser(width / 4, height / 4, width / 2, height / 2, this.font, dir, file, this, ((ServiceContainer) macro)::setFile, this::editFile));
    }

    @Override
    protected MultiElementContainer<MacroScreen> createTopbar() {
        return (MultiElementContainer) new ServiceListTopbar(this, this.width / 12, 25, this.width * 5 / 6, 14, this.font);
    }

    @Override
    public void onClose() {
        JsMacrosClient.clientCore.services.save();
        super.onClose();
    }

}
