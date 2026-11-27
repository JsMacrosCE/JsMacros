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
 * Fires when an entity's custom-name data changes. Cancelling prevents the update; otherwise, a
 * joined listener can replace the name with {@code setNewName(...)}, including setting it
 * to {@code null} to clear it.
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
     * @deprecated Removal in 3.0.0: Use {@link #getNewName()} and {@link #setNewName(TextHelper)} instead.
     */
    @Nullable
    @Deprecated
    public TextHelper newName;

    /**
     * Returns the incoming custom name, or {@code null} when the name is being cleared.
     *
     * @return the current replacement helper, or {@code null}
     * @since 2.2.0
     */
    @Nullable
    public TextHelper getNewName() {
        return newName;
    }

    /**
     * Replaces the incoming custom name. {@code null} clears the name. Create replacement text with
     * {@link com.jsmacrosce.jsmacros.client.api.library.impl.FChat#createTextHelperFromString(String) Chat.createTextHelperFromString(String)}.
     *
     * @param newName replacement helper, or {@code null} to clear the name
     * @throws IllegalStateException if this delivery is not joined or its event lock was released
     * @since 2.2.0
     */
    public void setNewName(@Nullable TextHelper newName) {
        assertCanModify("setNewName");
        this.newName = newName;
    }

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
        return String.format("%s:{\"oldName\": %s, \"newName\": %s}", this.getEventName(), oldName == null ? null : oldName.getString(), getNewName() == null ? null : getNewName().getString());
    }

}
