package com.jsmacrosce.jsmacros.client.api.classes.render.components;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.render.IDraw2D;

 /**
  * Base class for builders that configure render elements for a 2D overlay.
  * <pre>
 * const draw = Hud.createDraw2D();
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
    * Overlay to which {@link #buildAndAdd()} adds elements created by this builder.
    */
    protected final IDraw2D<?> parent;

    protected RenderElementBuilder(IDraw2D<?> parent) {
        this.parent = parent;
    }

    /**
    * Creates an element associated with the parent overlay without adding it to the
    * overlay.
    *
    * @return the created element
    * @since 1.8.4
    */
    public T build() {
        return createElement();
    }

    /**
    * Creates an element and attempts to add it to the parent overlay. If addition is
    * refused, the created element is still returned.
    *
    * @return the created element, whether or not it was added
    * @since 1.8.4
    */
    public T buildAndAdd() {
        T element = createElement();
        parent.reAddElement(element);
        return element;
    }

    protected abstract T createElement();

}
