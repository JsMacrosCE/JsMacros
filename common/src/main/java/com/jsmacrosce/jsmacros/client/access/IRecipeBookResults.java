package com.jsmacrosce.jsmacros.client.access;

import com.jsmacrosce.doclet.DocletIgnore;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

@ApiStatus.Internal
@DocletIgnore
public interface IRecipeBookResults {

    List<RecipeCollection> jsmacros_getResultCollections();

}
