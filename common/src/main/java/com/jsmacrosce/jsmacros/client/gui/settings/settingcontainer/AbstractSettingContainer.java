package com.jsmacrosce.jsmacros.client.gui.settings.settingcontainer;

import net.minecraft.client.gui.Font;
import com.jsmacrosce.jsmacros.client.gui.settings.SettingsOverlay;
import com.jsmacrosce.wagyourgui.containers.MultiElementContainer;
import com.jsmacrosce.wagyourgui.elements.Scrollbar;

public abstract class AbstractSettingContainer extends MultiElementContainer<SettingsOverlay> {
    public final String[] group;
    public Scrollbar scroll;

    public AbstractSettingContainer(int x, int y, int width, int height, Font textRenderer, SettingsOverlay parent, String[] group) {
        super(x, y, width, height, textRenderer, parent);
        this.group = group;
        init();
    }

    public abstract void addSetting(SettingsOverlay.SettingField<?> setting);

}
