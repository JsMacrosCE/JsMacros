package com.jsmacrosce.jsmacros.client.access;

/**
 * Mixin-backed access to a loom screen's stored pattern-list display state, used by loom pattern selection.
 * This is an access interface, not a global library or a type to construct directly.
 * <pre>
 * if (Player.getPlayer()) {
 *   const inv = Player.openInventory();
 *   const screen = inv.getRawContainer();
 *   if (screen && typeof screen.jsmacros_canApplyDyePattern === "function") {
 *     Chat.log(screen.jsmacros_canApplyDyePattern());
 *   }
 * }
 * </pre>
 */
public interface ILoomScreen {

    /**
     * Reports whether the loom screen's pattern list is displayed.
     * @return {@code true} when the pattern list is displayed; {@code false} otherwise
     */
    boolean jsmacros_canApplyDyePattern();

}
