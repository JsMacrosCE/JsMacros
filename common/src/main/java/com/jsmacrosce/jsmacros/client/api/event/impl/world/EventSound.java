package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires for every sound the client is about to play, right before it is handed to the sound
 * engine.<br>
 * This includes ambient and music sounds, block and entity sounds, and UI sounds, so the
 * {@link #position} is whatever coordinates the sound instance itself reports, which is not
 * necessarily a world position.<br>
 * This event is cancellable. Calling {@code cancel()} stops that one sound instance from playing,
 * the client keeps running and later sounds still fire normally.<br>
 * Note: {@link #sound} is {@code null} when the sound instance did not report an id, so check it
 * before comparing.
 * example:
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
     * the id of the sound being played, as the sound instance reported it. Script type
     * definitions narrow this to a {@code SoundId} union.<br>
     * Note: {@code null} when the sound instance did not report an id.
     */
    @DocletReplaceReturn("SoundId")
    public final String sound;
    /**
     * the volume of this sound instance. {@code 1.0} is the unmodified volume.
     */
    public final float volume;
    /**
     * the pitch of this sound instance. {@code 1.0} is the unmodified pitch, higher values make
     * the sound play faster and lower values slower.
     */
    public final float pitch;
    /**
     * the position the sound instance reported for this sound. For most sounds this is a world
     * position, but sounds without a world position, such as UI sounds, report their own local
     * coordinates.
     */
    public final Pos3D position;

    public EventSound(String sound, float volume, float pitch, double x, double y, double z) {
        super(JsMacrosClient.clientCore);
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.position = new Pos3D(x, y, z);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"sound\": \"%s\"}", this.getEventName(), sound);
    }

}
