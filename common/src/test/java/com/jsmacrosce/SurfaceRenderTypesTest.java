package com.jsmacrosce;

import com.jsmacrosce.jsmacros.client.api.classes.render.components3d.SurfaceRenderTypes;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.ResourceLocation;
//? if >=26.3 {
/*import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.api.pipeline.UniformType;
*///?}

//? if >=26.2 {
/*import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
*///?}
//? if >=26.1 {
/*import com.mojang.blaze3d.platform.CompareOp;
*///?} else {
import com.mojang.blaze3d.platform.DepthTestFunction;
//?}
//? if >=1.21.11 {
/*import net.minecraft.client.renderer.rendertype.RenderType;
*///?} else {
import net.minecraft.client.renderer.RenderType;
//?}

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Configuration checks only: no rendering device, client, or scripts are started.
public final class SurfaceRenderTypesTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        ResourceLocation texture = ResourceLocation.withDefaultNamespace("textures/item/diamond.png");
        for (boolean depth : new boolean[]{false, true}) {
            for (boolean cull : new boolean[]{false, true}) {
                RenderType quads = SurfaceRenderTypes.quads(cull, depth);
                check(quads == SurfaceRenderTypes.quads(cull, depth), "quad cache identity");
                configuration(pipeline(quads), "position_color", cull, depth, DefaultVertexFormat.POSITION_COLOR);
            }
            RenderType lines = SurfaceRenderTypes.lines(depth);
            check(lines == SurfaceRenderTypes.lines(depth), "line cache identity");
            //? if >=1.21.11 {
            /*VertexFormat lineFormat = DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH;
            *///?} else {
            VertexFormat lineFormat = DefaultVertexFormat.POSITION_COLOR_NORMAL;
            //?}
            configuration(pipeline(lines), "rendertype_lines", false, depth, lineFormat);

            RenderType image = SurfaceRenderTypes.images(texture, depth);
            check(image == SurfaceRenderTypes.images(texture, depth), "image cache identity");
            //? if >=26.1 {
            /*VertexFormat imageFormat = DefaultVertexFormat.ENTITY;
            *///?} else {
            VertexFormat imageFormat = DefaultVertexFormat.NEW_ENTITY;
            //?}
            configuration(pipeline(image), "entity", false, depth, imageFormat);
            check(image != SurfaceRenderTypes.images(ResourceLocation.withDefaultNamespace("textures/item/apple.png"), depth),
                    "textures have distinct render types");
        }
        check(SurfaceRenderTypes.images(texture, false) != SurfaceRenderTypes.images(texture, true), "image depth caches differ");
        check(SurfaceRenderTypes.lines(false) != SurfaceRenderTypes.lines(true), "line depth caches differ");
        System.out.println("SurfaceRenderTypes: 8 configurations, " + checks + " checks passed");
    }

    private static void configuration(RenderPipeline pipeline, String shader, boolean cull, boolean depth, VertexFormat format) {
        boolean lines = shader.equals("rendertype_lines");
        boolean image = shader.equals("entity");
        //? if >=26.3 {
        /*check(pipeline.getShaders().get(ShaderType.VERTEX).toString().equals("minecraft:core/" + shader), "vertex shader");
        check(pipeline.getShaders().get(ShaderType.FRAGMENT).equals(pipeline.getShaders().get(ShaderType.VERTEX)), "fragment shader");
        *///?} else {
        check(pipeline.getVertexShader().toString().equals("minecraft:core/" + shader), "vertex shader");
        check(pipeline.getFragmentShader().equals(pipeline.getVertexShader()), "fragment shader");
        //?}
        check(pipeline.isCull() == cull, "explicit face culling");
        //? if >=26.3 {
        /*check(pipeline.getColorTargetStates().size() == 1, "one overlay color target");
        check(pipeline.getColorTargetStates().getFirst().blendFunction().orElseThrow().equals(BlendFunction.TRANSLUCENT), "translucent blend");
        check(pipeline.getColorTargetStates().getFirst().writeMask() == 15, "all color channels written");
        check(pipeline.getDepthStencilState().depthTest() == expectedDepthTest(depth), "depth comparison");
        check(!pipeline.getDepthStencilState().writeDepth(), "modern overlay depth writes remain disabled");
        *///?} else if >=26.1 {
        /*check(pipeline.getColorTargetState().blendFunction().orElseThrow().equals(BlendFunction.TRANSLUCENT), "translucent blend");
        check(pipeline.getColorTargetState().writeMask() == 15, "all color channels written");
        check(pipeline.getDepthStencilState().depthTest() == expectedDepthTest(depth), "depth comparison");
        check(!pipeline.getDepthStencilState().writeDepth(), "modern overlay depth writes remain disabled");
        *///?} else {
        check(pipeline.getBlendFunction().orElseThrow().equals(BlendFunction.TRANSLUCENT), "translucent blend");
        check(pipeline.isWriteColor() && pipeline.isWriteAlpha(), "all color channels written");
        check(pipeline.getDepthTestFunction() == (depth ? DepthTestFunction.LEQUAL_DEPTH_TEST : DepthTestFunction.NO_DEPTH_TEST), "depth comparison");
        check(pipeline.isWriteDepth(), "legacy overlay depth writes preserved");
        //?}
        //? if >=26.2 {
        /*check(pipeline.getVertexFormatBinding(0) == format, "vertex format");
        check(pipeline.getPrimitiveTopology() == (lines ? PrimitiveTopology.LINES : PrimitiveTopology.QUADS), "primitive topology");
        BindGroupLayout.ensureCompatible(pipeline.getBindGroupLayouts());
        *///?} else {
        check(pipeline.getVertexFormat() == format, "vertex format");
        check(pipeline.getVertexFormatMode() == (lines ? VertexFormat.Mode.LINES : VertexFormat.Mode.QUADS), "primitive topology");
        //?}

        Map<String, String> expected = new HashMap<>();
        //? if >1.21.5 {
        expected.put("DynamicTransforms", "UNIFORM_BUFFER");
        expected.put("Projection", "UNIFORM_BUFFER");
        if (lines || image) expected.put("Fog", "UNIFORM_BUFFER");
        if (lines) expected.put("Globals", "UNIFORM_BUFFER");
        if (image) expected.put("Lighting", "UNIFORM_BUFFER");
        //?} else {
        /*expected.put("ModelViewMat", "MATRIX4X4");
        expected.put("ProjMat", "MATRIX4X4");
        expected.put("ColorModulator", "VEC4");
        if (lines || image) {
            expected.put("FogStart", "FLOAT");
            expected.put("FogEnd", "FLOAT");
            expected.put("FogShape", "INT");
            expected.put("FogColor", "VEC4");
        }
        if (lines) {
            expected.put("LineWidth", "FLOAT");
            expected.put("ScreenSize", "VEC2");
        }
        if (image) {
            expected.put("Light0_Direction", "VEC3");
            expected.put("Light1_Direction", "VEC3");
        }
        *///?}

        //? if >=26.3 {
        /*expected.put("Globals", "UNIFORM_BUFFER");
        *///?}

        Map<String, String> actual = new HashMap<>();
        //? if >=26.3 {
        /*var bindings = BindGroupLayout.flattenUniforms(pipeline.getBindGroupLayouts());
        var uniforms = bindings.stream().filter(uniform -> uniform.type() != UniformType.COMBINED_IMAGE_SAMPLER).toList();
        List<String> samplers = bindings.stream().filter(uniform -> uniform.type() == UniformType.COMBINED_IMAGE_SAMPLER)
                .map(BindGroupLayout.UniformDescription::name).toList();
        *///?} else if >=26.2 {
        /*var uniforms = BindGroupLayout.flattenUniforms(pipeline.getBindGroupLayouts());
        List<String> samplers = BindGroupLayout.flattenSamplers(pipeline.getBindGroupLayouts());
        *///?} else {
        var uniforms = pipeline.getUniforms();
        List<String> samplers = pipeline.getSamplers();
        //?}
        for (var uniform : uniforms) actual.put(uniform.name(), uniform.type().name());
        check(actual.size() == uniforms.size(), "no duplicate uniform declarations");
        check(actual.equals(expected), "complete shader uniform names and types: " + actual + " expected " + expected);
        //? if >=26.3 {
        /*check(Set.copyOf(samplers).equals(image ? Set.of("Sampler0", "Sampler2") : Set.of()), "shader sampler declarations");
        check(samplers.size() == (image ? 2 : 0), "no duplicate samplers");
        *///?} else {
        check(Set.copyOf(samplers).equals(image ? Set.of("Sampler0", "Sampler1", "Sampler2") : Set.of()), "shader sampler declarations");
        check(samplers.size() == (image ? 3 : 0), "no duplicate samplers");
        //?}
        if (image) {
            check(Float.parseFloat(pipeline.getShaderDefines().values().get("ALPHA_CUTOUT")) == 0.1F, "image alpha cutoff");
            check(pipeline.getShaderDefines().flags().equals(Set.of("PER_FACE_LIGHTING")), "image lighting define");
        } else {
            check(pipeline.getShaderDefines().isEmpty(), "untextured shader defines");
        }
    }

    // 26.2 switched to reversed-Z, so the comparison is the opposite of 26.1's.
    //? if >=26.2 {
    /*private static CompareOp expectedDepthTest(boolean depth) {
        return depth ? CompareOp.GREATER_THAN_OR_EQUAL : CompareOp.ALWAYS_PASS;
    }
    *///? } else if >=26.1 {
    /*private static CompareOp expectedDepthTest(boolean depth) {
        return depth ? CompareOp.LESS_THAN_OR_EQUAL : CompareOp.ALWAYS_PASS;
    }
    *///? }

    private static RenderPipeline pipeline(RenderType type) throws Exception {
        //? if >=1.21.11 {
        /*return type.pipeline();
        *///?} else {
        Field field = type.getClass().getDeclaredField("renderPipeline");
        field.setAccessible(true);
        return (RenderPipeline) field.get(type);
        //?}
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
