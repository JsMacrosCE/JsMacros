package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

import java.util.List;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "SignEdit", oldName = "SIGN_EDIT", cancellable = true)
public class EventSignEdit extends BaseEvent {
    public final Pos3D pos;
    public boolean closeScreen = false;
    public boolean front;
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
