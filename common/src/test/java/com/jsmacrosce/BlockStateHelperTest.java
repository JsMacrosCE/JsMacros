package com.jsmacrosce;

import com.jsmacrosce.jsmacros.client.api.helper.world.BlockHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

// Bootstraps registries only. No client, server, world, or native window is created.
public final class BlockStateHelperTest {
    private static int checks;

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        check(!blocks(Blocks.AIR), "air");
        check(blocks(Blocks.STONE), "stone");
        check(!blocks(Blocks.COBWEB), "cobweb");
        check(!blocks(Blocks.BAMBOO_SAPLING), "bamboo sapling");
        check(!blocks(Blocks.WATER), "water");
        //? if <=26.2 {
        /*for (Block block : new Block[]{Blocks.AIR, Blocks.STONE, Blocks.COBWEB, Blocks.BAMBOO_SAPLING, Blocks.WATER}) {
            check(blocks(block) == block.defaultBlockState().blocksMotion(),
                    "matches legacy blocksMotion for " + BuiltInRegistries.BLOCK.getKey(block));
        }
        *///?}
        check(!new BlockStateHelper(Blocks.STONE.defaultBlockState()).hasTag("jsmacrosce:missing_tag"), "unknown state tag");
        check(!new BlockHelper(Blocks.STONE).hasTag("jsmacrosce:missing_tag"), "unknown block tag");
        check(!new BlockStateHelper(Blocks.STONE.defaultBlockState()).hasTag("#jsmacrosce:missing_tag"), "hash-prefixed tag");
        check(!new BlockStateHelper(Blocks.STONE.defaultBlockState()).hasTag("missing_tag"), "namespace default");
        System.out.println("BlockStateHelper: " + checks + " block-state checks passed");
    }

    private static boolean blocks(Block block) {
        return new BlockStateHelper(block.defaultBlockState()).blocksMovement();
    }

    private static void check(boolean success, String message) {
        checks++;
        if (!success) throw new AssertionError(message);
    }
}
