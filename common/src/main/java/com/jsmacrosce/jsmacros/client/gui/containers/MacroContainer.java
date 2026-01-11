package com.jsmacrosce.jsmacros.client.gui.containers;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import com.jsmacrosce.jsmacros.client.JsMacros;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.gui.screens.MacroScreen;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.event.BaseEventRegistry;
import com.jsmacrosce.wagyourgui.containers.MultiElementContainer;
import com.jsmacrosce.wagyourgui.elements.Button;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class MacroContainer extends MultiElementContainer<MacroScreen> {
    private static final ResourceLocation key_down_tex = ResourceLocation.fromNamespaceAndPath(JsMacros.MOD_ID, "resources/key_down.png");
    private static final ResourceLocation key_up_tex = ResourceLocation.fromNamespaceAndPath(JsMacros.MOD_ID, "resources/key_up.png");
    private static final ResourceLocation key_both_tex = ResourceLocation.fromNamespaceAndPath(JsMacros.MOD_ID, "resources/key_both.png");
    @SuppressWarnings("unused")
    private static final ResourceLocation event_tex = ResourceLocation.fromNamespaceAndPath(JsMacros.MOD_ID, "resources/event.png");
    private static final ResourceLocation script_fork_tex = ResourceLocation.fromNamespaceAndPath(JsMacros.MOD_ID, "resources/script_fork.png");
    private static final ResourceLocation script_join_tex = ResourceLocation.fromNamespaceAndPath(JsMacros.MOD_ID, "resources/script_join.png");
    private final Minecraft mc;
    private final ScriptTrigger macro;
    private Button enableBtn;
    private Button keyBtn;
    private Button fileBtn;
    private Button delBtn;
    private Button editBtn;
    private Button keyStateBtn;
    private Button joinedBtn;
    private boolean selectkey = false;

    public MacroContainer(int x, int y, int width, int height, Font textRenderer, ScriptTrigger macro, MacroScreen parent) {
        super(x, y, width, height, textRenderer, parent);
        this.macro = macro;
        this.mc = Minecraft.getInstance();
        init();
    }

    public ScriptTrigger getRawMacro() {
        return macro;
    }

    private static Component getTranslatedEventName(String eventName) {
        String lowerCaseName = eventName.toLowerCase(Locale.ROOT);
        if (Language.getInstance().has("jsmacrosce.event." + lowerCaseName)) {
            return Component.translatable("jsmacrosce.event." + lowerCaseName);
        }

        return Component.literal(eventName);
    }

    @Override
    public void init() {
        super.init();
        int w = width - 12;
        enableBtn = addRenderableWidget(new Button(x + 1, y + 1, w / 12 - 1, height - 2, textRenderer, macro.enabled ? 0x7000FF00 : 0x70FF0000, 0xFF000000, 0x7F7F7F7F, 0xFFFFFFFF, Component.translatable(macro.enabled ? "jsmacrosce.enabled" : "jsmacrosce.disabled"), (btn) -> {
            macro.enabled = !macro.enabled;
            btn.setColor(macro.enabled ? 0x7000FF00 : 0x70FF0000);
            btn.setMessage(Component.translatable(macro.enabled ? "jsmacrosce.enabled" : "jsmacrosce.disabled"));
        }));

        boolean isEventMacro = macro.triggerType == ScriptTrigger.TriggerType.EVENT;
        keyBtn = addRenderableWidget(new Button(
                x + w / 12 + 1,
                y + 1,
                macro.triggerType == ScriptTrigger.TriggerType.EVENT ? (w / 4) - (w / 12) - 1 - height : (w / 4) - (w / 12) - 1 - height * 2,
                height - 2,
                textRenderer,
                0,
                0xFF000000,
                0x7F7F7F7F,
                0xFFFFFFFF,
                isEventMacro ? getTranslatedEventName(macro.event) : buildKeyName(macro.event),
                (btn) -> {
            if (isEventMacro) {
                parent.setEvent(this);
            } else {
                selectkey = true;
                btn.setMessage(Component.translatable("jsmacrosce.presskey"));
            }
        }));

        if (!isEventMacro) {
            joinedBtn = addRenderableWidget(new Button(x + w / 4 - height * 2, y + 1, height, height - 2, textRenderer, 0, 0xFF000000, 0x7F7F7F7F, 0xFFFFFFFF, Component.literal(""), (btn) -> {
                macro.joined = !macro.joined;
            }));
            keyStateBtn = addRenderableWidget(new Button(x + w / 4 - height, y + 1, height, height - 2, textRenderer, 0, 0xFF000000, 0x7F7F7F7F, 0xFFFFFFFF, Component.literal(""), (btn) -> {
                switch (macro.triggerType) {
                    default:
                    case KEY_RISING:
                        macro.triggerType = ScriptTrigger.TriggerType.KEY_FALLING;
                        break;
                    case KEY_FALLING:
                        macro.triggerType = ScriptTrigger.TriggerType.KEY_BOTH;
                        break;
                    case KEY_BOTH:
                        macro.triggerType = ScriptTrigger.TriggerType.KEY_RISING;
                        break;
                }
            }));
        } else {
            joinedBtn = addRenderableWidget(new Button(x + w / 4 - height, y + 1, height, height - 2, textRenderer, 0, 0xFF000000, 0x7F7F7F7F, 0xFFFFFFFF, Component.literal(""), (btn) -> {
                macro.joined = !macro.joined;
            }));
        }


        final String fileName;
        if (macro.scriptFile.isAbsolute()) {
            fileName = JsMacrosClient.clientCore.config.macroFolder.toPath().relativize(macro.scriptFile).toString();
        } else {
            fileName = macro.scriptFile.toString();
        }
        fileBtn = addRenderableWidget(new Button(x + (w / 4) + 1, y + 1, w * 3 / 4 - 3 - 30, height - 2, textRenderer, 0, 0xFF000000, 0x7F7F7F7F, 0xFFFFFFFF, Component.literal("./" + fileName.replaceAll("\\\\", "/")), (btn) -> {
            parent.setFile(this);
        }));

        editBtn = addRenderableWidget(new Button(x + w - 32, y + 1, 30, height - 2, textRenderer, 0, 0xFF000000, 0x7F7F7F7F, 0xFFFFFFFF, Component.translatable("selectServer.edit"), (btn) -> {
            if (macro.scriptFile != null) {
                final File file;
                if (macro.scriptFile.isAbsolute()) {
                    file = macro.scriptFile.toFile();
                } else {
                    file = JsMacrosClient.clientCore.config.macroFolder.toPath().resolve(macro.scriptFile).toFile();
                }
                parent.editFile(file);
            }
        }));

        delBtn = addRenderableWidget(new Button(x + w - 1, y + 1, 12, height - 2, textRenderer, 0, 0xFF000000, 0x7F7F7F7F, 0xFFFFFFFF, Component.literal("X"), (btn) -> {
            parent.confirmRemoveMacro(this);
        }));
    }

    public void setEventType(String type) {
        BaseEventRegistry reg = JsMacrosClient.clientCore.eventRegistry;
        reg.removeScriptTrigger(macro);
        macro.event = type;
        reg.addScriptTrigger(macro);
        keyBtn.setMessage(Component.translatable("jsmacrosce.event." + macro.event.toLowerCase(Locale.ROOT)));
    }

    public void setFile(File f) {
        macro.scriptFile = JsMacrosClient.clientCore.config.macroFolder.toPath().relativize(f.toPath());
        final String fileName = macro.scriptFile.toString();
        fileBtn.setMessage(Component.literal("./" + fileName.replaceAll("\\\\", "/")));
    }

    @Override
    public void setPos(int x, int y, int width, int height) {
        super.setPos(x, y, width, height);
        int w = width - 12;
        enableBtn.setPos(x + 1, y + 1, w / 12 - 1, height - 2);
        keyBtn.setPos(x + w / 12 + 1, y + 1, macro.triggerType == ScriptTrigger.TriggerType.EVENT ? (w / 4) - (w / 12) - 1 - height : (w / 4) - (w / 12) - 1 - height * 2, height - 2);
        if (macro.triggerType != ScriptTrigger.TriggerType.EVENT) {
            joinedBtn.setPos(x + w / 4 - height * 2, y + 1, height, height - 2);
            keyStateBtn.setPos(x + w / 4 - height, y + 1, height, height - 2);
        } else {
            joinedBtn.setPos(x + w / 4 - height, y + 1, height, height - 2);
        }
        fileBtn.setPos(x + (w / 4) + 1, y + 1, w * 3 / 4 - 3 - 30, height - 2);
        editBtn.setPos(x + w - 32, y + 1, 30, height - 2);
        delBtn.setPos(x + w - 1, y + 1, 12, height - 2);

    }

    public boolean onKey(String translationKey) {
        if (selectkey) {
            setKey(translationKey);
            return false;
        }
        return true;
    }

    public static Component buildKeyName(String translationKeys) {
        MutableComponent text = Component.literal("");
        boolean notfirst = false;
        for (String s : translationKeys.split("\\+")) {
            if (notfirst) {
                text.append("+");
            }
            text.append(JsMacrosClient.getKeyText(s));
            notfirst = true;
        }
        return text;
    }

    public void setKey(String translationKeys) {
        JsMacrosClient.clientCore.eventRegistry.removeScriptTrigger(macro);
        macro.event = translationKeys;
        JsMacrosClient.clientCore.eventRegistry.addScriptTrigger(macro);
        keyBtn.setMessage(buildKeyName(translationKeys));
        selectkey = false;
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        BaseEventRegistry reg = JsMacrosClient.clientCore.eventRegistry;
        if (macro.triggerType == ScriptTrigger.TriggerType.EVENT && reg.events.contains(macro.event)) {
            joinedBtn.active = reg.joinableEvents.contains(macro.event);
            if (!joinedBtn.active) {
                joinedBtn.setColor(0xA0000000);
                macro.joined = false;
            } else {
                joinedBtn.setColor(0);
            }
        } else {
            joinedBtn.setColor(0);
            joinedBtn.active = true;
        }
        if (visible) {
            int w = this.width - 12;
            // separate
            drawContext.fill(x + (w / 12), y + 1, x + (w / 12) + 1, y + height - 1, 0xFFFFFFFF);
            drawContext.fill(x + (w / 4), y + 1, x + (w / 4) + 1, y + height - 1, 0xFFFFFFFF);
            drawContext.fill(x + width - 14, y + 1, x + width - 13, y + height - 1, 0xFFFFFFFF);
            //RenderSystem.setShader(VertexFormats.POSITION_TEXTURE);

            //? if >1.21.5 {
            RenderPipeline renderType = RenderPipelines.GUI_TEXTURED;
            //?} else {
            /*Function<ResourceLocation, RenderType> renderType = RenderType::guiTextured;
            *///?}

            // icon for keystate
            ResourceLocation tex;
            if (macro.triggerType != ScriptTrigger.TriggerType.EVENT) {
                if (macro.joined) {
                    tex = script_join_tex;
                } else {
                    tex = script_fork_tex;
                }
                GlStateManager._enableBlend();
                drawContext.blit(
                        renderType,
                        tex,
                        x + w / 4 - 2 * height + 2,
                        y + 2,
                        0,
                        0,
                        height - 4,
                        height - 4,
                        32,
                        32,
                        32,
                        32
                );
                GlStateManager._disableBlend();
                switch (macro.triggerType) {
                    default:
                    case KEY_FALLING:
                        tex = key_up_tex;
                        break;
                    case KEY_RISING:
                        tex = key_down_tex;
                        break;
                    case KEY_BOTH:
                        tex = key_both_tex;
                        break;
                }
                GlStateManager._enableBlend();
                drawContext.blit(
                        renderType,
                        tex,
                        x + w / 4 - height + 2,
                        y + 2,
                        0,
                        0,
                        height - 4,
                        height - 4,
                        32,
                        32,
                        32,
                        32
                );
                GlStateManager._disableBlend();
            } else {
                if (macro.joined) {
                    tex = script_join_tex;
                } else {
                    tex = script_fork_tex;
                }
                GlStateManager._enableBlend();
                drawContext.blit(
                        renderType,
                        tex,
                        x + w / 4 - height + 2,
                        y + 2,
                        0,
                        0,
                        height - 4,
                        height - 4,
                        32,
                        32,
                        32,
                        32
                );
                GlStateManager._disableBlend();
            }

            // border
            drawContext.fill(x, y, x + width, y + 1, 0xFFFFFFFF);
            drawContext.fill(x, y + height - 1, x + width, y + height, 0xFFFFFFFF);
            drawContext.fill(x, y + 1, x + 1, y + height - 1, 0xFFFFFFFF);
            drawContext.fill(x + width - 1, y + 1, x + width, y + height - 1, 0xFFFFFFFF);

            // overlay
            if (keyBtn.hovering && keyBtn.cantRenderAllText()) {
                drawContext.fill(mouseX - 2, mouseY - textRenderer.lineHeight - 3, mouseX + textRenderer.width(keyBtn.getMessage()) + 2, mouseY, 0xFF000000);
                drawContext.drawString(textRenderer, keyBtn.getMessage(), mouseX, mouseY - textRenderer.lineHeight - 1, 0xFFFFFFFF);
            }
            if (fileBtn.hovering && fileBtn.cantRenderAllText()) {
                List<FormattedCharSequence> lines = textRenderer.split(fileBtn.getMessage(), this.x + this.width - mouseX);
                int top = mouseY - (textRenderer.lineHeight * lines.size()) - 2;
                int width = lines.stream().map(e -> textRenderer.width(e)).reduce(0, Math::max);
                drawContext.fill(mouseX - 2, top - 1, mouseX + width + 2, mouseY, 0xFF000000);
                for (int i = 0; i < lines.size(); ++i) {
                    int wi = textRenderer.width(lines.get(i)) / 2;
                    drawContext.drawString(textRenderer, lines.get(i), mouseX + width / 2 - wi, top + textRenderer.lineHeight * i, 0xFFFFFFFF, false);
                }
            }
        }
    }

}
