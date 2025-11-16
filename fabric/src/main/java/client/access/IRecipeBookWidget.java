package xyz.wagyourtail.jsmacros.client.access;

import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.ClientRecipeBook;

public interface IRecipeBookWidget {

    RecipeBookPage jsmacros_getResults();

    boolean jsmacros_isSearching();

    ClientRecipeBook jsmacros_getRecipeBook();

}
