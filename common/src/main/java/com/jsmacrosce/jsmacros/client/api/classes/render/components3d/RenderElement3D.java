package com.jsmacrosce.jsmacros.client.api.classes.render.components3d;
import com.jsmacrosce.jsmacros.client.api.classes.render.RenderSink;

import com.jsmacrosce.doclet.DocletCategory;
import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.NotNull;
import com.jsmacrosce.doclet.DocletIgnore;

/**
 * A drawable element in a {@code Draw3D} overlay. Overlays sort elements by class name, then
 * delegate same-class ordering to {@link #compareToSame(RenderElement3D)}; the comparison is not
 * based on insertion order. Elements are added to an overlay and drawn when it is registered.
 * <p>
 * <pre>
 * const draw = Hud.createDraw3D();
 * draw.addBox(0, 64, 0, 1, 65, 1, 0xFFFF0000, 0x00000000, false);
 * draw.addBox(10, 64, 10, 11, 65, 11, 0xFF00FF00, 0x00000000, false);
 * draw.register();
 * // Same-class boxes are ordered by their implementation's position comparison.
 * const boxes = draw.getBoxes();
 * Chat.log(`comparison: ${boxes.get(0).compareToSame(boxes.get(1))}`);
 * </pre>
 * @param <T> the element type used for same-class comparisons
 *
 * @author Wagyourtail
 * @since 1.6.5
 */
@DocletCategory("Rendering/Graphics")
public interface RenderElement3D<T extends RenderElement3D<?>> extends Comparable<RenderElement3D<?>> {

    /**
     * Draws this element into the world.
     *
     * @param matrices the world transform, already translated so the camera is at the origin
     * @param sink the buffer source the geometry goes into
     * @param tickDelta partial-tick value used to interpolate moving elements
     */
    @DocletIgnore
    void render(PoseStack matrices, RenderSink sink, float tickDelta);

    /**
     * Optionally renders this element directly into the buffer source; the default implementation does nothing.
     */
    @DocletIgnore
    default void renderDirect(PoseStack matrices, RenderSink sink, float tickDelta, boolean alwaysOnTop) {
    }

    /**
     * Compares elements by canonical class name, then delegates equal-name comparisons to
     * {@link #compareToSame(RenderElement3D)}. The default implementation requires only that
     * each class define its same-class comparison.
     *
     * @param o another element to order this one against
     * @return a negative number, zero, or a positive number as this element is before,
     *         the same as, or after {@code o}
     * @author Wagyourtail
     * @since 1.6.5
     */
    @Override
    default int compareTo(@NotNull RenderElement3D o) {
        int i = this.getClass().getCanonicalName().compareTo(o.getClass().getCanonicalName());
        if (i == 0) {
            i = this.compareToSame((T) o);
        }
        return i;
    }

    /**
     * Compares this element with another of the same class. The fields and ordering used are
     * specific to each implementation.
     *
     * @param other another element of the same class as this one
     * @return a negative number, zero, or a positive number as this element is before, the same
     *         as, or after {@code other}
     * @author Wagyourtail
     * @since 1.6.5
     */
    int compareToSame(T other);
}
