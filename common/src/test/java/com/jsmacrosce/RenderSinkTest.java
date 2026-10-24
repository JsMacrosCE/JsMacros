package com.jsmacrosce;

// Native feature-routing checks; no game, rendering device, or scripts are started.
//? if >=26.2 {
/*import com.jsmacrosce.jsmacros.client.api.classes.render.RenderSink;
import com.jsmacrosce.jsmacros.client.api.classes.render.components3d.SurfaceRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.ArrayList;
import java.util.List;

public final class RenderSinkTest {
    private static int checks;

    public static void main(String[] args) {
        mixedLayers(false);
        mixedLayers(true);
        existingOrders();
        frameReuse();
        frameCleanup();
        poseSnapshots();
        itemFeatures();
        System.out.println("RenderSink: 7 scenarios, " + checks + " checks passed");
    }

    private static void mixedLayers(boolean alwaysOnTop) {
        SubmitNodeStorage storage = new SubmitNodeStorage();
        RenderSink sink = new RenderSink(storage, alwaysOnTop);
        PoseStack pose = new PoseStack();
        RenderType quads = SurfaceRenderTypes.quads(false, !alwaysOnTop);
        RenderType other = SurfaceRenderTypes.quads(true, !alwaysOnTop);
        sink.submitGeometry(pose, quads, (p, vc) -> {});
        sink.submitGeometry(pose, other, (p, vc) -> {});
        sink.submitGeometry(pose, quads, (p, vc) -> {});
        sink.drawText(pose, text(), 0, 0, -1, false, Font.DisplayMode.NORMAL, 15728880);
        sink.submitItem(pose, item(false), 15728880);

        List<SubmitNode> nodes = drain(storage, alwaysOnTop);
        check(nodes.size() == 5, "one native layer per mixed submission");
        check(((CustomFeatureRenderer.Submit) nodes.get(0)).renderType() == quads, "first rect stays first");
        check(((CustomFeatureRenderer.Submit) nodes.get(1)).renderType() == other, "different pipeline stays between rects");
        check(((CustomFeatureRenderer.Submit) nodes.get(2)).renderType() == quads, "second rect is not batched with first");
        check(nodes.get(3) instanceof TextFeatureRenderer.Submit, "text stays after geometry");
        check(nodes.get(4) instanceof ItemFeatureRenderer.Submit, "item stays after text");
    }

    private static void existingOrders() {
        SubmitNodeStorage storage = new SubmitNodeStorage();
        storage.order(1).submitText(new PoseStack(), 0, 0, text(), false, Font.DisplayMode.NORMAL, 0, -1, 0, 0);
        storage.order(2).outline.submit(new TextFeatureRenderer.Submit(new PoseStack().last().pose(),
                0, 0, text(), false, Font.DisplayMode.NORMAL, 0, -1, 0, 0));
        RenderSink first = new RenderSink(storage, false);
        RenderSink second = new RenderSink(storage, false);
        first.drawText(new PoseStack(), text(), 0, 0, -1, false, Font.DisplayMode.NORMAL, 0);
        second.drawText(new PoseStack(), text(), 0, 0, -1, false, Font.DisplayMode.NORMAL, 0);
        check(storage.getSubmitsPerOrder().containsKey(3), "checks all phases before reusing an order");
        check(storage.getSubmitsPerOrder().containsKey(4), "separate sinks do not share layers");
        storage.order(1).texts.sortInto((node, sorted) -> {});
        storage.order(2).outline.sortInto((node, sorted) -> {});
        check(drain(storage, false).size() == 2, "existing empty orders add no nodes");
    }

    private static void frameReuse() {
        SubmitNodeStorage storage = new SubmitNodeStorage();
        var firstLayer = storage.order(1);
        for (int frame = 0; frame < 3; frame++) {
            RenderSink sink = new RenderSink(storage, false);
            for (int i = 0; i < 3; i++) {
                sink.drawText(new PoseStack(), text(), 0, 0, -1, false, Font.DisplayMode.NORMAL, 0);
            }
            check(drain(storage, false).size() == 3, "frame contains only its own nodes");
            check(storage.getSubmitsPerOrder().lastIntKey() == 3, "empty native layers are reused next frame");
            check(storage.getSubmitsPerOrder().get(1) == firstLayer, "reuses the existing layer instance");
        }
    }

    private static void frameCleanup() {
        for (boolean alwaysOnTop : new boolean[]{false, true}) {
            SubmitNodeStorage storage = new SubmitNodeStorage();
            for (int count : new int[]{256, 3, 0, 64, 1, 0}) {
                RenderSink sink = new RenderSink(storage, alwaysOnTop);
                for (int i = 0; i < count; i++) {
                    sink.drawText(new PoseStack(), text(), 0, 0, -1, false, Font.DisplayMode.NORMAL, 0);
                }
                check(drain(storage, alwaysOnTop).size() == count, "native drain keeps only this frame's nodes");
                check(storage.getSubmitsPerOrder().size() == count, "native drain removes unused empty layers");
            }
        }
    }

    private static void poseSnapshots() {
        SubmitNodeStorage storage = new SubmitNodeStorage();
        RenderSink sink = new RenderSink(storage, false);
        PoseStack pose = new PoseStack();
        pose.translate(10, 20, 30);
        PositionConsumer consumer = new PositionConsumer();
        sink.submitGeometry(pose, SurfaceRenderTypes.quads(false, true),
                (p, vc) -> vc.addVertex(p, 1, 2, 3));
        sink.drawText(pose, text(), 1, 2, -1, true, Font.DisplayMode.NORMAL, 0);
        pose.translate(100, 200, 300);
        check(consumer.vertices == 0, "geometry is deferred until native preparation");
        List<SubmitNode> nodes = drain(storage, false);
        CustomFeatureRenderer.Submit geometry = (CustomFeatureRenderer.Submit) nodes.get(0);
        geometry.customGeometryRenderer().render(geometry.pose(), consumer);
        check(consumer.vertices == 1 && consumer.x == 11 && consumer.y == 22 && consumer.z == 33,
                "geometry keeps submission pose after caller changes the stack");
        TextFeatureRenderer.Submit text = (TextFeatureRenderer.Submit) nodes.get(1);
        check(text.pose().m30() == 10 && text.pose().m31() == 20 && text.pose().m32() == 30,
                "text keeps submission pose");
    }

    private static void itemFeatures() {
        SubmitNodeStorage storage = new SubmitNodeStorage();
        RenderSink sink = new RenderSink(storage, true);
        sink.submitItem(new PoseStack(), item(true), 15728880);
        sink.submitItem(new PoseStack(), item(true), 15728880);
        List<SubmitNode> nodes = drain(storage, true);
        check(nodes.size() == 4, "reused item extraction does not retain old nodes");
        check(nodes.get(0) instanceof ItemFeatureRenderer.Submit && nodes.get(1) instanceof TextFeatureRenderer.Submit,
                "special item features follow the item into the selected phase");
        sink.submitItem(new PoseStack(), new ItemStackRenderState(), 0);
        check(drain(storage, true).isEmpty(), "empty items submit no layers");
    }

    private static ItemStackRenderState item(boolean special) {
        return new ItemStackRenderState() {
            @Override
            public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, int outline) {
                collector.submitItem(pose, ItemDisplayContext.GUI, light, overlay, outline,
                        new int[0], List.of(), FoilType.NONE);
                if (special) {
                    collector.order(7).submitText(pose, 0, 0, text(), false, Font.DisplayMode.NORMAL,
                            light, -1, 0, 0);
                }
            }
        };
    }

    private static FormattedCharSequence text() {
        return FormattedCharSequence.forward("overlay", Style.EMPTY);
    }

    private static List<SubmitNode> drain(SubmitNodeStorage storage, boolean alwaysOnTop) {
        List<SubmitNode> nodes = new ArrayList<>();
        for (var layer : storage.getSubmitsPerOrder().values()) {
            var target = alwaysOnTop ? layer.alwaysOnTop : layer.afterTerrain;
            for (var phase : layer.allPhases()) {
                check(phase == target || phase.isEmpty(), "no nodes leak into another depth or feature phase");
            }
        }
        storage.drainPhases(phase -> {
            int before = nodes.size();
            phase.sortInto((node, sorted) -> nodes.add(node));
            check(nodes.size() - before <= 1, "each layer contains at most one node");
        });
        for (var layer : storage.getSubmitsPerOrder().values()) {
            for (var phase : layer.allPhases()) {
                check(phase.isEmpty(), "native drain consumes every node");
            }
        }
        return nodes;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class PositionConsumer implements VertexConsumer {
        private int vertices;
        private float x;
        private float y;
        private float z;

        public VertexConsumer addVertex(float x, float y, float z) {
            vertices++;
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        public VertexConsumer setColor(int color) { return this; }
        public VertexConsumer setUv(float u, float v) { return this; }
        public VertexConsumer setUv1(int u, int v) { return this; }
        public VertexConsumer setUv2(int u, int v) { return this; }
        public VertexConsumer setNormal(float x, float y, float z) { return this; }
        public VertexConsumer setLineWidth(float width) { return this; }
    }
}
*///?}
