package com.jsmacrosce.wagyourgui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.function.Consumer;

//? if >1.21.8 {
/*import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}

import net.minecraft.client.gui.GuiGraphics;

import com.jsmacrosce.doclet.DocletCategory;

/** A native-button-backed editable text widget with selection and optional change and press callbacks.
 * Its constructor does not register it with a host; add it to an existing host separately for input and rendering.
 * <pre>
 * function makeTextInput(font, onChanged) {
 *   const TextInput = Java.type('com.jsmacrosce.wagyourgui.elements.TextInput');
 *   return new TextInput(10, 10, 120, 20, font, Color.rgb(0x00, 0x00, 0x00), Color.rgb(0xFF, 0xFF, 0xFF),
 *     Color.argb(0x80, 0x40, 0x40, 0xFF), Color.rgb(0xFF, 0xFF, 0xFF), '', null, JavaWrapper.methodToJava(onChanged));
 * }
 * </pre>
 * Supply a native font and JavaScript function for {@code onChanged}; {@code onClick} may be null.
 */
@DocletCategory("Screen and UI Elements")
public class TextInput extends Button {
    //? if >=26.3 {
    /*@com.jsmacrosce.doclet.DocletIgnore
    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        Minecraft.getInstance().onTextInputFocusChange(this, focused);
    }

    @com.jsmacrosce.doclet.DocletIgnore
    @Override
    public boolean capturesInput() {
        return isFocused();
    }
    *///?}
    /** Optional mutable callback receiving stored content after accepted character input, paste, cut, or deletion; null suppresses notification.
     * Some boundary deletions notify even when content is unchanged. Constructor initialization, direct field writes, {@code setMessage(String)}, selection changes, and focus changes do not notify. The callback runs before later selection updates.
     */
    public Consumer<String> onChange;
    /** Mutable Java regular-expression pattern, initially {@code ".*"}; character entry tests the entire candidate content with full-string matching.
     * Paste, cut, deletion, {@code setMessage(String)}, and direct content assignment do not apply the pattern.
     */
    public String mask = ".*";
    /** Current editable display string, initialized from the constructor message; it is not the native button label.
     * Direct writes and {@code setMessage(String)} do not update selection positions or other native-widget state; keep indices valid and refresh their GUI positions with the corresponding update methods after changing content.
     */
    public String content;
    protected int selColor;
    protected int selStart;
    /** Zero-based UTF-16 start endpoint of the selection, from zero through {@code content.length()}; endpoints may be reversed.
     * Direct writes do not refresh its cached GUI position; use {@code updateSelStart(int)} to recalculate it.
     */
    public int selStartIndex;
    protected int selEnd;
    /** Zero-based UTF-16 end endpoint of the selection, from zero through {@code content.length()}; endpoints may be reversed.
     * Direct writes do not refresh its cached GUI position; use {@code updateSelEnd(int)} to recalculate it.
     */
    public int selEndIndex;
    protected int arrowCursor;

    /** Initializes native geometry and font, stores editable content and optional callbacks, and collapses selection at the content end.
     * The native button label starts empty; {@code message} initializes editable content, not that label. Color, border, and text colors use button alpha correction; native hover fill uses {@code color}, while {@code highlightColor} is stored raw as the selection background. Does not invoke callbacks or register with a host.
     * @param x left position in GUI units
     * @param y top position in GUI units
     * @param width width in GUI units
     * @param height height in GUI units
     * @param textRenderer non-null native font
     * @param color idle fill as packed ARGB
     * @param borderColor border as packed ARGB
     * @param highlightColor raw packed ARGB selection background
     * @param textColor text color as packed ARGB
     * @param message non-null initial editable content
     * @param onClick optional native button press callback; may be null
     * @param onChange optional content-change callback; may be null
     */
    public TextInput(int x, int y, int width, int height, Font textRenderer, int color, int borderColor, int highlightColor, int textColor, String message, Consumer<Button> onClick, Consumer<String> onChange) {
        super(x, y, width, height, textRenderer, color, borderColor, color, textColor, Component.literal(""), onClick);
        this.selColor = highlightColor;
        this.content = message;
        this.onChange = onChange;
        this.updateSelStart(content.length());
        this.updateSelEnd(content.length());
        this.arrowCursor = content.length();
    }

    /** Replaces editable content only; unlike the inherited component-label setter, this does not update the native button label.
     * Does not apply {@code mask}, notify {@code onChange}, or refresh selection positions or {@code arrowCursor}; keep existing indices valid and refresh positions with the corresponding update methods.
     * @param message replacement content; supported as non-null
     */
    public void setMessage(String message) {
        content = message;
    }

