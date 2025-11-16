package xyz.wagyourtail.jsmacros.client.api.classes.render.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import org.joml.Matrix3x2fStack;
import xyz.wagyourtail.doclet.DocletIgnore;

/**
 * @author Wagyourtail
 */
public interface RenderElement extends Renderable {

    Minecraft mc = Minecraft.getInstance();

    int getZIndex();

    @DocletIgnore
    default void render3D(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        render(drawContext, mouseX, mouseY, delta);
    }

    @DocletIgnore
    default void setupMatrix(Matrix3x2fStack matrices, double x, double y, float scale, float rotation, double width, double height, boolean rotateAroundCenter) {
        matrices.translate((float) x, (float) y);
        matrices.scale(scale, scale);
        if (rotateAroundCenter) {
            matrices.translate((float) (width / 2), (float) (height / 2));
        }
        matrices.rotate((float) Math.toRadians(rotation));
        if (rotateAroundCenter) {
            matrices.translate((float) (-width / 2), (float) (-height / 2));
        }
        matrices.translate((float) -x, (float) -y);
    }

}
