package com.jsmacrosce.jsmacros.client.api.event.impl;

import net.minecraft.client.Minecraft;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * Fires when the loading overlay has finished its resource reload and has just started to fade out,
 * which is the moment the game considers resources loaded. That happens twice in a normal session:
 * once when the client itself finishes starting up, and once for every later reload, such as the
 * one behind the reload resources key or the one a resource pack being enabled or disabled
 * triggers.<br>
 * Use {@link #isGameStart} to tell the two apart. On the first one the title screen is about to
 * appear and no world has been joined; on the later ones the client is already in game and a reload
 * is the only reason the overlay is there.<br>
 * Note that this is raised as the overlay starts fading rather than after it has finished fading,
 * so a listener runs while the overlay is still on screen. Anything that needs the window, the
 * renderer or the title screen to be really there should wait for the next
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.player.EventOpenScreen} instead.<br>
 * This event is not cancellable, and it is not one of the events a {@code Joined} trigger can be
 * attached to, since those only work on cancellable or explicitly joinable events.
 * example:
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
     * whether this reload is the client starting up, rather than a reload requested after it had
     * already run.<br>
     * It is {@code true} on the initial load, where the title screen is about to appear, and
     * {@code false} on a reload the player asked for while the game was already running. One edge
     * case is worth knowing about: when a reload has to roll back after a failure, the game runs
     * it without a fade in and this reports {@code true} for what is really a recovery reload, so
     * treat this as "no fade in happened" rather than as a perfect test for the first launch.
     */
    public final boolean isGameStart;
    /**
     * the resource packs that were selected when the reload started, as their identifiers, which are
     * the {@code namespace:path} form such as {@code "minecraft:vanilla"}.<br>
     * It is a copy taken at that moment, so it will not follow a pack being enabled or disabled
     * later, and it includes the built-in pack, so it is rarely empty. It is a Java list rather
     * than a script array, so read it with {@code size()} and {@code get(index)}.
     */
    public final List<String> loadedPacks;

    public EventResourcePackLoaded(boolean isGameStart) {
        super(JsMacrosClient.clientCore);
        this.isGameStart = isGameStart;
        this.loadedPacks = new ArrayList<>(Minecraft.getInstance().getResourcePackRepository().getSelectedIds());
    }

    @Override
    public String toString() {
        return String.format("%s:{\"isGameStart\": %b, \"loadedPacks\": %s}", this.getEventName(), isGameStart, loadedPacks);
    }

}
