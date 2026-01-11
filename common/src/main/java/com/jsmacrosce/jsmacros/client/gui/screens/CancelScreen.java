package com.jsmacrosce.jsmacros.client.gui.screens;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.jsmacrosce.jsmacros.client.JsMacros;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.config.ClientConfigV2;
import com.jsmacrosce.jsmacros.client.gui.containers.RunningContextContainer;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;
import com.jsmacrosce.jsmacros.core.service.EventService;
import com.jsmacrosce.wagyourgui.BaseScreen;
import com.jsmacrosce.wagyourgui.elements.AnnotatedCheckBox;
import com.jsmacrosce.wagyourgui.elements.Button;
import com.jsmacrosce.wagyourgui.elements.Scrollbar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CancelScreen extends BaseScreen {
    private int topScroll;
    private Scrollbar s;
    private AnnotatedCheckBox services;
    private final List<RunningContextContainer> running = new ArrayList<>();

    public CancelScreen(Screen parent) {
        super(Component.literal("Cancel"), parent);
    }

    @Override
    public void init() {
        super.init();
        // force gc all currently closed contexts
        System.gc();
        topScroll = 10;
        running.clear();
        s = this.addRenderableWidget(new Scrollbar(width - 12, 5, 8, height - 10, 0xFFFFFFFF, 0xFF000000, 0x7FFFFFFF, 1, this::onScrollbar));

        this.addRenderableWidget(new Button(0, this.height - 12, this.width / 12, 12, font, 0, 0xFF000000, 0x7FFFFFFF, 0xFFFFFFFF, Component.translatable("jsmacrosce.back"), (btn) -> this.onClose()));
        services = this.addRenderableWidget(new AnnotatedCheckBox(this.width / 12 + 5, this.height - 12, 200, 12, font, 0xFFFFFFFF, 0xFF000000, 0xFFFFFFFF, 0xFFFFFFFF, Component.translatable("jsmacrosce.showservices"), JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).showRunningServices, btn -> JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).showRunningServices = ((AnnotatedCheckBox) btn).value));
    }

    public void addContainer(BaseScriptContext<?> t) {
        if (t == null) {
            return;
        }
        if (!services.value && t.getTriggeringEvent() instanceof EventService) {
            return;
        }
        if (!t.isContextClosed()) {
            running.add(new RunningContextContainer(10, topScroll + running.size() * 15, width - 26, 13, font, this, t));
            running.sort(new RTCSort());
            s.setScrollPages(running.size() * 15 / (double) (height - 20));
        } else {
            JsMacros.LOGGER.warn("Closed context {} was still in list", t.getMainThread().getName());
        }
    }

    public void removeContainer(RunningContextContainer t) {
        for (AbstractWidget b : t.getButtons()) {
            removeWidget(b);
        }
        running.remove(t);
        s.setScrollPages(running.size() * 15 / (double) (height - 20));
        updatePos();
    }

    private void onScrollbar(double page) {
        topScroll = 10 - (int) (page * (height - 20));
        updatePos();
    }

    public void updatePos() {
        for (int i = 0; i < running.size(); ++i) {
            if (topScroll + i * 15 < 10 || topScroll + i * 15 > height - 10) {
                running.get(i).setVisible(false);
            } else {
                running.get(i).setVisible(true);
                running.get(i).setPos(10, topScroll + i * 15, width - 26, 13);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horiz, double vert) {
        // TODO: This doesn't make sense? Why would we trigger a drag event when we scrolL??
        //  If this is *really* what we want, we'll need to make a fake event
        //  Later note: I believe this is how scrolling is done in the editor?
        //? if <=1.21.8 {
        s.mouseDragged(mouseX, mouseY, 0, 0, -vert * 2);
        //?}
        return super.mouseScrolled(mouseX, mouseY, horiz, vert);
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        if (drawContext == null) {
            return;
        }

        List<BaseScriptContext<?>> tl = new ArrayList<>(JsMacrosClient.clientCore.getContexts());

        for (RunningContextContainer r : ImmutableList.copyOf(this.running)) {
            tl.remove(r.t);
            if (!services.value && r.service) {
                removeContainer(r);
            }
            r.render(drawContext, mouseX, mouseY, delta);
        }

        for (BaseScriptContext<?> t : tl) {
            addContainer(t);
        }

        for (GuiEventListener b : ImmutableList.copyOf(this.children())) {
            if (!(b instanceof Renderable)) {
                continue;
            }
            ((Renderable) b).render(drawContext, mouseX, mouseY, delta);
        }
    }

    @Override
    public void removed() {
        assert minecraft != null;
    }

    @Override
    public void onClose() {
        this.openParent();
    }

    public static class RTCSort implements Comparator<RunningContextContainer> {
        @Override
        public int compare(RunningContextContainer arg0, RunningContextContainer arg1) {
            try {
                return arg0.t.getMainThread().getName().compareTo(arg1.t.getMainThread().getName());
            } catch (NullPointerException e) {
                return 0;
            }
        }

    }

}
