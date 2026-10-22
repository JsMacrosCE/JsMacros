package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the sound engine receives a sound instance, before it attempts playback. The reported
 * position may be relative rather than world coordinates. Cancelling prevents the playback attempt.
 * <pre>
 * JsMacros.on("Sound", JavaWrapper.methodToJava(function (event) {
 *   if (event.sound === null) {
 *     return;
 *   }
 *   Chat.log(`Playing ${event.sound} (volume ${event.volume}, pitch ${event.pitch})`);
 *   if (event.volume > 2.0) {
 *     event.cancel();
 *   }
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("World")
@Event(value = "Sound", oldName = "SOUND", cancellable = true)
public class EventSound extends BaseEvent {
    /**
     * Sound id reported by the instance, or {@code null} if it reported none.
     */
    @DocletReplaceReturn("SoundId")
    public final String sound;
    /**
     * Volume reported by the sound instance; {@code 1.0} is the unmodified value. If reading the
     * volume throws a {@code NullPointerException}, this is {@code 1.0}.
     */
    public final float volume;
    /**
     * Pitch reported by the sound instance; {@code 1.0} is the unmodified value. If reading the
     * pitch throws a {@code NullPointerException}, this is {@code 1.0}.
     */
    public final float pitch;
    /**
     * Coordinates reported by the sound instance; they may be relative rather than world coordinates.
     */
    public final Pos3D position;

    /**
     * Initializes this event with the supplied sound label, multipliers, and coordinates; it does
     * not play a sound. The nullable label is stored as provided, and the coordinates are reported
     * as supplied without conversion; they may be relative rather than world coordinates.
     * @param sound nullable sound label; it is stored as provided, not looked up or validated
     * @param volume supplied volume multiplier, stored unchanged
     * @param pitch supplied pitch multiplier, stored unchanged
     * @param x supplied x coordinate, stored unchanged
     * @param y supplied y coordinate, stored unchanged
     * @param z supplied z coordinate, stored unchanged
     */
    public EventSound(String sound, float volume, float pitch, double x, double y, double z) {
        super(JsMacrosClient.clientCore);
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.position = new Pos3D(x, y, z);
    }

    /**
     * Returns a diagnostic string containing the event name and quoted sound label. The label is
     * not escaped; a null label appears as the quoted text {@code "null"}. Volume, pitch, and
     * position are omitted.
     * @return event name followed by {@code :{"sound": "..."}}; this is not a complete JSON
     *         representation of the event
     */
    @Override
    public String toString() {
        return String.format("%s:{\"sound\": \"%s\"}", this.getEventName(), sound);
    }

}
