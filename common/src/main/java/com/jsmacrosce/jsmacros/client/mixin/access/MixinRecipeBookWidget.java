package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.jsmacrosce.jsmacros.client.access.IRecipeBookWidget;

@Mixin(RecipeBookComponent.class)
public abstract class MixinRecipeBookWidget implements IRecipeBookWidget {
    @Shadow
    @Final
    private RecipeBookPage recipeBookPage;

    @Shadow
    private boolean ignoreTextInput;

    @Shadow
    private ClientRecipeBook book;

    @Override
    public RecipeBookPage jsmacros_getResults() {
        return recipeBookPage;
    }

    @Override
    public boolean jsmacros_isSearching() {
        return ignoreTextInput;
    }

    @Override
    public ClientRecipeBook jsmacros_getRecipeBook() {
        return book;
    }

}
