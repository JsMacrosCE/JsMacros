package com.jsmacrosce.jsmacros.client.api.event.impl.inventory;

import com.jsmacrosce.doclet.DocletCategory;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the local player clicks a slot in an open container screen, which includes the
 * player's own inventory and the creative inventory. It is raised for every click the screen
 * handles, whether the click ends up moving items or not, and the fields describe the click
 * rather than its result.<br>
 * This event is cancellable, and cancelling really does stop the click. The event is raised
 * before the click is handed to the game mode, and cancelling makes the mixin abort before that
 * call happens, so the click packet is never sent and no slot changes. This is also why
 * {@link EventDropSlot} is not raised for a cancelled click: the drop event is only reached once
 * the click itself has been allowed through.
 * <br>
 * For drops on their own, see {@link EventDropSlot}. For a single slot changing because the
 * server sent an update, see {@link EventSlotUpdate}.
 * example:
 * <pre>
 * JsMacros.on("ClickSlot", JavaWrapper.methodToJava(function (event) {
 *   if (event.mode === 1) {
 *     // mode 1 is the shift-click quick move, refuse it entirely
 *     event.cancel();
 *     return;
 *   }
 *   if (event.slot === -999) {
 *     // -999 is the out-of-window slot, nothing lives there to protect
 *     return;
 *   }
 *   const stack = event.getInventory().getSlot(event.slot);
 *   if (stack.getItemID() === "minecraft:diamond_sword") {
 *     Chat.log("diamond swords are not allowed to move out of this chest");
 *     event.cancel();
 *   }
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.6.4
 */
@DocletCategory("Inventory")
@Event(value = "ClickSlot", cancellable = true)
public class EventClickSlot extends BaseEvent {
    protected final AbstractContainerScreen<?> screen;
    /**
     * which kind of click this is, as the numeric id of the game's click action. The values are
     * the same on every supported game version, so this number is safe to compare against.<br>
     * {@code 0} is a plain pick up or place, {@code 1} is the shift-click quick move, {@code 2} is
     * a hotbar swap, {@code 3} is the creative middle-click clone, {@code 4} is a throw or drop,
     * {@code 5} is a right-click quick craft, and {@code 6} is a double-click pick up all, which
     * sweeps everything of that item type into the cursor.<br>
     * The action used to be spelled {@code ClickType} and is called {@code ContainerInput} on
     * newer versions, but it kept both its order and its ids across the rename. The click itself
     * goes out as the
     * <a href="https://minecraft.wiki/w/Java_Edition_protocol/Packets" target="_blank">click window packet</a>.
     */
    public final int mode;
    /**
     * which button or key produced the click, as a number.<br>
     * For a pick up or a throw this is the mouse button: {@code 0} for the left button, or for
     * dropping a single item, and {@code 1} for the right button, or for dropping the whole
     * stack. For a swap it is the hotbar slot that was pressed, {@code 0} through {@code 8}, or
     * {@code 40} for the offhand key. The creative inventory also uses {@code 9} and {@code 10}
     * for its two extra buttons.
     */
    @DocletReplaceReturn("ClickSlotButton")
    public final int button;
    /**
     * the index of the clicked slot in the open screen's menu, the same numbering
     * {@link Inventory#getSlot(int) getSlot()} uses.<br>
     * This is {@code -999}, the game's out-of-window slot, when the click landed outside the
     * container window. That case is also reported as a drop by
     * {@link com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventDropSlot}.
     */
    public final int slot;

    public EventClickSlot(AbstractContainerScreen<?> screen, int mode, int button, int slot) {
        super(JsMacrosClient.clientCore);
        this.screen = screen;
        this.mode = mode;
        this.button = button;
        this.slot = slot;
    }

    /**
     * a fresh {@link Inventory} wrapper around the menu of the screen this click happened in, so
     * it is bound to the same container the player is looking at. A new wrapper is built on every
     * call, but they all drive the same underlying menu, so an action taken through it, such as
     * {@link Inventory#dropSlot(int) dropSlot()}, is a real action on the open screen.<br>
     * The concrete type follows the screen, so a chest gives back a container inventory while the
     * player's own inventory gives back a player inventory. There is no menu slot behind
     * {@code -999}, so do not read that one back through this.
     *
     * @return the inventory of the screen this click happened in
     */
    public Inventory<?> getInventory() {
        return Inventory.create(screen);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"slot\": %d, \"screen\": \"%s\"}", this.getEventName(), slot, JsMacrosClient.getScreenName(screen));
    }

}
