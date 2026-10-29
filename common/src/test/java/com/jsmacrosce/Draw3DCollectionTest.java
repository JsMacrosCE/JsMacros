package com.jsmacrosce;

import com.jsmacrosce.jsmacros.client.api.classes.render.Draw3D;
import com.jsmacrosce.jsmacros.client.api.classes.render.RenderSink;
import com.jsmacrosce.jsmacros.client.api.classes.render.components3d.RenderElement3D;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.List;

// Checks Draw3D's element bookkeeping without starting a client or rendering anything.
public final class Draw3DCollectionTest {
    private static int checks;

    public static void main(String[] args) {
        Draw3D draw = new Draw3D();
        TestElement first = new TestElement("first");
        TestElement second = new TestElement("second");

        check(draw.getElements().isEmpty(), "a new overlay is empty");
        check(draw.isVisible(), "a new overlay is visible");

        check(draw.reAddElement(first) == first, "reAddElement returns the element it added");
        draw.reAddElement(second);
        List<RenderElement3D<?>> snapshot = draw.getElements();
        check(snapshot.equals(List.of(first, second)), "elements stay in insertion order");

        check(draw.removeElement(first) == draw, "removeElement returns self for chaining");
        check(draw.getElements().equals(List.of(second)), "removeElement removes only the requested element");
        check(snapshot.size() == 2, "an existing snapshot is not changed by later removals");

        draw.clear();
        check(draw.getElements().isEmpty(), "clear removes every element");
        check(draw.isVisible(), "clear does not change visibility");

        check(draw.setVisible(false) == draw, "setVisible returns self for chaining");
        check(!draw.isVisible(), "setVisible updates visibility");

        System.out.println("Draw3DCollection: " + checks + " checks passed");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record TestElement(String name) implements RenderElement3D<TestElement> {
        @Override
        public void render(PoseStack poseStack, RenderSink sink, float tickDelta) {
        }

        @Override
        public int compareToSame(TestElement other) {
            return name.compareTo(other.name);
        }
    }
}
