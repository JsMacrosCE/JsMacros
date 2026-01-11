package com.jsmacrosce.jsmacros.client.gui.screens;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.config.ClientConfigV2;
import com.jsmacrosce.jsmacros.client.gui.editor.History;
import com.jsmacrosce.jsmacros.client.gui.editor.SelectCursor;
import com.jsmacrosce.jsmacros.client.gui.editor.highlighting.AbstractRenderCodeCompiler;
import com.jsmacrosce.jsmacros.client.gui.editor.highlighting.AutoCompleteSuggestion;
import com.jsmacrosce.jsmacros.client.gui.editor.highlighting.impl.DefaultCodeCompiler;
import com.jsmacrosce.jsmacros.client.gui.editor.highlighting.impl.NoStyleCodeCompiler;
import com.jsmacrosce.jsmacros.client.gui.editor.highlighting.scriptimpl.ScriptCodeCompiler;
import com.jsmacrosce.jsmacros.client.gui.settings.SettingsOverlay;
import com.jsmacrosce.jsmacros.core.library.impl.classes.FileHandler;
import com.jsmacrosce.wagyourgui.BaseScreen;
import com.jsmacrosce.wagyourgui.elements.Button;
import com.jsmacrosce.wagyourgui.elements.Scrollbar;
import com.jsmacrosce.wagyourgui.overlays.ConfirmOverlay;
import com.jsmacrosce.wagyourgui.overlays.SelectorDropdownOverlay;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

//? if >1.21.8 {
/*import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.FontDescription;
*///?}

public class EditorScreen extends BaseScreen {
    private static final FormattedCharSequence ellipses = Component.literal("...").withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText();
    public static final List<String> langs = Lists.newArrayList(
            "javascript",
            "lua",
            "python",
            "clike",
            "regex",
            "json",
            "ruby",
            "typescript",
            "groovy",
            "kotlin",
            "none"
    );
    // TODO: Why in the everloving fuck would we be changing this multiple times in here. Mistake?
    //? if >1.21.8 {
    /*public static FontDescription.Resource defaultFontResource = new FontDescription.Resource(ResourceLocation.fromNamespaceAndPath("jsmacrosce", "ubuntumono"));
    *///?} else {
    public static ResourceLocation defaultFontResource = ResourceLocation.fromNamespaceAndPath("jsmacrosce", "ubuntumono");
    //?}
    public static Style defaultStyle = Style.EMPTY.withFont(defaultFontResource);
    protected final File file;
    protected final FileHandler handler;
    public final History history;
    public final SelectCursor cursor;
    private int ellipsesWidth;
    protected String savedString;
    protected Component fileName = Component.literal("");
    protected String lineCol = "";
    protected Scrollbar scrollbar;
    protected Button saveBtn;
    protected int scroll = 0;
    protected int lineSpread = 0;
    protected int firstLine = 0;
    protected int lastLine;
    public boolean blockFirst = false;
    public long textRenderTime = 0;
    public char prevChar = '\0';
    public String language;
    public AbstractRenderCodeCompiler codeCompiler;

    public EditorScreen(Screen parent, @NotNull File file) {
        super(Component.literal("Editor"), parent);
        this.file = file;
        FileHandler handler = new FileHandler(file);
        String content;
        if (file.exists()) {
            try {
                content = handler.read();
            } catch (IOException e) {
                content = I18n.get("jsmacrosce.erroropening") + e.toString();
                handler = null;
            }
        } else {
            content = "";
        }
        savedString = content;

        this.handler = handler;
        //? if >1.21.8 {
        /*defaultFontResource = new FontDescription.Resource((ResourceLocation.parse(JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).editorFont)));
        *///?} else {
        defaultFontResource = ResourceLocation.parse(JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).editorFont);
        //?}
        defaultStyle = Style.EMPTY.withFont(defaultFontResource);

        cursor = new SelectCursor(defaultStyle);

        this.history = new History(content.replaceAll("\r\n", "\n").replaceAll("\t", "    "), cursor);

