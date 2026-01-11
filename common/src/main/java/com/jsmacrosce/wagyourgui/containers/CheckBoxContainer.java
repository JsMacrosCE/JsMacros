package com.jsmacrosce.wagyourgui.containers;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import com.jsmacrosce.wagyourgui.elements.Button;

import java.util.function.Consumer;

public class CheckBoxContainer extends MultiElementContainer<IContainerParent> {
    private boolean state;
    private Button checkBox;
    private final Consumer<Boolean> setState;
    public Component message;

    public CheckBoxContainer(int x, int y, int width, int height, Font textRenderer, boolean defaultState, Component message, IContainerParent parent, Consumer<Boolean> setState) {
        super(x, y, width, height, textRenderer, parent);
        this.state = defaultState;
        this.message = message;
        this.setState = setState;
        this.init();
    }

    @Override
    public void init() {
        super.init();

        checkBox = this.addRenderableWidget(new Button(x, y, height, height, textRenderer, 0, 0xFF000000, 0x7FFFFFFF, 0xFFFFFFFF, Component.literal(state ? "\u2713" : ""), btn -> {
            state = !state;
            if (setState != null) {
                setState.accept(state);
            }
            btn.setMessage(Component.literal(state ? "\u2713" : ""));
        }));
    }

    @Override
    public void setPos(int x, int y, int width, int height) {
        checkBox.setPos(x + 1, y + 1, height - 2, height - 2);
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        if (this.visible) {
            drawContext.drawWordWrap(textRenderer, message, x + height, y + 2, width - height - 2, 0xFFFFFFFF, false);
        }
    }

}
