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
 * Fires when the player drops items, either from an open container screen with the drop key or by
 * clicking outside the window, or straight from the hotbar with the drop key while no screen is
 * open.<br>
 * This event is cancellable, and cancelling really does prevent the drop: in a screen the click
 * handler is aborted before the drop is sent, and from the hotbar the game's own drop call is made
 * to report that nothing was dropped. A drop cancelled this way leaves the stack where it was and
 * no drop packet goes out.<br>
 * A drop from a screen always has a
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventClickSlot} raised for the
 * same interaction first, and that one has to be allowed through before this event is reached.
 * The other clicks that can move items out of a slot, such as a shift-click, are not reported
 * here, they only produce the click event.
 * <br>
 * There is no screen behind this event when the drop came from the hotbar with the drop key, so
 * read the inventory through {@link #getInventory()} rather than assuming a screen is there. That
 * method covers both cases and always returns something usable.
 * example:
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
     * the index of the slot being dropped from, in the numbering of the screen's menu, or
     * {@code -999} when the drop came from clicking outside the window with a stack on the
     * cursor.<br>
     * When there is no screen, because the drop came from the hotbar key, this is
     * {@code 36} plus the hotbar slot, which is the numbering the player's own inventory menu
     * uses for hotbar slots {@code 36} through {@code 44}. In every case the number can be handed
     * straight to {@link #getInventory()}'s {@link Inventory#getSlot(int) getSlot()}.
     */
    public final int slot;
    /**
     * whether the whole stack is being dropped, rather than a single item.<br>
     * On a keyboard this is the drop key with the control key held, and in a container screen it
     * is a right click rather than a left one. It is {@code false} for the plain single item drop.
     * Cancelling applies to the drop being made right now, so cancelling a single item drop does
     * not stop a later whole stack drop of the same slot.
     */
    public final boolean all;

    public EventDropSlot(AbstractContainerScreen<?> screen, int slot, boolean all) {
        super(JsMacrosClient.clientCore);
        this.screen = screen;
        this.slot = slot;
        this.all = all;
    }

    /**
     * a fresh {@link Inventory} wrapper around the container the drop is coming from.<br>
     * When there is a screen this is bound to the same menu the player is looking at, in the same
     * way {@link com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventClickSlot#getInventory()}
     * is. When there is no screen, which is the hotbar drop case, it is a player inventory built
     * around the player instead, so it is always safe to use and always safe to read
     * {@link Inventory#getSlot(int) getSlot()} on with this event's {@code slot}.
     *
     * @return the inventory the drop is coming from, never {@code null}
     */
    public Inventory<?> getInventory() {
        if (screen == null) {
            assert mc.player != null;
            return Inventory.create(new InventoryScreen(mc.player));
        }
        return Inventory.create(screen);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"slot\": %d, \"screen\": \"%s\"}", this.getEventName(), slot, JsMacrosClient.getScreenName(screen));
    }

}
