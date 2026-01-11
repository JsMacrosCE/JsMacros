package com.jsmacrosce.jsmacros.client.gui.editor.highlighting.impl;

import io.noties.prism4j.AbsVisitor;
import io.noties.prism4j.Prism4j;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TextStyleCompiler extends AbsVisitor {
    private final Style defaultStyle;
    private final Map<String, short[]> themeData;
    private final List<MutableComponent> result = new LinkedList<>();

    public TextStyleCompiler(Style defaultStyle, Map<String, short[]> themeData) {
        this.defaultStyle = defaultStyle;
        this.themeData = themeData;
        result.add((MutableComponent) Component.literal("").setStyle(defaultStyle));
    }

    @Override
    protected void visitText(@NotNull Prism4j.Text text) {
        String[] lines = text.literal().replaceAll("\t", "    ").split("\r\n|\n", -1);
        int i = 0;
        while (i < lines.length) {
            result.get(result.size() - 1).append(Component.literal(lines[i]).setStyle(defaultStyle));
            if (++i < lines.length) {
                result.add((MutableComponent) Component.literal("").setStyle(defaultStyle));
            }
        }
    }

    @Override
    protected void visitSyntax(@NotNull Prism4j.Syntax syntax) {
        TextColor update = colorForSyntax(syntax.type(), syntax.alias());
        Style newStyle = update == null ? defaultStyle : defaultStyle.withColor(update);
        final TextStyleCompiler child = new TextStyleCompiler(newStyle, themeData);
        child.visit(syntax.children());
        appendChildResult(child.getResult());
    }

    protected void appendChildResult(List<MutableComponent> childResult) {
        MutableComponent first = childResult.remove(0);
        result.get(result.size() - 1).append(first);
        result.addAll(childResult);
    }

    protected TextColor colorForSyntax(String name, String alias) {
        TextColor val = getColorForToken(name);
        if (val != null) {
            return val;
        } else {
            val = getColorForToken(alias);
        }
        return val;
    }

    public List<MutableComponent> getResult() {
        return result;
    }

    protected TextColor getColorForToken(String name) {
        if (!themeData.containsKey(name)) {
            return null;
        }
        short[] color = themeData.get(name);
        return TextColor.fromRgb((color[0] & 255) << 16 | (color[1] & 255) << 8 | (color[2] & 255));
    }

}
