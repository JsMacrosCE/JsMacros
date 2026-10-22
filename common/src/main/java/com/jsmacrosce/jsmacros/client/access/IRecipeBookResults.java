package com.jsmacrosce.jsmacros.client.access;

import com.jsmacrosce.doclet.DocletIgnore;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;

import java.util.List;

@DocletIgnore
public interface IRecipeBookResults {

    List<RecipeCollection> jsmacros_getResultCollections();

}
