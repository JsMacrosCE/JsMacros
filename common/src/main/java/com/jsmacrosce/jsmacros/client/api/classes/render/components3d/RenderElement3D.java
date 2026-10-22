package com.jsmacrosce.jsmacros.client.api.classes.render.components3d;

import com.jsmacrosce.doclet.DocletCategory;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.NotNull;
import com.jsmacrosce.doclet.DocletIgnore;

/**
 * What a {@code Draw3D} holds: one thing to draw in the world, and an order to draw it in.
 * <p>
 * Everything a 3D overlay can contain implements this. {@link Box} is the cube outline,
 * {@link Line3D} the straight segment, {@link TraceLine} the line that starts at the camera and
 * runs to a point, and {@link Surface} the flat panel that is a 2D overlay living in the world
 * instead of on the screen. Adding one to a {@code Draw3D} is what puts it in the world; the
 * overlay has to be registered for that to happen.
 * <p>
 * The order is not the order things were added. Before drawing, the overlay sorts its contents
 * with {@link #compareTo(RenderElement3D)}, which is two level: elements of different classes are
 * grouped by class name compared as text, and only two elements of the <em>same</em> class are
 * compared against each other at all, through {@link #compareToSame(RenderElement3D)}. The
 * grouping means all the cubes of a mixed overlay are drawn before all its lines, and which of the
 * two kinds of line comes first is decided by the class names, not by anything a script sets.
 * <p>
 * Within one class, what the comparison looks at differs by element: a {@link Box} and a
 * {@link Line3D} both compare their positions, a {@link Surface} compares its position, rotations
 * and size, and a {@link TraceLine} compares where its line ends up pointing.
 * <p>
 * A script gets this interface rather rarely. Reading what a 3D overlay contains goes through the
 * typed getters on {@code Draw3D}, and comparing two elements to each other is the one thing a
 * script can do with it directly.
 * example:
 * <pre>
 * const draw = Hud.createDraw3D();
 * draw.addBox(0, 64, 0, 1, 65, 1, 0xFFFF0000, 0x00000000, false);
 * draw.addBox(10, 64, 10, 11, 65, 11, 0xFF00FF00, 0x00000000, false);
 * draw.register();
 * // two boxes, so they are compared on position: the one nearer the
 * // origin sorts first, whichever order they were added in
 * const boxes = draw.getBoxes();
 * Chat.log(`first before second: ${boxes.get(0).compareToSame(boxes.get(1)) < 0}`);
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.6.5
 */
@DocletCategory("Rendering/Graphics")
public interface RenderElement3D<T extends RenderElement3D<?>> extends Comparable<RenderElement3D<?>> {

    /**
     * draws this element into the world, at the given fraction of a tick.
     *
     * @param matrices the world transform, already translated so the camera is at the origin
     * @param consumers the buffer source the geometry goes into
     * @param tickDelta how far into the current tick this frame is, from 0 to 1, used to
     *                 interpolate anything that moves
     */
    @DocletIgnore
    void render(PoseStack matrices, MultiBufferSource consumers, float tickDelta);

    /**
     * Renders elements that draw themselves directly into the buffer source rather
     * than through the Gizmos API. {@code alwaysOnTop} splits the depth-tested group
     * (false) from the group drawn after the depth clear (true).
     */
    @DocletIgnore
    default void renderDirect(PoseStack matrices, MultiBufferSource consumers, float tickDelta, boolean alwaysOnTop) {
    }

    /**
     * orders this element against another one, by class first and then by position.
     * <p>
     * The class names are compared as text, so elements of different classes are
     * grouped together before any of them are compared against each other, and only two
     * elements of the same class reach {@link #compareToSame(RenderElement3D)}. In this
     * package that groups them as {@code Box}, {@code EntityTraceLine}, {@code Line3D},
     * {@code Surface}, {@code TraceLine}, in that order.
     * <p>
     * This is a default method, so a class implementing the interface gets it for free
     * and only has to say how two of its own compare.
     * <p>
     * This is what a 3D overlay sorts its contents with before each frame, which is why
     * what {@code Draw3D.getBoxes()} and its siblings return is in draw order rather
     * than the order things were added in.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const box = draw.addBox(0, 64, 0, 1, 65, 1, 0xFFFFFFFF, 0x00000000, false);
     * const line = draw.addLine(0, 64, 0, 8, 72, 0, 0xFFFFFFFF);
     * draw.register();
     * // a box and a line are different classes, so the class name decides,
     * // not where they are
     * Chat.log(`box before line: ${box.compareTo(line) < 0}`);
     * </pre>
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
     * orders this element against another of the same class.
     * <p>
     * Only ever reached for two elements whose classes are identical, because
     * {@link #compareTo(RenderElement3D)} settles the class-name question first and only calls
     * this when that comparison tied. What it looks at is up to the element: what a
     * {@link Box} compares is its two corners, what a {@link Surface} compares is its position,
     * rotations and size, and what a {@link TraceLine} compares is where its line points.
     * <p>
     * The result is a plain comparison on coordinates rather than a distance, so two elements
     * that overlap still order against each other instead of tying.
     * example:
     <pre>
     * const a = Hud.createDraw3D().addPoint(0, 64, 0, 0.5, 0xFFFF0000);
     * const b = Hud.createDraw3D().addPoint(8, 64, 0, 0.5, 0xFF00FF00);
     * Chat.log(`a sorts before b: ${a.compareToSame(b) < 0}`);
     * </pre>
     *
     * @param other another element of the same class as this one
     * @return a negative number, zero, or a positive number as this element is before, the same
     *         as, or after {@code other}
     * @author Wagyourtail
     * @since 1.6.5
     */
    int compareToSame(T other);
}