    /** Stores the selection start endpoint and recalculates its cached GUI x-position from current content and font.
     * Indices are zero-based UTF-16 offsets in {@code 0..content.length()}; the index is stored before position calculation, which can fail for invalid content or index.
     * @param startIndex nonnegative UTF-16 endpoint within current content
     */
    public void updateSelStart(int startIndex) {
        selStartIndex = startIndex;
        if (startIndex == 0) {
            selStart = getX() + 1;
        } else {
            selStart = getX() + 2 + textRenderer.width(content.substring(0, startIndex));
        }
    }

    /** Stores the selection end endpoint and recalculates its cached GUI x-position from current content and font.
     * Indices are zero-based UTF-16 offsets in {@code 0..content.length()}; the index is stored before position calculation, which can fail for invalid content or index.
     * @param endIndex nonnegative UTF-16 endpoint within current content
     */
    public void updateSelEnd(int endIndex) {
        selEndIndex = endIndex;
        if (endIndex == 0) {
            selEnd = getX() + 2;
        } else {
            selEnd = getX() + 3 + textRenderer.width(content.substring(0, endIndex));
        }
    }

    //? if >1.21.8 {
    /*/^* When already focused on entry, moves both selection endpoints and {@code arrowCursor} to the position derived from the pointer's x-coordinate, then delegates to the native widget.
     * The local position update occurs before superclass filtering and may happen even if the returned handled state is false;
     * an unfocused call makes no local cursor change before delegation.
     * @param buttonEvent non-null pointer event; its x-coordinate in GUI units is read before the focus check, and the event is forwarded unchanged to the native handler
     * @param doubleClick double-click flag forwarded to the native handler, unused locally
     * @return the native handler's handled state, not whether the local selection moved
     ^/
    @Override
    public boolean mouseClicked(MouseButtonEvent buttonEvent, boolean doubleClick) {
        double mouseX = buttonEvent.x();
    *///?} else {
    /** When already focused on entry, moves both selection endpoints and {@code arrowCursor} to the position derived from {@code mouseX}, then delegates to the native widget.
     * The local position update occurs before superclass filtering and may happen even if the returned handled state is false; an unfocused call makes no local cursor change before delegation.
     * @param mouseX mouse x-coordinate in GUI units, used for the local text position
     * @param mouseY mouse y-coordinate in GUI units, forwarded to the native handler
     * @param button mouse button forwarded to the native handler
     * @return the native handler's handled state, not whether the local selection moved
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
    //?}
        if (this.isFocused()) {
            int pos = textRenderer.plainSubstrByWidth(content, (int) (mouseX - getX() - 2)).length();
            updateSelStart(pos);
            updateSelEnd(pos);
            arrowCursor = pos;
        }
        //? if >1.21.8 {
        /*return super.mouseClicked(buttonEvent, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    //? if >1.21.8 {
    /*/^* When focused on entry, moves the selection end and {@code arrowCursor} to the position derived from the pointer's x-coordinate, leaving the start endpoint unchanged, then delegates to the native widget.
     * The local update has no button, bounds, active, or visibility guard;
     * the native return value is independent of whether that update occurred.
     * @param buttonEvent non-null pointer event; its x-coordinate in GUI units is read before the focus check, and the event is forwarded unchanged to the native handler
     * @param deltaX horizontal drag delta forwarded to the native handler, unused locally
     * @param deltaY vertical drag delta forwarded to the native handler, unused locally
     * @return the native handler's handled state
     ^/
    @Override
    public boolean mouseDragged(MouseButtonEvent buttonEvent, double deltaX, double deltaY) {
        double mouseX = buttonEvent.x();
    *///?} else {
    /** When focused on entry, moves the selection end and {@code arrowCursor} to the position derived from {@code mouseX}, leaving the start endpoint unchanged, then delegates to the native widget.
     * The local update has no button, bounds, active, or visibility guard; the native return value is independent of whether that update occurred.
     * @param mouseX mouse x-coordinate in GUI units, used for the local text position
     * @param mouseY mouse y-coordinate forwarded to the native handler
     * @param button mouse button forwarded to the native handler
     * @param deltaX horizontal drag delta forwarded to the native handler
     * @param deltaY vertical drag delta forwarded to the native handler
     * @return the native handler's handled state
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
    //?}
        if (this.isFocused()) {
            int pos = textRenderer.plainSubstrByWidth(content, (int) (mouseX - getX() - 2)).length();
            updateSelEnd(pos);
            arrowCursor = pos;
        }

        //? if >1.21.8 {
        /*return super.mouseDragged(buttonEvent, deltaX, deltaY);
        *///?} else {
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        //?}
    }

