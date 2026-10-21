package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when an entity's custom-name data changes. Cancelling prevents the update; otherwise,
 * listeners can replace {@link #newName}, including setting it to {@code null} to clear the name.
 * @author aMelonRind
 * @since 1.9.1
 */
@DocletCategory("Network/Chat")
@Event(value = "NameChange", cancellable = true)
public class EventNameChange extends BaseEvent {
    /**
     * Entity whose custom name is changing.
     */
    public final EntityHelper<?> entity;
    /**
     * Previous custom name, or {@code null} if none was set.
     */
    @Nullable
    public final TextHelper oldName;
    /**
     * New custom name, or {@code null} to clear it. A listener may replace the incoming value;
     * cancellation prevents the update. Create replacement text with
     * {@link com.jsmacrosce.jsmacros.client.api.library.impl.FChat#createTextHelperFromString(String) Chat.createTextHelperFromString(String)}.
     */
    @Nullable
    public TextHelper newName;

    /**
     * Wraps the entity and custom-name values for a name-change event; it does not apply the
     * change or dispatch the event.
     * Passing {@code null} for {@code entity} causes a {@code NullPointerException}.
     * @param entity non-null entity to wrap
     * @param oldName previous raw custom-name component, or {@code null} if none was set
     * @param newName supplied raw custom-name component, or {@code null} to clear the name
     */
    public EventNameChange(Entity entity, @Nullable Component oldName, @Nullable Component newName) {
        super(JsMacrosClient.clientCore);
        this.entity = EntityHelper.create(entity);
        this.oldName = TextHelper.wrap(oldName);
        this.newName = TextHelper.wrap(newName);
    }

    /**
     * Returns a diagnostic string containing the event name and current name values.
     * Each value is the corresponding helper's {@code getString()} result, or the unquoted
     * token {@code null}; {@code newName} therefore reflects its current, potentially
     * listener-replaced value. This is not JSON serialization.
     * @return a string in the form {@code eventName:{"oldName": oldValue, "newName": newValue}}
     */
    @Override
    public String toString() {
        return String.format("%s:{\"oldName\": %s, \"newName\": %s}", this.getEventName(), oldName == null ? null : oldName.getString(), newName == null ? null : newName.getString());
    }

}
