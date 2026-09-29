package com.jsmacrosce.jsmacros.client.api.classes.render.components;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.render.IDraw2D;

 /**
 * the base every element builder on a 2D overlay is built from.
 * <p>
 * A script reaches it through the {@code Builder} nested inside each element class, and
 * those are all made by the {@code xxxBuilder} methods on the overlay, which is what
 * binds one to it. Two of the inherited methods finish the job, and the difference
 * between them is whether the element is put on that overlay.
 * <p>
 * Everything before them is named after the thing being set rather than taking it
 * positionally, and every one of those methods hands the builder straight back, so a
 * whole element is one chain.
 * example:
 * <pre>
 * const draw = Hud.createDraw2D();
 * // one chain, then the two ways to finish it
 * draw.lineBuilder().pos(0, 0, 50, 50).color(0xFFFFFFFF).width(2).buildAndAdd();
 * draw.register();
 * </pre>
 *
 * @param <T> the type of the render element for this builder
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Rendering/Graphics")
public abstract class RenderElementBuilder<T extends RenderElement> {

    /**
    * the overlay this builder was made from, which is where {@code buildAndAdd} puts
    * the element and which the element is bound to either way.
    */
    protected final IDraw2D<?> parent;

    protected RenderElementBuilder(IDraw2D<?> parent) {
        this.parent = parent;
    }

    /**
    * makes the element and hands it back, without putting it on the overlay.
    * <p>
    * The element is still bound to the overlay the builder came from, so its parent
    * width and height are the overlay's rather than the window's, but the overlay does
    * not know it is there and will not draw it. {@code reAddElement} on the overlay is
    * what puts a built element on.
    * <p>
    * A builder is not consumed by building, so the same one can build more than one and
    * each is a separate element.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.rectBuilder().pos(10, 10, 60, 30).color(0xFFFF0000).build();
    * // nothing on the overlay yet, so nothing is drawn
    * draw.register();
    * // and now it is on
    * draw.reAddElement(rect);
    * </pre>
    *
    * @return the newly created element.
    * @since 1.8.4
    */
    public T build() {
        return createElement();
    }

    /**
    * Builds and adds the element to the draw2D the builder was created from.
    * <p>
    * The same as {@code build()} and then the overlay's {@code reAddElement}, in one
    * call. Because the return of that add is not what comes back here, the element is
    * returned even when the overlay refused it, which happens to a nested overlay
    * element in three cases: it nests nothing at all, it nests the very overlay being
    * added to, or adding it would make a cycle. Check the overlay rather than this
    * return if that matters.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const rect = draw.rectBuilder().pos(10, 10, 60, 30).color(0xFFFF0000).buildAndAdd();
    * draw.register();
    * Chat.log(`${draw.getRects().size()} rectangle on the overlay`);
    * </pre>
    *
    * @return the newly created element.
    * @since 1.8.4
    */
    public T buildAndAdd() {
        T element = createElement();
        parent.reAddElement(element);
        return element;
    }

    protected abstract T createElement();

}