    /** Exchanges the selection's start and end indices and recalculates their GUI positions from the current content and font.
     * This is an exchange, not a sort; it does not change content, arrowCursor, focus or the change callback. Indices must be within the current non-null content's range.
     */
    public void swapStartEnd() {
        int temp1 = selStartIndex;
        updateSelStart(selEndIndex);
        updateSelEnd(temp1);

    }

    //? if >1.21.8 {
    /*/^* Handles focused text-editing shortcuts and navigation, then delegates the key event to the superclass.
     * Supports select-all, copy, paste, cut, deletion and cursor navigation; paste and deletions bypass the mask.
     * Shortcuts use the event's native edit-shortcut modifier (Command on macOS, Control elsewhere), with Shift and Alt absent.
     * Left/right selection extension uses that modifier in Minecraft 1.21.10, but Control specifically in 1.21.11 and later, not Shift.
     * Select-all, Home and End update selection endpoints without refreshing arrowCursor.
     * Edits can notify the optional change callback even when deletion leaves content unchanged;
     * a callback failure can leave changed content before endpoint updates and prevents superclass delegation.
     * @param keyEvent non-null native key event; its key and modifier bits are read before the focus check and drive local handling, while its scan code is only forwarded to the superclass
     * @return the superclass's handled state, not whether text changed; local edits can occur even when it is false
     ^/
    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        boolean selectAll = keyEvent.isSelectAll();
        boolean isCopy = keyEvent.isCopy();
        boolean isPaste = keyEvent.isPaste();
        boolean isCut = keyEvent.isCut();
        boolean isCtrl = keyEvent.hasControlDown();
        int keyCode = keyEvent.key();
        *///?} else {
    /** Handles focused text-editing shortcuts and navigation, then delegates the key event to the superclass.
     * Supports select-all, copy, paste, cut, deletion and cursor navigation. Paste and deletions bypass the mask; Control, not Shift, extends left/right selection using the current native modifier state.
     * Select-all, Home and End update selection endpoints without refreshing arrowCursor. Edits can notify the optional change callback even when deletion leaves content unchanged; a callback failure can leave changed content before endpoint updates and prevents superclass delegation.
     * @param keyCode GLFW key code used for local handling
     * @param scanCode scan code forwarded to the superclass, unused locally
     * @param modifiers modifier bits forwarded to the superclass, not consulted by local shortcuts or navigation
     * @return the superclass's handled state, not whether text changed; local edits can occur even when it is false
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean selectAll = Screen.isSelectAll(keyCode);
        boolean isCopy = Screen.isCopy(keyCode);
        boolean isPaste = Screen.isPaste(keyCode);
        boolean isCut = Screen.isCut(keyCode);
        boolean isCtrl = Screen.hasControlDown();
        //?}

        if (this.isFocused()) {
            if (selEndIndex < selStartIndex) {
                swapStartEnd();
            }
            Minecraft mc = Minecraft.getInstance();
            if (selectAll) {
                this.updateSelStart(0);
                this.updateSelEnd(content.length());
            } else if (isCopy) {
                mc.keyboardHandler.setClipboard(this.content.substring(selStartIndex, selEndIndex));
            } else if (isPaste) {
                content = content.substring(0, selStartIndex) + mc.keyboardHandler.getClipboard() + content.substring(selEndIndex);
                if (onChange != null) {
                    onChange.accept(content);
                }
                updateSelEnd(selStartIndex + mc.keyboardHandler.getClipboard().length());
                arrowCursor = selStartIndex + mc.keyboardHandler.getClipboard().length();
            } else if (isCut) {
                mc.keyboardHandler.setClipboard(this.content.substring(selStartIndex, selEndIndex));
                content = content.substring(0, selStartIndex) + content.substring(selEndIndex);
                if (onChange != null) {
                    onChange.accept(content);
                }
                updateSelEnd(selStartIndex);
                arrowCursor = selStartIndex;
            }
            switch (keyCode) {
                case InputConstants.KEY_BACKSPACE:
                    if (selStartIndex == selEndIndex && selStartIndex > 0) {
                        updateSelStart(selStartIndex - 1);
                    }
                    content = content.substring(0, selStartIndex) + content.substring(selEndIndex);
                    if (onChange != null) {
                        onChange.accept(content);
                    }
                    updateSelEnd(selStartIndex);
                    arrowCursor = selStartIndex;
                    break;
                case InputConstants.KEY_DELETE:
                    if (selStartIndex == selEndIndex && selStartIndex < content.length()) {
                        updateSelEnd(selEndIndex + 1);
                    }
                    content = content.substring(0, selStartIndex) + content.substring(selEndIndex);
                    if (onChange != null) {
                        onChange.accept(content);
                    }
                    updateSelEnd(selStartIndex);
                    arrowCursor = selStartIndex;
                    break;
                case InputConstants.KEY_HOME:
                    updateSelStart(0);
                    updateSelEnd(0);
                    break;
                case InputConstants.KEY_END:
                    this.updateSelStart(content.length());
                    this.updateSelEnd(content.length());
                    break;
                case InputConstants.KEY_LEFT:
                    if (arrowCursor > 0) {
                        if (arrowCursor < selEndIndex) {
                            updateSelStart(--arrowCursor);
                            if (!isCtrl) {
                                updateSelEnd(selStartIndex);
                            }
                        } else if (arrowCursor >= selEndIndex) {
                            updateSelEnd(--arrowCursor);
                            if (!isCtrl) {
                                updateSelStart(selEndIndex);
                            }
                        }
                    }
                    break;
                case InputConstants.KEY_RIGHT:
                    if (arrowCursor < content.length()) {
                        if (arrowCursor < selEndIndex) {
                            updateSelStart(++arrowCursor);
                            if (!isCtrl) {
                                updateSelEnd(selStartIndex);
                            }
                        } else {
                            updateSelEnd(++arrowCursor);
                            if (!isCtrl) {
                                updateSelStart(selEndIndex);
                            }
                        }
                    }
                    break;
                default:
            }
        }
        //? if >1.21.8 {
        /*return super.keyPressed(keyEvent);
        *///?} else {
        return super.keyPressed(keyCode, scanCode, modifiers);
        //?}
    }

    //? if >1.21.8 {
    /*/^* Replaces the current selection with the event's Java code point only when the resulting content fully matches the mask; this method does not check focus.
     * Inserts one or two UTF-16 code units and advances selection and arrowCursor by that count.
     * Reversed selection endpoints are exchanged before testing, even if insertion is rejected.
     * Accepted content is stored before the optional change callback, then selection and arrowCursor are updated; a callback failure prevents those later updates.
     * Requires a valid non-null regex mask and valid indices in non-null content.
     * An out-of-range code point throws IllegalArgumentException before selection changes.
     * Does not delegate to the superclass and returns false on normal completion even when insertion was accepted.
     * @param characterEvent non-null native character event; only its Java code point in {@code 0..0x10FFFF} is used
     * @return false on normal completion, independently of acceptance
     ^/
    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        int codePoint = characterEvent.codepoint();
        String inserted = new String(Character.toChars(codePoint));
        int advance = Character.charCount(codePoint);
    *///?} else {
    /** Replaces the current selection with one Java UTF-16 char only when the resulting content fully matches the mask; this method does not check focus.
     * Reversed selection endpoints are exchanged before testing, even if insertion is rejected. Accepted content is stored before the optional change callback, then selection and arrowCursor advance past the inserted char; a callback failure prevents those later updates.
     * Requires a valid non-null regex mask and valid indices in non-null content. Does not delegate to the superclass and returns false on normal completion even when insertion was accepted.
     * @param chr Java UTF-16 char to insert
     * @param modifiers unused modifier bits
     * @return false on normal completion, independently of acceptance
     */
    @Override
    public boolean charTyped(char chr, int modifiers) {
        String inserted = String.valueOf(chr);
        int advance = 1;
        //?}
        if (selEndIndex < selStartIndex) {
            swapStartEnd();
        }

        String newContent = content.substring(0, selStartIndex) + inserted + content.substring(selEndIndex);
        if (newContent.matches(mask)) {
            content = newContent;
            if (onChange != null) {
                onChange.accept(content);
            }
            updateSelStart(selStartIndex + advance);
            arrowCursor = selStartIndex;
            updateSelEnd(arrowCursor);
        }
        return false;
    }

    /** Sets this widget's native focus flag without changing text selection or arrowCursor, invoking the change callback, or requesting focus through the host.
     * @param sel desired native widget focus state, not a text-selection flag
     */
    public void setSelected(boolean sel) {
        this.setFocused(sel);
    }

    @Override
    protected void renderMessage(GuiGraphics drawContext) {
        drawContext.fill(selStart, height > 9 ? getY() + 2 : getY(), Math.min(selEnd, getX() + width - 2), (height > 9 ? getY() + 2 : getY()) + textRenderer.lineHeight, selColor);
        //? if >=26.1 {
        /*drawContext.text(textRenderer, textRenderer.plainSubstrByWidth(content, width - 4), getX() + 2, height > 9 ? getY() + 2 : getY(), textColor);
        *///?} else {
        drawContext.drawString(textRenderer, textRenderer.plainSubstrByWidth(content, width - 4), getX() + 2, height > 9 ? getY() + 2 : getY(), textColor);
        //?}
    }

}
