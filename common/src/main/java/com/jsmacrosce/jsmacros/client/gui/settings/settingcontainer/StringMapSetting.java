package com.jsmacrosce.jsmacros.client.gui.settings.settingcontainer;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import com.jsmacrosce.jsmacros.client.gui.settings.SettingsOverlay;
import com.jsmacrosce.wagyourgui.elements.Button;
import com.jsmacrosce.wagyourgui.overlays.TextPrompt;

import java.lang.reflect.InvocationTargetException;

public class StringMapSetting extends AbstractMapSettingContainer<String, StringMapSetting.StringEntry> {
    public StringMapSetting(int x, int y, int width, int height, Font textRenderer, SettingsOverlay parent, String[] group) {
        super(x, y, width, height, textRenderer, parent, group);
        defaultValue = () -> "";
    }

    @Override
    public void addField(String key, String value) {
        if (map.containsKey(key)) {
            return;
        }
        StringEntry entry = new StringEntry(x, y + 12 + totalHeight - topScroll, width - 12, textRenderer, this, key, value);
        map.put(key, entry);
        totalHeight += entry.height;
        scroll.setScrollPages(totalHeight / (double) height);
        if (scroll.active) {
            scroll.scrollToPercent(0);
        } else {
            onScrollbar(0);
        }
    }

    public static class StringEntry extends AbstractMapSettingContainer.MapSettingEntry<String> {

        public StringEntry(int x, int y, int width, Font textRenderer, StringMapSetting parent, String key, String value) {
            super(x, y, width, textRenderer, (AbstractMapSettingContainer) parent, key, value);
        }

        @Override
        public void init() {
            super.init();
            int w = width - height;
            addRenderableWidget(new Button(x + w / 2, y, w / 2, height, textRenderer, 0, 0xFF000000, 0x7FFFFFFF, 0xFFFFFFFF, Component.literal(value), (btn) -> {
                int x = parent.x;
                int y = parent.y;
                int width = parent.width;
                int height = parent.height;
                openOverlay(new TextPrompt(x + width / 4, y + height / 4, width / 2, height / 2, textRenderer, Component.translatable("jsmacrosce.setvalue"), value, getFirstOverlayParent(), (str) -> {
                    try {
                        parent.changeValue(key, str);
                        btn.setMessage(Component.literal(str));
                    } catch (InvocationTargetException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }));
            }));
        }

    }

}
