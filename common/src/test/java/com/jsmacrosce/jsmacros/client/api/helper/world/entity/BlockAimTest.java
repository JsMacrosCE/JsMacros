package com.jsmacrosce.jsmacros.client.api.helper.world.entity;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.Map;

public final class BlockAimTest {
    private static final BlockPos TARGET = new BlockPos(5, 10, -3);
    private static final Vec3 CENTER = Vec3.atCenterOf(TARGET);
    private static int checks;

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        BlockGetter stone = new TestBlocks(Map.of(TARGET, Blocks.STONE));
        for (Direction face : Direction.values()) {
            Vec3 eyes = CENTER.add(3 * face.getStepX(), 3 * face.getStepY(), 3 * face.getStepZ());
            check(hitFace(stone, eyes, find(stone, eyes, face), face), "aiming at the " + face + " face");
        }

        Vec3 westEyes = CENTER.add(-3, 0, 0);
        check(hitFace(stone, westEyes, find(stone, westEyes, null), Direction.WEST), "aiming without a face");
        check(find(stone, westEyes, Direction.EAST) == null, "the back of a block is not visible");
        check(find(new TestBlocks(Map.of()), westEyes, null) == null, "air has no outline");

        BlockPos inFront = TARGET.relative(Direction.WEST);
        BlockGetter blocked = new TestBlocks(Map.of(TARGET, Blocks.STONE, inFront, Blocks.STONE));
        check(find(blocked, westEyes, null) == null, "a nearer full block hides the target");

        BlockGetter halfBlocked = new TestBlocks(Map.of(TARGET, Blocks.STONE, inFront, Blocks.STONE_SLAB));
        Vec3 lowEyes = westEyes.add(0, -0.25, 0);
        check(hitFace(halfBlocked, lowEyes, find(halfBlocked, lowEyes, Direction.WEST), Direction.WEST),
                "finds a hit above the slab when the center ray is blocked");

        BlockGetter slab = new TestBlocks(Map.of(TARGET, Blocks.STONE_SLAB));
        Vec3 above = CENTER.add(0, 3, 0);
        check(hitFace(slab, above, find(slab, above, Direction.UP), Direction.UP),
                "hits the top of the outline rather than the top of the block space");
        System.out.println("Block aim: " + checks + " raycast checks passed");
    }

    private static float[] find(BlockGetter level, Vec3 eyes, Direction face) {
        return ClientPlayerEntityHelper.findBlockAngles(level, TARGET, eyes, face, CollisionContext.empty());
    }

    private static boolean hitFace(BlockGetter level, Vec3 eyes, float[] angles, Direction face) {
        if (angles == null || angles.length != 2) return false;
        Vec3 end = eyes.add(Vec3.directionFromRotation(angles[1], angles[0]).scale(5));
        BlockHitResult hit = level.clip(new ClipContext(eyes, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(TARGET) && hit.getDirection() == face;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private record TestBlocks(Map<BlockPos, Block> blocks) implements BlockGetter {
        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return blocks.getOrDefault(pos, Blocks.AIR).defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public int getHeight() {
            return 128;
        }

        @Override
        public int getMinY() {
            return 0;
        }
    }
}
