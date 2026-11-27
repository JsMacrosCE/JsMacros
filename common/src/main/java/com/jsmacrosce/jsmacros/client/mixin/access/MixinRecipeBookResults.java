package com.jsmacrosce.jsmacros.client.mixin.access;

import com.jsmacrosce.doclet.DocletIgnore;

import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.jsmacrosce.jsmacros.client.access.IRecipeBookResults;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

@Mixin(RecipeBookPage.class)
@ApiStatus.Internal
@DocletIgnore
public class MixinRecipeBookResults implements IRecipeBookResults {

    @Shadow
    private List<RecipeCollection> recipeCollections;

    @Override
    public List<RecipeCollection> jsmacros_getResultCollections() {
        return recipeCollections;
    }

}
