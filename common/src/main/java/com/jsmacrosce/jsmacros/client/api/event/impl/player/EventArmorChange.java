package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import net.minecraft.world.item.ItemStack;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletDeclareType;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
  * Fires when client-tick polling detects a change to one of the local player's four armor slots.
  * For a damage-only change, {@link com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventItemDamage}
  * fires first.
 * <pre>
 * JsMacros.on("ArmorChange", JavaWrapper.methodToJava(function (event) {
 *   const now = event.item.isEmpty() ? "empty" : `${event.item.getItemId()} x${event.item.getCount()}`;
 *   const before = event.oldItem.isEmpty() ? "empty" : `${event.oldItem.getItemId()} x${event.oldItem.getCount()}`;
 *   Chat.log(`Armor ${event.slot}: ${before} to ${now}`);
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Player/Stats")
@Event(value = "ArmorChange", oldName = "ARMOR_CHANGE")
public class EventArmorChange extends BaseEvent {
    /**
     * The changed armor slot: {@code HEAD}, {@code CHEST}, {@code LEGS}, or
     * {@code FEET}.
     */
    @DocletReplaceReturn("ArmorSlot")
    @DocletDeclareType(name = "ArmorSlot", type = "'HEAD' | 'CHEST' | 'LEGS' | 'FEET'")
    public final String slot;
    /**
     * The item currently in the slot. An empty slot is represented by an empty
     * {@link com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper}, so check
     * {@link com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper#isEmpty() isEmpty()}
     * before reading the id or the count.
     */
    public final ItemStackHelper item;
    /**
     * The item previously recorded for the slot by polling, represented by an empty
     * {@link com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper} if the slot was
     * empty.
     */
    public final ItemStackHelper oldItem;

    /**
     * Stores the supplied slot label and wraps the current and previous raw stacks in helpers.
     * The slot is stored verbatim without validation; either raw stack may be {@code null},
     * and is passed to its helper unchanged.
     * @param slot supplied armor-slot label, possibly {@code null}
     * @param item current raw item stack, possibly {@code null}
     * @param old previous raw item stack, possibly {@code null}
     */
    public EventArmorChange(String slot, ItemStack item, ItemStack old) {
        super(JsMacrosClient.clientCore);
        this.slot = slot;
        this.item = new ItemStackHelper(item);
        this.oldItem = new ItemStackHelper(old);
    }

    /**
     * Returns a diagnostic string containing the event name and slot label; the item values
     * are omitted. The format is {@code %s:{"slot": %s}} with standard {@code String.format}
     * conversion, so the slot is not quoted or escaped as JSON.
     * @return diagnostic event-name and slot string
     */
    @Override
    public String toString() {
        return String.format("%s:{\"slot\": %s}", this.getEventName(), slot);
    }

}
