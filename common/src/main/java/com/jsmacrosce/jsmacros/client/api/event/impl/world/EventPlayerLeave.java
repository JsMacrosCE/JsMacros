package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.client.multiplayer.PlayerInfo;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.PlayerListEntryHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired when a matching player-list entry is removed from the client player list.
 * <pre>
 * JsMacros.on("PlayerLeave", JavaWrapper.methodToJava(function (event) {
 *   const name = event.player.getName();
 *   Chat.log(`${name === null ? event.UUID : name} left the player list`);
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Network/Chat")
@Event(value = "PlayerLeave", oldName = "PLAYER_LEAVE")
public class EventPlayerLeave extends BaseEvent {
    /**
     * UUID of the entry being removed, in the standard dashed string form.
     */
    @NotNull
    public final String UUID;

    /**
     * The removed player-list entry.
     */
    @NotNull
    public final PlayerListEntryHelper player;

    /**
     * Captures the UUID string and wraps the removed player-list entry for this event payload.
     * A {@code null} UUID throws {@link NullPointerException} before the entry is wrapped. The
     * arguments are required to be non-null; a null {@code player} violates that precondition and
     * is retained as a null entry inside the helper. This constructor does not itself remove an
     * entry, disconnect a player, or dispatch the event.
     *
     * @param uuid the required identifier whose string form is captured independently of the entry
     * @param player the required player-list entry to wrap
     */
    public EventPlayerLeave(@NotNull UUID uuid, @NotNull PlayerInfo player) {
        super(JsMacrosClient.clientCore);
        this.UUID = uuid.toString();
        this.player = new PlayerListEntryHelper(player);
    }

    /**
     * Returns a diagnostic representation containing the event name and the current helper
     * representation of {@link #player}; it is not a JSON serialization or a rendering of the
     * separately captured {@link #UUID} field. If the helper wraps a null entry, its representation
     * throws {@link NullPointerException} while accessing that entry.
     *
     * @return the event name and helper representation in the form {@code %s:{"player": %s}}
     */
    @Override
    public String toString() {
        return String.format("%s:{\"player\": %s}", this.getEventName(), player);
    }

}
