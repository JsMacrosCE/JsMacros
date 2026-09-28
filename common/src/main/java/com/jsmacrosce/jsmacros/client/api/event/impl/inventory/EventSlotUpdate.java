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
 * Fires when a single slot of a container changes because the server said so, which covers the
 * item on the mouse cursor, the player's own inventory, and whatever container screen happens to
 * be open. It is the per-slot counterpart of
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventContainerUpdate}, which
 * fires when a whole container is resent at once.<br>
 * This event is not cancellable. It is raised just before the game hands the new stack to the
 * menu, so the slot still holds the old value while listeners run and only changes once they
 * return, and there is nothing to stop in any case.<br>
 * Three things are worth being careful about. The {@code type} field only says which family of
 * update this came from, so compare it against the strings in the field's own description rather
 * than against the shipped type, which lists a value the game never produces. The {@code slot} is
 * meaningless for a {@code 'HELD'} update, which is reported as {@code -999} because the item on
 * the cursor is not a slot of the menu at all. And an {@code 'UNKNOWN'} update reports a
 * throwaway player inventory screen, so {@code slot} and {@link #getInventory()} do not describe
 * the same menu; see both members for what that means in that case.
 * example:
 * <pre>
 * JsMacros.on("SlotUpdate", JavaWrapper.methodToJava(function (event) {
 *   const now = event.newStack;
 *   if (event.slot === -999) {
 *     // -999 means the item on the cursor, which is not a menu slot at all
 *     Chat.log(`cursor now holds ${now.getName().getString()} x${now.getCount()}`);
 *     return;
 *   }
 *   const before = event.oldStack;
 *   Chat.log(`slot ${event.slot} went from ${before.getItemID()} to ${now.getItemID()}`);
 * }))
 * </pre>
 * @since 1.9.0
 */
@DocletCategory("Inventory")
@Event(value = "SlotUpdate")
public class EventSlotUpdate extends BaseEvent {
    protected final AbstractContainerScreen<?> screen;
    /**
     * which family of update this slot change came from.<br>
     * {@code 'HELD'} is the item on the mouse cursor, {@code 'INVENTORY'} is the player's own
     * inventory, {@code 'CONTAINER'} is the open container screen, and {@code 'UNKNOWN'} is a
     * container update that matched neither the player's inventory nor the screen that is
     * currently open, so the screen it reports is a throwaway player inventory rather than the
     * container that actually changed.<br>
     * Note that the declared type shipped for this field lists {@code 'SCREEN'} instead of
     * {@code 'CONTAINER'} and leaves {@code 'UNKNOWN'} out, so the field can hold a value the
     * declared type does not allow. Match on the strings above rather than on the declared type.
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
     * @return a wrapper around the menu behind the reported screen, which is the menu
     *         {@code slot} indexes for a {@code 'CONTAINER'} or {@code 'INVENTORY'} update but not
     *         for an {@code 'UNKNOWN'} one, and is not what {@code slot} means at all for a
     *         {@code 'HELD'} one
     */
    public Inventory<?> getInventory() {
        return Inventory.create(screen);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"slot\": %d, \"screen\": \"%s\"}", this.getEventName(), slot, JsMacrosClient.getScreenName(screen));
    }

}
