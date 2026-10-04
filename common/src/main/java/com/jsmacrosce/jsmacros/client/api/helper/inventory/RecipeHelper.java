package com.jsmacrosce.jsmacros.client.api.helper.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A recipe: what it takes and what it makes, and whether the player can afford it right now.
 * <p>
 * This is the one helper in the package a plain script does not build for itself. A recipe is not
 * something the client can look up by id, so these come from an inventory object, and the inventory
 * objects are what an extension adds to a library it builds with
 * {@link com.jsmacrosce.jsmacros.core.library.impl.FReflection#createLibraryBuilder(String, boolean, String...)}. The examples below are
 * therefore written as functions taking the recipes as an argument, which is how an extension
 * hands them on.
 * <p>
 * The two groups of calls here are about the recipe and about the player. {@link #getIngredients()}
 * and {@link #getOutput()} are the same whatever the inventory holds, while
 * {@link #canCraft()}, {@link #canCraft(int)}, {@link #getCraftableAmount()} and
 * {@link #craft(boolean)} all read the player, and read the crafting screen as well when one is
 * open.
 * example:
 * <pre>
 * // what an extension's library would hand to a macro
 * function report(recipes) {
 *   for (let i = 0; i !== recipes.size(); i += 1) {
 *     const recipe = recipes.get(i);
 *     const out = recipe.getOutput();
 *     Chat.log(`${out.getItemId()} x${out.getCount()}, group ${recipe.getGroup()}`);
 *
 *     // every ingredient slot, each of which may accept more than one item
 *     const slots = recipe.getIngredients();
 *     for (let s = 0; s !== slots.size(); s += 1) {
 *       const slot = slots.get(s);
 *       const names = [];
 *       for (let k = 0; k !== slot.size(); k += 1) {
 *         names.push(slot.get(k).getItemId());
 *       }
 *       Chat.log(`  slot ${s}: ${names.join(" or ")}`);
 *     }
 *   }
 * }
 *
 * // and what the player can actually make
 * function affordable(recipes) {
 *   for (let i = 0; i !== recipes.size(); i += 1) {
 *     const recipe = recipes.get(i);
 *     const times = recipe.getCraftableAmount();
 *     if (times > 0) {
 *       Chat.log(`${recipe.getOutput().getItemId()} can be made ${times} times`);
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.3.1
 */
@DocletCategory("Items/Enchantments")
@SuppressWarnings("unused")
public class RecipeHelper extends BaseHelper<RecipeDisplayEntry> {
    private static final Minecraft mc = Minecraft.getInstance();
    protected int syncId;

    public RecipeHelper(RecipeDisplayEntry base, int syncId) {
        super(base);
        this.syncId = syncId;
    }

//    /**
//     * @return
//     * @since 1.3.1
//     */
//    @DocletReplaceReturn("RecipeId")
//    public String getId() {
//        return base.
//    }

    /**
     * The ingredients, as a list of slots with one list of item stacks in each. A slot holds every
     * item that will do for that position rather than a single item, so the outer list is the
     * shape of the crafting grid and the inner lists are what may go in each cell of it.<br>
     * A recipe that declares no ingredients gives a list with no slots in it rather than
     * {@code null}.
     * example:
     * <pre>
     * // the crafting grid, cell by cell
     * function showGrid(recipe) {
     *   const slots = recipe.getIngredients();
     *   for (let s = 0; s !== slots.size(); s += 1) {
     *     const slot = slots.get(s);
     *     const names = [];
     *     for (let k = 0; k !== slot.size(); k += 1) {
     *       names.push(`${slot.get(k).getItemId()} x${slot.get(k).getCount()}`);
     *     }
     *     Chat.log(`slot ${s} takes ${names.join(" or ")}`);
     *   }
     * }
     *
     * // a recipe that takes any one plank will list all of them in the same slot
     * function slotAccepts(recipe, slotIndex, id) {
     *   const slots = recipe.getIngredients();
     *   if (slotIndex >= slots.size()) {
     *     return false;
     *   }
     *   const slot = slots.get(slotIndex);
     *   for (let k = 0; k !== slot.size(); k += 1) {
     *     if (slot.get(k).getItemId() === id) {
     *       return true;
     *     }
     *   }
     *   return false;
     * }
     * </pre>
     *
     * @return the ingredient slots of this recipe, each holding every item that will do for it.
     * @since 1.8.3
     */
    public List<List<ItemStackHelper>> getIngredients() {
        List<List<ItemStackHelper>> ingredients = new ArrayList<>();

        for (Ingredient in : base.craftingRequirements().orElseGet(List::of)) {
            ingredients.add(in.items().map(ItemStack::new).map(ItemStackHelper::new).collect(Collectors.toList()));
        }

        return ingredients;
    }

    /**
     * What the recipe makes, as a stack of the first result. A recipe with more than one result
     * gives the first of them here, and the count is whatever the recipe's output says rather than
     * a number of crafts.<br>
     * The result is worked out against the world that is loaded, since some recipes name what they
     * make by an id only the world can resolve.
     * example:
     * <pre>
     * // what each recipe makes
     * function outputs(recipes) {
     *   for (let i = 0; i !== recipes.size(); i += 1) {
     *     const out = recipes.get(i).getOutput();
     *     Chat.log(`${out.getItemId()} x${out.getCount()} in group ${recipes.get(i).getGroup()}`);
     *   }
     * }
     *
     * // and a count of how many of one thing the recipes here would make
     * function totalOf(recipes, id) {
     *   let total = 0;
     *   for (let i = 0; i !== recipes.size(); i += 1) {
     *     const out = recipes.get(i).getOutput();
     *     if (out.getItemId() === id) {
     *       total += out.getCount();
     *     }
     *   }
     *   return total;
     * }
     * </pre>
     *
     * @return the first result of this recipe.
     * @since 1.3.1
     */
    public ItemStackHelper getOutput() {
        assert mc.level != null;
        return new ItemStackHelper(base.resultItems(SlotDisplayContext.fromLevel(mc.level)).getFirst());
    }

    /**
     * Crafts the recipe, as many times as {@code craftAll} asks for, and gives the recipe back so
     * calls can be chained. The crafting is sent to the screen the recipe came from, so the screen
     * that produced the recipe has to still be open: a recipe handed out earlier and then left
     * behind raises rather than quietly doing nothing.
     * <p>
     * {@code craftAll} is what decides between crafting once and filling the grid as far as the
     * ingredients go.
     * example:
     * <pre>
     * // craft the first recipe that can be made, once
     * function craftFirst(recipes) {
     *   for (let i = 0; i !== recipes.size(); i += 1) {
     *     const recipe = recipes.get(i);
     *     if (recipe.canCraft()) {
     *       recipe.craft(false);
     *       Chat.log(`crafted ${recipe.getOutput().getItemId()}`);
     *       return recipe;
     *     }
     *   }
     *   return null;
     * }
     *
     * // and the same thing repeatedly, which is what craftAll is for
     * function craftAllOf(recipes, id) {
     *   for (let i = 0; i !== recipes.size(); i += 1) {
     *     const recipe = recipes.get(i);
     *     if (recipe.getOutput().getItemId() === id) {
     *       if (recipe.getCraftableAmount() > 0) {
     *         recipe.craft(true);
     *         return recipe;
     *       }
     *     }
     *   }
     *   return null;
     * }
     * </pre>
     *
     * @param craftAll whether to craft as many times as the ingredients allow rather than once.
     * @return this recipe, for chaining.
     * @throws AssertionError if the screen this recipe came from is no longer open.
     * @since 1.3.1
     */
    public RecipeHelper craft(boolean craftAll) {
        Minecraft mc = Minecraft.getInstance();
        assert mc.player != null;
        if ((mc.screen instanceof AbstractContainerScreen && ((AbstractContainerScreen<?>) mc.screen).getMenu().containerId == syncId) ||
                (mc.screen == null && syncId == mc.player.inventoryMenu.containerId)) {
            assert mc.gameMode != null;
            mc.gameMode.handlePlaceRecipe(syncId, base.id(), craftAll);
            return this;
        }
        throw new AssertionError("Crafting Screen no longer open!");
    }

    /**
     * The recipe book's group for this recipe, as the id the game files the recipe under. This is
     * what decides which tab of the recipe book the recipe is filed in, and it is a property of the
     * recipe rather than of anything the player has.
     * example:
     * <pre>
     * // the recipes grouped the way the recipe book groups them
     * function byGroup(recipes) {
     *   const groups = [];
     *   for (let i = 0; i !== recipes.size(); i += 1) {
     *     const group = recipes.get(i).getGroup();
     *     if (!groups.includes(group)) {
     *       groups.push(group);
     *     }
     *   }
     *   for (const g of groups) {
     *     let count = 0;
     *     for (let i = 0; i !== recipes.size(); i += 1) {
     *       if (recipes.get(i).getGroup() === g) {
     *         count += 1;
     *       }
     *     }
     *     Chat.log(`${g}: ${count} recipes`);
     *   }
     * }
     * </pre>
     *
     * @return the type of this recipe.
     * @since 1.8.4
     */
    public String getGroup() {
        return BuiltInRegistries.RECIPE_BOOK_CATEGORY.getKey(base.category()).toString();
    }

//    /**
//     * This will not account for the actual items used in the recipe, but only the default recipe
//     * itself. Items with durability or with a lot of tags will probably not work correctly.
//     *
//     * @return will return {@code true} if any of the default ingredients have a recipe remainder.
//     * @since 1.8.4
//     */
//    public boolean hasRecipeRemainders() {
//        base.isCraftable()
//        return base.value().getIngredients().stream().anyMatch(ingredient -> ingredient.getMatchingStacks()[0].getItem().hasRecipeRemainder());
//    }
//
//    /**
//     * @return a list of all possible recipe remainders.
//     * @since 1.8.4
//     */
//    public List<List<ItemStackHelper>> getRecipeRemainders() {
//        return base.value().getIngredients().stream()
//                .filter(ingredient -> ingredient.getMatchingStacks().length > 0 && ingredient.getMatchingStacks()[0].getItem().hasRecipeRemainder())
//                .map(ingredient -> Arrays.stream(ingredient.getMatchingStacks()).map(ItemStackHelper::new).collect(Collectors.toList()))
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * @return the type of this recipe.
//     * @since 1.8.4
//     */
//    @DocletReplaceReturn("RecipeTypeId")
//    public String getType() {
//        return Registries.RECIPE_TYPE.getId(base.value().getType()).toString();
//    }

    /**
     * Whether the player can make this once with what they are carrying. The count comes from the
     * player's inventory and, when a recipe book screen is open, from the crafting slots on it as
     * well, so opening a crafting screen can change the answer.<br>
     * This is the recipe's own answer for one craft; {@link #getCraftableAmount()} is how many it
     * works out on its own.
     * example:
     * <pre>
     * // can this be made at all right now
     * function makeable(recipes, id) {
     *   for (let i = 0; i !== recipes.size(); i += 1) {
     *     const recipe = recipes.get(i);
     *     if (recipe.getOutput().getItemId() === id) {
     *       return recipe.canCraft();
     *     }
     *   }
     *   return false;
     * }
     *
     * // which of these the player can make at all
     * function listMakeable(recipes) {
     *   for (let i = 0; i !== recipes.size(); i += 1) {
     *     if (recipes.get(i).canCraft()) {
     *       Chat.log(recipes.get(i).getOutput().getItemId());
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the recipe can be crafted with the current inventory, {@code false}
     * otherwise.
     * @since 1.8.4
     */

    public boolean canCraft() {
        StackedItemContents recipeFinder = new StackedItemContents();
        mc.player.getInventory().fillStackedContents(recipeFinder);
        if (mc.screen instanceof AbstractRecipeBookScreen<?> screen) {
            screen.getMenu().fillCraftSlotsStackedContents(recipeFinder);
        }
        return base.canCraft(recipeFinder);
    }

    /**
     * Whether the player can make this recipe at least the given number of times. This is a
     * comparison rather than a fresh count: the amount is worked out once and then held against
     * the number asked for, so asking for zero is always true.
     * example:
     * <pre>
     * // enough for a stack, or for a full inventory of them
     * function enough(recipes, id, wanted) {
     *   for (let i = 0; i !== recipes.size(); i += 1) {
     *     const recipe = recipes.get(i);
     *     if (recipe.getOutput().getItemId() === id) {
     *       return recipe.canCraft(wanted);
     *     }
     *   }
     *   return false;
     * }
     *
     * // how many of the recipes here can make a stack or more
     * function stacks(recipes, id) {
     *   let count = 0;
     *   for (let i = 0; i !== recipes.size(); i += 1) {
     *     if (recipes.get(i).getOutput().getItemId() === id) {
     *       if (recipes.get(i).canCraft(64)) {
     *         count += 1;
     *       }
     *     }
     *   }
     *   return count;
     * }
     * </pre>
     *
     * @param amount the amount of items to craft
     * @return {@code true} if the given amount of items can be crafted with the current inventory,
     * {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean canCraft(int amount) {
        return getCraftableAmount() >= amount;
    }

    /**
     * How many times the player could make this recipe with what they are carrying. The count comes
     * from the player's inventory and, when a recipe book screen is open, from the crafting slots on
     * it as well, and it is worked out from the recipe's own ingredient requirements rather than
     * by making anything.
     * <p>
     * A player with nothing gives zero, and a recipe that needs more of something than a stack can
     * hold is capped by what the game allows rather than going above that.
     * example:
     * <pre>
     * // the best recipe for a given item, by how many it would make
     * function bestFor(recipes, id) {
     *   let best = 0;
     *   for (let i = 0; i !== recipes.size(); i += 1) {
     *     const recipe = recipes.get(i);
     *     if (recipe.getOutput().getItemId() === id) {
     *       if (recipe.getCraftableAmount() > best) {
     *         best = recipe.getCraftableAmount();
     *       }
     *     }
     *   }
     *   return best;
     * }
     *
     * // and the same count next to the recipe's own answer for one craft
     * function bothWays(recipes) {
     *   for (let i = 0; i !== recipes.size(); i += 1) {
     *     const recipe = recipes.get(i);
     *     if (recipe.canCraft()) {
     *       Chat.log(`${recipe.getOutput().getItemId()}: ${recipe.getCraftableAmount()} times`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return how often the recipe can be crafted with the current player inventory.
     * @since 1.8.4
     */
    public int getCraftableAmount() {
        StackedItemContents recipeFinder = new StackedItemContents();
        mc.player.getInventory().fillStackedContents(recipeFinder);
        if (mc.screen instanceof AbstractRecipeBookScreen<?> screen) {
            screen.getMenu().fillCraftSlotsStackedContents(recipeFinder);
        }
        return recipeFinder.raw.tryPickAll(base.craftingRequirements().get(), Integer.MAX_VALUE, null);
    }

    @Override
    public String toString() {
        return String.format("RecipeHelper:{\"id\": \"%s\"}", base.id().toString());
    }

}
