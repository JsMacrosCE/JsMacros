package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

import java.util.List;

/**
 * Fires when the local player opens a sign editing screen. Cancelling the event or setting
 * {@link #closeScreen} prevents the screen from opening and sends the edited text to the server.
 * Changed text is also applied to the local sign immediately. {@link #signText} starts with the
 * sign's current text; only its first four entries are used, missing entries become empty, and
 * {@code null} blanks all four lines.
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
     * The sign's block position in block coordinates.
     */
    public final Pos3D pos;
    /**
     * Whether to send the edit without opening the screen; defaults to {@code false}. Setting it
     * to {@code true} has the same effect as cancelling the event.
     */
    public boolean closeScreen = false;
    /**
     * {@code true} when editing the front side; {@code false} when editing the back side.
     */
    public boolean front;
    /**
     * The sign's four text lines, initially copied from the current sign text. Only the first four
     * entries are used; missing entries become empty, and {@code null} blanks all four lines.
     */
    @Nullable
    public List<String> signText;

    /**
     * Initializes the event with its client core, block position, edited text, and sign side.
     * The supplied coordinates are integer block coordinates stored in a new {@link Pos3D};
     * {@code front} is stored unchanged. {@code signText} is stored as the supplied list reference,
     * including {@code null}, without copying or normalizing it.
     * @param signText the text list to retain, or {@code null}
     * @param x the block-coordinate x position
     * @param y the block-coordinate y position
     * @param z the block-coordinate z position
     * @param front whether the edited side is the front; {@code false} denotes the back
     */
    @SuppressWarnings("NullableProblems")
    public EventSignEdit(List<String> signText, int x, int y, int z, boolean front) {
        super(JsMacrosClient.clientCore);
        this.pos = new Pos3D(x, y, z);
        this.front = front;
        this.signText = signText;
    }

    /**
     * Returns a diagnostic string containing the event name and current position in the form
     * {@code %s:{"pos": [%s]}}; the position is enclosed in square brackets. It omits the text,
     * side, and screen-close flag, and is not a serialization of the full event.
     * @return the event name and current position in diagnostic format
     */
    @Override
    public String toString() {
        return String.format("%s:{\"pos\": [%s]}", this.getEventName(), pos);
    }

}
