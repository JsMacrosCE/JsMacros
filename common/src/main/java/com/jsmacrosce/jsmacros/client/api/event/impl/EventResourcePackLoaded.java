package com.jsmacrosce.jsmacros.client.api.event.impl;

import net.minecraft.client.Minecraft;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * Fires when a resource reload completes and the loading overlay begins fading out. The overlay
 * is still visible when the event fires. {@link #isGameStart} reflects whether that overlay was
 * configured without a fade-in.
 * <pre>
 * JsMacros.on("ResourcePackLoaded", JavaWrapper.methodToJava(function (event) {
 *   const packs = event.loadedPacks;
 *   if (event.isGameStart) {
 *     Chat.log(`startup, with ${packs.size()} packs selected`);
 *     return;
 *   }
 *   Chat.log(`reloaded, now on ${packs.size()} packs`);
 * }))
 * </pre>
 * @since 1.5.1
 */
@DocletCategory("System/Lifecycle")
@Event("ResourcePackLoaded")
public class EventResourcePackLoaded extends BaseEvent {
    /**
     * Whether the loading overlay for this reload was configured without a fade-in.
     */
    public final boolean isGameStart;
    /**
     * The selected resource-pack identifiers copied when this event is constructed. Later changes
     * to the selection do not update this list.
     */
    public final List<String> loadedPacks;

    /**
     * Stores the supplied overlay configuration flag and copies the repository's selected pack
     * identifiers at construction; it does not reload resources or dispatch the event.
     * @param isGameStart whether the reload overlay was configured without a fade-in
     */
    public EventResourcePackLoaded(boolean isGameStart) {
        super(JsMacrosClient.clientCore);
        this.isGameStart = isGameStart;
        this.loadedPacks = new ArrayList<>(Minecraft.getInstance().getResourcePackRepository().getSelectedIds());
    }

    /**
     * Returns a diagnostic string containing the event name, stored overlay configuration flag, and current
     * string representation of {@code loadedPacks}; this is not serialized JSON.
     * @return the form {@code eventName:{"isGameStart": boolean, "loadedPacks": listString}}
     */
    @Override
    public String toString() {
        return String.format("%s:{\"isGameStart\": %b, \"loadedPacks\": %s}", this.getEventName(), isGameStart, loadedPacks);
    }

}
