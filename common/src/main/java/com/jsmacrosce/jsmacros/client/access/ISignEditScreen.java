package com.jsmacrosce.jsmacros.client.access;

/**
 * Mixin-backed access to text and cursor operations on an open sign-edit screen; this is not a
 * screen type to construct or a global API.
 * <pre>
 * const screen = Hud.getOpenScreen();
 * if (screen && typeof screen.jsmacros_setLine === "function" && typeof screen.jsmacros_fixSelection === "function") {
 *   screen.jsmacros_setLine(0, "Hello");
 *   screen.jsmacros_fixSelection(); // moves the cursor on the current line
 * }
 * </pre>
 */
public interface ISignEditScreen {

    /**
     * Replaces a line in the open sign editor's messages and displayed sign text; it does not send
     * a packet, update the block entity, close the editor, or change the active line or cursor.
     * A line outside the message array's range throws {@code IndexOutOfBoundsException} before either value is changed.
     * @param line zero-based line from the top (normally 0 through 3 for a four-line sign)
     * @param text non-null replacement line text
     */
    void jsmacros_setLine(int line, String text);

    /**
     * Moves the cursor to the end of the currently selected line and collapses any selection; it
     * does not change which line is active. With a non-null current message, a missing text-field
     * helper leaves the cursor unchanged.
     */
    void jsmacros_fixSelection();

}
