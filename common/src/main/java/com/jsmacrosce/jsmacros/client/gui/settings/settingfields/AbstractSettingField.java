package com.jsmacrosce.jsmacros.client.gui.settings.settingfields;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import com.jsmacrosce.jsmacros.client.gui.settings.SettingsOverlay;
import com.jsmacrosce.jsmacros.client.gui.settings.settingcontainer.AbstractSettingContainer;
import com.jsmacrosce.wagyourgui.containers.MultiElementContainer;

public abstract class AbstractSettingField<T> extends MultiElementContainer<AbstractSettingContainer> {
    protected final SettingsOverlay.SettingField<T> setting;
    protected final Component settingName;

    public AbstractSettingField(int x, int y, int width, int height, Font textRenderer, AbstractSettingContainer parent, SettingsOverlay.SettingField<T> field) {
        super(x, y, width, height, textRenderer, parent);
        setting = field;
        settingName = Component.translatable(field.option.translationKey());
        init();
    }

}
