package com.jsmacrosce.jsmacros.client.api.classes.render;

//? if >=26.3 {
/*import com.jsmacrosce.doclet.DocletIgnore;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase;
import java.util.ArrayList;
import java.util.List;

@DocletIgnore
public final class OverlaySubmitNodeCollection extends SubmitNodeCollection {
    public final SimpleFeatureRenderPhase overlayDepth = new SimpleFeatureRenderPhase();
    public final SimpleFeatureRenderPhase overlayTop = new SimpleFeatureRenderPhase();
    private final List<FeatureRenderPhase<?>> phases;

    public OverlaySubmitNodeCollection(TranslucentFeatureRenderPhase seeThrough) {
        super(false, seeThrough);
        List<FeatureRenderPhase<?>> all = new ArrayList<>(super.allPhases());
        all.add(overlayDepth);
        all.add(overlayTop);
        phases = List.copyOf(all);
    }

    @Override
    public List<FeatureRenderPhase<?>> allPhases() {
        return phases;
    }

    public static boolean hasTopSubmits(SubmitNodeStorage storage) {
        for (var layer : storage.getSubmitsPerOrder().values()) {
            if (!layer.alwaysOnTopGizmos.isEmpty()
                    || layer instanceof OverlaySubmitNodeCollection overlay && !overlay.overlayTop.isEmpty()) return true;
        }
        return false;
    }
}
*///?}
