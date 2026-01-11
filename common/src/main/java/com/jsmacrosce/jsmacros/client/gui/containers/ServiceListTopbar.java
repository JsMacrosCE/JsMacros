package com.jsmacrosce.jsmacros.client.gui.containers;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.config.ClientConfigV2;
import com.jsmacrosce.jsmacros.client.config.Sorting;
import com.jsmacrosce.jsmacros.client.gui.screens.ServiceScreen;
import com.jsmacrosce.jsmacros.core.service.ServiceTrigger;
import com.jsmacrosce.wagyourgui.containers.MultiElementContainer;
import com.jsmacrosce.wagyourgui.elements.Button;
import com.jsmacrosce.wagyourgui.overlays.TextPrompt;

import java.nio.file.Path;

public class ServiceListTopbar extends MultiElementContainer<ServiceScreen> {

    public ServiceListTopbar(ServiceScreen parent, int x, int y, int width, int height, Font textRenderer) {
        super(x, y, width, height, textRenderer, parent);
        init();
    }

    @Override
    public void init() {
        super.init();

        int w = width - 12;

        addRenderableWidget(new Button(x + 1, y + 1, w * 2 / 12 - 1, height - 3, textRenderer, JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).sortServicesMethod == Sorting.ServiceSortMethod.Name ? 0x3FFFFFFF : 0, 0xFF000000, 0x7F7F7F7F, 0xFFFFFFFF, Component.translatable("jsmacrosce.servicename"), (btn) -> {
            JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).sortServicesMethod = Sorting.ServiceSortMethod.Name;
            parent.reload();
        }));

        addRenderableWidget(new Button(x + w * 2 / 12 + 1, y + 1, w * 8 / 12 - 1, height - 3, textRenderer, JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).sortServicesMethod == Sorting.ServiceSortMethod.FileName ? 0x3FFFFFFF : 0, 0xFF000000, 0x7F7F7F7F, 0xFFFFFFFF, Component.translatable("jsmacrosce.file"), (btn) -> {
            JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).sortServicesMethod = Sorting.ServiceSortMethod.FileName;
            parent.reload();
        }));

        addRenderableWidget(new Button(x + w * 10 / 12 + 1, y + 1, w / 12, height - 3, textRenderer, JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).sortServicesMethod == Sorting.ServiceSortMethod.Enabled ? 0x3FFFFFFF : 0, 0xFF000000, 0x7F7F7F7F, 0xFFFFFFFF, Component.translatable("jsmacrosce.enabledstatus"), (btn) -> {
            JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).sortServicesMethod = Sorting.ServiceSortMethod.Enabled;
            parent.reload();
        }));

        addRenderableWidget(new Button(x + w * 11 / 12 + 1, y + 1, w / 12, height - 3, textRenderer, JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).sortServicesMethod == Sorting.ServiceSortMethod.Running ? 0x3FFFFFFF : 0, 0xFF000000, 0x7F7F7F7F, 0xFFFFFFFF, Component.translatable("jsmacrosce.runningstatus"), (btn) -> {
            JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).sortServicesMethod = Sorting.ServiceSortMethod.Running;
            parent.reload();
        }));

        addRenderableWidget(new Button(x + w - 1, y + 1, 11, height - 3, textRenderer, 0, 0xFF000000, 0x7F7F7F7F, 0xFFFFFFFF, Component.literal("+"), (btn) -> {
            openOverlay(new TextPrompt(parent.width / 4, parent.height / 4, parent.width / 2, parent.height / 2, textRenderer, Component.translatable("jsmacrosce.servicename"), "", getFirstOverlayParent(), (name) -> {
                if (JsMacrosClient.clientCore.services.registerService(name, new ServiceTrigger(Path.of(".").normalize(), false))) {
                    parent.addService(name);
                }
            }));
        }));
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        drawContext.fill(x, y, x + width, y + 1, 0xFFFFFFFF);
        drawContext.fill(x, y + height - 2, x + width, y + height - 1, 0xFFFFFFFF);
        drawContext.fill(x, y + height - 1, x + width, y + height, 0xFF7F7F7F);
        drawContext.fill(x, y + 1, x + 1, y + height - 1, 0xFFFFFFFF);
        drawContext.fill(x + width - 1, y + 1, x + width, y + height - 1, 0xFFFFFFFF);
        int w = this.width - 12;

        drawContext.fill(x + w * 2 / 12, y + 1, x + w * 2 / 12 + 1, y + height - 1, 0xFFFFFFFF);
        drawContext.fill(x + w * 10 / 12, y + 1, x + w * 10 / 12 + 1, y + height - 1, 0xFFFFFFFF);
        drawContext.fill(x + w * 11 / 12, y + 1, x + w * 11 / 12 + 1, y + height - 1, 0xFFFFFFFF);
        drawContext.fill(x + width - 14, y + 1, x + width - 13, y + height - 1, 0xFFFFFFFF);
    }

}
