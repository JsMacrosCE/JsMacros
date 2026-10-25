package com.jsmacrosce.jsmacros.client.access;

/**
 * Mixin-backed access to the selected trade in an open merchant screen. Scripts normally select
 * offers through {@code VillagerInventory.selectTrade(index)} instead of constructing or globally
 * accessing this interface.
 * <pre>
 * if (Player.getPlayer()) {
 *   const inv = Player.openInventory();
 *   if (inv instanceof Inventories.Villager) {
 *     if (inv.getTrades().size() > 0) {
 *       const screen = inv.getRawContainer();
 *       if (screen) {
 *         if (typeof screen.jsmacros_selectIndex === "function") {
 *           screen.jsmacros_selectIndex(0);
 *         }
 *       }
 *     }
 *   }
 * }
 * </pre>
 */
public interface IMerchantScreen {
    /**
     * Selects the offer at the given position, then invokes the screen's selection handler. This
     * updates the menu selection hint, attempts to move the trade's input items, and sends a
     * selection request; it does not complete a purchase or guarantee server acceptance.
     *
     * @param index zero-based position in the current offer list; the caller must supply a valid
     *              current index, as this bridge does not range-check it
     */
    void jsmacros_selectIndex(int index);

}
