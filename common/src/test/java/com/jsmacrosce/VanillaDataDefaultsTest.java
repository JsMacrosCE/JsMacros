package com.jsmacrosce;

//? if >=26.3 {
/*import com.jsmacrosce.jsmacros.client.util.VanillaDataDefaults;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.BrewingInput;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;

// Bootstraps data only. No client, server, world, or native window is created.
public final class VanillaDataDefaultsTest {
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        check(VanillaDataDefaults.fuelTicks(ResolvableInt.fromKey(ContextIntProviders.COOKING_TIME_COAL)) == 1600, "coal default");
        check(VanillaDataDefaults.fuelTicks(ResolvableInt.fromKey(ContextIntProviders.COOKING_TIME_DRIED_KELP_BLOCK)) == 4001, "exact target kelp default");
        check(VanillaDataDefaults.fuelTicks(new ResolvableInt.Constant(37)) == 37, "constant component override");
        check(!VanillaDataDefaults.hasFuelProvider(ResolvableInt.fromKey(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.CONTEXT_INT_PROVIDER,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("remote", "unknown_fuel")))), "unknown remote providers are omitted, not fabricated");
        check(!VanillaDataDefaults.brewing().isEmpty(), "native brewing recipes loaded");

        // Minimal standalone item prototypes; never run in a live client or alter its tags.
        BuiltInRegistries.ITEM.listElements().forEach(holder -> holder.bindComponents(DataComponentMap.EMPTY));
        ItemStack water = new ItemStack(Items.POTION);
        water.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
        BrewingInput input = new BrewingInput(water, new ItemStack(Items.NETHER_WART));
        var recipe = VanillaDataDefaults.brewing().stream().filter(candidate -> candidate.matches(input)).findFirst().orElseThrow();
        check(recipe.assemble(input).get(DataComponents.POTION_CONTENTS).potion().orElseThrow().equals(Potions.AWKWARD), "native water-to-awkward brewing");
        check(VanillaDataDefaults.brewing().stream().noneMatch(candidate -> candidate.matches(new BrewingInput(water, new ItemStack(Items.DIAMOND)))), "invalid brewing input");
        check(VanillaDataDefaults.brewing() == VanillaDataDefaults.brewing(), "isolated default snapshot is reused");
        System.out.println("VanillaDataDefaults: 8 data-only fallback checks passed");
    }

    private static void check(boolean success, String message) {
        if (!success) throw new AssertionError(message);
    }
}
*///?}
