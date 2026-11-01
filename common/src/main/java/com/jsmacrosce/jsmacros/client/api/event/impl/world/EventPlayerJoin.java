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
 * Fired when a player-list entry is first added to the client player list.
 * <pre>
 * JsMacros.on("PlayerJoin", JavaWrapper.methodToJava(function (event) {
 *   const name = event.player.getName();
 *   Chat.log(`${name === null ? event.UUID : name} joined, ping ${event.player.getPing()} ms`);
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Network/Chat")
@Event(value = "PlayerJoin", oldName = "PLAYER_JOIN")
public class EventPlayerJoin extends BaseEvent {
    /**
     * UUID of the added player's profile; the same value returned by
     * {@link PlayerListEntryHelper#getUUID()} on {@link #player}.
     */
    public final String UUID;

    /**
     * The added player-list entry. Its name may be {@code null} when the profile has no name.
     */
    @NotNull
    public final PlayerListEntryHelper player;

    /**
     * Wraps a player-list entry in a join event; it does not add the entry or dispatch the event.
     * The supplied UUID is converted to a string independently of the profile in {@code playerInfo}.
     * A null UUID causes a {@link NullPointerException} before the player entry is wrapped.
     * {@code playerInfo} is required to be non-null but is not eagerly checked; passing {@code null}
     * despite that precondition creates a wrapper around null and causes a downstream {@link NullPointerException}.
     * @param uuid non-null identifier whose string form is stored in {@link #UUID}
     * @param playerInfo non-null player-list entry to wrap
     */
    public EventPlayerJoin(UUID uuid, @NotNull PlayerInfo playerInfo) {
        super(JsMacrosClient.clientCore);
        this.UUID = uuid.toString();
        this.player = new PlayerListEntryHelper(playerInfo);
    }

    /**
     * Returns an event-name-prefixed diagnostic containing the current player-helper representation, not a full entry snapshot.
     * If {@code playerInfo} was {@code null} despite the constructor's non-null precondition, the non-null helper contains a null raw entry; formatting then causes a {@link NullPointerException} through {@code helper.getUUID()} and {@code base.getProfile()}.
     * @return diagnostic string in the form {@code eventName:{"player": playerRepresentation}}
     */
    @Override
    public String toString() {
        return String.format("%s:{\"player\": %s}", this.getEventName(), player);
    }

}
