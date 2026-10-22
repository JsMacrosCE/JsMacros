package com.jsmacrosce.jsmacros.client.api.event.impl.inventory;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletDeclareType;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires for a server update to one menu slot or the cursor stack. Unlike
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventContainerUpdate}, it does
 * not represent a whole-container update. The event is not cancellable and is dispatched at the
 * update site before the menu receives the new stack.
 * <p>
 * {@code 'HELD'} uses slot {@code -999} for the cursor. For {@code 'UNKNOWN'}, the reported screen
 * is the player inventory even though the packet addressed another menu; see the member
 * descriptions for the implications.
 * @since 1.9.0
 */
@DocletCategory("Inventory")
@Event(value = "SlotUpdate")
public class EventSlotUpdate extends BaseEvent {
    protected final AbstractContainerScreen<?> screen;
    /**
     * Identifies the update source: {@code 'HELD'} for the cursor, {@code 'INVENTORY'} for the
     * player's inventory, {@code 'CONTAINER'} for the open container, or {@code 'UNKNOWN'} when
     * the packet matches neither. For {@code 'UNKNOWN'}, the reported screen is the player
     * inventory, not the menu addressed by the packet.
     */
    @DocletReplaceReturn("SlotUpdateType")
    @DocletDeclareType(name = "SlotUpdateType", type = "'HELD' | 'INVENTORY' | 'SCREEN'")
    public final String type;
    /**
     * the index of the slot that changed, in the numbering of the menu the update was addressed
     * to. That is the numbering {@link Inventory#getSlot(int) getSlot()} uses, but only as long as
     * the menu behind the screen this event reports is that same menu.<br>
     * For a {@code 'CONTAINER'} update it is, since the reported screen is the container the
     * packet addressed. For an {@code 'UNKNOWN'} update it is not: the packet addressed the
     * player's current menu while the reported screen is a throwaway player inventory, so a
     * {@link Inventory#getSlot(int) getSlot()} call on the {@link #getInventory()} result reads a
     * different menu than {@code slot} indexes.<br>
     * This is {@code -999} for a {@code 'HELD'} update, because the item on the cursor does not
     * live in a menu slot. Reading it back through {@link #getInventory()} will fail in that case.
     */
    public final int slot;
    /**
     * what was in the {@code slot} before the update, wrapped in an
     * {@link ItemStackHelper}. Read the id from {@link ItemStackHelper#getItemID() getItemID()}.
     * <br>
     * This is the stack object itself rather than a copy of it, and the game replaces the slot's
     * stack right after this event, so read what you need while the event is running rather than
     * holding on to it.<br>
     * It is read out of the menu the update was addressed to: the player's current menu for a
     * {@code 'CONTAINER'} or {@code 'UNKNOWN'} update, and the player's own inventory for an
     * {@code 'INVENTORY'} one.<br>
     * For a {@code 'INVENTORY'} or {@code 'CONTAINER'} update that is the menu this event reports,
     * so this is the stack that really was in {@code slot} a moment ago, and for a {@code 'HELD'}
     * update it is the stack the cursor was carrying. For an {@code 'UNKNOWN'} update the packet
     * still addressed the player's current menu, but the screen this event reports is a throwaway
     * player inventory, so {@link #getInventory()} can be a different menu from the one this field
     * and {@code slot} describe. Read this field directly in that case rather than going back
     * through the inventory.
     */
    public final ItemStackHelper oldStack;
    /**
     * what is in the {@code slot} now, wrapped in an {@link ItemStackHelper}. Read the id from
     * {@link ItemStackHelper#getItemID() getItemID()}, the count from
     * {@link ItemStackHelper#getCount() getCount()} and the data from
     * {@link ItemStackHelper#getNBT() getNBT()}.<br>
     * For an emptied slot this is an empty stack rather than {@code null}, so check it with
     * {@link ItemStackHelper#isEmpty() isEmpty()} before reading anything off it.
     */
    public final ItemStackHelper newStack;

    /**
     * Stores the supplied event values and wraps both stacks without copying them; stack arguments
     * may be {@code null}. This constructor does not validate or normalize the supplied values.
     *
     * @param screen screen associated with the event; may be {@code null}
     * @param type source label, stored unchanged and not validated; may be {@code null}
     * @param slot menu slot index, stored without validation
     * @param oldStack previous raw stack; may be {@code null}
     * @param newStack replacement raw stack; may be {@code null}
     */
    public EventSlotUpdate(AbstractContainerScreen<?> screen, String type, int slot, ItemStack oldStack, ItemStack newStack) {
        super(JsMacrosClient.clientCore);
        this.screen = screen;
        this.type = type;
        this.slot = slot;
        this.oldStack = new ItemStackHelper(oldStack);
        this.newStack = new ItemStackHelper(newStack);
    }

    /**
     * a fresh {@link Inventory} wrapper around the menu behind the screen this event reports, the
     * same way
     * {@link com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventClickSlot#getInventory()}
     * is.<br>
     * That menu is the one {@code slot} indexes for a {@code 'CONTAINER'} or {@code 'INVENTORY'}
     * update, so the two can be used together, with two exceptions. A {@code 'HELD'} update has
     * no slot to look up, so {@code slot} is {@code -999} and cannot be used with this. An
     * {@code 'UNKNOWN'} update reports a throwaway player inventory screen, so this wraps the
     * player's inventory menu while {@code slot} still indexes the menu the packet addressed; read
     * {@link #oldStack} and {@link #newStack} directly there instead of looking {@code slot} up
     * in here.
     *
     * @return a wrapper around the menu behind the reported screen
     */
    public Inventory<?> getInventory() {
        return Inventory.create(screen);
    }

    /**
     * Formats the event name, stored slot, and current screen name as a diagnostic string in the
     * form {@code %s:{"slot": %d, "screen": "%s"}}. A {@code null} screen is represented by
     * the quoted name {@code "null"}; this does not include the event type or stack values.
     *
     * @return diagnostic string containing the event name, slot, and screen name
     */
    @Override
    public String toString() {
        return String.format("%s:{\"slot\": %d, \"screen\": \"%s\"}", this.getEventName(), slot, JsMacrosClient.getScreenName(screen));
    }

}
