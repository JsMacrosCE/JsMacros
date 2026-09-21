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
