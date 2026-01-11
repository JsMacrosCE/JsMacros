package com.jsmacrosce.jsmacros.client.gui.containers;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.config.ClientConfigV2;
import com.jsmacrosce.jsmacros.client.config.Sorting;
import com.jsmacrosce.jsmacros.client.gui.screens.MacroScreen;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.wagyourgui.containers.MultiElementContainer;
import com.jsmacrosce.wagyourgui.elements.Button;

import java.nio.file.Path;

public class MacroListTopbar extends MultiElementContainer<MacroScreen> {
    public ScriptTrigger.TriggerType deftype;
    private Button type;

    public MacroListTopbar(MacroScreen parent, int x, int y, int width, int height, Font textRenderer, ScriptTrigger.TriggerType deftype) {
        super(x, y, width, height, textRenderer, parent);
        this.deftype = deftype;
        init();
    }

    @Override
    public void init() {
        super.init();

        int w = width - 12;

        addRenderableWidget(new Button(x + 1, y + 1, w / 12 - 1, height - 3, textRenderer, JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).sortMethod == Sorting.MacroSortMethod.Enabled ? 0x3FFFFFFF : 0, 0xFF000000, 0x7F7F7F7F, 0xFFFFFFFF, Component.translatable("jsmacrosce.enabledstatus"), (btn) -> {
            JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).sortMethod = Sorting.MacroSortMethod.Enabled;
            parent.reload();
        }));

        type = addRenderableWidget(new Button(x + w / 12 + 1, y + 1, (w / 4) - (w / 12) - 1, height - 3, textRenderer, JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).sortMethod == Sorting.MacroSortMethod.TriggerName ? 0x3FFFFFFF : 0, 0xFF000000, 0x7F7F7F7F, 0xFFFFFFFF, Component.translatable(deftype == ScriptTrigger.TriggerType.EVENT ? "jsmacrosce.events" : "jsmacrosce.keys"), (btn) -> {
            JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).sortMethod = Sorting.MacroSortMethod.TriggerName;
            parent.reload();
        }));

        addRenderableWidget(new Button(x + (w / 4) + 1, y + 1, w * 3 / 4 - 3 - 30, height - 3, textRenderer, JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).sortMethod == Sorting.MacroSortMethod.FileName ? 0x3FFFFFFF : 0, 0xFF000000, 0x7F7F7F7F, 0xFFFFFFFF, Component.translatable("jsmacrosce.file"), (btn) -> {
            JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).sortMethod = Sorting.MacroSortMethod.FileName;
            parent.reload();
        }));

        addRenderableWidget(new Button(x + w - 32, y + 1, 30, height - 3, textRenderer, 0, 0xFF000000, 0x7F7F7F7F, 0xFFFFFFFF, Component.translatable("jsmacrosce.run"), (btn) -> {
            parent.runFile();
        }));

        addRenderableWidget(new Button(x + w - 1, y + 1, 11, height - 3, textRenderer, 0, 0xFF000000, 0x7F7F7F7F, 0xFFFFFFFF, Component.literal("+"), (btn) -> {
            ScriptTrigger macro = new ScriptTrigger(deftype, "", Path.of(".").normalize(), false, false);
            JsMacrosClient.clientCore.eventRegistry.addScriptTrigger(macro);
            parent.addMacro(macro);
        }));
    }

    public void updateType(ScriptTrigger.TriggerType type) {
        this.deftype = type;
        this.type.setMessage(Component.translatable(deftype == ScriptTrigger.TriggerType.EVENT ? "jsmacrosce.events" : "jsmacrosce.keys"));
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        drawContext.fill(x, y, x + width, y + 1, 0xFFFFFFFF);
        drawContext.fill(x, y + height - 2, x + width, y + height - 1, 0xFFFFFFFF);
        drawContext.fill(x, y + height - 1, x + width, y + height, 0xFF7F7F7F);
        drawContext.fill(x, y + 1, x + 1, y + height - 1, 0xFFFFFFFF);
        drawContext.fill(x + width - 1, y + 1, x + width, y + height - 1, 0xFFFFFFFF);
        int w = this.width - 12;

        drawContext.fill(x + (w / 12), y + 1, x + (w / 12) + 1, y + height - 1, 0xFFFFFFFF);
        drawContext.fill(x + (w / 4), y + 1, x + (w / 4) + 1, y + height - 1, 0xFFFFFFFF);
        drawContext.fill(x + width - 14, y + 1, x + width - 13, y + height - 1, 0xFFFFFFFF);
    }

}
