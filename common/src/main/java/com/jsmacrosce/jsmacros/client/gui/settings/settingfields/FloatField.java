package com.jsmacrosce.jsmacros.client.gui.settings.settingfields;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import com.jsmacrosce.jsmacros.client.gui.settings.SettingsOverlay;
import com.jsmacrosce.jsmacros.client.gui.settings.settingcontainer.AbstractSettingContainer;
import com.jsmacrosce.wagyourgui.BaseScreen;
import com.jsmacrosce.wagyourgui.elements.TextInput;

import java.lang.reflect.InvocationTargetException;

public class FloatField extends AbstractSettingField<Float> {

    public FloatField(int x, int y, int width, Font textRenderer, AbstractSettingContainer parent, SettingsOverlay.SettingField<Float> field) {
        super(x, y, width, textRenderer.lineHeight + 2, textRenderer, parent, field);
    }

    @Override
    public void init() {
        super.init();
        try {
            TextInput floatIn = addRenderableWidget(new TextInput(x + width / 2, y, width / 2, height, textRenderer, 0xFF101010, 0, 0xFF4040FF, 0xFFFFFFFF, setting.get().toString(), null, (value) -> {
                try {
                    if (value.equals("")) {
                        value = "0";
                    }
                    setting.set(Float.parseFloat(value));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }));
            floatIn.mask = "\\d*(\\.\\d*)";
        } catch (IllegalAccessException | InvocationTargetException e) {
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
