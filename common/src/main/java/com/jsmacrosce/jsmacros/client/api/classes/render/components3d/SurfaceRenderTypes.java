package com.jsmacrosce.jsmacros.client.api.classes.render.components3d;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.resources.ResourceLocation;

//? if >=26.2 {
/*import com.mojang.blaze3d.PrimitiveTopology;
import net.minecraft.client.renderer.BindGroupLayouts;
*///? } else {
import com.mojang.blaze3d.shaders.UniformType;
//? }

//? if >=26.1 {
/*import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.CompareOp;
*///? } else {
import com.mojang.blaze3d.platform.DepthTestFunction;
//? }

//? if >=1.21.11 {
/*import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
*///? } else {
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
//? }

import java.util.HashMap;
import java.util.Map;

/**
 * Provides custom render types used by the surface renderer for colored quads,
 * lines, and textured quads. The quad and image pipelines are used by the 2D
 * elements; the line pipelines are constructed lazily if requested.
 *
 * @since 2.0.0
 */
public final class SurfaceRenderTypes {

    private SurfaceRenderTypes() {
    }

    private static RenderType quadsCullDepth;
    private static RenderType quadsCullNoDepth;
    private static RenderType quadsNoCullDepth;
    private static RenderType quadsNoCullNoDepth;
    private static RenderType linesDepth;
    private static RenderType linesNoDepth;
    private static final Map<ResourceLocation, RenderType> imageDepth = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> imageNoDepth = new HashMap<>();

    // Colored quad geometry (rects), with independent back-face culling and depth testing.
    /**
     * Returns the cached render type for colored quads with the requested culling
     * and depth-test settings. Each of the four combinations is created on first use;
     * depth testing follows the game's depth convention when enabled and is disabled otherwise.
     *
     * @param cull whether back faces are discarded
     * @param depthTest whether the geometry is depth tested against what is already drawn
     * @return the cached render type for that combination, built on the first call for it
     * @since 2.0.0
     */
    public static RenderType quads(boolean cull, boolean depthTest) {
        if (cull) {
            if (depthTest) {
                if (quadsCullDepth == null) {
                    quadsCullDepth = createQuads(true, true);
                }
                return quadsCullDepth;
            }
            if (quadsCullNoDepth == null) {
                quadsCullNoDepth = createQuads(true, false);
            }
            return quadsCullNoDepth;
        }
        if (depthTest) {
            if (quadsNoCullDepth == null) {
                quadsNoCullDepth = createQuads(false, true);
            }
            return quadsNoCullDepth;
        }
        if (quadsNoCullNoDepth == null) {
            quadsNoCullNoDepth = createQuads(false, false);
        }
        return quadsNoCullNoDepth;
    }

    // Line geometry. cull is meaningless for lines; only depth testing varies.
    /**
     * Returns the cached render type for line geometry, creating it on first use.
     *
     * @param depthTest whether the geometry is depth tested against what is already drawn
     * @return the cached render type for that setting, built on the first call for it
     * @since 2.0.0
     */
    public static RenderType lines(boolean depthTest) {
        if (depthTest) {
            if (linesDepth == null) {
                linesDepth = createLines(true);
            }
            return linesDepth;
        }
        if (linesNoDepth == null) {
            linesNoDepth = createLines(false);
        }
        return linesNoDepth;
    }

    // Textured (entity-shader) quads for images, cached per texture.
    /**
     * Returns the cached render type for a textured quad. Pipelines are cached by
     * texture and depth-test setting; the texture is bound into the pipeline.
     *
     * @param texture the texture to bind into the pipeline
     * @param depthTest whether the geometry is depth tested against what is already drawn
     * @return the cached render type, created on the first call for this texture and setting
     * @since 2.0.0
     */
    public static RenderType images(ResourceLocation texture, boolean depthTest) {
        Map<ResourceLocation, RenderType> cache = depthTest ? imageDepth : imageNoDepth;
        RenderType type = cache.get(texture);
        if (type == null) {
            type = createImage(texture, depthTest);
            cache.put(texture, type);
        }
        return type;
    }

    private static RenderType createQuads(boolean cull, boolean depthTest) {
        String name = "jsmacrosce_surface_quads_" + (cull ? "cull" : "nocull") + (depthTest ? "" : "_nodepth");

        RenderPipeline.Builder builder = basePipeline(name, "core/position_color")
                .withCull(cull);
        setVertexLayout(builder, DefaultVertexFormat.POSITION_COLOR, false);

        return untexturedRenderType(name, finishPipeline(builder, depthTest));
    }

    private static RenderType createLines(boolean depthTest) {
        String name = "jsmacrosce_surface_lines" + (depthTest ? "" : "_nodepth");

        RenderPipeline.Builder builder = basePipeline(name, "core/rendertype_lines")
                .withCull(false);
        addFog(builder);

        // 26.3 supplies Globals in basePipeline for every shader.
        //? if >=26.2 <26.3 {
        /*builder.withBindGroupLayout(BindGroupLayouts.GLOBALS);
        *///? } else if >1.21.5 <26.2 {
        builder.withUniform("Globals", UniformType.UNIFORM_BUFFER);
        //? } else if <=1.21.5 {
        /*builder.withUniform("LineWidth", UniformType.FLOAT)
                .withUniform("ScreenSize", UniformType.VEC2);
        *///? }

        //? if >=1.21.11 {
        /*setVertexLayout(builder, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, true);
        *///? } else {
        setVertexLayout(builder, DefaultVertexFormat.POSITION_COLOR_NORMAL, true);
        //? }

        return untexturedRenderType(name, finishPipeline(builder, depthTest));
    }

