package com.jsmacrosce.jsmacros.client.api.classes.render.components3d;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///? } else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.api.math.Pos2D;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.client.api.classes.render.Draw2D;
import com.jsmacrosce.jsmacros.client.api.classes.render.Draw3D;
import com.jsmacrosce.jsmacros.client.api.classes.render.components.Draw2DElement;
import com.jsmacrosce.jsmacros.client.api.classes.render.components.RenderElement;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

import java.util.Iterator;
import java.util.Objects;

//? if >=26.1 {
/*import net.minecraft.util.LightCoordsUtil;
*///? } else {
import net.minecraft.client.renderer.LightTexture;
//?}

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

//? if <=1.21.11 {
import com.mojang.blaze3d.platform.DepthTestFunction;
//? }

/**
 * @author Wagyourtail
 * @since 1.6.5
 */
@SuppressWarnings("unused")
public class Surface extends Draw2D implements RenderElement, RenderElement3D<Surface> {
    public boolean rotateToPlayer;
    public boolean rotateCenter;
    @Nullable
    public EntityHelper<?> boundEntity;
    public Pos3D boundOffset = Pos3D.ZERO;
    public final Pos3D pos;
    public final Pos3D rotations;
    protected final Pos2D sizes;
    protected int minSubdivisions;

    protected double scale;
    /**
     * scale that zIndex is multiplied by to get the actual offset (in blocks) for rendering
     * default: {@code 1/1000} if there is still z-fighting, increase this value
     *
     * @since 1.6.5
     */
    public double zIndexScale = 0.001;
    public boolean renderBack;
    public boolean cull;

    /**
     * How the surface's elements are lit.
     *
     * @since 2.0.0
     */
    private enum LightMode { FULL_BRIGHT, WORLD, CUSTOM }

    private LightMode lightMode = LightMode.FULL_BRIGHT;
    private int customLight = 0xF000F0;

    public Surface(Pos3D pos, Pos3D rotations, Pos2D sizes, int minSubdivisions, boolean renderBack, boolean cull) {
        this.pos = pos;
        this.rotations = rotations;
        this.sizes = sizes;
        this.minSubdivisions = minSubdivisions;
        this.renderBack = renderBack;
        this.cull = cull;
        init();
    }

    /**
     * @param pos the position of the surface
     * @return self for chaining.
     * @since 1.8.4
     */
    public Surface setPos(Pos3D pos) {
        this.pos.x = pos.x;
        this.pos.y = pos.y;
        this.pos.z = pos.z;
        return this;
    }

    /**
     * @param pos the position of the surface
     * @return self for chaining.
     * @since 1.8.4
     */
    public Surface setPos(BlockPosHelper pos) {
        this.pos.x = pos.getX();
        this.pos.y = pos.getY();
        this.pos.z = pos.getZ();
        return this;
    }

    public Surface setPos(double x, double y, double z) {
        this.pos.x = x;
        this.pos.y = y;
        this.pos.z = z;
        return this;
    }

    /**
     * The surface will move with the entity at the offset location.
     *
     * @param boundEntity the entity to bind the surface to
     * @return self for chaining.
     * @since 1.8.4
     */
    public Surface bindToEntity(@Nullable EntityHelper<?> boundEntity) {
        this.boundEntity = boundEntity;
        return this;
    }

    /**
     * @return the entity the surface is bound to, or {@code null} if it is not bound to an
     * entity.
     * @since 1.8.4
     */
    @Nullable
    public EntityHelper<?> getBoundEntity() {
        return boundEntity;
    }

    /**
     * @param boundOffset the offset from the entity's position to render the surface at
     * @return self for chaining.
     * @since 1.8.4
     */
    public Surface setBoundOffset(Pos3D boundOffset) {
        this.boundOffset = boundOffset;
        return this;
    }

    /**
     * @param x the x offset from the entity's position to render the surface at
     * @param y the y offset from the entity's position to render the surface at
     * @param z the z offset from the entity's position to render the surface at
     * @return self for chaining.
     * @since 1.8.4
     */
    public Surface setBoundOffset(double x, double y, double z) {
        this.boundOffset = new Pos3D(x, y, z);
        return this;
    }

