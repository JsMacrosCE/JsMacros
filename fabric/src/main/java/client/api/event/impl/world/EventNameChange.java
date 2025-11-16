package xyz.wagyourtail.jsmacros.client.api.event.impl.world;

import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.client.api.helper.TextHelper;
import xyz.wagyourtail.jsmacros.client.api.helper.world.entity.EntityHelper;
import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.event.Event;

/**
 * @author aMelonRind
 * @since 1.9.1
 */
@Event(value = "NameChange", cancellable = true)
public class EventNameChange extends BaseEvent {
    public final EntityHelper<?> entity;
    @Nullable
    public final TextHelper oldName;
    @Nullable
    public TextHelper newName;

    public EventNameChange(Entity entity, @Nullable Component oldName, @Nullable Component newName) {
        super(JsMacrosClient.clientCore);
        this.entity = EntityHelper.create(entity);
        this.oldName = TextHelper.wrap(oldName);
        this.newName = TextHelper.wrap(newName);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"oldName\": %s, \"newName\": %s}", this.getEventName(), oldName == null ? null : oldName.getString(), newName == null ? null : newName.getString());
    }

}
