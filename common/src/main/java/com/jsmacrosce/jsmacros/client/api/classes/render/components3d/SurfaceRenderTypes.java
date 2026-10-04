package com.jsmacrosce.jsmacros.client.api.classes.render.components3d;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.ResourceLocation;

//? if >=26.1 {
/*import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
*///? } else if >=1.21.11 {
/*import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
*///? } else {
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
//? }

import java.util.HashMap;
import java.util.Map;

/**
 * Custom surface render pipelines.
 * <p>
 * Owning the pipeline means culling and depth testing are explicit instead of
 * relying on whichever stock render type happens to have the flags we want (and
 * instead of reflecting into {@code RenderPipelines.*.depthTestFunction}). Uses the
 * backend-agnostic pipeline API so it works on OpenGL and Vulkan alike.
 * <p>
 * Two render-type generations are supported: {@code RenderType.create(name, RenderSetup)}
 * on 1.21.11+, and the older {@code CompositeState} form below that.
 * <p>
 * All three of the methods here are for JsMacros' own 2D elements to draw with. A
 * {@code Rect} and a {@code Line} both take {@link #quads}, an {@code Image} takes
 * {@link #images}, and nothing in the tree currently calls {@link #lines}, so the
 * line pipelines are never built at all: each one is constructed inside the method,
 * so with no caller neither field is ever filled. A script has no reason to
 * reach for this class: it is a piece of the surface renderer rather than something
 * on the scripting surface, and it is not part of the shipped type definitions for
 * that reason.
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

    // Colored quad geometry (rects). Depth writes are off so painter's order
    // within a surface is preserved. cull: back-face culling; depthTest: depth test.
    /**
     * the pipeline for flat coloured geometry, in one of four fixed combinations.
     * <p>
     * The two flags are independent and all four combinations are cached, so calling
     * this repeatedly with the same pair builds the pipeline once and hands back the
     * same object every time after that. The cache is four static fields rather than a
     * map, because there are only four combinations.
     * <p>
     * The {@code cull} flag is back-face culling, which for a surface drawn as a flat
     * panel decides whether the back of it is thrown away. The {@code depthTest} flag
     * is depth testing, and is set to less-or-equal when on and to no depth test at all
     * when off; both are the depth test only, neither changes what the geometry is.
     * <p>
     * The 2D elements in this package ask for this with culling off and with
     * {@code depthTest} set to whether the surface they are on is depth tested at all,
     * so of the four combinations the two culled ones are never reached and, being
     * built lazily inside this method, never built.
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
     * the pipeline for line geometry, in one of two fixed combinations.
     * <p>
     * The two are cached in static fields, so the pipeline is built on the first call
     * for a setting and the same object is handed back after that. There is no
     * {@code cull} argument because back-face culling has no meaning for a line, which
     * has no inside.
     * <p>
     * Nothing in the tree calls this at the moment. The 2D line element draws itself
     * as a quad through {@link #quads}, so with no caller neither of these two
     * pipelines is ever built, and they are kept for the version of the line element
     * that uses them.
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
     * the pipeline for a textured quad, cached per texture rather than per setting.
     * <p>
     * This one is keyed on the texture as well as on the depth test, because the
     * texture is bound into the pipeline rather than passed to it at draw time. The two
     * caches are plain maps that are never cleared, so every distinct texture asked
     * for keeps its pipeline for the rest of the session and a script that draws an
     * unbounded number of different textures grows them without bound.
     * <p>
     * The pipeline takes the entity snippet and adds an alpha cutout at 0.1, per-face
     * lighting, and a second sampler; culling is off, and the setup binds the texture
     * to {@code Sampler0} and asks for the lightmap and overlay. Depth testing is the
     * only thing the flag here chooses.
     *
     * @param texture the texture to bind into the pipeline
     * @param depthTest whether the geometry is depth tested against what is already drawn
     * @return the render type for that texture and setting, built on the first call for
     *         the pair and the same object on every call after that
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

        //? if >=1.21.11 {
        /*RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                .withVertexShader("core/position_color")
                .withFragmentShader("core/position_color")
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS);
        *///? } else {
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET);
        //? }

        builder
                .withLocation("pipeline/jsmacrosce/" + name)
                .withCull(cull);

        //? if >=1.21.11 {
        /*return RenderType.create(
                name,
                RenderSetup.builder(finishPipeline(builder, depthTest)).createRenderSetup()
        );
        *///? } else {
        return RenderType.create(
                name,
                1536,
                finishPipeline(builder, depthTest),
                RenderType.CompositeState.builder().createCompositeState(false)
        );
        //? }
    }

    private static RenderType createLines(boolean depthTest) {
        String name = "jsmacrosce_surface_lines" + (depthTest ? "" : "_nodepth");

        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                .withLocation("pipeline/jsmacrosce/" + name);

        //? if >=1.21.11 {
        /*return RenderType.create(
                name,
                RenderSetup.builder(finishPipeline(builder, depthTest)).createRenderSetup()
        );
        *///? } else {
        return RenderType.create(
                name,
                1536,
                finishPipeline(builder, depthTest),
                RenderType.CompositeState.builder().createCompositeState(false)
        );
        //? }
    }

    private static RenderType createImage(ResourceLocation texture, boolean depthTest) {
        String name = "jsmacrosce_surface_image" + (depthTest ? "" : "_nodepth");

        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                .withLocation("pipeline/jsmacrosce/" + name)
                .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                .withShaderDefine("PER_FACE_LIGHTING")
                .withSampler("Sampler1")
                .withCull(false);

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

    private static RenderPipeline finishPipeline(RenderPipeline.Builder builder, boolean depthTest) {
        //? if >=26.1 {
        /*return builder
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(
                        depthTest ? CompareOp.LESS_THAN_OR_EQUAL : CompareOp.ALWAYS_PASS, false))
                .build();
        *///? } else {
        return builder
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(depthTest ? DepthTestFunction.LEQUAL_DEPTH_TEST : DepthTestFunction.NO_DEPTH_TEST)
                .build();
        //? }
    }
}