    /**
     * @return the offset from the entity's position to render the surface at.
     * @since 1.8.4
     */
    public Pos3D getBoundOffset() {
        return boundOffset;
    }

    /**
     * @param rotateToPlayer whether to rotate the surface to face the player or not
     * @return self for chaining.
     * @since 1.8.4
     */
    public Surface setRotateToPlayer(boolean rotateToPlayer) {
        this.rotateToPlayer = rotateToPlayer;
        return this;
    }

    /**
     * @return {@code true} if the surface should be rotated to face the player, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean doesRotateToPlayer() {
        return rotateToPlayer;
    }

    public void setRotations(double x, double y, double z) {
        this.rotations.x = x;
        this.rotations.y = y;
        this.rotations.z = z;
    }

    public void setSizes(double x, double y) {
        this.sizes.x = x;
        this.sizes.y = y;
        recomputeScale();
    }

    public Pos2D getSizes() {
        return sizes.add(0, 0);
    }

    public void setMinSubdivisions(int minSubdivisions) {
        this.minSubdivisions = minSubdivisions;
        recomputeScale();
    }

    private void recomputeScale() {
        scale = Math.min(sizes.x, sizes.y) / minSubdivisions;
    }

    public int getMinSubdivisions() {
        return minSubdivisions;
    }

    /**
     * Makes all elements on this surface render at full brightness, ignoring world lighting.
     *
     * @return self for chaining.
     * @since 2.0.0
     */
    public Surface setFullBrightLight() {
        this.lightMode = LightMode.FULL_BRIGHT;
        return this;
    }

    /**
     * Makes all elements on this surface sample block and sky light from the world each frame
     * at the surface's position (including the day/night sky darken). Cast shadows are not
     * modelled.
     *
     * @return self for chaining.
     * @since 2.0.0
     */
    public Surface setWorldLight() {
        this.lightMode = LightMode.WORLD;
        return this;
    }

    /**
     * Sets a fixed light level for all elements on this surface.
     *
     * @param blockLight block light level, 0-15 (e.g. 15 next to a torch)
     * @param skyLight   sky light level, 0-15 (e.g. 15 outdoors in daylight)
     * @return self for chaining.
     * @since 2.0.0
     */
    public Surface setLight(int blockLight, int skyLight) {
        this.lightMode = LightMode.CUSTOM;
        this.customLight = packLight(blockLight, skyLight);
        return this;
    }

    private static int packLight(int blockLight, int skyLight) {
        //? if >=26.1 {
        /*return LightCoordsUtil.pack(blockLight, skyLight);
        *///? } else {
        return LightTexture.pack(blockLight, skyLight);
        //?}
    }

    @Override
    public int getHeight() {
        return (int) (sizes.y / scale);
    }

    @Override
    public int getWidth() {
        return (int) (sizes.x / scale);
    }

    /**
     * @param rotateCenter whether to rotate the surface around its center or not
     * @return self for chaining.
     * @since 1.8.4
     */
    public Surface setRotateCenter(boolean rotateCenter) {
        this.rotateCenter = rotateCenter;
        return this;
    }

