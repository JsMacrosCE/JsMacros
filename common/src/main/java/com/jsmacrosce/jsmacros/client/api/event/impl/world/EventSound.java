package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "Sound", oldName = "SOUND", cancellable = true)
public class EventSound extends BaseEvent {
    @DocletReplaceReturn("SoundId")
    public final String sound;
    public final float volume;
    public final float pitch;
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
