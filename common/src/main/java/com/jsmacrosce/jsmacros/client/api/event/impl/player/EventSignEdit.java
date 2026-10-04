package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

import java.util.List;

/**
 * Fires when the local player opens a sign or other text editing block to edit it.<br>
 * This event is cancellable, and cancelling here does not throw the edit away. Cancelling, or
 * setting {@link #closeScreen}, stops the sign editing screen from opening and instead sends
 * whatever is in {@link #signText} straight to the server as a finished edit. That makes this the
 * place to rewrite sign text without ever showing the player an editor.<br>
 * {@link #signText} starts out holding the sign's current text, and is writable. Only the first
 * four entries are used, a shorter list leaves the remaining lines empty, and assigning
 * {@code null} blanks the sign. If the text ends up different from what was there, the client
 * applies it locally too, so the change shows up on screen without waiting for the server.<br>
 * Note: this only covers the sign editing screen. A hanging sign, a lectern or a sign that
 * another player is already editing follow their own paths.
 * example:
 * <pre>
 * JsMacros.on("SignEdit", JavaWrapper.methodToJava(function (event) {
 *   const text = event.signText;
 *   if (text === null) {
 *     return;
 *   }
 *   let blocked = false;
 *   for (const line of text) {
 *     if (line.includes("badWord")) {
 *       blocked = true;
 *     }
 *   }
 *   if (blocked) {
 *     // rewrite the text, then send the edit without ever opening the editor
 *     let i = 0;
 *     while (i !== text.size()) {
 *       text.set(i, text.get(i).replaceAll("badWord", "***"));
 *       i += 1;
 *     }
 *     event.cancel();
 *     return;
 *   }
 *   Chat.log(`Editing sign at ${event.pos.x}, ${event.pos.y}, ${event.pos.z}`);
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Inputs/Interactions")
@Event(value = "SignEdit", oldName = "SIGN_EDIT", cancellable = true)
public class EventSignEdit extends BaseEvent {
    /**
     * the block position of the sign, as block coordinates rather than the sign's own centred
     * position.
     */
    public final Pos3D pos;
    /**
     * whether to send the edit and close without opening the sign editing screen. It starts out
     * {@code false}. Setting it to {@code true} has the same effect on the screen as calling
     * {@code cancel()}, so use one or the other, not both.
     * <br>
     * This field is writable.
     */
    public boolean closeScreen = false;
    /**
     * {@code true} if the sign side being edited is the front, {@code false} if it is the back.
     * Signs have two independently editable sides.
     */
    public boolean front;
    /**
     * the four lines of text on the sign, one string per line. It starts out holding the sign's
     * current text and is writable.<br>
     * Only the first four entries are used. A shorter list leaves the remaining lines empty and
     * any entries past the fourth are ignored. Assigning {@code null} blanks the whole sign.
     */
    @Nullable
    public List<String> signText;

    @SuppressWarnings("NullableProblems")
    public EventSignEdit(List<String> signText, int x, int y, int z, boolean front) {
        super(JsMacrosClient.clientCore);
        this.pos = new Pos3D(x, y, z);
        this.front = front;
        this.signText = signText;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"pos\": [%s]}", this.getEventName(), pos);
    }

}