    /**
     * @return {@code true} if this surface is rotated around it's center, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isRotatingCenter() {
        return rotateCenter;
    }

    @Override
    public void init() {
        recomputeScale();
        super.init();
    }

    @Override
    public int getZIndex() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Surface surface = (Surface) o;
        return Objects.equals(pos, surface.pos) && Objects.equals(rotations, surface.rotations) && Objects.equals(sizes, surface.sizes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos, rotations, sizes);
    }

    @Override
    public int compareToSame(Surface other) {
        int i = pos.compareTo(other.pos);
        if (i == 0) {
            i = rotations.compareTo(other.rotations);
            if (i == 0) {
                i = sizes.compareTo(other.sizes);
            }
        }
        return i;
    }

    @Override
    @DocletIgnore
    public void render(PoseStack matrices, MultiBufferSource consumers, float tickDelta) {
        // On 1.21.11+ surfaces are drawn from renderDirect inside the Gizmos pass.
        //? if <1.21.11 {
        /*renderSurface(matrices, consumers, tickDelta);
        *///? }
    }

    @Override
    @DocletIgnore
    public void renderDirect(PoseStack matrices, MultiBufferSource consumers, float tickDelta, boolean alwaysOnTop) {
        //? if >=1.21.11 {
        /*// cull surfaces are depth-tested; non-cull surfaces draw after the depth clear.
        if ((!this.cull) != alwaysOnTop) {
            return;
        }
        renderSurface(matrices, consumers, tickDelta);
        *///? }
    }

    private void renderSurface(PoseStack matrices, MultiBufferSource consumers, float tickDelta) {
        boolean seeThrough = !this.cull;
        Pos3D renderPos = resolveRenderPos(tickDelta);
        updateRotateToPlayer(renderPos);
        Matrix4f transform = buildSurfaceTransform(renderPos);
        if (!renderBack && isCameraOnBackSide(transform)) {
            return;
        }
        int light = resolveLight(renderPos);
        matrices.pushPose();
        matrices.mulPose(transform);
        synchronized (elements) {
            renderDirectElements(matrices, consumers, light, seeThrough, tickDelta, getElementsByZIndex());
        }
        matrices.popPose();
    }

    /**
     * True when the camera is behind the surface's readable face. The surface
     * content is authored facing local -Z, so the camera is behind when it lies on
     * the +Z side of the surface plane.
     */
    private static boolean isCameraOnBackSide(Matrix4f transform) {
        //? if >=1.21.11 {
        /*Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        *///? } else {
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        //? }
        Vector3f origin = transform.transformPosition(new Vector3f(0, 0, 0));
        Vector3f front = transform.transformPosition(new Vector3f(0, 0, 1)).sub(origin);
        return front.x * (cameraPos.x - origin.x)
                + front.y * (cameraPos.y - origin.y)
                + front.z * (cameraPos.z - origin.z) > 0;
    }

    private Pos3D resolveRenderPos(float partialTicks) {
        boolean isTrackingEntity = boundEntity != null && boundEntity.isAlive();
        return isTrackingEntity ? boundEntity.getInterpolatedPos(partialTicks).add(boundOffset) : pos;
    }

    private void updateRotateToPlayer(Pos3D renderPos) {
        if (!rotateToPlayer) {
            return;
        }
        //? if >=1.21.11 {
        /*Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        *///? } else {
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        //? }
        double pivotX = rotateCenter ? renderPos.x + (sizes.x / 2.0) : renderPos.x;
        double pivotY = rotateCenter ? renderPos.y - (sizes.y / 2.0) : renderPos.y;
        double pivotZ = renderPos.z;
        double dx = cameraPos.x - pivotX;
        double dy = cameraPos.y - pivotY;
        double dz = cameraPos.z - pivotZ;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        rotations.x = -Math.toDegrees(Math.atan2(dy, horizontal));
        rotations.y = Math.toDegrees(Math.atan2(dx, dz));
        rotations.z = 0;
    }

    private void renderDirectElements(PoseStack matrices, MultiBufferSource consumers, int light, boolean seeThrough, float tickDelta, Iterator<RenderElement> iter) {
        while (iter.hasNext()) {
            RenderElement element = iter.next();
            if (element instanceof Draw2DElement draw2DElement) {
                // Give the nested panel its zIndex depth so it does not sit coplanar
                // with the parent's rects (older targets write depth in debugQuads).
                matrices.pushPose();
                if (scale != 0) {
                    matrices.translate(0, 0, (float) ((zIndexScale / scale) * element.getZIndex()));
                }
                renderNestedDirect(matrices, consumers, light, seeThrough, tickDelta, draw2DElement);
                matrices.popPose();
                continue;
            }
            matrices.pushPose();
            if (scale != 0) {
                matrices.translate(0, 0, (float) ((zIndexScale / scale) * element.getZIndex()));
            }
            element.render3D(matrices, consumers, light, seeThrough, tickDelta);
            // debugQuads sorts quads by camera distance on upload, which ignores
            // zIndex order, so flush each element as its own batch and rely on
            // painter's order.
            if (consumers instanceof MultiBufferSource.BufferSource bufferSource) {
                bufferSource.endBatch();
            }
            matrices.popPose();
        }
    }

    private void renderNestedDirect(PoseStack matrices, MultiBufferSource consumers, int light, boolean seeThrough, float tickDelta, Draw2DElement element) {
        matrices.pushPose();
        matrices.translate(element.x, element.y, 0);
        matrices.scale((float) element.scale, (float) element.scale, 1);
        float centerX = element.getWidth() / 2f;
        float centerY = element.getHeight() / 2f;
        if (element.rotateCenter) {
            matrices.translate(centerX, centerY, 0);
        }
        matrices.mulPose(new Quaternionf().rotateLocalZ((float) Math.toRadians(element.rotation)));
        if (element.rotateCenter) {
            matrices.translate(-centerX, -centerY, 0);
        }
        Draw2D draw2D = element.getDraw2D();
        synchronized (draw2D.getElements()) {
            renderDirectElements(matrices, consumers, light, seeThrough, tickDelta, draw2D.getElementsByZIndex());
        }
        matrices.popPose();
    }

    private Matrix4f buildSurfaceTransform(Pos3D renderPos) {
        Matrix4f m = new Matrix4f();
        m.translate((float) renderPos.x, (float) renderPos.y, (float) renderPos.z);
        float halfX = (float) (sizes.x / 2.0);
        float halfY = (float) (sizes.y / 2.0);
        if (rotateCenter) {
            // Rotate about the surface centre by giving each axis its own pivot.
            m.translate(halfX, 0, 0);
            m.rotateY((float) Math.toRadians(rotations.y));
            m.translate(-halfX, 0, 0);
            m.translate(0, -halfY, 0);
            m.rotateX((float) Math.toRadians(rotations.x));
            m.translate(0, halfY, 0);
            m.translate(halfX, -halfY, 0);
            m.rotateZ((float) Math.toRadians(rotations.z));
            m.translate(-halfX, halfY, 0);
        } else {
            m.rotateY((float) Math.toRadians(rotations.y));
            m.rotateX((float) Math.toRadians(rotations.x));
            m.rotateZ((float) Math.toRadians(rotations.z));
        }
        // Flip y (surface space grows downwards) and map surface pixels to blocks.
        m.scale((float) scale, (float) -scale, (float) scale);
        return m;
    }

    private int resolveLight(Pos3D renderPos) {
        return switch (lightMode) {
            case FULL_BRIGHT -> 0xF000F0;
            case CUSTOM -> customLight;
            case WORLD -> {
                var level = Minecraft.getInstance().level;
                if (level == null) {
                    yield 0xF000F0;
                }
                BlockPos blockPos = renderPos.toRawBlockPos();
                int block = level.getBrightness(LightLayer.BLOCK, blockPos);
                // Subtract the sky darken so the surface also dims at night, like
                // the vanilla lightmap. Cast shadows are not modelled.
                int sky = Math.max(0, level.getBrightness(LightLayer.SKY, blockPos) - level.getSkyDarken());
                yield packLight(block, sky);
            }
        };
    }

    private static Vector3f toEulerDegrees(Quaternionf quaternion) {
        // The old method
        float w = quaternion.w();
        float x = quaternion.x();
        float y = quaternion.y();
        float z = quaternion.z();

        float wSquared = w * w;
        float xSquared = x * x;
        float ySquared = y * y;
        float zSquared = z * z;
        float sumSquared = wSquared + xSquared + ySquared + zSquared;
        float k = 2.0F * w * x - 2.0F * y * z;

        double radianX = Math.asin(k / sumSquared);
        double radianY;
        double radianZ;
        if (Math.abs(k) > 0.999F * sumSquared) {
            radianY = 2.0F * Math.atan2(y, w);
            radianZ = 0.0F;
        } else {
            radianY = Math.atan2(2.0F * x * z + 2.0F * y * w, wSquared - xSquared - ySquared + zSquared);
            radianZ = Math.atan2(2.0F * x * y + 2.0F * w * z, wSquared - xSquared + ySquared - zSquared);
        }
        return new Vector3f((float) Math.toDegrees(radianX), (float) Math.toDegrees(radianY), (float) Math.toDegrees(radianZ));
    }

    //? if >=26.1 {
    /*private void renderElements3D(GuiGraphicsExtractor drawContext, Iterator<RenderElement> iter) {
    *///?} else {
    private void renderElements3D(GuiGraphics drawContext, Iterator<RenderElement> iter) {
    //?}
        while (iter.hasNext()) {
            RenderElement element = iter.next();
            // Render each draw2D element individually so that the cull and renderBack settings are used
            if (element instanceof Draw2DElement draw2DElement) {
                renderDraw2D3D(drawContext, draw2DElement);
            } else {
                renderElement3D(drawContext, element);
            }
        }
    }

    //? if >=26.1 {
    /*private void renderDraw2D3D(GuiGraphicsExtractor drawContext, Draw2DElement element) {
    *///?} else {
    private void renderDraw2D3D(GuiGraphics drawContext, Draw2DElement element) {
    //?}
        // TODO: Does setupMatrix operate the same here? Why does it have the final translation?
        //? if >1.21.5 {
        Matrix3x2fStack matrixStack = drawContext.pose();
        matrixStack.pushMatrix();
        setupMatrix(matrixStack, element.x, element.y, element.scale, element.rotation, element.getWidth(), element.getHeight(), element.rotateCenter);
        //?} else {
        /*PoseStack matrixStack = drawContext.pose();
        matrixStack.pushPose();
        matrixStack.translate(element.x, element.y, 0);
        matrixStack.scale(element.scale, element.scale, 1);
        if (rotateCenter) {
            matrixStack.translate(element.width.getAsInt() / 2d, element.height.getAsInt() / 2d, 0);
        }
        matrixStack.mulPose(new Quaternionf().rotateLocalZ((float) Math.toRadians(element.rotation)));
        if (rotateCenter) {
            matrixStack.translate(-element.width.getAsInt() / 2d, -element.height.getAsInt() / 2d, 0);
        }
        *///?}

        // Don't translate back!
        Draw2D draw2D = element.getDraw2D();
        synchronized (draw2D.getElements()) {
            renderElements3D(drawContext, draw2D.getElementsByZIndex());
        }
        //? if >1.21.5 {
        matrixStack.popMatrix();
        //?} else {
        /*matrixStack.popPose();
        *///?}
    }

    //? if >=26.1 {
    /*private void renderElement3D(GuiGraphicsExtractor drawContext, RenderElement element) {
    *///?} else {
    private void renderElement3D(GuiGraphics drawContext, RenderElement element) {
    //?}
        // TODO: Someone removed this around 1.21.5, is it needed?
        /*
        if (renderBack) {
            RenderSystem.disableCull();
        } else {
            RenderSystem.enableCull();
        }

        if (!cull) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }
        */

        //? if >1.21.5 {
        Matrix3x2fStack matrixStack = drawContext.pose();
        matrixStack.pushMatrix();
        // Z-index is no longer possible as this is a 3x2 matrix now.
        //matrixStack.translate(0, 0, zIndexScale * element.getZIndex());
        element.render3D(drawContext, 0, 0, 0);
        matrixStack.popMatrix();
        //?} else {
        /*PoseStack matrices = drawContext.pose();
        matrices.pushPose();
        matrices.translate(0, 0, zIndexScale * element.getZIndex());
        element.render3D(drawContext, 0, 0, 0);
        matrices.popPose();
        *///?}
    }

    @Override
    //? if >=26.1 {
    /*public void render(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
    *///?} else {
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
    //?}
        // This does nothing I guess?
    }

    /**
     * @author Etheradon
     * @since 1.8.4
     */
    public static class Builder {
        private final Draw3D parent;

        private Pos3D pos = new Pos3D(0, 0, 0);
        @Nullable
        private EntityHelper<?> boundEntity;
        private Pos3D boundOffset = Pos3D.ZERO;
        private double xRot = 0;
        private double yRot = 0;
        private double zRot = 0;
        private boolean rotateCenter = true;
        private boolean rotateToPlayer = false;
        private double width = 10;
        private double height = 10;
        private int minSubdivisions = 1;
        private double zIndexScale = 0.001;
        private boolean renderBack = true;
        private boolean cull = false;
        private LightMode lightMode = LightMode.FULL_BRIGHT;
        private int customLight = 0xF000F0;

        public Builder(Draw3D parent) {
            this.parent = parent;
        }

        /**
         * @param pos the position of the surface
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos(Pos3D pos) {
            this.pos = pos;
            return this;
        }

        /**
         * @param pos the position of the surface
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos(BlockPosHelper pos) {
            this.pos = pos.toPos3D();
            return this;
        }

        /**
         * @param x the x position of the surface
         * @param y the y position of the surface
         * @param z the z position of the surface
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos(double x, double y, double z) {
            this.pos = new Pos3D(x, y, z);
            return this;
        }

        /**
         * @return the position of the surface.
         * @since 1.8.4
         */
        public Pos3D getPos() {
            return pos;
        }

        /**
         * The surface will move with the entity at the offset location.
         *
         * @param boundEntity the entity to bind the surface to
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder bindToEntity(@Nullable EntityHelper<?> boundEntity) {
            this.boundEntity = boundEntity;
            return this;
        }

        /**
         * @return the entity the surface is bound to, or {@code null} if it is not bound to an
         * entity.
         * @since 1.8.4
         */
        @Nullable
        public EntityHelper<?> getBoundEntity() {
            return boundEntity;
        }

        /**
         * @param entityOffset the offset from the entity's position to render the surface at
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder boundOffset(Pos3D entityOffset) {
            this.boundOffset = entityOffset;
            return this;
        }

        /**
         * @param x the x offset from the entity's position to render the surface at
         * @param y the y offset from the entity's position to render the surface at
         * @param z the z offset from the entity's position to render the surface at
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder boundOffset(double x, double y, double z) {
            this.boundOffset = new Pos3D(x, y, z);
            return this;
        }

        /**
         * @return the offset from the entity's position to render the surface at.
         * @since 1.8.4
         */
        public Pos3D getBoundOffset() {
            return boundOffset;
        }

        /**
         * @param xRot the x rotation of the surface
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder xRotation(double xRot) {
            this.xRot = xRot;
            return this;
        }

        /**
         * @return the x rotation of the surface.
         * @since 1.8.4
         */
        public double getXRotation() {
            return xRot;
        }

        /**
         * @param yRot the y rotation of the surface
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder yRotation(double yRot) {
            this.yRot = yRot;
            return this;
        }

        /**
         * @return the y rotation of the surface.
         * @since 1.8.4
         */
        public double getYRotation() {
            return yRot;
        }

        /**
         * @param zRot the z rotation of the surface
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder zRotation(double zRot) {
            this.zRot = zRot;
            return this;
        }

        /**
         * @return the z rotation of the surface.
         * @since 1.8.4
         */
        public double getZRotation() {
            return zRot;
        }

        /**
         * @param xRot the x rotation of the surface
         * @param yRot the y rotation of the surface
         * @param zRot the z rotation of the surface
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder rotation(double xRot, double yRot, double zRot) {
            this.xRot = xRot;
            this.yRot = yRot;
            this.zRot = zRot;
            return this;
        }

        /**
         * @param rotateCenter whether to rotate around the center of the surface
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder rotateCenter(boolean rotateCenter) {
            this.rotateCenter = rotateCenter;
            return this;
        }

        /**
         * @return {@code true} if this surface should be rotated around its center,
         * {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isRotatingCenter() {
            return rotateCenter;
        }

        /**
         * @param rotateToPlayer whether to rotate the surface to face the player or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder rotateToPlayer(boolean rotateToPlayer) {
            this.rotateToPlayer = rotateToPlayer;
            return this;
        }

        /**
         * @return {@code true} if the surface should be rotated to face the player,
         * {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean doesRotateToPlayer() {
            return rotateToPlayer;
        }

        /**
         * @param width the width of the surface
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder width(double width) {
            this.width = width;
            return this;
        }

        /**
         * @return the width of the surface.
         * @since 1.8.4
         */
        public double getWidth() {
            return width;
        }

        /**
         * @param height the height of the surface
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder height(double height) {
            this.height = height;
            return this;
        }

        /**
         * @return the height of the surface.
         * @since 1.8.4
         */
        public double getHeight() {
            return height;
        }

        /**
         * @param width  the width of the surface
         * @param height the height of the surface
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder size(double width, double height) {
            this.width = width;
            this.height = height;
            return this;
        }

        /**
         * @param minSubdivisions the minimum number of subdivisions
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder minSubdivisions(int minSubdivisions) {
            this.minSubdivisions = minSubdivisions;
            return this;
        }

        /**
         * @return the minimum number of subdivisions.
         * @since 1.8.4
         */
        public int getMinSubdivisions() {
            return minSubdivisions;
        }

        /**
         * @param renderBack whether the back of the surface should be rendered or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder renderBack(boolean renderBack) {
            this.renderBack = renderBack;
            return this;
        }

        /**
         * @return {@code true} if the back of the surface should be rendered, {@code false}
         * otherwise.
         * @since 1.8.4
         */
        public boolean shouldRenderBack() {
            return renderBack;
        }

        /**
         * @param cull whether to enable culling or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder cull(boolean cull) {
            this.cull = cull;
            return this;
        }

        /**
         * @return {@code true} if culling is enabled for this box, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isCulled() {
            return cull;
        }

        /**
         * @param zIndexScale the scale of the z-index
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder zIndex(double zIndexScale) {
            this.zIndexScale = zIndexScale;
            return this;
        }

        /**
         * @return the scale of the z-index.
         * @since 1.8.4
         */
        public double getZIndexScale() {
            return zIndexScale;
        }

        /**
         * Renders all elements at full brightness, ignoring world lighting.
         *
         * @return self for chaining.
         * @since 2.0.0
         */
        public Builder fullBrightLight() {
            this.lightMode = LightMode.FULL_BRIGHT;
            return this;
        }

        /**
         * Samples block and sky light from the world each frame at the surface's position.
         *
         * @return self for chaining.
         * @since 2.0.0
         */
        public Builder worldLight() {
            this.lightMode = LightMode.WORLD;
            return this;
        }

        /**
         * Sets a fixed light level for all elements on this surface.
         *
         * @param blockLight block light level, 0-15 (e.g. 15 next to a torch)
         * @param skyLight   sky light level, 0-15 (e.g. 15 outdoors in daylight)
         * @return self for chaining.
         * @since 2.0.0
         */
        public Builder light(int blockLight, int skyLight) {
            this.lightMode = LightMode.CUSTOM;
            this.customLight = packLight(blockLight, skyLight);
            return this;
        }

        /**
         * Creates the surface for the given values and adds it to the draw3D.
         *
         * @return the build surface.
         * @since 1.8.4
         */
        public Surface buildAndAdd() {
            Surface surface = build();
            parent.addSurface(surface);
            return surface;
        }

        /**
         * Builds the surface from the given values.
         *
         * @return the build surface.
         */
        public Surface build() {
            Surface surface = new Surface(
                    pos,
                    new Pos3D(xRot, yRot, zRot),
                    new Pos2D(width, height),
                    minSubdivisions,
                    renderBack,
                    cull
            )
                    .setRotateCenter(rotateCenter)
                    .setRotateToPlayer(rotateToPlayer)
                    .bindToEntity(boundEntity)
                    .setBoundOffset(boundOffset);
            surface.lightMode = lightMode;
            surface.customLight = customLight;
            return surface;
        }

    }

}
