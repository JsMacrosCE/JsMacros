package com.jsmacrosce.jsmacros.client.util;

//? if >=26.3 {
/*import com.jsmacrosce.doclet.DocletIgnore;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.MultiRegistryBootstrap;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.packs.VanillaBrewingProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.predicates.LootPredicates;
import net.minecraft.world.level.storage.loot.predicates.MatchBlock;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConditionalValue;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.Quotient;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import java.util.List;
import java.util.Set;

// Isolated vanilla bootstraps: never apply fallback tags or components to the live registries.
@DocletIgnore
public final class VanillaDataDefaults {
    private VanillaDataDefaults() {
    }

    private static final class Snapshot {
        private static final HolderLookup.Provider REGISTRIES = new RegistrySetBuilder()
                .add(Registries.DAMAGE_TYPE, DamageTypes::bootstrap)
                .add(Registries.ENCHANTMENT, Enchantments::bootstrap)
                .add(Registries.PREDICATE, LootPredicates::bootstrap)
                .add(Registries.CONTEXT_INT_PROVIDER, ContextIntProviders::bootstrap)
                .add(Registries.TRIM_PATTERN, TrimPatterns::bootstrap)
                .add(brewingBootstrap())
                .build(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        private static final List<BrewingRecipe> BREWING = REGISTRIES.lookupOrThrow(Registries.RECIPE)
                .listElements().map(Holder::value).filter(BrewingRecipe.class::isInstance)
                .map(BrewingRecipe.class::cast).toList();
    }

    public static List<BrewingRecipe> brewing() {
        return Snapshot.BREWING;
    }

    public static int fuelTicks(ResolvableInt value) {
        if (value instanceof ResolvableInt.Constant constant) return constant.value();
        ResolvableInt.Reference reference = (ResolvableInt.Reference) value;
        ContextIntProvider provider = Snapshot.REGISTRIES.lookupOrThrow(Registries.CONTEXT_INT_PROVIDER)
                .getOrThrow(reference.key()).value();
        try {
            return fold(provider, Blocks.FURNACE.defaultBlockState(), 0);
        } catch (ArithmeticException e) {
            return 0;
        }
    }

    public static boolean hasFuelProvider(ResolvableInt value) {
        return value instanceof ResolvableInt.Constant
                || Snapshot.REGISTRIES.lookupOrThrow(Registries.CONTEXT_INT_PROVIDER)
                .get(((ResolvableInt.Reference) value).key()).isPresent();
    }

    private static int fold(ContextIntProvider provider, BlockState state, int depth) {
        if (depth > 64) throw new IllegalStateException("Cyclic or excessively deep vanilla fuel provider");
        return switch (provider) {
            case ConstantValue constant -> constant.value();
            case Quotient quotient -> fold(quotient.left().value(), state, depth + 1)
                    / fold(quotient.right().value(), state, depth + 1);
            case ConditionalValue conditional -> {
                if (!(conditional.condition().value() instanceof MatchBlock match) || match.predicate().willMatchBlockEntity()) {
                    throw new UnsupportedOperationException("Vanilla fuel condition requires server context: " + conditional.condition());
                }
                yield fold((match.predicate().matchesState(state) ? conditional.onTrue() : conditional.onFalse()).value(), state, depth + 1);
            }
            default -> throw new UnsupportedOperationException("Vanilla fuel provider requires server context: " + provider);
        };
    }

    private static MultiRegistryBootstrap brewingBootstrap() {
        return new MultiRegistryBootstrap() {
            @Override
            public Set<ResourceKey<? extends Registry<?>>> requestedRegistries() {
                return Set.of(Registries.RECIPE, Registries.ADVANCEMENT);
            }

            @Override
            public void run(MultiRegistryBootstrap.BootstrapGetter registries) {
                // Let the native RecipeProvider supply RecipeOutput, including loader extensions.
                new RecipeProvider(registries.get(Registries.RECIPE), registries.get(Registries.ADVANCEMENT)) {
                    @Override
                    public void buildRecipes() {
                        new VanillaBrewingProvider(output) {}.buildRecipes();
                    }
                }.buildRecipes();
            }
        };
    }
}
*///?}
