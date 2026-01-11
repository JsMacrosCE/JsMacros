package com.jsmacrosce.jsmacros.client.gui.settings.settingfields;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.gui.overlays.FileChooser;
import com.jsmacrosce.jsmacros.client.gui.settings.SettingsOverlay;
import com.jsmacrosce.jsmacros.client.gui.settings.settingcontainer.AbstractSettingContainer;
import com.jsmacrosce.wagyourgui.BaseScreen;
import com.jsmacrosce.wagyourgui.elements.Button;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

public class FileField extends AbstractSettingField<String> {

    public FileField(int x, int y, int width, Font textRenderer, AbstractSettingContainer parent, SettingsOverlay.SettingField<String> field) {
        super(x, y, width, textRenderer.lineHeight + 2, textRenderer, parent, field);
    }

    public static File getTopLevel(SettingsOverlay.SettingField<?> setting) {
        for (String option : setting.option.type().options()) {
            if (option.startsWith("topLevel=")) {
                switch (option.replace("topLevel=", "")) {
                    case "MC":
                        return Minecraft.getInstance().gameDirectory;
                    case "CONFIG":
                        return JsMacrosClient.clientCore.config.configFolder;
                    case "MACRO":
                    default:
                        return JsMacrosClient.clientCore.config.macroFolder;
                }
            }
        }
        //default
        return JsMacrosClient.clientCore.config.macroFolder;
    }

    public static String relativize(SettingsOverlay.SettingField<?> setting, File file) {
        File top = getTopLevel(setting).getAbsoluteFile();
        return top.toPath().relativize(file.getAbsoluteFile().toPath()).toString();
    }

    @Override
    public void init() {
        super.init();
        try {
            this.addRenderableWidget(new Button(x + width / 2, y, width / 2, height, textRenderer, 0, 0xFF000000, 0x7FFFFFFF, 0xFFFFFFFF, Component.literal(setting.get()), (btn) -> {
                try {
                    File current = new File(getTopLevel(setting), setting.get());
                    FileChooser fc = new FileChooser(parent.x, parent.y, parent.width, parent.height, textRenderer, current.getParentFile(), current, getFirstOverlayParent(), (file) -> {
                        try {
                            setting.set("./" + relativize(setting, file).replaceAll("\\\\", "/"));
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }, file -> {
                    });
                    fc.root = getTopLevel(setting);
                    parent.openOverlay(fc);
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw new RuntimeException(e);
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
        drawContext.drawString(textRenderer, BaseScreen.trimmed(textRenderer, settingName, width / 2), x, y + 1, 0xFFFFFFFF, false);
    }

}
