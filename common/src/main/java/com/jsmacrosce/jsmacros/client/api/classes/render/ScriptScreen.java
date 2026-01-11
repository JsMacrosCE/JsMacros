package com.jsmacrosce.jsmacros.client.api.classes.render;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.access.IScreenInternal;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.wagyourgui.BaseScreen;

/**
 * just go look at {@link IScreen IScreen}
 * since all the methods are done through a mixin...
 *
 * @author Wagyourtail
 * @see IScreen
 * @since 1.0.5
 */
public class ScriptScreen extends BaseScreen {
    public boolean drawTitle;
    /**
     * @since 1.8.4
     * WARNING: this can break the game if you set it false and don't have a way to close the screen.
     */
    public boolean shouldCloseOnEsc = true;
    /**
     * @since 1.8.4
     */
    public boolean shouldPause = true;
    private final int bgStyle;
    @Nullable
    private MethodWrapper<Pos3D, GuiGraphics, Object, ?> onRender;

    public ScriptScreen(String title, boolean dirt) {
        super(Component.literal(title), null);
        this.bgStyle = dirt ? 0 : 1;
        this.drawTitle = true;
    }

    @Override
    protected void init() {
        BaseScreen prev = JsMacrosClient.prevScreen;
        super.init();
        JsMacrosClient.prevScreen = prev;
    }

    /**
     * @param parent parent screen to go to when this one exits.
     * @since 1.4.0
     */
    public void setParent(IScreen parent) {
        this.parent = (net.minecraft.client.gui.screens.Screen) parent;
    }

    /**
     * add custom stuff to the render function on the main thread.
     *
     * @param onRender pos3d elements are mousex, mousey, tickDelta
     * @since 1.4.0
     */
    public void setOnRender(@Nullable MethodWrapper<Pos3D, GuiGraphics, Object, ?> onRender) {
        this.onRender = onRender;
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        if (drawContext == null) {
            return;
        }
        if (bgStyle == 0) {
            this.renderMenuBackground(drawContext);
        }

        if (drawTitle) {
            drawContext.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
        }

        super.render(drawContext, mouseX, mouseY, delta);

        for (GuiEventListener button : ImmutableList.copyOf(this.children())) {
            if (!(button instanceof Renderable)) {
                continue;
            }
            ((Renderable) button).render(drawContext, mouseX, mouseY, delta);
        }

        ((IScreenInternal) this).jsmacros_render(drawContext, mouseX, mouseY, delta);
        try {
            if (onRender != null) {
                onRender.accept(new Pos3D(mouseX, mouseY, delta), drawContext);
            }
        } catch (Throwable e) {
            JsMacrosClient.clientCore.profile.logError(e);
            onRender = null;
        }
    }

    @Override
    public void onClose() {
        openParent();
    }

    @Override
    public boolean isPauseScreen() {
        return shouldPause;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return shouldCloseOnEsc && super.shouldCloseOnEsc();
    }

}
