package com.jsmacrosce.jsmacros.client.util;

//? if >=26.3 {
/*import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinAbstractFurnaceScreenHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.crafting.BrewingInput;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

@DocletIgnore
public final class LocalServerData {
    private LocalServerData() {
    }

    private static <T> T query(Function<MinecraftServer, T> live, Supplier<T> defaults) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null || !server.isRunning() || server.isShutdown()) return defaults.get();
        if (server.isSameThread()) return live.apply(server);
        return server.submit(() -> live.apply(server)).orTimeout(5, TimeUnit.SECONDS).join();
    }

    private static <T> T brewing(Function<List<BrewingRecipe>, T> operation) {
        return query(server -> operation.apply(server.getRecipeManager().getRecipes().stream()
                        .map(holder -> holder.value()).filter(BrewingRecipe.class::isInstance).map(BrewingRecipe.class::cast).toList()),
                () -> operation.apply(VanillaDataDefaults.brewing()));
    }

    public static boolean isBrewablePotion(ItemStack stack) {
        var contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null || contents.potion().isEmpty()) return false;
        return brewing(recipes -> recipes.stream().anyMatch(recipe -> {
            var output = recipe.getOutput().create().get(DataComponents.POTION_CONTENTS);
            return output != null && output.potion().equals(contents.potion());
        }));
    }

    public static boolean isBrewingIngredient(ItemStack stack) {
        ItemStack snapshot = stack.copy();
        return brewing(recipes -> recipes.stream().anyMatch(recipe -> recipe.getReagent().test(snapshot)));
    }

    public static boolean canBrew(ItemStack potion, ItemStack ingredient) {
        BrewingInput input = new BrewingInput(potion.copy(), ingredient.copy());
        return brewing(recipes -> recipes.stream().anyMatch(recipe -> recipe.matches(input)));
    }

    public static ItemStack brew(ItemStack potion, ItemStack ingredient) {
        BrewingInput input = new BrewingInput(potion.copy(), ingredient.copy());
        return brewing(recipes -> recipes.stream().filter(recipe -> recipe.matches(input)).findFirst()
                .map(recipe -> recipe.assemble(input)).orElse(potion));
    }

    public static Map<String, Integer> fuelValues(int menuId) {
        UUID playerId = Minecraft.getInstance().player.getUUID();
        return query(server -> {
            var player = server.getPlayerList().getPlayer(playerId);
            if (player == null || player.containerMenu.containerId != menuId
                    || !(player.containerMenu instanceof AbstractFurnaceMenu menu)
                    || !(((MixinAbstractFurnaceScreenHandler) menu).jsmacros_getContainer() instanceof AbstractFurnaceBlockEntity furnace)) {
                return defaultFuelValues();
            }
            var level = player.level();
            LootContext context = new LootContext.Builder(new LootParams.Builder(level)
                    .withParameter(LootContextParams.BLOCK_STATE, furnace.getBlockState())
                    .withParameter(LootContextParams.BLOCK_ENTITY, furnace)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(furnace.getBlockPos()))
                    .withParameter(LootContextParams.CONTAINER, furnace)
                    .create(LootContextParamSets.CONTAINER_PROCESS)).create(Optional.empty());
            return collectFuel(fuel -> fuel.burnTime().get(context, 0));
        }, LocalServerData::defaultFuelValues);
    }

    public static Map<String, Integer> defaultFuelValues() {
        return collectFuel(fuel -> VanillaDataDefaults.hasFuelProvider(fuel.burnTime())
                ? VanillaDataDefaults.fuelTicks(fuel.burnTime()) : 0);
    }

    private static Map<String, Integer> collectFuel(Function<CookingFuel, Integer> ticks) {
        Map<String, Integer> result = new HashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            CookingFuel fuel = item.components().get(DataComponents.COOKING_FUEL);
            if (fuel != null) {
                int duration = ticks.apply(fuel);
                if (duration > 0) result.put(BuiltInRegistries.ITEM.getKey(item).toString(), duration);
            }
        }
        return result;
    }
}
*///?}