        cursor.updateStartIndex(0, history.current);
        cursor.updateEndIndex(0, history.current);
        cursor.dragStartIndex = 0;
    }

    public String getDefaultLanguage() {
        final String[] fname = file.getName().split("\\.", -1);
        String ext = fname[fname.length - 1].toLowerCase(Locale.ROOT);

        switch (ext) {
            case "py":
                return "python";
            case "lua":
                return "lua";
            case "json":
                return "json";
            case "rb":
                return "ruby";
            case "kts":
                return "kotlin";
            case "groovy":
                return "groovy";
            case "ts":
                return "typescript";
            default:
                return "javascript";
        }
    }

    public static void openAndScrollToIndex(@NotNull File file, int startIndex, int endIndex) {
        Minecraft mc = Minecraft.getInstance();
        int finalEndIndex = endIndex == -1 ? startIndex : endIndex;
        mc.execute(() -> {
            EditorScreen screen;
            try {
                if (JsMacrosClient.prevScreen instanceof EditorScreen &&
                        ((EditorScreen) JsMacrosClient.prevScreen).file.getCanonicalPath().equals(file.getCanonicalPath())) {
                    screen = (EditorScreen) JsMacrosClient.prevScreen;
                } else {
                    screen = new EditorScreen(JsMacrosClient.prevScreen, file);
                }
                screen.cursor.updateStartIndex(startIndex, screen.history.current);
                screen.cursor.updateEndIndex(finalEndIndex, screen.history.current);
                mc.setScreen(screen);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void openAndScrollToLine(@NotNull File file, int line, int col, int endCol) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            EditorScreen screen;
            try {
                if (JsMacrosClient.prevScreen instanceof EditorScreen &&
                        ((EditorScreen) JsMacrosClient.prevScreen).file.getCanonicalPath().equals(file.getCanonicalPath())) {
                    screen = (EditorScreen) JsMacrosClient.prevScreen;
                } else {
                    screen = new EditorScreen(JsMacrosClient.prevScreen, file);
                }
                String[] lines = screen.history.current.split("\n", -1);
                int lineIndex = 0;
                int max = Math.min(lines.length - 1, line - 1);
                for (int i = 0; i < max; ++i) {
                    lineIndex += lines[i].length() + 1;
                }
                int startIndex;
                if (col == -1) {
                    startIndex = lineIndex;
                } else {
                    startIndex = lineIndex + Math.min(lines[max].length(), col);
                }
                screen.cursor.updateStartIndex(startIndex, screen.history.current);
                if (endCol == -1) {
                    startIndex = lineIndex + lines[max].length();
                } else {
                    startIndex = lineIndex + Math.min(lines[max].length(), endCol);
                }
                screen.cursor.updateEndIndex(startIndex, screen.history.current);
                mc.setScreen(screen);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void setScroll(double pages) {
        scroll = (int) ((height - 24) * pages);
        int add = lineSpread - scroll % lineSpread;
        if (add == lineSpread) {
            add = 0;
        }
        firstLine = (int) Math.ceil(scroll / (double) lineSpread);
        lastLine = (int) (firstLine + (height - 24 - add) / (double) lineSpread) - 1;
    }

    public synchronized void setLanguage(String language) {
        this.language = language;
        Map<String, String> linterOverrides = JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).editorLinterOverrides;
        if (!language.equals("none")) {
            if (linterOverrides.containsKey(language)) {
                this.codeCompiler = new ScriptCodeCompiler(
                        language,
                        this,
                        JsMacrosClient.clientCore.config.macroFolder.getAbsoluteFile()
                                .toPath()
                                .resolve(linterOverrides.get(language))
                                .toFile()
                );
            } else {
                this.codeCompiler = new DefaultCodeCompiler(language, this);
            }
        } else {
            this.codeCompiler = new NoStyleCodeCompiler(null, this);
        }
        compileRenderedText();
    }

    @Override
    public void init() {
        super.init();
        assert minecraft != null;

        ellipsesWidth = minecraft.font.width(ellipses);
        lineSpread = minecraft.font.lineHeight + 1;
        int width = this.width - 10;

        scrollbar = addRenderableWidget(new Scrollbar(width, 12, 10, height - 24, 0, 0xFF000000, 0xFFFFFFFF, 1, this::setScroll));
        saveBtn = this.addRenderableWidget(new Button(width / 2, 0, width / 6, 12, font, needSave() ? 0xFFA0A000 : 0xFF00A000, 0xFF000000, needSave() ? 0xFF707000 : 0xFF007000, 0xFFFFFFFF, Component.translatable("jsmacrosce.save"), (btn) -> save()));
        this.addRenderableWidget(new Button(width * 4 / 6, 0, width / 6, 12, font, 0, 0xFF000000, 0x7FFFFFFF, 0xFFFFFFFF, Component.translatable("jsmacrosce.close"), (btn) -> openParent()));
        this.addRenderableWidget(new Button(width, 0, 10, 12, font, 0, 0xFF000000, 0x7FFFFFFF, 0xFFFFFFFF, Component.literal(minecraft.level == null ? "X" : "-"), (btn) -> onClose()));

        if (language == null) {
            setLanguage(getDefaultLanguage());
        } else {
            compileRenderedText();
        }

        history.onChange = (content) -> {
            if (savedString.equals(content)) {
                saveBtn.setColor(0xFF00A000);
                saveBtn.setHighlightColor(0xFF007000);
            } else {
                saveBtn.setColor(0xFFA0A000);
                saveBtn.setHighlightColor(0xFF707000);
            }
        };

        cursor.onChange = (cursor) -> {
            lineCol = (cursor.startIndex != cursor.endIndex ? (cursor.endIndex - cursor.startIndex) + " " : "") +
                    (cursor.arrowEnd ? String.format("%d:%d", cursor.endLine + 1, cursor.endLineIndex + 1) : String.format("%d:%d", cursor.startLine + 1, cursor.startLineIndex + 1));
            prevChar = '\0';
            if (overlay instanceof SelectorDropdownOverlay) {
                closeOverlay(overlay);
            }
        };

        this.addRenderableWidget(new Button(this.width - width / 8, height - 12, width / 8, 12, font, 0, 0xFF000000, 0x7FFFFFFF, 0xFFFFFFFF, Component.literal(language), (btn) -> {
            int height = langs.size() * (font.lineHeight + 1) + 4;
            openOverlay(new SelectorDropdownOverlay(btn.getX(), btn.getY() - height, btn.getWidth(), height, langs.stream().map(Component::literal).collect(Collectors.toList()), font, this, (i) -> {
                setLanguage(langs.get(i));
                btn.setMessage(Component.literal(langs.get(i)));
            }));
        }));

        this.addRenderableWidget(new Button(this.width - width / 4, height - 12, width / 8, 12, font, 0, 0xFF000000, 0x7FFFFFFF, 0xFFFFFFFF, Component.translatable("jsmacrosce.settings"), (btn) -> {
            openOverlay(new SettingsOverlay(this.width / 4, this.height / 4, this.width / 2, this.height / 2, font, this));
        }));

        this.fileName = Component.literal(font.plainSubstrByWidth(file.getName(), (width - 10) / 2));

        setScroll(0);
        scrollToCursor();
    }

    public void copyToClipboard() {
        assert minecraft != null;
        minecraft.keyboardHandler.setClipboard(history.current.substring(cursor.startIndex, cursor.endIndex));
    }

    public void pasteFromClipboard() {
        assert minecraft != null;

        String pasteContent = minecraft.keyboardHandler.getClipboard();
        history.replace(cursor.startIndex, cursor.endIndex - cursor.startIndex, pasteContent);
        compileRenderedText();
    }

    public void cutToClipboard() {
        assert minecraft != null;

        minecraft.keyboardHandler.setClipboard(history.current.substring(cursor.startIndex, cursor.endIndex));
        history.replace(cursor.startIndex, cursor.endIndex - cursor.startIndex, "");
        compileRenderedText();
    }

    @Override
            //? if >1.21.8 {
    /*public boolean keyPressed(KeyEvent keyEvent) {
        boolean isSelectAll = keyEvent.isSelectAll();
        boolean isCopy = keyEvent.isCopy();
        boolean isPaste = keyEvent.isPaste();
        boolean isCut = keyEvent.isCut();
        boolean hasCtrl = keyEvent.hasControlDown();
        boolean hasShift = keyEvent.hasShiftDown();
        boolean hasAlt = keyEvent.hasAltDown();
        int keyCode = keyEvent.key();
        *///?} else {
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean isSelectAll = Screen.isSelectAll(keyCode);
        boolean isCopy = Screen.isCopy(keyCode);
        boolean isPaste = Screen.isPaste(keyCode);
        boolean isCut = Screen.isCut(keyCode);
        boolean hasCtrl = Screen.hasControlDown();
        boolean hasShift = Screen.hasShiftDown();
        boolean hasAlt = Screen.hasAltDown();
        //?}
        assert minecraft != null;
        if (overlay == null) {
            setFocused(null);
//? if >1.21.8 {
      /*} else if (overlay.keyPressed(keyEvent)) {
*///?} else {
        } else if (overlay.keyPressed(keyCode, scanCode, modifiers)) {
//?}
            return true;
        }
        if (isSelectAll) {
            cursor.updateStartIndex(0, history.current);
            cursor.updateEndIndex(history.current.length(), history.current);
            cursor.arrowEnd = true;

            return true;
        } else if (isCopy) {
            copyToClipboard();
            return true;
        } else if (isPaste) {
            pasteFromClipboard();
            return true;
        } else if (isCut) {
            cutToClipboard();
            return true;
        }
        String startSpaces;
        int index;
        double currentPage;
        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE:
                if (cursor.startIndex != cursor.endIndex) {
                    history.bkspacePos(cursor.startIndex, cursor.endIndex - cursor.startIndex);
                    compileRenderedText();
                } else if (cursor.startIndex > 0) {
                    history.bkspacePos(cursor.startIndex - 1, 1);
                    compileRenderedText();
                }
                return true;
            case GLFW.GLFW_KEY_DELETE:
                if (cursor.startIndex != cursor.endIndex) {
                    history.deletePos(cursor.startIndex, cursor.endIndex - cursor.startIndex);
                    compileRenderedText();
                } else if (cursor.startIndex < history.current.length()) {
                    history.deletePos(cursor.startIndex, 1);
                    compileRenderedText();
                }
                return true;
            case GLFW.GLFW_KEY_HOME:
                startSpaces = history.current.split("\n", -1)[cursor.startLine].split("[^\\s]", -1)[0];
                if (cursor.startLineIndex <= startSpaces.length()) {
                    cursor.updateStartIndex(cursor.startIndex - cursor.startLineIndex, history.current);
                } else {
                    cursor.updateStartIndex(cursor.startIndex - cursor.startLineIndex + startSpaces.length(), history.current);
                }
                if (!hasShift) {
                    cursor.updateEndIndex(cursor.startIndex, history.current);
                }
/*
                    //home to start of file impl

                    cursor.updateStartIndex(0, history.current);
                    cursor.updateEndIndex(0, history.current);
                    if (scrollToPercent != null) scrollToPercent.accept(0D);
*/
                return true;
            case GLFW.GLFW_KEY_END:
                int endLineLength = history.current.split("\n", -1)[cursor.endLine].length();
                cursor.updateEndIndex(cursor.endIndex + (endLineLength - cursor.endLineIndex), history.current);
                if (!hasShift) {
                    cursor.updateStartIndex(cursor.endIndex, history.current);
                }
/*
                    //end to end of file impl

                    cursor.updateStartIndex(history.current.length(), history.current);
                    cursor.updateEndIndex(history.current.length(), history.current);
                    if (scrollToPercent != null) scrollToPercent.accept(1D);
*/
                return true;
            case GLFW.GLFW_KEY_LEFT:
                if (hasShift) {
                    if (cursor.arrowEnd && cursor.startIndex != cursor.endIndex) {
                        cursor.updateEndIndex(cursor.endIndex - 1, history.current);
                        cursor.arrowLineIndex = cursor.endLineIndex;
                    } else {
                        cursor.updateStartIndex(cursor.startIndex - 1, history.current);
                        cursor.arrowLineIndex = cursor.startLineIndex;
                        cursor.arrowEnd = false;
                    }
                } else {
                    if (cursor.startIndex != cursor.endIndex) {
                        cursor.updateEndIndex(cursor.startIndex, history.current);
                    } else {
                        cursor.updateStartIndex(cursor.startIndex - 1, history.current);
                        cursor.updateEndIndex(cursor.startIndex, history.current);
                        cursor.arrowLineIndex = cursor.startLineIndex;
                    }
                }
                scrollToCursor();
                return true;
            case GLFW.GLFW_KEY_RIGHT:
                if (hasShift) {
                    if (cursor.arrowEnd || cursor.endIndex == cursor.startIndex) {
                        cursor.updateEndIndex(cursor.endIndex + 1, history.current);
                        cursor.arrowLineIndex = cursor.endLineIndex;
                        cursor.arrowEnd = true;
                    } else {
                        cursor.updateStartIndex(cursor.startIndex + 1, history.current);
                        cursor.arrowLineIndex = cursor.startLineIndex;
                    }
                } else {
                    if (cursor.startIndex != cursor.endIndex) {
                        cursor.updateStartIndex(cursor.endIndex, history.current);
                    } else {
                        cursor.updateStartIndex(cursor.startIndex + 1, history.current);
                        cursor.updateEndIndex(cursor.startIndex, history.current);
                        cursor.arrowLineIndex = cursor.startLineIndex;
                    }
                }
                scrollToCursor();
                return true;
            case GLFW.GLFW_KEY_UP:
                if (hasAlt) {
                    history.shiftLine(cursor.startLine, cursor.endLine - cursor.startLine + 1, false);
                    cursor.arrowEnd = false;
                    compileRenderedText();
                } else {
                    index = 0;
                    if (cursor.startLine > 0 || (cursor.arrowEnd && cursor.endLine > 0)) {
                        String[] lines = history.current.split("\n", -1);
                        int line = (cursor.arrowEnd ? cursor.endLine : cursor.startLine) - 1;
                        for (int i = 0; i < line; ++i) {
                            index += lines[i].length() + 1;
                        }
                        index += Math.min(lines[line].length(), cursor.arrowLineIndex);
                    }
                    if (hasShift) {
                        if (cursor.arrowEnd && index >= cursor.startIndex) {
                            cursor.updateEndIndex(index, history.current);
                            cursor.arrowEnd = true;
                        } else {
                            cursor.updateStartIndex(index, history.current);
                            cursor.arrowEnd = false;
                        }
                    } else {
                        if (cursor.startIndex == cursor.endIndex) {
                            cursor.updateStartIndex(index, history.current);
                        }
                        cursor.updateEndIndex(cursor.startIndex, history.current);
                    }
                }
                scrollToCursor();
                return true;
            case GLFW.GLFW_KEY_DOWN:
                if (hasAlt) {
                    history.shiftLine(cursor.startLine, cursor.endLine - cursor.startLine + 1, true);
                    cursor.arrowEnd = true;
                    compileRenderedText();
                } else {
                    index = 0;
                    String[] lines = history.current.split("\n", -1);
                    if (cursor.endLine < lines.length - 1 || (!cursor.arrowEnd && cursor.startLine < lines.length - 1)) {
                        int line = (cursor.arrowEnd ? cursor.endLine : cursor.startLine) + 1;
                        for (int i = 0; i < line; ++i) {
                            index += lines[i].length() + 1;
                        }
                        index += Math.min(lines[line].length(), cursor.arrowLineIndex);
                    } else {
                        index = history.current.length();
                    }
                    if (hasShift) {
                        if (!cursor.arrowEnd && index <= cursor.endIndex) {
                            cursor.updateStartIndex(index, history.current);
                            cursor.arrowEnd = false;
                        } else {
                            cursor.updateEndIndex(index, history.current);
                            cursor.arrowEnd = true;
                        }
                    } else {
                        if (cursor.startIndex != cursor.endIndex) {
                            cursor.updateStartIndex(cursor.endIndex, history.current);
                        } else {
                            cursor.updateStartIndex(index, history.current);
                            cursor.updateEndIndex(cursor.startIndex, history.current);
                        }
                    }
                }
                scrollToCursor();
                return true;
            case GLFW.GLFW_KEY_Z:
                if (hasCtrl) {
                    if (hasShift) {
                        int i = history.redo();

                        if (i != -1) {
                            compileRenderedText();
                        }
                    } else {
                        int i = history.undo();

                        if (i != -1) {
                            compileRenderedText();
                        }
                    }
                }
                return true;
            case GLFW.GLFW_KEY_Y:
                int i = history.redo();

                if (i != -1) {
                    compileRenderedText();
                }
            case GLFW.GLFW_KEY_S:
                if (hasCtrl) {
                    save();
                }
                return true;
            case GLFW.GLFW_KEY_ENTER:
                startSpaces = history.current.split("\n", -1)[cursor.startLine].split("[^\\s]", -1)[0];
                if (cursor.startIndex != cursor.endIndex) {
                    history.replace(cursor.startIndex, cursor.endIndex - cursor.startIndex, "\n" + startSpaces);
                    cursor.updateStartIndex(cursor.endIndex, history.current);
                } else {
                    history.add(cursor.startIndex, "\n" + startSpaces);
                }

                compileRenderedText();
                return true;
            case GLFW.GLFW_KEY_TAB:
                if (cursor.startIndex != cursor.endIndex || hasShift) {
                    history.tabLines(cursor.startLine, cursor.endLine - cursor.startLine + 1, hasShift);
                } else {
                    history.addChar(cursor.startIndex, '\t');
                }
                compileRenderedText();
                return true;
            case GLFW.GLFW_KEY_PAGE_UP:
                currentPage = scroll / (height - 24.0D);
                scrollbar.scrollToPercent(Mth.clamp((currentPage - 1) / (calcTotalPages() - 1), 0, 1));
                break;
            case GLFW.GLFW_KEY_PAGE_DOWN:
                currentPage = scroll / (height - 24.0D);
                scrollbar.scrollToPercent(Mth.clamp((currentPage + 1) / (calcTotalPages() - 1), 0, 1));
                return true;
            case GLFW.GLFW_KEY_RIGHT_BRACKET:
                if (hasCtrl) {
                    history.tabLinesKeepCursor(cursor.startLine, cursor.startLineIndex, cursor.endLineIndex, cursor.endLine - cursor.startLine + 1, false);
                    compileRenderedText();
                    return true;
                }
                break;
            case GLFW.GLFW_KEY_LEFT_BRACKET:
                if (hasCtrl) {
                    history.tabLinesKeepCursor(cursor.startLine, cursor.startLineIndex, cursor.endLineIndex, cursor.endLine - cursor.startLine + 1, true);
                    compileRenderedText();
                    return true;
                }
                break;
            default:
        }

        //? if >1.21.8 {
        /*return super.keyPressed(keyEvent);
        *///?} else {
        return super.keyPressed(keyCode, scanCode, modifiers);
        //?}
    }

    private synchronized void compileRenderedText() {
        long time = System.currentTimeMillis();
        List<AutoCompleteSuggestion> suggestionList;
        try {
            codeCompiler.recompileRenderedText(history.current);
            suggestionList = codeCompiler.getSuggestions();
        } catch (Throwable e) {
            e.printStackTrace();
            setLanguage("none");
            return;
        }

        if (overlay instanceof SelectorDropdownOverlay) {
            closeOverlay(overlay);
        }

        if (suggestionList.size() > 0 && JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).editorSuggestions) {
            suggestionList.sort(Comparator.comparing(a -> a.suggestion));
            int startIndex = cursor.startIndex;
            int maxWidth = 0;
            List<Component> displayList = new LinkedList<>();
            for (AutoCompleteSuggestion sug : suggestionList) {
                if (sug.startIndex < startIndex) {
                    startIndex = sug.startIndex;
                }
                int width = font.width(sug.displayText);
                if (width > maxWidth) {
                    maxWidth = width;
                }
                displayList.add(sug.displayText);
            }
            String[] lines = history.current.substring(0, startIndex).split("\n", -1);
            int startCol = font.width(Component.literal(lines[lines.length - 1]).setStyle(defaultStyle));
            int add = lineSpread - scroll % lineSpread;
            if (add == lineSpread) {
                add = 0;
            }
            int startRow = (lines.length - firstLine + 1) * lineSpread + add;

            openOverlay(
                    new SelectorDropdownOverlay(startCol + 30, startRow, maxWidth + 8, suggestionList.size() * lineSpread + 4, displayList, font, this, (i) -> {
                        if (i == -1) {
                            return;
                        }
                        AutoCompleteSuggestion selected = suggestionList.get(i);
                        history.replace(selected.startIndex, cursor.startIndex - selected.startIndex, selected.suggestion);
                        cursor.updateStartIndex(cursor.endIndex, history.current);
                        compileRenderedText();
                    })
            );
            ((SelectorDropdownOverlay) overlay).setSelected(0);
        }
        textRenderTime = System.currentTimeMillis() - time;
        this.scrollbar.setScrollPages(calcTotalPages());
        scrollToCursor();
    }

    public void scrollToCursor() {
        int cursorLine = cursor.arrowEnd ? cursor.endLine : cursor.startLine;
        if (cursorLine < firstLine || cursorLine > lastLine) {
            int pagelength = lastLine - firstLine;
            double scrollLines = history.current.split("\n", -1).length - pagelength;
            cursorLine -= pagelength / 2;
            scrollbar.scrollToPercent(Mth.clamp(cursorLine, 0, scrollLines) / scrollLines);
        }
    }

    private double calcTotalPages() {
        if (history == null) {
            return 1;
        }
        return (history.current.split("\n", -1).length) / (Math.floor((height - 24) / (double) lineSpread) - 1D);
    }

    public void save() {
        if (needSave()) {
            String current = history.current;
            try {
                handler.write(current);
                savedString = current;
                saveBtn.setColor(0xFF00A000);
                saveBtn.setHighlightColor(0xFF707000);
            } catch (IOException e) {
                openOverlay(new ConfirmOverlay(this.width / 4, height / 4, this.width / 2, height / 2, font, Component.translatable("jsmacrosce.errorsaving").append(Component.literal("\n\n" + e.getMessage())), this, null));
            }
        }
    }

    public boolean needSave() {
        return !savedString.equals(history.current);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horiz, double vert) {
        if (overlay == null && scrollbar != null) {
            // TODO: This doesn't make sense? Why would we trigger a drag event when we scrolL??
            //  If this is *really* what we want, we'll need to make a fake event
            //  Later note: I believe this is how scrolling is done in the editor?
            //? if <=1.21.8 {
            scrollbar.mouseDragged(mouseX, mouseY, 0, 0, -vert * 2);
            //?}
        }
        return super.mouseScrolled(mouseX, mouseY, horiz, vert);
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        assert minecraft != null;

        drawContext.drawString(font, fileName, 2, 2, 0xFFFFFFFF);

        drawContext.drawString(font, String.format("%d ms", (int) textRenderTime), 2, height - 10, 0xFFFFFFFF);
        drawContext.drawString(font, lineCol, (int) (width - font.width(lineCol) - (width - 10) / 4F - 2), height - 10, 0xFFFFFFFF);

        drawContext.fill(0, 12, width - 10, height - 12, 0xFF2B2B2B);
        drawContext.fill(28, 12, 29, height - 12, 0xFF707070);
        drawContext.fill(0, 12, 1, height - 12, 0xFF707070);
        drawContext.fill(width - 11, 12, width - 10, height - 12, 0xFF707070);
        drawContext.fill(1, 12, width - 11, 13, 0xFF707070);
        drawContext.fill(1, height - 13, width - 11, height - 12, 0xFF707070);

        Style lineNumStyle = defaultStyle.withColor(TextColor.fromRgb(0xFFD8D8D8));
        int add = lineSpread - scroll % lineSpread;
        if (add == lineSpread) {
            add = 0;
        }
        int y = 13;

        final Component[] renderedText = codeCompiler.getRenderedText();

        for (int i = 0, j = firstLine; j <= lastLine && j < renderedText.length; ++i, ++j) {
            if (cursor.startLine == j && cursor.endLine == j) {
                drawContext.fill(30 + cursor.startCol, y + add + i * lineSpread, 30 + cursor.endCol, y + add + (i + 1) * lineSpread, 0xFF33508F);
            } else if (cursor.startLine == j) {
                drawContext.fill(30 + cursor.startCol, y + add + i * lineSpread, width - 10, y + add + (i + 1) * lineSpread, 0xFF33508F);
            } else if (j > cursor.startLine && j < cursor.endLine) {
                drawContext.fill(29, y + add + i * lineSpread, width - 10, y + add + (i + 1) * lineSpread, 0xFF33508F);
            } else if (cursor.endLine == j) {
                drawContext.fill(29, y + add + i * lineSpread, 30 + cursor.endCol, y + add + (i + 1) * lineSpread, 0xFF33508F);
            }
            Component lineNum = Component.literal(String.format("%d.", j + 1)).setStyle(lineNumStyle);
            drawContext.drawString(minecraft.font, lineNum, 28 - minecraft.font.width(lineNum), y + add + i * lineSpread, 0xFFFFFFFF, false);
            drawContext.drawString(minecraft.font, trim(renderedText[j]), 30, y + add + i * lineSpread, 0xFFFFFFFF, false);
        }

        for (GuiEventListener b : ImmutableList.copyOf(this.children())) {
            if (b instanceof Renderable) {
                ((Renderable) b).render(drawContext, mouseX, mouseY, delta);
            }
        }

        super.render(drawContext, mouseX, mouseY, delta);
    }

    private FormattedCharSequence trim(Component text) {
        assert minecraft != null;
        if (minecraft.font.width(text) > width - 30) {
            FormattedCharSequence trimmed = Language.getInstance().getVisualOrder(minecraft.font.substrByWidth(text, width - 40 - ellipsesWidth));
            return FormattedCharSequence.composite(trimmed, ellipses);
        } else {
            return text.getVisualOrderText();
        }
    }

    @Override
    public void openParent() {
        if (needSave()) {
            openOverlay(new ConfirmOverlay(width / 4, height / 4, width / 2, height / 2, font, Component.translatable("jsmacrosce.nosave"), this, (container) -> super.openParent()));
        } else {
            super.openParent();
        }
    }

    @Override
    //? if >1.21.8 {
    /*public boolean mouseClicked(MouseButtonEvent buttonEvent, boolean debounce) {
        setFocused(null);
        boolean handled = super.mouseClicked(buttonEvent, debounce);
        double mouseX = buttonEvent.x();
        double mouseY = buttonEvent.y();
        int button = buttonEvent.button();
        boolean hasShift = buttonEvent.hasShiftDown();
    *///?} else {
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        setFocused(null);
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        boolean hasShift = Screen.hasShiftDown();
    //?}
        if (!handled && overlay == null) {
            int index = getIndexPosition(mouseX - 30, mouseY - 12 + 1);
            if (button != 1 || (cursor.startIndex > index || cursor.endIndex < index)) {
                if (hasShift) {
                    if (index < cursor.dragStartIndex) {
                        cursor.updateEndIndex(cursor.dragStartIndex, history.current);
                        cursor.updateStartIndex(index, history.current);
                        cursor.arrowEnd = false;
                        cursor.arrowLineIndex = cursor.startLineIndex;
                    } else {
                        cursor.updateStartIndex(cursor.dragStartIndex, history.current);
                        cursor.updateEndIndex(index, history.current);
                        cursor.arrowEnd = true;
                        cursor.arrowLineIndex = cursor.endLineIndex;
                    }
                } else {
                    if (cursor.startIndex == index && cursor.endIndex == index) {
                        selectWordAtCursor();
                    } else {
                        cursor.updateStartIndex(index, history.current);
                        cursor.updateEndIndex(index, history.current);
                    }
                    cursor.dragStartIndex = index;
                    cursor.arrowEnd = false;
                    cursor.arrowLineIndex = cursor.startLineIndex;
                }
            }
            if (button == 1) {
                openRClickMenu(index, (int) mouseX, (int) mouseY);
            }
        }
        return handled;
    }

    private void openRClickMenu(int index, int mouseX, int mouseY) {
        Map<String, Runnable> options = new LinkedHashMap<>();
        if (cursor.startIndex != cursor.endIndex) {
            options.put("cut", this::cutToClipboard);
            options.put("copy", this::copyToClipboard);
        }
        options.put("paste", this::pasteFromClipboard);
        options.putAll(codeCompiler.getRightClickOptions(index));
        openOverlay(new SelectorDropdownOverlay(mouseX, mouseY, 100, (font.lineHeight + 1) * options.size() + 4, options.keySet().stream().map(Component::literal).collect(Collectors.toList()), font, this, i -> options.values().toArray(new Runnable[0])[i].run()));
    }

    private int getIndexPosition(double x, double y) {
        assert minecraft != null;
        int add = lineSpread - scroll % lineSpread;
        if (add == lineSpread) {
            add = 0;
        }
        int line = firstLine + (int) ((y - add) / (double) lineSpread);
        if (line < 0) {
            line = 0;
        }
        String[] lines = history.current.split("\n", -1);
        int col;
        if (line >= lines.length) {
            line = lines.length - 1;
            col = lines[lines.length - 1].length();
        } else {
            col = minecraft.font.getSplitter().plainHeadByWidth(lines[line], (int) x, defaultStyle).length();
        }
        int count = 0;
        for (int i = 0; i < line; ++i) {
            count += lines[i].length() + 1;
        }
        count += col;
        return count;
    }

    public void selectWordAtCursor() {
        String currentLine = history.current.split("\n", -1)[cursor.startLine];
        String[] startWords = currentLine.substring(0, cursor.startLineIndex).split("\\b");
        int dStart = -startWords[startWords.length - 1].length();
        String[] endWords = currentLine.substring(cursor.startLineIndex).split("\\b");
        int dEnd = endWords[0].length();
        int currentIndex = cursor.startIndex;
        cursor.updateStartIndex(currentIndex + dStart, history.current);
        cursor.updateEndIndex(currentIndex + dEnd, history.current);
    }

    @Override
    //? if >1.21.8 {
    /*public boolean mouseDragged(MouseButtonEvent buttonEvent, double deltaX, double deltaY) {
        if (!(getFocused() instanceof Scrollbar) && buttonEvent.isLeft() && overlay == null) {
            int index = getIndexPosition(buttonEvent.x() - 30, buttonEvent.y() - 12);
    *///?} else {
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!(getFocused() instanceof Scrollbar) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && overlay == null) {
            int index = getIndexPosition(mouseX - 30, mouseY - 12);
    //?}
            if (index == cursor.dragStartIndex) {
                cursor.updateStartIndex(index, history.current);
                cursor.updateEndIndex(index, history.current);
            } else if (index < cursor.dragStartIndex) {
                cursor.updateEndIndex(cursor.dragStartIndex, history.current);
                cursor.arrowEnd = false;
                cursor.updateStartIndex(index, history.current);
                cursor.arrowLineIndex = cursor.startLineIndex;
            } else {
                cursor.updateStartIndex(cursor.dragStartIndex, history.current);
                cursor.arrowEnd = true;
                cursor.updateEndIndex(index, history.current);
                cursor.arrowLineIndex = cursor.endLineIndex;
            }
        }

        //? if >1.21.8 {
        /*return super.mouseDragged(buttonEvent, deltaX, deltaY);
        *///?} else {
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        //?}
    }

    @Override
    public void updateSettings() {
        //? if >1.21.8 {
        /*defaultFontResource = new FontDescription.Resource((ResourceLocation.parse(JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).editorFont)));
        *///?} else {
        defaultFontResource = ResourceLocation.parse(JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).editorFont);
        //?}
        defaultStyle = Style.EMPTY.withFont(defaultFontResource);
        cursor.defaultStyle = defaultStyle;
        cursor.updateStartIndex(cursor.startIndex, history.current);
        cursor.updateEndIndex(cursor.endIndex, history.current);
        setLanguage(language);
    }

    @Override
    //? if >1.21.8 {
    /*public synchronized boolean charTyped(CharacterEvent characterEvent) {
        char chr = (char) characterEvent.codepoint();
    *///?} else {
    public synchronized  boolean charTyped(char chr, int keyCode) {
    //?}
        if (overlay == null) {
            setFocused(null);
        } else if (overlay instanceof SettingsOverlay) {
            //? if >1.21.8 {
            /*return super.charTyped(characterEvent);
            *///?} else {
            return super.charTyped(chr, keyCode);
            //?}
        }
        if (blockFirst) {
            blockFirst = false;
        } else {
            if (cursor.startIndex != cursor.endIndex) {
                history.replace(cursor.startIndex, cursor.endIndex - cursor.startIndex, String.valueOf(chr));
                cursor.updateStartIndex(cursor.endIndex, history.current);
            } else {
                if (cursor.startIndex < history.current.length() &&
                        ((chr == ']' && history.current.charAt(cursor.startIndex) == ']') ||
                                (chr == '}' && history.current.charAt(cursor.startIndex) == '}') ||
                                (chr == ')' && history.current.charAt(cursor.startIndex) == ')'))) {
                    cursor.updateEndIndex(cursor.startIndex + 1, history.current);
                    cursor.updateStartIndex(cursor.endIndex, history.current);
                } else {
                    history.addChar(cursor.startIndex, chr);
                }
            }
            switch (chr) {
                case '[':
                    if (countCharBefore('[', cursor.startIndex) > countCharAfter(']', cursor.startIndex)) {
                        history.addChar(cursor.startIndex, ']');
                        cursor.updateStartIndex(cursor.startIndex - 1, history.current);
                        cursor.updateEndIndex(cursor.startIndex, history.current);
                    }
                    break;
                case '{':
                    if (countCharBefore('{', cursor.startIndex) > countCharAfter('}', cursor.startIndex)) {
                        history.addChar(cursor.startIndex, '}');
                        cursor.updateStartIndex(cursor.startIndex - 1, history.current);
                        cursor.updateEndIndex(cursor.startIndex, history.current);
                    }
                    break;
                case '(':
                    if (countCharBefore('(', cursor.startIndex) > countCharAfter(')', cursor.startIndex)) {
                        history.addChar(cursor.startIndex, ')');
                        cursor.updateStartIndex(cursor.startIndex - 1, history.current);
                        cursor.updateEndIndex(cursor.startIndex, history.current);
                    }
                    break;
                default:
            }
            prevChar = chr;
            compileRenderedText();
        }
        return false;
    }

    private int countCharBefore(char chr, int startIndex) {
        int count = 0;
        for (int i = startIndex - 1; i >= 0; --i) {
            if (history.current.charAt(i) == chr) {
                ++count;
            } else {
                break;
            }
        }
        return count;
    }

    private int countCharAfter(char chr, int startIndex) {
        int count = 0;
        for (int i = startIndex; i < history.current.length(); ++i) {
            if (history.current.charAt(i) == chr) {
                ++count;
            } else {
                break;
            }
        }
        return count;
    }

}
