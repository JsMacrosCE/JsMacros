package com.jsmacrosce.jsmacros.client.api.classes.types;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.render.components.*;
import com.jsmacrosce.jsmacros.client.api.classes.render.components3d.*;
import com.jsmacrosce.jsmacros.core.library.ScriptNamespace;

/**
 * Lets scripts identify 2D and 3D drawing elements through the {@code RenderElements} global.
 * <pre>
 * const draw = Hud.createDraw2D();
 * draw.addText("hello", 10, 10, 0xFFFFFF, true);
 * for (const element of draw.getElements()) {
 *   if (element instanceof RenderElements.Text) {
 *     element.setText("updated");
 *   }
 * }
 * </pre>
 */
@ScriptNamespace("RenderElements")
@DocletCategory("Script Type Namespaces")
@SuppressWarnings("rawtypes")
public final class RenderElements {
    private RenderElements() {
    }

    public static final Class<RenderElement> Element = RenderElement.class;
    public static final Class<Alignable> Alignable = Alignable.class;
    public static final Class<Text> Text = Text.class;
    public static final Class<Image> Image = Image.class;
    public static final Class<Item> Item = Item.class;
    public static final Class<Rect> Rect = Rect.class;
    public static final Class<Line> Line = Line.class;
    public static final Class<Draw2DElement> Draw2D = Draw2DElement.class;
    public static final Class<RenderElement3D> Element3D = RenderElement3D.class;
    public static final Class<Box> Box = Box.class;
    public static final Class<Line3D> Line3D = Line3D.class;
    public static final Class<TraceLine> TraceLine = TraceLine.class;
    public static final Class<EntityTraceLine> EntityTraceLine = EntityTraceLine.class;
    public static final Class<Surface> Surface = Surface.class;
}
