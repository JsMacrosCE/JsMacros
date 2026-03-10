package com.jsmacrosce.jsmacros.client.api.classes.render.components3d;

import com.jsmacrosce.jsmacros.client.JsMacros;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
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
    public Pos3D boundOffset;
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

    private enum LightMode { FULL_BRIGHT, WORLD, CUSTOM }
    private LightMode lightMode = LightMode.FULL_BRIGHT;
    private int customLight = 0xF000F0;

    public Surface(Pos3D pos, Pos3D rotations, Pos2D sizes, int minSubdivisions, boolean renderBack, boolean cull) {
        this.pos = pos;
        this.rotations = rotations;
        this.sizes = sizes;
        this.minSubdivisions = Math.max(minSubdivisions, 1);
        if (minSubdivisions != this.minSubdivisions) {
            JsMacros.LOGGER.warn("Surface instantiated with invalid minSubdivisions: {}, defaulting to 1",
                    minSubdivisions);
        }
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

    public int getMinSubdivisions() {
        return minSubdivisions;
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

    /**
     * Makes all elements on this surface render at full brightness, ignoring world lighting.
     * This is the default behaviour.
     *
     * @return self for chaining.
     * @since 1.9.1
     */
    public Surface setFullBrightLight() {
        this.lightMode = LightMode.FULL_BRIGHT;
        return this;
    }

    /**
     * Makes all elements on this surface sample block and sky light from the world each frame
     * at the surface's position, so they dim and brighten with their environment.
     *
     * @return self for chaining.
     * @since 1.9.1
     */
    public Surface setWorldLight() {
        this.lightMode = LightMode.WORLD;
        return this;
    }

    /**
     * Sets a fixed light level for all elements on this surface.
     *
     * @param blockLight block light level, 0–15 (e.g. 15 next to a torch)
     * @param skyLight   sky light level, 0–15 (e.g. 15 outdoors in daylight)
     * @return self for chaining.
     * @since 1.9.1
     */
    public Surface setLight(int blockLight, int skyLight) {
        this.lightMode = LightMode.CUSTOM;
        this.customLight = LightTexture.pack(blockLight, skyLight);
        return this;
    }

    @Override
    public void init() {
        recomputeScale();
        super.init();
    }

    private void recomputeScale() {
        scale = Math.min(sizes.x, sizes.y) / minSubdivisions;
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
    public void render(PoseStack matrices, MultiBufferSource consumers, float partialTicks) {
        boolean seeThrough = !this.cull;
        matrices.pushPose();

        boolean isTrackingEntity = boundEntity != null && boundEntity.isAlive();
        Pos3D renderPos = isTrackingEntity ? new Pos3D(boundEntity.getPos(partialTicks)) : pos;
        matrices.translate(renderPos.x, renderPos.y, renderPos.z);

        if (rotateToPlayer) {
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

        if (rotateCenter) {
            matrices.translate(sizes.x / 2, 0, 0);
            matrices.mulPose(new Quaternionf().rotateLocalY((float) Math.toRadians(rotations.y)));
            matrices.translate(-sizes.x / 2, 0, 0);
            matrices.translate(0, -sizes.y / 2, 0);
            matrices.mulPose(new Quaternionf().rotateLocalX((float) Math.toRadians(rotations.x)));
            matrices.translate(0, sizes.y / 2, 0);
            matrices.translate(sizes.x / 2, -sizes.y / 2, 0);
            matrices.mulPose(new Quaternionf().rotateLocalZ((float) Math.toRadians(rotations.z)));
            matrices.translate(-sizes.x / 2, sizes.y / 2, 0);
        } else {
            Quaternionf q = new Quaternionf();
            q.rotateLocalY((float) Math.toRadians(rotations.y));
            q.rotateLocalX((float) Math.toRadians(rotations.x));
            q.rotateLocalZ((float) Math.toRadians(rotations.z));
            matrices.mulPose(q);
        }
        // fix it so that y-axis goes down instead of up
        matrices.scale(1, -1, 1);
        // scale so that x or y have minSubdivisions units between them
        matrices.scale((float) scale, (float) scale, (float) scale);

        synchronized (elements) {
            renderElements3D(matrices,
                    consumers,
                    partialTicks,
                    resolveLightValue(renderPos.toRawBlockPos()),
                    seeThrough,
                    getElementsByZIndex());
        }
        matrices.popPose();
    }

    private int resolveLightValue() {
        return resolveLightValue(pos.toRawBlockPos());
    }

    private int resolveLightValue(BlockPos blockPos) {
        return switch (lightMode) {
            case FULL_BRIGHT -> 0xF000F0;
            case CUSTOM      -> customLight;
            case WORLD       -> {
                ClientLevel level = Minecraft.getInstance().level;
                if (level == null) yield 0xF000F0;
                int block = level.getBrightness(LightLayer.BLOCK, blockPos);
                int sky = level.getBrightness(LightLayer.SKY, blockPos);
                yield LightTexture.pack(block, sky);
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

    private void renderElements3D(PoseStack matrices, MultiBufferSource consumers, float delta, int light, boolean seeThrough, Iterator<RenderElement> iter) {
        while (iter.hasNext()) {
            RenderElement element = iter.next();
            // Render each draw2D element individually so that the cull and renderBack settings are used
            if (element instanceof Draw2DElement draw2DElement) {
                renderDraw2D3D(matrices, consumers, delta, light, seeThrough, draw2DElement);
            } else {
                renderElement3D(matrices, consumers, delta, light, seeThrough, element);
            }
        }
    }

    private void renderDraw2D3D(PoseStack matrices, MultiBufferSource consumers, float delta, int light, boolean seeThrough, Draw2DElement element) {
        matrices.pushPose();
        matrices.translate(element.x, element.y, 0);
        matrices.scale(element.scale, element.scale, 1);
        if (element.rotateCenter) {
            matrices.translate(element.getWidth() / 2d, element.getHeight() / 2d, 0);
        }
        matrices.mulPose(new Quaternionf().rotateLocalZ((float) Math.toRadians(element.rotation)));
        if (element.rotateCenter) {
            matrices.translate(-element.getWidth() / 2d, -element.getHeight() / 2d, 0);
        }
        // Don't translate back! Elements are rendered relative to the translated origin.
        Draw2D draw2D = element.getDraw2D();
        synchronized (draw2D.getElements()) {
            renderElements3D(matrices, consumers, delta, light, seeThrough, draw2D.getElementsByZIndex());
        }
        matrices.popPose();
    }

    private void renderElement3D(PoseStack matrices, MultiBufferSource consumers, float delta, int light, boolean seeThrough, RenderElement element) {
        matrices.pushPose();
        // The surface's scale transform has already been applied to the matrix stack, so a plain
        // zIndexScale * zIndex translation would be scaled down by `scale` (e.g. 0.01), causing
        // z-fighting.  Divide by scale to keep the world-space z-separation equal to
        // zIndexScale * zIndex regardless of the surface's pixel-to-block scale factor.
        matrices.translate(0, 0, (zIndexScale / scale) * element.getZIndex());
        element.render3D(matrices, consumers, light, seeThrough, delta);
        matrices.popPose();
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
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
         * Makes all elements on this surface render at full brightness, ignoring world lighting.
         * This is the default behaviour.
         *
         * @return self for chaining.
         * @since 1.9.1
         */
        public Builder fullBrightLight() {
            this.lightMode = LightMode.FULL_BRIGHT;
            return this;
        }

        /**
         * Makes all elements on this surface sample block and sky light from the world each frame
         * at the surface's position, so they dim and brighten with their environment.
         *
         * @return self for chaining.
         * @since 1.9.1
         */
        public Builder worldLight() {
            this.lightMode = LightMode.WORLD;
            return this;
        }

        /**
         * Sets a fixed light level for all elements on this surface.
         *
         * @param blockLight block light level, 0–15 (e.g. 15 next to a torch)
         * @param skyLight   sky light level, 0–15 (e.g. 15 outdoors in daylight)
         * @return self for chaining.
         * @since 1.9.1
         */
        public Builder light(int blockLight, int skyLight) {
            this.lightMode = LightMode.CUSTOM;
            this.customLight = LightTexture.pack(blockLight, skyLight);
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
