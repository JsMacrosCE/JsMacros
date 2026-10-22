package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import net.minecraft.world.item.ItemStack;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when polling detects a change to the local player's main- or off-hand stack, at most once
 * per client tick. It does not fire for an empty-to-empty transition. If only damage changes,
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventItemDamage} fires first. This
 * event is not cancellable.
 * <pre>
 * JsMacros.on("HeldItemChange", JavaWrapper.methodToJava(function (event) {
 *   const hand = event.offHand ? "off hand" : "main hand";
 *   const now = event.item.isEmpty() ? "empty" : `${event.item.getItemId()} x${event.item.getCount()}`;
 *   const before = event.oldItem.isEmpty() ? "empty" : `${event.oldItem.getItemId()} x${event.oldItem.getCount()}`;
 *   Chat.log(`${hand}: ${before} to ${now}`);
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Inventory")
@Event(value = "HeldItemChange", oldName = "HELD_ITEM")
public class EventHeldItemChange extends BaseEvent {
    /**
     * Whether the changed stack is in the off hand; {@code false} indicates the main hand.
     */
    public final boolean offHand;
    /**
     * The current stack in the changed hand. An empty hand is represented by an empty
     * {@link com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper}, not {@code null}.
     */
    public final ItemStackHelper item;
    /**
     * The previous stack in the changed hand; an empty hand is represented by an empty
     * {@link com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper}, not {@code null}.
     */
    public final ItemStackHelper oldItem;

    /**
     * Wraps the supplied current and previous raw stacks and records which hand they describe.
     * This constructor does not detect or dispatch a hand change. Each argument is retained as
     * the corresponding helper's raw value; a {@code null} stack therefore produces a non-null
     * helper wrapping {@code null}.
     *
     * @param item current raw stack; may be {@code null}
     * @param oldItem previous raw stack; may be {@code null}
     * @param offHand {@code true} for the off hand, {@code false} for the main hand
     */
    public EventHeldItemChange(ItemStack item, ItemStack oldItem, boolean offHand) {
        super(JsMacrosClient.clientCore);
        this.item = new ItemStackHelper(item);
        this.oldItem = new ItemStackHelper(oldItem);
        this.offHand = offHand;
    }

    /**
     * Formats the event name and current stack helper as {@code %s:{"item": %s}}; the previous
     * stack and hand indicator are omitted. Formatting dereferences the current raw stack, so a
     * current stack wrapped from {@code null} causes a {@link NullPointerException}.
     *
     * @return the formatted event name and current stack representation
     */
    @Override
    public String toString() {
        return String.format("%s:{\"item\": %s}", this.getEventName(), item.toString());
    }

}
