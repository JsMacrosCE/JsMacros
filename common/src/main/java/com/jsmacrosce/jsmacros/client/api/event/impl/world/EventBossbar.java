package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import net.minecraft.client.gui.components.LerpingBossEvent;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletDeclareType;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.BossBarHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

import java.util.UUID;

/**
 * Fires when the client receives a boss bar update packet from the server.<br>
 * The {@link #type} field says which part of the bar the server touched. Only the data
 * belonging to that update type actually changed; when {@link #bossBar} is not {@code null} it
 * exposes the full vanilla boss bar state as it is at the moment the event fires.<br>
 * This event is not cancellable, it only reports what the server sent.
 * example:
 * <pre>
 * JsMacros.on("Bossbar", JavaWrapper.methodToJava(function (event) {
 *   if (event.bossBar === null) {
 *     Chat.log(`Bossbar ${event.uuid} (${event.type}) is not on screen`);
 *     return;
 *   }
 *   const bar = event.bossBar;
 *   Chat.log(`${event.type} ${bar.getName().getString()} ${(bar.getPercent() * 100).toFixed(1)}% (${bar.getColor()})`);
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Render/UI")
@Event(value = "Bossbar", oldName = "BOSSBAR_UPDATE")
public class EventBossbar extends BaseEvent {
    /**
     * the boss bar this event is about, or {@code null} when the bar is not currently in the boss
     * bar overlay.<br>
     * A {@code 'REMOVE'} update always reports {@code null} here, because the bar is taken off the
     * screen before the event is fired, but that is not the only case: the overlay is looked up by
     * the bar's uuid for every other update type too, so any of those can report {@code null} as
     * well when the overlay no longer holds a bar with that uuid. Always check this field before
     * reading from it.<br>
     * When it is not {@code null} it exposes the full bar state through
     * {@link BossBarHelper#getName() getName()},
     * {@link BossBarHelper#getPercent() getPercent()},
     * {@link BossBarHelper#getColor() getColor()} and
     * {@link BossBarHelper#getStyle() getStyle()}.
     */
    @Nullable
    public final BossBarHelper bossBar;
    /**
     * the uuid of the boss bar this event is about, in its string form. Remains usable even when
     * {@link #bossBar} is {@code null}.
     */
    public final String uuid;
    /**
     * which part of the boss bar the server updated.<br>
     * {@code 'ADD'} for a new bar, {@code 'REMOVE'} when it is taken away, and
     * {@code 'UPDATE_PERCENT'}, {@code 'UPDATE_NAME'}, {@code 'UPDATE_STYLE'} or
     * {@code 'UPDATE_PROPERTIES'} for the respective changes to an existing bar.
     */
    @DocletReplaceReturn("BossBarUpdateType")
    @DocletDeclareType(name = "BossBarUpdateType", type =
            """
            'ADD' | 'REMOVE' | 'UPDATE_PERCENT'
            | 'UPDATE_NAME' | 'UPDATE_STYLE' | 'UPDATE_PROPERTIES'
            """
    )
    public final String type;

    public EventBossbar(String type, UUID uuid, LerpingBossEvent bossBar) {
        super(JsMacrosClient.clientCore);
        if (bossBar != null) {
            this.bossBar = new BossBarHelper(bossBar);
        } else {
            this.bossBar = null;
        }
        this.uuid = uuid.toString();
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"bossBar\": %s}", this.getEventName(), bossBar != null ? bossBar.toString() : uuid);
    }

}