    private static RenderType createImage(ResourceLocation texture, boolean depthTest) {
        String name = "jsmacrosce_surface_image" + (depthTest ? "" : "_nodepth");

        RenderPipeline.Builder builder = basePipeline(name, "core/entity")
                .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                .withShaderDefine("PER_FACE_LIGHTING")
                .withCull(false);
        addFog(builder);

        //? if >=26.2 {
        /*builder.withBindGroupLayout(BindGroupLayouts.LIGHTING)
                .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER1_SAMPLER2);
        *///? } else {
        builder.withSampler("Sampler0").withSampler("Sampler1").withSampler("Sampler2");
        //? }

        //? if >1.21.5 <26.2 {
        builder.withUniform("Lighting", UniformType.UNIFORM_BUFFER);
        //? } else if <=1.21.5 {
        /*builder.withUniform("Light0_Direction", UniformType.VEC3)
                .withUniform("Light1_Direction", UniformType.VEC3);
        *///? }

        //? if >=26.1 {
        /*setVertexLayout(builder, DefaultVertexFormat.ENTITY, false);
        *///? } else {
        setVertexLayout(builder, DefaultVertexFormat.NEW_ENTITY, false);
        //? }

        //? if >=1.21.11 {
        /*return RenderType.create(
                name,
                RenderSetup.builder(finishPipeline(builder, depthTest))
                        .withTexture("Sampler0", texture)
                        .useLightmap()
                        .useOverlay()
                        .createRenderSetup()
        );
        *///? } else {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setTextureState(surfaceTextureState(texture))
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .createCompositeState(false);

        return RenderType.create(
                name,
                1536,
                finishPipeline(builder, depthTest),
                state
        );
        //? }
    }

    //? if <=1.21.5 {
    /*private static RenderStateShard.TextureStateShard surfaceTextureState(ResourceLocation texture) {
        return new RenderStateShard.TextureStateShard(texture, net.minecraft.util.TriState.FALSE, false);
    }
    *///? } else if <=1.21.10 {
    private static RenderStateShard.TextureStateShard surfaceTextureState(ResourceLocation texture) {
        return new RenderStateShard.TextureStateShard(texture, false);
    }
    //? }

    // 1.21.5 uses individual uniforms; later targets use UBOs, then bind groups in 26.2.
    private static RenderPipeline.Builder basePipeline(String name, String shader) {
        RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation("pipeline/jsmacrosce/" + name)
                .withVertexShader(shader)
                .withFragmentShader(shader);
        //? if >=26.3 {
        /*builder.withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                .withBindGroupLayout(BindGroupLayouts.PROJECTION)
                .withBindGroupLayout(BindGroupLayouts.GLOBALS);
        *///? } else if >=26.2 {
        /*builder.withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION);
        *///? } else if >1.21.5 {
        builder.withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                .withUniform("Projection", UniformType.UNIFORM_BUFFER);
        //? } else {
        /*builder.withUniform("ModelViewMat", UniformType.MATRIX4X4)
                .withUniform("ProjMat", UniformType.MATRIX4X4)
                .withUniform("ColorModulator", UniformType.VEC4);
        *///? }
        return builder;
    }

    private static void addFog(RenderPipeline.Builder builder) {
        //? if >=26.2 {
        /*builder.withBindGroupLayout(BindGroupLayouts.FOG);
        *///? } else if >1.21.5 {
        builder.withUniform("Fog", UniformType.UNIFORM_BUFFER);
        //? } else {
        /*builder.withUniform("FogStart", UniformType.FLOAT)
                .withUniform("FogEnd", UniformType.FLOAT)
                .withUniform("FogShape", UniformType.INT)
                .withUniform("FogColor", UniformType.VEC4);
        *///? }
    }

    private static void setVertexLayout(RenderPipeline.Builder builder, VertexFormat format, boolean lines) {
        //? if >=26.2 {
        /*builder.withVertexBinding(0, format)
                .withPrimitiveTopology(lines ? PrimitiveTopology.LINES : PrimitiveTopology.QUADS);
        *///? } else {
        builder.withVertexFormat(format, lines ? VertexFormat.Mode.LINES : VertexFormat.Mode.QUADS);
        //? }
    }

    private static RenderType untexturedRenderType(String name, RenderPipeline pipeline) {
        //? if >=1.21.11 {
        /*return RenderType.create(name, RenderSetup.builder(pipeline).createRenderSetup());
        *///? } else {
        return RenderType.create(name, 1536, pipeline, RenderType.CompositeState.builder().createCompositeState(false));
        //? }
    }

    // Preserve legacy depth writes; 26.1+ overlays already disable them explicitly.
    private static RenderPipeline finishPipeline(RenderPipeline.Builder builder, boolean depthTest) {
        //? if >=26.2 {
        /*return builder
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                // 26.2 switched to reversed-Z
                .withDepthStencilState(new DepthStencilState(
                        depthTest ? CompareOp.GREATER_THAN_OR_EQUAL : CompareOp.ALWAYS_PASS, false))
                .build();
        *///? } else if >=26.1 {
        /*return builder
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(
                        depthTest ? CompareOp.LESS_THAN_OR_EQUAL : CompareOp.ALWAYS_PASS, false))
                .build();
        *///? } else {
        return builder
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(depthTest ? DepthTestFunction.LEQUAL_DEPTH_TEST : DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(true)
                .build();
        //? }
    }
}
