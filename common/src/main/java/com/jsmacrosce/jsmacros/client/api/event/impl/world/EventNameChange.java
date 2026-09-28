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
 * Fires when the custom name of an entity changes, either because the server renamed it or
 * because something else cleared or set that name.<br>
 * This event is cancellable. Calling {@code cancel()} keeps the entity's previous name, so the
 * rename never reaches the client. Assigning to {@link #newName} instead replaces the name with
 * the value a listener chose, and assigning {@code null} clears it.<br>
 * Note: this only covers the custom name data tracker entry, the entity's translated type name
 * is not part of it.
 * example:
 * <pre>
 * JsMacros.on("NameChange", JavaWrapper.methodToJava(function (event) {
 *   const to = event.newName === null ? null : event.newName.getString();
 *   if (to !== null) {
 *     if (to.includes("badWord")) {
 *       event.cancel();
 *       return;
 *     }
 *     event.newName = Chat.createTextHelperFromString(`${event.entity.getType()} the ${to}`);
 *   }
 *   Chat.log(`Name of ${event.entity.getType()} changed to ${to === null ? "no name" : to}`);
 * }))
 * </pre>
 * @author aMelonRind
 * @since 1.9.1
 */
@DocletCategory("Network/Chat")
@Event(value = "NameChange", cancellable = true)
public class EventNameChange extends BaseEvent {
    /**
     * the entity whose name is changing. It is wrapped in the most specific
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper} subclass for its
     * type, so it can be narrowed further with {@code asLiving()}, {@code asPlayer()} and the
     * other {@code as...()} helpers. The entity's type is available from
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper#getType() getType()}
     * and its name from
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper#getName() getName()}.
     */
    public final EntityHelper<?> entity;
    /**
     * the name the entity had before this change, {@code null} if it had no custom name.
     */
    @Nullable
    public final TextHelper oldName;
    /**
     * the name the entity is being given, {@code null} if the name is being cleared.<br>
     * This field is writable. Assigning to it overrides the name the server sent, and assigning
     * {@code null} clears the name instead. Assigning to it and calling {@code cancel()} together
     * cancel out, cancelling wins. A replacement can be built with
     * {@link com.jsmacrosce.jsmacros.client.api.library.impl.FChat#createTextHelperFromString(String) Chat.createTextHelperFromString(String)}.
     */
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
