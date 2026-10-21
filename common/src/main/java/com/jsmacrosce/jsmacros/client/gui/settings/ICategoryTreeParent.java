package com.jsmacrosce.jsmacros.client.gui.settings;

import com.jsmacrosce.wagyourgui.containers.IContainerParent;

/**
 * Receives category-selection requests from a settings category tree, in addition to the inherited container operations.
 * A script can forward an ordered path using the array parameter:
 * <pre>
 * function selectSettingsCategory(parent, segments) {
 *   parent.selectCategory(Java.to(segments, 'java.lang.String[]'));
 * }
 * </pre>
 * Here, {@code parent} is an {@code ICategoryTreeParent} supplied by the editor or container.
 */
public interface ICategoryTreeParent extends IContainerParent {
    /**
     * Requests selection of a category path. Implementations determine how the path is handled; tree nodes prepend their own segment when forwarding the request.
     * @param category ordered path segments from outer to inner in the current selection context
     */
    void selectCategory(String[] category);

}
