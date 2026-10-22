package com.jsmacrosce.jsmacros.client.api.event.impl.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires for drops from a container screen (including clicking outside it) and for hotbar-key
 * drops when no screen is open. Cancelling prevents the drop. Screen drops first raise
 * {@link EventClickSlot}; cancelling that click prevents this event from firing. Other item-moving
 * clicks, such as shift-clicks, do not raise this event.
 * <p>Use {@link #getInventory()} to access the source inventory; the hotbar-key case has no screen.
 * <pre>
 * JsMacros.on("DropSlot", JavaWrapper.methodToJava(function (event) {
 *   if (event.slot === -999) {
 *     // dropping what is on the cursor by clicking outside the window
 *     event.cancel();
 *     return;
 *   }
 *   const stack = event.getInventory().getSlot(event.slot);
 *   if (stack.getItemID() === "minecraft:diamond_sword") {
 *     Chat.log(event.all ? "no dropping whole swords" : "no dropping swords");
 *     event.cancel();
 *   }
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.6.4
 */
@DocletCategory("Inventory")
@Event(value = "DropSlot", cancellable = true)
public class EventDropSlot extends BaseEvent {
    protected static final Minecraft mc = Minecraft.getInstance();

    protected final AbstractContainerScreen<?> screen;
    /**
     * The index of the slot being dropped from, in the screen menu's numbering, or
     * {@code -999} when the drop came from clicking outside the window with a stack on the
     * cursor.<br>
     * When there is no screen, because the drop came from the hotbar key, this is
     * {@code 36} plus the hotbar slot, which is the numbering the player's own inventory menu
     * uses for hotbar slots {@code 36} through {@code 44}. The number can be passed
     * straight to {@link #getInventory()}'s {@link Inventory#getSlot(int) getSlot()}.
     */
    public final int slot;
    /**
     * Whether this action drops the whole stack rather than one item. For a screen drop this is
     * {@code true} for a right-click throw; for a hotbar-key drop it reflects the game's
     * {@code entireStack} argument.
     */
    public final boolean all;

    /**
     * Stores the source screen and drop-slot values on the event after initializing its base event.
     * This does not perform or dispatch a drop, look up the inventory, or validate, normalize, or
     * copy the supplied values.
     *
     * @param screen the source container screen, or {@code null} for a screenless hotbar-key drop
     * @param slot the source inventory-menu index; {@code -999} denotes an outside-window drop, and
     *     hotbar-key indices conventionally range from {@code 36} through {@code 44}; the value is
     *     stored without validation
     * @param all the stored flag indicating a whole-stack rather than one-item drop; it does not
     *     guarantee that a drop occurs
     */
    public EventDropSlot(AbstractContainerScreen<?> screen, int slot, boolean all) {
        super(JsMacrosClient.clientCore);
        this.screen = screen;
        this.slot = slot;
        this.all = all;
    }

    /**
     * Returns a new inventory wrapper for the source: the screen menu for screen drops, or the
     * player's inventory for hotbar-key drops. The event's {@code slot} can be used with
     * {@link Inventory#getSlot(int) getSlot()} on the returned wrapper.
     *
     * @return the inventory the drop is coming from
     */
    public Inventory<?> getInventory() {
        if (screen == null) {
            assert mc.player != null;
            return Inventory.create(new InventoryScreen(mc.player));
        }
        return Inventory.create(screen);
    }

    /**
     * Returns a diagnostic string in the format {@code %s:{"slot": %d, "screen": "%s"}},
     * containing the event name, stored slot, and screen name. A {@code null} screen is rendered
     * as the quoted string {@code "null"}; this is not a JSON {@code null} value, and the string
     * does not include the {@code all} flag.
     *
     * @return the diagnostic string in the format shown above
     */
    @Override
    public String toString() {
        return String.format("%s:{\"slot\": %d, \"screen\": \"%s\"}", this.getEventName(), slot, JsMacrosClient.getScreenName(screen));
    }

}
