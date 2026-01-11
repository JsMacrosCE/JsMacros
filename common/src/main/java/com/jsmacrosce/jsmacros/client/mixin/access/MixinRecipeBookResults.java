package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.jsmacrosce.jsmacros.client.access.IRecipeBookResults;

import java.util.List;

@Mixin(RecipeBookPage.class)
public class MixinRecipeBookResults implements IRecipeBookResults {

    @Shadow
    private List<RecipeCollection> recipeCollections;

    @Override
    public List<RecipeCollection> jsmacros_getResultCollections() {
        return recipeCollections;
    }

}
