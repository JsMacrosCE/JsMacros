package com.jsmacrosce.jsmacros.client.api.classes.render.components3d;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.NotNull;
import com.jsmacrosce.doclet.DocletIgnore;

public interface RenderElement3D<T extends RenderElement3D<?>> extends Comparable<RenderElement3D<?>> {

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

    @Override
    default int compareTo(@NotNull RenderElement3D o) {
        int i = this.getClass().getCanonicalName().compareTo(o.getClass().getCanonicalName());
        if (i == 0) {
            i = this.compareToSame((T) o);
        }
        return i;
    }

    int compareToSame(T other);
}