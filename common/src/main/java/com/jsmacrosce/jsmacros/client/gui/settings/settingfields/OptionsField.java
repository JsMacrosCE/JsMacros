package com.jsmacrosce.jsmacros.client.gui.settings.settingfields;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import com.jsmacrosce.jsmacros.client.gui.settings.SettingsOverlay;
import com.jsmacrosce.jsmacros.client.gui.settings.settingcontainer.AbstractSettingContainer;
import com.jsmacrosce.wagyourgui.BaseScreen;
import com.jsmacrosce.wagyourgui.elements.Button;
import com.jsmacrosce.wagyourgui.overlays.SelectorDropdownOverlay;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

public class OptionsField extends AbstractSettingField<Object> {

    public OptionsField(int x, int y, int width, Font textRenderer, AbstractSettingContainer parent, SettingsOverlay.SettingField<Object> field) {
        super(x, y, width, textRenderer.lineHeight + 2, textRenderer, parent, field);
    }

    @Override
    public void init() {
        super.init();
        try {
            List<Object> values = setting.getOptions();
            List<Component> textvalues = values.stream().map(e -> Component.literal(e.toString())).collect(Collectors.toList());
            this.addRenderableWidget(new Button(x + width / 2, y, width / 2, height, textRenderer, 0, 0xFF000000, 0x7FFFFFFF, 0xFFFFFFFF, Component.literal(setting.get().toString()), (btn) -> {
                getFirstOverlayParent().openOverlay(new SelectorDropdownOverlay(x + width / 2, y, width / 2, values.size() * (textRenderer.lineHeight + 1) + 4, textvalues, textRenderer, getFirstOverlayParent(), (choice) -> {
                    btn.setMessage(textvalues.get(choice));
                    try {
                        setting.set(values.get(choice));
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }));
            }));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setPos(int x, int y, int width, int height) {
        super.setPos(x, y, width, height);
        for (AbstractWidget btn : buttons) {
            btn.setY(y);
        }
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        drawContext.drawString(textRenderer, BaseScreen.trimmed(textRenderer, settingName, width / 2), x, y + 1, 0xFFFFFFFF, false);
    }

}
