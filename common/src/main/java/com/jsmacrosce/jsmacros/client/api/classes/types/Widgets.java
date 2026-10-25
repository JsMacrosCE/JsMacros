package com.jsmacrosce.jsmacros.client.api.classes.types;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.screen.*;
import com.jsmacrosce.jsmacros.core.library.ScriptNamespace;

/**
 * Lets scripts identify buttons, sliders, and other widget helpers through the {@code Widgets} global.
 * <pre>
 * const screen = Hud.getOpenScreen();
 * if (screen !== null) {
 *   for (const widget of screen.getButtonWidgets()) {
 *     if (widget instanceof Widgets.Slider) {
 *       Chat.log(widget.getValue());
 *     }
 *   }
 * }
 * </pre>
 */
@ScriptNamespace("Widgets")
@DocletCategory("Script Type Namespaces")
@SuppressWarnings("rawtypes")
public final class Widgets {
    private Widgets() {
    }

    public static final Class<ClickableWidgetHelper> Clickable = ClickableWidgetHelper.class;
    public static final Class<ButtonWidgetHelper> Button = ButtonWidgetHelper.class;
    public static final Class<CheckBoxWidgetHelper> CheckBox = CheckBoxWidgetHelper.class;
    public static final Class<CyclingButtonWidgetHelper> CyclingButton = CyclingButtonWidgetHelper.class;
    public static final Class<LockButtonWidgetHelper> LockButton = LockButtonWidgetHelper.class;
    public static final Class<SliderWidgetHelper> Slider = SliderWidgetHelper.class;
    public static final Class<TextFieldWidgetHelper> TextField = TextFieldWidgetHelper.class;
}
