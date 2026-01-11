package com.jsmacrosce.jsmacros.client.gui.settings.settingfields;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import com.jsmacrosce.jsmacros.client.gui.settings.SettingsOverlay;
import com.jsmacrosce.jsmacros.client.gui.settings.settingcontainer.AbstractSettingContainer;
import com.jsmacrosce.wagyourgui.elements.AnnotatedCheckBox;

import java.lang.reflect.InvocationTargetException;

public class BooleanField extends AbstractSettingField<Boolean> {

    public BooleanField(int x, int y, int width, Font textRenderer, AbstractSettingContainer parent, SettingsOverlay.SettingField<Boolean> field) {
        super(x, y, width, textRenderer.lineHeight + 2, textRenderer, parent, field);
    }

    @Override
    public void init() {
        super.init();
        try {
            this.addRenderableWidget(new AnnotatedCheckBox(x, y, width, height, textRenderer, 0xFFFFFFFF, 0xFF242424, 0x7FFFFFFF, 0xFFFFFFFF, settingName, setting.get(), button -> {
                try {
                    setting.set(((AnnotatedCheckBox) button).value);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }));
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

    }

}
