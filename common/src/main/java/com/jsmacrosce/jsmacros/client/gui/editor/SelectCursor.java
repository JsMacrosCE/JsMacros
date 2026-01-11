package com.jsmacrosce.jsmacros.client.gui.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

public class SelectCursor {
    private final Minecraft mc = Minecraft.getInstance();
    public Consumer<SelectCursor> onChange;
    public Style defaultStyle;
    public int startLine = 0;
    public int endLine = 0;

    public int startIndex = 0;
    public int endIndex = 0;

    public int startLineIndex = 0;
    public int endLineIndex = 0;

    public int dragStartIndex = 0;

    public int arrowLineIndex = 0;
    public boolean arrowEnd = false;

    public int startCol = 0;
    public int endCol = 0;

    public SelectCursor(Style defaultStyle) {
        this.defaultStyle = defaultStyle;
    }

    public synchronized void updateStartIndex(int startIndex, String current) {
        this.startIndex = Mth.clamp(startIndex, 0, current.length());
        String[] prev = current.substring(0, this.startIndex).split("\n", -1);
        startLine = prev.length - 1;
        startCol = mc.font.width(Component.literal(prev[startLine]).setStyle(defaultStyle)) - 1;
        startLineIndex = prev[startLine].length();
        if (onChange != null) {
            onChange.accept(this);
        }
    }

    public synchronized void updateEndIndex(int endIndex, String current) {
        this.endIndex = Mth.clamp(endIndex, 0, current.length());
        String[] prev = current.substring(0, this.endIndex).split("\n", -1);
        endLine = prev.length - 1;
        endCol = mc.font.width(Component.literal(prev[endLine]).setStyle(defaultStyle));
        endLineIndex = prev[endLine].length();
        if (onChange != null) {
            onChange.accept(this);
        }
    }

}
