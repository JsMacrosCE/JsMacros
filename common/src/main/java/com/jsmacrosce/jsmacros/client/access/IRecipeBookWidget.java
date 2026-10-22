package com.jsmacrosce.jsmacros.client.access;

import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;

/**
 * Accesses recipe-book state on a focused screen component.
 * <pre>
 * const screen = Hud.getOpenScreen();
 * if (screen && typeof screen.getFocused === "function") {
 *   const widget = screen.getFocused();
 *   if (widget && typeof widget.jsmacros_isSearching === "function") {
 *     Chat.log(widget.jsmacros_isSearching());
 *   }
 * }
 * </pre>
 */
public interface IRecipeBookWidget {

    /** Returns the component's stored results page, not a list of recipes.
     * @return the same raw recipe-book page held by the component
     */
    RecipeBookPage jsmacros_getResults();

    /** Reports the component's transient text-input suppression state, not search focus or query contents.
     * @return {@code true} while the component's text-input suppression flag is set; {@code false} otherwise
     */
    boolean jsmacros_isSearching();

    /** Returns the client recipe book stored by the component.
     * @return the same raw client recipe book, or {@code null} before component initialization
     */
    ClientRecipeBook jsmacros_getRecipeBook();

}
