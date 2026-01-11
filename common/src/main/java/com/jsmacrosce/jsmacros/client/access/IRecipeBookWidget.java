package com.jsmacrosce.jsmacros.client.access;

import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;

public interface IRecipeBookWidget {

    RecipeBookPage jsmacros_getResults();

    boolean jsmacros_isSearching();

    ClientRecipeBook jsmacros_getRecipeBook();

}
